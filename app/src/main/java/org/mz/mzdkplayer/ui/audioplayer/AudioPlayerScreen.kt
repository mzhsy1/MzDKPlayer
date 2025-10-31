package org.mz.mzdkplayer.ui.audioplayer


import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*

import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color

import androidx.compose.ui.platform.LocalContext

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.mz.mzdkplayer.logic.model.AudioInfo
import org.mz.mzdkplayer.logic.model.AudioItem

import org.mz.mzdkplayer.tool.SmbUtils
import org.mz.mzdkplayer.tool.Tools
import org.mz.mzdkplayer.tool.createArtworkBitmap
import org.mz.mzdkplayer.tool.extractAudioInfoAndLyricsFromStream
import org.mz.mzdkplayer.tool.handleDPadKeyEvents

import org.mz.mzdkplayer.ui.audioplayer.components.*
import org.mz.mzdkplayer.ui.screen.vm.AudioPlayerViewModel
import org.mz.mzdkplayer.ui.videoplayer.BackPress


import java.io.InputStream
import java.net.URL
import java.util.Locale

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
            Log.d(
                "artworkData",
                "Cover bitmap created with size: ${coverBitmap?.width}x${coverBitmap?.height}"
            )
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
    return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
}


// --- 主要 Composable ---

@OptIn(UnstableApi::class)
@Composable
fun AudioPlayerScreen(
    mediaUri: String,
    dataSourceType: String,
    fileName: String,
    extraList: List<AudioItem>,
    currentIndex: String
) {
    val context = LocalContext.current
    val exoPlayer = rememberAudioPlayer(context, mediaUri, dataSourceType)
    val audioPlayerState = rememberAudioPlayerState(hideSeconds = 6)
    val audioPlayerViewModel: AudioPlayerViewModel = viewModel()
    var showToast by remember { mutableStateOf(false) }
    var backPressState by remember { mutableStateOf<BackPress>(BackPress.Idle) }
    var contentCurrentPosition by remember { mutableLongStateOf(0L) }
    var isPlaying: Boolean by remember { mutableStateOf(exoPlayer.isPlaying) }
    var sampleMimeType: String by remember { mutableStateOf("") }

    var audioMetadata by remember { mutableStateOf(AudioMetadata()) }
    var audioInfo: AudioInfo? by remember { mutableStateOf(null) } // 关键：存储 audioInfo
    var currentMediaUri by remember {  mutableStateOf(mediaUri) }
    var currentFileName by remember { mutableStateOf(fileName) } // 替换原来的 fileName 状态
    BuilderMzAudioPlayer(context, currentMediaUri, exoPlayer, dataSourceType,extraList,currentIndex)

    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }


    // 加载音频信息和歌词
    LaunchedEffect(currentMediaUri, sampleMimeType) {
        Log.e("sampleMimeType", sampleMimeType)
        if (sampleMimeType.isNotEmpty()) {
            withContext(Dispatchers.IO) {
                try {
                    val inputStream: InputStream? = when (currentMediaUri.toUri().scheme?.lowercase()) {
                        "smb" -> SmbUtils.openSmbFileInputStream(currentMediaUri.toUri(),sampleMimeType)
                        "http", "https" -> {
                            when (dataSourceType) {
                                "WEBDAV" -> SmbUtils.openWebDavFileInputStream(currentMediaUri.toUri(),sampleMimeType)
                                "HTTP" -> SmbUtils.openHTTPLinkXmlInputStream(currentMediaUri,sampleMimeType)
                                else -> URL(currentMediaUri).openStream()
                            }
                        }

                        "file" -> context.contentResolver.openInputStream(currentMediaUri.toUri())
                            ?: throw java.io.IOException("Could not open file input stream for $currentMediaUri")

                        "ftp" -> SmbUtils.openFtpFileInputStream(currentMediaUri.toUri(),sampleMimeType)
                        "nfs" -> SmbUtils.openNfsFileInputStream(currentMediaUri.toUri(),sampleMimeType)
                        else -> {
                            Log.w("AudioPlayerScreen", "Unsupported scheme for URI: $currentMediaUri")
                            null
                        }
                    }

                    Log.d("AudioPlayerScreen", "Opened input stream")

                    inputStream.use { stream ->
                        // 关键：调用工具函数获取 audioInfo，其中包含 lyrics
                        val info =
                            extractAudioInfoAndLyricsFromStream(context, stream, sampleMimeType)
                        audioInfo = info // 更新状态

                        Log.i("AudioPlayerScreen", "Loaded audio info and lyrics")
                    }
                } catch (e: Exception) {
                    Log.e(
                        "AudioPlayerScreen",
                        "Failed to load audio info or lyrics from $currentMediaUri",
                        e
                    )
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

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                super.onMediaItemTransition(mediaItem, reason)
                Log.d("onMediaItemTransition", mediaItem?.localConfiguration?.uri.toString())
                currentMediaUri = mediaItem?.localConfiguration?.uri.toString()
                audioInfo = null // 重置音频信息

                // ✅ 提取并更新文件名
                currentFileName = Tools.extractFileNameFromUri(currentMediaUri)
            }

            override fun onIsPlayingChanged(isExoPlaying: Boolean) {
                isPlaying = isExoPlaying
            }

            override fun onPlayerError(error: PlaybackException) {
                super.onPlayerError(error)
            }
        })
        delay(500)
        audioMetadata = extractAudioMetadataFromPlayer(exoPlayer)
    }

    LaunchedEffect(Unit) {
        while (true) {
            delay(300) // 更频繁地更新进度
            contentCurrentPosition = exoPlayer.currentPosition
        }
    }
    // 显示 "再按一次退出" Toast
    if (showToast) {
        Toast.makeText(context, "再按一次退出", Toast.LENGTH_SHORT).show()
        showToast = false
    }

    // 处理双击返回退出逻辑
    LaunchedEffect(key1 = backPressState) {
        if (backPressState == BackPress.InitialTouch) {
            delay(2000) // 2秒延迟
            backPressState = BackPress.Idle // 重置状态
        }
    }
    BackHandler(backPressState == BackPress.Idle) {
        backPressState = BackPress.InitialTouch
        showToast = true
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
                .fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. 使用 remember 来缓存解码后的 Bitmap
            val coverBitmap: Bitmap? = remember(audioInfo?.artworkData) { // 依赖 artworkData
                // 当 artworkData 改变时，才重新执行 lambda
                audioInfo?.artworkData?.let { data ->
                    try {
                        // 假设 createArtworkBitmap 是一个纯函数，只做解码
                        // 为了性能，也可以考虑在这里直接用 BitmapFactory.decodeByteArray
                        createArtworkBitmap(data) // 调用工具函数
                    } catch (e: Exception) {
                        Log.e("BitmapCache", "Failed to decode bitmap", e)
                        null
                    }
                }
            }
            // 优化 5: 专辑封面部分
            Box(
                modifier = Modifier
                    .fillMaxHeight(0.5f)
                    .fillMaxWidth(0.25f)
                    .offset(x = 56.dp, y = (-38).dp), contentAlignment = Alignment.CenterStart
            ) { // 使用 wrapContentSize 并居中对齐到末端

                AlbumCoverDisplay(coverBitmap)

            }
            Column(  modifier = Modifier
                .fillMaxHeight(0.8f)
                .fillMaxWidth(0.75f)) {

                Box(
                    modifier = Modifier
                        .fillMaxHeight(0.18f)
                        .fillMaxWidth()
                        .offset(90.dp, y = (-32).dp), contentAlignment = Alignment.CenterStart
                ) {
                    Column {
                        Text(
                            text = audioInfo?.title ?: "未知标题",
                            style = MaterialTheme.typography.headlineSmall,
                            color = Color.White,
                            maxLines = 1
                        )
                        Row (Modifier.padding(top = 8.dp)){
                            Text(text = "艺术家 : ${audioInfo?.artist ?: "未知歌手"}", color = Color.Gray, fontSize = 16.sp, maxLines = 1)

                            Text(text = " · ", color = Color.Gray)
                            Text(text = "专辑 : ${audioInfo?.album ?: "未知专辑"}", color = Color.Gray,fontSize = 16.sp, maxLines = 1)
                        }
                    }
                }
                // 优化 2: 歌词部分 - 使用 audioInfo?.lyrics
                // 优化 3: 使用 weight(1f) 让歌词部分占据剩余空间
                Box(
                    modifier = Modifier
                        .fillMaxHeight(0.82f)
                        .fillMaxWidth()
                        .offset(90.dp, y = (-30).dp), contentAlignment = Alignment.CenterStart
                ) {

                    // 使用您提供的 LRC 解析器
                    val parsedLyrics =
                        remember(audioInfo?.lyrics) { parseLrc(audioInfo?.lyrics) }
                    // 优化 4: ScrollableLyricsView 现在会撑满其父 Box
                    ScrollableLyricsView(
                        currentPosition = contentCurrentPosition.milliseconds,
                        parsedLyrics = parsedLyrics
                    )


                }
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
                    currentFileName,
                    "${audioInfo?.bitsPerSample ?: "--"} bit - " +
                            "${
                                audioInfo?.sampleRate?.let {
                                    String.format(
                                        Locale.getDefault(),
                                        "%.1f kHz",
                                        it.toInt() / 1000.0
                                    )
                                } ?: "未知采样率"
                            } - " +
                            "${audioInfo?.bit ?: "--"} Kbps",

                    "时长: ${audioMetadata.duration}", // 直接使用已格式化的字符串
                    audioPlayerViewModel,
                    safeDurationMs.milliseconds // 传递安全的 Duration
                )
            },
            atpFocus = audioPlayerViewModel.atpFocus
        )
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


