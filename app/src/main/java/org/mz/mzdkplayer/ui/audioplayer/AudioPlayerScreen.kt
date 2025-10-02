package org.mz.mzdkplayer.ui.audioplayer

import LyricEntry
import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import androidx.annotation.OptIn
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.C
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.tv.material3.Text
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.mz.mzdkplayer.R
import org.mz.mzdkplayer.tool.AudioInfo
import org.mz.mzdkplayer.tool.SmbUtils
import org.mz.mzdkplayer.tool.createArtworkBitmap
import org.mz.mzdkplayer.tool.extractAudioInfoAndLyricsFromStream
import org.mz.mzdkplayer.tool.handleDPadKeyEvents
import org.mz.mzdkplayer.ui.audioplayer.components.*
import org.mz.mzdkplayer.ui.screen.vm.AudioPlayerViewModel
import org.mz.mzdkplayer.ui.videoplayer.BackPress
import org.mz.mzdkplayer.ui.videoplayer.components.BuilderMzPlayer
import org.mz.mzdkplayer.ui.videoplayer.components.rememberPlayer
import parseLrc
import java.io.InputStream
import java.net.URL
import kotlin.math.abs
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

// --- 数据类 ---
data class AudioMetadata(
    val title: String = "未知标题",
    val artist: String = "未知艺术家",
    val album: String = "未知专辑",
    val year: String = "",
    val genre: String = "",
    val duration: String = "", // 注意：这个已经是格式化后的字符串了
    val coverArt: Bitmap? = null,
    val extras: Bundle? = null,
    // 注意：不再直接存储歌词，因为我们从 audioInfo 获取
)

// --- 工具函数 ---

@OptIn(UnstableApi::class)
fun extractAudioMetadataFromPlayer(exoPlayer: ExoPlayer): AudioMetadata {
    val mediaMetadata = exoPlayer.mediaMetadata

    val title = mediaMetadata.title?.toString() ?: "未知标题"
    val artist = mediaMetadata.artist?.toString() ?: "未知艺术家"
    val album = mediaMetadata.albumTitle?.toString() ?: "未知专辑"
    val recordingYear = mediaMetadata.releaseYear?.toString() ?: ""
    val genre = mediaMetadata.genre?.toString() ?: ""
    val durationMs = exoPlayer.duration
    val extras = mediaMetadata.extras

    var coverBitmap: Bitmap? = null
    try {
        val artworkData = mediaMetadata.artworkData
        if (artworkData != null) {
            coverBitmap = BitmapFactory.decodeByteArray(artworkData, 0, artworkData.size)
            Log.d("artworkData", "Cover bitmap created with size: ${coverBitmap?.width}x${coverBitmap?.height}")
        } else {
            Log.d("artworkData", "No artwork data available")
        }
    } catch (e: Exception) {
        e.printStackTrace()
        Log.e("artworkData", "Error decoding artwork data", e)
    }

    // 格式化时长，处理 C.TIME_UNSET (-1) 的情况
    val formattedDuration = if (durationMs != C.TIME_UNSET) {
        formatDuration(durationMs.toInt())
    } else {
        "--:--"
    }

    return AudioMetadata(
        title = title,
        artist = artist,
        album = album,
        year = recordingYear,
        genre = genre,
        duration = formattedDuration, // 使用格式化后的字符串
        coverArt = coverBitmap,
        extras = extras
    )
}

fun formatDuration(durationMs: Int?): String {
    // 处理 null 或无效值
    if (durationMs == null || durationMs <= 0) return "--:--"
    val totalSeconds = durationMs / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}


// --- 主要 Composable ---

