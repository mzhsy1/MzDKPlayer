package org.mz.mzdkplayer.ui.videoplayer

import CustomSubtitleView
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
import androidx.compose.ui.unit.TextUnit
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
import org.mz.mzbi.ui.videoplayer.components.VideoTrackPanel
import org.mz.mzdkplayer.R
import org.mz.mzdkplayer.danmaku.DanmakuData
import org.mz.mzdkplayer.danmaku.DanmakuResponse
import org.mz.mzdkplayer.danmaku.getDanmakuXmlFromFile
import org.mz.mzdkplayer.tool.SmbUtils
import org.mz.mzdkplayer.tool.handleDPadKeyEvents
import org.mz.mzdkplayer.ui.screen.vm.VideoPlayerViewModel
import org.mz.mzdkplayer.ui.videoplayer.components.AkDanmakuPlayer
import org.mz.mzdkplayer.ui.videoplayer.components.AudioTrackPanel
import org.mz.mzdkplayer.ui.videoplayer.components.BuilderMzPlayer
import org.mz.mzdkplayer.ui.videoplayer.components.SubtitleTrackPanel
import org.mz.mzdkplayer.ui.videoplayer.components.VideoPlayerControlsIcon
import org.mz.mzdkplayer.ui.videoplayer.components.VideoPlayerMainFrame
import org.mz.mzdkplayer.ui.videoplayer.components.VideoPlayerMediaTitle
import org.mz.mzdkplayer.ui.videoplayer.components.VideoPlayerMediaTitleType
import org.mz.mzdkplayer.ui.videoplayer.components.VideoPlayerOverlay
import org.mz.mzdkplayer.ui.videoplayer.components.VideoPlayerPulse
import org.mz.mzdkplayer.ui.videoplayer.components.VideoPlayerPulse.Type.BACK
import org.mz.mzdkplayer.ui.videoplayer.components.VideoPlayerPulse.Type.FORWARD
import org.mz.mzdkplayer.ui.videoplayer.components.VideoPlayerPulseState
import org.mz.mzdkplayer.ui.videoplayer.components.VideoPlayerSeeker
import org.mz.mzdkplayer.ui.videoplayer.components.VideoPlayerState
import org.mz.mzdkplayer.ui.videoplayer.components.rememberPlayer
import org.mz.mzdkplayer.ui.videoplayer.components.rememberVideoPlayerPulseState
import org.mz.mzdkplayer.ui.videoplayer.components.rememberVideoPlayerState
import java.io.InputStream
import java.net.URL
import kotlin.time.Duration.Companion.milliseconds

//var selectedAorV by mutableStateOf("A")

