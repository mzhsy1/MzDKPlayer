package org.mz.mzdkplayer.ui.videoplayer

// 导入必要的库和组件

import android.app.Activity
import android.net.TrafficStats
import android.view.KeyEvent
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.input.key.Key

import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.toUpperCase
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.core.net.toUri
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.text.Cue
import androidx.media3.common.text.CueGroup
import androidx.media3.common.util.Log
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.compose.ContentFrame
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults

import androidx.tv.material3.Text
import com.kuaishou.akdanmaku.DanmakuConfig
import com.kuaishou.akdanmaku.data.DanmakuItemData
import com.kuaishou.akdanmaku.ext.RETAINER_BILIBILI
import com.kuaishou.akdanmaku.render.SimpleRenderer
import com.kuaishou.akdanmaku.ui.DanmakuPlayer
import kotlinx.coroutines.delay
import org.mz.mzdkplayer.R
import org.mz.mzdkplayer.danmaku.DanmakuData
import org.mz.mzdkplayer.danmaku.DanmakuResponse
import org.mz.mzdkplayer.danmaku.getDanmakuXmlFromFile
import org.mz.mzdkplayer.data.model.DanmakuScreenRatio
import org.mz.mzdkplayer.data.model.MediaHistoryRecord

import org.mz.mzdkplayer.data.repository.DanmakuSettingsManager
import org.mz.mzdkplayer.player.core.IMzPlayer
import org.mz.mzdkplayer.player.core.MzIsoTitle
import org.mz.mzdkplayer.player.core.autoLoadSameNameSubtitles
import org.mz.mzdkplayer.player.exo.MzExoPlayer
import org.mz.mzdkplayer.player.vlc.MzVlcPlayer
import org.mz.mzdkplayer.tool.FtpDataSource
import org.mz.mzdkplayer.tool.SmbDataSource
import org.mz.mzdkplayer.tool.SmbUtils
import org.mz.mzdkplayer.tool.SubtitleView
import org.mz.mzdkplayer.tool.Tools
import org.mz.mzdkplayer.tool.Tools.toSafeInt
import org.mz.mzdkplayer.tool.Tools.toBase64
import org.mz.mzdkplayer.data.repository.VideoPlaylistRepository
import org.mz.mzdkplayer.data.model.VideoItem
import androidx.navigation.NavHostController
import org.mz.mzdkplayer.tool.WebDavDataSource
import org.mz.mzdkplayer.tool.handleDPadKeyEvents

import org.mz.mzdkplayer.ui.screen.common.LoadingScreen
import org.mz.mzdkplayer.ui.screen.common.VAErrorScreen
import org.mz.mzdkplayer.ui.screen.common.showToast
import org.mz.mzdkplayer.ui.screen.vm.MediaHistoryViewModel
import org.mz.mzdkplayer.ui.screen.vm.SettingsViewModel
import org.mz.mzdkplayer.ui.screen.vm.VideoPlayerStatus
import org.mz.mzdkplayer.ui.screen.vm.VideoPlayerViewModel
import org.mz.mzdkplayer.ui.screen.vm.asDisplayString
import org.mz.mzdkplayer.ui.theme.myIconButtonColor
import org.mz.mzdkplayer.ui.videoplayer.components.AkDanmakuPlayer
import org.mz.mzdkplayer.ui.videoplayer.components.AudioTrackPanel
import org.mz.mzdkplayer.ui.videoplayer.components.DanmakuPanel
import org.mz.mzdkplayer.ui.videoplayer.components.IsoTitlePanel
import org.mz.mzdkplayer.ui.videoplayer.components.PlaybackSpeedPanel
import org.mz.mzdkplayer.ui.videoplayer.components.SubtitleTrackPanel
import org.mz.mzdkplayer.ui.videoplayer.components.VideoPlayerControls
import org.mz.mzdkplayer.ui.videoplayer.components.VideoPlayerControlsIcon
import org.mz.mzdkplayer.ui.videoplayer.components.VideoPlayerMainFrame
import org.mz.mzdkplayer.ui.videoplayer.components.VideoPlayerMediaTitle
import org.mz.mzdkplayer.ui.videoplayer.components.VideoPlayerMediaTitleType
import org.mz.mzdkplayer.ui.videoplayer.components.VideoPlayerOverlay
import org.mz.mzdkplayer.ui.videoplayer.components.VideoPlayerOverlayLayer
import org.mz.mzdkplayer.ui.videoplayer.components.VideoPlayerPulse
import org.mz.mzdkplayer.ui.videoplayer.components.VideoPlayerPulseState
import org.mz.mzdkplayer.ui.videoplayer.components.VideoPlayerSeeker
import org.mz.mzdkplayer.ui.videoplayer.components.VideoPlayerState
import org.mz.mzdkplayer.ui.videoplayer.components.VideoPlayerStatusLayer
import org.mz.mzdkplayer.ui.videoplayer.components.VideoPlayerTrackSelectionPanel
import org.mz.mzdkplayer.ui.videoplayer.components.VideoPlayerViewLayer
import org.mz.mzdkplayer.ui.videoplayer.components.VideoTrackPanel

