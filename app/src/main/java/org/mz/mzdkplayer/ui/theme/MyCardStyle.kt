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
        containerColor = Color.Black,
        contentColor = Color.White,
        focusedContentColor = Color.Black,
        focusedContainerColor = Color.White
    )
}

@Composable
fun myCardBorderStyle(): CardBorder {
    return CardDefaults.border(
                Border(
                    border = BorderStroke(width = 2.dp, color = Color.Gray),
                    shape = RoundedCornerShape(5),
                ),
        focusedBorder = Border(
            border = BorderStroke(width = 0.dp, color = Color(0, 0, 0, 0)),
            shape = RoundedCornerShape(5),
        ),
//                pressedBorder = Border(
//                    border = BorderStroke(width = 2.dp, color = Color.White),
//                    shape = RoundedCornerShape(5),
//                )
    )
}@Composable
fun myCardScaleStyle(): CardScale {
    return CardDefaults.scale(
        focusedScale = 1.03f,
    )
}
