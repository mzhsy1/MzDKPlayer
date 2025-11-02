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

import org.mz.mzdkplayer.R
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.delay


import kotlin.time.Duration

@Composable
fun AudioPlayerSeeker(
    focusRequester: FocusRequester,
    state: AudioPlayerState,
    isPlaying: Boolean,
    onPlayPauseToggle: (Boolean) -> Unit,
    onSeek: (Float) -> Unit,
    contentProgress: Duration,
    contentDuration: Duration,
    exoPlayer: ExoPlayer
) {
    LaunchedEffect(Unit) {
        delay(100)
        focusRequester.requestFocus()
    }
    val contentProgressString =
        contentProgress.toComponents { h, m, s, _ ->
            if (h > 0) {
                "$h:${m.padStartWith0()}:${s.padStartWith0()}"
            } else {
                "${m.padStartWith0()}:${s.padStartWith0()}"
            }
        }
    val contentDurationString =
        contentDuration.toComponents { h, m, s, _ ->
            if (h > 0) {
                "$h:${m.padStartWith0()}:${s.padStartWith0()}"
            } else {
                "${m.padStartWith0()}:${s.padStartWith0()}"
            }
        }

    Row(
        verticalAlignment = Alignment.CenterVertically,

    ) {
        AudioPlayerControlsIcon(
            modifier = Modifier.padding(end = 8.dp) ,
            icon = painterResource(id = R.drawable.skipprevious),
            onClick = { exoPlayer.seekToPreviousMediaItem() },
            state = state,
            isPlaying = isPlaying,
        )
        AudioPlayerControlsIcon(
            modifier = Modifier.focusRequester(focusRequester).padding(horizontal = 8.dp) ,
            icon = if (!isPlaying) painterResource(id = R.drawable.baseline_play_arrow_24) else painterResource(
                id = R.drawable.baseline_pause_24
            ),
            iconSize = 50,
            onClick = { onPlayPauseToggle(!isPlaying) },
            state = state,
            isPlaying = isPlaying,
        )
        AudioPlayerControlsIcon(
            modifier = Modifier.padding(horizontal = 8.dp),
            icon = painterResource(id = R.drawable.skipnext),
            onClick = { exoPlayer.seekToNextMediaItem() },
            state = state,
            isPlaying = isPlaying,
        )

        AudioPlayerControllerText(text = contentProgressString)
        AudioPlayerControllerIndicator(
            progress = (contentProgress / contentDuration).toFloat(),
            onSeek = onSeek,
            state = state
        )
        AudioPlayerControllerText(text = contentDurationString)
    }
}

private fun Number.padStartWith0() = this.toString().padStart(2, '0')
