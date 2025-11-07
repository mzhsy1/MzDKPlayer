package org.mz.mzdkplayer.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.CardBorder
import androidx.tv.material3.CardColors
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.CardScale
import androidx.tv.material3.ListItemColors

@Composable
fun myCardColor(): CardColors {
    return CardDefaults.colors(
        containerColor = Color(32, 32, 32), // 更深的灰色
        contentColor = Color.White,
        focusedContentColor = Color(32, 32, 32),
        focusedContainerColor = Color.White
    )
}

@Composable
fun myCardBorderStyle(): CardBorder {
    return CardDefaults.border(
        border=Border(
                    border = BorderStroke(width = 0.dp, color = Color.Black),
                    shape = RoundedCornerShape(10),
                ),
        focusedBorder = Border(
            border = BorderStroke(width = 0.dp, color = Color(0, 0, 0, 0)),
            shape = RoundedCornerShape(0),
        ),
    )
}
@Composable
fun myCardScaleStyle(): CardScale {
    return CardDefaults.scale(
        focusedScale = 1.03f,
    )
}
@Composable
fun myFileTypeCardScaleStyle(): CardScale {
    return CardDefaults.scale(
        focusedScale = 1.02f,
    )
}

