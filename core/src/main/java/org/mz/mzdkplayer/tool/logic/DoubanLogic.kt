package org.mz.mzdkplayer.tool.logic

import java.net.URLEncoder

/**
 * 豆瓣刮削的**纯逻辑**（不碰 Android / 不碰网络），便于 JVM 单测。
 *
 * 只负责三件事：接口地址与请求头怎么拼、返回的字段怎么归一化、多个搜索候选怎么打分。
 * 真正的 HTTP 请求见 `org.mz.mzdkplayer.data.douban.DoubanScraper`。
 *
 * ### 实测结论（决定了这里为什么这么写）
 * 1. **不能走“抓 HTML + 解析 JSON-LD”那条路**：`movie.douban.com/subject/{id}/` 对非浏览器的
 *    自动化请求只返回约 130 字节的“载入中…”空壳页，`<script type="application/ld+json">` 已经不再下发。
 * 2. **详情走移动端 rexxar 接口**：`m.douban.com/rexxar/api/v2/{movie|tv}/{id}`，带桌面 Chrome UA +
 *    `Referer: https://m.douban.com/` 时返回完整 JSON（标题/简介/评分/类型/国家/上映日期/海报/演职员）。
 *    电影 id 用 `/tv/` 会 404，剧集 id 用 `/movie/` 会 301 跳转，所以调用方要按类型分别请求并在失败时换一条重试。
 * 3. **搜索走 `j/subject_suggest`**：返回的是**顶层 JSON 数组**（不是 TMDB 那种 `{results:[...]}`）。
 *    注意里面的 `type` 字段**不可信**——实测电视剧《三体》也返回 `type=movie`，只有 `episode` 非空能暗示
 *    “这是一部有集数的剧集”。
 * 4. **豆瓣图床有防盗链**：不带 `Referer` 时返回 HTTP 418（14 字节占位图），必须带 Referer 才能拿到真图，
 *    详见 `DoubanLogic.REFERER_*` 与 `:app` 里给 Coil 配置的全局 ImageLoader。
 */
object DoubanLogic {

    /** 搜索建议接口（顶层 JSON 数组） */
    const val SEARCH_URL = "https://movie.douban.com/j/subject_suggest"

    /** 详情接口前缀（移动端 rexxar） */
    const val DETAIL_BASE_URL = "https://m.douban.com/rexxar/api/v2"

    /** 搜索接口要求域名级 Referer，否则容易被反爬拦下 */
    const val SEARCH_REFERER = "https://movie.douban.com/"

    /** rexxar 详情接口必须带这个 Referer 才返回数据 */
    const val DETAIL_REFERER = "https://m.douban.com/"

    /**
     * 统一使用桌面 Chrome UA。
     *
     * 选它而不是移动端 UA 的原因：实测移动端/爬虫 UA 在 `movie.douban.com` 上直接被送到反爬空壳页，
     * 而这个桌面 UA 在 rexxar 详情接口上稳定可用；同时它是真实浏览器标识，不会伪装成官方 App 的
     * `api-client/x com.douban.frodo/...`（那属于伪装客户端，容易被封且不诚实）。
     */
    const val BROWSER_UA =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36"

    /** 搜索关键词直接拼进 query，必须 URL 编码（中文/空格） */
    fun searchUrl(keyword: String): String = "$SEARCH_URL?q=" + URLEncoder.encode(keyword, "UTF-8")

    /**
     * 是否豆瓣域名（`img1.doubanio.com`、`m.douban.com` 等都算）。
     *
     * 图片加载器要靠它决定「要不要补 Referer」：豆瓣图床对没有 Referer 的请求一律返回 HTTP 418，
     * 补头只对豆瓣加，别的图床（TMDB 等）不要被牵连。
     */
    fun isDoubanHost(host: String): Boolean {
        val lower = host.lowercase()
        return lower == "douban.com" || lower == "doubanio.com" ||
            lower.endsWith(".douban.com") || lower.endsWith(".doubanio.com")
    }

    fun detailUrl(subjectId: String, isTv: Boolean): String =
        "$DETAIL_BASE_URL/${if (isTv) "tv" else "movie"}/$subjectId"

    /**
     * 剧照接口。豆瓣详情里没有“背景图/宽幅图”字段，只有竖版海报，
     * 详情页要的 16:9 背景图来自 `type=W`（宽幅）剧照的第一张。
     */
    fun photosUrl(subjectId: String, isTv: Boolean, count: Int = 1): String =
        "${detailUrl(subjectId, isTv)}/photos?type=W&start=0&count=$count"

    /**
     * 豆瓣图床把同一张图按尺寸分段：`/view/photo/s_ratio_poster/`（小）、`m_ratio_poster`（中）、
     * `l_ratio_poster`（大，约 1000px）。刮削统一升到 l 档，避免详情页海报糊。
     * 已经是 l 档的地址原样返回（`/view/photo/l/public/...` 这种不带 ratio 的也保持不动）。
     */
    private val SMALL_RATIO_POSTER = Regex("/view/photo/(?:s|m)_ratio_poster/")

    fun largePosterUrl(raw: String?): String? {
        val url = raw?.trim().orEmpty()
        if (url.isEmpty()) return null
        return SMALL_RATIO_POSTER.replace(url, "/view/photo/l_ratio_poster/")
    }

    /** 背景/剧照地址：豆瓣给的就是 `image.large.url`，只需要去空 */
    fun backdropUrl(raw: String?): String? = raw?.trim()?.takeIf { it.isNotEmpty() }

