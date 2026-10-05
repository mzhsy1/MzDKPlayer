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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.mz.mzdkplayer.R
import org.mz.mzdkplayer.data.local.MediaCacheEntity
import java.util.Locale

/** 简介折叠到几行 */
private const val INFO_OVERVIEW_COLLAPSED_LINES = 4

/** 海报：竖屏信息区里比标题略高一点，圆角取 Expressive 的 `medium` 一档 */
private val PosterWidth = 92.dp
private val PosterHeight = 138.dp

/**
 * 竖屏（未全屏）时播放页**下半屏的信息区**。
 *
 * **配色跟随 App 主题**（`MaterialTheme.colorScheme`）：信息区在画面下方、属于页面内容，
 * 跟首页 / 详情页应当是同一套观感（与 `PhonePlayerVideoInfo` 早先写死深色的做法相反）。
 * 压在画面上的那层控制（`PhonePlayerControls`）才是另一套独立色板 ——
 * 那层必须自己保证白字在黑画面上可读，这一层则应当跟着主题走。
 *
 * 内容全部取自已有的刮削缓存 [MediaCacheEntity]：海报、年份、评分、类型、简介。
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
            .background(MaterialTheme.colorScheme.surface)
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(horizontal = 16.dp),
    ) {
        // 画面是黑的、信息区是主题底色，两者糊在一起没有边界，压一条分隔线分开
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Spacer(Modifier.height(14.dp))

        Row(verticalAlignment = Alignment.Top) {
            // 没刮到海报时整块不占位（PhonePosterImage 返回 false 表示它什么都没画）
            val posterPath = meta?.posterPath?.takeIf { it.isNotBlank() }
            if (posterPath != null) {
                val drawn = PhonePosterImage(
                    posterPath = posterPath,
                    size = "w200",
                    modifier = Modifier
                        .width(PosterWidth)
                        .height(PosterHeight)
                        .clip(MaterialTheme.shapes.medium),
                )
                if (drawn) Spacer(Modifier.width(14.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )

                if (meta != null) {
                    Spacer(Modifier.height(10.dp))
                    MetaPillRow(meta)
                }

                Spacer(Modifier.height(10.dp))
                Text(
                    text = "${stringResource(R.string.phone_detail_connection)}：$connectionName · $dataSourceType",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (dateText.isNotBlank()) {
                    Text(
                        text = dateText,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }
        }

        meta?.overview?.takeIf { it.isNotBlank() }?.let { overview ->
            Spacer(Modifier.height(18.dp))
            OverviewSection(overview)
        }

        Spacer(Modifier.height(16.dp))
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

/**
 * 信息胶囊：普通项用中性的 `surfaceContainerHighest`，评分那一颗换 tertiary 容器，
 * 一排胶囊里唯一需要被先看到的信息就是分数。
 */
@Composable
private fun InfoPill(text: String, withStar: Boolean = false) {
    Surface(
        shape = CircleShape,
        color = if (withStar) {
            MaterialTheme.colorScheme.tertiaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerHighest
        },
        contentColor = if (withStar) {
            MaterialTheme.colorScheme.onTertiaryContainer
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
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
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = overview,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}
