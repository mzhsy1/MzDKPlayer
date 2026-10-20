package org.mz.mzdkplayer.ui.screen.common

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Icon
import androidx.tv.material3.Text

/** 空态 / 错误 / 无结果页共用的文字色，和主页、设置页的次级文字保持一致 */
private val PlaceholderTitle = Color(0xFFEDEAF2)
private val PlaceholderDescription = Color(0xFF8C8A96)

/**
 * 电视端统一的状态占位：强调色圆角图标底 + 标题 + 说明 + 可选操作。
 *
 * 之前空库、加载失败、搜索无结果、空目录各写一套，图标大小、文字颜色和间距都不一致：
 * 有的用 100dp 原色 PNG，有的用灰色 tint 图标，有的干脆只有一行灰字。这里收成一个组件，
 * 调用方只需要给图标和文案，视觉细节由组件统一。
 *
 * @param compact 嵌在列表 / 窄区域里时用紧凑版（图标底与字号都小一档）
 * @param action 可选的操作区，例如「去文件浏览添加」
 */
@Composable
fun EmptyStateView(
    @DrawableRes iconRes: Int,
    title: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    accent: Color = Color(0xFF90A4AE),
    compact: Boolean = false,
    action: (@Composable ColumnScope.() -> Unit)? = null
) {
    val tileSize = if (compact) 80.dp else 108.dp
    val tileCorner = if (compact) 22.dp else 30.dp
    val iconSize = if (compact) 34.dp else 46.dp

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = if (compact) 26.dp else 36.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(tileSize)
                .clip(RoundedCornerShape(tileCorner))
                .background(accent.copy(alpha = 0.10f))
                .border(
                    width = 1.dp,
                    color = accent.copy(alpha = 0.28f),
                    shape = RoundedCornerShape(tileCorner)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = null,
                tint = accent.copy(alpha = 0.95f),
                modifier = Modifier.size(iconSize)
            )
        }

        Spacer(modifier = Modifier.height(if (compact) 14.dp else 20.dp))

        Text(
            text = title,
            fontSize = if (compact) 17.sp else 22.sp,
            fontWeight = FontWeight.Bold,
            color = PlaceholderTitle,
            textAlign = TextAlign.Center,
            maxLines = 2
        )

        if (!description.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = description,
                fontSize = if (compact) 12.sp else 14.sp,
                color = PlaceholderDescription,
                textAlign = TextAlign.Center,
                maxLines = 3,
                modifier = Modifier.widthIn(max = 560.dp)
            )
        }

        if (action != null) {
            Spacer(modifier = Modifier.height(24.dp))
            action()
        }
    }
}
