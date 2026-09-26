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

    /**
     * 一次取回多个播放地址的刮削记录（手机端目录列表批量刷新海报/标题用）。
     *
     * 调用方需保证 [uris] **非空**：SQLite 不认 `IN ()` 这种空集合语法。
     */
    @Query("SELECT * FROM media_cache WHERE videoUri IN (:uris)")
    suspend fun getMediaByUris(uris: List<String>): List<MediaCacheEntity>

    // 插入新记录 (如果存在则替换，适用于搜索结果更新)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMedia(media: MediaCacheEntity)

    // 更新记录 (用于详情页回填数据)
    @Update
    suspend fun updateMedia(media: MediaCacheEntity)

    // 1. 分页获取电影 (修改：增加 GROUP BY tmdbId)
    // 原来：@Query("SELECT * FROM media_cache WHERE mediaType = 'movie' ORDER BY title ASC")
    @Query("SELECT * FROM media_cache WHERE mediaType = 'movie' GROUP BY tmdbId ORDER BY title ASC")
    fun getMoviesPaged(): androidx.paging.PagingSource<Int, MediaCacheEntity>

    // ... (其他代码保持不变)

    // 【新增】获取某部电影的所有版本 (用于弹窗选择不同画质/来源)
    @Query("SELECT * FROM media_cache WHERE mediaType = 'movie' AND tmdbId = :tmdbId")
    suspend fun getMovieVersions(tmdbId: Int): List<MediaCacheEntity>

    // 2. 分页获取电视剧（按 tmdbId 分组，确保一部剧只显示一张卡片）
    @Query("SELECT * FROM media_cache WHERE mediaType = 'tv' GROUP BY tmdbId ORDER BY title ASC")
    fun getTVSeriesPaged(): androidx.paging.PagingSource<Int, MediaCacheEntity>

    // 【新增】获取最近添加的媒体 (不区分电影电视，按 tmdbId 分组)
    @Query("SELECT * FROM media_cache GROUP BY groupKey ORDER BY dateAdded DESC")
    fun getRecentlyAddedPaged(): androidx.paging.PagingSource<Int, MediaCacheEntity>

    // 3. 获取某部剧集下的所有本地集数 (用于弹窗选集)
    @Query("SELECT * FROM media_cache WHERE mediaType = 'tv' AND tmdbId = :tmdbId ORDER BY seasonNumber ASC, episodeNumber ASC")
    suspend fun getEpisodesForSeries(tmdbId: Int): List<MediaCacheEntity>

    /**
     * 【新增】清理资料库功能：删除 media_cache 表中所有记录
     */
    @Query("DELETE FROM media_cache")
    suspend fun clearAllMediaCache()

    /**
     * 【新增】模糊搜索功能
     * 1. 同时也搜索 title 和 fileName
     * 2. 针对 TV 类型，按 tmdbId 分组，避免搜索结果出现几十集同一部剧
     * 3. 如果你有拼音字段，可以在 WHERE 中加入 OR pinyin LIKE ...
     */
//    @Query("""
//        SELECT * FROM media_cache
//        WHERE (title LIKE '%' || :query || '%' OR fileName LIKE '%' || :query || '%')
//        GROUP BY (CASE WHEN mediaType = 'tv' THEN tmdbId ELSE videoUri END)
//        ORDER BY
//            CASE WHEN title LIKE :query || '%' THEN 1 ELSE 2 END, --以此开头优先
//            title ASC
//    """)
    /**
     * 【新增】模糊搜索功能（性能优化版）
     * 1. 使用 groupKey 分组，性能远高于 CASE WHEN 分组。
     * 2. LIKE '%%' 依然是全表扫描，但配合防抖和索引能保证可接受的性能。
     */
    @Query("""
        SELECT * FROM media_cache 
        WHERE (title LIKE '%' || :query || '%' OR fileName LIKE '%' || :query || '%')
        GROUP BY groupKey -- 【优化点】直接使用预计算的 groupKey 字段进行分组
        ORDER BY 
            CASE WHEN title LIKE :query || '%' THEN 1 ELSE 2 END, -- 以关键词开头的优先
            title ASC
    """)
    fun searchMediaPaged(query: String): androidx.paging.PagingSource<Int, MediaCacheEntity>

    /**
     * 【新增】批量插入，用于性能测试。
     * 使用 REPLACE 策略，避免重复插入时报错。
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(mediaList: List<MediaCacheEntity>)
}