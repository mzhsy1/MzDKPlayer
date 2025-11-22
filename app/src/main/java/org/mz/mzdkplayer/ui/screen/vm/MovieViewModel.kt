package org.mz.mzdkplayer.ui.screen.vm

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.mz.mzdkplayer.data.local.MediaCacheEntity
import org.mz.mzdkplayer.data.local.MediaDao
import org.mz.mzdkplayer.data.model.MediaItem
import org.mz.mzdkplayer.data.model.Movie
import org.mz.mzdkplayer.data.model.MovieDetails
import org.mz.mzdkplayer.data.model.TVEpisode
import org.mz.mzdkplayer.data.model.TVSeriesDetails
import org.mz.mzdkplayer.data.repository.Resource
import org.mz.mzdkplayer.data.repository.TmdbRepository

class MovieViewModel(private val repository: TmdbRepository,private val mediaDao: MediaDao) : ViewModel() {

    private val _popularMovies = MutableStateFlow<Resource<List<Movie>>>(Resource.Loading)
    val popularMovies: StateFlow<Resource<List<Movie>>> = _popularMovies

    private val _topRatedMovies = MutableStateFlow<Resource<List<Movie>>>(Resource.Loading)
    val topRatedMovies: StateFlow<Resource<List<Movie>>> = _topRatedMovies

    private val _searchResults = MutableStateFlow<Resource<List<Movie>>>(Resource.Loading)
    val searchResults: StateFlow<Resource<List<Movie>>> = _searchResults

    private val _movieDeResults = MutableStateFlow<Resource<MovieDetails>>(Resource.Loading)

    val movieDeResults: StateFlow<Resource<MovieDetails>> = _movieDeResults

    private val _tvSeriesResults = MutableStateFlow<Resource<TVSeriesDetails>>(Resource.Loading)
    val tvSeriesResults: StateFlow<Resource<TVSeriesDetails>> = _tvSeriesResults

    private val _tvEpisodeResults = MutableStateFlow<Resource<TVEpisode>>(Resource.Loading)
    val tvEpisodeResults: StateFlow<Resource<TVEpisode>> = _tvEpisodeResults

    // 新增：当前焦点电影的搜索结果
    // 替换原来的 _focusedMovie
    private val _focusedMovie = MutableStateFlow<Resource<MediaItem?>>(Resource.Success(null))
    val focusedMovie: StateFlow<Resource<MediaItem?>> = _focusedMovie



    // 搜索任务 Job
    private var currentSearchJob: Job? = null

