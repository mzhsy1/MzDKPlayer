package org.mz.mzdkplayer.data.douban

import android.util.Log
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.mz.mzdkplayer.tool.logic.DoubanLogic
import org.mz.mzdkplayer.tool.logic.ScrapeCircuitBreaker
import java.util.concurrent.TimeUnit

/**
 * 豆瓣刮削数据源（默认首选源，可在设置里把首选切成 TMDB）。
 *
 * 两个接口、三条经验（都是实测出来的，别再改回去）：
 * 1. 搜索用 `movie.douban.com/j/subject_suggest?q=`，返回**顶层 JSON 数组**；
 * 2. 详情用 `m.douban.com/rexxar/api/v2/{movie|tv}/{id}`，**必须带 `Referer: https://m.douban.com/`**；
 * 3. 图床有防盗链，图片请求必须带 Referer（在 `:app` 的 Coil ImageLoader 里统一加，不在这里）。
 *
 * 详情接口按类型分两条路径，用错会失败：电影 id 走 `/tv/` 是 404、剧集 id 走 `/movie/` 是 301，
 * 所以先按期望类型请求，失败再换另一条重试。
 *
 * **故障处理**：网络层失败会抛 [DoubanNetworkException]，由 [scrape] 统一计数给 [ScrapeCircuitBreaker]；
 * 连续失败若干次后本数据源被短路一段时间，期间 [scrape] 直接返回 null，调用方立刻走另一个数据源。
 * 「搜不到结果」不算失败 —— 那说明接口是通的，反而会让熔断计数清零。
 */