import org.mz.mzdkplayer.ui.videoplayer.components.rememberVideoPlayerPulseState
import org.mz.mzdkplayer.ui.videoplayer.components.rememberVideoPlayerState
import java.io.IOException
import java.io.InputStream
import java.net.URL
import java.util.Locale
import kotlin.concurrent.thread
import kotlin.math.abs
import kotlin.time.Duration.Companion.milliseconds
import androidx.compose.ui.platform.LocalLocale

/**
 * 视频播放器主界面 Composable
 *
 * @param mediaUri 媒体文件的 URI
 * @param dataSourceType 数据源类型 (例如 "SMB", "HTTP", "NFS")
 */
@OptIn(UnstableApi::class)
@Composable
fun VideoPlayerScreen(
    mediaUri: String,
    dataSourceType: String,
    fileName: String = stringResource(R.string.ui_label_unknown_filename),
    connectionName: String,
    mediaHistoryViewModel: MediaHistoryViewModel,
    useVlc: Boolean = false, // 开关：让用户或者设置决定用哪个内核
    settingsViewModel: SettingsViewModel = viewModel(),
    navController: NavHostController
) {
    // 获取当前 Compose 上下文
    val context = LocalContext.current

    // 防止屏保弹出：进入播放器时设置 FLAG_KEEP_SCREEN_ON，退出时清除
    val activity = context as? Activity
    DisposableEffect(Unit) {
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    // 记住并创建 Player 实例
    // 1. 根据配置实例化解耦的 Player 内核
    val player: IMzPlayer = remember(useVlc, mediaUri) {
        if (useVlc) {
            MzVlcPlayer(context, mediaUri,dataSourceType=dataSourceType,settingsViewModel=settingsViewModel)
        } else {
            MzExoPlayer(context, mediaUri, dataSourceType, settingsViewModel)
        }
    }
    // 记住并创建视频播放器状态管理器
    val videoPlayerState = rememberVideoPlayerState(hideSeconds = 6)
    // 获取 ViewModel 实例
    val videoPlayerViewModel: VideoPlayerViewModel = viewModel()
    // 状态：是否显示 Toast 提示
    var showToast by remember { mutableStateOf(false) }
    // 状态：返回按钮按压状态，用于双击退出逻辑
    var backPressState by remember { mutableStateOf<BackPress>(BackPress.Idle) }
    // 状态：当前媒体播放位置
    var contentCurrentPosition by remember { mutableLongStateOf(0L) }
    // 状态：当前播放状态 (播放/暂停)
    var isPlaying: Boolean by remember { mutableStateOf(player.isPlaying) }

    // 网速监控相关状态
    // 状态：计算得到的网络速度 (bytes/sec)
    var networkSpeed by remember { mutableLongStateOf(0L) }
    // 状态：上一次记录的接收总字节数
    var lastTotalRxBytes by remember { mutableLongStateOf(0L) }
    // 状态：上一次记录的时间戳
    var lastTimeStamp by remember { mutableLongStateOf(0L) }

    // 记住并创建弹幕播放器实例
    val mDanmakuPlayer: DanmakuPlayer = remember(mediaUri) { DanmakuPlayer(SimpleRenderer()) }

    // 根据媒体 URI 和数据源类型推断弹幕文件 URI
    val danmakuUri =
        if (dataSourceType == "NFS") SmbUtils.getDanmakuNfsUri(mediaUri.toUri()) else SmbUtils.getDanmakuSmbUri(
            mediaUri.toUri()
        )
    // 状态：当前的字幕组 (CueGroup)
    var currentCueGroup: CueGroup? by remember { mutableStateOf<CueGroup?>(null) }
    // 弹幕数据
    // 状态：解析后的弹幕数据列表
    var danmakuDataList by remember { mutableStateOf<List<DanmakuData>?>(null) }
    // 状态：弹幕是否已加载完成
    var isDanmakuLoaded by remember { mutableStateOf(false) }
    val pulseState = rememberVideoPlayerPulseState()

    // 弹幕设置管理器
    val settingsManager = remember { DanmakuSettingsManager(context) }

    // 从 SettingsViewModel 中获取字幕相关的状态
    val settingsState by settingsViewModel.uiState.collectAsState()


    // 记录从数据库查到的历史进度
    var historySeekPos by remember { mutableLongStateOf(0L) }
    // 控制跳转提示条的显示
    var showHistoryTip by remember { mutableStateOf(false) }
    // 增加一个标记，确保只在第一次准备好时触发计时
    var hasTriggeredTimer by remember { mutableStateOf(false) }

    // 👇 新增：标记是否是首次加载
    var isFirstLoad by remember { mutableStateOf(true) }
    var isExoSubtitleVis by remember { mutableStateOf(true) }
    // 自动加载同名字幕：只尝试一次，避免播放器重载导致重复触发
    var autoSubtitleAttempted by remember { mutableStateOf(false) }
    val videoTracks by player.videoTracks.collectAsState()
    val audioTracks by player.audioTracks.collectAsState()
    val subtitleTracks by player.subtitleTracks.collectAsState()

    // 新增这一行
    val isoTitles by player.isoTitles.collectAsState()

    val isPlayerPlaying by player.isPlayingFlow.collectAsState()
    val playerStatus by player.playerStatus.collectAsState()
    val playbackSpeed by player.playbackSpeed.collectAsState()
    val currentAspectRatio by player.aspectRatio.collectAsState()
    // 构建播放器 (设置媒体源等)
    //BuilderMzPlayer(context, mediaUri, exoPlayer, dataSourceType, settingsViewModel)
    // 当 Composable 离开组合或播放器实例变化时，释放资源
    DisposableEffect(player, mDanmakuPlayer) {
        onDispose {
            // 1. 获取播放器当前状态
            val currentPos = player.currentPosition
            val totalDur = player.duration

            // 2. 构建历史记录对象
            // 只要播放过（进度 > 0）且总时长有效，才保存
            if (currentPos > 0 && totalDur > 0) {
                val record = MediaHistoryRecord(
                    mediaUri = mediaUri,
                    fileName = fileName,
                    playbackPosition = currentPos,
                    mediaDuration = totalDur,
                    // 处理协议名称显示的逻辑
                    protocolName = if (dataSourceType == "LOCAL") "LOCAL" else dataSourceType,
                    connectionName = connectionName,
                    serverAddress = "test", // 如果你有真实的 server IP，请传入，否则留空或用占位符
                    mediaType = "VIDEO",    // 明确标记为视频
                    timestamp = System.currentTimeMillis()
                )

                // 3. 调用 ViewModel 保存 (ViewModel 内部会启动协程写入数据库)
                mediaHistoryViewModel.saveHistory(record)
            }

            // 4. 释放资源
            player.release()
            mDanmakuPlayer.release()
        }
    }
    LaunchedEffect(Unit) {
        // 调用 suspend 函数从数据库获取进度
        val historyPosition = mediaHistoryViewModel.getHistoryPosition(mediaUri)

        // 进度大于 5 秒才有必要提示跳转（避开片头）
        if (historyPosition > 5000) {
            historySeekPos = historyPosition
        }
    }

    // 辅助方法：获取当前弹幕配置
    fun getDanmakuConfig(): DanmakuConfig {
        val currentSettings = settingsManager.loadSettings()
        val screenPartValue =
            DanmakuScreenRatio.fromDisplayName(currentSettings.selectedRatio).ratioValue

        return videoPlayerViewModel.danmakuConfig.copy(
            retainerPolicy = RETAINER_BILIBILI,
            visibility = videoPlayerViewModel.danmakuVisibility,
            screenPart = screenPartValue,
            textSizeScale = currentSettings.fontSize.toFloat() / 100,
            alpha = currentSettings.transparency.toFloat() / 100,
            dataFilter = listOf(videoPlayerViewModel.createDanmakuTypeFilter(currentSettings.selectedTypes)) // 添加弹幕过滤器
        )
    }

    // 在初始化时从本地加载弹幕设置，如果没有则使用默认值
    LaunchedEffect(Unit) {
        val savedSettings = settingsManager.loadSettings()
        videoPlayerViewModel.danmakuConfig = videoPlayerViewModel.danmakuConfig.copy(
            retainerPolicy = RETAINER_BILIBILI, // 设置弹幕保留策略
            textSizeScale = savedSettings.fontSize.toFloat() / 100, // 字体缩放
            screenPart = DanmakuScreenRatio.fromDisplayName(savedSettings.selectedRatio).ratioValue, // 屏幕占用比例
            alpha = savedSettings.transparency.toFloat() / 100, // 透明度
            visibility = savedSettings.isSwitchEnabled, // 可见性
            dataFilter = listOf(videoPlayerViewModel.createDanmakuTypeFilter(savedSettings.selectedTypes)) // 添加弹幕过滤器
        )
        videoPlayerViewModel.danmakuConfig.updateFilter()
        mDanmakuPlayer.updateConfig(videoPlayerViewModel.danmakuConfig)
        // 设置ViewModel的弹幕可见性状态与本地设置一致
        videoPlayerViewModel.danmakuVisibility = savedSettings.isSwitchEnabled
    }

    // 加载弹幕数据
    LaunchedEffect(danmakuUri) {
        Log.d("danmakuUri", danmakuUri.toString())
        Log.d("mediaUri", mediaUri.toUri().toString())
        try {
            Log.d("danmakuUriScheme", danmakuUri.scheme?.lowercase().toString())
            // 根据 URI scheme 打开对应的输入流
            val inputStream: InputStream? = when (danmakuUri.scheme?.lowercase()) {
                "smb" -> {
                    // 使用 SMB 工具打开输入流
                    SmbUtils.openSmbFileInputStream(danmakuUri, "video")
                }

                "http", "https" -> {
                    // 打开 HTTP 输入流
                    when (dataSourceType) {
                        "WEBDAV" -> {
                            SmbUtils.openWebDavFileInputStream(danmakuUri, "video")
                        }

                        "HTTP" -> {
                            SmbUtils.openHTTPLinkXmlInputStream(danmakuUri.toString(), "video")
                        }

                        else -> {
                            URL(danmakuUri.toString()).openStream()
                        }
                    }
                }

                "file" -> {
                    // 打开本地文件输入流
                    context.contentResolver.openInputStream(danmakuUri)
                        ?: throw IOException("Could not open file input stream for $danmakuUri")
                }

                "ftp" -> {
                    // 使用 SMB 工具打开输入流
                    SmbUtils.openFtpFileInputStream(danmakuUri, "video")
                }

                "nfs" -> {
                    SmbUtils.openNfsFileInputStream(danmakuUri, "video")
                }

                else -> {
                    // 不支持的 scheme
                    Log.w("VideoPlayerScreen", "Unsupported scheme for danmaku URI: $danmakuUri")
                    null
                }
            }

            // 使用输入流解析弹幕 XML
            inputStream.use { stream ->
                val danmakuResponse: DanmakuResponse = getDanmakuXmlFromFile(stream)
                danmakuDataList = danmakuResponse.data
                isDanmakuLoaded = true
                Log.i(
                    "VideoPlayerScreen",
                    "Loaded ${danmakuDataList?.size ?: 0} danmaku items from $danmakuUri"
                )
            }
        } catch (e: Exception) {
            Log.e("VideoPlayerScreen", "Failed to load danmaku from $danmakuUri", e)
            isDanmakuLoaded = false // 标记加载失败
            // 可以在这里设置一个状态变量来在UI上显示加载失败
        }
    }

    // 将加载的弹幕数据发送到播放器
    // 监听播放器状态变化来启动/暂停弹幕
    var hasSentDanmaku by remember { mutableStateOf(false) }
    LaunchedEffect(isDanmakuLoaded, danmakuDataList) {
        // 如果弹幕已加载且尚未发送给播放器
        if (isDanmakuLoaded && !hasSentDanmaku && danmakuDataList != null) {
            Log.d("danmakuData", "状态弹幕")
            // 将 DanmakuData 转换为 DanmakuItemData
            val danmakuItemDataList = danmakuDataList?.map { danmakuData ->
                DanmakuItemData(
                    danmakuId = if (danmakuData.rowId != 0L) danmakuData.rowId else (Math.random() * 100000000).toLong(), // 使用解析的ID或生成随机ID
                    position = (danmakuData.time * 1000).toLong(), // 时间戳转换为毫秒
                    content = danmakuData.content, // 弹幕内容
                    mode = when (danmakuData.mode) { // 映射弹幕模式
                        4 -> DanmakuItemData.DANMAKU_MODE_CENTER_TOP
                        5 -> DanmakuItemData.DANMAKU_MODE_CENTER_BOTTOM
                        else -> DanmakuItemData.DANMAKU_MODE_ROLLING
                    },
                    textSize = danmakuData.size, // 字体大小
                    textColor = danmakuData.color, // 颜色
                )
            }
            // 更新弹幕播放器的数据
            if (danmakuItemDataList != null) {
                mDanmakuPlayer.updateData(danmakuItemDataList)
            }

            hasSentDanmaku = true
            Log.i(
                "VideoPlayerScreen",
                "Sent ${danmakuDataList?.size ?: 0} danmaku items to player."
            )
        }
    }


    // 网速监控协程
    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            // 初始化网络统计
            lastTotalRxBytes = TrafficStats.getTotalRxBytes()
            lastTimeStamp = System.currentTimeMillis()

            while (isPlaying) {
                delay(1000.milliseconds) // 每秒更新一次

                val currentRxBytes = TrafficStats.getTotalRxBytes()
                val currentTimeStamp = System.currentTimeMillis()

                // 计算网速 (bytes/second)
                val timeDiff = currentTimeStamp - lastTimeStamp
                if (timeDiff > 0) {
                    val byteDiff = currentRxBytes - lastTotalRxBytes
                    networkSpeed = (byteDiff * 1000) / timeDiff

                    // 更新状态
                    lastTotalRxBytes = currentRxBytes
                    lastTimeStamp = currentTimeStamp
                }
            }
        }
    }

    // 自定义第三方字幕字体（仅对 CustomSubtitleView 纯文本字幕生效）
    val customSubtitleFontFamily = remember(settingsState.subFontPath) {
        val path = settingsState.subFontPath
        if (path.isNotBlank()) {
            try {
                val fontFile = java.io.File(path)
                if (fontFile.exists() && fontFile.isFile) {
                    FontFamily(Font(fontFile))
                } else {
                    null
                }
            } catch (e: Exception) {
                Log.e("VideoPlayer", "Failed to load subtitle font: $path", e)
                null
            }
        } else {
            null
        }
    }

    // 定义自定义字幕样式
    val customSubtitleStyle = TextStyle(
        color = Color(settingsState.subColor), // 字幕颜色为白色
        fontSize = settingsState.subFontSize.sp,     // 字体大小
        fontFamily = customSubtitleFontFamily,
        shadow = Shadow(
            color = Color.Black, // 黑色阴影
            offset = Offset(3f, 3f),
            blurRadius = 1f
        ),
    )

    // 监听 ExoPlayer 播放状态变化，并更新 `isPlaying` 状态
    // 同时定期更新 `contentCurrentPosition`
    LaunchedEffect(Unit) {
        while (true) {
            delay(200.milliseconds)
            contentCurrentPosition = player.currentPosition
            isPlaying = player.isPlaying
        }
    }


    LaunchedEffect(isPlayerPlaying) {
        if (videoPlayerViewModel.danmakuVisibility) {
            if (isPlayerPlaying) {
                // 启动弹幕
                videoPlayerViewModel.danmakuConfig = getDanmakuConfig()
                mDanmakuPlayer.updateConfig(videoPlayerViewModel.danmakuConfig)
                mDanmakuPlayer.start()
                mDanmakuPlayer.seekTo(player.currentPosition)
            } else {
                mDanmakuPlayer.pause()
            }
        }
    }
    // 监听准备就绪状态：按顺序处理自动加载字幕和历史进度跳转，避免两者冲突
    LaunchedEffect(playerStatus) {
        if (playerStatus == VideoPlayerStatus.READY) {
            isFirstLoad = false
            videoPlayerViewModel.updatePlayerStatus(VideoPlayerStatus.READY)

            // 1. 优先尝试自动加载同名字幕
            if (!autoSubtitleAttempted && settingsState.autoLoadSubtitle) {
                autoSubtitleAttempted = true
                val count = autoLoadSameNameSubtitles(mediaUri, dataSourceType, player)
                if (count > 0) {
                    showToast(context, context.getString(R.string.ui_label_auto_subtitle_found, count))
                }
                // 注意：对于 ExoPlayer，加载字幕会触发 setMediaItem，导致播放器重新准备并再次触发 READY。
                // 我们在加载动作发出后直接返回，确保跳转逻辑在字幕加载完成后的下一次 READY 中执行，从而避免位置重置。
                if (count > 0 && !useVlc) return@LaunchedEffect
            }

            // 2. 处理历史进度跳转
            if (historySeekPos > 0 && !hasTriggeredTimer) {
                player.seekTo(historySeekPos)
                showHistoryTip = true
                hasTriggeredTimer = true
            }
        } else {
            videoPlayerViewModel.updatePlayerStatus(playerStatus)
        }
    }
    // 3. 设置错误回调
    DisposableEffect(player) {
        player.onError = { msg ->
            videoPlayerViewModel.setPlayerError(msg)
        }
        player.onPlaybackEnded = {
            when (settingsState.videoFinishAction) {
                0 -> { // 循环播放
                    player.seekTo(0)
                    player.play()
                }
                1 -> { // 播放暂停
                    player.pause()
                }
                2 -> { // 播放下一个
                    val playlist = VideoPlaylistRepository.getPlaylist()
                    if (playlist.isNotEmpty()) {
                        val currentIndex = playlist.indexOfFirst { it.uri == mediaUri }
                        if (currentIndex != -1 && currentIndex < playlist.size - 1) {
                            val nextVideo = playlist[currentIndex + 1]
                            val encodedUri = nextVideo.uri.toBase64()
                            val encodedName = nextVideo.fileName.toBase64()
                            val encodedConn = nextVideo.connectionName.toBase64()
                            navController.navigate("VideoPlayer/$encodedUri/${nextVideo.dataSourceType}/$encodedName/$encodedConn") {
                                popUpTo("VideoPlayer/{sourceUri}/{dataSourceType}/{fileName}/{connectionName}") { inclusive = true }
                            }
                        } else {
                            showToast(context, "已是最后一个视频")
                        }
                    }
                }
            }
        }
        player.onCuesChanged = { cues ->
            currentCueGroup = cues as CueGroup?
            // 处理字幕显示逻辑
        }
        onDispose {
            player.onError = null
            player.onCuesChanged = null
        }
    }
    // 专门负责提示框的自动倒计时消失

    // 主 UI 布局 - 保持 PlayerView 作为底层渲染，状态覆盖层在上层
    Box(
        Modifier
            // 添加 D-Pad 事件处理
            .dPadEvents(
                player,
                videoPlayerState,
                pulseState,
                videoPlayerViewModel,
                settingsState.dpadUpAction,
                settingsState.dpadDownAction,
                settingsState.ffDuration,
                settingsState.rwDuration
            )
            .background(Color(0, 0, 0)) // 黑色背景
            .focusable() // 可获得焦点
            .fillMaxHeight()
            .fillMaxWidth()
    ) {

        // 创建焦点请求器
        val focusRequester = remember { FocusRequester() }
        val reRequester = remember { FocusRequester() }
        var videoSizePx by remember { mutableStateOf(IntSize.Zero) }
        val density = LocalDensity.current.density

        val videoSizeDp = with(density) {
            IntSize(
                width = (videoSizePx.width / density).toInt(),
                height = (videoSizePx.height / density).toInt()
            )
        }
        // 在 VideoPlayerScreen.kt 中修改或添加以下 LaunchedEffect
        LaunchedEffect(showHistoryTip) {
            if (showHistoryTip) {
                // 给一点点时间让 AnimatedVisibility 把内容挂载到树上
                delay(100.milliseconds)
                try {
                    reRequester.requestFocus()
                } catch (e: Exception) {
                    Log.e("VideoPlayer", "Focus request failed", e)
                }

                // 原有的 5 秒后自动消失逻辑
                delay(5000.milliseconds)
                showHistoryTip = false
            }
        }

        // AndroidView 包裹 PlayerView
        VideoPlayerViewLayer(
            player = player,
            isCusSubtitleViewVis = videoPlayerViewModel.isCusSubtitleViewVis,
            useVlc = useVlc,
            currentCueGroup = currentCueGroup,
            customSubtitleStyle = customSubtitleStyle,
            subBottomPadding = settingsState.subBottomPadding,
            subBgColor = settingsState.subBgColor,
            videoSizeDp = videoSizeDp,
            forcePgsCenter = settingsState.forcePgsCenter,
            mDanmakuPlayer = mDanmakuPlayer,
            onVideoSizeChanged = { videoSizePx = it }
        )

        // 实时网速显示
        if (isPlaying && !settingsState.hideNetworkSpeed) {
            NetworkSpeedIndicator(
                networkSpeed = networkSpeed, // 传递网络速度
                modifier = Modifier
                    .align(Alignment.TopEnd) // 右上角对齐
                    .padding(16.dp) // 内边距
            )
        }
        val statusText = playerStatus.asDisplayString()

        // 视频播放器覆盖层
        if (!videoPlayerViewModel.atpVisibility) {
            VideoPlayerOverlayLayer(
                videoPlayerViewModel = videoPlayerViewModel,
                focusRequester = focusRequester,
                videoPlayerState = videoPlayerState,
                isPlaying = isPlaying,
                pulseState = pulseState,
                currentPositionProvider = { contentCurrentPosition },
                player = player,
                fileName = fileName,
                statusText = statusText,
                mDanmakuPlayer = mDanmakuPlayer,
                settingsManager = settingsManager,
                getDanmakuConfig = ::getDanmakuConfig,
                ffDuration = settingsState.ffDuration,
                rwDuration = settingsState.rwDuration,
                onTogglePlayPause = {
                    if (player.isPlaying) player.pause() else player.play()
                }
            )
        }

        LaunchedEffect(videoPlayerState.controlsVisible) {
            if (videoPlayerState.controlsVisible && !videoPlayerViewModel.atpVisibility) {
                focusRequester.requestFocus()
            }
        }
        // 历史记录跳转提示浮窗
        AnimatedVisibility(
            visible = showHistoryTip,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 40.dp, bottom = 150.dp) // 避开进度条位置
        ) {
            Button(
                modifier = Modifier.focusRequester(reRequester),
                onClick = {
                    player.seekTo(0L)
                    reRequester.freeFocus()
                    showHistoryTip = false

                },
                colors = myIconButtonColor()
            ) {
                val minutes = (historySeekPos / 1000 / 60).toString().padStart(2, '0').toSafeInt(0)
                val seconds = (historySeekPos / 1000 % 60).toString().padStart(2, '0').toSafeInt(0)
                Text(
                    text = stringResource(
                        id = R.string.ui_label_resume_from_time,
                        minutes,
                        seconds
                    ),
                    fontSize = 14.sp,
                     // 建议给提示语加点透明度，更符合电视端审美
                )
            }
        }
        // 显示 "再按一次退出" Toast
        if (showToast) {
            val pressAgainText = stringResource(R.string.ui_label_press_again_to_exit)
            showToast(context, pressAgainText)
            showToast = false
        }

        // 处理双击返回退出逻辑
        LaunchedEffect(key1 = backPressState) {
            if (backPressState == BackPress.InitialTouch) {
                delay(2000.milliseconds) // 2秒延迟
                backPressState = BackPress.Idle // 重置状态
            }
        }
        LaunchedEffect(videoPlayerViewModel.conFocus) {
            Log.d("conFocus", videoPlayerViewModel.conFocus.toString())
        }

        BackHandler(backPressState == BackPress.Idle) {
            if (backPressState == BackPress.Idle && !videoPlayerState.controlsVisible) {
                backPressState = BackPress.InitialTouch
                showToast = true
            }


        }
        BackHandler(videoPlayerState.controlsVisible) {
            if (!videoPlayerViewModel.conFocus) {
                videoPlayerState.hideControls()
            }
        }


        // 音轨/字幕选择面板
        VideoPlayerTrackSelectionPanel(
            videoPlayerViewModel = videoPlayerViewModel,
            settingsViewModel = settingsViewModel,
            player = player,
            audioTracks = audioTracks,
            videoTracks = videoTracks,
            subtitleTracksFlow = player.subtitleTracks,
            isoTitles = isoTitles,
            playbackSpeed = playbackSpeed,
            currentAspectRatio = currentAspectRatio,
            enablePassthrough = settingsState.enablePassthrough,
            mDanmakuPlayer = mDanmakuPlayer,
            mediaUri = mediaUri,
            useVlc = useVlc,
            onHideControls = { videoPlayerState.hideControls() },
            onVideoSelected = { videoItem ->
                val encodedUri = videoItem.uri.toBase64()
                val encodedName = videoItem.fileName.toBase64()
                val encodedConn = videoItem.connectionName.toBase64()
                navController.navigate("VideoPlayer/$encodedUri/${videoItem.dataSourceType}/$encodedName/$encodedConn") {
                    popUpTo("VideoPlayer/{sourceUri}/{dataSourceType}/{fileName}/{connectionName}") { inclusive = true }
                }
            }
        )

        // 状态层 (Error, Buffering, FirstLoad)
        VideoPlayerStatusLayer(
            playerStatus = playerStatus,
            isFirstLoad = isFirstLoad
        )
    }
}

