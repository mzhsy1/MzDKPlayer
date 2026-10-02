package org.mz.mzdkplayer.ui.phone.screen

import android.content.Context
import android.content.pm.ActivityInfo
import android.net.TrafficStats
import android.net.Uri
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.text.CueGroup
import androidx.media3.common.util.UnstableApi
import com.kuaishou.akdanmaku.render.SimpleRenderer
import com.kuaishou.akdanmaku.ui.DanmakuPlayer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.mz.mzdkplayer.R
import org.mz.mzdkplayer.common.KeepScreenOnManager
import org.mz.mzdkplayer.common.findActivity
import org.mz.mzdkplayer.danmaku.DanmakuData
import org.mz.mzdkplayer.danmaku.danmakuConfigFor
import org.mz.mzdkplayer.danmaku.getDanmakuXmlFromFile
import org.mz.mzdkplayer.danmaku.pushDanmakuConfig
import org.mz.mzdkplayer.danmaku.toDanmakuItems
import org.mz.mzdkplayer.data.datasource.SmbUtils
import org.mz.mzdkplayer.data.model.MediaHistoryRecord
import org.mz.mzdkplayer.data.model.VideoItem
import org.mz.mzdkplayer.data.repository.DanmakuSettingsManager
import org.mz.mzdkplayer.data.repository.VideoPlaylistRepository
import org.mz.mzdkplayer.player.core.IMzPlayer
import org.mz.mzdkplayer.player.core.autoLoadSameNameSubtitles
import org.mz.mzdkplayer.player.exo.MzExoPlayer
import org.mz.mzdkplayer.player.vlc.MzVlcPlayer
import org.mz.mzdkplayer.tool.FileTimeResolver
import org.mz.mzdkplayer.tool.Tools
import org.mz.mzdkplayer.tool.logic.PhonePlayerLogic
import org.mz.mzdkplayer.tool.logic.PhoneSettingsLogic
import org.mz.mzdkplayer.tool.logic.PlayerMediaText
import org.mz.mzdkplayer.ui.phone.component.PhoneLongPressSpeedOverlay
import org.mz.mzdkplayer.ui.phone.component.PhonePlayerBanner
import org.mz.mzdkplayer.ui.phone.component.PhonePlayerControls
import org.mz.mzdkplayer.ui.phone.component.PhonePlayerGestureLayer
import org.mz.mzdkplayer.ui.phone.component.PhonePlayerPanel
import org.mz.mzdkplayer.ui.phone.component.PhonePlayerSheet
import org.mz.mzdkplayer.ui.phone.component.PhonePlayerVideoInfo
import org.mz.mzdkplayer.ui.phone.component.PhonePlayerSurface
import org.mz.mzdkplayer.ui.phone.component.PhoneSeekPulse
import org.mz.mzdkplayer.ui.phone.component.PhoneSeekPulseOverlay
import org.mz.mzdkplayer.viewmodel.MediaHistoryViewModel
import org.mz.mzdkplayer.viewmodel.MediaMetaViewModel
import org.mz.mzdkplayer.viewmodel.SettingsViewModel
import org.mz.mzdkplayer.viewmodel.VideoPlayerStatus
import org.mz.mzdkplayer.viewmodel.VideoPlayerViewModel
import java.io.InputStream
import java.net.URL
import kotlin.math.abs

/** 播放历史的定时落盘间隔，与电视端 / 手机端音频页同一口径 */
private const val HISTORY_SAVE_INTERVAL_MS = 10_000L

/** 相比上一次落盘，播放位置至少要推进这么多毫秒才值得再写一次库 */
private const val HISTORY_SAVE_MIN_DELTA_MS = 1_000L

/** 进度与播放状态的心跳间隔：进度条、时间、自动隐藏都要跟着它走 */
private const val POSITION_POLL_INTERVAL_MS = 200L

/** 双击快进 / 快退的中央提示显示多久 */
private const val SEEK_PULSE_DURATION_MS = 700L

/** 「继续播放」的判定阈值：与 `PhonePlayerLogic.shouldOfferResume` 的默认值保持一致 */
private const val RESUME_MIN_MS = 5_000L

