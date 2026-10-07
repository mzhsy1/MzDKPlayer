/*
 * Copyright 2023 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.mz.mzdkplayer.ui.videoplayer.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp

/**
 * 控制栏里的进度条。**只负责显示**，左右键不在这里处理。
 *
 * 长按快进/快退统一由播放页（`VideoPlayerScreen` 的 D-Pad 处理）驱动：这里跟着 `progress` 画即可。
 * 之前这里各写了一套「长按加速」并依赖焦点态（`isSelected` 恒为 false 导致本地累加的进度画不出来），
 * 结果是控制栏可见时长按既看不到定位、步长也不放大 —— 现在只有一条路径，行为一致。
 */
@Composable
fun RowScope.VideoPlayerControllerIndicator(
    progress: Float,
    state: VideoPlayerState,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val color = Color.White
    val animatedIndicatorHeight by animateDpAsState(
        targetValue = 5.dp.times(if (isFocused) 2.5f else 1f), label = ""
    )

    // 焦点到达进度条时续一下控制栏的自动隐藏计时
    LaunchedEffect(isFocused) {
        state.showControls()
    }

    Canvas(
        modifier = modifier
            .weight(1f)
            .height(animatedIndicatorHeight)
            .padding(horizontal = 4.dp)
            .focusable(interactionSource = interactionSource),
        onDraw = {
            val yOffset = size.height.div(2)
            // 片长还没读到时分母为 0，进度会是 NaN；NaN 参与绘制会让整条进度条消失
            val fraction = progress.takeIf { it.isFinite() }?.coerceIn(0f, 1f) ?: 0f
            val currentProgressX = size.width.times(fraction)
            drawLine(
                color = color.copy(alpha = 0.24f),
                start = Offset(x = 0f, y = yOffset),
                end = Offset(x = size.width, y = yOffset),
                strokeWidth = size.height,
                cap = StrokeCap.Round
            )
            drawLine(
                color = color,
                start = Offset(x = 0f, y = yOffset),
                end = Offset(x = currentProgressX, y = yOffset),
                strokeWidth = size.height,
                cap = StrokeCap.Round
            )
            // 进度条末尾的小圆球
            drawCircle(
                color = color,
                radius = size.height * 0.8f,
                center = Offset(x = currentProgressX, y = yOffset)
            )
        }
    )
}