/**
 * 网速显示组件
 *
 * @param networkSpeed 当前网络速度 (bytes/sec)
 * @param modifier 修饰符
 */
@Composable
fun NetworkSpeedIndicator(networkSpeed: Long, modifier: Modifier = Modifier) {
    // 格式化网速显示文本
    val bps = stringResource(R.string.unit_bps)
    val kbps = stringResource(R.string.unit_kbps)
    val mbps = stringResource(R.string.unit_mbps)
    
    val speedText = when {
        networkSpeed < 1024 -> "$networkSpeed $bps"
        networkSpeed < 1024 * 1024 -> "${
            String.format(
                LocalLocale.current.platformLocale,
                "%.1f",
                networkSpeed / 1024.0
            )
        } $kbps"

        else -> "${
            String.format(
                LocalLocale.current.platformLocale,
                "%.1f",
                networkSpeed / (1024.0 * 1024.0)
            )
        } $mbps"
    }

    // 绘制包含网速文本的 Box
    Box(
        modifier = modifier
            .background(
                Color.Black.copy(alpha = 0.6f),
                shape = RoundedCornerShape(4.dp)
            ) // 半透明黑色背景和圆角
            .padding(horizontal = 8.dp, vertical = 4.dp) // 内边距
    ) {
        Text(
            text = speedText, // 显示的文本
            color = Color.White, // 文本颜色
            fontSize = 14.sp // 字体大小
        )
    }
}

