package org.mz.mzdkplayer.ui.phone.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.mz.mzdkplayer.R
import org.mz.mzdkplayer.data.local.MediaCacheEntity
import java.util.Locale

/** 简介折叠到几行 */
private const val INFO_OVERVIEW_COLLAPSED_LINES = 4

/**
 * 信息区的配色**写死不跟随主题**：播放页是沉浸式黑底，浅色主题下若用
 * `MaterialTheme.colorScheme` 会变成浅底深字，跟上面的画面直接撕裂。
 * 这几条只在信息区内部用，不外泄给别的组件。
 */
private val InfoBackground = Color(0xFF121214)
private val InfoPrimary = Color.White
private val InfoSecondary = Color.White.copy(alpha = 0.62f)
private val InfoPillBackground = Color.White.copy(alpha = 0.12f)
private val InfoAccent = Color(0xFF9ECBFF)

/**
 * 竖屏（未全屏）时播放页**下半屏的信息区** —— 布局对齐 B 站竖屏播放页：视频在上、信息在下。
 *
 * 内容全部取自已有的刮削缓存 [MediaCacheEntity]：海报、评分、类型、简介。
 * 没刮削过时只留标题与来源 —— 播放页这里没有刮削入口，摆一句「尚未刮削」也操作不了，
 * 真要继续刮削应该回目录页（那里有整目录刮削按钮）。
 */
@Composable
internal fun PhonePlayerVideoInfo(
    meta: MediaCacheEntity?,
    title: String,
    connectionName: String,
    dataSourceType: String,
    dateText: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .background(InfoBackground)
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            // 没刮到海报时整块不占位（PhonePosterImage 返回 false 表示它什么都没画）
            val posterPath = meta?.posterPath?.takeIf { it.isNotBlank() }
            if (posterPath != null) {
                val drawn = PhonePosterImage(
                    posterPath = posterPath,
                    size = "w200",
                    modifier = Modifier
                        .width(96.dp)
                        .height(144.dp)
                        .clip(RoundedCornerShape(10.dp)),
                )
                if (drawn) Spacer(Modifier.width(12.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = InfoPrimary,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )

                if (meta != null) {
                    Spacer(Modifier.height(8.dp))
                    MetaPillRow(meta)
                }

                Spacer(Modifier.height(10.dp))
                Text(
                    text = "${stringResource(R.string.phone_detail_connection)}：$connectionName · $dataSourceType",
                    color = InfoSecondary,
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (dateText.isNotBlank()) {
                    Text(
                        text = dateText,
                        color = InfoSecondary,
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }
        }

        meta?.overview?.takeIf { it.isNotBlank() }?.let { overview ->
            Spacer(Modifier.height(16.dp))
            OverviewSection(overview)
        }
    }
}

/** 年份 / 评分 / 类型，自动换行（与刮削详情页的 `MetaPills` 同一套口径） */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MetaPillRow(meta: MediaCacheEntity) {
    // `releaseDate` 里的年份只有 4 位数字才算数，否则会拿 "2026-05-04" 整串去做胶囊
    val year = meta.releaseDate?.take(4)?.takeIf { it.length == 4 }

    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        year?.let { InfoPill(text = it) }
        if (meta.voteAverage > 0.0) {
            InfoPill(text = String.format(Locale.US, "%.1f", meta.voteAverage), withStar = true)
        }
        meta.genres.forEach { InfoPill(text = it.name) }
    }
}

@Composable
private fun InfoPill(text: String, withStar: Boolean = false) {
    Surface(
        shape = RoundedCornerShape(percent = 50),
        color = InfoPillBackground,
        contentColor = InfoPrimary,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            if (withStar) {
                Icon(
                    imageVector = Icons.Filled.Star,
                    contentDescription = null,
                    modifier = Modifier.size(12.dp),
                )
            }
            Text(text = text, style = MaterialTheme.typography.labelMedium)
        }
    }
}

/** 简介：被截断了才给「展开 / 收起」，与刮削详情页同一个判断方式 */
@Composable
private fun OverviewSection(overview: String) {
    var expanded by remember(overview) { mutableStateOf(false) }
    var truncated by remember(overview) { mutableStateOf(false) }

    Column {
        Text(
            text = stringResource(R.string.phone_detail_overview),
            color = InfoPrimary,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = overview,
            color = InfoSecondary,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = if (expanded) Int.MAX_VALUE else INFO_OVERVIEW_COLLAPSED_LINES,
            overflow = TextOverflow.Ellipsis,
            onTextLayout = { result -> if (!expanded) truncated = result.hasVisualOverflow },
        )
        if (expanded || truncated) {
            TextButton(
                onClick = { expanded = !expanded },
                contentPadding = PaddingValues(horizontal = 0.dp, vertical = 4.dp),
            ) {
                Text(
                    text = stringResource(
                        if (expanded) R.string.phone_detail_collapse else R.string.phone_detail_expand
                    ),
                    color = InfoAccent,
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}
