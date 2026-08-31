package org.mz.mzdkplayer.ui.videoplayer.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.tv.material3.Icon
import androidx.tv.material3.ListItem
import androidx.tv.material3.ListItemDefaults
import androidx.tv.material3.Text
import org.mz.mzdkplayer.R
import org.mz.mzdkplayer.tool.focusOnInitialVisibility

@Composable
fun PlaybackSpeedPanel(
    currentSpeed: Float,
    onSpeedSelected: (Float) -> Unit,
    isPassthroughEnabled: Boolean = false
) {
    val speeds = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
    val focusRequester = remember { FocusRequester() }
    val isVis = remember { mutableStateOf(false) }

    Column {
        Text(
            text = stringResource(R.string.ui_label_speed),
            fontWeight = FontWeight.Bold,
            color = Color.White,
            fontSize = 24.sp,
            modifier = Modifier.padding(15.dp, 10.dp)
        )
        LazyColumn(
            modifier = Modifier
                .width(360.dp)
                .focusRequester(focusRequester)
        ) {
        if (isPassthroughEnabled) {
            item {
                Box(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "音频直通模式下不可更改倍速",
                        color = Color.Yellow,
                        fontSize = 14.sp
                    )
                }
            }
        }
        items(speeds.size) { index ->
            val speed = speeds[index]
            val isSelected = speed == currentSpeed

            ListItem(
                modifier = Modifier
                    .padding(start = 15.dp, end = 2.dp, top = 10.dp, bottom = 10.dp)
                    .let {
                        if (isSelected) it.focusOnInitialVisibility(isVis) else it
                    },
                selected = false,
                colors = ListItemDefaults.colors(
                    containerColor = Color(0, 0, 0),
                    contentColor = Color(255, 255, 255),
                    focusedContainerColor = Color(255, 255, 255),
                    focusedContentColor = Color(0, 0, 0)
                ),
                headlineContent = {
                    Text("${speed}x")
                },
                leadingContent = if (isSelected) {
                    { Icon(Icons.Filled.Check, contentDescription = stringResource(R.string.ui_label_checked)) }
                } else null,
                onClick = {
                    onSpeedSelected(speed)
                }
            )
        }
    }
}
}