/**
 * 为 Modifier 添加 D-Pad 事件处理
 *
 * @param
 * @param videoPlayerState 视频播放器状态
 * @param pulseState 脉冲状态
 * @param videoPlayerViewModel ViewModel
 * @return 添加了事件处理的 Modifier
 */
private fun Modifier.dPadEvents(
    player: IMzPlayer,
    videoPlayerState: VideoPlayerState,
    pulseState: VideoPlayerPulseState,
    videoPlayerViewModel: VideoPlayerViewModel,
    dpadUpAction: String,
    dpadDownAction: String,
    ffDuration: Int,
    rwDuration: Int
): Modifier = this.onKeyEvent { keyEvent ->
    val isActionDown = keyEvent.nativeKeyEvent.action == KeyEvent.ACTION_DOWN
    val isActionUp = keyEvent.nativeKeyEvent.action == KeyEvent.ACTION_UP
    val isLongPress = keyEvent.nativeKeyEvent.repeatCount > 0

    when (keyEvent.key) {
        Key.DirectionLeft -> {
            if (isActionDown) {
                if (isLongPress) {
                    if (!videoPlayerState.controlsVisible) {
                        videoPlayerState.showControls()
                    }
                    return@onKeyEvent true
                }
            } else if (isActionUp) {
                if (!videoPlayerState.controlsVisible) {
                    player.seekBack(rwDuration * 1000L)
                    pulseState.setType(VideoPlayerPulse.Type.BACK)
                    videoPlayerState.showControls()
                    return@onKeyEvent true
                }
            }
            false
        }

        Key.DirectionRight -> {
            if (isActionDown) {
                if (isLongPress) {
                    if (!videoPlayerState.controlsVisible) {
                        videoPlayerState.showControls()
                    }
                    return@onKeyEvent true
                }
            } else if (isActionUp) {
                if (!videoPlayerState.controlsVisible) {
                    player.seekForward(ffDuration * 1000L)
                    pulseState.setType(VideoPlayerPulse.Type.FORWARD)
                    videoPlayerState.showControls()
                    return@onKeyEvent true
                }
            }
            false
        }

        Key.DirectionCenter, Key.Enter -> {
            if (isActionUp) {
                if (videoPlayerState.controlsVisible) {
                    if (player.isPlaying) {
                        player.pause()
                    } else {
                        player.play()
                        videoPlayerState.hideControls()
                    }
                } else {
                    if (player.isPlaying) {
                        player.pause()
                        videoPlayerState.showControls()
                    } else {
                        player.play()
                    }
                }
                return@onKeyEvent true
            }
            false
        }

        Key.DirectionUp -> {
            if (isActionUp) {
                if (!videoPlayerState.controlsVisible && !videoPlayerViewModel.atpVisibility) {
                    videoPlayerViewModel.selectedAorVorS = dpadUpAction
                    videoPlayerViewModel.atpVisibility = true
                    return@onKeyEvent true
                }
            } else if (isActionDown && isLongPress) {
                videoPlayerViewModel.selectedAorVorS = "ROOT"
                videoPlayerViewModel.atpVisibility = true
                return@onKeyEvent true
            }
            false
        }

        Key.DirectionDown -> {
            if (isActionUp) {
                if (!videoPlayerState.controlsVisible && !videoPlayerViewModel.atpVisibility) {
                    videoPlayerViewModel.selectedAorVorS = dpadDownAction
                    videoPlayerViewModel.atpVisibility = true
                    return@onKeyEvent true
                }
            }
            false
        }

        Key.Menu, Key.ButtonY -> {
            if (isActionUp) {
                videoPlayerViewModel.selectedAorVorS = "ROOT"
                videoPlayerViewModel.atpVisibility = true
                return@onKeyEvent true
            }
            false
        }

        else -> {
            if (keyEvent.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_MENU) {
                if (isActionUp) {
                    videoPlayerViewModel.selectedAorVorS = "ROOT"
                    videoPlayerViewModel.atpVisibility = true
                    return@onKeyEvent true
                }
                false
            } else false
        }
    }
}




// 返回按钮按压状态枚举
sealed class BackPress {
    data object Idle : BackPress()
    data object InitialTouch : BackPress()
}



