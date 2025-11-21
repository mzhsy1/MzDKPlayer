package org.mz.mzdkplayer.data.model

/**
 * 一个通用的媒体数据模型，用于 UI 层展示列表项。
 * 它可以统一表示 Movie（电影）和 TV（剧集）的基本信息。
 * * 这个类作为 database entity (MediaCacheEntity) 和 UI 之间的桥梁。
 */
data class MediaItem(
    val id: Int = 0,                  // TMDB ID
    val title: String?,               // 标题 (Movie.title 或 TV.name)
    val overview: String,             // 简介
    val posterPath: String?,          // 海报路径
    val releaseDate: String?,         // 发布日期 (Movie.release_date 或 TV.first_air_date)
    val isMovie: Boolean = true,      // 类型标记

    // === TV 专属字段 ===
    val seasonNumber: Int = 0,        // 第几季 (用于定位文件对应的具体集数)
    val episodeNumber: Int = 0        // 第几集
)