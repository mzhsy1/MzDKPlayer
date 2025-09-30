package org.mz.mzdkplayer.ui.audioplayer


import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import androidx.annotation.OptIn
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource

import androidx.compose.ui.unit.dp

import androidx.core.net.toUri
import androidx.lifecycle.viewmodel.compose.viewModel

import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi

import androidx.media3.exoplayer.ExoPlayer

import kotlinx.coroutines.Dispatchers

import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.mz.mzdkplayer.R
import org.mz.mzdkplayer.tool.AudioInfo


import org.mz.mzdkplayer.tool.SmbUtils
import org.mz.mzdkplayer.tool.extractAudioInfoAndLyricsFromStream

import org.mz.mzdkplayer.tool.handleDPadKeyEvents
import org.mz.mzdkplayer.ui.audioplayer.components.AudioPlayerControlsIcon
import org.mz.mzdkplayer.ui.audioplayer.components.AudioPlayerMainFrame
import org.mz.mzdkplayer.ui.audioplayer.components.AudioPlayerMediaTitle
import org.mz.mzdkplayer.ui.audioplayer.components.AudioPlayerMediaTitleType
import org.mz.mzdkplayer.ui.audioplayer.components.AudioPlayerOverlay
import org.mz.mzdkplayer.ui.audioplayer.components.AudioPlayerPulse
import org.mz.mzdkplayer.ui.audioplayer.components.AudioPlayerPulseState
import org.mz.mzdkplayer.ui.audioplayer.components.AudioPlayerSeeker
import org.mz.mzdkplayer.ui.audioplayer.components.AudioPlayerState
import org.mz.mzdkplayer.ui.audioplayer.components.rememberAudioPlayerPulseState
import org.mz.mzdkplayer.ui.audioplayer.components.rememberAudioPlayerState
import org.mz.mzdkplayer.ui.screen.vm.AudioPlayerViewModel
import org.mz.mzdkplayer.ui.videoplayer.BackPress
import org.mz.mzdkplayer.ui.videoplayer.components.BuilderMzPlayer
import org.mz.mzdkplayer.ui.videoplayer.components.rememberPlayer
import java.io.InputStream
import java.net.URL
import java.util.Locale
import kotlin.time.Duration.Companion.milliseconds

// 数据类用于存储音频元数据
data class AudioMetadata(
    val title: String = "未知标题",
    val artist: String = "未知艺术家",
    val album: String = "未知专辑",
    val year: String = "",
    val genre: String = "",
    val duration: String = "",
    val coverArt: Bitmap? = null,  // 修改为Bitmap类型
    val extras: Bundle? = null,
    val lyrics: String = ""
)

// 从ExoPlayer获取音频元数据的工具函数
@OptIn(UnstableApi::class)
fun extractAudioMetadataFromPlayer(exoPlayer: ExoPlayer): AudioMetadata {
    val mediaMetadata = exoPlayer.mediaMetadata

    // 提取基本元数据
    val title = mediaMetadata.title?.toString() ?: "未知标题"
    val artist = mediaMetadata.artist?.toString() ?: "未知艺术家"
    val album = mediaMetadata.albumTitle?.toString() ?: "未知专辑"
    val recordingYear = mediaMetadata.releaseYear?.toString() ?: ""
    val genre = mediaMetadata.genre?.toString() ?: ""
    val durationMs = exoPlayer.duration
    val duration = formatDuration(durationMs)
    val extras = mediaMetadata.extras

    // 从artworkData提取封面图片
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

    return AudioMetadata(
        title = title,
        artist = artist,
        album = album,
        year = recordingYear,
        genre = genre,
        duration = duration,
        coverArt = coverBitmap,
        extras = extras,
        lyrics = "" // Media3中歌词通常不直接提供，需要单独处理
    )
}

