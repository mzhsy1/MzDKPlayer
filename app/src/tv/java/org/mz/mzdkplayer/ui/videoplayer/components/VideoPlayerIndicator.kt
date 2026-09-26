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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import kotlinx.coroutines.delay
import org.mz.mzdkplayer.tool.handleDPadKeyEvents
import org.mz.mzdkplayer.tool.ifElse

@Composable
fun RowScope.VideoPlayerControllerIndicator(
    progress: Float,
    onSeek: (seekProgress: Float) -> Unit,
    state: VideoPlayerState,
    modifier: Modifier = Modifier,
    durationMs: Long = 0,
    ffDurationS: Int = 15,
    rwDurationS: Int = 15,
    onTogglePlayPause: () -> Unit = {}
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isSelected by remember { mutableStateOf(false) }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val color by rememberUpdatedState(
        newValue = if (isSelected) MaterialTheme.colorScheme.primary
        else Color.White
    )
    val animatedIndicatorHeight by animateDpAsState(
        targetValue = 5.dp.times((if (isFocused) 2.5f else 1f)), label = ""
    )
    var seekProgress by remember { mutableFloatStateOf(0f) }
    var lastSeekTime by remember { mutableLongStateOf(0L) }
    val throttleMs = 500L

    LaunchedEffect(progress) {
        if (!isFocused) {
            seekProgress = progress
        }
    }

    LaunchedEffect(isFocused) {
        state.showControls()
    }

    // 落地跳转：确保停止操作后，最后停留的位置能被精确执行
    LaunchedEffect(seekProgress) {
        if (isFocused) {
            delay(600L) // 略长于节流时间，确保是最终停顿
            onSeek(seekProgress)
            lastSeekTime = System.currentTimeMillis()
            state.showControls()
        }
    }

    val handleSeekEventModifier = Modifier.handleDPadKeyEvents(
        onEnter = {
            onSeek(seekProgress)
            state.showControls()
        },
        onLeft = {
            val now = System.currentTimeMillis()
            if (now - lastSeekTime > throttleMs) {
                if (durationMs > 0) {
                    val delta = rwDurationS.toFloat() * 1000 / durationMs
                    seekProgress = (seekProgress - delta).coerceAtLeast(0f)
                } else {
                    seekProgress = (seekProgress - 0.016f).coerceAtLeast(0f)
                }
                onSeek(seekProgress)
                lastSeekTime = now
            }
            state.showControls()
        },
        onRight = {
            val now = System.currentTimeMillis()
            if (now - lastSeekTime > throttleMs) {
                if (durationMs > 0) {
                    val delta = ffDurationS.toFloat() * 1000 / durationMs
                    seekProgress = (seekProgress + delta).coerceAtMost(1f)
                } else {
                    seekProgress = (seekProgress + 0.016f).coerceAtMost(1f)
                }
                onSeek(seekProgress)
                lastSeekTime = now
            }
            state.showControls()
        }
    )

    val handleDpadCenterClickModifier = Modifier.handleDPadKeyEvents(
        onEnter = {
            onTogglePlayPause()
            state.hideControls()
        },
        onLeft = {
            val now = System.currentTimeMillis()
            if (now - lastSeekTime > throttleMs) {
                if (durationMs > 0) {
                    val delta = rwDurationS.toFloat() * 1000 / durationMs
                    seekProgress = (seekProgress - delta).coerceAtLeast(0f)
                } else {
                    seekProgress = (seekProgress - 0.016f).coerceAtLeast(0f)
                }
                onSeek(seekProgress)
                lastSeekTime = now
            }
            state.showControls()
        },
        onRight = {
            val now = System.currentTimeMillis()
            if (now - lastSeekTime > throttleMs) {
                if (durationMs > 0) {
                    val delta = ffDurationS.toFloat() * 1000 / durationMs
                    seekProgress = (seekProgress + delta).coerceAtMost(1f)
                } else {
                    seekProgress = (seekProgress + 0.016f).coerceAtMost(1f)
                }
                onSeek(seekProgress)
                lastSeekTime = now
            }
            state.showControls()
        },
    )
    Canvas(
        modifier = modifier
            .weight(1f)
            .height(animatedIndicatorHeight)
            .padding(horizontal = 4.dp) .ifElse(
                condition = isSelected,
                ifTrueModifier = handleSeekEventModifier,
                ifFalseModifier = handleDpadCenterClickModifier
            ).focusable(interactionSource = interactionSource),
        onDraw = {
            val yOffset = size.height.div(2)
            // 算出当前进度的 X 坐标
            val currentProgressX = size.width.times(if (isSelected) seekProgress else progress)
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
                end = Offset(
                    x = size.width.times(if (isSelected) seekProgress else progress),
                    y = yOffset
                ),
                strokeWidth = size.height,
                cap = StrokeCap.Round
            )
            // 3. 👇 新增：在进度条末尾画一个小圆球
            drawCircle(
                color = color,
                // 圆球的半径稍微比线条高一点，看起来会更饱满，比例你可以自己微调
                radius = size.height * 0.8f,
                center = Offset(x = currentProgressX, y = yOffset)
            )
        }
    )
}
