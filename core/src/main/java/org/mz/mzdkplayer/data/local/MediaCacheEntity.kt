package org.mz.mzdkplayer.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import org.mz.mzdkplayer.data.model.Genre
import org.mz.mzdkplayer.data.model.MediaItem

@Entity(tableName = "media_cache",indices = [
    androidx.room.Index(value = ["groupKey"]), // 加速分组
    androidx.room.Index(value = ["title"]),    // 加速排序
    androidx.room.Index(value = ["tmdbId"])    // 加速关联查询
])
@TypeConverters(MediaConverters::class)
data class MediaCacheEntity(
    @PrimaryKey
    val videoUri: String, // 主键：文件的完整 SMB URI (或者 HTTP URL, 本地路径)

    // === 核心播放源信息 (新增) ===
    val dataSourceType: String, // 例如: "SMB", "Local", "WebDAV"
    val fileName: String,       // 原始文件名 (用于展示或回溯)，例如 "Iron.Man.2008.mkv"
    val connectionName: String, // 连接配置名称 (用于查找存储的账号密码)，例如 "MyNas"

    // 基础信息 (列表页显示)

    val tmdbId: Int,
    val mediaType: String, // "movie" or "tv"
    val title: String,
    val overview: String,
    val posterPath: String?,
    val backdropPath: String?,
    val releaseDate: String?,
    val voteAverage: Double,

    // TV 专属定位信息
    val seasonNumber: Int = 0,
    val episodeNumber: Int = 0,

    // 详细信息
    val isDetailsLoaded: Boolean = false,
    val status: String = "",
    val genres: List<Genre> = emptyList(),
    val originCountry: List<String> = emptyList(),

    // TV 详情专属
    val numberOfSeasons: Int? = 0,
    val numberOfEpisodes: Int? = 0,

    // TV 单集详情专属
    val episodeName: String? = null,
    val episodeOverview: String? = null,
    val episodeStillPath: String? = null,
    val episodeAirDate: String? = null,
    val episodeRuntime: Int? = null,
    // 添加时间
    val dateAdded: Long = System.currentTimeMillis(),
    // 【新增字段】groupKey (必须与 MIGRATION_3_4 对应)
    val groupKey: String,

    /**
     * 元数据来源（必须与 MIGRATION_8_9 对应），取值见 [SOURCE_TMDB] / [SOURCE_DOUBAN]。
     *
     * 为什么需要它：[tmdbId] 这一列在不同来源里**不是同一个 id 空间** —— TMDB 的电影 id 与豆瓣的条目 id
     * 数值区间是重叠的。没有这一列，一部「豆瓣刮到的片子」和一部「TMDB 刮到的片子」只要数字撞上，
     * 就会被媒体库的分组查询当成同一部（`GROUP BY tmdbId`）、被选集/多版本查询串到一起。
     * 同时详情页在缓存未命中时会把 [tmdbId] 直接当 TMDB id 去联网，来源标记也是那里的刹车。
     */
    val source: String = SOURCE_TMDB,
) {
    // 转换为 UI 使用的 MediaItem
    // 假设你的 MediaItem 还没更新，这里暂时只传已有字段
    // 如果你也想在 MediaItem 里携带这些信息，请同步更新 MediaItem 数据类
    fun toMediaItem(): MediaItem {
        return MediaItem(
            id = tmdbId,
            title = if (mediaType == "tv" && episodeName != null) "$title S${seasonNumber}E${episodeNumber}" else title,
            overview = overview,
            posterPath = posterPath,
            releaseDate = releaseDate,
            isMovie = mediaType == "movie",
            seasonNumber = seasonNumber,
            episodeNumber = episodeNumber
            // 如果 MediaItem 也添加了这些字段，请在这里赋值
            // dataSourceType = dataSourceType,
            // fileName = fileName,
            // connectionName = connectionName
        )
    }

    companion object {
        /** 元数据来自 TMDB（或本地 NFO 里写的 TMDB id）。历史数据默认值也是它。 */
        const val SOURCE_TMDB = "tmdb"

        /** 元数据来自豆瓣兜底刮削。[tmdbId] 里存的是豆瓣条目 id。 */
        const val SOURCE_DOUBAN = "douban"
    }
}