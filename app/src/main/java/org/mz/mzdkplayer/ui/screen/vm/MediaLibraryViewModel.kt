package org.mz.mzdkplayer.ui.screen.vm



import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.mz.mzdkplayer.data.local.MediaDao
import org.mz.mzdkplayer.data.local.MediaCacheEntity

class MediaLibraryViewModel(private val mediaDao: MediaDao) : ViewModel() {

    // 电影分页数据流
    val pagedMovies: Flow<PagingData<MediaCacheEntity>> = Pager(
        config = PagingConfig(pageSize = 20, enablePlaceholders = false)
    ) {
        mediaDao.getMoviesPaged()
    }.flow.cachedIn(viewModelScope)

    // 电视剧分页数据流
    val pagedTVSeries: Flow<PagingData<MediaCacheEntity>> = Pager(
        config = PagingConfig(pageSize = 20, enablePlaceholders = false)
    ) {
        mediaDao.getTVSeriesPaged()
    }.flow.cachedIn(viewModelScope)

    // 用于存储当前选中的剧集列表（给弹窗用）
    private val _selectedSeriesEpisodes = MutableStateFlow<List<MediaCacheEntity>>(emptyList())
    val selectedSeriesEpisodes = _selectedSeriesEpisodes.asStateFlow()

    // 加载特定剧集的集数列表
    fun loadEpisodes(tmdbId: Int) {
        viewModelScope.launch {
            _selectedSeriesEpisodes.value = mediaDao.getEpisodesForSeries(tmdbId)
        }
    }

    // 清空选中状态
    fun clearSelectedEpisodes() {
        _selectedSeriesEpisodes.value = emptyList()
    }
}