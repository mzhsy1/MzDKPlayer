package org.mz.mzdkplayer.ui.phone.screen

import android.content.Context
import android.graphics.BitmapFactory
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.mz.mzdkplayer.R
import org.mz.mzdkplayer.data.model.AudioInfo
import org.mz.mzdkplayer.data.model.AudioItem
import org.mz.mzdkplayer.data.model.MediaHistoryRecord
import org.mz.mzdkplayer.data.repository.AudioPlaylistRepository
import org.mz.mzdkplayer.player.core.selectedDataSourceFactory
import org.mz.mzdkplayer.tool.LyricEntry
import org.mz.mzdkplayer.tool.PhoneMediaLogic
import org.mz.mzdkplayer.tool.SmbUtils
import org.mz.mzdkplayer.tool.Tools
import org.mz.mzdkplayer.tool.createArtworkBitmap
import org.mz.mzdkplayer.tool.extractAudioInfoAndLyricsFromStream
import org.mz.mzdkplayer.tool.lyricIndexAt
import org.mz.mzdkplayer.tool.parseLrc
import org.mz.mzdkplayer.ui.phone.PhoneIcons
import org.mz.mzdkplayer.viewmodel.AudioViewModel
import org.mz.mzdkplayer.viewmodel.MediaHistoryViewModel
import java.io.InputStream
import java.io.File
import kotlin.math.abs

/** 播放历史的定时落盘间隔，与电视端同一口径（播放中每 10 秒写一次库） */
private const val HISTORY_SAVE_INTERVAL_MS = 10_000L

/** 相比上一次落盘，播放位置至少要推进这么多毫秒才值得再写一次库 */
private const val HISTORY_SAVE_MIN_DELTA_MS = 1_000L

/** 进度条刷新间隔（`ExoPlayer.currentPosition` 只在主线程读） */
private const val POSITION_POLL_INTERVAL_MS = 500L

/** 解析元数据与开流的日志 TAG，出问题时按它过滤 */
private const val LOG_TAG = "PhoneAudioPlayer"

/** 读外挂 `.lrc` 时用的 MIME：避免被各协议当成大媒体文件处理 */
private const val LYRIC_MIME_TYPE = "text/plain"

