package org.mz.mzdkplayer.ui.phone.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import org.mz.mzdkplayer.tool.Tools
import org.mz.mzdkplayer.ui.phone.PhoneIcons

/**
 * 海报 / 剧照的通用加载（原在 `screen/PhoneScrape.kt`）。
 *
 * 列表行、详情页、首页卡片都用同一套，差别只有尺寸与 tmdb 图片档位。
 */

/**
 * 加载一张 tmdb 海报 / 剧照。
 *
 * `posterPath` 为空（没刮到）时返回 false，由调用方回退到图标 —— 这样调用方
 * 不用在 Compose 里做 `if (url == null) Icon() else AsyncImage()` 的重复分支。
 */
@Composable
internal fun PhonePosterImage(
    posterPath: String?,
    modifier: Modifier = Modifier,
    size: String = "w200",
): Boolean {
    val url = Tools.formatImageUrl(posterPath, size) ?: return false
    AsyncImage(
        model = ImageRequest.Builder(LocalContext.current).data(url).crossfade(true).build(),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier,
    )
    return true
}

/** 列表行 / 详情页共用的海报缩略图，没有图就用占位底色 + 影片图标 */
@Composable
internal fun PosterThumb(
    posterPath: String?,
    width: Dp,
    height: Dp,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier = modifier
            .size(width = width, height = height)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
        contentAlignment = Alignment.Center,
    ) {
        if (!PhonePosterImage(posterPath = posterPath, modifier = Modifier.size(width, height))) {
            Icon(
                imageVector = PhoneIcons.Movie,
                contentDescription = null,
                modifier = Modifier.size(28.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
