package org.mz.mzdkplayer.ui.style

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.ListItemBorder
import androidx.tv.material3.ListItemColors
import androidx.tv.material3.ListItemDefaults

@Composable
fun myListItemColor(): ListItemColors {
    return     ListItemDefaults.colors(
        containerColor = Color(0, 0, 0),
        contentColor = Color(255, 255, 255),
        selectedContainerColor = Color(255, 255, 255),
        selectedContentColor = Color(0, 0, 0),
        focusedSelectedContentColor = Color(0, 0, 0),
        focusedSelectedContainerColor = Color(255, 255, 255),
        focusedContainerColor = Color(255, 255, 255),
        focusedContentColor = Color(0, 0, 0)
    )
}
@Composable
fun myListItemCoverColor(): ListItemColors {
    return ListItemDefaults.colors(
        containerColor = Color(32, 32, 32), // 更深的灰色
        contentColor = Color.White,
        selectedContainerColor = Color(32, 32, 32), // 更深的灰色
        selectedContentColor = Color.White,
        focusedSelectedContentColor = Color.White,
        focusedSelectedContainerColor = Color(32, 32, 32), // 更深的灰色
        focusedContainerColor = Color(255, 255, 255),
        focusedContentColor = Color(0, 0, 0)
    )
}
@Composable
fun myListItemBorder(): ListItemBorder {
    return     ListItemDefaults.border(
        border = Border(border = BorderStroke(width = 2.dp, color = Color.White),
            inset = 0.dp,
            shape = RoundedCornerShape(8.dp))
    )
}