/**
 * 手机端视频播放页（第七阶段：功能对齐电视端，交互按触屏重做）。
 *
 * **功能**与电视端 `VideoPlayerScreen` 一致：Exo / VLC 双内核（传输流与蓝光原盘强制 VLC）、
 * 同名字幕自动加载与外挂字幕、自定义字幕外观与 PGS 居中、弹幕（加载 / 配置 / 面板）、
 * 视频轨 / 音轨 / 字幕轨 / ISO 标题选择、字幕时间轴偏移、倍速、画面比例与锁定、
 * 播放列表与「播放下一个」、网速显示、播放进度落盘与「继续播放」。
 *
 * **交互**全部按触屏重做（电视端那一套 KeyEvent + 焦点体系在手机上不成立）：
 * - 单击画面显隐控制栏，5 秒无操作自动隐藏（暂停时不隐藏）；
 * - 进度条是可拖动的 `Slider`（电视端是左右键累计 + 落定）；
 * - 双击屏幕左右 1/3 快退 / 快进，中央给一个 700ms 的箭头提示；
 * - 所有面板收进底部弹出的 `ModalBottomSheet`（电视端是右侧抽屉 + 焦点回退）；
 * - 返回键交给导航默认行为（电视端的「按两次退出」是为了防遥控器误触，手机不需要）。
 *
 * 与电视端共用 `IMzPlayer`（含播放偏好记忆）、`SubtitleView`、`DanmakuSettingsManager`、
 * `PlayerMediaText`、`FileTimeResolver`、`VideoPlaylistRepository`，以及 `:core` 里
 * 第七阶段新收拢的 `danmakuConfigFor` / `pushDanmakuConfig` / `toDanmakuItems` /
 * `PhonePlayerLogic`。
 */
