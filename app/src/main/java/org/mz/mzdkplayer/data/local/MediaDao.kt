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

    // 1. 分页获取电影
    @Query("SELECT * FROM media_cache WHERE mediaType = 'movie' ORDER BY title ASC")
    fun getMoviesPaged(): androidx.paging.PagingSource<Int, MediaCacheEntity>

    // 2. 分页获取电视剧（按 tmdbId 分组，确保一部剧只显示一张卡片）
    @Query("SELECT * FROM media_cache WHERE mediaType = 'tv' GROUP BY tmdbId ORDER BY title ASC")
    fun getTVSeriesPaged(): androidx.paging.PagingSource<Int, MediaCacheEntity>

    // 3. 获取某部剧集下的所有本地集数 (用于弹窗选集)
    @Query("SELECT * FROM media_cache WHERE mediaType = 'tv' AND tmdbId = :tmdbId ORDER BY seasonNumber ASC, episodeNumber ASC")
    suspend fun getEpisodesForSeries(tmdbId: Int): List<MediaCacheEntity>

    /**
     * 【新增】清理资料库功能：删除 media_cache 表中所有记录
     */
    @Query("DELETE FROM media_cache")
    suspend fun clearAllMediaCache()
}