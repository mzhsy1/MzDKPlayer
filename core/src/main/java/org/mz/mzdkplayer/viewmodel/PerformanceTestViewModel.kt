package org.mz.mzdkplayer.viewmodel

// PerformanceTestViewModel.kt



import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.mz.mzdkplayer.data.local.MediaCacheEntity
import org.mz.mzdkplayer.data.local.MediaDao

import kotlin.random.Random

import kotlin.time.measureTime


class PerformanceTestViewModel (
    private val mediaDao: MediaDao
) : ViewModel() {

    private val _status = MutableStateFlow<String>("准备就绪，当前数据：未知")
    val status = _status.asStateFlow()

    private val _isInserting = MutableStateFlow(false)
    val isInserting = _isInserting.asStateFlow()

    // 基础标题库，用于生成模糊搜索的关键词
    private val baseTitles = listOf("星球", "复仇", "速度", "魔法", "异形", "代号", "战争", "末日", "潜伏", "爱情")

    init {
        // 启动时查询一下当前数据量
        // 【注意】需要你在 MediaDao 中添加一个 `getMediaCount(): Int` 方法才能显示准确数量
        _status.update { "准备就绪，请点击按钮开始插入" }
    }

    /**
     * 开始插入模拟数据
     * @param count 要插入的总条数 (默认为 50000)
     */
    fun startInsertion(count: Int = 50000) {
        if (_isInserting.value) return

        viewModelScope.launch {
            _isInserting.value = true
            _status.value = "开始生成 $count 条数据..."

            val totalTime = measureTime {
                // 1. 生成数据
                val dummyData = generateDummyMedia(count)
                _status.value = "数据生成完成，共 ${dummyData.size} 条，准备插入数据库..."

                // 2. 批量插入数据库
                val insertionTime = measureTime {
                    // 确保在大事务中运行，以优化性能
                    withContext(Dispatchers.IO) {
                        mediaDao.insertAll(dummyData)
                    }
                }

            }

            _isInserting.value = false
        }
    }

    /**
     * 清理数据库中的所有媒体缓存
     */
    fun clearDatabase() {
        if (_isInserting.value) return
        viewModelScope.launch(Dispatchers.IO) {
            _status.value = "正在清理数据库..."
            mediaDao.clearAllMediaCache()
            _status.value = "数据库清理完成。"
        }
    }

    /**
     * 模拟生成 MediaCacheEntity 列表
     */
    private fun generateDummyMedia(count: Int): List<MediaCacheEntity> {
        val list = mutableListOf<MediaCacheEntity>()
        val movieCount = count / 2

        // 确保 tmdbId 不会冲突
        val movieTmdbIdStart = 1000000
        val tvTmdbIdStart = 2000000
        val maxSeriesId = 10000 // 假设有 10000 部不同的剧

        // --- 1. 电影数据 (一半) ---
        for (i in 0 until movieCount) {
            val tmdbId = movieTmdbIdStart + i
            val titleIndex = i % baseTitles.size
            // 确保文件名和标题都包含一些可搜索的关键词
            val title = "电影：${baseTitles[titleIndex]}${i}号代号 ${i % 100}"
            val year = (2000 + Random.nextInt(25)).toString()
            val videoUri = "smb://nas/movie/media_uri_$i.mp4"

            list.add(
                MediaCacheEntity(
                    videoUri = videoUri,
                    dataSourceType = "SMB",
                    fileName = "media_${baseTitles[titleIndex]}$i.mp4",
                    connectionName = "TestNas",
                    tmdbId = tmdbId,
                    mediaType = "movie",
                    title = title,
                    overview = "电影简介 $i",
                    posterPath = null,
                    backdropPath = null,
                    releaseDate = "$year-01-01",
                    voteAverage = Random.nextDouble(5.0, 10.0),
                    groupKey = "movie_$videoUri"
                )
            )
        }

        // --- 2. TV 剧集数据 (另一半) ---
        var tvIndex = 0
        while (tvIndex < count - movieCount) {
            val seriesTmdbId = tvTmdbIdStart + Random.nextInt(maxSeriesId)
            val seriesTitleIndex = seriesTmdbId % baseTitles.size
            val seriesTitle = "剧集：${baseTitles[seriesTitleIndex]}之${seriesTmdbId % 100}"
            val maxEpisodes = Random.nextInt(1, 100) // 随机集数
            val season = Random.nextInt(1, 4) // 随机季数

            for (episodeNum in 1..maxEpisodes) {
                if (tvIndex >= count - movieCount) break

                val videoUri = "smb://nas/tv/${seriesTmdbId}/S${season}E${episodeNum}.mkv"

                list.add(
                    MediaCacheEntity(
                        videoUri = videoUri,
                        dataSourceType = "SMB",
                        fileName = "S${season}E${episodeNum}_${seriesTitle}.mkv",
                        connectionName = "TestNas",
                        tmdbId = seriesTmdbId,
                        mediaType = "tv",
                        title = seriesTitle,
                        overview = "剧集简介 $seriesTmdbId",
                        posterPath = null,
                        backdropPath = null,
                        releaseDate = null,
                        voteAverage = Random.nextDouble(5.0, 10.0),
                        seasonNumber = season,
                        episodeNumber = episodeNum,
                        episodeName = "第${episodeNum}集：命运的转折点",
                        groupKey = "tv_$seriesTmdbId"
                    )
                )
                tvIndex++
            }
        }

        return list.shuffled()
    }
}