@OptIn(UnstableApi::class)
@Composable
fun PhonePlayerScreen(
    mediaUri: String,
    dataSourceType: String,
    /** 原始文件名：既用来查刮削结果，也是写进播放历史的 `fileName` */
    fileName: String,
    connectionName: String,
    mediaMetaViewModel: MediaMetaViewModel,
    mediaHistoryViewModel: MediaHistoryViewModel,
    onBack: () -> Unit,
    /** 切换到播放列表里的另一条（「播放下一个」与播放列表面板共用） */
    onPlayOther: (VideoItem) -> Unit,
) {
    val context = LocalContext.current
    val settingsViewModel: SettingsViewModel = viewModel()
    val videoPlayerViewModel: VideoPlayerViewModel = viewModel()
    val settingsState by settingsViewModel.uiState.collectAsState()

    val activity = context.findActivity()

    // 播放中不让屏幕熄灭：引用计数式管理，切集时新旧页面短暂共存也不会互相清标志
    DisposableEffect(activity) {
        KeepScreenOnManager.acquire(activity?.window)
        onDispose { KeepScreenOnManager.release(activity?.window) }
    }

    // 沉浸式：进页面隐藏状态栏与导航栏（滑动边缘可临时唤出），退出时还给系统
    val view = LocalView.current
    DisposableEffect(view) {
        val controller = activity?.window?.let { WindowCompat.getInsetsController(it, view) }
        controller?.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller?.hide(WindowInsetsCompat.Type.systemBars())
        onDispose { controller?.show(WindowInsetsCompat.Type.systemBars()) }
    }

    // 内核选择与电视端同一口径：传输流 / 蓝光原盘强制 VLC，否则跟随设置里的默认内核
    val useVlc = remember(fileName, settingsState.defaultPlayer) {
        PhonePlayerLogic.forceVlcByExtension(fileName) || settingsState.defaultPlayer == "vlc"
    }
    val player: IMzPlayer = remember(useVlc, mediaUri) {
        if (useVlc) {
            MzVlcPlayer(
                context = context,
                mediaUri = mediaUri,
                dataSourceType = dataSourceType,
                settingsViewModel = settingsViewModel,
            )
        } else {
            MzExoPlayer(context, mediaUri, dataSourceType, settingsViewModel)
        }
    }

    // ==================== 基本状态 ====================
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var positionMs by remember { mutableLongStateOf(0L) }
    var durationMs by remember { mutableLongStateOf(0L) }
    var isFirstLoad by remember { mutableStateOf(true) }
    var cueGroup by remember { mutableStateOf<CueGroup?>(null) }
    var fileTimeMillis by remember { mutableLongStateOf(0L) }

    val isPlaying by player.isPlayingFlow.collectAsState()
    val playerStatus by player.playerStatus.collectAsState()
    val videoTracks by player.videoTracks.collectAsState()
    val audioTracks by player.audioTracks.collectAsState()
    val subtitleTracks by player.subtitleTracks.collectAsState()
    val isoTitles by player.isoTitles.collectAsState()
    val playbackSpeed by player.playbackSpeed.collectAsState()
    val currentAspectRatio by player.aspectRatio.collectAsState()

    // 刮削信息（只读 media_cache）+ 文件时间：展示标题与日期用
    val mediaMeta by mediaMetaViewModel.mediaMeta.collectAsState()
    LaunchedEffect(mediaUri, dataSourceType) {
        mediaMetaViewModel.load(mediaUri)
        fileTimeMillis = FileTimeResolver.resolve(mediaUri, dataSourceType) ?: 0L
    }
    val displayTitle = remember(fileName, mediaMeta) {
        PlayerMediaText.buildTitle(mediaMeta, fileName)
    }
    val displayDateText = remember(fileTimeMillis) {
        PlayerMediaText.buildFileDateText(fileTimeMillis)
    }

    // ==================== 控制栏与提示 ====================
    var controlsVisible by remember { mutableStateOf(true) }
    var interactionToken by remember { mutableIntStateOf(0) }
    var panel by remember { mutableStateOf<PhonePlayerPanel?>(null) }
    var pulse by remember { mutableStateOf<PhoneSeekPulse?>(null) }
    var transientMessage by remember { mutableStateOf<String?>(null) }
    var resumeTipVisible by remember { mutableStateOf(false) }
    var resumePositionMs by remember { mutableLongStateOf(0L) }
    var hasResumed by remember { mutableStateOf(false) }
    var hasAutoLoadedSubtitle by remember { mutableStateOf(false) }

    val showControls: () -> Unit = {
        controlsVisible = true
        interactionToken++
    }

    // 全屏（横屏）状态：进页面是竖屏，按全屏按钮转横屏、再按回到竖屏。
    // 离开页面必须把方向还给系统，否则横屏会一路带到浏览页与首页上。
    var isFullscreen by remember { mutableStateOf(false) }
    DisposableEffect(activity) {
        onDispose {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }
    val toggleFullscreen: () -> Unit = {
        val target = !isFullscreen
        isFullscreen = target
        activity?.requestedOrientation = if (target) {
            // 横屏且允许左右翻转：用 SENSOR_LANDSCAPE 而不是固定 LANDSCAPE，
            // 横过来哪边朝上都能看
            ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        } else {
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
        showControls()
    }

    // 控制栏自动隐藏的等待时长（「设置 → 播放与视频 → 触屏手势」里可调，3/5/8/10 秒）
    val controlsHideDelayMs = remember(settingsState.phoneControlsHideSeconds) {
        PhoneSettingsLogic.normalizeControlsHideSeconds(settingsState.phoneControlsHideSeconds) * 1_000L
    }

    // 播放中无操作就收起控制栏；暂停时一直留着（与电视端 `VideoPlayerState` 同一口径）
    LaunchedEffect(controlsVisible, interactionToken, isPlaying, panel, controlsHideDelayMs) {
        if (!controlsVisible || panel != null) return@LaunchedEffect
        delay(controlsHideDelayMs)
        if (player.isPlaying) controlsVisible = false
    }

    // ---- 长按临时倍速（手机端特色）----
    // 进入时先记住当前倍速，退出时还原：改的是同一个 `setPlaybackSpeed`，
    // 所以松手后浮层里显示的倍速与长按前一致（长按不改变「整段视频的倍速」这个设置）
    var longPressActive by remember { mutableStateOf(false) }
    var speedBeforeLongPress by remember { mutableFloatStateOf(1f) }

    val startLongPressSpeed: () -> Unit = {
        if (!longPressActive && player.isPlaying) {
            speedBeforeLongPress = player.playbackSpeed.value
            player.setPlaybackSpeed(
                PhoneSettingsLogic.normalizeLongPressSpeed(settingsState.phoneLongPressSpeedValue)
            )
            longPressActive = true
        }
    }
    val endLongPressSpeed: () -> Unit = {
        if (longPressActive) {
            player.setPlaybackSpeed(speedBeforeLongPress)
            longPressActive = false
        }
    }

    LaunchedEffect(pulse) {
        if (pulse != null) {
            delay(SEEK_PULSE_DURATION_MS)
            pulse = null
        }
    }

    // 只有「不需要用户点按钮」的提示才自动消失；继续播放那条留给用户处理
    LaunchedEffect(transientMessage) {
        if (transientMessage != null) {
            delay(3_000L)
            transientMessage = null
        }
    }

    val isLastVideoMessage = stringResource(R.string.ui_label_last_video)

    // ==================== 播放历史 ====================
    var lastSavedPosition by remember { mutableLongStateOf(-1L) }

    val savePlaybackHistory: () -> Unit = {
        val position = player.currentPosition
        val total = player.duration
        if (position > 0 && total > 0) {
            mediaHistoryViewModel.saveHistory(
                MediaHistoryRecord(
                    mediaUri = mediaUri,
                    fileName = fileName,
                    playbackPosition = position,
                    mediaDuration = total,
                    protocolName = if (dataSourceType == "LOCAL") "LOCAL" else dataSourceType,
                    connectionName = connectionName,
                    serverAddress = "",
                    mediaType = "VIDEO",
                    timestamp = System.currentTimeMillis(),
                )
            )
            lastSavedPosition = position
        }
    }
    // 定时任务与 onDispose 都活得比一次重组久，必须读最新一版的保存逻辑
    val latestSavePlaybackHistory by rememberUpdatedState(savePlaybackHistory)

    LaunchedEffect(player, mediaUri) {
        while (true) {
            delay(HISTORY_SAVE_INTERVAL_MS)
            // 只有真正在播放时才写：暂停 / 缓冲中写进去的可能是无效位置
            if (!player.isPlaying) continue
            val position = player.currentPosition
            if (position <= 0) continue
            if (abs(position - lastSavedPosition) < HISTORY_SAVE_MIN_DELTA_MS) continue
            latestSavePlaybackHistory()
        }
    }

    // 退出前再存一次，保证库里是最新位置
    DisposableEffect(player) {
        onDispose {
            latestSavePlaybackHistory()
            player.release()
        }
    }

    // 读上次看到哪儿；真正跳转留到播放就绪之后（此刻 seek 会被随后的 prepare 覆盖）
    LaunchedEffect(mediaUri) {
        val history = mediaHistoryViewModel.getHistoryPosition(mediaUri)
        if (PhonePlayerLogic.shouldOfferResume(history, RESUME_MIN_MS)) {
            resumePositionMs = history
        }
    }

    // ==================== 进度与状态轮询 ====================
    LaunchedEffect(player) {
        while (true) {
            positionMs = player.currentPosition
            durationMs = player.duration
            delay(POSITION_POLL_INTERVAL_MS)
        }
    }

    // 回调只在 `DisposableEffect(player)` 里装一次，直接读 `settingsState` 会捕获到进入页面那一刻的
    // 值（改完「播放完成动作」要等重进页面才生效）；这里过一层 state，读到的始终是最新值
    val finishAction by rememberUpdatedState(settingsState.videoFinishAction)

    DisposableEffect(player) {
        player.onError = { message -> errorMessage = message }
        player.onCuesChanged = { cues -> cueGroup = cues as? CueGroup }
        player.onPlaybackEnded = {
            when (finishAction) {
                0 -> {
                    player.seekTo(0)
                    player.play()
                }

                1 -> player.pause()

                else -> {
                    val playlist = VideoPlaylistRepository.getPlaylist()
                    val index = PhonePlayerLogic.playlistIndexOf(playlist.map { it.uri }, mediaUri)
                    val next = PhonePlayerLogic.nextPlaylistIndex(playlist.size, index)
                    if (next != null) {
                        onPlayOther(playlist[next])
                    } else {
                        transientMessage = isLastVideoMessage
                    }
                }
            }
        }
        onDispose {
            player.onError = null
            player.onCuesChanged = null
            player.onPlaybackEnded = null
        }
    }

    // 播放状态 → 首帧加载标记；就绪后按顺序做「自动字幕 → 历史跳转」
    LaunchedEffect(playerStatus) {
        if (playerStatus == VideoPlayerStatus.READY) {
            isFirstLoad = false

            if (!hasAutoLoadedSubtitle && settingsState.autoLoadSubtitle) {
                hasAutoLoadedSubtitle = true
                val count = runCatching {
                    autoLoadSameNameSubtitles(mediaUri, dataSourceType, player)
                }.getOrDefault(0)
                if (count > 0) {
                    transientMessage = context.getString(R.string.ui_label_auto_subtitle_found, count)
                }
                // Exo 加字幕会 setMediaItem 重建媒体源并再触发一次 READY；
                // 这时先返回，让历史跳转在下一次 READY 里执行，否则位置会被重置
                if (count > 0 && !useVlc) return@LaunchedEffect
            }

            if (resumePositionMs > 0 && !hasResumed) {
                player.seekTo(resumePositionMs)
                positionMs = resumePositionMs
                hasResumed = true
                resumeTipVisible = true
            }
        }
    }

    // 字幕时间轴偏移：Exo 在构造播放器时就带上了保存的偏移；VLC 要等媒体就绪后补设一次
    LaunchedEffect(playerStatus) {
        if (playerStatus == VideoPlayerStatus.READY) {
            player.setSubtitleDelay(settingsState.subtitleDelayMs)
        }
    }

    // 就绪后把「继续播放」提示收起来（用户没点也没关系，10 秒后自己消失）
    LaunchedEffect(resumeTipVisible) {
        if (resumeTipVisible) {
            delay(10_000L)
            resumeTipVisible = false
        }
    }

    // ==================== 弹幕 ====================
    val danmakuPlayer = remember(mediaUri) { DanmakuPlayer(SimpleRenderer()) }
    var danmakuData by remember { mutableStateOf<List<DanmakuData>?>(null) }
    var danmakuSent by remember { mutableStateOf(false) }

    DisposableEffect(danmakuPlayer) {
        onDispose { danmakuPlayer.release() }
    }

    // 同目录同名 `.xml`：地址构造失败只应导致「没有弹幕」，不能把播放页带崩
    LaunchedEffect(mediaUri, dataSourceType) {
        val danmakuUri = runCatching {
            if (dataSourceType == "NFS") {
                SmbUtils.getDanmakuNfsUri(mediaUri.toUri())
            } else {
                SmbUtils.getDanmakuSmbUri(mediaUri.toUri())
            }
        }.getOrNull()

        if (danmakuUri != null) {
            danmakuData = withContext(Dispatchers.IO) {
                runCatching {
                    openDanmakuStream(context, danmakuUri, dataSourceType)?.use { stream ->
                        getDanmakuXmlFromFile(stream).data
                    }
                }.getOrNull()
            }
        }
    }

    LaunchedEffect(danmakuData) {
        val data = danmakuData
        if (data != null && !danmakuSent) {
            danmakuPlayer.updateData(data.toDanmakuItems())
            danmakuSent = true
        }
    }

    // 进页面时把本地保存的弹幕设置推给渲染器（与电视端初始化那一段同一口径）
    LaunchedEffect(Unit) {
        val saved = DanmakuSettingsManager(context).loadSettings()
        videoPlayerViewModel.danmakuConfig =
            videoPlayerViewModel.danmakuConfigFor(saved, visibility = saved.isSwitchEnabled)
        videoPlayerViewModel.danmakuVisibility = saved.isSwitchEnabled
        danmakuPlayer.pushDanmakuConfig(videoPlayerViewModel.danmakuConfig)
    }

    LaunchedEffect(isPlaying, videoPlayerViewModel.danmakuVisibility) {
        if (videoPlayerViewModel.danmakuVisibility) {
            if (isPlaying) {
                danmakuPlayer.updateConfig(videoPlayerViewModel.danmakuConfig)
                danmakuPlayer.start()
                danmakuPlayer.seekTo(player.currentPosition)
            } else {
                danmakuPlayer.pause()
            }
        }
    }

    // ==================== 网速 ====================
    var networkSpeed by remember { mutableLongStateOf(0L) }
    LaunchedEffect(isPlaying) {
        if (!isPlaying) return@LaunchedEffect
        var lastBytes = TrafficStats.getTotalRxBytes()
        var lastStamp = System.currentTimeMillis()
        while (true) {
            delay(1_000L)
            val bytes = TrafficStats.getTotalRxBytes()
            val stamp = System.currentTimeMillis()
            val elapsed = stamp - lastStamp
            if (elapsed > 0 && bytes >= lastBytes) {
                networkSpeed = (bytes - lastBytes) * 1000 / elapsed
            }
            lastBytes = bytes
            lastStamp = stamp
        }
    }

    // ==================== 自定义字幕外观 ====================
    val customSubtitleFontFamily = remember(settingsState.subFontPath) {
        val path = settingsState.subFontPath
        if (path.isBlank()) {
            null
        } else {
            runCatching {
                val fontFile = java.io.File(path)
                if (fontFile.exists() && fontFile.isFile) FontFamily(Font(fontFile)) else null
            }.getOrNull()
        }
    }
    val customSubtitleStyle = remember(
        settingsState.subColor,
        settingsState.subFontSize,
        customSubtitleFontFamily,
    ) {
        TextStyle(
            color = Color(settingsState.subColor),
            fontSize = settingsState.subFontSize.sp,
            fontFamily = customSubtitleFontFamily,
            shadow = Shadow(
                color = Color.Black,
                offset = Offset(3f, 3f),
                blurRadius = 1f,
            ),
        )
    }

    // ==================== 播放列表 ====================
    val playlist by VideoPlaylistRepository.playlist.collectAsState()
    val playlistIndex = remember(playlist, mediaUri) {
        PhonePlayerLogic.playlistIndexOf(playlist.map { it.uri }, mediaUri)
    }
    val hasPrevious = playlistIndex > 0
    val nextIndex = PhonePlayerLogic.nextPlaylistIndex(playlist.size, playlistIndex)
    val hasNext = nextIndex != null

    val forwardSeconds = settingsState.ffDuration
    val backSeconds = settingsState.rwDuration

    fun seekBy(deltaMs: Long, pulseType: PhoneSeekPulse) {
        val target = PhonePlayerLogic.seekTargetMs(player.currentPosition, deltaMs, player.duration)
        if (target != null) {
            player.seekTo(target)
            positionMs = target
            pulse = pulseType
        }
    }

    // ==================== 界面 ====================
    // 竖屏（未全屏）时画面只占顶部一块、下方是信息区，与 B 站竖屏播放页同一套布局；
    // 全屏（横屏）时画面区就是整屏。所有播放器图层都装在「画面区」这一个 Box 里，
    // 于是「在信息区上下滑」不会被误当成调亮度 / 音量，也不会误触显隐控制栏。
    val videoRatio = remember(playerStatus, player.videoWidth, player.videoHeight) {
        val width = player.videoWidth
        val height = player.videoHeight
        if (width > 0 && height > 0) width.toFloat() / height else 16f / 9f
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // ── 画面区 ──
            Box(
                modifier = if (isFullscreen) {
                    Modifier.fillMaxSize()
                } else {
                    Modifier
                        .fillMaxWidth()
                        .statusBarsPadding() // 沉浸式下状态栏虽已隐藏，这块仍要避开挖孔
                        // 画面区在屏幕上半部分，底部的导航栏 inset 与它无关；
                        // 不消费掉的话控制栏会白抬一条导航栏的高度、贴不到画面下沿
                        .consumeWindowInsets(WindowInsets.navigationBars)
                        .aspectRatio(videoRatio)
                },
            ) {
                PhonePlayerSurface(
                    player = player,
                    useVlc = useVlc,
                    cueGroup = cueGroup,
                    subtitleStyle = customSubtitleStyle,
                    subBottomPadding = settingsState.subBottomPadding,
                    subBgColor = settingsState.subBgColor,
                    forcePgsCenter = settingsState.forcePgsCenter,
                    showCustomSubtitle = videoPlayerViewModel.isCusSubtitleViewVis,
                    danmakuPlayer = danmakuPlayer,
                    modifier = Modifier.fillMaxSize(),
                )

                // 手势层压在画面之上、控制层之下：控制层里没绑点击的留白会落到这一层
                PhonePlayerGestureLayer(
                    onTap = {
                        if (controlsVisible) controlsVisible = false else showControls()
                    },
                    onDoubleTapLeft = { seekBy(-backSeconds * 1000L, PhoneSeekPulse.BACK) },
                    onDoubleTapRight = { seekBy(forwardSeconds * 1000L, PhoneSeekPulse.FORWARD) },
                    enableBrightnessGesture = settingsState.phoneGestureBrightness,
                    enableVolumeGesture = settingsState.phoneGestureVolume,
                    enableLongPress = settingsState.phoneLongPressSpeed,
                    onLongPressStart = startLongPressSpeed,
                    onLongPressEnd = endLongPressSpeed,
                    modifier = Modifier.fillMaxSize(),
                )

                PhoneSeekPulseOverlay(
                    pulse = pulse,
                    seekStepSeconds = if (pulse == PhoneSeekPulse.BACK) backSeconds else forwardSeconds,
                    modifier = Modifier.fillMaxSize(),
                )

                // 长按期间浮出「2.0x 快进中」：长按只是临时改倍速，不给提示的话 2x 根本看不出来
                PhoneLongPressSpeedOverlay(
                    visible = longPressActive,
                    speedText = PhoneSettingsLogic.formatLongPressSpeed(settingsState.phoneLongPressSpeedValue),
                    modifier = Modifier.fillMaxSize(),
                )

                if (controlsVisible) {
                    PhonePlayerControls(
                        title = displayTitle,
                        dateText = displayDateText,
                        isPlaying = isPlaying,
                        positionMs = positionMs,
                        durationMs = durationMs,
                        hasPrevious = hasPrevious,
                        hasNext = hasNext,
                        forwardSeconds = forwardSeconds,
                        backSeconds = backSeconds,
                        networkSpeed = if (settingsState.hideNetworkSpeed || !isPlaying) {
                            null
                        } else {
                            networkSpeed
                        },
                        isFullscreen = isFullscreen,
                        onBack = onBack,
                        onTogglePlayPause = { if (player.isPlaying) player.pause() else player.play() },
                        onSeekTo = { target ->
                            player.seekTo(target)
                            positionMs = target
                        },
                        onSeekBack = { seekBy(-backSeconds * 1000L, PhoneSeekPulse.BACK) },
                        onSeekForward = { seekBy(forwardSeconds * 1000L, PhoneSeekPulse.FORWARD) },
                        onPrevious = {
                            if (hasPrevious) playlist.getOrNull(playlistIndex - 1)?.let(onPlayOther)
                        },
                        onNext = { nextIndex?.let { onPlayOther(playlist[it]) } },
                        onOpenPlaylist = {
                            panel = PhonePlayerPanel.PLAYLIST
                            showControls()
                        },
                        onOpenSettings = {
                            panel = PhonePlayerPanel.ROOT
                            showControls()
                        },
                        onToggleFullscreen = toggleFullscreen,
                        onInteraction = showControls,
                        modifier = Modifier.fillMaxSize(),
                    )
                }

                // 继续播放 / 一次性提示：抬到控制栏之上（这条提示可点，压在按钮上会挡住它）。
                // 竖屏时控制栏比横屏矮一截（没有进度条那一行），所以抬得少一些。
                PhonePlayerBanner(
                    message = when {
                        resumeTipVisible -> stringResource(
                            R.string.ui_label_resume_from_time,
                            (resumePositionMs / 1000 / 60).toInt(),
                            (resumePositionMs / 1000 % 60).toInt(),
                        )

                        else -> transientMessage
                    },
                    // 继续播放那条本身就是一句「点击此处从头播放」，整条可点
                    onClick = if (resumeTipVisible) {
                        {
                            player.seekTo(0L)
                            positionMs = 0L
                            resumeTipVisible = false
                        }
                    } else {
                        null
                    },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = if (isFullscreen) 180.dp else 56.dp),
                )
            }

            // ── 信息区：只在竖屏未全屏时占下半屏 ──
            if (!isFullscreen) {
                PhonePlayerVideoInfo(
                    meta = mediaMeta,
                    title = displayTitle,
                    connectionName = connectionName,
                    dataSourceType = dataSourceType,
                    dateText = displayDateText,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                )
            }
        }

        PhonePlayerStatusLayer(
            status = playerStatus,
            isFirstLoad = isFirstLoad,
            errorMessage = errorMessage,
            onBack = onBack,
        )

        val currentPanel = panel
        if (currentPanel != null) {
            PhonePlayerSheet(
                panel = currentPanel,
                onPanelChange = { panel = it },
                onDismiss = { panel = null },
                player = player,
                settingsViewModel = settingsViewModel,
                videoPlayerViewModel = videoPlayerViewModel,
                danmakuPlayer = danmakuPlayer,
                videoTracks = videoTracks,
                audioTracks = audioTracks,
                subtitleTracks = subtitleTracks,
                isoTitles = isoTitles,
                playbackSpeed = playbackSpeed,
                aspectRatio = currentAspectRatio,
                isPassthroughEnabled = settingsState.enablePassthrough,
                mediaUri = mediaUri,
                showCustomSubtitle = videoPlayerViewModel.isCusSubtitleViewVis,
                onShowCustomSubtitleChange = {
                    videoPlayerViewModel.isCusSubtitleViewVis = it
                },
                onLoadExternalSubtitles = {
                    loadExternalSubtitles(mediaUri, player)
                },
                onVideoSelected = { item ->
                    panel = null
                    onPlayOther(item)
                },
            )
        }
    }
}

