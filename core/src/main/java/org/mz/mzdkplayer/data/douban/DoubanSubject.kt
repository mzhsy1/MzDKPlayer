package org.mz.mzdkplayer.data.douban

/**
 * 豆瓣刮削的归一化结果（与接口解耦，调用方只依赖这个模型）。
 *
 * 字段口径刻意对齐 TMDB 那边：`rating` 都是 10 分制；`posterUrl`/`backdropUrl` 都是**完整可直连的
 * URL**（豆瓣图床地址，`Tools.formatImageUrl` 对 `http` 开头的地址会原样透传）。
 */
data class DoubanSubject(
    /** 豆瓣条目 id（纯数字字符串）。注意它和 TMDB id **不是同一个 id 空间**，落库时要带来源区分。 */
    val id: String,
    val title: String,
    val originalTitle: String?,
    val year: Int?,
    /** 是否为剧集（取自接口的 `subtype`/`is_tv`，比搜索结果里的 `type` 字段可信） */
    val isTv: Boolean,
    val overview: String,
    val posterUrl: String?,
    /** 宽幅剧照，用作详情页背景图（豆瓣详情本身不给背景图，取 `type=W` 剧照第一张） */
    val backdropUrl: String?,
    /** 10 分制评分 */
    val rating: Double,
    val genres: List<String>,
    val countries: List<String>,
    val languages: List<String>,
    val releaseDate: String?,
    val duration: String?,
    /** 剧集总集数（电影为 null） */
    val episodesCount: Int?,
    val directors: List<String>,
    val actors: List<String>,
)