    /**
     * [修改] 搜索焦点电影/剧集 (带缓存)
     * 增加了 dataSourceType, connectionName 参数以便存入数据库
     */
    fun searchFocusedMovie(
        movieName: String?,
        isDirectory: Boolean,
        videoUri: String,
        dataSourceType: String, // 新增
        connectionName: String  // 新增
    )
    {
        if (isDirectory) {
            _focusedMovie.value = Resource.Success(null)
            return
        }

        currentSearchJob?.cancel()

        currentSearchJob = viewModelScope.launch(Dispatchers.IO) {
            // 1. 先检查本地数据库
            val cachedMedia = mediaDao.getMediaByUri(videoUri)
            if (cachedMedia != null) {
                Log.d("MovieViewModel", "Hit Cache for: $movieName")
                _focusedMovie.value = Resource.Success(cachedMedia.toMediaItem())
                return@launch
            }

            _focusedMovie.value = Resource.Loading
            delay(800)
            if (movieName==null) {
                _focusedMovie.value = Resource.Success(null)
                return@launch
            }
            val mediaInfo = MediaInfoExtractorFormFileName.extract(movieName)
            if (mediaInfo.title.isBlank()) {
                _focusedMovie.value = Resource.Success(null)
                return@launch
            }

            try {
                if (mediaInfo.mediaType == "movie") {
                    val result = repository.searchMovies(mediaInfo.title, year = mediaInfo.year)
                    if (result is Resource.Success) {
                        val movie = result.data.results.firstOrNull()
                        if (movie != null) {
                            // [修改] 保存到数据库时填入新字段
                            val entity = MediaCacheEntity(
                                videoUri = videoUri,
                                dataSourceType = dataSourceType, // 保存
                                fileName = movieName,            // 保存原始文件名
                                connectionName = connectionName, // 保存
                                tmdbId = movie.id,
                                mediaType = "movie",
                                title = movie.title ?: "",
                                overview = movie.overview,
                                posterPath = movie.posterPath,
                                backdropPath = movie.backdropPath,
                                releaseDate = movie.releaseDate,
                                voteAverage = movie.voteAverage,
                                isDetailsLoaded = false
                            )
                            mediaDao.insertMedia(entity)
                            _focusedMovie.value = Resource.Success(entity.toMediaItem())
                        } else {
                            _focusedMovie.value = Resource.Success(null)
                        }
                    } else if (result is Resource.Error) {
                        _focusedMovie.value = Resource.Error(result.message, result.exception)
                    }
                } else {
                    val result = repository.searchTV(mediaInfo.title, year = mediaInfo.year)
                    if (result is Resource.Success) {
                        val tv = result.data.results.firstOrNull()
                        if (tv != null) {
                            // [修改] 保存到数据库时填入新字段
                            val entity = MediaCacheEntity(
                                videoUri = videoUri,
                                dataSourceType = dataSourceType, // 保存
                                fileName = movieName,            // 保存
                                connectionName = connectionName, // 保存
                                tmdbId = tv.id,
                                mediaType = "tv",
                                title = tv.name ?: "",
                                overview = tv.overview,
                                posterPath = tv.posterPath,
                                backdropPath = tv.backdropPath,
                                releaseDate = tv.firstAirDate,
                                voteAverage = tv.voteAverage,
                                seasonNumber = mediaInfo.season.toIntOrNull() ?: 1,
                                episodeNumber = mediaInfo.episode.toIntOrNull() ?: 1,
                                isDetailsLoaded = false
                            )
                            mediaDao.insertMedia(entity)
                            _focusedMovie.value = Resource.Success(entity.toMediaItem())
                        } else {
                            _focusedMovie.value = Resource.Success(null)
                        }
                    } else if (result is Resource.Error) {
                        _focusedMovie.value = Resource.Error(result.message, result.exception)
                    }
                }
            } catch (e: Exception) {
                _focusedMovie.value = Resource.Error("Search failed", e)
            }
        }
    }
    /**
     * 获取电影详情 (带缓存更新)
     */
    fun getMovieDetailsWithCache(
        movieId: Int,
        videoUri: String,
        dataSourceType: String,
        fileName: String,
        connectionName: String
    ) {
        _movieDeResults.value = Resource.Loading
        viewModelScope.launch(Dispatchers.IO) {
            // 1. 检查缓存是否包含详情
            val cached = mediaDao.getMediaByUri(videoUri)
            if (cached != null && cached.isDetailsLoaded && cached.mediaType == "movie") {
                Log.d("MovieViewModel", "Hit Details Cache for Movie")
                // 构造 MovieDetails 对象返回给 UI
                val details = MovieDetails(
                    id = cached.tmdbId,
                    title = cached.title,
                    status = cached.status,
                    overview = cached.overview,
                    posterPath = cached.posterPath,
                    backdropPath = cached.backdropPath,
                    voteAverage = cached.voteAverage,
                    releaseDate = cached.releaseDate,
                    originCountry = cached.originCountry,
                    genreList = cached.genres
                )
                _movieDeResults.value = Resource.Success(details)
                return@launch
            }

            // 2. 网络请求
            val result = repository.getMovieDetails(movieId)
            if (result is Resource.Success) {
                val details = result.data
                _movieDeResults.value = Resource.Success(details)

                // 3. 更新数据库 (如果存在记录)
                if (cached != null) {
                    val updatedEntity = cached.copy(
                        isDetailsLoaded = true,
                        status = details.status,
                        genres = details.genreList,
                        originCountry = details.originCountry,
                        // 有可能 API 详情里的 overview 比列表里的详细，这里更新一下
                        overview = details.overview,
                        backdropPath = details.backdropPath ?: cached.backdropPath,
                        posterPath = details.posterPath ?: cached.posterPath
                    )
                    mediaDao.updateMedia(updatedEntity)
                } else {
                    // 极端情况：没有经过列表搜索直接进详情 (理论上现有流程不会发生，但为了健壮性)
                    val newEntity = MediaCacheEntity(
                        videoUri = videoUri,
                        tmdbId = details.id,
                        mediaType = "movie",
                        title = details.title ?: "",
                        overview = details.overview,
                        posterPath = details.posterPath,
                        backdropPath = details.backdropPath,
                        releaseDate = details.releaseDate,
                        voteAverage = details.voteAverage,
                        status = details.status,
                        genres = details.genreList,
                        originCountry = details.originCountry,
                        isDetailsLoaded = true,
                        dataSourceType = dataSourceType,
                        fileName = fileName,
                        connectionName = connectionName
                    )
                    mediaDao.insertMedia(newEntity)
                }
            } else if (result is Resource.Error) {
                _movieDeResults.value = Resource.Error(result.message, result.exception)
            }
        }
    }

