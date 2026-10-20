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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import org.mz.mzdkplayer.R
import org.mz.mzdkplayer.ui.common.PlayerStatusChip


enum class VideoPlayerMediaTitleType { AD, LIVE, DEFAULT }

@Composable
fun VideoPlayerMediaTitle(
    title: String,
    statusChip: PlayerStatusChip,
    tertiaryText: String,
    modifier: Modifier = Modifier,
    type: VideoPlayerMediaTitleType = VideoPlayerMediaTitleType.DEFAULT
) {
    Column(modifier.fillMaxWidth(0.95f)) {
        Text(
            title,
            style = MaterialTheme.typography.headlineSmall,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {

            when (type) {
                VideoPlayerMediaTitleType.AD -> {
                    Text(
                        text = stringResource(R.string.ad),
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White,
                        modifier = Modifier
                            .background(Color(0xFFFBC02D), shape = RoundedCornerShape(12.dp))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
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
                    )

                    Spacer(Modifier.width(8.dp))
                }

                VideoPlayerMediaTitleType.DEFAULT -> {}
            }

            // 播放状态做成胶囊：播放中 / 暂停中 / 缓冲中 / 播放结束 各一种颜色
            PlayerStatusBadge(statusChip)

            if (tertiaryText.isNotEmpty()) {
                Spacer(Modifier.width(10.dp))
                Text(
                    text = tertiaryText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color(0xFFB9B4C0)
                )
            }
        }
    }
}

/** 状态胶囊本体：圆角全弧 + 状态色底 + 同色文字 */
@Composable
private fun PlayerStatusBadge(chip: PlayerStatusChip) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(percent = 50))
            .background(chip.containerColor)
            .padding(horizontal = 10.dp, vertical = 3.dp)
    ) {
        Text(
            text = chip.text,
            style = MaterialTheme.typography.labelLarge,
            color = chip.contentColor,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
    }
}



