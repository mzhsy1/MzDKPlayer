package org.mz.mzdkplayer.ui.audioplayer.components

import androidx.annotation.OptIn
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import org.mz.mzdkplayer.R
import org.mz.mzdkplayer.ui.screen.vm.AudioPlayerViewModel
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

// --- 控件 ---
@OptIn(UnstableApi::class)
@Composable
fun AudioPlayerControls(
    isPlaying: Boolean,
    contentCurrentPosition: Long, // 安全的当前位置 (ms)
    exoPlayer: ExoPlayer,
    state: AudioPlayerState,
    focusRequester: FocusRequester,
    title: String?,
    secondaryText: String,
    tertiaryText: String,
    audioPlayerViewModel: AudioPlayerViewModel,
    contentDuration: Duration = 0.milliseconds // 添加 contentDuration 参数，默认值
) {
    val onPlayPauseToggle = { shouldPlay: Boolean ->
        if (shouldPlay) {
            exoPlayer.play()
        } else {
            exoPlayer.pause()
        }
    }

    AudioPlayerMainFrame(
        mediaTitle = {
            AudioPlayerMediaTitle(
                title = title,
                secondaryText = secondaryText,
                tertiaryText = tertiaryText,
                type = AudioPlayerMediaTitleType.DEFAULT
            )
        },
        mediaActions = {
            Row(
                modifier = Modifier.padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AudioPlayerControlsIcon(
                    icon = getRepeatModeIcon(exoPlayer.repeatMode), // 根据当前模式获取对应图标
                    state = state,
                    isPlaying = isPlaying,
                    onClick = {
                        // 切换重复模式
                        val currentMode = exoPlayer.repeatMode
                        val newMode = when (currentMode) {
                            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ONE
                            Player.REPEAT_MODE_ONE -> Player.REPEAT_MODE_ALL
                            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_OFF
                            else -> Player.REPEAT_MODE_OFF
                        }
                        exoPlayer.repeatMode = newMode
                    }
                )
                AudioPlayerControlsIcon(
                    modifier = Modifier.padding(start = 12.dp),
                    icon = painterResource(id = R.drawable.queuemusic),
                    state = state,
                    isPlaying = isPlaying,
                    onClick = {
                        audioPlayerViewModel.atpVisibility = !audioPlayerViewModel.atpVisibility
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
                onSeek = { progressRatio ->
                    // 确保 duration 有效
                    val durationMs = if (exoPlayer.duration != C.TIME_UNSET) exoPlayer.duration else 0L
                    val seekPosition = (durationMs * progressRatio).toLong()
                    exoPlayer.seekTo(seekPosition)
                },
                contentProgress = contentCurrentPosition.milliseconds, // 使用安全的当前位置
                contentDuration = contentDuration, // 使用传入的安全 Duration,
                exoPlayer
            )
        },
        more = null
    )
}

// 根据重复模式返回对应的图标资源
@Composable
private fun getRepeatModeIcon(repeatMode: Int): androidx.compose.ui.graphics.painter.Painter {
    return when (repeatMode) {
        Player.REPEAT_MODE_OFF -> painterResource(id = R.drawable.listsx) // 列表顺序图标
        Player.REPEAT_MODE_ONE -> painterResource(id = R.drawable.repeatone) // 单曲循环图标
        Player.REPEAT_MODE_ALL -> painterResource(id = R.drawable.repeatlist) // 列表循环图标
        else -> painterResource(id = R.drawable.listsx) // 默认返回普通重复图标
    }
}



