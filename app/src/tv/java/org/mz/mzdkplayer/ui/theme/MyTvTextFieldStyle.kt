package org.mz.mzdkplayer.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceBorder
import androidx.tv.material3.ClickableSurfaceColors
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.MaterialTheme

@Composable
fun myTTFColor(): ClickableSurfaceColors {
    return      ClickableSurfaceDefaults.colors(
        contentColor = Color.DarkGray,
        containerColor = Color.DarkGray,
        focusedContentColor = Color.Gray,
        focusedContainerColor = Color.Gray,

    )
}
@Composable
fun myTTFBorder(isTfFocused: Boolean): ClickableSurfaceBorder {
    return      ClickableSurfaceDefaults.border(
        focusedBorder = Border(
            border = BorderStroke(
                width = if (isTfFocused) 2.dp else 1.dp,
                color = animateColorAsState(
                    targetValue = if (isTfFocused) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.border,
                    label = ""
                ).value
            ),
        )
    )
}