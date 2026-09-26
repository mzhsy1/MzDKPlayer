package org.mz.mzdkplayer.data.repository


import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import kotlinx.coroutines.flow.Flow
import org.mz.mzdkplayer.data.local.MediaHistoryDao
import org.mz.mzdkplayer.data.local.MediaHistoryEntity
import org.mz.mzdkplayer.data.model.HistoryWithMetadata
import org.mz.mzdkplayer.data.model.MediaHistoryRecord

class RoomMediaHistoryRepository(private val historyDao: MediaHistoryDao) {

    // 分页配置
    private val pagingConfig= PagingConfig(
        pageSize = 20,           // 每页加载 20 条记录
        enablePlaceholders = false // TV 端通常不需要占位符
    )
    // 保存历史 (接收旧的 MediaHistoryRecord 对象以兼容现有代码，或者你直接改用 Entity)
    suspend fun saveHistory(record: MediaHistoryRecord) {
        val entity = MediaHistoryEntity(
            mediaUri = record.mediaUri,
            fileName = record.fileName,
            playbackPosition = record.playbackPosition,
            mediaDuration = record.mediaDuration,
            protocolName = record.protocolName,
            connectionName = record.connectionName,
            serverAddress = record.serverAddress,
            timestamp = System.currentTimeMillis(),
            mediaType = record.mediaType
        )
        historyDao.insertHistory(entity)
    }
    // 【修改点 3】: 返回 PagingData 的 Flow
    fun getVideoHistoryPaged(): Flow<PagingData<HistoryWithMetadata>> {
        return Pager(
            config = pagingConfig,
            pagingSourceFactory = { historyDao.getVideoHistoryPagingSource() } // 使用 DAO 返回的分页源
        ).flow
    }
    // 【修改点 4】: 音频历史记录分页
    fun getAudioHistoryPaged(): Flow<PagingData<MediaHistoryEntity>> {
        return Pager(
            config = pagingConfig,
            pagingSourceFactory = { historyDao.getAudioHistoryPagingSource() }
        ).flow
    }
    // 获取视频历史（带元数据）
    fun getVideoHistory(): Flow<List<HistoryWithMetadata>> {
        return historyDao.getVideoHistoryWithMetadata()
    }

    // 获取最近访问的文件记录（最大 100 条）
    fun getRecentFiles(): Flow<List<MediaHistoryEntity>> {
        return historyDao.getRecentFiles()
    }

    // 获取音频历史
    fun getAudioHistory(): Flow<List<MediaHistoryEntity>> {
        return historyDao.getAudioHistory()
    }

    suspend fun deleteHistory(uri: String) {
        historyDao.deleteHistoryByUri(uri)
    }

    suspend fun clearHistory() {
        historyDao.clearAllHistory()
    }

    // 供播放器查询断点
    suspend fun getHistoryPosition(uri: String): Long {
        return historyDao.getHistoryByUri(uri)?.playbackPosition ?: 0L
    }
}