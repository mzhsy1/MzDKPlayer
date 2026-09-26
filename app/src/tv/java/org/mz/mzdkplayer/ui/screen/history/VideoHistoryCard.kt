package org.mz.mzdkplayer.ui.screen.history

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Border
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import org.mz.mzdkplayer.R
import org.mz.mzdkplayer.data.model.HistoryWithMetadata

@Composable
fun VideoHistoryCard(
    historyItem: HistoryWithMetadata,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val history = historyItem.history
    val metadata = historyItem.metadata

    // 优先使用元数据中的标题和图片，如果没有则使用文件名
    val title = metadata?.title ?: history.fileName
    val posterPath = metadata?.posterPath
    val year = metadata?.releaseDate?.take(4)?: "--"
    val seasonNumber = metadata?.seasonNumber ?: 0
    val episodeNumber = metadata?.episodeNumber ?: 0
// --- 关键修改点：确保微小进度至少显示 1% ---
    val rawPercentageInt = history.getPlaybackPercentage()
    // Req 2: 计算进度。只要有播放进度，percentage 就大于 0
    // 如果 getPlaybackPercentage() 返回 0，但实际播放位置大于 0，则强制显示 1%。
    // 这样 10 秒（0.1%）的播放量就会显示 1%。
    val percentageToDisplayInt = when {
        rawPercentageInt == 0 && history.playbackPosition > 0 -> 1
        else -> rawPercentageInt
    }

    val percentage = percentageToDisplayInt / 100f

    // 估算文字区域的精确高度 (4.dp top + title + 2.dp + year + 4.dp bottom) 约为 45.dp
    val textSectionHeightDp = 45.dp

    Card(
        onClick = onClick,
        modifier = modifier
            // Req 6: 外部 padding 设为 4.dp，使卡片总体变小
            .padding(4.dp)
            .aspectRatio(2f / 3f),
        shape = CardDefaults.shape(shape = RoundedCornerShape(12.dp)),
        colors = CardDefaults.colors(
            containerColor = Color.Transparent,
            focusedContainerColor = Color.White.copy(alpha = 0.1f)
        ),
        border = CardDefaults.border(
            focusedBorder = Border(
                border = androidx.compose.foundation.BorderStroke(3.dp, Color(0xFFFFC200)),
                shape = RoundedCornerShape(12.dp)
            )
        ),
        scale = CardDefaults.scale(
            focusedScale = 1.03f // 聚焦时轻微放大
        )
    ) {
        // 使用一个 Box 来包裹所有内容，以便进行叠加 (图片、文字、进度条、标签)
        Box(modifier = Modifier.fillMaxSize()) {

            Column(modifier = Modifier.fillMaxSize()) {

                // --- 1. 图片区域 ---
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f) // 图片占据剩余高度
                ) {
                    if (!posterPath.isNullOrEmpty()) {
                        AsyncImage(
                            model = org.mz.mzdkplayer.tool.Tools.formatImageUrl(posterPath, "w500"),
                            contentDescription = title,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        // 占位图
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No Image", color = Color.Gray)
                        }
                    }
                }

                // --- 2. 文字区域 ---
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        // 垂直 padding 从 8.dp 减少到 4.dp
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )


                    Text(
                        text = if (seasonNumber > 0 && episodeNumber > 0) {
                            // 自动匹配第1季、第2集、百分比
                            stringResource(
                                id = R.string.ui_label_playback_progress_episode,
                                seasonNumber,
                                episodeNumber,
                                (percentage * 100).toInt()
                            )
                        } else {
                            // 自动匹配年份、百分比
                            stringResource(
                                id = R.string.ui_label_playback_progress_year,
                                (percentage * 100).toInt()
                            )
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray,
                        modifier = Modifier.padding(top = 2.dp)
                    )

                }
            }

            // --- 3. 覆盖层元素：解决焦点消失问题 (Req 5) ---

            // 协议标签 (左上角) (Req 3, 4, 5)
            if (history.connectionName.isNotEmpty() || history.protocolName.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        // Req 4: 外部 padding 8.dp，使其与 Card 内部对齐
                        .padding(8.dp)
                        .align(Alignment.TopStart)
                        .background(
                            color = Color.Black.copy(alpha = 0.7f), // 确保对比度
                            shape = RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 6.dp, vertical = 3.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        val protocolText = history.protocolName
                        val connectionText = history.connectionName

                        if (connectionText.isNotEmpty()) {
                            Text(
                                text = connectionText,
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        if (protocolText.isNotEmpty()) {
                            if (connectionText.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(1.dp))
                            }
                            Text(
                                text = if (connectionText.isNotEmpty()) "($protocolText)" else protocolText,
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = if (connectionText.isNotEmpty()) 8.sp else 10.sp,
                                fontWeight = if (connectionText.isNotEmpty()) FontWeight.Normal else FontWeight.Bold,
                                color = Color.LightGray
                            )
                        }
                    }
                }
            }


            // 播放进度条 (图片底部) (Req 1, 2)
            if (percentage > 0f) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        // Req 1: 向上偏移 textSectionHeightDp，定位到图片和文字区域的精确交界处
                        .padding(bottom = textSectionHeightDp - 2.dp)
                        .padding(horizontal = 12.dp) // 与 Card 边框对齐
                ) {
                    // 进度百分比提示
//                    Text(
//                        text = "${(percentage * 100).toInt()}%",
//                        style = MaterialTheme.typography.labelSmall,
//                        fontSize = 10.sp,
//                        fontWeight = FontWeight.SemiBold,
//                        color = Color.White,
//                        modifier = Modifier
//                            .align(Alignment.End)
//                            .padding(bottom = 2.dp)
//                    )

                    // 进度条容器
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(2.dp))
                    ) {
                        // 实际进度
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(percentage)
                                .fillMaxHeight()
                                .background(
                                    brush = Brush.horizontalGradient(
                                        colors = listOf(Color(0xFFFFC200), Color(0xFFFF9800))
                                    ),
                                    shape = RoundedCornerShape(2.dp)
                                )
                        )
                    }
                }
            }
        }
    }
}