@OptIn(UnstableApi::class)
@Composable
fun AudioPlayerScreen(mediaUri: String, dataSourceType: String) {
    val context = LocalContext.current
    val exoPlayer = rememberPlayer(context, mediaUri, dataSourceType)
    val audioPlayerState = rememberAudioPlayerState(hideSeconds = 6)
    val audioPlayerViewModel: AudioPlayerViewModel = viewModel()
    var showToast by remember { mutableStateOf(false) }
    var backPressState by remember { mutableStateOf<BackPress>(BackPress.Idle) }
    var contentCurrentPosition by remember { mutableLongStateOf(0L) }
    var isPlaying: Boolean by remember { mutableStateOf(exoPlayer.isPlaying) }
    var sampleMimeType: String by remember { mutableStateOf("") }

    var audioMetadata by remember { mutableStateOf(AudioMetadata()) }
    var audioInfo: AudioInfo? by remember { mutableStateOf(null) } // 关键：存储 audioInfo

    BuilderMzPlayer(context, mediaUri, exoPlayer, dataSourceType)

    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }

    LaunchedEffect(exoPlayer) {
        while (true) {
            delay(600)
        }
    }

    // 加载音频信息和歌词
    LaunchedEffect(mediaUri, sampleMimeType) {
        Log.d("sampleMimeType", sampleMimeType)
        if (sampleMimeType.isNotEmpty()) {
            withContext(Dispatchers.IO) {
                try {
                    val inputStream: InputStream? = when (mediaUri.toUri().scheme?.lowercase()) {
                        "smb" -> SmbUtils.openSmbFileInputStream(mediaUri.toUri())
                        "http", "https" -> {
                            when (dataSourceType) {
                                "WEBDAV" -> SmbUtils.openWebDavFileInputStream(mediaUri.toUri())
                                "HTTP" -> SmbUtils.openHTTPLinkXmlInputStream(mediaUri)
                                else -> URL(mediaUri).openStream()
                            }
                        }
                        "file" -> context.contentResolver.openInputStream(mediaUri.toUri())
                            ?: throw java.io.IOException("Could not open file input stream for $mediaUri")
                        "ftp" -> SmbUtils.openFtpFileInputStream(mediaUri.toUri())
                        "nfs" -> SmbUtils.openNfsFileInputStream(mediaUri.toUri())
                        else -> {
                            Log.w("AudioPlayerScreen", "Unsupported scheme for URI: $mediaUri")
                            null
                        }
                    }

                    Log.d("AudioPlayerScreen", "Opened input stream")

                    inputStream.use { stream ->
                        // 关键：调用工具函数获取 audioInfo，其中包含 lyrics
                        val info = extractAudioInfoAndLyricsFromStream(context, stream, sampleMimeType)
                        audioInfo = info // 更新状态
                        Log.i("AudioPlayerScreen", "Loaded audio info and lyrics")
                    }
                } catch (e: Exception) {
                    Log.e("AudioPlayerScreen", "Failed to load audio info or lyrics from $mediaUri", e)
                }
            }
        }
    }


    LaunchedEffect(exoPlayer) {
        exoPlayer.addListener(object : Player.Listener {
            override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
                super.onMediaMetadataChanged(mediaMetadata)
                audioMetadata = extractAudioMetadataFromPlayer(exoPlayer)
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_READY -> {
                        sampleMimeType = exoPlayer.audioFormat?.sampleMimeType.toString()
                        // Player 准备好后，更新元数据（可能包含更准确的时长）
                        audioMetadata = extractAudioMetadataFromPlayer(exoPlayer)
                    }
                    Player.STATE_BUFFERING -> {}
                    Player.STATE_ENDED -> {}
                    Player.STATE_IDLE -> {}
                }
            }

            override fun onIsPlayingChanged(isExoPlaying: Boolean) {
                isPlaying = isExoPlaying
            }
        })
        delay(500)
        audioMetadata = extractAudioMetadataFromPlayer(exoPlayer)
    }

    LaunchedEffect(Unit) {
        while (true) {
            delay(200) // 更频繁地更新进度
            contentCurrentPosition = exoPlayer.currentPosition
        }
    }

    val pulseState = rememberAudioPlayerPulseState()

    Box(
        Modifier
            .dPadEvents(exoPlayer, audioPlayerState, pulseState, audioPlayerViewModel)
            .background(Color.Black)
            .focusable()
            .fillMaxSize()
    ) {
        val focusRequester = remember { FocusRequester() }

        // 创建水平布局容器
        // 优化 1: 使用 fillMaxSize() 确保 Row 占据整个可用空间
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 优化 5: 专辑封面部分 - 移到右侧
            Box(modifier = Modifier.fillMaxHeight(0.5f).fillMaxWidth(0.25f),contentAlignment= Alignment.CenterStart) { // 使用 wrapContentSize 并居中对齐到末端

                    AlbumCoverDisplay(createArtworkBitmap(audioInfo?.artworkData))

            }
            // 优化 2: 歌词部分 - 使用 audioInfo?.lyrics
            // 优化 3: 使用 weight(1f) 让歌词部分占据剩余空间
            Box(modifier = Modifier.fillMaxHeight(0.5f).fillMaxWidth(0.75f).offset(30.dp),contentAlignment= Alignment.CenterStart) {
                // 使用您提供的 LRC 解析器
                val parsedLyrics = remember(audioInfo?.lyrics) { parseLrc(audioInfo?.lyrics) }
                // 优化 4: ScrollableLyricsView 现在会撑满其父 Box
                ScrollableLyricsView(currentPosition = contentCurrentPosition.milliseconds, parsedLyrics = parsedLyrics)
            }

            // Spacer(Modifier.width(16.dp)) // 添加间距


        }

        // --- 修复点：安全地处理 duration ---
        // 确保传递给 UI 组件的 duration 是有效的
        val safeDurationMs = if (exoPlayer.duration != C.TIME_UNSET) exoPlayer.duration else 0L
        val safeCurrentPositionMs = contentCurrentPosition

        // 控制层叠在底部
        AudioPlayerOverlay(
            modifier = Modifier.align(Alignment.BottomCenter),
            focusRequester = focusRequester,
            state = audioPlayerState,
            isPlaying = isPlaying,
            subtitles = { },
            controls = {
                AudioPlayerControls(
                    isPlaying,
                    safeCurrentPositionMs, // 使用安全的当前位置
                    exoPlayer,
                    audioPlayerState,
                    focusRequester,
                    audioInfo?.title ?: audioMetadata.title,
                    "${audioInfo?.artist ?: audioMetadata.artist} - ${audioInfo?.album ?: audioMetadata.album}",
                    "时长: ${audioMetadata.duration}", // 直接使用已格式化的字符串
                    audioPlayerViewModel,
                    safeDurationMs.milliseconds // 传递安全的 Duration
                )
            },
            atpFocus = audioPlayerViewModel.atpFocus
        )
    }
}

