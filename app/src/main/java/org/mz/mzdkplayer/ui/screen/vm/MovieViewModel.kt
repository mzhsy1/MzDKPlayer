package org.mz.mzdkplayer.ui.screen.vm


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.mz.mzdkplayer.data.model.Movie
import org.mz.mzdkplayer.data.repository.Resource
import org.mz.mzdkplayer.data.repository.TmdbRepository

class MovieViewModel(private val repository: TmdbRepository) : ViewModel() {

    private val _popularMovies = MutableStateFlow<Resource<List<Movie>>>(Resource.Loading)
    val popularMovies: StateFlow<Resource<List<Movie>>> = _popularMovies

    private val _topRatedMovies = MutableStateFlow<Resource<List<Movie>>>(Resource.Loading)
    val topRatedMovies: StateFlow<Resource<List<Movie>>> = _topRatedMovies

    private val _searchResults = MutableStateFlow<Resource<List<Movie>>>(Resource.Loading)
    val searchResults: StateFlow<Resource<List<Movie>>> = _searchResults

    init {
        loadPopularMovies()
        loadTopRatedMovies()
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

    fun searchMovies(query: String) {
        if (query.isBlank()) {
            _searchResults.value = Resource.Success(emptyList())
            return
        }

        viewModelScope.launch {
            _searchResults.value = Resource.Loading
            when (val result = repository.searchMovies(query)) {
                is Resource.Success -> _searchResults.value = Resource.Success(result.data.results)
                is Resource.Error -> _searchResults.value = Resource.Error(result.message, result.exception)
                Resource.Loading -> {}
            }
        }
    }

    fun refreshAll() {
        loadPopularMovies()
        loadTopRatedMovies()
    }
}