package org.mz.mzdkplayer.ui.videoplayer.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.kuaishou.akdanmaku.ui.DanmakuPlayer
import kotlinx.coroutines.flow.StateFlow
import org.mz.mzdkplayer.player.core.IMzPlayer
import org.mz.mzdkplayer.player.core.MzBasicTrack
import org.mz.mzdkplayer.player.core.MzIsoTitle
import org.mz.mzdkplayer.player.core.MzVideoTrack
import org.mz.mzdkplayer.player.core.MzAspectRatio
import org.mz.mzdkplayer.tool.Tools
import org.mz.mzdkplayer.tool.handleDPadKeyEvents
import org.mz.mzdkplayer.ui.screen.vm.VideoPlayerViewModel

@Composable
fun BoxScope.VideoPlayerTrackSelectionPanel(
    videoPlayerViewModel: VideoPlayerViewModel,
    player: IMzPlayer,
    audioTracks: List<MzBasicTrack>,
    videoTracks: List<MzVideoTrack>,
    subtitleTracksFlow: StateFlow<List<MzBasicTrack>>,
    isoTitles: List<MzIsoTitle>,
    playbackSpeed: Float,
    currentAspectRatio: MzAspectRatio,
    enablePassthrough: Boolean,
    mDanmakuPlayer: DanmakuPlayer,
    mediaUri: String,
    onHideControls: () -> Unit
) {
    AnimatedVisibility(
        videoPlayerViewModel.atpVisibility,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = Modifier
            .widthIn(200.dp, 420.dp)
            .fillMaxHeight()
            .align(AbsoluteAlignment.CenterRight)
            .background(
                Color.Black.copy(0.8f), shape = RoundedCornerShape(2.dp)
            )
            .handleDPadKeyEvents(
                onRight = { true },
                onUp = { true },
                onDown = { true }
            )
            .onFocusChanged {
                if (it.isFocused) {
                    videoPlayerViewModel.atpFocus = it.isFocused
                } else {
                    onHideControls()
                    videoPlayerViewModel.atpFocus = it.isFocused
                }
            }
    ) {
        when (videoPlayerViewModel.selectedAorVorS) {
            "A" -> AudioTrackPanel(
                lists = audioTracks, onTrackSelected = { track ->
                    player.selectAudioTrack(track)
                }
            )

            "V" -> VideoTrackPanel(lists = videoTracks, onTrackSelected = { track ->
                player.selectVideoTrack(track)
            })

            "D" -> DanmakuPanel(
                mDanmakuPlayer,
                videoPlayerViewModel,
            )

            "ISO" -> IsoTitlePanel(
                lists = isoTitles,
                onTitleSelected = { title ->
                    player.selectIsoTitle(title.index)
                }
            )

            "SPEED" -> PlaybackSpeedPanel(
                currentSpeed = playbackSpeed,
                onSpeedSelected = { speed ->
                    player.setPlaybackSpeed(speed)
                },
                isPassthroughEnabled = enablePassthrough
            )

            "R" -> AspectRatioPanel(
                currentRatio = currentAspectRatio,
                onRatioSelected = { ratio ->
                    player.setAspectRatio(ratio)
                }
            )

            else -> {
                SubtitleTrackPanel(
                    subtitleTracks = subtitleTracksFlow,
                    onTrackSelected = { track ->
                        player.selectSubtitleTrack(track)
                    }, 
                    onLoadExternalSubtitle = {
                        val videoUri = mediaUri
                        val lastDotIndex = videoUri.lastIndexOf('.')
                        if (lastDotIndex > 0) {
                            val basePath = videoUri.substring(0, lastDotIndex)
                            val extensions = listOf("ass", "srt", "ssa", "vtt")
                            val subList = extensions.map { ext ->
                                val rawSubtitleUrl = "$basePath.$ext"
                                val safeSubtitleUrl = Tools.encodeUrlForPlayer(rawSubtitleUrl)
                                safeSubtitleUrl to "[外部加载]$ext"
                            }
                            player.addExternalSubtitles(subList)
                        }
                    }
                )
            }
        }
        BackHandler(true) {
            videoPlayerViewModel.atpVisibility = false
        }
    }
}