class DoubanScraper(
    private val client: OkHttpClient = defaultClient(),
    private val gson: Gson = Gson(),
    private val circuitBreaker: ScrapeCircuitBreaker = sharedCircuitBreaker,
) {

    /**
     * 一次完整的刮削：搜索 → 挑最匹配 → 取详情 → 补背景图。
     *
     * @param keyword 文件名解析出来的片名
     * @param year 文件名解析出来的年份（可为空）
     * @param preferTv 文件名结构判断的类型，用于搜索候选打分与详情路径首选
     * @return 拿不到数据一律返回 null（调用方应当去试另一个数据源），**不向上抛异常**
     */
    suspend fun scrape(keyword: String, year: Int?, preferTv: Boolean): DoubanSubject? =
        withContext(Dispatchers.IO) {
            if (keyword.isBlank()) return@withContext null
            if (circuitBreaker.shouldSkip(System.currentTimeMillis())) {
                Log.d(TAG, "豆瓣处于短路期，本次直接交给另一个数据源: $keyword")
                return@withContext null
            }
            try {
                val subject = scrapeOnce(keyword, year, preferTv)
                // 能走到这里说明请求都拿到了可解析响应 —— 搜不到结果也算「接口是通的」
                circuitBreaker.onReachable(System.currentTimeMillis())
                subject
            } catch (e: DoubanNetworkException) {
                circuitBreaker.onNetworkFailure(System.currentTimeMillis())
                Log.w(TAG, "豆瓣网络层失败（已计入熔断）: ${e.message}")
                null
            } catch (e: Exception) {
                Log.w(TAG, "豆瓣刮削失败: $keyword", e)
                null
            }
        }

    private suspend fun scrapeOnce(keyword: String, year: Int?, preferTv: Boolean): DoubanSubject? =
        withContext(Dispatchers.IO) {
            val candidates = searchCandidates(keyword)
            if (candidates.isEmpty()) {
                Log.d(TAG, "没有搜到候选: $keyword")
                return@withContext null
            }

            val best = DoubanLogic.pickBest(candidates, keyword, year, preferTv)
                ?: return@withContext null
            Log.d(TAG, "选中候选 id=${best.id} title=${best.title} year=${best.year}")

            val subject = fetchSubject(best.id, preferTv) ?: return@withContext null

            // 背景图要单独问一次剧照接口（详情接口只给竖版海报）。
            // 这一步是可选的：失败就只丢背景图 —— 既不连累已经刮到的信息，也不拿它去判「源挂了」。
            val withBackdrop = subject.copy(
                backdropUrl = subject.backdropUrl ?: try {
                    fetchBackdrop(best.id, subject.isTv)
                } catch (e: DoubanNetworkException) {
                    Log.d(TAG, "背景图获取失败，忽略: ${e.message}")
                    null
                }
            )
            Log.d(TAG, "豆瓣刮削成功: ${withBackdrop.title}(${withBackdrop.year}) isTv=${withBackdrop.isTv}")
            withBackdrop
        }

    /** 搜索候选。响应不是 JSON（反爬空壳页）时抛 [DoubanNetworkException] */
    private suspend fun searchCandidates(keyword: String): List<DoubanLogic.Candidate> =
        withContext(Dispatchers.IO) {
            val body = getJson(DoubanLogic.searchUrl(keyword)) ?: return@withContext emptyList()
            val list = try {
                gson.fromJson(body, Array<DoubanSuggestDto>::class.java)?.toList().orEmpty()
            } catch (e: Exception) {
                throw DoubanNetworkException("搜索响应不是 JSON（可能被反爬挡住）")
            }
            list.mapNotNull { it.toCandidate() }
        }

    private suspend fun fetchSubject(subjectId: String, isTv: Boolean): DoubanSubject? {
        fetchSubjectFrom(DoubanLogic.detailUrl(subjectId, isTv))?.let { return it }
        // 类型判断错了（文件名说电影、实际是剧集，或反之），换另一条路径再试一次
        Log.d(TAG, "详情请求失败，换另一类型路径重试: $subjectId")
        return fetchSubjectFrom(DoubanLogic.detailUrl(subjectId, !isTv))
    }

    private suspend fun fetchSubjectFrom(url: String): DoubanSubject? = withContext(Dispatchers.IO) {
        val body = getJson(url) ?: return@withContext null
        try {
            gson.fromJson(body, DoubanSubjectDto::class.java)?.toSubject()
        } catch (e: Exception) {
            throw DoubanNetworkException("详情响应不是 JSON: $url")
        }
    }

    private suspend fun fetchBackdrop(subjectId: String, isTv: Boolean): String? =
        withContext(Dispatchers.IO) {
            val body = getJson(DoubanLogic.photosUrl(subjectId, isTv)) ?: return@withContext null
            try {
                gson.fromJson(body, DoubanPhotosDto::class.java)?.firstWideImageUrl()
            } catch (e: Exception) {
                Log.w(TAG, "剧照解析失败: $subjectId", e)
                null
            }
        }

    /**
     * 发一次 GET 并返回 UTF-8 文本。
     *
     * - `404` = 对端明确说「没这个条目」→ 返回 null 让调用方换路径重试，**不算故障**
     * - 连接异常 / 超时 / 其它非 2xx / 空响应体 → 抛 [DoubanNetworkException]（算故障，喂给熔断器）
     */
    private fun getJson(url: String): String? {
        val request = Request.Builder().url(url).get().build()
        val response = try {
            client.newCall(request).execute()
        } catch (e: Exception) {
            throw DoubanNetworkException("请求 $url 失败: ${e.message}")
        }
        response.use { r ->
            if (r.code == 404) return null
            if (!r.isSuccessful) throw DoubanNetworkException("HTTP ${r.code} for $url")
            val bytes = r.body?.bytes()
            if (bytes == null || bytes.isEmpty()) throw DoubanNetworkException("响应体为空: $url")
            // 显式按 UTF-8 解码：接口用 application/json 且不总是带 charset，
            // 交给 okhttp 的默认字符集可能把中文解成乱码
            return String(bytes, Charsets.UTF_8)
        }
    }

    companion object {
        private const val TAG = "DoubanScraper"

        /**
         * 单次请求超时。
         *
         * 豆瓣可达时响应是亚秒级的，所以这里给 10 秒就够；刻意不取更长 ——
         * 接口不可达时每个文件都要白等一个超时才轮到另一个源，熔断器（连续 2 次失败才生效）
         * 生效前的那一两次尝试就是靠这个值兜住体感。
         */
        private const val TIMEOUT_SECONDS = 10L

        /**
         * 全进程共享的熔断器。
         *
         * 必须是静态的一份：`MovieViewModel` 是按页面创建的（每次进详情页都会 new 一个，
         * 里面又 new 一个 [DoubanScraper]），熔断状态放在实例字段里就永远攒不到阈值。
         */
        private val sharedCircuitBreaker = ScrapeCircuitBreaker()

        fun defaultClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val request = chain.request()
                // 两个接口要的 Referer 不同：搜索要 movie 域，rexxar 详情要 m 域
                val referer = if (request.url.host.startsWith("m.")) {
                    DoubanLogic.DETAIL_REFERER
                } else {
                    DoubanLogic.SEARCH_REFERER
                }
                chain.proceed(
                    request.newBuilder()
                        .header("User-Agent", DoubanLogic.BROWSER_UA)
                        .header("Referer", referer)
                        .header("Accept", "application/json, text/plain, */*")
                        .header("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8")
                        .build()
                )
            }
            .build()
    }
}
