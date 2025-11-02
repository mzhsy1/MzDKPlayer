package org.mz.mzdkplayer.ui.audioplayer
import android.graphics.Bitmap
import android.util.Log
import android.view.KeyEvent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape

import androidx.compose.runtime.*
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent

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

    var audioInfo: AudioInfo? by remember { mutableStateOf(null) } // 存储 audioInfo，包含所有音频信息
    var currentMediaUri by remember { mutableStateOf(mediaUri) }
    var currentFileName by remember { mutableStateOf(fileName) } // 替换原来的 fileName 状态
    var isAudioInfoLoading by remember { mutableStateOf(false) } // 添加加载状态

    // 添加缓存状态，用于在单曲循环时恢复音频信息
    var cachedAudioInfo by remember { mutableStateOf<AudioInfo?>(null) }

    // 添加Seek状态，用于跟踪快速Seek操作
    var isSeeking by remember { mutableStateOf(false) }
    var lastSeekTime by remember { mutableLongStateOf(0L) }
// 替换原来的模拟实现

    BuilderMzAudioPlayer(
        context,
        currentMediaUri,
        exoPlayer,
        dataSourceType,
        extraList,
        currentIndex,
        audioPlayerViewModel
    )

    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }

    // 加载音频信息和歌词 - 使用 currentMediaUri 作为依赖项
    LaunchedEffect(currentMediaUri,audioPlayerViewModel.selectedAtIndex) {
        Log.d("AudioPlayerScreen", "Loading audio info for URI: $currentMediaUri")
        isAudioInfoLoading = true

        // 获取当前媒体项的 MIME 类型 - 改进逻辑
        var mimeType: String? = null

        // 首先尝试从文件扩展名推断
        val fileExtension = currentMediaUri.substringAfterLast(".", "").lowercase()
        val mimeTypeFromExtension = when (fileExtension) {
            "flac" -> "audio/flac"
            "mp3" -> "audio/mpeg"
            "wav" -> "audio/wav"
            "aac" -> "audio/aac"
            "m4a" -> "audio/mp4"
            else -> null
        }

        Log.e("sampleMimeType1", currentMediaUri + (mimeTypeFromExtension?.toString() ?: "null"))

        // 尝试从播放器获取实际 MIME 类型
        var retryCount = 0
        val maxRetries = 10 // 最多重试10次，每次等待100ms，总共1秒
        while (retryCount < maxRetries) {
            delay(100) // 等待100ms让播放器准备好
            val playerMimeType = exoPlayer.audioFormat?.sampleMimeType
            if (playerMimeType != null) {
                mimeType = playerMimeType
                break
            }
            retryCount++
        }

        Log.e("sampleMimeType2", currentMediaUri + (mimeType?.toString() ?: "null"))

        // 如果播放器获取失败，使用从扩展名推断的 MIME 类型
        if (mimeType == null) {
            mimeType = mimeTypeFromExtension ?: "audio/mpeg" // 默认为 mp3
        }

        Log.e("sampleMimeType3", currentMediaUri + mimeType)

        withContext(Dispatchers.IO) {
            try {
                val inputStream: InputStream? =
                    when (currentMediaUri.toUri().scheme?.lowercase()) {
                        "smb" -> SmbUtils.openSmbFileInputStream(
                            currentMediaUri.toUri(),
                            mimeType
                        )

                        "http", "https" -> {
                            when (dataSourceType) {
                                "WEBDAV" -> SmbUtils.openWebDavFileInputStream(
                                    currentMediaUri.toUri(),
                                    mimeType
                                )

                                "HTTP" -> SmbUtils.openHTTPLinkXmlInputStream(
                                    currentMediaUri,
                                    mimeType
                                )

                                else -> URL(currentMediaUri).openStream()
                            }
                        }

                        "file" -> context.contentResolver.openInputStream(currentMediaUri.toUri())
                            ?: throw java.io.IOException("Could not open file input stream for $currentMediaUri")

                        "ftp" -> SmbUtils.openFtpFileInputStream(
                            currentMediaUri.toUri(),
                            mimeType
                        )

                        "nfs" -> SmbUtils.openNfsFileInputStream(
                            currentMediaUri.toUri(),
                            mimeType
                        )

                        else -> {
                            Log.w(
                                "AudioPlayerScreen",
                                "Unsupported scheme for URI: $currentMediaUri"
                            )
                            null
                        }
                    }

                Log.d("AudioPlayerScreen", "Opened input stream for $currentMediaUri with MIME: $mimeType")

                inputStream?.use { stream ->
                    // 关键：调用工具函数获取 audioInfo，其中包含 lyrics
                    val info = extractAudioInfoAndLyricsFromStream(context, stream, mimeType)
                    audioInfo = info // 更新状态
                    cachedAudioInfo = info // 同时缓存一份

                    Log.i("AudioPlayerScreen", "Loaded audio info and lyrics for $currentMediaUri")
                } ?: Log.e("AudioPlayerScreen", "Failed to open input stream for $currentMediaUri")
            } catch (e: Exception) {
                Log.e(
                    "AudioPlayerScreen",
                    "Failed to load audio info or lyrics from $currentMediaUri",
                    e
                )
            } finally {
                isAudioInfoLoading = false
            }
        }
    }


    LaunchedEffect(exoPlayer) {
        exoPlayer.addListener(object : Player.Listener {
            override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
                super.onMediaMetadataChanged(mediaMetadata)
                // 不再单独处理 metadata，因为所有信息都从 audioInfo 获取
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_READY -> {
                        sampleMimeType = exoPlayer.audioFormat?.sampleMimeType.toString()
                        Log.d("AudioPlayerScreen", "Player ready, MIME type: $sampleMimeType")
                    }

                    Player.STATE_BUFFERING -> {}
                    Player.STATE_ENDED -> {
                        // 播放结束时，如果是单曲循环模式，恢复音频信息
                        if (exoPlayer.repeatMode == Player.REPEAT_MODE_ONE) {
                            if (cachedAudioInfo != null) {
                                audioInfo = cachedAudioInfo
                            }
                        }
                    }

                    Player.STATE_IDLE -> {}

                }
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                super.onMediaItemTransition(mediaItem, reason)
                Log.d("onMediaItemTransition", mediaItem?.localConfiguration?.uri.toString())

                if (reason != Player.MEDIA_ITEM_TRANSITION_REASON_REPEAT) {
                    val newUri = mediaItem?.localConfiguration?.uri?.toString() ?: return
                    Log.d("onMediaItemTransition", "Transitioning to new URI: $newUri")
                    currentMediaUri = newUri
                    audioInfo = null // 重置音频信息，触发重新加载
                    isAudioInfoLoading = true // 设置加载状态
                    currentFileName = Tools.extractFileNameFromUri(newUri)

                    // ✅ 查找当前 URI 在 extraList 中的索引
                    val newIndex = extraList.indexOfFirst { it.uri == newUri }
                    if (newIndex != -1) {
                        audioPlayerViewModel.selectedAtIndex = newIndex
                    } else {
                        Log.w("AudioPlayerScreen", "Could not find index for URI: $newUri")
                    }
                } else {
                    Log.d("onMediaItemTransition", "Repeat transition - keeping current audio info")
                }
            }

            override fun onIsPlayingChanged(isExoPlaying: Boolean) {
                isPlaying = isExoPlaying
            }

            override fun onPlayerError(error: PlaybackException) {
                super.onPlayerError(error)
                Log.e("AudioPlayerScreen", "Player error: ${error.message}", error)

                // 如果是FLAC格式的解析错误，尝试使用通用音频格式
                if (error.cause?.message?.contains("contentIsMalformed") == true) {
                    Log.w("AudioPlayerScreen", "FLAC parsing error detected, continuing...")
                    // 不要中断播放，只是记录错误
                }
            }

            override fun onPositionDiscontinuity(
                oldPosition: Player.PositionInfo,
                newPosition: Player.PositionInfo,
                reason: Int
            ) {
                super.onPositionDiscontinuity(oldPosition, newPosition, reason)
                // 当发生位置跳跃时，标记正在Seek
                if (reason == Player.DISCONTINUITY_REASON_SEEK ||
                    reason == Player.DISCONTINUITY_REASON_SEEK_ADJUSTMENT
                ) {
                    isSeeking = true
                    lastSeekTime = System.currentTimeMillis()
                }
            }
        })
    }

    // 处理播放进度更新，使用防抖机制避免快速Seek时的问题
    LaunchedEffect(exoPlayer) {
        var lastUpdateTime = System.currentTimeMillis()
        val debounceDelay = 300L // 300ms防抖延迟

        while (true) {

            delay(100) // 检查间隔改为100ms

            val currentTime = System.currentTimeMillis()
            val currentPosition = exoPlayer.currentPosition

            // 检查是否仍在Seek状态中
            val isCurrentlySeeking = isSeeking && (currentTime - lastSeekTime < 1000) // 1秒内认为仍在Seek

            // 只有当时间间隔超过防抖延迟且不在Seek状态时才更新位置
            if (!isCurrentlySeeking && currentTime - lastUpdateTime > debounceDelay) {
                contentCurrentPosition = currentPosition
                lastUpdateTime = currentTime
            }

            // 如果Seek状态持续超过1秒，重置Seek状态
            if (isSeeking && currentTime - lastSeekTime > 1000) {
                isSeeking = false
            }
        }
    }

    // 处理Seek结束状态
    LaunchedEffect(contentCurrentPosition) {
        // 当位置更新时，重置Seek状态
        if (isSeeking) {
            isSeeking = false
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
    val focusRequester = remember { FocusRequester() }
    Box(
        Modifier
            .dPadEvents(
                exoPlayer,
                audioPlayerState,
                pulseState,
                audioPlayerViewModel,
                focusRequester
            )
            .background(Color.Black)
            .focusable()
            .fillMaxSize()
    ) {


        // 创建水平布局容器
        // 优化 1: 使用 fillMaxSize() 确保 Row 占据整个可用空间
        // 在频谱显示组件中使用：

        Row(
            modifier = Modifier
                .fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        )
        {
            // 优化 1: 使用 remember 来缓存解码后的 Bitmap
            val coverBitmap: Bitmap? = remember(audioInfo?.artworkData) { // 依赖 artworkData
                // 当 artworkData 改变时，才重新执行 lambda
                // 为了性能，也可以考虑在这里直接用 BitmapFactory.decodeByteArray
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

                AlbumCoverDisplay(coverBitmap,isPlaying)

            }
            Column(
                modifier = Modifier
                    .fillMaxHeight(0.8f)
                    .fillMaxWidth(0.75f)
            ) {

                Box(
                    modifier = Modifier
                        .fillMaxHeight(0.18f)
                        .fillMaxWidth()
                        .offset(90.dp, y = (-32).dp), contentAlignment = Alignment.CenterStart
                ) {
                    Column {
                        Text(
                            text = if (isAudioInfoLoading) "加载中..." else audioInfo?.title
                                ?: "未知标题",
                            style = MaterialTheme.typography.headlineSmall,
                            color = Color.White,
                            maxLines = 1
                        )
                        Row(Modifier.padding(top = 8.dp)) {
                            Text(
                                text = "艺术家 : ${if (isAudioInfoLoading) "加载中..." else audioInfo?.artist ?: "未知歌手"}",
                                color = Color.Gray,
                                fontSize = 16.sp,
                                maxLines = 1
                            )

                            Text(text = " · ", color = Color.Gray)
                            Text(
                                text = "专辑 : ${if (isAudioInfoLoading) "加载中..." else audioInfo?.album ?: "未知专辑"}",
                                color = Color.Gray,
                                fontSize = 16.sp,
                                maxLines = 1
                            )
                        }
                    }
                }
                // 优化 2: 歌词部分 - 使用 audioInfo?.lyrics
                // 优化 3: 使用 weight(1f) 让歌词部分占据剩余空间
                Box(
                    modifier = Modifier
                        .fillMaxHeight(0.72f)
                        .fillMaxWidth()
                        .offset(90.dp, y = (-30).dp), contentAlignment = Alignment.CenterStart
                ) {

                    // 使用您提供的 LRC 解析器
                    val parsedLyrics = remember(audioInfo?.lyrics) {
                        if (audioInfo?.lyrics != null) {
                            parseLrc(audioInfo!!.lyrics)
                        } else {
                            emptyList() // 返回空列表而不是null
                        }
                    }
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
            modifier = Modifier.align(Alignment.BottomCenter) .onFocusChanged {
                audioPlayerViewModel.conFocus =
                    it.isFocused
            }, // 底部居中,
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

                    "时长: ${
                        if (safeDurationMs != 0L) {
                            formatDuration(safeDurationMs.toInt())
                        } else {
                            "--:--"
                        }
                    }", // 直接使用 exoPlayer.duration 格式化后的字符串
                    audioPlayerViewModel,
                    safeDurationMs.milliseconds // 传递安全的 Duration
                )
            },
            atpFocus = audioPlayerViewModel.atpFocus
        )
        // 在频谱显示组件中

        // 音轨/字幕选择面板的动画可见性
        AnimatedVisibility(
            audioPlayerViewModel.atpVisibility, // 根据 ViewModel 状态显示/隐藏
            enter = fadeIn(), // 淡入动画
            exit = fadeOut(), // 淡出动画
            modifier = Modifier
                .widthIn(200.dp, 360.dp) // 宽度范围
                .fillMaxHeight() // 高度范围
                .align(AbsoluteAlignment.CenterRight) // 右侧居中
                // 向左偏移
                .background(
                    Color.Black.copy(0.8f), shape = RoundedCornerShape(2.dp) // 半透明黑色背景和圆角
                )
                // ✅ 关键修复：阻止事件冒泡到父组件
                .handleDPadKeyEvents(
                    onRight = { true },  // 消耗事件
                    onUp = { true },     // 消耗事件
                    onDown = { true },   // 消耗事件
                    onLeft = { true }    // ✅ 关键：向左按键在面板内消耗，不传递到控制栏
                )
                // 处理焦点变化
                .onFocusChanged { focusState ->
                    // 隐藏控制栏
                    audioPlayerViewModel.atpFocus = focusState.isFocused

//                    // ✅ 关键：当面板失去焦点且不可见时，将焦点返回到控制栏
//                    if (!focusState.isFocused && !audioPlayerViewModel.atpVisibility) {
//                        focusRequester.requestFocus()
//                    }
                }) {
            // 根据 ViewModel 中的选择显示不同的面板
            when (audioPlayerViewModel.selectedAorVorS) {
                "L" -> AudioListPanel(
                    audioPlayerViewModel.selectedAtIndex, // 当前选中的音频轨道索引
                    onSelectedIndexChange = {
                        audioPlayerViewModel.selectedAtIndex = it
                    }, // 索引变化回调
                    extraList.toMutableList(), // 音频轨道组
                    exoPlayer,
                    audioPlayerViewModel

                )
            }
            BackHandler(true) {
                audioPlayerViewModel.atpVisibility = false
                audioPlayerViewModel.atpFocus = false // 隐藏时重置焦点状态
                focusRequester.requestFocus()
            }
        }
    }
}


