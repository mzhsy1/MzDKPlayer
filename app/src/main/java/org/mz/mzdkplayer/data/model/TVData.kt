package org.mz.mzdkplayer.data.model

import com.google.gson.annotations.SerializedName

data class TVData(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String? = "",
    @SerializedName("overview") val overview: String,
    @SerializedName("poster_path") val posterPath: String?,
    @SerializedName("original_name") val originalName: String?,
    @SerializedName("backdrop_path") val backdropPath: String?,
    @SerializedName("vote_average") val voteAverage: Double,
    @SerializedName("first_air_date") val firstAirDate: String?
)

data class TVListResponse(
    @SerializedName("results") val results: List<TVData>,
    @SerializedName("page") val page: Int,
    @SerializedName("total_pages") val totalPages: Int,
    @SerializedName("total_results") val totalResults: Int
)