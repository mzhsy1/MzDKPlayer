package org.mz.mzdkplayer.ui.videoplayer.components

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.unit.dp
import com.kuaishou.akdanmaku.DanmakuConfig
import com.kuaishou.akdanmaku.ui.DanmakuPlayer
import org.mz.mzdkplayer.data.repository.DanmakuSettingsManager
import org.mz.mzdkplayer.player.core.IMzPlayer
import org.mz.mzdkplayer.player.core.MzIsoTitle
import org.mz.mzdkplayer.ui.screen.vm.VideoPlayerViewModel

@Composable
fun BoxScope.VideoPlayerOverlayLayer(
    videoPlayerViewModel: VideoPlayerViewModel,
    focusRequester: FocusRequester,
    videoPlayerState: VideoPlayerState,
    isPlaying: Boolean,
    pulseState: VideoPlayerPulseState,
    currentPositionProvider: () -> Long,
    player: IMzPlayer,
    fileName: String,
    statusText: String,
    isoTitles: List<MzIsoTitle>,
    mediaUri: String,
    useVlc: Boolean,
    mDanmakuPlayer: DanmakuPlayer,
    settingsManager: DanmakuSettingsManager,
    getDanmakuConfig: () -> DanmakuConfig
) {
    VideoPlayerOverlay(
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .onFocusChanged {
                videoPlayerViewModel.conFocus = it.isFocused
            },
        focusRequester = focusRequester,
        state = videoPlayerState,
        isPlaying = isPlaying,
        centerButton = { VideoPlayerPulse(pulseState) },
        subtitles = { },
        controls = {
            VideoPlayerControls(
                isPlaying = isPlaying,
                currentPositionProvider = currentPositionProvider,
                player = player,
                state = videoPlayerState,
                focusRequester = focusRequester,
                title = fileName,
                secondaryText = statusText,
                tertiaryText = "2022/1/20", // TODO: 获取真实日期或移除
                videoPlayerViewModel = videoPlayerViewModel,
                isoTitles = isoTitles,
                mediaUri = mediaUri,
                useVlc = useVlc,
                danmakuPlayer = mDanmakuPlayer,
                settingsManager = settingsManager,
                getDanmakuConfig = getDanmakuConfig
            )
        },
        atpFocus = videoPlayerViewModel.atpFocus
    )
}
