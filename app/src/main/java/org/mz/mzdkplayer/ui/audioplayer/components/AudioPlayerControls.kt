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
                    icon = painterResource(id = R.drawable.baseline_hd_24),
                    state = state,
                    isPlaying = isPlaying,
                    onClick = {
                        // 可以扩展为显示视频选项或切换音频质量
                    }
                )
                AudioPlayerControlsIcon(
                    modifier = Modifier.padding(start = 12.dp),
                    icon = painterResource(id = R.drawable.baseline_speaker_24),
                    state = state,
                    isPlaying = isPlaying,
                    onClick = {
                        // 可以扩展为显示音频设置
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
                contentDuration = contentDuration // 使用传入的安全 Duration
            )
        },
        more = null
    )
}



