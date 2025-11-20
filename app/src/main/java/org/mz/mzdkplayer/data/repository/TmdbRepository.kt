package org.mz.mzdkplayer.data.repository

import org.mz.mzdkplayer.data.api.TmdbApiService
import org.mz.mzdkplayer.data.api.TmdbServiceCreator
import org.mz.mzdkplayer.data.model.Movie
import retrofit2.Response
import retrofit2.http.Path


class TmdbRepository(private val apiService: TmdbApiService) {

    suspend fun getPopularMovies(page: Int = 1) = safeApiCall {
        apiService.getPopularMovies(page = page)
    }

    suspend fun getTopRatedMovies(page: Int = 1) = safeApiCall {
        apiService.getTopRatedMovies(page = page)
    }

    suspend fun searchMovies(query: String, page: Int = 1, year: String) = safeApiCall {
        apiService.searchMovies(query = query, page = page, year = year)
    }

    suspend fun searchTV(query: String, page: Int = 1, year: String) = safeApiCall {
        apiService.searchTV(query = query, page = page, year = year)
    }

    suspend fun getMovieDetails(movieId: Int) = safeApiCall {
        apiService.getMovieDetails(movieId = movieId)
    }

    suspend fun getTVSeriesDetails(seriesId: Int) = safeApiCall {
        apiService.getTVSeriesDetails(seriesId = seriesId)
    }

    suspend fun getTVEpisodeDetails(
        seriesId: Int,
        seasonNumber: Int,
        episodeNumber: Int
    ) = safeApiCall {
        apiService.getTVEpisodeDetails(
            seriesId = seriesId,
            seasonNumber = seasonNumber,
            episodeNumber = episodeNumber
        )
    }

    //  提取通用安全调用逻辑
    private suspend fun <T> safeApiCall(apiCall: suspend () -> Response<T>): Resource<T> {
        return try {
            val response = apiCall()
            if (response.isSuccessful && response.body() != null) {
                Resource.Success(response.body()!!)
            } else {
                Resource.Error("Request failed: ${response.code()}")
            }
        } catch (e: Exception) {
            Resource.Error("Network error: ${e.message}", e)
        }
    }

    companion object {
        // 单例：通过 ServiceCreator 创建
        val instance by lazy {
            TmdbRepository(TmdbServiceCreator.create<TmdbApiService>())
        }
    }
}