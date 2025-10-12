package org.mz.mzdkplayer.ui.videoplayer

// 导入必要的库和组件

import android.net.TrafficStats
import android.widget.Toast
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.Player
import androidx.media3.common.text.CueGroup
import androidx.media3.common.util.Log
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

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

import org.mz.mzdkplayer.tool.SmbUtils
import org.mz.mzdkplayer.tool.SubtitleView
import org.mz.mzdkplayer.tool.handleDPadKeyEvents
import org.mz.mzdkplayer.ui.audioplayer.components.AudioTrackPanel
import org.mz.mzdkplayer.ui.screen.vm.VideoPlayerViewModel
import org.mz.mzdkplayer.ui.videoplayer.components.AkDanmakuPlayer
import org.mz.mzdkplayer.ui.videoplayer.components.BuilderMzPlayer
import org.mz.mzdkplayer.ui.videoplayer.components.SubtitleTrackPanel
import org.mz.mzdkplayer.ui.videoplayer.components.VideoPlayerControlsIcon
import org.mz.mzdkplayer.ui.videoplayer.components.VideoPlayerMainFrame
import org.mz.mzdkplayer.ui.videoplayer.components.VideoPlayerMediaTitle
import org.mz.mzdkplayer.ui.videoplayer.components.VideoPlayerMediaTitleType
import org.mz.mzdkplayer.ui.videoplayer.components.VideoPlayerOverlay
import org.mz.mzdkplayer.ui.videoplayer.components.VideoPlayerPulse
import org.mz.mzdkplayer.ui.videoplayer.components.VideoPlayerPulseState
import org.mz.mzdkplayer.ui.videoplayer.components.VideoPlayerSeeker
import org.mz.mzdkplayer.ui.videoplayer.components.VideoPlayerState
import org.mz.mzdkplayer.ui.videoplayer.components.VideoTrackPanel
import org.mz.mzdkplayer.ui.videoplayer.components.rememberPlayer
import org.mz.mzdkplayer.ui.videoplayer.components.rememberVideoPlayerPulseState
import org.mz.mzdkplayer.ui.videoplayer.components.rememberVideoPlayerState
import java.io.InputStream
import java.net.URL
import java.util.Locale
import kotlin.time.Duration.Companion.milliseconds

/**
 * 视频播放器主界面 Composable
 *
 * @param mediaUri 媒体文件的 URI
 * @param dataSourceType 数据源类型 (例如 "SMB", "HTTP", "NFS")
 */
