package org.mz.mzdkplayer.ui.audioplayer.components

import androidx.annotation.OptIn
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.painter.Painter
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
                // 组合按钮：点击循环切换模式（随机播放/重复模式）
                AudioPlayerControlsIcon(
                    icon = getCombinedModeIcon(exoPlayer.shuffleModeEnabled, exoPlayer.repeatMode), // 根据当前模式获取对应图标
                    state = state,
                    isPlaying = isPlaying,
                    onClick = {
                        // 循环切换模式：随机播放 -> 重复模式 OFF -> 重复模式 ONE -> 重复模式 ALL -> 随机播放
                        if (exoPlayer.shuffleModeEnabled) {
                            // 如果随机播放启用，关闭随机播放，设置为重复模式 OFF
                            exoPlayer.shuffleModeEnabled = false
                            exoPlayer.repeatMode = Player.REPEAT_MODE_OFF
                        } else {
                            // 如果随机播放未启用，循环切换重复模式
                            val currentMode = exoPlayer.repeatMode
                            val newMode = when (currentMode) {
                                Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ONE
                                Player.REPEAT_MODE_ONE -> Player.REPEAT_MODE_ALL
                                Player.REPEAT_MODE_ALL -> {
                                    // 如果是重复模式 ALL，开启随机播放
                                    exoPlayer.shuffleModeEnabled = true
                                    Player.REPEAT_MODE_OFF // 随机播放时通常设置为 OFF
                                }
                                else -> Player.REPEAT_MODE_OFF
                            }
                            if (!exoPlayer.shuffleModeEnabled) {
                                exoPlayer.repeatMode = newMode
                            }
                        }
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

// 根据随机播放和重复模式返回对应的组合图标资源
@Composable
private fun getCombinedModeIcon(isShuffleEnabled: Boolean, repeatMode: Int): Painter {
    return if (isShuffleEnabled) {
        // 如果随机播放启用，显示随机播放图标
        painterResource(id = R.drawable.shufflepaly)
    } else {
        // 如果随机播放未启用，根据重复模式显示图标
        when (repeatMode) {
            Player.REPEAT_MODE_OFF -> painterResource(id = R.drawable.listsx) // 列表顺序图标
            Player.REPEAT_MODE_ONE -> painterResource(id = R.drawable.repeatone) // 单曲循环图标
            Player.REPEAT_MODE_ALL -> painterResource(id = R.drawable.repeatlist) // 列表循环图标
            else -> painterResource(id = R.drawable.listsx) // 默认返回普通重复图标
        }
    }
}



