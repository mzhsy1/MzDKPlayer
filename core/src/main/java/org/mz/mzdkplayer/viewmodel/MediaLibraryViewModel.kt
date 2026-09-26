package org.mz.mzdkplayer.viewmodel



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
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import org.mz.mzdkplayer.data.local.MediaDao
import org.mz.mzdkplayer.data.local.MediaCacheEntity
import org.mz.mzdkplayer.data.local.MediaHistoryEntity
import org.mz.mzdkplayer.data.model.HistoryWithMetadata
import org.mz.mzdkplayer.data.repository.RoomMediaHistoryRepository

class MediaLibraryViewModel(
    private val mediaDao: MediaDao,
    private val historyRepository: RoomMediaHistoryRepository
) : ViewModel() {
    // 最近观看 (带元数据，用于主页展示，取前 20 条)
    val recentlyWatched: StateFlow<List<HistoryWithMetadata>> = historyRepository.getVideoHistory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 最近访问的文件记录 (最大 100 条)
    val recentlyAccessedFiles: StateFlow<List<MediaHistoryEntity>> = historyRepository.getRecentFiles()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 用于存储当前选中的电影版本列表（给弹窗用）
    private val _selectedMovieVersions = MutableStateFlow<List<MediaCacheEntity>>(emptyList())
    val selectedMovieVersions = _selectedMovieVersions.asStateFlow()
    // 电影分页数据流
    val pagedMovies: Flow<PagingData<MediaCacheEntity>> = Pager(
        config = PagingConfig(pageSize = 20, enablePlaceholders = false)
    ) {
        mediaDao.getMoviesPaged()
    }.flow.cachedIn(viewModelScope)
    // 加载特定电影的所有版本
    fun loadMovieVersions(tmdbId: Int) {
        viewModelScope.launch {
            _selectedMovieVersions.value = mediaDao.getMovieVersions(tmdbId)
        }
    }

    // 清空电影选中状态
    fun clearSelectedMovieVersions() {
        _selectedMovieVersions.value = emptyList()
    }
    // 电视剧分页数据流
    val pagedTVSeries: Flow<PagingData<MediaCacheEntity>> = Pager(
        config = PagingConfig(pageSize = 20, enablePlaceholders = false)
    ) {
        mediaDao.getTVSeriesPaged()
    }.flow.cachedIn(viewModelScope)

    // 最近添加
    val recentlyAdded: Flow<PagingData<MediaCacheEntity>> = Pager(
        config = PagingConfig(pageSize = 20, enablePlaceholders = false)
    ) {
        mediaDao.getRecentlyAddedPaged()
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