    /**
     * 获取 TV 详情 (带缓存更新)
     * 这里比较复杂，因为 TV 有 SeriesDetails 和 EpisodeDetails 两部分
     */
    fun getTVDetailsWithCache(
        seriesId: Int,
        season: Int,
        episode: Int,
        videoUri: String,
        dataSourceType: String,
        fileName: String,
        connectionName: String
    ) {
        _tvSeriesResults.value = Resource.Loading
        _tvEpisodeResults.value = Resource.Loading

        viewModelScope.launch(Dispatchers.IO) {
            // 1. 先查本地缓存
            val cached = mediaDao.getMediaByUri(videoUri)

            // 如果缓存命中且是 TV 类型，且已经加载过详情
            if (cached != null && cached.isDetailsLoaded && cached.mediaType == "tv") {
                // ... (这部分缓存读取逻辑保持不变，直接返回缓存数据) ...
                // 构造 SeriesDetails
                val seriesDetails = TVSeriesDetails(
                    id = cached.tmdbId,
                    name = cached.title,
                    overview = cached.overview,
                    posterPath = cached.posterPath,
                    backdropPath = cached.backdropPath,
                    voteAverage = cached.voteAverage,
                    firstAirDate = cached.releaseDate,
                    status = cached.status,
                    genreList = cached.genres,
                    originCountry = cached.originCountry,
                    numberOfSeasons = cached.numberOfSeasons,
                    numberOfEpisodes = cached.numberOfEpisodes,
                    lastAirDate = null
                )
                _tvSeriesResults.value = Resource.Success(seriesDetails)

                // 构造 EpisodeDetails
                if (cached.episodeName != null) {
                    val episodeDetails = TVEpisode(
                        id = 0,
                        name = cached.episodeName,
                        overview = cached.episodeOverview ?: "",
                        stillPath = cached.episodeStillPath,
                        airDate = cached.episodeAirDate ?: "",
                        runtime = cached.episodeRuntime,
                        seasonNumber = cached.seasonNumber,
                        episodeNumber = cached.episodeNumber,
                        voteAverage = 0.0,
                        voteCount = 0,
                        episodeType = "standard",
                        productionCode = ""
                    )
                    _tvEpisodeResults.value = Resource.Success(episodeDetails)
                    return@launch // ✪ 命中缓存，直接结束，无需联网
                }
            }

            // 2. 缓存未命中，发起网络请求 (重点优化：并行请求)
            try {
                // 使用 async 同时发起两个请求
                val seriesDeferred = async { repository.getTVSeriesDetails(seriesId) }
                val episodeDeferred = async { repository.getTVEpisodeDetails(seriesId, season, episode) }

                // 等待两个结果都返回
                val seriesResult = seriesDeferred.await()
                val episodeResult = episodeDeferred.await()

                // 更新 UI 状态
                if (seriesResult is Resource.Success) {
                    _tvSeriesResults.value = seriesResult
                } else if (seriesResult is Resource.Error) {
                    _tvSeriesResults.value = seriesResult
                }

                if (episodeResult is Resource.Success) {
                    _tvEpisodeResults.value = episodeResult
                } else if (episodeResult is Resource.Error) {
                    _tvEpisodeResults.value = episodeResult
                }

                // 3. 只要两个请求都成功，就写入数据库缓存
                if (seriesResult is Resource.Success && episodeResult is Resource.Success) {
                    val sData = seriesResult.data
                    val eData = episodeResult.data

                    val newOrUpdatedEntity = cached?.// 更新现有记录
                    copy(
                        isDetailsLoaded = true,
                        status = sData.status,
                        genres = sData.genreList,
                        originCountry = sData.originCountry,
                        numberOfSeasons = sData.numberOfSeasons,
                        numberOfEpisodes = sData.numberOfEpisodes,
                        episodeName = eData.name,
                        episodeOverview = eData.overview,
                        episodeStillPath = eData.stillPath,
                        episodeAirDate = eData.airDate,
                        episodeRuntime = eData.runtime,
                        // 可能会更新的基础信息
                        overview = sData.overview ?: cached.overview,
                        backdropPath = sData.backdropPath ?: cached.backdropPath,
                        posterPath = sData.posterPath ?: cached.posterPath
                    )
                        ?: // 新建记录 (虽然通常列表页已经创建了，但为了代码健壮性保留)
                        MediaCacheEntity(
                            videoUri = videoUri,
                            tmdbId = sData.id,
                            mediaType = "tv",
                            title = sData.name ?: "",
                            overview = sData.overview ?: "",
                            posterPath = sData.posterPath,
                            backdropPath = sData.backdropPath,
                            releaseDate = sData.firstAirDate,
                            voteAverage = sData.voteAverage,
                            seasonNumber = season,
                            episodeNumber = episode,
                            isDetailsLoaded = true,
                            status = sData.status,
                            genres = sData.genreList,
                            originCountry = sData.originCountry,
                            numberOfSeasons = sData.numberOfSeasons,
                            numberOfEpisodes = sData.numberOfEpisodes,
                            episodeName = eData.name,
                            episodeOverview = eData.overview,
                            episodeStillPath = eData.stillPath,
                            episodeAirDate = eData.airDate,
                            episodeRuntime = eData.runtime,
                            dataSourceType = dataSourceType,
                            fileName = fileName,
                            connectionName = connectionName
                        )
                    mediaDao.insertMedia(newOrUpdatedEntity) // 使用 insert(onConflict = REPLACE) 或者 update
                }

            } catch (e: Exception) {
                // 处理未捕获的异常
                _tvSeriesResults.value = Resource.Error("Unknown error", e)
            }
        }
    }

