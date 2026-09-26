package org.mz.mzdkplayer.data.local


import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface AudioDao {
    @Query("SELECT * FROM audio_cache WHERE audioUri = :uri LIMIT 1")
    suspend fun getAudioByUri(uri: String): AudioCacheEntity?

    // 修改点 1：将 REPLACE 改为 IGNORE，防止覆盖已有记录
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAudio(audio: AudioCacheEntity)

    // 修改点 2：新增批量插入支持，同样使用 IGNORE 策略
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(audioList: List<AudioCacheEntity>)

    // 列表页使用：按添加时间倒序
    @Query("SELECT * FROM audio_cache ORDER BY title ASC, artist ASC")
    fun getAllAudio(): Flow<List<AudioCacheEntity>>

    // 搜索功能 (可选)
    @Query("SELECT * FROM audio_cache WHERE title LIKE '%' || :query || '%' OR artist LIKE '%' || :query || '%'")
    suspend fun searchAudio(query: String): List<AudioCacheEntity>

    // 用于回写详细信息
    @Update
    suspend fun updateAudio(audio: AudioCacheEntity)

    @Query("DELETE FROM audio_cache")
    suspend fun clearAllAudio()

    // 新增：只更新从流中解析出的元数据
    @Query(
        """
        UPDATE audio_cache 
        SET title = :title, 
            artist = :artist, 
            album = :album, 
            duration = :duration, 
            lyrics = :lyrics, 
            localCoverPath = :localCoverPath,
            isDetailsLoaded = :isDetailsLoaded,
            bit = :bit,
            sampleRate = :sampleRate,
            bitsPerSample = :bitsPerSample
        WHERE audioUri = :uri
    """
    )
    suspend fun updateAudioMetadata(
        uri: String,
        title: String,
        artist: String,
        album: String,
        duration: Long,
        lyrics: String?,
        localCoverPath: String?,
        isDetailsLoaded: Boolean,
        bit: Long? = 0,
        sampleRate: String? = "",
        bitsPerSample: Int? = 16,
    )


}