@OptIn(UnstableApi::class)
@Composable
fun VideoPlayerScreen(mediaUri: String, dataSourceType: String) {
    // 获取当前 Compose 上下文
    val context = LocalContext.current
    // 记住并创建 ExoPlayer 实例
    val exoPlayer = rememberPlayer(context, mediaUri, dataSourceType)
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
    var isPlaying: Boolean by remember { mutableStateOf(exoPlayer.isPlaying) }

    // 网速监控相关状态
    // 状态：计算得到的网络速度 (bytes/sec)
    var networkSpeed by remember { mutableLongStateOf(0L) }
    // 状态：上一次记录的接收总字节数
    var lastTotalRxBytes by remember { mutableLongStateOf(0L) }
    // 状态：上一次记录的时间戳
    var lastTimeStamp by remember { mutableLongStateOf(0L) }

    // 记住并创建弹幕播放器实例
    val mDanmakuPlayer: DanmakuPlayer = remember { DanmakuPlayer(SimpleRenderer()) }

    // 根据媒体 URI 和数据源类型推断弹幕文件 URI
    val danmakuUri = if(dataSourceType=="NFS") SmbUtils.getDanmakuNfsUri(mediaUri.toUri()) else SmbUtils.getDanmakuSmbUri(mediaUri.toUri())
    // 状态：当前的字幕组 (CueGroup)
    var currentCueGroup: CueGroup? by remember { mutableStateOf<CueGroup?>(null) }
    // 状态：弹幕配置
    var danmakuConfig by remember { mutableStateOf(DanmakuConfig()) }

    // 弹幕数据
    // 状态：解析后的弹幕数据列表
    var danmakuDataList by remember { mutableStateOf<List<DanmakuData>?>(null) }
    // 状态：弹幕是否已加载完成
    var isDanmakuLoaded by remember { mutableStateOf(false) }
    val pulseState = rememberVideoPlayerPulseState()
    // 构建播放器 (设置媒体源等)
    BuilderMzPlayer(context, mediaUri, exoPlayer, dataSourceType)
    // 当 Composable 离开组合时，释放资源
    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
            mDanmakuPlayer.release() // 释放弹幕播放器
        }
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
                    SmbUtils.openSmbFileInputStream(danmakuUri)
                }
                "http", "https" -> {
                    // 打开 HTTP 输入流
                    when (dataSourceType) {
                        "WEBDAV" -> {
                            SmbUtils.openWebDavFileInputStream(danmakuUri)
                        }
                        "HTTP" -> {
                            SmbUtils.openHTTPLinkXmlInputStream(danmakuUri.toString())
                        }
                        else -> {
                            URL(danmakuUri.toString()).openStream()
                        }
                    }
                }
                "file" -> {
                    // 打开本地文件输入流
                    context.contentResolver.openInputStream(danmakuUri)
                        ?: throw java.io.IOException("Could not open file input stream for $danmakuUri")
                }
                "ftp" -> {
                    // 使用 SMB 工具打开输入流
                    SmbUtils.openFtpFileInputStream(danmakuUri)
                }
                "nfs" -> {
                    SmbUtils.openNfsFileInputStream(danmakuUri)
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
    LaunchedEffect(isDanmakuLoaded, danmakuDataList, isPlaying, contentCurrentPosition) {
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
            // 配置弹幕播放器
            danmakuConfig = danmakuConfig.copy(
                retainerPolicy = RETAINER_BILIBILI, // 设置弹幕保留策略
                textSizeScale = 1.0f, // 字体缩放
                screenPart = 0.12f, // 屏幕占用比例
            )
            mDanmakuPlayer.updateConfig(danmakuConfig)
            Log.i(
                "VideoPlayerScreen",
                "Sent ${danmakuDataList?.size ?: 0} danmaku items to player."
            )
        }
    }

    // 每隔200毫秒获取当前字幕 (CueGroup)
    LaunchedEffect(Unit) {
        while (true) {
            delay(200)
            //currentCueGroup = exoPlayer.currentCues
        }
    }

    // 网速监控协程
    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            // 初始化网络统计
            lastTotalRxBytes = TrafficStats.getTotalRxBytes()
            lastTimeStamp = System.currentTimeMillis()

            while (isPlaying) {
                delay(1000) // 每秒更新一次

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

    // 定义自定义字幕样式
    val customSubtitleStyle = TextStyle(
        color = Color.White, // 字幕颜色为白色
        fontSize = 20.sp,     // 字体大小
        shadow = androidx.compose.ui.graphics.Shadow(
            color = Color.Black, // 黑色阴影
            offset = androidx.compose.ui.geometry.Offset(3f, 3f),
            blurRadius = 1f
        ),
    )

    // 监听 ExoPlayer 播放状态变化，并更新 `isPlaying` 状态
    // 同时定期更新 `contentCurrentPosition`
    LaunchedEffect(Unit) {
        exoPlayer.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isExoPlaying: Boolean) {
                isPlaying = isExoPlaying
            }
        })
        while (true) {
            delay(200)
            contentCurrentPosition = exoPlayer.currentPosition
        }
    }

    // 为 ExoPlayer 添加监听器，用于同步弹幕播放状态和位置
    exoPlayer.addListener(object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            super.onIsPlayingChanged(isPlaying)
            // 如果弹幕可见性为 true
            if (videoPlayerViewModel.danmakuVisibility) {
                if (isPlaying) {
                    // 启动弹幕播放器并同步位置
                    mDanmakuPlayer.start(danmakuConfig)
                    mDanmakuPlayer.seekTo(contentCurrentPosition)
                } else {
                    // 暂停弹幕播放器
                    mDanmakuPlayer.pause()
                }
            }
        }
        override fun onCues(cueGroup: CueGroup) {
            currentCueGroup = cueGroup
        }

        // 当播放位置发生不连续变化时 (如 Seek)
        override fun onPositionDiscontinuity(
            oldPosition: Player.PositionInfo,
            newPosition: Player.PositionInfo,
            reason: Int
        ) {
            super.onPositionDiscontinuity(oldPosition, newPosition, reason)
            // 如果当前是暂停状态，同步弹幕播放器位置并暂停
            if (!isPlaying) {
                mDanmakuPlayer.seekTo(newPosition.contentPositionMs)
                mDanmakuPlayer.pause()
                Log.d("newPosition", newPosition.contentPositionMs.toString())
            }
        }
    })
