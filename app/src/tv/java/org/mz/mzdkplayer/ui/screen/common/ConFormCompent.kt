package org.mz.mzdkplayer.ui.screen.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.ShapeDefaults
import androidx.tv.material3.Surface
import androidx.tv.material3.SurfaceDefaults
import androidx.tv.material3.Text
import org.mz.mzdkplayer.data.model.FileConnectionStatus

/**
 * 连接状态 → 指示色。五个协议表单页原来各抄了一份同样的 when，改口径时得改五处。
 */
fun connectionStatusColor(status: FileConnectionStatus): Color = when (status) {
    is FileConnectionStatus.Connected -> Color(0xFF4CAF50)
    is FileConnectionStatus.Connecting -> Color(0xFFFFC107)
    is FileConnectionStatus.Error -> Color(0xFFF44336)
    is FileConnectionStatus.LoadingFile -> Color(0xFFFFC107)
    is FileConnectionStatus.FilesLoaded -> Color(0xFF00BCD4)
    else -> Color(0xFF9E9E9E)
}

/**
 * 连接状态胶囊：一颗色点 + 状态文字，底色取状态色的低透明度。
 *
 * 五个协议表单页原本都是「一行粗体状态文本 + 一颗彩色圆点」，色点悬浮在文字旁边，
 * 状态到底是「已连接」还是「连接中」全靠读文字；胶囊把状态色铺到背景上，一眼能看出。
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun ConnectionStatusPill(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.widthIn(max = 460.dp),
        shape = ShapeDefaults.ExtraLarge,
        colors = SurfaceDefaults.colors(
            containerColor = color.copy(alpha = 0.16f),
            contentColor = color,
        ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(color = color, shape = CircleShape)
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/**
 * 表单卡片：把一组输入框收进一张卡片，标题贴在卡片内顶部。
 *
 * 各协议表单字段数量在 3~6 个之间，原来是一列裸输入框直接铺在深色背景上，
 * 字段与按钮没有分组；卡片负责给出「这一块是连接信息」的边界。
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun ConnectionFormCard(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = ShapeDefaults.Medium,
        colors = SurfaceDefaults.colors(
            containerColor = Color(0xFF1E1E1E),
            contentColor = Color.White,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFFB0B0B0),
            )
            content()
        }
    }
}
