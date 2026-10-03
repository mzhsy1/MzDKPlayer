package org.mz.mzdkplayer.data.repository

import android.util.Log
import kotlinx.coroutines.CancellationException
import org.mz.mzdkplayer.data.api.TmdbApiService
import org.mz.mzdkplayer.data.api.TmdbServiceCreator
import org.mz.mzdkplayer.data.model.Movie
import retrofit2.Response
import retrofit2.http.Path


class TmdbRepository(private val apiService: TmdbApiService) {

    /**
     * 统一语言：搜索与详情都用设置里的「TMDB 详情语言」，空值跟随系统。
     *
     * 原来搜索走的是另一条 `tmdbSearchLang`，导致「修改文件对应影视信息」里搜出来的行
     * 和点进去抓到的详情语言不一致，看起来就是「详情语言没生效」。现在只有一个入口。
     */
    private fun getLanguage(): String {
        val pref = SettingsRepository.tmdbResultLang
        return if (pref.isEmpty()) java.util.Locale.getDefault().toLanguageTag() else pref
    }

    suspend fun getPopularMovies(page: Int = 1) = safeApiCall {
        apiService.getPopularMovies(page = page, language = getLanguage())
    }

    suspend fun getTopRatedMovies(page: Int = 1) = safeApiCall {
        apiService.getTopRatedMovies(page = page, language = getLanguage())
    }

    suspend fun searchMovies(query: String, page: Int = 1, year: String) = safeApiCall {
        apiService.searchMovies(query = query, page = page, year = year, language = getLanguage())
    }

    suspend fun searchTV(query: String, page: Int = 1, year: String) = safeApiCall {
        apiService.searchTV(query = query, page = page, year = year, language = getLanguage())
    }

    suspend fun getMovieDetails(movieId: Int) = safeApiCall {
        apiService.getMovieDetails(movieId = movieId, language = getLanguage())
    }

    suspend fun getTVSeriesDetails(seriesId: Int) = safeApiCall {
        apiService.getTVSeriesDetails(seriesId = seriesId, language = getLanguage())
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
            language = getLanguage()
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
        } catch (e: CancellationException) {
            // 协程被取消（退出页面 / 重启批量）不是「网络错误」：必须透传，
            // 否则取消之后调用方会继续按「这个源失败了」往下走
            throw e
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