//    LaunchedEffect(exoPlayer) {
//        exoPlayer.
//    }

    // 主 UI 布局
    Box(
        Modifier
            // 添加 D-Pad 事件处理
            .dPadEvents(
                exoPlayer,
                videoPlayerState,
                pulseState,
                videoPlayerViewModel
            )
            .background(Color(0, 0, 0)) // 黑色背景
            .focusable() // 可获得焦点
            .fillMaxHeight()
            .fillMaxWidth()
    ) {
        // 创建焦点请求器
        val focusRequester = remember { FocusRequester() }

   //      AndroidView 包裹 PlayerView，用于显示视频
        AndroidView(
            factory = { context ->
                PlayerView(context).apply{
                    useController = false // 禁用默认控制器
                    player = exoPlayer // 设置 ExoPlayer
                    // 根据 ViewModel 状态设置字幕视图可见性
                    subtitleView?.visibility = videoPlayerViewModel.isSubtitleViewVis
                }
            },
            update = { playView ->
                // 更新 PlayerView
                playView.player = exoPlayer
                playView.subtitleView?.visibility = videoPlayerViewModel.isSubtitleViewVis
            },
            modifier = Modifier.fillMaxSize(), // 填充整个父容器
            onRelease = {
                // 释放资源
                exoPlayer.release()
            }
        )
        //PlayerSurface(player = exoPlayer,modifier = Modifier.fillMaxSize(),SURFACE_TYPE_SURFACE_VIEW)
        // 显示 SRT/PSG/ ASS(ASS暂时由PlayerView显示,SubtitleView不显示)
        if (videoPlayerViewModel.isCusSubtitleViewVis) {
            // 字幕视图，显示 SRT/PSG/ASS(ASS暂时由PlayerView显示) 字幕 (从 CueGroup 中获取)
            SubtitleView(
                cueGroup = currentCueGroup, // 传递当前字幕组
                subtitleStyle = customSubtitleStyle, // 使用自定义字幕样式(只影响srt字幕)
                modifier = Modifier.align(Alignment.BottomCenter), // 底部居中对齐(只影响srt字幕)
                backgroundColor = Color.Black.copy(alpha = 0.5f) // 背景色(只影响srt字幕)
            )
        }
        // 弹幕层
        AkDanmakuPlayer(
            modifier = Modifier
                .fillMaxSize()
                .align(Alignment.TopCenter), // 顶部居中对齐
            danmakuPlayer = mDanmakuPlayer // 传递弹幕播放器实例
        )

        // 实时网速显示
        if (isPlaying) {
            NetworkSpeedIndicator(
                networkSpeed = networkSpeed, // 传递网络速度
                modifier = Modifier
                    .align(Alignment.TopEnd) // 右上角对齐
                    .padding(16.dp) // 内边距
            )
        }

        // 视频播放器覆盖层 (包含控制按钮等)
        VideoPlayerOverlay(
            modifier = Modifier.align(Alignment.BottomCenter), // 底部居中
            focusRequester = focusRequester, // 焦点请求器
            state = videoPlayerState, // 播放器状态
            isPlaying = isPlaying, // 播放状态
            centerButton = { VideoPlayerPulse(pulseState) }, // 中心脉冲按钮
            subtitles = {  }, // 子标题 (未实现)
            controls = {
                // 控制按钮区域
                VideoPlayerControls(
                    isPlaying, // 播放状态
                    contentCurrentPosition, // 当前位置
                    exoPlayer, // ExoPlayer 实例
                    videoPlayerState, // 播放器状态
                    focusRequester, // 焦点请求器
                    "asasas", // 标题 (示例)
                    "sdssdsd", // 副标题 (示例)
                    "2022/1/20", // 第三行文本 (示例)
                    videoPlayerViewModel, // ViewModel
                    danmakuConfig, // 弹幕配置
                    mDanmakuPlayer // 弹幕播放器
                )
            },
            atpFocus = videoPlayerViewModel.atpFocus // 音轨/字幕面板焦点状态
        )

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

        // 音轨/字幕选择面板的动画可见性
        AnimatedVisibility(
            videoPlayerViewModel.atpVisibility, // 根据 ViewModel 状态显示/隐藏
            enter = fadeIn(), // 淡入动画
            exit = fadeOut(), // 淡出动画
            modifier = Modifier
                .widthIn(200.dp, 420.dp) // 宽度范围
                .heightIn(200.dp, 420.dp) // 高度范围
                .align(AbsoluteAlignment.CenterRight) // 右侧居中
                .offset(x = (-20).dp) // 向左偏移
                .background(
                    Color(0, 0, 0, 193), shape = RoundedCornerShape(8.dp) // 半透明黑色背景和圆角
                )
                // 处理 D-Pad 事件
                .handleDPadKeyEvents(
                    onRight = {
                        if (!videoPlayerState.controlsVisible) {
                            videoPlayerViewModel.atpVisibility = false // 隐藏面板
                        }
                    },
                )
                // 处理焦点变化
                .onFocusChanged {
                    if (it.isFocused) {
                        videoPlayerViewModel.atpFocus = it.isFocused

                    } else {
                        videoPlayerState.hideControls() // 隐藏控制栏
                        videoPlayerViewModel.atpFocus = it.isFocused
                    }
                }) {
            // 根据 ViewModel 中的选择显示不同的面板
            when (videoPlayerViewModel.selectedAorVorS) {
                "A" -> AudioTrackPanel(
                    videoPlayerViewModel.selectedAtIndex, // 当前选中的音频轨道索引
                    onSelectedIndexChange = { videoPlayerViewModel.selectedAtIndex = it }, // 索引变化回调
                    videoPlayerViewModel.mutableSetOfAudioTrackGroups, // 音频轨道组
                    exoPlayer // ExoPlayer 实例
                )
                "V" -> VideoTrackPanel(
                    videoPlayerViewModel.selectedVtIndex, // 当前选中的视频轨道索引
                    onSelectedIndexChange = { videoPlayerViewModel.selectedVtIndex = it }, // 索引变化回调
                    videoPlayerViewModel.mutableSetOfVideoTrackGroups, // 视频轨道组
                    exoPlayer // ExoPlayer 实例
                )
                else -> {
                    SubtitleTrackPanel(
                        videoPlayerViewModel.selectedStIndex, // 当前选中的字幕轨道索引
                        onSelectedIndexChange = { videoPlayerViewModel.selectedStIndex = it }, // 索引变化回调
                        videoPlayerViewModel.mutableSetOfTextTrackGroups, // 字幕轨道组
                        exoPlayer // ExoPlayer 实例
                    )
                }
            }
            // 处理返回键，隐藏面板
            BackHandler(true) {
                videoPlayerViewModel.atpVisibility = false
            }
        }
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
    val speedText = when {
        networkSpeed < 1024 -> "$networkSpeed B/s"
        networkSpeed < 1024 * 1024 -> "${String.format(Locale.getDefault(),"%.1f", networkSpeed / 1024.0)} KB/s"
        else -> "${String.format(Locale.getDefault(),"%.1f", networkSpeed / (1024.0 * 1024.0))} MB/s"
    }

    // 绘制包含网速文本的 Box
    Box(
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.6f), shape = RoundedCornerShape(4.dp)) // 半透明黑色背景和圆角
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
 * @param exoPlayer ExoPlayer 实例
 * @param videoPlayerState 视频播放器状态
 * @param pulseState 脉冲状态
 * @param videoPlayerViewModel ViewModel
 * @return 添加了事件处理的 Modifier
 */