@OptIn(UnstableApi::class)
@Composable
fun VideoPlayerScreen(mediaUri: String, dataSourceType: String) {
    val context = LocalContext.current
    val exoPlayer = rememberPlayer(context, mediaUri,dataSourceType)
    val videoPlayerState = rememberVideoPlayerState(hideSeconds = 6)
    val videoPlayerViewModel: VideoPlayerViewModel = viewModel()
    var showToast by remember { mutableStateOf(false) }
    var backPressState by remember { mutableStateOf<BackPress>(BackPress.Idle) }
    var contentCurrentPosition by remember { mutableLongStateOf(0L) }
    var isPlaying: Boolean by remember { mutableStateOf(exoPlayer.isPlaying) }

    // 网速监控相关状态
    var networkSpeed by remember { mutableLongStateOf(0L) }
    var lastTotalRxBytes by remember { mutableLongStateOf(0L) }
    var lastTimeStamp by remember { mutableLongStateOf(0L) }

    val mDanmakuPlayer: DanmakuPlayer = remember { DanmakuPlayer(SimpleRenderer()) }

    val danmakuUri = SmbUtils.getDanmakuSmbUri(mediaUri.toUri())
    var currentCueGroup: CueGroup? by remember { mutableStateOf<CueGroup?>(null) }
    var danmakuConfig by remember { mutableStateOf(DanmakuConfig()) }

    // 弹幕数据
    var danmakuDataList by remember { mutableStateOf<List<DanmakuData>?>(null) }
    var isDanmakuLoaded by remember { mutableStateOf(false) }

    BuilderMzPlayer(context, mediaUri, exoPlayer,dataSourceType)
    DisposableEffect(Unit) {

        onDispose {
            exoPlayer.release()
            mDanmakuPlayer.release() // 释放弹幕播放器
        }
    }
    // 加载弹幕数据
    LaunchedEffect(danmakuUri) {
        Log.d("danmakuUri", danmakuUri.toString())
        Log.d("mediaUri", mediaUri.toString())
        try {
            Log.d("danmakuUriScheme", danmakuUri.scheme?.lowercase().toString())
            val inputStream: InputStream? = when (danmakuUri.scheme?.lowercase()) {

                "smb" -> {
                    // 使用 SMB 工具打开输入流
                    SmbUtils.openSmbFileInputStream(danmakuUri)

                }

                "http", "https" -> {
                    // 打开 HTTP 输入流
                    if (dataSourceType=="WEBDAV"){
                        SmbUtils.openWebDavFileInputStream(danmakuUri)
                    }else {
                        URL(danmakuUri.toString()).openStream()
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

                else -> {
                    // 不支持的 scheme
                    Log.w("VideoPlayerScreen", "Unsupported scheme for danmaku URI: $danmakuUri")
                    null
                }
            }

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
        if (isDanmakuLoaded && !hasSentDanmaku && danmakuDataList != null) {
            Log.d("danmakuData", "状态弹幕")
            // 发送所有弹幕数据到播放器
            val danmakuItemDataList = danmakuDataList?.map { danmakuData ->

                DanmakuItemData(
                    danmakuId = if (danmakuData.rowId != 0L) danmakuData.rowId else (Math.random() * 100000000).toLong(), // 使用解析的ID或生成随机ID
                    position = (danmakuData.time * 1000).toLong(), // 使用解析的时间戳
                    content = danmakuData.content, // 使用解析的文本
                    mode = when (danmakuData.mode) { // 映射模式
                        4 -> DanmakuItemData.DANMAKU_MODE_CENTER_TOP
                        5 -> DanmakuItemData.DANMAKU_MODE_CENTER_BOTTOM
                        else -> DanmakuItemData.DANMAKU_MODE_ROLLING
                    },
                    textSize = danmakuData.size, // 使用解析的大小或默认值
                    textColor = danmakuData.color, // 使用解析的颜色或默认白色
                    // 可以根据 DanmakuData 的其他字段设置更多属性
                )


            }
            if (danmakuItemDataList != null) {
                mDanmakuPlayer.updateData(danmakuItemDataList)
            }

            hasSentDanmaku = true
            danmakuConfig = danmakuConfig.copy(
                retainerPolicy = RETAINER_BILIBILI,
                textSizeScale = 1.0f,
                screenPart = 0.12f,

//                durationMs = 5000L,
//                rollingDurationMs = 24000L

            )
            //danmakuConfig.updateFilter()
            //logger.info { "Init danmaku config: $danmakuConfig" }
            mDanmakuPlayer.updateConfig(danmakuConfig)
            Log.i(
                "VideoPlayerScreen",
                "Sent ${danmakuDataList?.size ?: 0} danmaku items to player."
            )
        }

//        // 同步播放/暂停状态
//        if (isPlaying && hasSentDanmaku) {
//            // 确保弹幕播放器启动，传入配置和当前播放位置
//            if (!mDanmakuPlayer.isPlaying()) { // 检查是否已在播放
//                mDanmakuPlayer.start(danmakuConfig)
//                // 可能需要 seek 到正确位置，如果弹幕播放器支持的话
//                // mDanmakuPlayer.seekTo(contentCurrentPosition)
//            }
//        } else if (!isPlaying && hasSentDanmaku) {
//            if (mDanmakuPlayer.isPlaying()) { // 检查是否正在播放
//                mDanmakuPlayer.pause()
//            }
//        }
    }

    //exoPlayer.setMediaItem(MediaItem.fromUri("http://127.0.0.1:13656/27137672496.mpd"))
//    LaunchedEffect(Unit) {
//        val playerListener = object : Player.Listener {
//            override fun onTracksChanged(tracks: Tracks) {
//                super.onTracksChanged(tracks)
//                // 5. 在轨道变化时检查
//                isVisSub = updateSubtitleViewVisibility(exoPlayer, tracks)
//            }
//        }
//        exoPlayer.addListener(playerListener)
//
//    }
    // 每隔100毫秒获取字幕
    LaunchedEffect(Unit) {
        while (true) {
            delay(200)
            currentCueGroup = exoPlayer.currentCues
        }

    }

    // 网速监控
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
        color = Color.White, // 字幕颜色为黄色
        fontSize = 20.sp,     // 字幕字体大小为 20sp
        shadow = androidx.compose.ui.graphics.Shadow(
            color = Color.Black, // 黑色阴影
            offset = androidx.compose.ui.geometry.Offset(3f, 3f),
            blurRadius = 1f
        ),

        )
    LaunchedEffect(Unit) {
        while (true) {
            delay(200)
            contentCurrentPosition = exoPlayer.currentPosition
            isPlaying = exoPlayer.isPlaying
            // Log.d("isPlay",isPlaying.toString())
        }
        // 4. 启动一个协程来定期检查字幕


    }
//    // 模拟接收弹幕
//    LaunchedEffect(Unit) {
//        while (true) {
//            delay(1000) // 每秒添加一条弹幕
//            val data: DanmakuItemData = DanmakuItemData(
//                danmakuId = (Math.random() * 1000000).toInt().toLong(),
//                position = (mDanmakuPlayer.getCurrentTimeMs().plus(500)),
//                content = "sdsaaddsadsadd",
//                mode = DanmakuItemData.DANMAKU_MODE_ROLLING,
//                textSize = 50,
//                textColor = Color.White.toArgb()

//            )  // 数据解析

//            // mDanmakuPlayer.send(data)
//            //Log.d("DanmakuItemData",data.toString())

//        }

//    }
    // 状态管理
//    var resizeMode by remember {
//      //  mutableIntStateOf(AspectRatioFrameLayout.RESIZE_MODE_FIT)
//    }

    val pulseState = rememberVideoPlayerPulseState()
    exoPlayer.addListener(object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            super.onIsPlayingChanged(isPlaying)
            if (videoPlayerViewModel.danmakuVisibility) {
                if (isPlaying) {

                    mDanmakuPlayer.start(danmakuConfig)
                    mDanmakuPlayer.seekTo(contentCurrentPosition)

                } else {
                    mDanmakuPlayer.pause()
                }
            }
        }


    })
    Box(
        Modifier
            .dPadEvents(
                exoPlayer,
                videoPlayerState,
                pulseState,
                videoPlayerViewModel
            )
            .background(Color(0, 0, 0))
            .focusable()
            .fillMaxHeight()
            .fillMaxWidth()

    ) {
        val focusRequester = remember { FocusRequester() }
//        Surface {
//            PlayerSurface(
//                player = exoPlayer,
//                surfaceType = SURFACE_TYPE_SURFACE_VIEW,
//                modifier = Modifier.fillMaxWidth().fillMaxHeight(),
//            )
//        }
//        AndroidView(
//            modifier = Modifier.align(Alignment.Center).fillMaxWidth(),
//            factory = {
//                PlayerView(context).apply { useController = false }
//            },
//            update = { it.player = exoPlayer },
//            onRelease = { exoPlayer.release();atpVisibility = false;atpFocus = false }
//        )

//        LaunchedEffect(videoPlayerViewModel.isSubtitleViewVis) {
//            isVisSub =  videoPlayerViewModel.isSubtitleViewVis
//            playerView?.subtitleView?.visibility = isVisSub
//            Log.d("isSubtitleViewVis",videoPlayerViewModel.isSubtitleViewVis.toString()+isVisSub.toString())
//        }

        AndroidView(
            factory = { context ->
                PlayerView(context).apply{
                    //LayoutInflater.from(context).inflate(R.layout.player_view, null).findViewById<PlayerView>(R.id.player_view).apply {
                    useController = false // 如果你不需要控制器
                    // exoPlayer.setVideoSurfaceView(VideoDecoderGLSurfaceView(context) )
                    player = exoPlayer

                    // LINT.IfChange

                    //player?.setVideoSurfaceView(VideoDecoderGLSurfaceView(context) )
                    subtitleView?.visibility = videoPlayerViewModel.isSubtitleViewVis

                }

//                    val field = PlayerView::class.java.getDeclaredField("surfaceType")
//                    fieldp.isAccessible = true
//                    field.setInt(this, 4) // 2 可能对应 TEXTURE_VIEW，但需要查看源码确认
                //useController = false // 如果你不需要控制器
                // playView.player = exoPlayer
                //playView.subtitleView?.visibility = videoPlayerViewModel.isSubtitleViewVis



            },
            update = { playView ->
                // exoPlayer.setVideoSurfaceView(VideoDecoderGLSurfaceView(context) )
                playView.player = exoPlayer

                //danmakuConfig.updateCache()

                playView.subtitleView?.visibility = videoPlayerViewModel.isSubtitleViewVis
                // playView.resizeMode = resizeMode
            },
            modifier = Modifier.fillMaxSize(),
            onRelease = {
                exoPlayer.release()
            }
        )
        // 自定义字幕视图，显示 SRT 字幕 (从 CueGroup 中获取)
        CustomSubtitleView(
            cueGroup = currentCueGroup, // 传递 CueGroup?
            subtitleStyle = customSubtitleStyle,
            modifier = Modifier.align(Alignment.BottomCenter),
            backgroundColor = Color.Black.copy(alpha = 0.0f) // 半透明背景
        )


        // 弹幕层
        AkDanmakuPlayer(
            modifier = Modifier
                .fillMaxSize()
                .align(Alignment.TopCenter),
            danmakuPlayer = mDanmakuPlayer
        )

        // 实时网速显示
        if (isPlaying) {
            NetworkSpeedIndicator(
                networkSpeed = networkSpeed,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
            )
        }

        VideoPlayerOverlay(
            modifier = Modifier.align(Alignment.BottomCenter),
            focusRequester = focusRequester,
            state = videoPlayerState,
            isPlaying = isPlaying,
            centerButton = { VideoPlayerPulse(pulseState) },
            subtitles = { /* TODO Implement subtitles */ },
            controls = {

                VideoPlayerControls(
                    isPlaying,
                    contentCurrentPosition,
                    exoPlayer,
                    videoPlayerState,
                    focusRequester,
                    "asasas",
                    "sdssdsd",

                    "2022/1/20",
                    videoPlayerViewModel,
                    danmakuConfig,
                    mDanmakuPlayer
                )

            },
            atpFocus = videoPlayerViewModel.atpFocus
        )

        if (showToast) {
            Toast.makeText(context, "再按一次退出", Toast.LENGTH_SHORT).show()
            showToast = false
        }


        LaunchedEffect(key1 = backPressState) {
            if (backPressState == BackPress.InitialTouch) {
                delay(2000)
                backPressState = BackPress.Idle
            }
        }

        BackHandler(backPressState == BackPress.Idle) {
            backPressState = BackPress.InitialTouch
            showToast = true
        }
        AnimatedVisibility(
            videoPlayerViewModel.atpVisibility,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .widthIn(200.dp, 420.dp)
                .heightIn(200.dp, 420.dp)
                .align(AbsoluteAlignment.CenterRight)
                .offset(x = (-20).dp)
                .background(
                    Color(0, 0, 0, 193), shape = RoundedCornerShape(8.dp)
                )
                .handleDPadKeyEvents(
                    onRight = {
                        if (!videoPlayerState.controlsVisible) {
                            videoPlayerViewModel.atpVisibility = false
                        }
                    },
                )
                .onFocusChanged {
                    if (it.isFocused) {
                        videoPlayerViewModel.atpFocus = it.isFocused
                    } else {
                        videoPlayerState.hideControls()
                        videoPlayerViewModel.atpFocus = it.isFocused
                    }
                }) {
            when (videoPlayerViewModel.selectedAorVorS) {
                "A" -> AudioTrackPanel(
                    videoPlayerViewModel.selectedAtIndex,
                    onSelectedIndexChange = { videoPlayerViewModel.selectedAtIndex = it },
                    videoPlayerViewModel.mutableSetOfAudioTrackGroups, exoPlayer
                )

                "V" -> VideoTrackPanel(
                    videoPlayerViewModel.selectedVtIndex,
                    onSelectedIndexChange = { videoPlayerViewModel.selectedVtIndex = it },
                    videoPlayerViewModel.mutableSetOfVideoTrackGroups, exoPlayer
                )

                else -> {
                    SubtitleTrackPanel(
                        videoPlayerViewModel.selectedStIndex,
                        onSelectedIndexChange = { videoPlayerViewModel.selectedStIndex = it },
                        videoPlayerViewModel.mutableSetOfTextTrackGroups, exoPlayer
                    )
                }
            }
            BackHandler(true) {
                videoPlayerViewModel.atpVisibility = false
            }
        }


    }


}

