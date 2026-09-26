package org.mz.mzdkplayer.ui.audioplayer.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.media3.exoplayer.ExoPlayer
import org.mz.mzdkplayer.data.model.AudioItem
import org.mz.mzdkplayer.tool.handleDPadKeyEvents
import org.mz.mzdkplayer.viewmodel.AudioPlayerViewModel

@Composable
fun AudioPlaylistSidePanel(
    isVisible: Boolean,
    onVisibilityChange: (Boolean) -> Unit,
    audioPlayerViewModel: AudioPlayerViewModel,
    extraList: List<AudioItem>,
    exoPlayer: ExoPlayer,
    focusRequester: FocusRequester,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
            .width(300.dp)
            .fillMaxHeight()
            .background(Color.Black.copy(0.95f))
            .handleDPadKeyEvents(
                onRight = { true },
                onUp = { true },
                onDown = { true },
                onLeft = { true }
            )
            .onFocusChanged { focusState ->
                audioPlayerViewModel.atpFocus = focusState.isFocused
            }
    ) {
        when (audioPlayerViewModel.selectedAorVorS) {
            "L" -> AudioListPanel(
                audioPlayerViewModel.selectedAtIndex,
                onSelectedIndexChange = { audioPlayerViewModel.selectedAtIndex = it },
                extraList.toMutableList(),
                exoPlayer,
                audioPlayerViewModel
            )
        }
        // 列表显示时的返回键拦截
        BackHandler(true) {
            onVisibilityChange(false)
            audioPlayerViewModel.atpFocus = false
            focusRequester.requestFocus()
        }
    }
}
