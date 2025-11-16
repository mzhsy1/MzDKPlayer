package org.mz.mzdkplayer.data.api




import org.mz.mzdkplayer.data.model.MovieListResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface TmdbApiService {

    @GET("movie/popular")
    suspend fun getPopularMovies(
        @Query("language") language: String = "zh-CN",
        @Query("page") page: Int = 1
    ): Response<MovieListResponse>

    @GET("movie/top_rated")
    suspend fun getTopRatedMovies(
        @Query("language") language: String = "en-US",
        @Query("page") page: Int = 1
    ): Response<MovieListResponse>

    @GET("search/movie")
    suspend fun searchMovies(
        @Query("query") query: String,
        @Query("language") language: String = "zh-CN",
        @Query(value = "year") year: String = "",
        @Query("page") page: Int = 1
    ): Response<MovieListResponse>
}