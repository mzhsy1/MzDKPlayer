package org.mz.mzdkplayer.data.local


import androidx.paging.PagingSource
import androidx.room.*
import kotlinx.coroutines.flow.Flow
import org.mz.mzdkplayer.data.model.HistoryWithMetadata

@Dao
interface MediaHistoryDao {

    // 插入或更新历史记录
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(history: MediaHistoryEntity)

    // 获取所有视频历史，并关联元数据，按时间倒序
    @Transaction
    @Query("SELECT * FROM media_history WHERE mediaType = 'VIDEO' ORDER BY timestamp DESC")
    fun getVideoHistoryWithMetadata(): Flow<List<HistoryWithMetadata>>

    // 获取最近访问的文件记录（最大 100 条）
    @Query("SELECT * FROM media_history ORDER BY timestamp DESC LIMIT 100")
    fun getRecentFiles(): Flow<List<MediaHistoryEntity>>

    // 获取所有音频历史 (音频通常没有 metadata，直接查实体即可)
    @Query("SELECT * FROM media_history WHERE mediaType = 'AUDIO' ORDER BY timestamp DESC")
    fun getAudioHistory(): Flow<List<MediaHistoryEntity>>
    // 【修改点 1】: 视频历史记录分页源
    @Transaction
    @Query("SELECT * FROM media_history WHERE mediaType = 'VIDEO' ORDER BY timestamp DESC")
    fun getVideoHistoryPagingSource(): PagingSource<Int, HistoryWithMetadata>

    // 【修改点 2】: 音频历史记录分页源
    @Query("SELECT * FROM media_history WHERE mediaType = 'AUDIO' ORDER BY timestamp DESC")
    fun getAudioHistoryPagingSource(): PagingSource<Int, MediaHistoryEntity>
    // 删除单条
    @Query("DELETE FROM media_history WHERE mediaUri = :uri")
    suspend fun deleteHistoryByUri(uri: String)

    // 清空历史
    @Query("DELETE FROM media_history")
    suspend fun clearAllHistory()

    // 获取单条历史（用于断点续播）
    @Query("SELECT * FROM media_history WHERE mediaUri = :uri LIMIT 1")
    suspend fun getHistoryByUri(uri: String): MediaHistoryEntity?
}