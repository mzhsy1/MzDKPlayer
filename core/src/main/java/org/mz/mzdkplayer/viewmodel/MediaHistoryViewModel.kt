package org.mz.mzdkplayer.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.mz.mzdkplayer.data.local.MediaHistoryEntity
import org.mz.mzdkplayer.data.model.HistoryWithMetadata
import org.mz.mzdkplayer.data.model.MediaHistoryRecord
import org.mz.mzdkplayer.data.repository.RoomMediaHistoryRepository

class MediaHistoryViewModel(
    private val repository: RoomMediaHistoryRepository
) : ViewModel() {

    // 视频历史流 (StateFlow)
//    val videoHistory = repository.getVideoHistory()
//        .stateIn(
//            scope = viewModelScope,
//            started = SharingStarted.WhileSubscribed(5000),
//            initialValue = emptyList()
//        )
    /**
     * 保存历史记录
     * 这里的 launch 确保了即使 UI 销毁，ViewModel 作用域内的保存操作通常也能完成
     */
    fun saveHistory(record: MediaHistoryRecord) {
        viewModelScope.launch {
            repository.saveHistory(record)
        }
    }
    // 音频历史流
//    val audioHistory = repository.getAudioHistory()
//        .stateIn(
//            scope = viewModelScope,
//            started = SharingStarted.WhileSubscribed(5000),
//            initialValue = emptyList()
//        )
    // 【修改点 5】: 视频历史流 (返回 PagingData)
    val videoHistory: Flow<PagingData<HistoryWithMetadata>> =
        repository.getVideoHistoryPaged()
            .cachedIn(viewModelScope)

    // 【修改点 6】: 音频历史流 (返回 PagingData)
    val audioHistory: Flow<PagingData<MediaHistoryEntity>> =
        repository.getAudioHistoryPaged()
            .cachedIn(viewModelScope)
    fun deleteHistory(uri: String) {
        viewModelScope.launch {
            repository.deleteHistory(uri)
        }
    }
    /**
     * 【新增】获取历史播放位置
     * 这是一个 suspend 函数，必须在协程中调用
     */
    suspend fun getHistoryPosition(mediaUri: String): Long {
        return repository.getHistoryPosition(mediaUri)
    }
    fun clearAll() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }
}