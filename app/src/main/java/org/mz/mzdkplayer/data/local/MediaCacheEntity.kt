package org.mz.mzdkplayer.data.local



import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import org.mz.mzdkplayer.data.model.Genre
import org.mz.mzdkplayer.data.model.MediaItem


@Entity(tableName = "media_cache")
@TypeConverters(MediaConverters::class)
data class MediaCacheEntity(
    @PrimaryKey
    val videoUri: String, // 主键：文件的完整 SMB URI

    // 基础信息 (列表页显示)
    val tmdbId: Int,
    val mediaType: String, // "movie" or "tv"
    val title: String,
    val overview: String,
    val posterPath: String?,
    val backdropPath: String?,
    val releaseDate: String?, // 电影是 release_date, TV 是 first_air_date
    val voteAverage: Double,

    // TV 专属定位信息 (从文件名解析出来的)
    val seasonNumber: Int = 0,
    val episodeNumber: Int = 0,

    // 详细信息 (详情页加载后更新)
    val isDetailsLoaded: Boolean = false, // 标记是否已经加载过完整详情
    val status: String = "",
    val genres: List<Genre> = emptyList(),
    val originCountry: List<String> = emptyList(),

    // TV 详情专属
    val numberOfSeasons: Int? = 0,
    val numberOfEpisodes: Int? = 0,

    // TV 单集详情专属 (TVEpisode)
    val episodeName: String? = null,
    val episodeOverview: String? = null,
    val episodeStillPath: String? = null,
    val episodeAirDate: String? = null,
    val episodeRuntime: Int? = null
) {
    // 转换为 UI 使用的 MediaItem
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
        )
    }
}