private fun Modifier.dPadEvents(
    exoPlayer: ExoPlayer,
    videoPlayerState: VideoPlayerState,
    pulseState: VideoPlayerPulseState,
    videoPlayerViewModel: VideoPlayerViewModel
): Modifier = this.handleDPadKeyEvents(
    onLeft = {
        // 如果控制栏未显示，则快退
        if (!videoPlayerState.controlsVisible) {
            exoPlayer.seekBack()
            pulseState.setType(VideoPlayerPulse.Type.BACK) // 设置脉冲类型
        }
    },
    onRight = {
        // 如果控制栏未显示，则快进
        if (!videoPlayerState.controlsVisible) {
            exoPlayer.seekForward()
            pulseState.setType(VideoPlayerPulse.Type.FORWARD) // 设置脉冲类型
        }
    },
    onUp = { if (videoPlayerViewModel.atpFocus) videoPlayerState.showControls() }, // 如果面板有焦点，显示控制栏
    onDown = { if (videoPlayerViewModel.atpFocus) videoPlayerState.showControls(); }, // 如果面板有焦点，显示控制栏
    onEnter = {
        // 暂停播放并显示控制栏
        exoPlayer.pause()
        videoPlayerState.showControls()
    }
)

/**
 * 视频播放器控制按钮区域
 *
 * @param isPlaying 当前播放状态
 * @param contentCurrentPosition 当前播放位置
 * @param exoPlayer ExoPlayer 实例
 * @param state 视频播放器状态
 * @param focusRequester 焦点请求器
 * @param title 标题
 * @param secondaryText 副标题
 * @param tertiaryText 第三行文本
 * @param videoPlayerViewModel ViewModel
 * @param danmakuConfig 弹幕配置
 * @param danmakuPlayer 弹幕播放器
 */
