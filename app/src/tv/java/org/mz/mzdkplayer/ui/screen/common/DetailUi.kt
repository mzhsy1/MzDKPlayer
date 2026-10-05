package org.mz.mzdkplayer.ui.screen.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.palette.graphics.Palette
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import coil3.BitmapImage
import coil3.asDrawable
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.request.crossfade

/** 详情页面板的统一圆角 */
val DetailPanelShape = RoundedCornerShape(18.dp)

/** 详情页评分徽章的琥珀金（电影 / 剧集统一） */
val DetailAccentGold = Color(0xFFF5C518)

/**
 * 沉浸式背景：全屏剧照 + 从剧照中提取压暗的主题色 + 底部/侧边渐变遮罩。
 *
 * @param imageUrl 背景剧照地址，为空时使用纯黑底
 * @param themeColor 当前主题色，随 [onThemeColorExtracted] 回调更新
 */
@Composable
fun ImmersiveBackdrop(
    imageUrl: String?,
    themeColor: Color,
    onThemeColorExtracted: (Color) -> Unit
) {
    val context = LocalContext.current
    Box(modifier = Modifier.fillMaxSize()) {
        if (imageUrl != null) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(imageUrl)
                    .allowHardware(false)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                onSuccess = { result ->
                    val image = result.result.image
                    val bitmap = (image as? BitmapImage)?.bitmap
                        ?: image.asDrawable(context.resources).toBitmap()
                    Palette.from(bitmap).generate { palette ->
                        val rgb = palette?.darkVibrantSwatch?.rgb
                            ?: palette?.darkMutedSwatch?.rgb
                            ?: palette?.dominantSwatch?.rgb
                        if (rgb != null) {
                            val base = Color(rgb)
                            onThemeColorExtracted(
                                Color(
                                    red = base.red * 0.24f,
                                    green = base.green * 0.24f,
                                    blue = base.blue * 0.24f,
                                    alpha = 1f
                                )
                            )
                        }
                    }
                }
            )
        } else {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black))
        }

        // 主垂直渐变：上半屏保留剧照，下半屏过渡到主题色（按比例，与分辨率无关）
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.0f to Color.Transparent,
                        0.34f to Color.Transparent,
                        0.55f to themeColor.copy(alpha = 0.35f),
                        0.72f to themeColor.copy(alpha = 0.75f),
                        0.88f to themeColor.copy(alpha = 0.95f),
                        1.0f to themeColor
                    )
                )
        )

        // 左侧渐暗，提升左下角文字可读性，同时保留右侧剧照的通透感
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        0.0f to themeColor.copy(alpha = 0.55f),
                        0.18f to themeColor.copy(alpha = 0.3f),
                        0.55f to Color.Transparent
                    )
                )
        )
    }
}

/**
 * 详情页的玻璃质感面板。
 *
 * @param clickable 为 true 时保持可聚焦（用于电视端焦点滚动），聚焦后边框与底色提亮
 */
@Composable
fun DetailPanel(
    modifier: Modifier = Modifier,
    clickable: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(24.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    if (clickable) {
        Surface(
            onClick = { },
            shape = ClickableSurfaceDefaults.shape(shape = DetailPanelShape),
            colors = ClickableSurfaceDefaults.colors(
                containerColor = Color.Black.copy(alpha = 0.28f),
                focusedContainerColor = Color.Black.copy(alpha = 0.12f),
                pressedContainerColor = Color.Black.copy(alpha = 0.04f)
            ),
            border = ClickableSurfaceDefaults.border(
                border = Border(BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))),
                focusedBorder = Border(BorderStroke(1.dp, Color.White.copy(alpha = 0.45f)))
            ),
            scale = ClickableSurfaceDefaults.scale(focusedScale = 1.01f),
            modifier = modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(contentPadding)) { content() }
        }
    } else {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .clip(DetailPanelShape)
                .background(Color.Black.copy(alpha = 0.28f))
                .border(1.dp, Color.White.copy(alpha = 0.1f), DetailPanelShape)
                .padding(contentPadding)
        ) { content() }
    }
}

/** 面板小标题：左侧带一条强调色竖条 */
@Composable
fun DetailPanelTitle(
    text: String,
    accent: Color = Color.White.copy(alpha = 0.9f),
    style: TextStyle = MaterialTheme.typography.titleMedium,
    barHeight: Dp = 18.dp
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(width = 3.dp, height = barHeight)
                .clip(RoundedCornerShape(2.dp))
                .background(accent)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = text,
            style = style,
            fontWeight = FontWeight.Bold,
            color = Color.White.copy(alpha = 0.92f)
        )
    }
}

/** 信息行左侧的标签文字 */
@Composable
fun DetailInfoLabel(text: String, width: Dp = 96.dp) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = Color.White.copy(alpha = 0.45f),
        modifier = Modifier.width(width)
    )
}

/** 面板内的「标签 - 值」信息行 */
@Composable
fun DetailInfoRow(
    label: String,
    value: String,
    labelWidth: Dp = 96.dp
) {
    Row {
        DetailInfoLabel(text = label, width = labelWidth)
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.88f),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/** 圆角标签，用于类型等短文本 */
@Composable
fun DetailChip(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(Color.White.copy(alpha = 0.11f))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = Color.White.copy(alpha = 0.85f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/** 评分徽章 */
@Composable
fun DetailRatingBadge(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(DetailAccentGold)
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = Color.Black,
            fontWeight = FontWeight.Bold
        )
    }
}

/** 第一屏的元数据行（年份 / 类型等） */
@Composable
fun DetailMetaRow(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        content()
    }
}