    /**
     * 【新增】清理媒体缓存数据库 (相当于 Kodi 的清理资料库)
     * 在设置页面调用此方法
     */
    fun clearMediaLibrary() {
        // 必须在 IO 线程执行数据库操作
        viewModelScope.launch(Dispatchers.IO) {
            try {
                mediaDao.clearAllMediaCache()
                Log.d("MovieViewModel", "Media cache successfully cleared.")

                // 🚀 【下一步建议】如果你想在 UI 上显示“清理完成”的提示，
                // 可以在这里更新一个 MutableStateFlow 或 LiveData，并在设置 Composable 中监听它。

            } catch (e: Exception) {
                Log.e("MovieViewModel", "Failed to clear media cache: ${e.message}", e)
                // 可以在这里处理清理失败的逻辑
            }
        }
    }

    // 扩展函数：把 Movie/TvData 转成通用的 MediaItem
//    private fun Movie.toMediaItem() = MediaItem(
//        id = id,
//        title = title ?: "",
//        overview = overview,
//        posterPath = posterPath,
//        releaseDate = releaseDate,
//        isMovie = true
//    )

//    private fun TVData.toMediaItem() = MediaItem(
//        id = id,
//        title = name ?: "",
//        overview = overview,
//        posterPath = posterPath,
//        releaseDate = firstAirDate, // TV 的 releaseDate 实际是 first_air_date
//        isMovie = false,
//    )

    // 辅助函数：检测字符串是否包含中文字符
    fun String.containsChinese(): Boolean {
        return this.any { it in '\u4e00'..'\u9fff' }
    }

//    fun refreshAll() {
//        loadPopularMovies()
//        loadTopRatedMovies()
//    }

    // 清空焦点电影信息
    fun clearFocusedMovie() {
        currentSearchJob?.cancel()
        _focusedMovie.value = Resource.Success(null)
    }
}


