package org.mz.mzdkplayer.ui.screen.vm

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.mz.mzdkplayer.data.model.Movie
import org.mz.mzdkplayer.data.repository.Resource
import org.mz.mzdkplayer.data.repository.TmdbRepository
import java.time.Year

class MovieViewModel(private val repository: TmdbRepository) : ViewModel() {

    private val _popularMovies = MutableStateFlow<Resource<List<Movie>>>(Resource.Loading)
    val popularMovies: StateFlow<Resource<List<Movie>>> = _popularMovies

    private val _topRatedMovies = MutableStateFlow<Resource<List<Movie>>>(Resource.Loading)
    val topRatedMovies: StateFlow<Resource<List<Movie>>> = _topRatedMovies

    private val _searchResults = MutableStateFlow<Resource<List<Movie>>>(Resource.Loading)
    val searchResults: StateFlow<Resource<List<Movie>>> = _searchResults

    // 新增：当前焦点电影的搜索结果
    private val _focusedMovie = MutableStateFlow<Resource<Movie?>>(Resource.Success(null))
    val focusedMovie: StateFlow<Resource<Movie?>> = _focusedMovie

    init {
       // loadPopularMovies()
        // 移除初始加载，按需加载
    }

    fun loadPopularMovies() {
        viewModelScope.launch {
            _popularMovies.value = Resource.Loading
            when (val result = repository.getPopularMovies()) {
                is Resource.Success -> _popularMovies.value = Resource.Success(result.data.results)
                is Resource.Error -> _popularMovies.value = Resource.Error(result.message, result.exception)
                Resource.Loading -> {} // 不会执行到这里
            }
        }
    }

    fun loadTopRatedMovies() {
        viewModelScope.launch {
            _topRatedMovies.value = Resource.Loading
            when (val result = repository.getTopRatedMovies()) {
                is Resource.Success -> _topRatedMovies.value = Resource.Success(result.data.results)
                is Resource.Error -> _topRatedMovies.value = Resource.Error(result.message, result.exception)
                Resource.Loading -> {}
            }
        }
    }

    fun searchMovies(query: String,year: String) {
        if (query.isBlank()) {
            _searchResults.value = Resource.Success(emptyList())
            return
        }

        viewModelScope.launch {
            _searchResults.value = Resource.Loading
            when (val result = repository.searchMovies(query = query, year =year )) {
                is Resource.Success -> _searchResults.value = Resource.Success(result.data.results)
                is Resource.Error -> _searchResults.value = Resource.Error(result.message, result.exception)
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
            val (title, year) = extractTitleAndYear(movieName) // 重点：这里直接拆出标题和年份
            if (title.isBlank()) {
                _focusedMovie.value = Resource.Success(null)
                return@launch
            }
            Log.d("MovieViewModel", "Cleaned title: $title, Year: $year")

            Log.d("MovieViewModel", "org movie: $movieName")
            when (val result = repository.searchMovies(title, year = year)) {
                is Resource.Success -> {
                    Log.d("MovieViewModel", "Search results: ${result.data.results}")
                    // 取第一个结果作为焦点电影
                    val movie = result.data.results.firstOrNull()
                    _focusedMovie.value = Resource.Success(movie)
                }
                is Resource.Error -> {
                    _focusedMovie.value = Resource.Error(result.message, result.exception)
                }
                Resource.Loading -> {}
            }
        }
    }

    private fun extractTitleAndYear(movieName: String): Pair<String, String> {
        // 1. 移除扩展名（仅处理 .mkv，其他扩展名同理）
        val cleanName = movieName.replace(Regex("\\.mkv$"), "")

        // 2. 处理括号年份（如 "（2018）"）
        val yearFromParentheses = Regex("（(\\d{4})）").find(cleanName)?.groupValues?.get(1)
        if (yearFromParentheses != null) {
            val title = cleanName.replace(Regex("（\\d{4}）"), "").trim()
            return Pair(title.replace(".", " "), yearFromParentheses)
        }

        // 3. 分割文件名并扫描年份（从后往前找第一个四位数字）
        val parts = cleanName.split('.')
        var year: String? = null
        for (i in parts.indices.reversed()) {
            if (parts[i].matches(Regex("\\d{4}"))) {
                year = parts[i]
                break
            }
        }

        // 4. 提取标题（年份前的所有部分）
        if (year != null) {
            val titleParts = parts.slice(0 until parts.indexOf(year))
            val title = titleParts.joinToString(" ").replace(".", " ")

            // 5. 中文标题特殊处理：取第一个中文单词
            return if (title.containsChinese()) {
                Pair(title.split(' ')[0].trim(), year)
            } else {
                Pair(title, year)
            }
        }

        // 6. 无年份情况：返回原始文件名（无扩展名），年份为空
        return Pair(cleanName.replace(".", " ").trim(), "")
    }

    // 辅助函数：检测字符串是否包含中文字符
    private fun String.containsChinese(): Boolean {
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