/**
 * 同步加载同名的四个候选外挂字幕（ass / srt / ssa / vtt）。
 *
 * 与电视端的「加载外部字幕」是同一套候选与同一份 `[外部加载]` 标记口径：
 * 面板靠这个标记把外挂字幕与内嵌轨道区分开。
 */
private fun loadExternalSubtitles(mediaUri: String, player: IMzPlayer) {
    val lastDot = mediaUri.lastIndexOf('.')
    if (lastDot <= 0) return
    val basePath = mediaUri.substring(0, lastDot)
    val subtitles = listOf("ass", "srt", "ssa", "vtt").map { extension ->
        Tools.encodeUrlForPlayer("$basePath.$extension") to "[外部加载]$extension"
    }
    player.addExternalSubtitles(subtitles)
}

/**
 * 按 scheme 打开弹幕 XML 的输入流。
 *
 * 与电视端播放页里那段 `when(scheme)` 一致，差别只有两处：`file://` 走
 * [SmbUtils.openLocalFileInputStream]（手机端拿的是「所有文件访问」，绝对路径可直接读，
 * 与音频页同一口径），以及整段跑在 IO 线程上（电视端是在主线程直接开流）。
 */
private suspend fun openDanmakuStream(
    context: Context,
    uri: Uri,
    dataSourceType: String,
): InputStream? = withContext(Dispatchers.IO) {
    runCatching {
        when (uri.scheme?.lowercase()) {
            "smb" -> SmbUtils.openSmbFileInputStream(uri, "video")
            "ftp" -> SmbUtils.openFtpFileInputStream(uri, "video")
            "nfs" -> SmbUtils.openNfsFileInputStream(uri, "video")
            "file" -> SmbUtils.openLocalFileInputStream(uri)
            "http", "https" -> when (dataSourceType) {
                "WEBDAV" -> SmbUtils.openWebDavFileInputStream(uri, "video")
                "HTTP" -> SmbUtils.openHTTPLinkXmlInputStream(uri.toString(), "video")
                else -> URL(uri.toString()).openStream()
            }

            else -> null
        }
    }.getOrNull()
}

/**
 * 状态层：错误 / 缓冲 / 首帧加载。
 *
 * 这里刻意不用 `PhoneMessageBox` 那套（它取主题的 `onSurface`，浅色主题下压在黑色画面上
 * 会看不清），文字一律用白色；错误卡用 `errorContainer`，它在深浅两套主题里都是自洽的。
 */
@Composable
private fun BoxScope.PhonePlayerStatusLayer(
    status: VideoPlayerStatus,
    isFirstLoad: Boolean,
    errorMessage: String?,
    onBack: () -> Unit,
) {
    when {
        errorMessage != null -> Surface(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(24.dp),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = stringResource(R.string.phone_player_error),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(text = errorMessage, style = MaterialTheme.typography.bodySmall)
                FilledTonalButton(onClick = onBack) {
                    Text(stringResource(R.string.phone_action_back))
                }
            }
        }

        status == VideoPlayerStatus.BUFFERING || isFirstLoad -> Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            CircularProgressIndicator(
                color = Color.White,
                modifier = Modifier.size(36.dp),
            )
            Text(
                text = stringResource(
                    if (isFirstLoad) R.string.ui_label_initializing else R.string.ui_label_buffering
                ),
                color = Color.White,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(top = 12.dp),
            )
        }
    }
}
