package org.mz.mzdkplayer.ui.screen.vm

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.mz.mzdkplayer.data.model.Movie
import org.mz.mzdkplayer.data.model.MovieDetails
import org.mz.mzdkplayer.data.model.TVData
import org.mz.mzdkplayer.data.model.TVSeriesDetails
import org.mz.mzdkplayer.data.repository.Resource
import org.mz.mzdkplayer.data.repository.TmdbRepository

class MovieViewModel(private val repository: TmdbRepository) : ViewModel() {

    private val _popularMovies = MutableStateFlow<Resource<List<Movie>>>(Resource.Loading)
    val popularMovies: StateFlow<Resource<List<Movie>>> = _popularMovies

    private val _topRatedMovies = MutableStateFlow<Resource<List<Movie>>>(Resource.Loading)
    val topRatedMovies: StateFlow<Resource<List<Movie>>> = _topRatedMovies

    private val _searchResults = MutableStateFlow<Resource<List<Movie>>>(Resource.Loading)
    val searchResults: StateFlow<Resource<List<Movie>>> = _searchResults

    private val _movieDeResults = MutableStateFlow<Resource<MovieDetails>>(Resource.Loading)

    val movieDeResults: StateFlow<Resource<MovieDetails>> = _movieDeResults

    private val _tvSeriesResults = MutableStateFlow<Resource<TVSeriesDetails>>(Resource.Loading)
    val tvSeriesResults: StateFlow<Resource<TVSeriesDetails>> = _tvSeriesResults

    // 新增：当前焦点电影的搜索结果
    // 替换原来的 _focusedMovie
    private val _focusedMovie = MutableStateFlow<Resource<MediaItem?>>(Resource.Success(null))
    val focusedMovie: StateFlow<Resource<MediaItem?>> = _focusedMovie

    init {
        // loadPopularMovies()
        // 移除初始加载，按需加载
    }

    fun loadPopularMovies() {
        viewModelScope.launch {
            _popularMovies.value = Resource.Loading
            when (val result = repository.getPopularMovies()) {
                is Resource.Success -> _popularMovies.value = Resource.Success(result.data.results)
                is Resource.Error -> _popularMovies.value =
                    Resource.Error(result.message, result.exception)

                Resource.Loading -> {} // 不会执行到这里
            }
        }
    }

    fun loadTopRatedMovies() {
        viewModelScope.launch {
            _topRatedMovies.value = Resource.Loading
            when (val result = repository.getTopRatedMovies()) {
                is Resource.Success -> _topRatedMovies.value = Resource.Success(result.data.results)
                is Resource.Error -> _topRatedMovies.value =
                    Resource.Error(result.message, result.exception)

                Resource.Loading -> {}
            }
        }
    }

    fun searchMovies(query: String, year: String) {
        if (query.isBlank()) {
            _searchResults.value = Resource.Success(emptyList())
            return
        }

        viewModelScope.launch {
            _searchResults.value = Resource.Loading
            when (val result = repository.searchMovies(query = query, year = year)) {
                is Resource.Success -> _searchResults.value = Resource.Success(result.data.results)
                is Resource.Error -> _searchResults.value =
                    Resource.Error(result.message, result.exception)

                Resource.Loading -> {}
            }
        }
    }

    // 新增：搜索焦点电影（带防抖）
    private var currentSearchJob: kotlinx.coroutines.Job? = null

