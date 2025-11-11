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


@Composable
fun myCardBorderStyle(): CardBorder {
    return CardDefaults.border(
        border=Border(
                    border = BorderStroke(width = 0.dp, color = Color.Transparent),
                    shape = RoundedCornerShape(0),
                ),
        focusedBorder = Border(
            border = BorderStroke(width = 0.dp, color = Color(0, 0, 0, 0)),
            shape = RoundedCornerShape(0),
        ),
    )
}
@Composable
fun myCardColor(): CardColors {
    return CardDefaults.colors(
        containerColor = Color(0xFF2D2D2D), // 保持原有的深色背景
        contentColor = Color(255, 248, 240), // 暖白色替代纯白
        focusedContainerColor = Color(255, 250, 245), // 米白色替代纯白
        focusedContentColor = Color(0, 0, 0), // 纯黑
        pressedContainerColor = Color(0xFF4A4540), // 调整为暖色调的深灰
        pressedContentColor = Color(255, 248, 240) // 暖白色
    )

}

@Composable
fun myFileTypeCardScaleStyle(): CardScale {
    return CardDefaults.scale(
        focusedScale = 1.02f,
    )
}

