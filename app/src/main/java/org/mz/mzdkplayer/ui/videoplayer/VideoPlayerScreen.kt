package org.mz.mzdkplayer.ui.videoplayer

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
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.common.util.Log
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.kuaishou.akdanmaku.DanmakuConfig
import com.kuaishou.akdanmaku.data.DanmakuItemData
import com.kuaishou.akdanmaku.ecs.DanmakuEngine
import com.kuaishou.akdanmaku.ext.RETAINER_BILIBILI
import com.kuaishou.akdanmaku.render.SimpleRenderer
import com.kuaishou.akdanmaku.ui.DanmakuPlayer
import com.kuaishou.akdanmaku.ui.DanmakuView
import kotlinx.coroutines.delay
import org.mz.mzdkplayer.R

import org.mz.mzdkplayer.tool.handleDPadKeyEvents
import org.mz.mzdkplayer.ui.videoplayer.components.AkDanmakuPlayer
import org.mz.mzdkplayer.ui.videoplayer.components.AudioTrackPanel
import org.mz.mzdkplayer.ui.videoplayer.components.BuilderMzPlayer
import org.mz.mzdkplayer.ui.videoplayer.components.VideoPlayerOverlay
import org.mz.mzdkplayer.ui.videoplayer.components.VideoPlayerPulseState
import org.mz.mzdkplayer.ui.videoplayer.components.VideoPlayerState
import kotlin.to
import org.mz.mzdkplayer.ui.videoplayer.components.VideoPlayerPulse.Type.BACK
import org.mz.mzdkplayer.ui.videoplayer.components.VideoPlayerPulse.Type.FORWARD
import org.mz.mzdkplayer.ui.videoplayer.components.rememberVideoPlayerPulseState
import org.mz.mzdkplayer.ui.videoplayer.components.rememberVideoPlayerState
import org.mz.mzdkplayer.ui.videoplayer.components.VideoPlayerPulse
import org.mz.mzdkplayer.ui.videoplayer.components.VideoPlayerSeeker
import org.mz.mzdkplayer.ui.videoplayer.components.VideoPlayerControlsIcon
import org.mz.mzdkplayer.ui.videoplayer.components.VideoPlayerMainFrame
import org.mz.mzdkplayer.ui.videoplayer.components.VideoPlayerMediaTitle
import org.mz.mzdkplayer.ui.videoplayer.components.VideoPlayerMediaTitleType
import org.mz.mzbi.ui.videoplayer.components.VideoTrackPanel
import org.mz.mzdkplayer.tool.handleDPadKeyEvents
import org.mz.mzdkplayer.ui.videoplayer.components.rememberPlayer
import org.mz.mzdkplayer.ui.screen.vm.VideoPlayerViewModel
import kotlin.time.Duration.Companion.milliseconds

var atpVisibility by mutableStateOf(false)
var atpFocus by mutableStateOf(false)
var selectedAorV by mutableStateOf("A")