// 网速显示组件
@Composable
fun NetworkSpeedIndicator(networkSpeed: Long, modifier: Modifier = Modifier) {
    val speedText = when {
        networkSpeed < 1024 -> "${networkSpeed} B/s"
        networkSpeed < 1024 * 1024 -> "${String.format("%.1f", networkSpeed / 1024.0)} KB/s"
        else -> "${String.format("%.1f", networkSpeed / (1024.0 * 1024.0))} MB/s"
    }

    Box(
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.6f), shape = RoundedCornerShape(4.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = speedText,
            color = Color.White,
            fontSize = 14.sp
        )
    }
}

private fun Modifier.dPadEvents(
    exoPlayer: ExoPlayer,
    videoPlayerState: VideoPlayerState,
    pulseState: VideoPlayerPulseState,
    videoPlayerViewModel: VideoPlayerViewModel
): Modifier = this.handleDPadKeyEvents(
    onLeft = {
        if (!videoPlayerState.controlsVisible) {
            exoPlayer.seekBack()
            pulseState.setType(BACK)
        }
    },
    onRight = {
        if (!videoPlayerState.controlsVisible) {
            exoPlayer.seekForward()
            pulseState.setType(FORWARD)
        }
    },
    onUp = { if (videoPlayerViewModel.atpFocus) videoPlayerState.showControls() },
    onDown = { if (videoPlayerViewModel.atpFocus) videoPlayerState.showControls(); },
    onEnter = {
        exoPlayer.pause()
        videoPlayerState.showControls()
    }
)


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

    val onPlayPauseToggle = { shouldPlay: Boolean ->
        if (shouldPlay) {
            exoPlayer.play()
        } else {
            exoPlayer.pause()
        }
    }

    VideoPlayerMainFrame(
        mediaTitle = {
            VideoPlayerMediaTitle(
                title = title,
                secondaryText = secondaryText,
                tertiaryText = tertiaryText,
                type = VideoPlayerMediaTitleType.DEFAULT
            )

        },
        mediaActions = {
            Row(
                modifier = Modifier.padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                VideoPlayerControlsIcon(
                    icon = painterResource(id = R.drawable.baseline_hd_24),
                    state = state,
                    isPlaying = isPlaying,
                    onClick = {
                        videoPlayerViewModel.selectedAorVorS = "V"
                        videoPlayerViewModel.atpVisibility = !videoPlayerViewModel.atpVisibility;focusRequester.requestFocus()
                    }
                )
                VideoPlayerControlsIcon(
                    modifier = Modifier.padding(start = 12.dp),
                    icon = painterResource(id = R.drawable.baseline_speaker_24),
                    state = state,
                    isPlaying = isPlaying,
                    onClick = {
                        videoPlayerViewModel.selectedAorVorS = "A"
                        videoPlayerViewModel.atpVisibility = !videoPlayerViewModel.atpVisibility;focusRequester.requestFocus()

                    }

                )
                VideoPlayerControlsIcon(
                    modifier = Modifier.padding(start = 12.dp),
                    icon = painterResource(id = R.drawable.baseline_subtitles_24),
                    state = state,
                    isPlaying = isPlaying,
                    onClick = {
                        videoPlayerViewModel.selectedAorVorS = "S"
                        videoPlayerViewModel.atpVisibility = !videoPlayerViewModel.atpVisibility;
                        focusRequester.requestFocus()
//                        videoPlayerViewModel.textSize += 0.05f
//                        Log.d("textSizeScale", videoPlayerViewModel.textSize.toString())
//                        Log.d("danmakuConfig", danmakuConfig.toString())
//
//                        danmakuPlayer.updateConfig(
//                            danmakuConfig.copy(
//                                textSizeScale = videoPlayerViewModel.textSize,
//                                retainerPolicy = RETAINER_BILIBILI
//                            )
//                        )

                    }
                )

                VideoPlayerControlsIcon(
                    modifier = Modifier.padding(start = 12.dp),
                    icon = if (videoPlayerViewModel.danmakuVisibility) painterResource(id = R.drawable.dmaopen) else painterResource(id = R.drawable.damclose) ,
                    state = state,
                    isPlaying = isPlaying,
                    onClick = {
//                        videoPlayerViewModel.selectedAorVorS = "A"
//                        videoPlayerViewModel.atpVisibility = !videoPlayerViewModel.atpVisibility;
//                        focusRequester.requestFocus()
                        videoPlayerViewModel.danmakuVisibility = !videoPlayerViewModel.danmakuVisibility
                        danmakuPlayer.updateConfig(
                            danmakuConfig.copy(
                                visibility = videoPlayerViewModel.danmakuVisibility,
                            ))
                        if (!videoPlayerViewModel.danmakuVisibility){
                            danmakuPlayer.stop()
                        }else{
                            danmakuPlayer.start(danmakuConfig)
                            danmakuPlayer.seekTo(contentCurrentPosition)
                        }
//                        )
                    }

                )
            }
        },
        seeker = {
            VideoPlayerSeeker(
                focusRequester,
                state,
                isPlaying,
                onPlayPauseToggle,
                onSeek = { exoPlayer.seekTo(exoPlayer.duration.times(it).toLong()) },
                contentProgress = contentCurrentPosition.milliseconds,
                contentDuration = exoPlayer.duration.milliseconds
            )
        },
        more = null
    )
}