/**
 * 手机端音频播放页（第五阶段）。
 *
 * 与电视端 [org.mz.mzdkplayer.ui.audioplayer.AudioPlayerScreen] 的关系：
 * 播放内核同样是 ExoPlayer（`selectedDataSourceFactory` 按协议选数据源），播放列表同样来自
 * [AudioPlaylistRepository]；差别只在 UI —— 这里按手机的竖屏习惯重排（大封面 + 滑块进度 +
 * 上一首/播放/下一首 + 底部弹出播放列表 + 可折叠歌词），并把「读取内嵌封面 / 歌词」简化成
 * 一次流解析（电视端那套可视化频谱、脉冲动画、背景取色都没有搬）。
 *
 * 元数据写入 `audio_cache`：进页面先 [AudioViewModel.ensureAudioCache] 补一行（否则
 * `updateAudioInfo` 的 UPDATE 会静默落空），解析成功后回写标题/歌手/专辑/歌词/封面路径。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhoneAudioPlayerScreen(
    startIndex: Int,
    dataSourceType: String,
    connectionName: String,
    audioViewModel: AudioViewModel,
    mediaHistoryViewModel: MediaHistoryViewModel,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val playlist by AudioPlaylistRepository.playlist.collectAsState()

    // 播放列表是异步落盘 / 进程重建后读回来的，可能还没到；空列表时直接给一张空态
    if (playlist.isEmpty()) {
        PhoneAudioEmpty(onBack = onBack)
        return
    }

    val initialIndex = startIndex.coerceIn(0, playlist.lastIndex)
    val player = remember(playlist, dataSourceType) {
        createAudioPlayer(context, playlist.first().uri, dataSourceType)
    }

    var currentIndex by remember { mutableIntStateOf(initialIndex) }
    var isPlaying by remember { mutableStateOf(false) }
    var positionMs by remember { mutableLongStateOf(0L) }
    var durationMs by remember { mutableLongStateOf(0L) }
    var isDragging by remember { mutableStateOf(false) }
    var dragValue by remember { mutableFloatStateOf(0f) }
    var showPlaylist by remember { mutableStateOf(false) }
    // 歌词默认展开：手机屏比电视小，用户不会去翻顶栏找歌词；封面让出一部分高度就够
    var showLyrics by remember { mutableStateOf(true) }

    var audioInfo by remember { mutableStateOf<AudioInfo?>(null) }
    var infoLoading by remember { mutableStateOf(false) }

    val currentItem: AudioItem? = playlist.getOrNull(currentIndex)
    val currentUri = currentItem?.uri.orEmpty()
    val currentFileName = currentItem?.fileName.orEmpty()

    // 1. 建播放列表并跳到点进来的那一首（只在播放器重建时做一次）
    LaunchedEffect(player, playlist) {
        player.setMediaItems(playlist.map { MediaItem.fromUri(it.uri) })
        player.seekTo(initialIndex, 0L)
        player.prepare()
        player.playWhenReady = true
        currentIndex = initialIndex
    }

    // 2. 监听换歌 / 播放状态；自动连播时下标要跟着走，否则界面会停在上一首
    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                val uri = mediaItem?.localConfiguration?.uri?.toString() ?: return
                currentIndex = PhoneMediaLogic.indexOfUri(playlist, uri)
                audioInfo = null
            }

            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }
        }
        player.addListener(listener)
        onDispose { player.removeListener(listener) }
    }

    // 3. 进度轮询：ExoPlayer 的 currentPosition / duration 只能在主线程读
    LaunchedEffect(player) {
        while (true) {
            if (!isDragging) positionMs = player.currentPosition.coerceAtLeast(0L)
            val total = player.duration
            durationMs = if (total == C.TIME_UNSET || total < 0L) 0L else total
            delay(POSITION_POLL_INTERVAL_MS)
        }
    }

    // ==================== 播放进度持久化（口径与电视端一致）====================
    var lastSavedPosition by remember { mutableLongStateOf(-1L) }
    var lastSavedUri by remember { mutableStateOf(currentUri) }

    val savePlaybackHistory: () -> Unit = {
        val position = player.currentPosition
        val total = player.duration
        if (position > 0 && total > 0 && currentUri.isNotBlank()) {
            mediaHistoryViewModel.saveHistory(
                MediaHistoryRecord(
                    mediaUri = currentUri,
                    fileName = currentFileName,
                    playbackPosition = position,
                    mediaDuration = total,
                    protocolName = dataSourceType,
                    connectionName = connectionName,
                    serverAddress = "",
                    mediaType = "AUDIO",
                    timestamp = System.currentTimeMillis(),
                )
            )
            lastSavedPosition = position
            lastSavedUri = currentUri
        }
    }

    // 定时任务与 onDispose 都活得比一次重组久，必须读**最新一版**的保存逻辑：
    // 直接捕获 savePlaybackHistory 会一直用进入页面时的 currentUri，
    // 换歌后就把新歌的进度写到第一首的记录上了（电视端踩过这个坑）。
    val latestSavePlaybackHistory by rememberUpdatedState(savePlaybackHistory)
    val latestCurrentUri by rememberUpdatedState(currentUri)

    LaunchedEffect(player) {
        while (true) {
            delay(HISTORY_SAVE_INTERVAL_MS)
            if (latestCurrentUri != lastSavedUri) {
                lastSavedPosition = -1L
                lastSavedUri = latestCurrentUri
            }
            if (!player.isPlaying) continue
            val position = player.currentPosition
            if (position <= 0) continue
            if (abs(position - lastSavedPosition) < HISTORY_SAVE_MIN_DELTA_MS) continue
            latestSavePlaybackHistory()
        }
    }

    DisposableEffect(player) {
        onDispose {
            latestSavePlaybackHistory()
            player.release()
        }
    }

    // 4. 每换一首读一次元数据：先读缓存，没有就抓一次流解析并回写
    LaunchedEffect(currentUri, dataSourceType, connectionName) {
        if (currentUri.isBlank()) return@LaunchedEffect
        infoLoading = true
        val fileName = currentFileName.ifBlank { Tools.extractFileNameFromUri(currentUri) }
        // 先保证库里有这一行，否则下面回写元数据会静默落空
        audioViewModel.ensureAudioCache(fileName, currentUri, dataSourceType, connectionName)

        val cached = audioViewModel.getAudioCacheByUri(currentUri)
        // `localCoverPath` 为 null 表示「这一版还没解析过」：解析完无论有没有封面都会写值
        // （有封面写路径，没有写空串），所以 null 就重新解析一次 —— 这样第五阶段早期
        // 留下的记录（那时 WAV 的封面、外挂歌词都还读不出来）会自动补上。
        if (cached != null && cached.isDetailsLoaded && cached.localCoverPath != null) {
            val cachedInfo = AudioInfo(
                title = cached.title,
                artist = cached.artist,
                album = cached.album,
                durationSeconds = cached.duration / 1000,
                lyrics = cached.lyrics,
                artworkData = null,
                localCoverPath = cached.localCoverPath,
                bit = cached.bit,
                sampleRate = cached.sampleRate,
                bitsPerSample = cached.bitsPerSample,
            )
            // 库里没歌词时仍然试一次外挂 `.lrc`：老记录（外挂歌词还没做的那版解析出来的）
            // `lyrics` 是 NULL，但同目录可能一直躺着一个 .lrc，不补一次就永远显示不出来。
            // 真的没有 .lrc 的歌每次都多一次「打开失败」，代价可以接受；一旦读到就会写回库，
            // 下次走「歌词非空」的分支不再尝试。
            val sidecar = if (cachedInfo.lyrics.isNullOrBlank()) {
                loadSidecarLyrics(context, currentUri, dataSourceType, fileName)
            } else {
                null
            }
            val merged = if (sidecar != null) cachedInfo.copy(lyrics = sidecar) else cachedInfo
            audioInfo = merged
            if (sidecar != null) {
                audioViewModel.updateAudioInfo(
                    uri = currentUri,
                    info = merged,
                    localCoverPath = cached.localCoverPath,
                    duration = cached.duration,
                    isDetailsLoaded = true,
                )
            }
            infoLoading = false
            return@LaunchedEffect
        }

        val mimeType = audioMimeTypeOf(currentUri)
        val parsed = withContext(Dispatchers.IO) {
            runCatching {
                // 文件名要一并传进去：没有标签的文件（裸 WAV）靠它兜底标题与歌手
                openMediaStream(context, currentUri, dataSourceType, mimeType)?.use { stream ->
                    extractAudioInfoAndLyricsFromStream(context, stream, mimeType, fileName)
                }
            }.onFailure { Log.w(LOG_TAG, "解析音频元数据失败: $currentUri", it) }.getOrNull()
        }
        if (parsed != null) {
            // 内嵌歌词没有时读同目录的同名 `.lrc`：实测素材里五个音频**全都**配了外挂 .lrc，
            // 而只有两个文件带内嵌歌词，所以这一步才是歌词的主要来源
            val merged = if (parsed.lyrics.isNullOrBlank()) {
                loadSidecarLyrics(context, currentUri, dataSourceType, fileName)
                    ?.let { parsed.copy(lyrics = it) }
                    ?: parsed
            } else {
                parsed
            }
            audioInfo = merged
            val coverPath = withContext(Dispatchers.IO) {
                runCatching {
                    Tools.saveCoverImageToInternalStorage(context, currentUri, merged.artworkData)
                }.getOrNull()
            }
            if (coverPath != null) audioInfo = merged.copy(localCoverPath = coverPath)
            // 时长直接读播放器（此刻 durationMs 那份快照可能还没轮询到）
            val playerDuration = if (player.duration == C.TIME_UNSET) 0L else player.duration
            audioViewModel.updateAudioInfo(
                uri = currentUri,
                info = merged,
                // 没有封面就写空串（而不是 null）：空串表示「解析过了，确实没有」，
                // null 留给「还没解析」，缓存判断据此决定要不要重解析
                localCoverPath = coverPath.orEmpty(),
                duration = playerDuration,
                isDetailsLoaded = true,
            )
        }
        infoLoading = false
    }

    // 换歌时把歌词重新展开（有歌词就默认看得见），收起只由用户点图标决定
    LaunchedEffect(currentUri) { showLyrics = true }

    val lyrics = remember(audioInfo?.lyrics) { parseLrc(audioInfo?.lyrics) }
    // 兜底：有歌词但没有 `[mm:ss]` 时间轴（内嵌歌词与外挂 .lrc 都有这种写法），
    // 解析不出条目时就整段平铺显示，否则用户看到的是「什么都没有」，和没歌词一样
    val plainLyrics = remember(audioInfo?.lyrics) {
        audioInfo?.lyrics?.takeIf { it.isNotBlank() && parseLrc(it).isEmpty() }
    }
    val hasLyrics = lyrics.isNotEmpty() || plainLyrics != null
    val duration = durationMs
    val lyricsModifier = Modifier
        .fillMaxWidth()
        .height(140.dp)
        .padding(top = 12.dp)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = audioInfo?.title?.takeIf { it.isNotBlank() } ?: currentFileName,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.phone_action_back),
                        )
                    }
                },
                actions = {
                    if (hasLyrics) {
                        IconButton(onClick = { showLyrics = !showLyrics }) {
                            Icon(
                                imageVector = PhoneIcons.Music,
                                contentDescription = stringResource(R.string.phone_audio_lyrics),
                                tint = if (showLyrics) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                            )
                        }
                    }
                    IconButton(onClick = { showPlaylist = true }) {
                        Icon(
                            imageVector = PhoneIcons.Playlist,
                            contentDescription = stringResource(R.string.phone_audio_playlist),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            AlbumArt(
                audioInfo = audioInfo,
                loading = infoLoading,
                modifier = Modifier
                    .weight(1f)
                    .aspectRatio(1f)
                    .padding(vertical = 16.dp),
            )

            Text(
                text = audioInfo?.title?.takeIf { it.isNotBlank() }
                    ?: currentFileName.ifBlank { stringResource(R.string.phone_audio_unknown_title) },
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                text = listOfNotNull(
                    audioInfo?.artist?.takeIf { it.isNotBlank() },
                    audioInfo?.album?.takeIf { it.isNotBlank() },
                ).joinToString(" · ").ifBlank { stringResource(R.string.phone_audio_unknown_artist) },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
            )

            if (showLyrics && hasLyrics) {
                if (lyrics.isNotEmpty()) {
                    LyricsBox(
                        lyrics = lyrics,
                        positionMs = positionMs,
                        modifier = lyricsModifier,
                    )
                } else {
                    PlainLyricsBox(text = plainLyrics.orEmpty(), modifier = lyricsModifier)
                }
            }

            // 这里用「收 value 的旧重载」而不是新的 `SliderState`：新 API 的
            // `rememberSliderState(value = ...)` 把 value 当成 remember 的 key，
            // 进度条每 500ms 轮询一次就会重建状态、把正在拖拽的手势打断。
            Slider(
                value = if (isDragging) dragValue else positionMs.toFloat(),
                onValueChange = { value ->
                    isDragging = true
                    dragValue = value
                },
                onValueChangeFinished = {
                    player.seekTo(dragValue.toLong())
                    positionMs = dragValue.toLong()
                    isDragging = false
                },
                valueRange = 0f..duration.toFloat().coerceAtLeast(1f),
                enabled = duration > 0L,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
            )
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = Tools.formatTime(if (isDragging) dragValue.toLong() else positionMs),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = Tools.formatTime(duration),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    onClick = { player.seekToPreviousMediaItem() },
                    enabled = currentIndex > 0,
                ) {
                    Icon(
                        imageVector = PhoneIcons.SkipPrevious,
                        contentDescription = stringResource(R.string.phone_audio_previous),
                        modifier = Modifier.size(36.dp),
                    )
                }
                FilledIconButton(
                    onClick = { if (isPlaying) player.pause() else player.play() },
                    modifier = Modifier
                        .padding(horizontal = 24.dp)
                        .size(64.dp),
                ) {
                    Icon(
                        imageVector = if (isPlaying) PhoneIcons.Pause else Icons.Filled.PlayArrow,
                        contentDescription = stringResource(
                            if (isPlaying) R.string.phone_audio_pause else R.string.phone_audio_play
                        ),
                        modifier = Modifier.size(32.dp),
                    )
                }
                IconButton(
                    onClick = { player.seekToNextMediaItem() },
                    enabled = currentIndex < playlist.lastIndex,
                ) {
                    Icon(
                        imageVector = PhoneIcons.SkipNext,
                        contentDescription = stringResource(R.string.phone_audio_next),
                        modifier = Modifier.size(36.dp),
                    )
                }
            }
        }
    }

    if (showPlaylist) {
        PhonePlaylistSheet(
            playlist = playlist,
            currentIndex = currentIndex,
            onDismiss = { showPlaylist = false },
            onSelect = { index ->
                player.seekTo(index, 0L)
                player.play()
                showPlaylist = false
            },
        )
    }
}

/**
 * 底部弹出的播放列表。
 *
 * 单独抽一个函数（而不是直接写在播放页里）：`ModalBottomSheet` 需要 `ExperimentalMaterial3Api`，
 * 把 opt-in 收在这一层，播放页主体就不用整段带着实验 API 跑了。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PhonePlaylistSheet(
    playlist: List<AudioItem>,
    currentIndex: Int,
    onDismiss: () -> Unit,
    onSelect: (Int) -> Unit,
) {
    // 仍用 `rememberModalBottomSheetState`：它默认就是 `SheetValue.Hidden` 起始
    // （正是新 `rememberBottomSheetState` 的推荐用法），`skipPartiallyExpanded = true`
    // 也避免露出「半展开」那一档。1.5.0-alpha29 里它只是被标了废弃、行为没变。
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        PlaylistSheet(
            playlist = playlist,
            currentIndex = currentIndex,
            onSelect = onSelect,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PhoneAudioEmpty(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.ui_label_music)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.phone_action_back),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(32.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(R.string.phone_audio_playlist_empty),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * 封面：优先用回写到 `audio_cache` 的本地封面文件，其次用刚解析出来的内嵌图片，
 * 都没有就给一个音符占位（不给空盒子，否则大屏上会看起来很怪）。
 */
