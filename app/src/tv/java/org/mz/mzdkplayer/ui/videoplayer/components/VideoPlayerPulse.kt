/*
 * Copyright 2024 Google LLC
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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Icon

import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.debounce
import org.mz.mzdkplayer.R
import org.mz.mzdkplayer.ui.audioplayer.components.AudioPlayerPulse

object VideoPlayerPulse {
    enum class Type { FORWARD, BACK, NONE }
}

@Composable
fun VideoPlayerPulse(
    state: VideoPlayerPulseState = rememberVideoPlayerPulseState()
) {
    val icon = when (state.type) {
        VideoPlayerPulse.Type.FORWARD -> painterResource(R.drawable.baseline_arrow_forward_ios_24)
        VideoPlayerPulse.Type.BACK -> painterResource(R.drawable.baseline_arrow_back_ios_new_24)
        VideoPlayerPulse.Type.NONE -> null
    }
    if (icon != null) {
        // === 核心修改点：使风格与 LoadingScreen 保持统一 ===
        Icon(
            painter = icon,
            contentDescription = null,
            // 🔑 修改图标本身颜色为白色，与 Loading 菊花颜色一致
            tint = Color.White,
            modifier = Modifier
                // 🔑 1. 修改背景：由白改黑，使用与 Loading 一致的 0.5f 透明度
                // 🔑 2. 修改形状：由 CircleShape 改为 8dp 圆角矩形，与 Loading 框一致
                .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                // 🔑 3. 增加内边距，使图标在黑框内有呼吸感，大小更接近 Loading 框
                .padding(5.dp)
                // 下面保持原样，控制图标本身大小
                .wrapContentSize()
                .width(85.dp).height(30.dp)
        )
    }
}

class VideoPlayerPulseState {
    private var _type by mutableStateOf(VideoPlayerPulse.Type.NONE)
    val type: VideoPlayerPulse.Type get() = _type

    private val channel = Channel<Unit>(Channel.CONFLATED)

    @OptIn(FlowPreview::class)
    suspend fun observe() {
        channel.consumeAsFlow()
            .debounce(2.seconds)
            .collect { _type = VideoPlayerPulse.Type.NONE }
    }

    fun setType(type: VideoPlayerPulse.Type) {
        _type = type
        channel.trySend(Unit)
    }
}

@Composable
fun rememberVideoPlayerPulseState() =
    remember { VideoPlayerPulseState() }.also { LaunchedEffect(it) { it.observe() } }