sealed class BackPress {
    data object Idle : BackPress()
    data object InitialTouch : BackPress()
}

/**
 * 根据当前选中的轨道信息，更新 PlayerView 中 SubtitleView 的可见性。
 * 如果选中的文本轨道是 SRT，则隐藏 SubtitleView。
 *
 * @param player ExoPlayer 实例
 * @param tracks 当前的 Tracks 信息
 */
//@OptIn(UnstableApi::class)
//private fun updateSubtitleViewVisibility(player: ExoPlayer, tracks: Tracks): Int {
//
//    // SRT 字幕的 MIME 类型
//    val mimeTypeSRT = "application/x-subrip"
//
//
//    var isSrtTrackSelected = false
//
//    // 遍历所有轨道组
//    for (trackGroupInfo in tracks.groups) {
//        // 检查是否是文本轨道类型
//        if (trackGroupInfo.type == C.TRACK_TYPE_TEXT) {
//            val trackGroup = trackGroupInfo.mediaTrackGroup
//            // 遍历轨道组中的每个轨道
//            for (i in 0 until trackGroup.length) {
//                // 检查轨道是否被选中
//                if (trackGroupInfo.isTrackSelected(i)) {
//                    val format: Format = trackGroup.getFormat(i)
//                    // 检查 MIME 类型是否为 SRT
//                    // 注意：内嵌 SRT 在 MP4 中可能显示为 "text/x-subrip" 或 "application/x-subrip"
//                    // 或者检查 containerMimeType
//                    Log.d("SD2", "$format ${format.containerMimeType} ")
//                    if (mimeTypeSRT == format.codecs
//                    ) {
//                        isSrtTrackSelected = true
//                        break // 找到一个 SRT 轨道就够了
//                    }
//
//                }
//            }
//            if (isSrtTrackSelected) break // 找到就退出外层循环
//        }
//    }
//
//    // 根据是否选中了 SRT 轨道来设置可见性
//    if (isSrtTrackSelected) {
//        Log.d("SD", "SubtitleView set to GONE because SRT track is selected.")
//        return View.GONE

//    } else {
//        Log.d("SD", "SubtitleView set to VISIBLE because no SRT track is selected.")
//        return View.VISIBLE

//    }
//
//}

@Composable
fun SimpleStrokedText(
    text: String,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 20.sp,
    textColor: Color = Color.White,
    strokeColor: Color = Color.Black
) {
    Box(modifier = modifier) {
        // 绘制4次轻微偏移的文本来创建描边效果
        Text(
            text = text,
            color = strokeColor,
            fontSize = fontSize,
            modifier = Modifier.offset((-1).dp, 0.dp)
        )
        Text(
            text = text,
            color = strokeColor,
            fontSize = fontSize,
            modifier = Modifier.offset(1.dp, 0.dp)
        )
        Text(
            text = text,
            color = strokeColor,
            fontSize = fontSize,
            modifier = Modifier.offset(0.dp, (-1).dp)
        )
        Text(
            text = text,
            color = strokeColor,
            fontSize = fontSize,
            modifier = Modifier.offset(0.dp, 1.dp)
        )

        // 最上层的填充文字
        Text(text = text, color = textColor, fontSize = fontSize)
    }
}



