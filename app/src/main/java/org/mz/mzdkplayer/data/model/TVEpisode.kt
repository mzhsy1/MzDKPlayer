package org.mz.mzdkplayer.data.model

import com.google.gson.annotations.SerializedName



/**
 * 表示电视剧某一集的基本信息。
 *
 * 该数据类用于解析来自影视数据库（如 TMDb）的单集数据，
 * 包含播出日期、集数、标题、简介、评分等元数据。
 *
 * @property airDate 首播日期，格式为 "YYYY-MM-DD"
 * @property episodeNumber 当前季中的集号（从 1 开始）
 * @property episodeType 集类型，例如 "standard"（标准集）
 * @property name 集的标题（中文或原始语言）
 * @property overview 集的剧情简介
 * @property id 该集在数据库中的唯一标识符
 * @property productionCode 制作编号（通常为空）
 * @property runtime 集的时长（以分钟为单位），可能为 null
 * @property seasonNumber 所属季的编号（第几季）
 * @property stillPath 该集的静态画面（截图）路径，相对于图片基础 URL，可能为 null
 * @property voteAverage 用户平均评分（满分 10 分）
 * @property voteCount 参与评分的用户数量
 */
data class TVEpisode(
    @SerializedName("air_date") val airDate: String,
    @SerializedName("episode_number") val episodeNumber: Int,
    @SerializedName("episode_type") val episodeType: String,
    @SerializedName("name") val name: String,
    @SerializedName("overview") val overview: String,
    @SerializedName("id") val id: Int,
    @SerializedName("production_code") val productionCode: String,
    @SerializedName("runtime") val runtime: Int?,
    @SerializedName("season_number") val seasonNumber: Int,
    @SerializedName("still_path") val stillPath: String?,
    @SerializedName("vote_average") val voteAverage: Double,
    @SerializedName("vote_count") val voteCount: Int
)