// 辅助函数：格式化时长
fun formatDuration(durationMs: Long): String {
    if (durationMs == androidx.media3.common.C.TIME_UNSET) return "00:00"
    val totalSeconds = durationMs / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
}

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

    // 提取音频元数据
    var audioMetadata by remember { mutableStateOf(AudioMetadata()) }

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
    LaunchedEffect(mediaUri,sampleMimeType) {
        //Log.d("danmakuUri", mediaUri.toString())
        // androidx.media3.common.util.Log.d("mediaUri", mediaUri.toUri().toString())
        Log.d("sampleMimeType",sampleMimeType)

        withContext(Dispatchers.IO) {
            if (sampleMimeType.isNotEmpty()) {
                try {
                    Log.d("danmakuUriScheme", mediaUri.toUri().scheme?.lowercase().toString())
                    val inputStream: InputStream? = when (mediaUri.toUri().scheme?.lowercase()) {

                        "smb" -> {
                            // 使用 SMB 工具打开输入流
                            SmbUtils.openSmbFileInputStream(mediaUri.toUri())

                        }

                        "http", "https" -> {
                            // 打开 HTTP 输入流
                            when (dataSourceType) {
                                "WEBDAV" -> {
                                    SmbUtils.openWebDavFileInputStream(mediaUri.toUri())
                                }

                                "HTTP" -> {
                                    SmbUtils.openHTTPLinkXmlInputStream(mediaUri)
                                }

                                else -> {
                                    URL(mediaUri).openStream()
                                }
                            }
                        }

                        "file" -> {
                            // 打开本地文件输入流
                            context.contentResolver.openInputStream(mediaUri.toUri())
                                ?: throw java.io.IOException("Could not open file input stream for $mediaUri.toUri()")
                        }

                        "ftp" -> {
                            // 使用 SMB 工具打开输入流
                            SmbUtils.openFtpFileInputStream(mediaUri.toUri())

                        }

                        "nfs" -> {
                            SmbUtils.openNfsFileInputStream(mediaUri.toUri())
                        }

                        else -> {
                            // 不支持的 scheme
                            Log.w(
                                "AudioPlayerScreen",
                                "Unsupported scheme for danmaku URI: $mediaUri.toUri()"
                            )
                            null
                        }
                    }
                    Log.d("AudioPlayerScreen", inputStream.toString())

                    inputStream.use { stream ->

                        val audioInfo: AudioInfo? =
                            extractAudioInfoAndLyricsFromStream(context, stream, sampleMimeType)
                        Log.i(
                            "AudioPlayerScreen",
                            audioInfo.toString()
                        )


//                val danmakuResponse: DanmakuResponse = getDanmakuXmlFromFile(stream)
//                danmakuDataList = danmakuResponse.data
//                isDanmakuLoaded = true
//                Log.i(
//                    "AudioPlayerScreen",
//                    "Loaded ${danmakuDataList?.size ?: 0} danmaku items from $danmakuUri"
//                )
                    }

                } catch (e: Exception) {
                    //Log.e("AudioPlayerScreen", "Failed to load danmaku from $danmakuUri", e)
                    //isDanmakuLoaded = false // 标记加载失败
                    // 可以在这里设置一个状态变量来在UI上显示加载失败
                }
            }
        }
    }

    // 监听播放器状态变化，获取元数据
    LaunchedEffect(exoPlayer) {
        exoPlayer.addListener(object : Player.Listener {
            override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
                super.onMediaMetadataChanged(mediaMetadata)
                // 当元数据加载完成时更新
                audioMetadata = extractAudioMetadataFromPlayer(exoPlayer)
                Log.d("audioMetadata", audioMetadata.toString())
            }

            override fun onPlaybackStateChanged(playbackState: Int) {

                when (playbackState) {

                    Player.STATE_READY -> {
                        sampleMimeType = exoPlayer.audioFormat?.sampleMimeType.toString()
                    }

                    Player.STATE_BUFFERING -> {

                    }

                    Player.STATE_ENDED -> {

                    }

                    Player.STATE_IDLE -> {

                    }
                }
            }

            override fun onIsPlayingChanged(isExoPlaying: Boolean) {
                isPlaying = isExoPlaying
            }
        })

        // 初始时也尝试获取元数据
        delay(500) // 等待一小段时间以确保元数据已加载
        audioMetadata = extractAudioMetadataFromPlayer(exoPlayer)

    }

    LaunchedEffect(Unit) {
        while (true) {
            delay(200)
            contentCurrentPosition = exoPlayer.currentPosition
        }
    }

    val pulseState = rememberAudioPlayerPulseState()
    Box(
        Modifier
            .dPadEvents(
                exoPlayer,
                audioPlayerState,
                pulseState,
                audioPlayerViewModel
            )
            .background(Color(0, 0, 0))
            .focusable()
            .fillMaxHeight()
            .fillMaxWidth()
    ) {
        val focusRequester = remember { FocusRequester() }
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 56.dp, bottom = 30.dp)
        ) {
            AlbumCoverDisplay(audioMetadata.coverArt) // 显示专辑封面
        }
        AudioPlayerOverlay(
            modifier = Modifier.align(Alignment.BottomCenter),
            focusRequester = focusRequester,
            state = audioPlayerState,
            isPlaying = isPlaying,
            subtitles = { },
            controls = {
                AudioPlayerControls(
                    isPlaying,
                    contentCurrentPosition,
                    exoPlayer,
                    audioPlayerState,
                    focusRequester,
                    audioMetadata.title,
                    "${audioMetadata.artist} - ${audioMetadata.album}",
                    "时长: ${audioMetadata.duration}",
                    audioPlayerViewModel
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
                .size(240.dp), // 设置封面图片大小
            contentScale = ContentScale.Fit
        )
    } else {
        // 如果没有封面，则显示默认图标
        Image(
            painter = painterResource(id = R.drawable.album), // 请确保有默认封面图片
            contentDescription = "默认专辑封面",
            modifier = Modifier
                .size(240.dp),
            contentScale = ContentScale.Fit
        )
    }
}

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

@OptIn(UnstableApi::class)
@Composable
fun AudioPlayerControls(
    isPlaying: Boolean,
    contentCurrentPosition: Long,
    exoPlayer: ExoPlayer,
    state: AudioPlayerState,
    focusRequester: FocusRequester,
    title: String,
    secondaryText: String,
    tertiaryText: String,
    audioPlayerViewModel: AudioPlayerViewModel,
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
                onSeek = { exoPlayer.seekTo((exoPlayer.duration * it).toLong()) },
                contentProgress = contentCurrentPosition.milliseconds,
                contentDuration = exoPlayer.duration.milliseconds
            )
        },
        more = null
    )
}



