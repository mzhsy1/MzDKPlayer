package org.mz.mzdkplayer.viewmodel

import org.mz.mzdkplayer.tool.metadata.MediaInfoExtractorFormFileName
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.mz.mzdkplayer.data.douban.DoubanScraper
import org.mz.mzdkplayer.data.douban.DoubanSubject
import org.mz.mzdkplayer.data.douban.toMediaItem
import org.mz.mzdkplayer.data.local.MediaCacheEntity
import org.mz.mzdkplayer.data.local.MediaDao
import org.mz.mzdkplayer.data.model.Genre
import org.mz.mzdkplayer.data.model.MediaItem
import org.mz.mzdkplayer.data.model.Movie
import org.mz.mzdkplayer.data.model.MovieDetails
import org.mz.mzdkplayer.data.model.TVData
import org.mz.mzdkplayer.data.model.TVEpisode
import org.mz.mzdkplayer.data.model.TVSeriesDetails
import org.mz.mzdkplayer.data.repository.Resource
import org.mz.mzdkplayer.data.repository.TmdbRepository
import org.mz.mzdkplayer.data.repository.SettingsRepository
import org.mz.mzdkplayer.tool.logic.ScrapeSourcePolicy
import org.mz.mzdkplayer.tool.metadata.NfoReader
import org.mz.mzdkplayer.tool.metadata.NfoTool
import org.mz.mzdkplayer.tool.metadata.MediaInfo
import org.mz.mzdkplayer.di.AppContext
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi

class MovieViewModel(private val repository: TmdbRepository,private val mediaDao: MediaDao) : ViewModel() {
    private val settingsRepo = SettingsRepository

    /** 豆瓣兜底数据源（TMDB 搜不到时才用），内部自带 OkHttp 客户端，做成单例字段复用 */
    private val doubanScraper = DoubanScraper()

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
    // [新增] 手动搜索的结果流 (避免干扰主界面的 focusedMovie 或 popularMovies)
    private val _manualSearchResults = MutableStateFlow<Resource<List<MediaItem>>>(Resource.Success(emptyList()))
    val manualSearchResults: StateFlow<Resource<List<MediaItem>>> = _manualSearchResults

    // 新增：扫描状态，用于UI显示进度或禁用按钮（可选）
    private val _isScanning = MutableStateFlow(false)
    val isScanning = _isScanning.asStateFlow()

    // 新增：追踪扫描进度和总数
    private val _currentScanIndex = MutableStateFlow(0)
    val currentScanIndex: StateFlow<Int> = _currentScanIndex.asStateFlow()

    private val _totalScanCount = MutableStateFlow(0)
    val totalScanCount: StateFlow<Int> = _totalScanCount.asStateFlow()
    // 搜索任务 Job
    private var currentSearchJob: Job? = null

    // 手动匹配页的搜索任务（换关键词 / 换数据源时取消上一次）
    private var manualSearchJob: Job? = null

    /**
     * [修改] 搜索焦点电影/剧集 (一步到位获取详情)
     */
    fun searchFocusedMovie(
        movieName: String?,
        isDirectory: Boolean,
        videoUri: String,
        dataSourceType: String,
        connectionName: String
    ) {
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

                // [优化] 如果缓存里只是基础信息(isDetailsLoaded=false)，这里可以静默去更新一下详情
                // 如果不需要自动补全详情，可以把下面这段 if 去掉
                if (!cachedMedia.isDetailsLoaded) {
                    val mediaInfo = MediaInfoExtractorFormFileName.extract(movieName ?: "")
                    searchAndFetchFullDetails(mediaInfo, videoUri, dataSourceType, fileName = movieName ?: "", connectionName)
                    // 更新完后重新发个通知给 UI
                    val updated = mediaDao.getMediaByUri(videoUri)
                    if (updated != null) _focusedMovie.value = Resource.Success(updated.toMediaItem())
                }
                return@launch
            }

            _focusedMovie.value = Resource.Loading
            delay(500) // 稍微防抖一下

            if (movieName == null) {
                _focusedMovie.value = Resource.Success(null)
                return@launch
            }

            val mediaInfo = MediaInfoExtractorFormFileName.extract(movieName)
            Log.d("MovieViewModel", "Extracted MediaInfo: $mediaInfo for file: $movieName")
            if (mediaInfo.title.isBlank()) {
                _focusedMovie.value = Resource.Success(null)
                return@launch
            }

