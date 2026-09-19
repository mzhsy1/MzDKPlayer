package org.mz.mzdkplayer.ui.videoplayer.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import org.mz.mzdkplayer.R
import org.mz.mzdkplayer.tool.SubtitleOffsetLogic
import org.mz.mzdkplayer.tool.focusOnInitialVisibility
import org.mz.mzdkplayer.ui.screen.common.MyIconButton

/**
 * 字幕时间轴微调面板。
 *
 * 正值 = 字幕延后出现，负值 = 字幕提前出现，步进 0.5 秒。
 * 这里只负责显示与转发，真正作用到播放器由上层的 `IMzPlayer.setSubtitleDelay` 完成
 * （Exo 需要重建媒体源，VLC 是原生即时生效）。
 *
 * @param currentDelayMs 当前偏移量（毫秒），来自设置仓库，播放页与设置页共用同一个值
 * @param useVlc 当前用的是 VLC 内核：Exo 侧只能偏移文本字幕（PGS 等图形字幕的解析结果不带
 *   绝对时间，偏移加不上去），所以限制说明只在 Exo 下出现
 * @param onDelayChange 偏移量变化回调
 */
@Composable
fun SubtitleTimingPanel(
    currentDelayMs: Int,
    useVlc: Boolean,
    onDelayChange: (Int) -> Unit
) {
    val secondsUnit = stringResource(R.string.unit_seconds)

    // 本地即时显示：按键后数值立刻变，不用等设置仓库回调。
    // 以 currentDelayMs 为 key，外部（设置页 / 恢复的记录）改过之后会自动对齐。
    var displayDelayMs by remember(currentDelayMs) { mutableIntStateOf(currentDelayMs) }
    // 与其他面板一致：面板第一次摆好之后把焦点收到「提前」按钮上，不用用户先按方向键
    val isVis = remember { mutableStateOf(false) }

    fun apply(newDelayMs: Int) {
        displayDelayMs = newDelayMs
        onDelayChange(newDelayMs)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = stringResource(R.string.ui_label_subtitle_delay),
            fontWeight = FontWeight.Bold,
            color = Color.White,
            fontSize = 24.sp,
            modifier = Modifier.padding(start = 20.dp, top = 20.dp, bottom = 4.dp)
        )
        Text(
            text = stringResource(R.string.ui_label_subtitle_delay_hint),
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 13.sp,
            modifier = Modifier.padding(start = 20.dp, end = 12.dp, bottom = 4.dp)
        )
        if (!useVlc) {
            Text(
                text = stringResource(R.string.ui_label_subtitle_delay_text_only),
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 12.sp,
                modifier = Modifier.padding(start = 20.dp, end = 12.dp, bottom = 8.dp)
            )
        }

        // 三个按钮与数值框放在同一行，左边距与标题对齐（20dp），不再各写各的缩进
        Row(
            modifier = Modifier.padding(start = 20.dp, end = 12.dp, top = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircularIconButton(
                onClick = { apply(SubtitleOffsetLogic.step(displayDelayMs, -1)) },
                icon = Icons.Outlined.KeyboardArrowDown,
                modifier = Modifier.focusOnInitialVisibility(isVis),
                enabled = SubtitleOffsetLogic.canStep(displayDelayMs, -1)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .background(Color(0xFF333333), shape = RoundedCornerShape(6.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = SubtitleOffsetLogic.formatSeconds(displayDelayMs) + secondsUnit,
                    color = Color.White
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            CircularIconButton(
                onClick = { apply(SubtitleOffsetLogic.step(displayDelayMs, 1)) },
                icon = Icons.Outlined.KeyboardArrowUp,
                enabled = SubtitleOffsetLogic.canStep(displayDelayMs, 1)
            )
            Spacer(modifier = Modifier.width(16.dp))
            MyIconButton(
                text = stringResource(R.string.ui_label_reset),
                icon = R.drawable.sync24dp,
                onClick = { apply(0) },
                enabled = !SubtitleOffsetLogic.isDefault(displayDelayMs)
            )
        }
    }
}
