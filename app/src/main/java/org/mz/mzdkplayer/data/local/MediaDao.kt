package org.mz.mzdkplayer.data.local



import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface MediaDao {
    // 根据 URI 查询缓存
    @Query("SELECT * FROM media_cache WHERE videoUri = :uri LIMIT 1")
    suspend fun getMediaByUri(uri: String): MediaCacheEntity?

    // 插入新记录 (如果存在则替换，适用于搜索结果更新)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMedia(media: MediaCacheEntity)

    // 更新记录 (用于详情页回填数据)
    @Update
    suspend fun updateMedia(media: MediaCacheEntity)
}