@Composable
fun AlbumCoverDisplay(coverArt: Bitmap?) {
    if (coverArt != null) {
        Image(
            bitmap = coverArt.asImageBitmap(),
            contentDescription = "专辑封面",
            modifier = Modifier
                .size(240.dp), // 保持封面大小
            contentScale = ContentScale.Fit
        )
    } else {
        Image(
            painter = painterResource(id = R.drawable.album),
            contentDescription = "默认专辑封面",
            modifier = Modifier
                .size(240.dp), // 保持封面大小
            contentScale = ContentScale.Fit
        )
    }
}

// --- 歌词组件 ---

/**
 * 可滚动的歌词视图 (使用已解析的歌词列表)，并尝试将高亮行保持在中间。
 *
 * @param currentPosition 当前播放位置 (Duration)
 * @param parsedLyrics 已解析的歌词列表 (包含时间戳)
 */
@SuppressLint("UnrememberedMutableState")
@Composable
fun ScrollableLyricsView(currentPosition: Duration, parsedLyrics: List<LyricEntry>) {
    val lazyListState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    var lastHighlightedIndex by remember { mutableIntStateOf(-1) }

    // 查找当前应高亮的歌词索引
    val highlightedIndex by derivedStateOf {
        if (parsedLyrics.isEmpty()) return@derivedStateOf -1
        var index = parsedLyrics.indexOfLast { it.time <= currentPosition }
        index = index.coerceAtLeast(0)
        index
    }

    // 自动滚动到高亮行，并尽量使其居中
    LaunchedEffect(highlightedIndex) {
        if (highlightedIndex >= 0 && highlightedIndex != lastHighlightedIndex) {
            lastHighlightedIndex = highlightedIndex
            coroutineScope.launch {
                // 尝试计算使目标项大致居中的偏移量。
                // 假设每行文本大约占用 30dp + padding (约为 44px on mdpi, ~66px on xhdpi etc.)
                // 我们希望它出现在列表可视区域的大致中间。
                // 一种常见的近似做法是滚动到该项，然后向上偏移半个列表高度。
                // lazyListState.layoutInfo.viewportEndOffset - viewportStartOffset 是可视区总高度(px)
                // 我们假定单个item平均高度约60px用于计算 offset.
                val avgItemHeightPx = 60 // 启发式估计，可根据实际UI调整
                val viewportHeightPx = lazyListState.layoutInfo.viewportEndOffset - lazyListState.layoutInfo.viewportStartOffset
                val estimatedCenterOffset = (viewportHeightPx / 2) - (avgItemHeightPx / 2)

                // animateScrollToItem 第二个参数是相对于该项顶部的偏移量，
                // 负数表示向上滚动更多，从而让该项处于更中心的位置。
                lazyListState.animateScrollToItem(highlightedIndex, -estimatedCenterOffset)
            }
        }
    }

    LazyColumn(
        state = lazyListState,
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.Start
    ) {
        itemsIndexed(parsedLyrics) { index, entry ->
            Text(
                text = entry.text.ifEmpty { "..." },
                fontSize = if (index == highlightedIndex) 18.sp else 16.sp,
                fontWeight = if (index == highlightedIndex) FontWeight.Bold else FontWeight.Normal,
                color = if (index == highlightedIndex) Color.White else Color.Gray,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .alpha(if (index == highlightedIndex) 1f else 0.7f)
            )
        }
    }
}


