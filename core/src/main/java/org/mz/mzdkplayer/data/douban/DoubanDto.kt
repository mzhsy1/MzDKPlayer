package org.mz.mzdkplayer.data.douban

import com.google.gson.annotations.SerializedName

/**
 * 豆瓣接口的原始响应结构（字段名即实测返回的原始字段，见 `DoubanScraper` 的注释）。
 * 全部 `internal`：只有 `DoubanScraper`/`DoubanMapper` 和同模块的单测会用到。
 */

/** `j/subject_suggest` 数组里的一个元素 */
internal data class DoubanSuggestDto(
    @SerializedName("id") val id: String? = null,
    @SerializedName("title") val title: String? = null,
    @SerializedName("sub_title") val subTitle: String? = null,
    @SerializedName("year") val year: String? = null,
    @SerializedName("type") val type: String? = null,
    @SerializedName("img") val img: String? = null,
    @SerializedName("episode") val episode: String? = null,
    @SerializedName("url") val url: String? = null,
)

/** rexxar `{movie|tv}/{id}` 详情 */
internal data class DoubanSubjectDto(
    @SerializedName("id") val id: String? = null,
    @SerializedName("title") val title: String? = null,
    @SerializedName("original_title") val originalTitle: String? = null,
    @SerializedName("year") val year: String? = null,
    @SerializedName("subtype") val subtype: String? = null,
    @SerializedName("is_tv") val isTv: Boolean? = null,
    @SerializedName("intro") val intro: String? = null,
    @SerializedName("rating") val rating: DoubanRatingDto? = null,
    @SerializedName("genres") val genres: List<String>? = null,
    @SerializedName("countries") val countries: List<String>? = null,
    @SerializedName("languages") val languages: List<String>? = null,
    @SerializedName("durations") val durations: List<String>? = null,
    @SerializedName("pubdate") val pubdate: List<String>? = null,
    @SerializedName("release_date") val releaseDate: String? = null,
    @SerializedName("pic") val pic: DoubanPicDto? = null,
    @SerializedName("cover_url") val coverUrl: String? = null,
    @SerializedName("card_subtitle") val cardSubtitle: String? = null,
    @SerializedName("aka") val aka: List<String>? = null,
    @SerializedName("directors") val directors: List<DoubanCreditDto>? = null,
    @SerializedName("actors") val actors: List<DoubanCreditDto>? = null,
    @SerializedName("episodes_count") val episodesCount: Int? = null,
    @SerializedName("last_episode_number") val lastEpisodeNumber: Int? = null,
    @SerializedName("comment_count") val commentCount: Int? = null,
)

internal data class DoubanRatingDto(
    @SerializedName("value") val value: Double? = null,
    @SerializedName("max") val max: Double? = null,
    @SerializedName("count") val count: Int? = null,
    @SerializedName("star_count") val starCount: Double? = null,
)

internal data class DoubanPicDto(
    @SerializedName("normal") val normal: String? = null,
    @SerializedName("large") val large: String? = null,
)

internal data class DoubanCreditDto(
    @SerializedName("id") val id: String? = null,
    @SerializedName("name") val name: String? = null,
)

/** rexxar `{movie|tv}/{id}/photos` 详情 */
internal data class DoubanPhotosDto(
    @SerializedName("total") val total: Int? = null,
    @SerializedName("photos") val photos: List<DoubanPhotoDto>? = null,
)

internal data class DoubanPhotoDto(
    @SerializedName("id") val id: String? = null,
    @SerializedName("image") val image: DoubanImageDto? = null,
)

internal data class DoubanImageDto(
    @SerializedName("large") val large: DoubanImageSizeDto? = null,
    @SerializedName("normal") val normal: DoubanImageSizeDto? = null,
)

internal data class DoubanImageSizeDto(
    @SerializedName("url") val url: String? = null,
    @SerializedName("width") val width: Int? = null,
    @SerializedName("height") val height: Int? = null,
)