@OptIn(UnstableApi::class)
@Composable
fun VideoPlayerScreen(smbUri: String,) {
    val context = LocalContext.current
    val exoPlayer = rememberPlayer(context)
    val videoPlayerState = rememberVideoPlayerState(hideSeconds = 6)
    val videoPlayerViewModel: VideoPlayerViewModel = viewModel()
    var showToast by remember { mutableStateOf(false) }
    var backPressState by remember { mutableStateOf<BackPress>(BackPress.Idle) }
    var contentCurrentPosition by remember { mutableLongStateOf(0L) }
    var isPlaying: Boolean by remember { mutableStateOf(exoPlayer.isPlaying) }
    val danmakuConfig by remember { mutableStateOf(DanmakuConfig()) }

    var danmakuView: DanmakuView? by remember { mutableStateOf(null) }
    var mDanmakuPlayer: DanmakuPlayer= remember {DanmakuPlayer(SimpleRenderer())}
    var danmakuEngine: DanmakuEngine? by remember { mutableStateOf(null) }



    BuilderMzPlayer(context,smbUri,exoPlayer)
    DisposableEffect(Unit) {

        onDispose {
            atpVisibility = false
            atpFocus = false
            exoPlayer.release()
        }
    }

    //exoPlayer.setMediaItem(MediaItem.fromUri("http://127.0.0.1:13656/27137672496.mpd"))
   danmakuConfig.copy(
        textSizeScale = 3.0f
    )

    LaunchedEffect(Unit) {
        while (true) {
            delay(300)
            contentCurrentPosition = exoPlayer.currentPosition
            isPlaying = exoPlayer.isPlaying
        }
    }
    // 模拟接收弹幕
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000) // 每秒添加一条弹幕
                val data: DanmakuItemData = DanmakuItemData(
                    danmakuId = (Math.random() * 1000000).toInt().toLong(),
                    position = (mDanmakuPlayer.getCurrentTimeMs().plus(500)).toLong(),
                    content = "sdsaaddsadsadd",
                    mode = DanmakuItemData.DANMAKU_MODE_ROLLING,
                    textSize = 50,
                    textColor = Color.White.toArgb()

                )  // 数据解析

            // mDanmakuPlayer.send(data)
                //Log.d("DanmakuItemData",data.toString())

        }

    }
    // 状态管理
    var resizeMode by remember {
        mutableIntStateOf(AspectRatioFrameLayout.RESIZE_MODE_FIT)
    }

    val pulseState = rememberVideoPlayerPulseState()
    exoPlayer.addListener(object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            super.onIsPlayingChanged(isPlaying)
            if (isPlaying){

                mDanmakuPlayer.start(danmakuConfig)
            }else{
                mDanmakuPlayer.pause()

            }
        }


    })
    Box(
        Modifier
            .dPadEvents(
                exoPlayer,
                videoPlayerState,
                pulseState
            )
            .background(Color(0, 0, 0))
            .focusable()
            .fillMaxHeight()
            .fillMaxWidth()

    ) {
        val focusRequester = remember { FocusRequester() }
        //val mediaSource: MediaSource =
        //DashMediaSource.Factory(dataSourceFactory)
        //    .createMediaSource(MediaItem.fromUri("https://media.w3.org/2010/05/sintel/trailer.mp4"))
        //exoPlayer.setMediaSource(mediaSource)
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
        AndroidView(
            factory = { context ->
                PlayerView(context).apply {
                    useController = false // 如果你不需要控制器
                    player = exoPlayer


                }
            },
            update = { view ->
                view.player = exoPlayer
                view.resizeMode = resizeMode
            },
            modifier = Modifier.fillMaxSize(),
            onRelease = {
                exoPlayer.release()
            }
        )
        // 弹幕层
        AkDanmakuPlayer(
            modifier = Modifier.fillMaxSize().align(Alignment.TopCenter),
            danmakuPlayer = mDanmakuPlayer
        )

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
                            "asasas","sdssdsd","2022/1/20"
                              ,videoPlayerViewModel,danmakuConfig, mDanmakuPlayer
                        )

                },
                atpFocus = atpFocus
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
            atpVisibility,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .widthIn(200.dp, 420.dp)
                .heightIn(200.dp, 420.dp)
                .align(AbsoluteAlignment.CenterRight)
                .offset(y = 20.dp)
                .background(
                    Color(0, 0, 0), shape = RoundedCornerShape(2)
                )
                .handleDPadKeyEvents(
                    onRight = {
                        if (!videoPlayerState.controlsVisible) {
                            atpVisibility = false
                        }
                    },
                )
                .onFocusChanged {
                    if (it.isFocused) {
                        atpFocus = it.isFocused
                    } else {
                        videoPlayerState.hideControls()
                        atpFocus = it.isFocused
                    }
                }) {
            if (selectedAorV == "A") AudioTrackPanel(
                videoPlayerViewModel.selectedAtIndex,
                onSelectedIndexChange = { videoPlayerViewModel.selectedAtIndex = it },
                videoPlayerViewModel.mutableSetOfAudioTrackGroups, exoPlayer
            )
            else VideoTrackPanel(
                videoPlayerViewModel.selectedVtIndex,
                onSelectedIndexChange = { videoPlayerViewModel.selectedVtIndex = it },
                videoPlayerViewModel.mutableSetOfVideoTrackGroups, exoPlayer
            )
            BackHandler(true) {
                atpVisibility = false
            }
        }


    }


}


private fun Modifier.dPadEvents(
    exoPlayer: ExoPlayer,
    videoPlayerState: VideoPlayerState,
    pulseState: VideoPlayerPulseState
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
    onUp = { if (atpFocus) videoPlayerState.showControls() },
    onDown = { if (atpFocus) videoPlayerState.showControls(); },
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
    title: String,secondaryText:String,tertiaryText:String,
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
                        selectedAorV = "V"
                        atpVisibility = !atpVisibility;focusRequester.requestFocus()
                    }
                )
                VideoPlayerControlsIcon(
                    modifier = Modifier.padding(start = 12.dp),
                    icon = painterResource(id = R.drawable.baseline_speaker_24),
                    state = state,
                    isPlaying = isPlaying,
                    onClick = {
                        selectedAorV = "A"
                        atpVisibility = !atpVisibility;focusRequester.requestFocus()

                    }

                )
                VideoPlayerControlsIcon(
                    modifier = Modifier.padding(start = 12.dp),
                    icon = painterResource(id = R.drawable.baseline_subtitles_24),
                    state = state,
                    isPlaying = isPlaying,
                    onClick = {
                        videoPlayerViewModel.textSize+=0.05f
                        Log.d("textSizeScale",videoPlayerViewModel.textSize.toString())
                        Log.d("danmakuConfig",danmakuConfig.toString())

                        danmakuPlayer.updateConfig( danmakuConfig.copy(textSizeScale = videoPlayerViewModel.textSize, retainerPolicy = RETAINER_BILIBILI))

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