@OptIn(UnstableApi::class)
@Composable
fun VideoPlayerControls(
    isPlaying: Boolean,
    contentCurrentPosition: Long,
    exoPlayer: ExoPlayer,
    state: VideoPlayerState,
    focusRequester: FocusRequester,
    title: String, secondaryText: String, tertiaryText: String,
    videoPlayerViewModel: VideoPlayerViewModel,
    danmakuConfig: DanmakuConfig,
    danmakuPlayer: DanmakuPlayer
) {
    // 播放/暂停切换回调
    val onPlayPauseToggle = { shouldPlay: Boolean ->
        if (shouldPlay) {
            exoPlayer.play()
        } else {
            exoPlayer.pause()
        }
    }

    // 构建主框架
    VideoPlayerMainFrame(
        mediaTitle = {
            // 媒体标题区域
            VideoPlayerMediaTitle(
                title = title,
                secondaryText = secondaryText,
                tertiaryText = tertiaryText,
                type = VideoPlayerMediaTitleType.DEFAULT
            )
        },
        mediaActions = {
            // 媒体操作按钮区域 (音轨、字幕、弹幕开关)
            Row(
                modifier = Modifier.padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                VideoPlayerControlsIcon(
                    icon = painterResource(id = R.drawable.baseline_hd_24), // 高清图标
                    state = state,
                    isPlaying = isPlaying,
                    onClick = {
                        // 点击高清图标，显示视频轨道选择面板
                        videoPlayerViewModel.selectedAorVorS = "V"
                        videoPlayerViewModel.atpVisibility = !videoPlayerViewModel.atpVisibility; focusRequester.requestFocus()
                    }
                )
                VideoPlayerControlsIcon(
                    modifier = Modifier.padding(start = 12.dp),
                    icon = painterResource(id = R.drawable.baseline_speaker_24), // 音频图标
                    state = state,
                    isPlaying = isPlaying,
                    onClick = {
                        // 点击音频图标，显示音频轨道选择面板
                        videoPlayerViewModel.selectedAorVorS = "A"
                        videoPlayerViewModel.atpVisibility = !videoPlayerViewModel.atpVisibility; focusRequester.requestFocus()
                    }
                )
                VideoPlayerControlsIcon(
                    modifier = Modifier.padding(start = 12.dp),
                    icon = painterResource(id = R.drawable.baseline_subtitles_24), // 字幕图标
                    state = state,
                    isPlaying = isPlaying,
                    onClick = {
                        // 点击字幕图标，显示字幕轨道选择面板
                        videoPlayerViewModel.selectedAorVorS = "S"
                        videoPlayerViewModel.atpVisibility = !videoPlayerViewModel.atpVisibility;
                        focusRequester.requestFocus()
                    }
                )
                VideoPlayerControlsIcon(
                    modifier = Modifier.padding(start = 12.dp),
                    icon = if (videoPlayerViewModel.danmakuVisibility) painterResource(id = R.drawable.dmaopen) else painterResource(id = R.drawable.damclose), // 弹幕开关图标
                    state = state,
                    isPlaying = isPlaying,
                    onClick = {
                        // 点击弹幕开关图标，切换弹幕可见性
                        videoPlayerViewModel.danmakuVisibility = !videoPlayerViewModel.danmakuVisibility
                        // 更新弹幕配置
                        danmakuPlayer.updateConfig(
                            danmakuConfig.copy(
                                visibility = videoPlayerViewModel.danmakuVisibility,
                            )
                        )
                        Log.d("isPlay", isPlaying.toString())
                        // 根据播放状态和可见性控制弹幕播放器
                        if (!videoPlayerViewModel.danmakuVisibility) {
                            danmakuPlayer.pause()
                        } else {
                            if (isPlaying) {
                                danmakuPlayer.start(danmakuConfig)
                                danmakuPlayer.seekTo(contentCurrentPosition)
                            } else {
                                // 修复关闭弹幕在打开时如果视频处于暂停状态弹幕还会继续滚动
                                danmakuPlayer.seekTo(contentCurrentPosition)
                                danmakuPlayer.pause()
                            }
                        }
                    }
                )
            }
        },
        seeker = {
            // 进度条区域
            VideoPlayerSeeker(
                focusRequester,
                state,
                isPlaying,
                onPlayPauseToggle,
                onSeek = { exoPlayer.seekTo(exoPlayer.duration.times(it).toLong()) }, // Seek 回调
                contentProgress = contentCurrentPosition.milliseconds, // 当前进度
                contentDuration = exoPlayer.duration.milliseconds // 总时长
            )
        },
        more = null // 更多按钮 (未实现)
    )
}

// 返回按钮按压状态枚举
sealed class BackPress {
    data object Idle : BackPress()
    data object InitialTouch : BackPress()
}



