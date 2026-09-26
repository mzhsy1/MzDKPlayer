package org.mz.mzdkplayer.data.repository

import android.util.Log
import org.mz.mzdkplayer.data.api.TmdbApiService
import org.mz.mzdkplayer.data.api.TmdbServiceCreator
import org.mz.mzdkplayer.data.model.Movie
import retrofit2.Response
import retrofit2.http.Path


class TmdbRepository(private val apiService: TmdbApiService) {

    private fun getSearchLanguage(): String {
        val pref = SettingsRepository.tmdbSearchLang
        return if (pref.isEmpty()) java.util.Locale.getDefault().toLanguageTag() else pref
    }

    private fun getResultLanguage(): String {
        val pref = SettingsRepository.tmdbResultLang
        return if (pref.isEmpty()) java.util.Locale.getDefault().toLanguageTag() else pref
    }

    suspend fun getPopularMovies(page: Int = 1) = safeApiCall {
        apiService.getPopularMovies(page = page, language = getResultLanguage())
    }

    suspend fun getTopRatedMovies(page: Int = 1) = safeApiCall {
        apiService.getTopRatedMovies(page = page, language = getResultLanguage())
    }

    suspend fun searchMovies(query: String, page: Int = 1, year: String) = safeApiCall {
        apiService.searchMovies(query = query, page = page, year = year, language = getSearchLanguage())
    }

    suspend fun searchTV(query: String, page: Int = 1, year: String) = safeApiCall {
        apiService.searchTV(query = query, page = page, year = year, language = getSearchLanguage())
    }

    suspend fun getMovieDetails(movieId: Int) = safeApiCall {
        apiService.getMovieDetails(movieId = movieId, language = getResultLanguage())
    }

    suspend fun getTVSeriesDetails(seriesId: Int) = safeApiCall {
        apiService.getTVSeriesDetails(seriesId = seriesId, language = getResultLanguage())
    }

    suspend fun getTVEpisodeDetails(
        seriesId: Int,
        seasonNumber: Int,
        episodeNumber: Int
    ) = safeApiCall {
        apiService.getTVEpisodeDetails(
            seriesId = seriesId,
            seasonNumber = seasonNumber,
            episodeNumber = episodeNumber,
            language = getResultLanguage()
        )
    }

    //  提取通用安全调用逻辑
    private suspend fun <T> safeApiCall(apiCall: suspend () -> Response<T>): Resource<T> {
        return try {
            val response = apiCall()
            if (response.isSuccessful && response.body() != null) {
                Resource.Success(response.body()!!)
            } else {
                Log.e("TmdbRepository", "Request failed: ${response.code()} ${response.message()} - ${response.errorBody()?.string()}")
                Resource.Error("Request failed: ${response.code()}")
            }
        } catch (e: Exception) {
            Log.e("TmdbRepository", "Network error: ${e.message}", e)
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