@Composable
private fun AlbumArt(
    audioInfo: AudioInfo?,
    loading: Boolean,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val coverPath = audioInfo?.localCoverPath?.takeIf { it.isNotBlank() }
    val artwork = remember(audioInfo?.artworkData) {
        audioInfo?.artworkData?.takeIf { it.isNotEmpty() }?.let { createArtworkBitmap(it) }
    }
    // 封面文件可能已经被清理，解码失败就退回占位图，不然 Coil 会先闪一下再报错
    val fileBitmap = remember(coverPath) {
        coverPath?.let { runCatching { BitmapFactory.decodeFile(it) }.getOrNull() }
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
        contentAlignment = Alignment.Center,
    ) {
        val bitmap = artwork ?: fileBitmap
        when {
            bitmap != null -> Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )

            // 本地路径还在但解码失败时让 Coil 再试一次（它自己也带磁盘缓存）
            coverPath != null -> AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(File(coverPath))
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )

            else -> Icon(
                imageVector = PhoneIcons.Music,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                // 读取元数据期间小一号，读完确实没有封面再放大
                modifier = Modifier.size(if (loading) 72.dp else 96.dp),
            )
        }
    }
}

/** 歌词：固定高度的滚动列表，跟着播放位置高亮并自动滚到中间 */
@Composable
private fun LyricsBox(
    lyrics: List<LyricEntry>,
    positionMs: Long,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val currentIndex = lyricIndexAt(lyrics, positionMs)

    LaunchedEffect(currentIndex, lyrics) {
        if (currentIndex >= 0) {
            // 让当前行大致落在中间（上方留两行），短列表时不会越界
            listState.animateScrollToItem((currentIndex - 2).coerceAtLeast(0))
        }
    }

    LazyColumn(
        state = listState,
        modifier = modifier.clip(RoundedCornerShape(16.dp)),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        itemsIndexed(lyrics) { index, entry ->
            val current = index == currentIndex
            Text(
                text = entry.text.ifBlank { " " },
                style = if (current) {
                    MaterialTheme.typography.titleMedium
                } else {
                    MaterialTheme.typography.bodyMedium
                },
                color = if (current) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
            )
        }
    }
}