    /** 豆瓣标题里可能带不可见的方向控制符（‎ / ‏）和尾巴上的年份，展示前清掉 */
    private val DIRECTION_MARKS = Regex("[\u200E\u200F\u202A-\u202E]")
    private val TRAILING_YEAR = Regex("""\s*[（(]\s*(?:18|19|20)\d{2}\s*[)）]\s*$""")

    fun cleanTitle(raw: String?): String {
        val trimmed = raw?.trim().orEmpty()
        if (trimmed.isEmpty()) return ""
        return TRAILING_YEAR.replace(DIRECTION_MARKS.replace(trimmed, ""), "").trim()
    }

    /** `"2019"` / `"2019-02-05"` → 2019；非年份返回 null */
    fun toYear(raw: String?): Int? {
        val text = raw?.trim().orEmpty()
        if (text.length < 4) return null
        val year = text.take(4).toIntOrNull() ?: return null
        return year.takeIf { it in 1800..2200 }
    }

    /**
     * 上映日期归一化：豆瓣给的是 `["2019-02-05(中国大陆)"]`，去掉括号里的地区说明，
     * 拿不到日期时退回年份字符串（TMDB 的 releaseDate 也是字符串，UI 只做展示）。
     */
    fun releaseDate(pubdate: List<String>?, year: Int?): String? {
        val first = pubdate?.firstOrNull { it.isNotBlank() }?.trim()
        if (!first.isNullOrEmpty()) {
            val date = first.substringBefore('(').substringBefore('（').trim()
            if (date.isNotEmpty()) return date
        }
        return year?.toString()
    }

    /** `durations` 形如 `["125分钟", "137分钟(重映版)"]`，取第一条并去掉括号说明 */
    fun firstDuration(durations: List<String>?): String? =
        durations?.firstOrNull { it.isNotBlank() }?.substringBefore('(')?.substringBefore('（')?.trim()
            ?.takeIf { it.isNotEmpty() }

    /** 演职员列表：豆瓣返回的顺序已按番位排好，这里只做去空、去重、限长 */
    fun creditNames(names: List<String?>?, limit: Int): List<String> =
        names.orEmpty()
            .mapNotNull { it?.trim()?.takeIf { name -> name.isNotEmpty() } }
            .distinct()
            .take(limit)

    /**
     * 搜索候选。`hasEpisode` 来自 `subject_suggest` 的 `episode` 字段（非空 = 有集数，多半是剧集）。
     */
    data class Candidate(
        val id: String,
        val title: String,
        val year: Int?,
        val hasEpisode: Boolean,
        val posterUrl: String?,
    )

    /**
     * 从搜索建议里挑最匹配的一条。
     *
     * 打分规则（越大越优先，同分时保留豆瓣自己的相关度顺序 —— `maxByOrNull` 取第一个最大值）：
     * - 标题完全相同 +100；前缀互相包含 +60；互相包含 +30
     * - 年份相同 +40，不同 -20（没有年份信息时不加不减）
     * - 与期望类型一致 +15，不一致 -15
     *
     * 类型只作为**加权**而不是过滤：`type` 字段实测不可信，若强行按它过滤会把《三体》这种剧集过滤掉。
     */
    fun pickBest(
        candidates: List<Candidate>,
        expectedTitle: String,
        expectedYear: Int?,
        preferTv: Boolean?,
        /** 每条候选的打分回传（`候选 → 分数`），仅用于日志排查，默认不启用 */
        onScored: ((Candidate, Int) -> Unit)? = null,
    ): Candidate? {
        if (candidates.isEmpty()) return null
        val want = normalizeTitle(expectedTitle)
        // 刻意手写循环而不是 maxByOrNull：标准库的 maxByOrNull 对**单元素**集合会直接返回该元素、
        // 不调用 selector，打分日志就会漏记（实测候选恰好只有一条时日志为空）。
        // 语义与 maxByOrNull 保持一致：同分保留先出现的那条（豆瓣自己的相关度顺序）。
        var best: Candidate? = null
        var bestScore = 0
        for (candidate in candidates) {
            val score = score(candidate, want, expectedYear, preferTv)
            onScored?.invoke(candidate, score)
            if (best == null || score > bestScore) {
                best = candidate
                bestScore = score
            }
        }
        return best
    }

    private fun score(candidate: Candidate, want: String, year: Int?, preferTv: Boolean?): Int {
        var score = 0
        val got = normalizeTitle(candidate.title)
        if (want.isNotEmpty() && got.isNotEmpty()) {
            score += when {
                got == want -> 100
                got.startsWith(want) || want.startsWith(got) -> 60
                got.contains(want) || want.contains(got) -> 30
                else -> 0
            }
        }
        if (year != null && candidate.year != null) {
            score += if (year == candidate.year) 40 else -20
        }
        if (preferTv != null) {
            score += if (preferTv == candidate.hasEpisode) 15 else -15
        }
        return score
    }

    /** 比较标题时先归一化：小写、去空白、去中英标点（`流浪地球.2019` 与 `流浪地球` 视为相近） */
    private val TITLE_NOISE = Regex("[\\s\\p{Punct}：:，。、·！？；’‘“”《》（）【】〈〉…—－·・]+")

    fun normalizeTitle(raw: String): String = TITLE_NOISE.replace(raw.lowercase(), "")
}