            // [核心修改] 调用新方法：搜索 + 获取详情 + 入库
            val savedEntity = searchAndFetchFullDetails(mediaInfo, videoUri, dataSourceType, movieName, connectionName)

            if (savedEntity != null) {
                _focusedMovie.value = Resource.Success(savedEntity.toMediaItem())
            } else {
                // 如果搜不到，或者网络错误，返回 Null 或者错误
                _focusedMovie.value = Resource.Success(null)
            }
        }
    }
    /**
     * [修改] 仅查询数据库获取焦点信息 (用于"禁止自动刮削"模式)
     * 如果数据库有记录，返回记录；如果没有，返回 null (清空 UI)
     */
    fun getFocusedInfo(
        movieName: String?,
        isDirectory: Boolean,
        videoUri: String,
        dataSourceType: String,
        connectionName: String
    ) {
        // 1. 如果是文件夹 或 文件名为空，直接清空并返回
        if (isDirectory || movieName == null) {
            _focusedMovie.value = Resource.Success(null)
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            // 2. 检查本地数据库
            val cachedMedia = mediaDao.getMediaByUri(videoUri)

            if (cachedMedia != null) {
                // 命中缓存：显示数据库里的信息
                Log.d("MovieViewModel", "Hit Cache (Read-Only) for: $movieName")
                _focusedMovie.value = Resource.Success(cachedMedia.toMediaItem())
                if (!cachedMedia.isDetailsLoaded) {
                    val mediaInfo = MediaInfoExtractorFormFileName.extract(movieName)
                    searchAndFetchFullDetails(mediaInfo, videoUri, dataSourceType, fileName = movieName, connectionName)
                    // 更新完后重新发个通知给 UI
                    val updated = mediaDao.getMediaByUri(videoUri)
                    if (updated != null) _focusedMovie.value = Resource.Success(updated.toMediaItem())
                }
            } else {
                // [关键修改]：数据库没查到，必须显式返回 null
                // 这样 UI 才会把海报区清空，而不是显示上一个文件的信息，也不是强制去联网搜索
                _focusedMovie.value = Resource.Success(null)
            }
        }
    }
    fun getFocusedMediaInfoIsExisted(
        isDirectory: Boolean,
        videoUri: String,
    ): MediaCacheEntity?{
        if (isDirectory) {
            _focusedMovie.value = Resource.Success(null)
            return null
        }
        var re:MediaCacheEntity ?= null
        viewModelScope.launch(Dispatchers.IO) {
            // 1. 先检查本地数据库
            val cachedMedia = mediaDao.getMediaByUri(videoUri)
            re = cachedMedia
        }
        return re
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
            // 【注意】非 TMDB 来源（豆瓣兜底）记录里的 tmdbId 列存的是**豆瓣条目 id**，
            // 数值空间和 TMDB id 重叠，拿去查 TMDB 会查到另一部片子，所以只要本地有这条记录就直接收尾
            if (cached != null && cached.mediaType == "movie" &&
                (cached.isDetailsLoaded || cached.source != MediaCacheEntity.SOURCE_TMDB)
            ) {
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
                        connectionName = connectionName,
                        groupKey = "movie_${videoUri}"

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

                // 【关键】非 TMDB 来源（豆瓣兜底）走到这里说明：剧集信息已从本地拿到了，但没有分集信息。
                // 它的 tmdbId 列存的是**豆瓣条目 id**（数值空间和 TMDB 重叠），继续往下联网会查到另一部剧，
                // 所以这里必须收尾：分集状态用 Error 收口，UI 会隐藏「当前单集详情」卡片、背景图退回剧集背景。
                if (cached.source != MediaCacheEntity.SOURCE_TMDB) {
                    Log.d("MovieViewModel", "非 TMDB 来源，跳过联网补分集: ${cached.source}")
                    _tvEpisodeResults.value = Resource.Error("该数据源没有单集信息")
                    return@launch
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
                            connectionName = connectionName,
                            groupKey = "tv_${sData.id}"
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
     * 批量扫描当前目录下的视频文件 (同时获取完整详情)
     */
    fun batchScrapeVideoInfo(
        videoList: List<Pair<String, String>>, // Pair(fileName, videoUri)
        dataSourceType: String,
        connectionName: String
    ) {
        if (_isScanning.value) return

        viewModelScope.launch(Dispatchers.IO) {
            _isScanning.value = true
            _totalScanCount.value = videoList.size
            _currentScanIndex.value = 0
            Log.d("MovieViewModel", "开始批量扫描(含详情)，待处理: ${videoList.size}")

            try {
                videoList.forEachIndexed { index, (fileName, videoUri) ->
                    _currentScanIndex.value = index + 1

                    // 1. 检查数据库
                    val cachedMedia = mediaDao.getMediaByUri(videoUri)
                    if (cachedMedia != null) {
                        // 如果已存在且详情已加载，直接跳过
                        if (cachedMedia.isDetailsLoaded) {
                            Log.d("MovieViewModel", "跳过已存在完整数据: $fileName")
                            return@forEachIndexed
                        }
                        // 如果只是基础数据，可以选择继续往下走去更新详情
                    }

                    val mediaInfo = MediaInfoExtractorFormFileName.extract(fileName)
                    if (mediaInfo.title.isBlank()) return@forEachIndexed

                    // 2. 延时防封 (因为现在请求多了，稍微保持一点间隔)
                    if (index > 0) delay(1500)

                    Log.d("MovieViewModel", "正在获取完整信息 ($index/${videoList.size}): ${mediaInfo.title}")

                    // [核心修改] 调用通用方法获取完整详情
                    searchAndFetchFullDetails(mediaInfo, videoUri, dataSourceType, fileName, connectionName)
                }
            } finally {
                _isScanning.value = false
                Log.d("MovieViewModel", "批量扫描结束")
            }
        }
    }
    /**
     * [新增] 手动搜索电影或剧集（TMDB / 豆瓣两个源，见 `EditTMDBInfoScreen` / `PhoneMatchScreen`）。
     *
     * @param source 搜索源，取值见 [ScrapeSourcePolicy]（`tmdb` / `douban`）。结果行的
     *   [MediaItem.source] 会带上它，点选之后取详情与入库都按这个来源走。
     */
    fun searchMediaManual(
        query: String,
        isMovie: Boolean,
        source: String = ScrapeSourcePolicy.TMDB
    ) {
        // 换关键词 / 换数据源时用户会连点，先把上一次取消：慢的那次结果后到会把「TMDB 的列表」
        // 顶到「豆瓣」开关下面，点选就用错了 id 空间（两个源的 id 数值区间是重叠的）
        manualSearchJob?.cancel()
        manualSearchJob = viewModelScope.launch(Dispatchers.IO) {
            _manualSearchResults.value = Resource.Loading
            try {
                if (source == ScrapeSourcePolicy.DOUBAN) {
                    // 豆瓣的搜索建议接口不分电影/剧集（`type` 字段不可信），要的就是整份候选；
                    // 类型在点选之后由详情接口的 subtype 定，见 updateFromDouban。
                    // distinctBy：手机端的结果列表用 id 当 LazyColumn 的 key，重复 id 会直接崩
                    val items = doubanScraper.search(query)
                        .mapNotNull { it.toMediaItem() }
                        .distinctBy { it.id }
                    _manualSearchResults.value = Resource.Success(items)
                } else if (isMovie) {
                    val result = repository.searchMovies(query, year = "") // 手动搜索通常不强制年份
                    if (result is Resource.Success) {
                        // 转换 Movie -> MediaItem
                        val items = result.data.results.map { it.toMediaItem() }
                        _manualSearchResults.value = Resource.Success(items)
                    } else if (result is Resource.Error) {
                        _manualSearchResults.value = Resource.Error(result.message, result.exception)
                    }
                } else {
                    val result = repository.searchTV(query, year = "")
                    if (result is Resource.Success) {
                        // 转换 TVData -> MediaItem
                        val items = result.data.results.map { it.toMediaItem() }
                        _manualSearchResults.value = Resource.Success(items)
                    } else if (result is Resource.Error) {
                        _manualSearchResults.value = Resource.Error(result.message, result.exception)
                    }
                }
            } catch (e: Exception) {
                _manualSearchResults.value = Resource.Error("Search failed", e)
            }
        }
    }

    /**
     * 手动匹配页默认选中的搜索源：跟随设置里的「刮削首选数据源」。
     *
     * 复用 [ScrapeSourcePolicy.order] 而不是再判断一次谁先谁后 —— 顺序的定义只有那一处。
     */
    fun defaultSearchSource(): String =
        ScrapeSourcePolicy.order(settingsRepo.scrapeSourcePriority).first()

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
    /**
     * [修改] 手动保存/修正文件映射 (修正后立即获取完整详情)
     *
     * 取哪边的详情由 [MediaItem.source] 决定：TMDB 走 `repository`，豆瓣走 [updateFromDouban]，
     * 两条路径用的是同一个视频地址，所以入库后是「覆盖」而不是新增一条。
     */
    fun updateMediaMapping(
        videoUri: String,
        selectedMedia: MediaItem, // 用户选中的条目 (只包含基础信息)，自带数据来源
        seasonNumber: Int,        // 用户输入的季 (仅TV有效)
        episodeNumber: Int,       // 用户输入的集 (仅TV有效)
        originalFileName: String,
        dataSourceType: String = "SMB",
        connectionName: String = "电影"
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            // 1. 获取旧记录以保留连接信息
            val oldRecord = mediaDao.getMediaByUri(videoUri)
            val finalDataSource = oldRecord?.dataSourceType ?: dataSourceType
            val finalConnection = oldRecord?.connectionName ?: connectionName

            try {
                if (selectedMedia.source == MediaCacheEntity.SOURCE_DOUBAN) {
                    updateFromDouban(
                        videoUri = videoUri,
                        selectedMedia = selectedMedia,
                        seasonNumber = seasonNumber,
                        episodeNumber = episodeNumber,
                        originalFileName = originalFileName,
                        dataSourceType = finalDataSource,
                        connectionName = finalConnection
                    )
                } else if (selectedMedia.isMovie) {
                    // === 电影逻辑：立即获取详情 ===
                    val detailResult = repository.getMovieDetails(selectedMedia.id)
                    // 如果获取成功用详情，失败用基础信息
                    val finalDetails = if (detailResult is Resource.Success) detailResult.data else null

                    val newEntity = MediaCacheEntity(
                        videoUri = videoUri,
                        dataSourceType = finalDataSource,
                        fileName = originalFileName,
                        connectionName = finalConnection,
                        tmdbId = selectedMedia.id,
                        mediaType = "movie",
                        // 优先用详情数据
                        title = finalDetails?.title ?: selectedMedia.title ?: "",
                        overview = finalDetails?.overview ?: selectedMedia.overview,
                        posterPath = finalDetails?.posterPath ?: selectedMedia.posterPath,
                        backdropPath = finalDetails?.backdropPath ?: selectedMedia.backdropPath,
                        releaseDate = finalDetails?.releaseDate ?: selectedMedia.releaseDate,
                        voteAverage = finalDetails?.voteAverage ?: 0.0,
                        // 详情字段
                        status = finalDetails?.status?:"未知状态",
                        genres = finalDetails?.genreList ?: emptyList(),
                        originCountry = finalDetails?.originCountry ?: emptyList(),
                        isDetailsLoaded = finalDetails != null, // 标记是否加载完成
                        groupKey = "movie_${videoUri}"
                    )
                    mediaDao.insertMedia(newEntity)

                } else {
                    // === TV 逻辑：立即获取系列详情 + 分集详情 ===
                    // 使用 async 并行请求
                    val seriesDeferred = async { repository.getTVSeriesDetails(selectedMedia.id) }
                    val episodeDeferred = async { repository.getTVEpisodeDetails(selectedMedia.id, seasonNumber, episodeNumber) }

                    val seriesResult = seriesDeferred.await()
                    val episodeResult = episodeDeferred.await()

                    val sData = if (seriesResult is Resource.Success) seriesResult.data else null
                    val eData = if (episodeResult is Resource.Success) episodeResult.data else null

                    val newEntity = MediaCacheEntity(
                        videoUri = videoUri,
                        dataSourceType = finalDataSource,
                        fileName = originalFileName,
                        connectionName = finalConnection,
                        tmdbId = selectedMedia.id,
                        mediaType = "tv",
                        // 基础/系列信息
                        title = sData?.name ?: selectedMedia.title ?: "",
                        overview = sData?.overview ?: selectedMedia.overview,
                        posterPath = sData?.posterPath ?: selectedMedia.posterPath,
                        backdropPath = sData?.backdropPath ?: selectedMedia.backdropPath,
                        releaseDate = sData?.firstAirDate ?: selectedMedia.releaseDate,
                        voteAverage = sData?.voteAverage ?: 0.0,
                        status = sData?.status?:"未知状态",
                        genres = sData?.genreList ?: emptyList(),
                        originCountry = sData?.originCountry ?: emptyList(),
                        numberOfSeasons = sData?.numberOfSeasons,
                        numberOfEpisodes = sData?.numberOfEpisodes,
                        // 修正后的季/集信息
                        seasonNumber = seasonNumber,
                        episodeNumber = episodeNumber,
                        // 分集详情
                        episodeName = eData?.name,
                        episodeOverview = eData?.overview,
                        episodeStillPath = eData?.stillPath,
                        episodeAirDate = eData?.airDate,
                        episodeRuntime = eData?.runtime,

                        isDetailsLoaded = sData != null,
                        groupKey = "tv_${selectedMedia.id}"
                    )
                    mediaDao.insertMedia(newEntity)
                }

                Log.d("MovieViewModel", "手动修正并获取详情成功: ${selectedMedia.title}")

                // 立即更新当前焦点的 StateFlow，这样 UI 会马上刷新背景和文字
                val updatedEntity = mediaDao.getMediaByUri(videoUri)
                if (updatedEntity != null) {
                    _focusedMovie.value = Resource.Success(updatedEntity.toMediaItem())
                }

            } catch (e: Exception) {
                Log.e("MovieViewModel", "手动修正获取详情失败", e)
                // 发生异常时，至少保存一个基础版本，防止白屏
                // 这里可以写一个降级逻辑，或者直接提示失败
            }
        }
    }

    /**
     * 手动匹配到豆瓣条目：按用户点选的条目 id 直接取详情，覆盖该文件的记录。
     *
     * 与自动兜底 [fetchFromDouban] 只有一处差异：**入库类型以详情接口的 subtype 为准**。
     * 自动流程拿不到用户意图，只能信文件名结构（免得换个数据源就在电影库/剧集库之间跳）；
     * 而手动匹配是用户主动挑了一个条目，此时条目自己的类型更权威 —— 挑成剧集就该进剧集库、
     * 记下季集，否则「选集」和剧集库分组都会错。
     *
     * 取详情失败时**不动原记录**（不写半条数据），由界面自己提示。
     */
    private suspend fun updateFromDouban(
        videoUri: String,
        selectedMedia: MediaItem,
        seasonNumber: Int,
        episodeNumber: Int,
        originalFileName: String,
        dataSourceType: String,
        connectionName: String
    ) {
        val requestedTv = !selectedMedia.isMovie
        val subject = doubanScraper.fetchById(selectedMedia.id.toString(), requestedTv)
        if (subject == null) {
            Log.w("MovieViewModel", "豆瓣取详情失败，保留原记录: $originalFileName")
            return
        }
        if (subject.isTv != requestedTv) {
            Log.d(
                "MovieViewModel",
                "豆瓣条目类型(${subject.isTv})与界面选择($requestedTv)不一致，按条目类型入库"
            )
        }

        val entity = buildDoubanEntity(
            subject = subject,
            videoUri = videoUri,
            dataSourceType = dataSourceType,
            fileName = originalFileName,
            connectionName = connectionName,
            isTv = subject.isTv,
            seasonNumber = seasonNumber,
            episodeNumber = episodeNumber
        ) ?: return
        mediaDao.insertMedia(entity)
        Log.d("MovieViewModel", "手动匹配豆瓣成功: ${subject.title} (id=${subject.id})")
    }

    // [新增] 核心通用方法：搜索并获取完整详情，然后入库
// 返回插入的 Entity，如果失败返回 null
    @OptIn(UnstableApi::class)
    private suspend fun searchAndFetchFullDetails(
        mediaInfo: MediaInfo,
        videoUri: String,
        dataSourceType: String,
        fileName: String,
        connectionName: String
    ): MediaCacheEntity? {
        try {
            // 0. 尝试加载本地 NFO (如果开启)
            if (settingsRepo.prioritizeLocalNfo) {
                NfoReader.getNfoStream(AppContext.require, videoUri, dataSourceType)?.use { inputStream ->
                    val nfoEntity = NfoTool.parseNfo(inputStream, videoUri, dataSourceType, fileName, connectionName)
                    if (nfoEntity != null) {
                        Log.d("MovieViewModel", "Hit local NFO for: $fileName")
                        mediaDao.insertMedia(nfoEntity)
                        return nfoEntity
                    }
                }
            }

            // 1. 按设置里的首选数据源依次尝试（默认豆瓣优先），首选搜不到就用另一个兜底。
            //    顺序只在 ScrapeSourcePolicy 里定义，这里不写 if-else 判断谁先谁后。
            for (source in ScrapeSourcePolicy.order(settingsRepo.scrapeSourcePriority)) {
                Log.d("MovieViewModel", "尝试数据源[$source]: $fileName")
                val entity = if (source == ScrapeSourcePolicy.DOUBAN) {
                    fetchFromDouban(mediaInfo, videoUri, dataSourceType, fileName, connectionName)
                } else {
                    fetchFromTmdb(mediaInfo, videoUri, dataSourceType, fileName, connectionName)
                }
                if (entity != null) return entity
            }
        } catch (e: Exception) {
            Log.e("MovieViewModel", "Fetch full details failed: $fileName", e)
        }
        return null
    }

    /**
     * TMDB 主流程：搜索 + 取详情 + 入库。返回 null 表示 TMDB 没搜到（交由上层去兜底）。
     *
     * 入库记录一律带 [MediaCacheEntity.SOURCE_TMDB]，这样库里同号但不同来源的条目不会互相串。
     */
    @OptIn(UnstableApi::class)
    private suspend fun fetchFromTmdb(
        mediaInfo: MediaInfo,
        videoUri: String,
        dataSourceType: String,
        fileName: String,
        connectionName: String
    ): MediaCacheEntity? {
        if (mediaInfo.mediaType == "movie") {
            // 1. 搜索电影
            var searchResult = repository.searchMovies(mediaInfo.title, year = mediaInfo.year)

            // [优化] 如果带年份搜索不到结果，且年份不为空，则尝试不带年份重新搜索
            if (searchResult is Resource.Success && searchResult.data.results.isEmpty() && mediaInfo.year.isNotEmpty()) {
                Log.d("MovieViewModel", "Search with year failed, retrying without year: ${mediaInfo.title}")
                searchResult = repository.searchMovies(mediaInfo.title, year = "")
            }

            if (searchResult is Resource.Success) {
                val basicMovie = searchResult.data.results.firstOrNull() ?: return null

                // 2. 立即获取详细信息
                val detailResult = repository.getMovieDetails(basicMovie.id)
                val finalDetails = if (detailResult is Resource.Success) detailResult.data else null

                // 3. 构建包含详情的实体
                val entity = MediaCacheEntity(
                    videoUri = videoUri,
                    dataSourceType = dataSourceType,
                    fileName = fileName,
                    connectionName = connectionName,
                    tmdbId = basicMovie.id,
                    mediaType = "movie",
                    // 优先使用详情里的数据
                    title = finalDetails?.title ?: basicMovie.title ?: "",
                    overview = finalDetails?.overview ?: basicMovie.overview,
                    posterPath = finalDetails?.posterPath ?: basicMovie.posterPath,
                    backdropPath = finalDetails?.backdropPath ?: basicMovie.backdropPath,
                    releaseDate = finalDetails?.releaseDate ?: basicMovie.releaseDate,
                    voteAverage = finalDetails?.voteAverage ?: basicMovie.voteAverage,
                    // 详情特有字段
                    status = finalDetails?.status ?: "未知状态",
                    genres = finalDetails?.genreList ?: emptyList(),
                    originCountry = finalDetails?.originCountry ?: emptyList(),
                    // 标记为详情已加载
                    isDetailsLoaded = finalDetails != null,
                    groupKey = "movie_${videoUri}",
                    // 显式标注来源：媒体库分组与详情页都靠它区分「这是 TMDB id 还是豆瓣 id」
                    source = MediaCacheEntity.SOURCE_TMDB
                )
                mediaDao.insertMedia(entity)
                return entity
            }
        } else {
            // 1. 搜索 TV
            var searchResult = repository.searchTV(mediaInfo.title, year = mediaInfo.year)

            // [优化] 如果带年份搜索不到结果，且年份不为空，则尝试不带年份重新搜索
            if (searchResult is Resource.Success && searchResult.data.results.isEmpty() && mediaInfo.year.isNotEmpty()) {
                Log.d("MovieViewModel", "Search TV with year failed, retrying without year: ${mediaInfo.title}")
                searchResult = repository.searchTV(mediaInfo.title, year = "")
            }

            if (searchResult is Resource.Success) {
                val basicTV = searchResult.data.results.firstOrNull() ?: return null

                // 2. 并行获取 Series详情 和 Episode详情
                val seriesDeferred = viewModelScope.async(Dispatchers.IO) {
                    repository.getTVSeriesDetails(basicTV.id)
                }

                // 如果文件名里解析出了季和集，就去查分集详情，否则只查剧集详情
                val seasonNum = mediaInfo.season.toIntOrNull() ?: 1
                val episodeNum = mediaInfo.episode.toIntOrNull() ?: 1

                val episodeDeferred = viewModelScope.async(Dispatchers.IO) {
                    repository.getTVEpisodeDetails(basicTV.id, seasonNum, episodeNum)
                }

                val seriesResult = seriesDeferred.await()
                val episodeResult = episodeDeferred.await()

                val sData = if (seriesResult is Resource.Success) seriesResult.data else null
                val eData = if (episodeResult is Resource.Success) episodeResult.data else null

                // 3. 构建包含详情的实体
                val entity = MediaCacheEntity(
                    videoUri = videoUri,
                    dataSourceType = dataSourceType,
                    fileName = fileName,
                    connectionName = connectionName,
                    tmdbId = basicTV.id,
                    mediaType = "tv",
                    title = sData?.name ?: basicTV.name ?: "",
                    overview = sData?.overview ?: basicTV.overview, // 系列简介
                    posterPath = sData?.posterPath ?: basicTV.posterPath,
                    backdropPath = sData?.backdropPath ?: basicTV.backdropPath,
                    releaseDate = sData?.firstAirDate ?: basicTV.firstAirDate,
                    voteAverage = sData?.voteAverage ?: basicTV.voteAverage,
                    seasonNumber = seasonNum,
                    episodeNumber = episodeNum,
                    // 详情特有字段
                    status = sData?.status ?:"未知状态",
                    genres = sData?.genreList ?: emptyList(),
                    originCountry = sData?.originCountry ?: emptyList(),
                    numberOfSeasons = sData?.numberOfSeasons,
                    numberOfEpisodes = sData?.numberOfEpisodes,
                    // 分集特有字段
                    episodeName = eData?.name,
                    episodeOverview = eData?.overview, // 分集简介
                    episodeStillPath = eData?.stillPath,
                    episodeAirDate = eData?.airDate,
                    episodeRuntime = eData?.runtime,

                    // 只要获取到了 Series 详情就算详情已加载
                    isDetailsLoaded = sData != null,
                    groupKey = "tv_${basicTV.id}",
                    source = MediaCacheEntity.SOURCE_TMDB
                )
                mediaDao.insertMedia(entity)
                return entity
            }
        }
        return null
    }
    /**
     * 豆瓣兜底：用中文名搜索 + 取详情 + 入库。
     *
     * 与 TMDB 流程的两个关键差别：
     * 1. 豆瓣没有分集接口，所以剧集只落「剧集级」信息（`episodeName` 等分集字段留空）；
     * 2. 来源必须写成 [MediaCacheEntity.SOURCE_DOUBAN]：豆瓣条目 id 和 TMDB id 的数值区间是重叠的，
     *    来源标记既用于媒体库分组，也用于详情页判断「这个 id 到底能不能拿去查 TMDB」。
     */
    private suspend fun fetchFromDouban(
        mediaInfo: MediaInfo,
        videoUri: String,
        dataSourceType: String,
        fileName: String,
        connectionName: String
    ): MediaCacheEntity? {
        return try {
            val isTv = mediaInfo.mediaType == "tv"
            val subject = doubanScraper.scrape(
                keyword = mediaInfo.title,
                year = mediaInfo.year.toIntOrNull(),
                preferTv = isTv,
            ) ?: return null

            // 类型仍以文件名结构为准（而不是豆瓣的 subtype）：一份文件不该因为换了数据源就在
            // 「电影库 / 剧集库」之间跳来跳去。不一致时只记日志，便于排查匹配错误。
            if (subject.isTv != isTv) {
                Log.d("MovieViewModel", "豆瓣类型(${subject.isTv})与文件名结构($isTv)不一致: $fileName")
            }

            val entity = buildDoubanEntity(
                subject = subject,
                videoUri = videoUri,
                dataSourceType = dataSourceType,
                fileName = fileName,
                connectionName = connectionName,
                isTv = isTv,
                seasonNumber = mediaInfo.season.toIntOrNull() ?: 1,
                episodeNumber = mediaInfo.episode.toIntOrNull() ?: 1
            ) ?: return null
            mediaDao.insertMedia(entity)
            Log.d("MovieViewModel", "豆瓣兜底入库成功: ${subject.title} (id=${subject.id})")
            entity
        } catch (e: Exception) {
            Log.e("MovieViewModel", "豆瓣兜底失败: $fileName", e)
            null
        }
    }

    /**
     * 豆瓣条目 → 入库实体（自动兜底 [fetchFromDouban] 与手动匹配 [updateFromDouban] 共用）。
     *
     * 返回 null 说明豆瓣 id 不是数字串（接口变了）：[MediaCacheEntity.tmdbId] 是 Int，
     * 借存不了这种 id，只能放弃这一条。
     */
    private fun buildDoubanEntity(
        subject: DoubanSubject,
        videoUri: String,
        dataSourceType: String,
        fileName: String,
        connectionName: String,
        isTv: Boolean,
        seasonNumber: Int,
        episodeNumber: Int
    ): MediaCacheEntity? {
        val subjectId = subject.id.toIntOrNull()
        if (subjectId == null) {
            Log.w("MovieViewModel", "豆瓣 id 不是数字，跳过入库: ${subject.id}")
            return null
        }
        return MediaCacheEntity(
            videoUri = videoUri,
            dataSourceType = dataSourceType,
            fileName = fileName,
            connectionName = connectionName,
            // 豆瓣条目 id 借存在 tmdbId 列里，靠 source 区分是两个不同的 id 空间
            tmdbId = subjectId,
            mediaType = if (isTv) "tv" else "movie",
            title = subject.title,
            overview = subject.overview,
            posterPath = subject.posterUrl,
            backdropPath = subject.backdropUrl,
            releaseDate = subject.releaseDate,
            voteAverage = subject.rating,
            seasonNumber = if (isTv) seasonNumber else 0,
            episodeNumber = if (isTv) episodeNumber else 0,
            // 豆瓣接口没有“上映状态”这个字段，别编，沿用和 TMDB 失败时相同的兜底文案
            status = "未知状态",
            genres = subject.genres.mapIndexed { index, name -> Genre(id = index, name = name) },
            originCountry = subject.countries,
            numberOfEpisodes = subject.episodesCount,
            // 豆瓣没有分集信息，这五项留空 —— 详情页正是靠 source 判断「别再拿这个 id 去查 TMDB」
            episodeName = null,
            episodeOverview = null,
            episodeStillPath = null,
            episodeAirDate = null,
            episodeRuntime = null,
            // 标题/简介/海报/评分/类型都拿到了，按“详情已加载”处理
            isDetailsLoaded = true,
            source = MediaCacheEntity.SOURCE_DOUBAN,
            groupKey = if (isTv) "douban_tv_$subjectId" else "douban_movie_$videoUri"
        )
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
//
//    private fun TVData.toMediaItem() = MediaItem(
//        id = id,
//        title = name ?: "",
//        overview = overview,
//        posterPath = posterPath,
//        releaseDate = firstAirDate, // TV 的 releaseDate 实际是 first_air_date
//        isMovie = false,
//    )
    // 扩展函数需要放在类内部或同文件下 (如果之前没写的话)
    private fun Movie.toMediaItem() = MediaItem(
        id = id, title = title ?: "", overview = overview, posterPath = posterPath,
        backdropPath = backdropPath?:"未知", releaseDate = releaseDate, isMovie = true
    )

    private fun TVData.toMediaItem() = MediaItem(
        id = id, title = name ?: "", overview = overview, posterPath = posterPath,
        backdropPath = backdropPath?:"未知", releaseDate = firstAirDate, isMovie = false
    )


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


