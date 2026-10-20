package org.mz.mzdkplayer.ui.screen.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import org.mz.mzdkplayer.R

/**
 * 搜索无结果 / 过滤无匹配。
 *
 * 原来只有一枚 48dp 纯白 tint 图标加一行白字，和新的空态组件不是一套；
 * 现在统一走 [EmptyStateView] 的紧凑版，文件列表里塞进 LazyColumn 时也不会把行高撑爆。
 */
@Composable
fun NoSearchResult(
    text: String,
    modifier: Modifier = Modifier
) {
    EmptyStateView(
        iconRes = R.drawable.searchoff,
        title = text,
        accent = Color(0xFF90A4AE),
        compact = true,
        modifier = modifier
    )
}
