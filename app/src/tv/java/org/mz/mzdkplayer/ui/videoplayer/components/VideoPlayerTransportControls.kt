package org.mz.mzdkplayer.ui.videoplayer.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.mz.mzdkplayer.R

/**
 * 底部控制栏的换集按钮行：上一个视频 / 播放暂停 / 下一个视频。
 *
 * 这几个按钮是控制栏里唯一的可点击项，控制栏可见时按「下」键就能把焦点从进度条挪进来；
 * 控制栏收起后焦点回到播放页，左右键仍是快进 / 快退（播放页统一处理，见 `VideoPlayerScreen`）。
 *
 * 播放列表为空或已经到头时按钮压暗（0.35），点了会提示，不做静默失败。
 */
@Composable
fun VideoPlayerTransportControls(
    state: VideoPlayerState,
    isPlaying: Boolean,
    hasPrevious: Boolean,
    hasNext: Boolean,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    val playPauseLabel = stringResource(
        if (isPlaying) R.string.ui_label_pause else R.string.ui_label_play
    )

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(24.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        VideoPlayerControlsIcon(
            modifier = Modifier.alpha(if (hasPrevious) 1f else 0.35f),
            icon = painterResource(R.drawable.skipprevious),
            contentDescription = stringResource(R.string.ui_label_previous_video),
            tooltipText = stringResource(R.string.ui_label_previous_video),
            state = state,
            isPlaying = isPlaying,
            onClick = onPrevious
        )
        VideoPlayerControlsIcon(
            icon = painterResource(
                if (isPlaying) R.drawable.baseline_pause_24 else R.drawable.baseline_play_arrow_24
            ),
            contentDescription = playPauseLabel,
            tooltipText = playPauseLabel,
            state = state,
            isPlaying = isPlaying,
            buttonSize = 56.dp,
            onClick = onPlayPause
        )
        VideoPlayerControlsIcon(
            modifier = Modifier.alpha(if (hasNext) 1f else 0.35f),
            icon = painterResource(R.drawable.skipnext),
            contentDescription = stringResource(R.string.ui_label_next_video),
            tooltipText = stringResource(R.string.ui_label_next_video),
            state = state,
            isPlaying = isPlaying,
            onClick = onNext
        )
    }
}
