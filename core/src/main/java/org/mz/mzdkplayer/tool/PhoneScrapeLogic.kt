package org.mz.mzdkplayer.tool

import java.util.Locale

/**
 * 手机端文件浏览里与「刮削」相关的纯逻辑（第四阶段）。
 *
 * 与 [FileBrowserLogic] / [PhoneFileBrowserLogic] 的分工：那两个管「路径怎么拼」，
 * 本对象管「目录里的哪些条目要去刮、刮完列表怎么显示」。抽出来的目的是能在 JVM 单测里
 * 锁住边界（目录/非视频/无播放地址不参与刮削、同一地址只刮一次、季集号补位）。
 *
 * 约定：这里**只能**用 JDK API（外加 [Tools] 这种纯 JDK 工具），不要引入 `android.*` / `androidx.*`。
 */
object PhoneScrapeLogic {

    /** 目录里一个条目在刮削视角下需要的最小信息 */
    data class Item(val name: String, val playbackUri: String?)

    /**
     * 刮削结果里列表要展示的字段。
     *
     * 之所以不直接用 `MediaCacheEntity`：那是 Room 实体（带 `androidx` 注解），
     * 这里拷成纯数据类后单测不需要碰 Android。
     */
    data class Meta(
        val title: String,
        val year: String? = null,
        val voteAverage: Double = 0.0,
        val mediaType: String = "movie",
        val seasonNumber: Int = 0,
        val episodeNumber: Int = 0,
        val posterPath: String? = null,
        val backdropPath: String? = null,
        val overview: String = "",
        val genres: List<String> = emptyList(),
    )

    /**
     * 目录里「需要刮削」的条目：视频文件且**有播放地址**（目录、图片、字幕、无地址的条目直接跳过）。
     *
     * 返回值是 `文件名 to 播放地址`，播放地址同时是 `media_cache` 的主键。
     * 同一个播放地址只保留第一次出现的那个：FTP / NFS / HTTP 的 key 可能不是完整地址，
     * 万一出现重复，重复请求只会白耗 TMDB 配额。
     */
    fun scrapeTargets(items: List<Item>): List<Pair<String, String>> {
        val seen = HashSet<String>()
        return items.mapNotNull { item ->
            val uri = item.playbackUri?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            if (!Tools.containsVideoFormat(Tools.extractFileExtension(item.name))) return@mapNotNull null
            if (!seen.add(uri)) return@mapNotNull null
            item.name to uri
        }
    }

    /**
     * 还需要去联网刮的条目：在 [scrapeTargets] 的基础上，排除库里**已有记录**的地址。
     *
     * 只按「有没有记录」判断，不区分 `isDetailsLoaded`：列表页要的是海报和标题，
     * 有记录就能显示；真要补详情由 `MovieViewModel` 那条链自己决定。
     */
    fun pendingTargets(items: List<Item>, cachedUris: Set<String>): List<Pair<String, String>> =
        scrapeTargets(items).filter { (_, uri) -> uri !in cachedUris }

    /**
     * 列表主标题：有刮削就用刮削标题（剧集补 `SxxExx`），否则原样文件名。
     *
     * 与电视端 `PlayerMediaText.buildTitle` 同一口径，只是这里**不带年份**
     * （年份在副标题里，列表行放不下那么长）。
     */
    fun displayTitle(meta: Meta?, fileName: String): String {
        val title = meta?.title?.trim().orEmpty()
        if (title.isEmpty()) return fileName

        val isTv = meta?.mediaType == "tv"
        val season = meta?.seasonNumber ?: 0
        val episode = meta?.episodeNumber ?: 0
        if (isTv && (season > 0 || episode > 0)) {
            return "$title " + String.format(Locale.US, "S%02dE%02d", season, episode)
        }
        return title
    }

    /**
     * 列表副标题：`年份 · 评分`（缺哪项就少哪项）。
     *
     * 没有刮削（或两项都缺）时返回 null，由 UI 回退到显示文件大小。
     */
    fun displaySubtitle(meta: Meta?): String? {
        if (meta == null) return null
        val parts = ArrayList<String>(2)
        meta.year?.takeIf { it.isNotBlank() }?.let { parts.add(it) }
        if (meta.voteAverage > 0.0) {
            parts.add(String.format(Locale.US, "%.1f", meta.voteAverage))
        }
        return parts.joinToString(" · ").ifBlank { null }
    }

    /**
     * 刮削进度百分比（0..100）。
     *
     * 扫描还没开始时 `total` 为 0，此时返回 0 而不是除零。
     */
    fun progressPercent(done: Int, total: Int): Int {
        if (total <= 0) return 0
        return ((done.toDouble() / total.toDouble()) * 100).toInt().coerceIn(0, 100)
    }
}
