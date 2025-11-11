package org.mz.mzdkplayer.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.tv.material3.ButtonColors
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.ListItemColors
import androidx.tv.material3.ListItemDefaults
import androidx.tv.material3.MaterialTheme

val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)

@Composable
fun MyIconButtonColor(): ButtonColors{
    return ButtonDefaults.colors(
        containerColor = Color(0xFF2D2D2D), // 保持默认或根据需要调整
        contentColor = Color(255, 248, 240), // 暖白色替代默认
        focusedContainerColor = Color(255, 250, 245), // 米白色替代纯白
        focusedContentColor = Color(80, 70, 60), // 暖深灰替代纯黑
        pressedContainerColor = Color(255, 250, 245), // 与聚焦状态一致
        pressedContentColor = Color(80, 70, 60), // 与聚焦状态一致
        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), // 保持默认
        disabledContentColor = Color(150, 140, 130) // 暖中灰替代默认
    )
}
/**
 * Home等ListItem颜色 OperationListItem
 */
@Composable
fun MyListItemCoverColor(): ListItemColors {
    return ListItemDefaults.colors(
        containerColor = Color(32, 32, 32), // 保持深色背景
        contentColor = Color(255, 248, 240), // 暖白色替代纯白
        selectedContainerColor = Color(32, 32, 32), // 保持深色背景
        selectedContentColor = Color(255, 248, 240), // 暖白色
        focusedSelectedContentColor = Color(255, 248, 240), // 暖白色
        focusedSelectedContainerColor = Color(32, 32, 32), // 保持深色背景
        focusedContainerColor = Color(255, 250, 245), // 米白色替代纯白
        focusedContentColor = Color(80, 70, 60) // 暖深灰替代纯黑
    )
}
/**
 * FileListScreen ListItem 颜色
 */
@Composable
fun MyFileListItemColor(): ListItemColors {
    return ListItemDefaults.colors(
        containerColor = Color(0, 0, 0), // 保持深色背景
        contentColor = Color(255, 248, 240), // 暖白色替代纯白
        selectedContainerColor = Color(32, 32, 32), // 保持深色背景
        selectedContentColor = Color(255, 248, 240), // 暖白色
        focusedSelectedContentColor = Color(255, 248, 240), // 暖白色
        focusedSelectedContainerColor = Color(32, 32, 32), // 保持深色背景
        focusedContainerColor = Color(255, 250, 245), // 米白色替代纯白
        focusedContentColor = Color(80, 70, 60) // 暖深灰替代纯黑
    )
}

/**
 * 主页侧边栏 ListItem 颜色
 */
@Composable
fun MySideListItemColor(): ListItemColors {
    return ListItemDefaults.colors(
        containerColor = Color(38, 38, 42, 255),
        contentColor = Color(255, 248, 240), // 暖白色
        selectedContainerColor = Color(255, 250, 245), // 米白色
        selectedContentColor = Color(80, 70, 60), // 暖深灰
        focusedSelectedContentColor = Color(80, 70, 60),
        focusedSelectedContainerColor = Color(255, 250, 245),
        focusedContainerColor = Color(255, 250, 245),
        focusedContentColor = Color(80, 70, 60)
    )
}