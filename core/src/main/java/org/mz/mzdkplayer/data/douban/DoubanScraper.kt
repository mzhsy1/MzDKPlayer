package org.mz.mzdkplayer.data.douban

import android.util.Log
import com.google.gson.Gson
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.mz.mzdkplayer.data.repository.SettingsRepository
import org.mz.mzdkplayer.tool.logic.DoubanLogic
import org.mz.mzdkplayer.tool.logic.ScrapeTimeoutPolicy
import java.util.concurrent.TimeUnit

/**
 * 豆瓣刮削数据源（默认数据源，可在设置里改成 TMDB）。
 *
 * 两个接口、三条经验（都是实测出来的，别再改回去）：
 * 1. 搜索用 `movie.douban.com/j/subject_suggest?q=`，返回**顶层 JSON 数组**；
 * 2. 详情用 `m.douban.com/rexxar/api/v2/{movie|tv}/{id}`，**必须带 `Referer: https://m.douban.com/`**；
 * 3. 图床有防盗链，图片请求必须带 Referer（在 `:app` 的 Coil ImageLoader 里统一加，不在这里）。
 *
 * 详情接口按类型分两条路径，用错会失败：电影 id 走 `/tv/` 是 404、剧集 id 走 `/movie/` 是 301，
 * 所以先按期望类型请求，失败再换另一条重试。
 *
 * **故障处理**：网络层失败会抛 [DoubanNetworkException]，[scrape] 就地把这一次记成「没刮到」（返回 null）；
 * 「搜不到结果」与「请求失败」都不向上抛异常，由调用方按设置决定要不要再试别的源。
 *
 * 「修改文件对应影视信息」这类**手动匹配**走 [search] + [fetchById]：同一个 OkHttp 客户端、同一套请求头，
 * 只是不做「自动挑最匹配的一条」那步。
 */
