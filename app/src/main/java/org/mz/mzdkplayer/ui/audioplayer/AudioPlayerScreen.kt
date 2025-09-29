package org.mz.mzdkplayer.ui.audioplayer

import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.kuaishou.akdanmaku.DanmakuConfig
import com.kuaishou.akdanmaku.ui.DanmakuPlayer
import kotlinx.coroutines.delay
import org.mz.mzdkplayer.R
import org.mz.mzdkplayer.tool.handleDPadKeyEvents
import org.mz.mzdkplayer.ui.audioplayer.components.AudioPlayerControlsIcon
import org.mz.mzdkplayer.ui.audioplayer.components.AudioPlayerOverlay
import org.mz.mzdkplayer.ui.audioplayer.components.AudioPlayerPulse
import org.mz.mzdkplayer.ui.audioplayer.components.AudioPlayerPulseState
import org.mz.mzdkplayer.ui.audioplayer.components.AudioPlayerSeeker
import org.mz.mzdkplayer.ui.audioplayer.components.AudioPlayerState
import org.mz.mzdkplayer.ui.audioplayer.components.rememberAudioPlayerPulseState
import org.mz.mzdkplayer.ui.audioplayer.components.rememberAudioPlayerState
import org.mz.mzdkplayer.ui.audioplayer.dPadEvents
import org.mz.mzdkplayer.ui.screen.vm.AudioPlayerViewModel
import org.mz.mzdkplayer.ui.screen.vm.VideoPlayerViewModel
import org.mz.mzdkplayer.ui.videoplayer.BackPress
import org.mz.mzdkplayer.ui.videoplayer.VideoPlayerControls
import org.mz.mzdkplayer.ui.videoplayer.components.BuilderMzPlayer
import org.mz.mzdkplayer.ui.videoplayer.components.VideoPlayerControlsIcon
import org.mz.mzdkplayer.ui.videoplayer.components.VideoPlayerMainFrame
import org.mz.mzdkplayer.ui.videoplayer.components.VideoPlayerMediaTitle
import org.mz.mzdkplayer.ui.videoplayer.components.VideoPlayerMediaTitleType
import org.mz.mzdkplayer.ui.videoplayer.components.VideoPlayerOverlay
import org.mz.mzdkplayer.ui.videoplayer.components.VideoPlayerPulse
import org.mz.mzdkplayer.ui.videoplayer.components.VideoPlayerPulseState
import org.mz.mzdkplayer.ui.videoplayer.components.VideoPlayerSeeker
import org.mz.mzdkplayer.ui.videoplayer.components.VideoPlayerState
import org.mz.mzdkplayer.ui.videoplayer.components.rememberPlayer
import org.mz.mzdkplayer.ui.videoplayer.components.rememberVideoPlayerPulseState
import org.mz.mzdkplayer.ui.videoplayer.components.rememberVideoPlayerState
import kotlin.time.Duration.Companion.milliseconds


@Composable
fun AudioPlayerScreen(mediaUri: String, dataSourceType: String) {
    val context = LocalContext.current
    val exoPlayer = rememberPlayer(context, mediaUri,dataSourceType)
    val audioPlayerState = rememberAudioPlayerState(hideSeconds = 6)
    val audioPlayerViewModel: AudioPlayerViewModel = viewModel()
    var showToast by remember { mutableStateOf(false) }
    var backPressState by remember { mutableStateOf<BackPress>(BackPress.Idle) }
    var contentCurrentPosition by remember { mutableLongStateOf(0L) }
    var isPlaying: Boolean by remember { mutableStateOf(exoPlayer.isPlaying) }

    BuilderMzPlayer(context, mediaUri, exoPlayer,dataSourceType)
    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }
    exoPlayer.addListener(object : Player.Listener {
        override fun onIsPlayingChanged(isExoPlaying: Boolean) {
            // super.onIsPlayingChanged(isPlaying)
            isPlaying = isExoPlaying

        }

    })
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
        AudioPlayerOverlay(
            modifier = Modifier.align(Alignment.BottomCenter),
            focusRequester = focusRequester,
            state = audioPlayerState,
            isPlaying = isPlaying,
            centerButton = { AudioPlayerPulse(pulseState) },
            subtitles = {  },
            controls = {

                AudioPlayerControls(
                    isPlaying,
                    contentCurrentPosition,
                    exoPlayer,
                    audioPlayerState,
                    focusRequester,
                    "asasas",
                    "sdssdsd",
                    "2022/1/20",
                    audioPlayerViewModel,

                )

            },
            atpFocus = audioPlayerViewModel.atpFocus
        )

    }
}

private fun Modifier.dPadEvents(
    exoPlayer: ExoPlayer,
    audioPlayerState: AudioPlayerState,
    pulseState: AudioPlayerPulseState,
    audioPlayerViewModel: AudioPlayerViewModel
): Modifier = this.handleDPadKeyEvents(
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
                AudioPlayerControlsIcon(
                    icon = painterResource(id = R.drawable.baseline_hd_24),
                    state = state,
                    isPlaying = isPlaying,
                    onClick = {
//                        videoPlayerViewModel.selectedAorVorS = "V"
//                        videoPlayerViewModel.atpVisibility = !videoPlayerViewModel.atpVisibility;focusRequester.requestFocus()
                    }
                )
                AudioPlayerControlsIcon(
                    modifier = Modifier.padding(start = 12.dp),
                    icon = painterResource(id = R.drawable.baseline_speaker_24),
                    state = state,
                    isPlaying = isPlaying,
                    onClick = {
//                        videoPlayerViewModel.selectedAorVorS = "A"
//                        videoPlayerViewModel.atpVisibility = !videoPlayerViewModel.atpVisibility;focusRequester.requestFocus()

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
                onSeek = { exoPlayer.seekTo(exoPlayer.duration.times(it).toLong()) },
                contentProgress = contentCurrentPosition.milliseconds,
                contentDuration = exoPlayer.duration.milliseconds
            )
        },
        more = null
    )
}