/** 没有时间轴的歌词：整段平铺、可以上下翻，但不跟随播放位置高亮 */
@Composable
private fun PlainLyricsBox(
    text: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
        )
    }
}

@Composable
private fun PlaylistSheet(
    playlist: List<AudioItem>,
    currentIndex: Int,
    onSelect: (Int) -> Unit,
) {
    LazyColumn(modifier = Modifier.fillMaxWidth()) {
        itemsIndexed(playlist, key = { index, item -> "$index-${item.uri}" }) { index, item ->
            val current = index == currentIndex
            ListItem(
                onClick = { onSelect(index) },
                leadingContent = {
                    if (current) {
                        Icon(
                            imageVector = PhoneIcons.Music,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                },
                trailingContent = {
                    Text(
                        text = (index + 1).toString(),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = item.fileName,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = if (current) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                )
            }
        }
    }
}

/** 音频的 ExoPlayer：数据源工厂按协议选（与视频共用一个实现），只额外设了音频属性 */
private fun createAudioPlayer(
    context: Context,
    sampleUri: String,
    dataSourceType: String,
): ExoPlayer {
    val dataSourceFactory = selectedDataSourceFactory(
        mediaUri = sampleUri,
        dataSourceType = dataSourceType,
        context = context,
    )
    return ExoPlayer.Builder(context)
        .setMediaSourceFactory(DefaultMediaSourceFactory(dataSourceFactory))
        .build()
        .apply {
            playWhenReady = true
            repeatMode = Player.REPEAT_MODE_OFF
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                /* handleAudioFocus = */ true,
            )
            setSeekBackIncrementMs(30_000)
            setSeekForwardIncrementMs(30_000)
        }
}

/** 按扩展名给出 MIME（`extractAudioInfoAndLyricsFromStream` 靠它决定临时文件后缀） */
private fun audioMimeTypeOf(uri: String): String =
    when (Tools.extractFileExtension(uri.substringBefore('?')).lowercase()) {
        "flac" -> "audio/flac"
        "wav" -> "audio/wav"
        "aac" -> "audio/aac"
        "m4a" -> "audio/mp4"
        else -> "audio/mpeg"
    }

/**
 * 同目录歌词文件：先算出 `同名.lrc` 的地址，再读成文本；读不到就返回 null（静默）。
 *
 * `mimeType` 传 `text/plain` 是为了让各协议的 `openXxxFileInputStream` 走「小文件」那条路。
 */
private suspend fun loadSidecarLyrics(
    context: Context,
    audioUri: String,
    dataSourceType: String,
    fileName: String,
): String? {
    val lyricUri = PhoneMediaLogic.siblingUri(audioUri, PhoneMediaLogic.lyricSiblingName(fileName))
        ?: return null
    return withContext(Dispatchers.IO) {
        runCatching {
            openMediaStream(context, lyricUri, dataSourceType, LYRIC_MIME_TYPE)?.use { it.readBytes() }
        }.onFailure { Log.w(LOG_TAG, "读取外挂歌词失败: $lyricUri", it) }
            .getOrNull()
            ?.let { PhoneMediaLogic.decodeLyricText(it) }
    }
}

/**
 * 按 scheme 打开媒体流（音频本身，或它的外挂 `.lrc`）。
 *
 * 与电视端的差别只有一处：`file://` 走 [SmbUtils.openLocalFileInputStream]（`java.io.File`），
 * 不再走 `ContentResolver` —— 手机端拿的是「所有文件访问」权限，目录里的绝对路径本来就可读。
 */
private suspend fun openMediaStream(
    context: Context,
    uri: String,
    dataSourceType: String,
    mimeType: String,
): InputStream? = withContext(Dispatchers.IO) {
    runCatching {
        when (uri.toUri().scheme?.lowercase()) {
            "smb" -> SmbUtils.openSmbFileInputStream(uri.toUri(), mimeType)
            "ftp" -> SmbUtils.openFtpFileInputStream(uri.toUri(), mimeType)
            "nfs" -> SmbUtils.openNfsFileInputStream(uri.toUri(), mimeType)
            "file" -> SmbUtils.openLocalFileInputStream(uri.toUri())
            "http", "https" -> when (dataSourceType) {
                "WEBDAV" -> SmbUtils.openWebDavFileInputStream(uri.toUri(), mimeType)
                "HTTP" -> SmbUtils.openHTTPLinkXmlInputStream(uri, mimeType)
                else -> java.net.URL(uri).openStream()
            }

            else -> null
        }
    }.onFailure { Log.w(LOG_TAG, "打开媒体流失败: $uri", it) }.getOrNull()
}
