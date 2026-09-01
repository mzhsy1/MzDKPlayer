package org.mz.mzdkplayer.ui.screen.vm




import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import kotlinx.coroutines.ExperimentalCoroutinesApi

import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.mz.mzdkplayer.data.local.MediaCacheEntity
import org.mz.mzdkplayer.data.local.MediaDao
import kotlin.time.Duration.Companion.milliseconds

@OptIn(FlowPreview::class)

class SearchViewModel (
    private val mediaDao: MediaDao
) : ViewModel() {

    // --- 搜索相关 ---
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val searchResults: Flow<PagingData<MediaCacheEntity>> = _searchQuery
        .debounce(600.milliseconds)
        .flatMapLatest { query ->
            if (query.isBlank()) {
                flowOf(PagingData.empty())
            } else {
                Pager(
                    config = PagingConfig(pageSize = 20, enablePlaceholders = false),
                    pagingSourceFactory = { mediaDao.searchMediaPaged(query) }
                ).flow
            }
        }
        .cachedIn(viewModelScope)

    fun onSearchQueryChanged(query: String) {
        _searchQuery.update { query }
    }

    // --- 新增：TV 选集相关逻辑 ---

    // 存储当前选中剧集的所有分集
    private val _selectedSeriesEpisodes = MutableStateFlow<List<MediaCacheEntity>>(emptyList())
    val selectedSeriesEpisodes = _selectedSeriesEpisodes.asStateFlow()

    // 点击搜索结果中的 TV 时调用
    fun loadEpisodes(tmdbId: Int) {
        viewModelScope.launch {
            // 从数据库获取该剧的所有集数
            val episodes = mediaDao.getEpisodesForSeries(tmdbId)
            _selectedSeriesEpisodes.value = episodes
        }
    }

    // 关闭弹窗时清理数据
    fun clearSelectedEpisodes() {
        _selectedSeriesEpisodes.value = emptyList()
    }
}