package org.mz.mzdkplayer.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.ListItemBorder
import androidx.tv.material3.ListItemDefaults



@Composable
fun myListItemBorder(): ListItemBorder {
    return     ListItemDefaults.border(
        border = Border(border = BorderStroke(width = 2.dp, color = Color.White),
            inset = 0.dp,
            shape = RoundedCornerShape(8.dp))
    )
}