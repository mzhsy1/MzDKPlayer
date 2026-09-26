package org.mz.mzdkplayer.data.model

import com.google.gson.annotations.SerializedName

data class MovieDetails(
    @SerializedName("id") val id: Int,
    @SerializedName("title") val title: String? = "",
    @SerializedName("status") val status: String,
    @SerializedName("original_title") val originalTitle: String? = "",
    @SerializedName("overview") val overview: String,
    @SerializedName("poster_path") val posterPath: String?,
    @SerializedName("backdrop_path") val backdropPath: String?,
    @SerializedName("vote_average") val voteAverage: Double,
    @SerializedName("release_date") val releaseDate: String?,
    @SerializedName("origin_country") val originCountry: List<String>,
    @SerializedName("genres") val genreList: List<Genre>
)

/**
 * 电影类型
 */
data class Genre(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String
)
