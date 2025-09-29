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

package org.mz.mzdkplayer.ui.audioplayer.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
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


object AudioPlayerPulse {
    enum class Type { FORWARD, BACK, NONE }
}

@Composable
fun AudioPlayerPulse(
    state: AudioPlayerPulseState = rememberAudioPlayerPulseState()
) {
    val icon = when (state.type) {
        AudioPlayerPulse.Type.FORWARD -> painterResource(R.drawable.baseline_arrow_forward_ios_24)
        AudioPlayerPulse.Type.BACK -> painterResource(R.drawable.baseline_arrow_back_ios_new_24)
        AudioPlayerPulse.Type.NONE -> null
    }
    if (icon != null) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier
                .background(Color.White.copy(alpha = 0.6f), CircleShape)
                .size(88.dp)
                .wrapContentSize()
                .size(48.dp)
        )
    }
}

class AudioPlayerPulseState {
    private var _type by mutableStateOf(AudioPlayerPulse.Type.NONE)
    val type: AudioPlayerPulse.Type get() = _type

    private val channel = Channel<Unit>(Channel.CONFLATED)

    @OptIn(FlowPreview::class)
    suspend fun observe() {
        channel.consumeAsFlow()
            .debounce(2.seconds)
            .collect { _type = AudioPlayerPulse.Type.NONE
            }
    }

    fun setType(type: AudioPlayerPulse.Type) {
         _type = type
        channel.trySend(Unit)
    }
}

@Composable
fun rememberAudioPlayerPulseState() =
    remember { AudioPlayerPulseState() }.also { LaunchedEffect(it) { it.observe() } }