    fun searchFocusedMovie(movieName: String, isDirectory: Boolean) {
        // 如果是目录，清空电影信息
        if (isDirectory) {
            _focusedMovie.value = Resource.Success(null)
            return
        }

        // 取消之前的搜索任务
        currentSearchJob?.cancel()

        // 防抖延迟：500ms
        currentSearchJob = viewModelScope.launch {
            // 先显示加载状态
            _focusedMovie.value = Resource.Loading

            delay(800) // 防抖延迟

            // 清理文件名，移除扩展名和常见干扰字符
            //val (title, year) = extractTitleAndYear(movieName) // 重点：这里直接拆出标题和年份
            val mediaInfo = MediaInfoExtractorFormFileName.extract(movieName)

            if (mediaInfo.title.isBlank()) {
                _focusedMovie.value = Resource.Success(null)
                return@launch
            }
            Log.d(
                "MovieViewModel",
                "Cleaned title: ${mediaInfo.title}, Year: ${mediaInfo.year},S:${mediaInfo.season},E:${mediaInfo.episode}"
            )

            Log.d("MovieViewModel", "org movie: $movieName")
            // 根据 mediaType 选择搜索方法
            when (mediaInfo.mediaType) {
                "movie" -> {
                    when (val result =
                        repository.searchMovies(mediaInfo.title, year = mediaInfo.year)) {
                        is Resource.Success -> {
                            val movie = result.data.results.firstOrNull()
                            _focusedMovie.value = Resource.Success(movie?.toMediaItem())
                        }

                        is Resource.Error -> _focusedMovie.value =
                            Resource.Error(result.message, result.exception)

                        else -> {}
                    }
                }

                "tv" -> {
                    when (val result =
                        repository.searchTV(mediaInfo.title, year = mediaInfo.year)) {
                        is Resource.Success -> {
                            val tv = result.data.results.firstOrNull()
                            _focusedMovie.value = Resource.Success(tv?.toMediaItem())
                        }

                        is Resource.Error -> _focusedMovie.value =
                            Resource.Error(result.message, result.exception)

                        else -> {}
                    }
                }

                else -> _focusedMovie.value =
                    Resource.Error("Unsupported media type: ${mediaInfo.mediaType}")
            }
        }
    }

    fun getMovieDetails(movieId: Int) {
        _movieDeResults.value = Resource.Loading
        viewModelScope.launch {
            when (val result = repository.getMovieDetails(movieId = movieId)) {
                is Resource.Success -> {
                    val movie = result.data
                    _movieDeResults.value = Resource.Success(movie)
                }

                is Resource.Error -> _movieDeResults.value =
                    Resource.Error(result.message, result.exception)

                else -> {}
            }
        }

    }

    fun getTVSeriesDetails(seriesId: Int) {
        _tvSeriesResults.value = Resource.Loading
        viewModelScope.launch {
            when (val result = repository.getTVSeriesDetails(seriesId = seriesId)) {
                is Resource.Success -> {
                    val tvSeriesDetails = result.data
                    _tvSeriesResults.value = Resource.Success(tvSeriesDetails)
                }

                is Resource.Error -> _movieDeResults.value =
                    Resource.Error(result.message, result.exception)

                else -> {}
            }
        }

    }

    // 扩展函数：把 Movie/TvData 转成通用的 MediaItem
    private fun Movie.toMediaItem() = MediaItem(
        id = id,
        title = title ?: "",
        overview = overview,
        posterPath = posterPath,
        releaseDate = releaseDate
    )

    private fun TVData.toMediaItem() = MediaItem(
        id = id,
        title = name ?: "",
        overview = overview,
        posterPath = posterPath,
        releaseDate = firstAirDate // TV 的 releaseDate 实际是 first_air_date
    )

    // 辅助函数：检测字符串是否包含中文字符
    fun String.containsChinese(): Boolean {
        return this.any { it in '\u4e00'..'\u9fff' }
    }

    fun refreshAll() {
        loadPopularMovies()
        loadTopRatedMovies()
    }

    // 清空焦点电影信息
    fun clearFocusedMovie() {
        currentSearchJob?.cancel()
        _focusedMovie.value = Resource.Success(null)
    }
}


data class MediaItem(
    val id: Int = 0,
    val title: String?,
    val overview: String,
    val posterPath: String?,
    val releaseDate: String? // 通用字段：电影用 release_date，电视用 first_air_date
)