class DoubanScraper(
    /** 固定 client（只有测试 / 自定义场景才传）；为 null 时按设置里的超时构建并缓存，见 [client] */
    private val fixedClient: OkHttpClient? = null,
    private val gson: Gson = Gson(),
) {

    /** 按设置超时构建出来的 client 缓存：超时值没变就复用（OkHttpClient 要共享连接池） */
    private var cachedClient: OkHttpClient? = null
    private var cachedTimeoutSeconds = -1

    /**
     * 取当前该用的 client。
     *
     * 每次请求都读一次设置里的超时值（[ScrapeTimeoutPolicy]）：用户在设置里改完超时，
     * **不需要重启应用**，下一次请求就按新值走。
     */
    private fun client(): OkHttpClient {
        fixedClient?.let { return it }
        val timeoutSeconds = SettingsRepository.scrapeTimeoutSeconds
        cachedClient?.let { if (timeoutSeconds == cachedTimeoutSeconds) return it }
        return defaultClient(timeoutSeconds.toLong()).also {
            cachedClient = it
            cachedTimeoutSeconds = timeoutSeconds
        }
    }

    /**
     * 一次完整的刮削：搜索 → 挑最匹配 → 取详情 → 补背景图。
     *
     * @param keyword 文件名解析出来的片名
     * @param year 文件名解析出来的年份（可为空）
     * @param preferTv 文件名结构判断的类型，用于搜索候选打分与详情路径首选
     * @return 拿不到数据一律返回 null（调用方按设置决定还有没有别的源要试），**不向上抛异常**
     */
    suspend fun scrape(keyword: String, year: Int?, preferTv: Boolean): DoubanSubject? =
        withContext(Dispatchers.IO) {
            if (keyword.isBlank()) return@withContext null
            val start = System.currentTimeMillis()
            Log.d(TAG, "[SCRAPE] 豆瓣刮削开始: keyword=「$keyword」year=$year preferTv=$preferTv")
            try {
                val subject = scrapeOnce(keyword, year, preferTv)
                Log.d(
                    TAG,
                    "[SCRAPE] 豆瓣刮削结束(${System.currentTimeMillis() - start}ms): " +
                        (subject?.let { "命中「${it.title}」(${it.year}) id=${it.id}" } ?: "未命中")
                )
                subject
            } catch (e: DoubanNetworkException) {
                Log.w(
                    TAG,
                    "[SCRAPE] 豆瓣网络层失败（${System.currentTimeMillis() - start}ms）: ${e.message}"
                )
                null
            } catch (e: CancellationException) {
                // 页面退出/任务取消不是「刮削失败」：必须透传，否则取消之后还会继续去试另一个源
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "[SCRAPE] 豆瓣刮削异常: $keyword", e)
                null
            }
        }

    private suspend fun scrapeOnce(keyword: String, year: Int?, preferTv: Boolean): DoubanSubject? =
        withContext(Dispatchers.IO) {
            val candidates = searchCandidates(keyword)
            if (candidates.isEmpty()) {
                Log.d(TAG, "[SCRAPE] 没有搜到候选: $keyword（接口是通的，本次按没刮到处理）")
                return@withContext null
            }

            // 把每条候选的打分留到日志里：匹配错条目时能一眼看出是标题没对上还是年份/类型拉了分
            val scored = mutableListOf<String>()
            val best = DoubanLogic.pickBest(candidates, keyword, year, preferTv) { candidate, score ->
                scored += "$score=${candidate.title}(${candidate.year}${if (candidate.hasEpisode) ",有集" else ""})"
            }
            if (best == null) {
                Log.d(TAG, "[SCRAPE] 候选打分后无可选项: $keyword")
                return@withContext null
            }
            Log.d(TAG, "[SCRAPE] 候选打分[期望 title=「$keyword」year=$year preferTv=$preferTv]: ${scored.joinToString(" ")}")
            Log.d(TAG, "[SCRAPE] 选中候选 id=${best.id} title=${best.title} year=${best.year} 有集=${best.hasEpisode}")

            fetchSubjectWithBackdrop(best.id, preferTv)
        }

    /**
     * 「修改文件对应影视信息」用的候选列表：按关键词搜一次。
     *
     * 与 [scrape] 的差别是**不做**打分挑选、也不取详情 —— 挑哪条由用户在界面上点。
     * 故障口径和 [scrape] 一致：网络层失败返回空列表，由界面自己提示「没搜到」。
     */
    suspend fun search(keyword: String): List<DoubanLogic.Candidate> =
        withContext(Dispatchers.IO) {
            if (keyword.isBlank()) return@withContext emptyList()
            try {
                searchCandidates(keyword)
            } catch (e: DoubanNetworkException) {
                Log.w(TAG, "[SCRAPE] 豆瓣搜索网络层失败: ${e.message}")
                emptyList()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "[SCRAPE] 豆瓣搜索异常: $keyword", e)
                emptyList()
            }
        }

    /**
     * 按条目 id 直接取详情（含背景图），供手动匹配「点选某条候选」使用。
     *
     * 拿不到返回 null，由调用方保留原记录 / 提示失败；与 [scrape] 一样不向上抛异常。
     */
    suspend fun fetchById(subjectId: String, preferTv: Boolean): DoubanSubject? =
        withContext(Dispatchers.IO) {
            if (subjectId.isBlank()) return@withContext null
            try {
                Log.d(TAG, "[SCRAPE] 豆瓣按 id 取详情: id=$subjectId preferTv=$preferTv")
                fetchSubjectWithBackdrop(subjectId, preferTv)
            } catch (e: DoubanNetworkException) {
                Log.w(TAG, "[SCRAPE] 豆瓣取详情网络层失败: ${e.message}")
                null
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "[SCRAPE] 豆瓣取详情异常: $subjectId", e)
                null
            }
        }

    /**
     * 取详情 + 尽力补背景图（两条人工/自动路径共用，见 [scrapeOnce] 与 [fetchById]）。
     *
     * 背景图要单独问一次剧照接口（详情接口只给竖版海报），且这一步是**可选**的：失败就只丢背景图
     * —— 既不连累已经刮到的信息，也不拿它去判「源挂了」。
     */
    private suspend fun fetchSubjectWithBackdrop(subjectId: String, preferTv: Boolean): DoubanSubject? {
        val subject = fetchSubject(subjectId, preferTv) ?: return null
        val withBackdrop = subject.copy(
            backdropUrl = subject.backdropUrl ?: try {
                fetchBackdrop(subjectId, subject.isTv)
            } catch (e: DoubanNetworkException) {
                Log.d(TAG, "背景图获取失败，忽略: ${e.message}")
                null
            }
        )
        Log.d(
            TAG,
            "[SCRAPE] 豆瓣详情成功: 「${withBackdrop.title}」(${withBackdrop.year}) id=${withBackdrop.id} " +
                "isTv=${withBackdrop.isTv} 海报=${withBackdrop.posterUrl != null} " +
                "背景=${withBackdrop.backdropUrl != null} 简介长度=${withBackdrop.overview?.length ?: 0}"
        )
        return withBackdrop
    }

    /** 搜索候选。响应不是 JSON（反爬空壳页）时抛 [DoubanNetworkException] */
    private suspend fun searchCandidates(keyword: String): List<DoubanLogic.Candidate> =
        withContext(Dispatchers.IO) {
            val url = DoubanLogic.searchUrl(keyword)
            Log.d(TAG, "[SCRAPE] 搜索请求: $url")
            val body = getJson(url) ?: return@withContext emptyList()
            val list = try {
                gson.fromJson(body, Array<DoubanSuggestDto>::class.java)?.toList().orEmpty()
            } catch (e: Exception) {
                throw DoubanNetworkException("搜索响应不是 JSON（可能被反爬挡住）: ${body.take(120)}")
            }
            val candidates = list.mapNotNull { it.toCandidate() }
            Log.d(
                TAG,
                "[SCRAPE] 搜索「$keyword」拿到 ${list.size} 条原始记录 → ${candidates.size} 条可用: " +
                    candidates.joinToString(" | ") { "${it.id}/「${it.title}」/${it.year}/${if (it.hasEpisode) "有集" else "无集"}" }
            )
            candidates
        }

    private suspend fun fetchSubject(subjectId: String, isTv: Boolean): DoubanSubject? {
        Log.d(TAG, "[SCRAPE] 详情首选路径: /${if (isTv) "tv" else "movie"}/$subjectId")
        fetchSubjectFrom(DoubanLogic.detailUrl(subjectId, isTv))?.let { return it }
        // 类型判断错了（文件名说电影、实际是剧集，或反之），换另一条路径再试一次
        Log.d(TAG, "[SCRAPE] 详情首选路径没拿到，换 /${if (isTv) "movie" else "tv"}/ 重试: $subjectId")
        return fetchSubjectFrom(DoubanLogic.detailUrl(subjectId, !isTv))
    }

    private suspend fun fetchSubjectFrom(url: String): DoubanSubject? = withContext(Dispatchers.IO) {
        val body = getJson(url) ?: return@withContext null
        try {
            val subject = gson.fromJson(body, DoubanSubjectDto::class.java)?.toSubject()
            if (subject == null) {
                Log.w(TAG, "[SCRAPE] 详情 JSON 解析成了空对象: $url")
            }
            subject
        } catch (e: Exception) {
            throw DoubanNetworkException("详情响应不是 JSON: $url")
        }
    }

    private suspend fun fetchBackdrop(subjectId: String, isTv: Boolean): String? =
        withContext(Dispatchers.IO) {
            val url = DoubanLogic.photosUrl(subjectId, isTv)
            val body = getJson(url) ?: return@withContext null
            try {
                val backdrop = gson.fromJson(body, DoubanPhotosDto::class.java)?.firstWideImageUrl()
                Log.d(TAG, "[SCRAPE] 背景图: ${backdrop ?: "没有 type=W 剧照"} ($url)")
                backdrop
            } catch (e: Exception) {
                Log.w(TAG, "[SCRAPE] 剧照解析失败: $subjectId", e)
                null
            }
        }

    /**
     * 发一次 GET 并返回 UTF-8 文本。
     *
     * - `404` = 对端明确说「没这个条目」→ 返回 null 让调用方换路径重试
     * - 连接异常 / 超时 / 其它非 2xx / 空响应体 → 抛 [DoubanNetworkException]（由调用方就地处成「没刮到」）
     */
    private fun getJson(url: String): String? {
        val request = Request.Builder().url(url).get().build()
        val start = System.currentTimeMillis()
        val response = try {
            client().newCall(request).execute()
        } catch (e: Exception) {
            Log.w(TAG, "[SCRAPE] 请求异常(${System.currentTimeMillis() - start}ms) $url → ${e.message}")
            throw DoubanNetworkException("请求 $url 失败: ${e.message}")
        }
        response.use { r ->
            val cost = System.currentTimeMillis() - start
            if (r.code == 404) {
                Log.d(TAG, "[SCRAPE] GET 404(${cost}ms)（条目不存在，换类型重试）: $url")
                return null
            }
            if (!r.isSuccessful) {
                // 非 2xx 时把响应体前一段打出来：反爬壳页、限流页都靠它认
                val preview = runCatching { r.body?.string()?.take(200) }.getOrNull()
                Log.w(TAG, "[SCRAPE] GET HTTP ${r.code}(${cost}ms) $url body=<$preview>")
                throw DoubanNetworkException("HTTP ${r.code} for $url")
            }
            val bytes = r.body?.bytes()
            if (bytes == null || bytes.isEmpty()) {
                Log.w(TAG, "[SCRAPE] 响应体为空(${cost}ms): $url")
                throw DoubanNetworkException("响应体为空: $url")
            }
            // 显式按 UTF-8 解码：接口用 application/json 且不总是带 charset，
            // 交给 okhttp 的默认字符集可能把中文解成乱码
            val body = String(bytes, Charsets.UTF_8)
            Log.d(
                TAG,
                "[SCRAPE] GET 200(${cost}ms) ${bytes.size}B $url 首段=<${body.take(120).replace('\n', ' ')}>"
            )
            return body
        }
    }

    companion object {
        private const val TAG = "DoubanScraper"

        /**
         * 按超时（秒）构建一个豆瓣专用 client。
         *
         * 超时值来自设置（见 `ScrapeTimeoutPolicy`，默认 5 秒）：豆瓣可达时响应是亚秒级，
         * 而连不上时每个文件都要白等满这个超时，所以把它做成用户可调的旋钮。
         */
        fun defaultClient(timeoutSeconds: Long = ScrapeTimeoutPolicy.DEFAULT_SECONDS.toLong()): OkHttpClient =
            OkHttpClient.Builder()
                .connectTimeout(timeoutSeconds, TimeUnit.SECONDS)
                .readTimeout(timeoutSeconds, TimeUnit.SECONDS)
                .writeTimeout(timeoutSeconds, TimeUnit.SECONDS)
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
