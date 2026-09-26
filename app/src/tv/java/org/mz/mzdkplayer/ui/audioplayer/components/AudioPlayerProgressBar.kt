package org.mz.mzdkplayer.ui.audioplayer.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlin.time.Duration



@Composable
fun AudioPlayerProgressBar(
    contentProgress: Duration,
    contentDuration: Duration,
    onSeek: (Float) -> Unit,
    state: AudioPlayerState
) {
    // 格式化时间
    val currentStr = formatDurationComp(contentProgress)
    val remaining = contentDuration - contentProgress
    // 显示为负数，例如 -03:20
    val remainingStr = "-" + formatDurationComp(remaining.coerceAtLeast(Duration.ZERO))

    // 进度比例
    val progress = if (contentDuration.inWholeMilliseconds > 0)
        (contentProgress / contentDuration).toFloat() else 0f

    // 改为 Column 布局，让进度条和数字上下排列
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 1. 进度指示器 (占据上方)
        // 移除了左右的 RowScope 限制，或者直接在 Column 里调用
        AudioPlayerControllerIndicator(
            progress = progress,
            onSeek = onSeek,
            state = state
        )

        // 2. 下方时间文字 (左右分布)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp), // 进度条和文字之间的间距
            horizontalArrangement = Arrangement.SpaceBetween, // 左右对齐的关键
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 左侧当前时间
            AudioPlayerControllerText(text = currentStr)

            // 右侧剩余时间
            AudioPlayerControllerText(text = remainingStr)
        }
    }
}

private fun formatDurationComp(duration: Duration): String {
    return duration.toComponents { h, m, s, _ ->
        if (h > 0) {
            "$h:${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}"
        } else {
            "${m}:${s.toString().padStart(2, '0')}"
        }
    }
}