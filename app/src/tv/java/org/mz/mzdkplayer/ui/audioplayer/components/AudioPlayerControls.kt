package org.mz.mzdkplayer.ui.audioplayer.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.media3.common.C
import org.mz.mzdkplayer.ui.audioplayer.components.AudioPlayerProgressBar


import androidx.compose.runtime.Composable
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import org.mz.mzdkplayer.R
import org.mz.mzdkplayer.ui.audioplayer.components.AudioPlayerControlsIcon
import org.mz.mzdkplayer.ui.audioplayer.components.AudioPlayerState
import org.mz.mzdkplayer.viewmodel.AudioPlayerViewModel
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds


@Composable
fun AudioPlayerControls(
    isPlaying: Boolean,
    currentPositionProvider: () -> Long,
    exoPlayer: ExoPlayer,
    state: AudioPlayerState,
    audioPlayerViewModel: AudioPlayerViewModel,
    contentDuration: Duration = 0.milliseconds
) {
    val onPlayPauseToggle = { shouldPlay: Boolean ->
        if (shouldPlay) exoPlayer.play() else exoPlayer.pause()
    }

    val onSeek = { progressRatio: Float ->
        val durationMs =
            if (exoPlayer.duration != C.TIME_UNSET) exoPlayer.duration else 0L
        exoPlayer.seekTo((durationMs * progressRatio).toLong())
    }

    // 🎯 Apple Music 控制岛主体
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 1.dp), // 轻微留白即可
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {

        // ===== 1️⃣ 进度条 =====
        AudioPlayerProgressBar(
            contentProgress = currentPositionProvider().milliseconds,
            contentDuration = contentDuration,
            onSeek = onSeek,
            state = state
        )

        // ===== 2️⃣ 控制按钮行 =====
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {

            // ───── 左侧功能区（播放模式 / 歌词）─────
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AudioPlayerControlsIcon(
                    icon = getCombinedModeIcon(
                        exoPlayer.shuffleModeEnabled,
                        exoPlayer.repeatMode
                    ),
                    modifier = Modifier
                        .size(32.dp),
                    state = state,
                    iconSize = 32,
                    isPlaying = isPlaying,
                    onClick = {
                        if (exoPlayer.shuffleModeEnabled) {
                            exoPlayer.shuffleModeEnabled = false
                            exoPlayer.repeatMode = Player.REPEAT_MODE_OFF
                        } else {
                            when (exoPlayer.repeatMode) {
                                Player.REPEAT_MODE_OFF ->
                                    exoPlayer.repeatMode = Player.REPEAT_MODE_ONE
                                Player.REPEAT_MODE_ONE ->
                                    exoPlayer.repeatMode = Player.REPEAT_MODE_ALL
                                Player.REPEAT_MODE_ALL -> {
                                    exoPlayer.shuffleModeEnabled = true
                                    exoPlayer.repeatMode = Player.REPEAT_MODE_OFF
                                }
                            }
                        }
                    }
                )

                AudioPlayerControlsIcon(
                    icon = painterResource(id = R.drawable.lyrics24dp),
                    state = state,
                    modifier = Modifier
                        .size(32.dp),
                    iconSize = 32,
                    isPlaying = isPlaying,
                    onClick = { /* 歌词模式切换 */ }
                )
            }

            // 自动把中间播放区推到视觉中心
            Spacer(modifier = Modifier.weight(1f))

            // ───── 中央播放控制区（Apple Music 核心）─────
            Row(
                horizontalArrangement = Arrangement.spacedBy(18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {

                AudioPlayerControlsIcon(
                    icon = painterResource(id = R.drawable.skipprevious),
                    state = state,
                    isPlaying = isPlaying,
                    modifier = Modifier
                        .size(32.dp),
                    iconSize = 32,
                    onClick = { exoPlayer.seekToPreviousMediaItem() }
                )

                AudioPlayerControlsIcon(
                    modifier = Modifier
                        .size(40.dp),
                    icon = if (isPlaying)
                        painterResource(id = R.drawable.baseline_pause_24)
                    else
                        painterResource(id = R.drawable.baseline_play_arrow_24),
                    iconSize = 40,
                    state = state,
                    isPlaying = isPlaying,
                    onClick = { onPlayPauseToggle(!isPlaying) }
                )

                AudioPlayerControlsIcon(
                    icon = painterResource(id = R.drawable.skipnext),
                    state = state,
                    isPlaying = isPlaying,
                    modifier = Modifier
                        .size(32.dp),
                    iconSize = 32,
                    onClick = { exoPlayer.seekToNextMediaItem() }
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // ───── 右侧功能区（播放列表等）─────
            Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AudioPlayerControlsIcon(
                    icon = painterResource(id = R.drawable.queuemusic),
                    state = state,
                    isPlaying = isPlaying,
                    modifier = Modifier
                        .size(32.dp),
                    iconSize = 32,
                    onClick = {
                        audioPlayerViewModel.atpVisibility =
                            !audioPlayerViewModel.atpVisibility
                    }
                )
                AudioPlayerControlsIcon(
                    icon = painterResource(id = R.drawable.info24dp),
                    state = state,
                    isPlaying = isPlaying,
                    iconSize = 32,
                    modifier = Modifier
                        .size(32.dp),
                    onClick = {

                    }
                )
            }
        }
    }
}


// 别忘了把这个辅助函数也放回 AudioPlayerControls.kt 文件底部
@Composable
private fun getCombinedModeIcon(isShuffleEnabled: Boolean, repeatMode: Int): Painter {
    return if (isShuffleEnabled) {
        painterResource(id = R.drawable.shufflepaly)
    } else {
        when (repeatMode) {
            Player.REPEAT_MODE_OFF -> painterResource(id = R.drawable.listsx)
            Player.REPEAT_MODE_ONE -> painterResource(id = R.drawable.repeatone)
            Player.REPEAT_MODE_ALL -> painterResource(id = R.drawable.repeatlist)
            else -> painterResource(id = R.drawable.listsx)
        }
    }
}