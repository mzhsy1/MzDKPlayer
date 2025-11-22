package org.mz.mzdkplayer.ui.screen.common

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil3.compose.AsyncImage

@Composable
fun MediaCard(
    title: String,
    posterPath: String?,
    year: String?,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    // 使用基础 Card，完全自定义内部布局，避免 StandardCard/ClassicCard 版本兼容问题
    Card(
        onClick = onClick,
        modifier = modifier
            .padding(8.dp)
            .aspectRatio(2f / 3f), // 电影海报标准比例
        shape = CardDefaults.shape(shape = RoundedCornerShape(12.dp)),
        colors = CardDefaults.colors(
            containerColor = Color.Transparent, // 背景透明
            contentColor = MaterialTheme.colorScheme.onSurface,
            focusedContainerColor = Color.White.copy(alpha = 0.1f) // 聚焦时的背景高亮
        ),
        border = CardDefaults.border(
            focusedBorder = Border(
                border = androidx.compose.foundation.BorderStroke(3.dp, Color(0xFFFFC200)),
                shape = RoundedCornerShape(12.dp)
            )
        ),
        scale = CardDefaults.scale(
            focusedScale = 1.05f // 聚焦时轻微放大
        )
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // 1. 图片区域 (占据大部分空间)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f) // 图片占据剩余高度
            ) {
                if (!posterPath.isNullOrEmpty()) {
                    AsyncImage(
                        model = "https://image.tmdb.org/t/p/w500$posterPath",
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

            // 2. 文字区域 (在图片下方)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp) // 文字区域的内边距
            ) {
                Text(
                    text = title,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White // 强制白色，防止聚焦时变色看不清
                )

                if (!year.isNullOrEmpty()) {
                    Text(
                        text = year,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }
    }
}