// --- D-Pad 事件修饰符 ---
private fun Modifier.dPadEvents(
    exoPlayer: ExoPlayer,
    audioPlayerState: AudioPlayerState,
    pulseState: AudioPlayerPulseState,
    audioPlayerViewModel: AudioPlayerViewModel
): Modifier = handleDPadKeyEvents(
    onLeft = {
        if (!audioPlayerState.controlsVisible) {
            exoPlayer.seekBack()
            pulseState.setType(AudioPlayerPulse.Type.BACK)
        }
    },
    onRight = {
        if (!audioPlayerState.controlsVisible) {
            exoPlayer.seekForward()
            pulseState.setType(AudioPlayerPulse.Type.FORWARD)
        }
    },
    onUp = { if (audioPlayerViewModel.atpFocus) audioPlayerState.showControls() },
    onDown = { if (audioPlayerViewModel.atpFocus) audioPlayerState.showControls(); },
    onEnter = {
        exoPlayer.pause()
        audioPlayerState.showControls()
    }
)


// --- 控件 ---
@OptIn(UnstableApi::class)
@Composable
fun AudioPlayerControls(
    isPlaying: Boolean,
    contentCurrentPosition: Long, // 安全的当前位置 (ms)
    exoPlayer: ExoPlayer,
    state: AudioPlayerState,
    focusRequester: FocusRequester,
    title: String?,
    secondaryText: String,
    tertiaryText: String,
    audioPlayerViewModel: AudioPlayerViewModel,
    contentDuration: Duration = 0.milliseconds // 添加 contentDuration 参数，默认值
) {
    val onPlayPauseToggle = { shouldPlay: Boolean ->
        if (shouldPlay) {
            exoPlayer.play()
        } else {
            exoPlayer.pause()
        }
    }

    AudioPlayerMainFrame(
        mediaTitle = {
            AudioPlayerMediaTitle(
                title = title,
                secondaryText = secondaryText,
                tertiaryText = tertiaryText,
                type = AudioPlayerMediaTitleType.DEFAULT
            )
        },
        mediaActions = {
            Row(
                modifier = Modifier.padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AudioPlayerControlsIcon(
                    icon = painterResource(id = R.drawable.baseline_hd_24),
                    state = state,
                    isPlaying = isPlaying,
                    onClick = {
                        // 可以扩展为显示视频选项或切换音频质量
                    }
                )
                AudioPlayerControlsIcon(
                    modifier = Modifier.padding(start = 12.dp),
                    icon = painterResource(id = R.drawable.baseline_speaker_24),
                    state = state,
                    isPlaying = isPlaying,
                    onClick = {
                        // 可以扩展为显示音频设置
                    }
                )
            }
        },
        seeker = {
            AudioPlayerSeeker(
                focusRequester,
                state,
                isPlaying,
                onPlayPauseToggle,
                onSeek = { progressRatio ->
                    // 确保 duration 有效
                    val durationMs = if (exoPlayer.duration != C.TIME_UNSET) exoPlayer.duration else 0L
                    val seekPosition = (durationMs * progressRatio).toLong()
                    exoPlayer.seekTo(seekPosition)
                },
                contentProgress = contentCurrentPosition.milliseconds, // 使用安全的当前位置
                contentDuration = contentDuration // 使用传入的安全 Duration
            )
        },
        more = null
    )
}



