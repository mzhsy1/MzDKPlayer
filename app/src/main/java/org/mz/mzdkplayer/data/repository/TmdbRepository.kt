package org.mz.mzdkplayer.data.repository

import org.mz.mzdkplayer.data.api.TmdbApiService
import org.mz.mzdkplayer.data.api.TmdbServiceCreator
import retrofit2.Response


class TmdbRepository(private val apiService: TmdbApiService) {

    suspend fun getPopularMovies(page: Int = 1) = safeApiCall {
        apiService.getPopularMovies(page = page)
    }

    suspend fun getTopRatedMovies(page: Int = 1) = safeApiCall {
        apiService.getTopRatedMovies(page = page)
    }

    suspend fun searchMovies(query: String, page: Int = 1,year: String) = safeApiCall {
        apiService.searchMovies(query = query, page = page, year = year)
    }

    suspend fun searchTV(query: String, page: Int = 1,year: String) = safeApiCall {
        apiService.searchTV(query = query, page = page, year = year)
    }

    // 👇 提取通用安全调用逻辑
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