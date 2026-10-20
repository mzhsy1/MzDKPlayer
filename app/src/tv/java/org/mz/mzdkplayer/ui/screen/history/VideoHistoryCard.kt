package org.mz.mzdkplayer.ui.screen.history

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Border
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import org.mz.mzdkplayer.R
import org.mz.mzdkplayer.data.model.HistoryWithMetadata

private val CardBackground = Color(0xFF1A1A21)
private val CardFocusedBackground = Color(0xFF24242C)
private val PosterBackground = Color(0xFF141419)
private val TitleColor = Color(0xFFF4F1EA)
private val MetaColor = Color(0xFF9A96A6)
private val ProgressStart = Color(0xFFE9C46A)
private val ProgressEnd = Color(0xFFD9A441)

/**
 * 播放历史卡片。
 *
 * 原来的版式是「协议标签在卡片最上方 → 图片 → 标题 + 进度」，协议标签把海报挤下去一块，
 * 没有海报时又是一整片黑底 + 写死的英文 "No Image"；进度条还得靠 `textSectionHeightDp`
 * 估算位置往上顶。这里改成标准海报卡：
 * - 图片区占满卡片上半部分，协议/连接名做成海报左上角的小胶囊
 * - 无海报时显示图标 + 本地化的「无海报」，不再是英文占位
 * - 进度条贴在海报底边，进度色与设置页开关统一为暖金
 */
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
    val year = metadata?.releaseDate?.take(4) ?: "--"
    val seasonNumber = metadata?.seasonNumber ?: 0
    val episodeNumber = metadata?.episodeNumber ?: 0

    // 只要有播放进度就至少显示 1%，避免 10 秒（0.1%）看起来像没看过
    val rawPercentageInt = history.getPlaybackPercentage()
    val percentageToDisplayInt = when {
        rawPercentageInt == 0 && history.playbackPosition > 0 -> 1
        else -> rawPercentageInt
    }
    val percentage = (percentageToDisplayInt / 100f).coerceIn(0f, 1f)
    val percentageText = (percentage * 100).toInt()

    val progressText = if (seasonNumber > 0 && episodeNumber > 0) {
        stringResource(
            R.string.ui_label_playback_progress_episode,
            seasonNumber,
            episodeNumber,
            percentageText
        )
    } else {
        stringResource(R.string.ui_label_playback_progress_year, percentageText)
    }
    // 电影补一个年份，剧集已经有 S/E 信息就不重复
    val metaText = if (seasonNumber == 0 && year.isNotBlank() && year != "--") {
        "$year · $progressText"
    } else {
        progressText
    }

    val sourceLabel = listOf(history.connectionName, history.protocolName)
        .filter { it.isNotBlank() }
        .joinToString(" · ")

    val shape = RoundedCornerShape(14.dp)

    Card(
        onClick = onClick,
        modifier = modifier
            .padding(4.dp)
            .aspectRatio(2f / 3f),
        shape = CardDefaults.shape(shape = shape),
        colors = CardDefaults.colors(
            containerColor = CardBackground,
            contentColor = TitleColor,
            focusedContainerColor = CardFocusedBackground,
            focusedContentColor = TitleColor
        ),
        border = CardDefaults.border(
            border = Border(
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
                shape = shape
            ),
            focusedBorder = Border(
                border = BorderStroke(2.dp, Color(0xFFFFF6E5)),
                shape = shape
            )
        ),
        scale = CardDefaults.scale(focusedScale = 1.03f, pressedScale = 0.99f)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // --- 海报区 ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(PosterBackground)
            ) {
                if (!posterPath.isNullOrEmpty()) {
                    AsyncImage(
                        model = org.mz.mzdkplayer.tool.Tools.formatImageUrl(posterPath, "w500"),
                        contentDescription = title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.image24dp),
                            contentDescription = null,
                            tint = Color(0xFF4A4752),
                            modifier = Modifier.size(38.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.ui_label_no_poster),
                            fontSize = 11.sp,
                            color = Color(0xFF6E6A78)
                        )
                    }
                }

                // 来源胶囊：连接名 + 协议，贴在海报左上角
                if (sourceLabel.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color.Black.copy(alpha = 0.62f))
                            .padding(horizontal = 7.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = sourceLabel,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFFE4DFD8),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // 进度条：贴在海报底边，和封面同宽
                if (percentage > 0f) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .height(4.dp)
                            .background(Color.Black.copy(alpha = 0.55f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(percentage)
                                .fillMaxHeight()
                                .background(
                                    brush = Brush.horizontalGradient(
                                        colors = listOf(ProgressStart, ProgressEnd)
                                    )
                                )
                        )
                    }
                }
            }

            // --- 文字区 ---
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            ) {
                Text(
                    text = title,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TitleColor
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = metaText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 11.sp,
                    color = MetaColor
                )
            }
        }
    }
}