// --- D-Pad 事件修饰符 ---
private fun Modifier.dPadEvents(
    exoPlayer: ExoPlayer,
    audioPlayerState: AudioPlayerState,
    pulseState: AudioPlayerPulseState,
    audioPlayerViewModel: AudioPlayerViewModel,
    focusRequester: FocusRequester
): Modifier = handleDPadKeyEvents(
    onLeft = {
//        // 面板可见时，不处理向左事件
        if (!audioPlayerViewModel.conFocus) {
            focusRequester.requestFocus()
        }

        true
    },
    onRight = {
        if (!audioPlayerViewModel.conFocus) {
            focusRequester.requestFocus()
        }
        true
    },
    onUp = {
        true
//        if (!audioPlayerViewModel.atpVisibility) {
//            audioPlayerViewModel.atpVisibility = true
//            audioPlayerViewModel.selectedAorVorS = "L"
//            true
//        } else {
//            false
//        }
    },
    onDown = {
        true
//        if (audioPlayerViewModel.atpVisibility) {
//            audioPlayerViewModel.atpVisibility = false
//            true
//        } else {
//            false
//        }
    },
    onEnter = {
        focusRequester.requestFocus()
        exoPlayer.pause()
        true


    },
).onKeyEvent { keyEvent ->
    when (keyEvent.key) {
        Key.Menu -> {
            // 菜单键处理逻辑
            audioPlayerViewModel.atpVisibility = true
            true // 消费事件
        }

        Key.ButtonY -> {
            audioPlayerViewModel.atpVisibility = true
            true // 消费事件
        }

        else -> {
            // 检查原生键码
            when (keyEvent.nativeKeyEvent.keyCode) {
                KeyEvent.KEYCODE_MENU -> {
                    audioPlayerViewModel.atpVisibility = true
                    true // 消费事件
                }

                else -> false
            }
        }
    }
}

// 格式化时长的工具函数
fun formatDuration(durationMs: Int?): String {
    // 处理 null 或无效值
    if (durationMs == null || durationMs <= 0) return "--:--"
    val totalSeconds = durationMs / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
}




