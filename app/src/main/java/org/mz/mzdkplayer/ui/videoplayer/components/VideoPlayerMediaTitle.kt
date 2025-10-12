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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import org.mz.mzdkplayer.R


enum class VideoPlayerMediaTitleType { AD, LIVE, DEFAULT }

@Composable
fun VideoPlayerMediaTitle(
    title: String,
    secondaryText: String,
    tertiaryText: String,
    modifier: Modifier = Modifier,
    type: VideoPlayerMediaTitleType = VideoPlayerMediaTitleType.DEFAULT
) {
    val subTitle = buildString {
        append(secondaryText)
        if (secondaryText.isNotEmpty() && tertiaryText.isNotEmpty()) append(" • ")
        append(tertiaryText)
    }
    Column(modifier.fillMaxWidth(0.95f)) {
        Text(
            title,
            style = MaterialTheme.typography.headlineSmall,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(4.dp))
        Row {
            // TODO: Replaced with Badge component once developed
            when (type) {
                VideoPlayerMediaTitleType.AD -> {
                    Text(
                        text = stringResource(R.string.ad),
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White,
                        modifier = Modifier
                            .background(Color(0xFFFBC02D), shape = RoundedCornerShape(12.dp))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                            .alignByBaseline()
                    )
                    Spacer(Modifier.width(8.dp))
                }

                VideoPlayerMediaTitleType.LIVE -> {
                    Text(
                        text = stringResource(R.string.live),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.inverseSurface,
                        modifier = Modifier
                            .background(Color(0xFFCC0000), shape = RoundedCornerShape(12.dp))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                            .alignByBaseline()
                    )

                    Spacer(Modifier.width(8.dp))
                }

                VideoPlayerMediaTitleType.DEFAULT -> {}
            }

            Text(
                text = subTitle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,// 超出部分显示省略号
                style = MaterialTheme.typography.bodyLarge,
                color = Color.Gray,
                modifier = Modifier.alignByBaseline()
            )
        }
    }
}



