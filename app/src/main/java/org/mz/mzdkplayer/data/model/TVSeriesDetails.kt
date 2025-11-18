package org.mz.mzdkplayer.data.model

import com.google.gson.annotations.SerializedName

data class TVSeriesDetails(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String? = "",
    @SerializedName("overview") val overview: String?="",
    @SerializedName("poster_path") val posterPath: String?,
    @SerializedName("original_name") val originalName: String?="",
    @SerializedName("backdrop_path") val backdropPath: String?,
    @SerializedName("vote_average") val voteAverage: Double,
    @SerializedName("first_air_date") val firstAirDate: String?,
    @SerializedName("last_air_date") val lastAirDate: String?,
    @SerializedName("origin_country") val originCountry: List<String>,
    @SerializedName("genres") val genreList: List<Genre>,
    @SerializedName("number_of_seasons") val numberOfSeasons: Int?=1,
    @SerializedName("number_of_episodes") val numberOfEpisodes:Int?=1,

)
