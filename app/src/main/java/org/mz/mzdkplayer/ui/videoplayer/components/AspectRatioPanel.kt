package org.mz.mzdkplayer.ui.videoplayer.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Icon
import androidx.tv.material3.ListItem
import androidx.tv.material3.ListItemDefaults
import androidx.tv.material3.Switch
import androidx.tv.material3.Text
import kotlinx.coroutines.launch
import org.mz.mzdkplayer.R
import org.mz.mzdkplayer.player.core.MzAspectRatio
import org.mz.mzdkplayer.tool.focusOnInitialVisibility

@Composable
fun AspectRatioPanel(
    currentRatio: MzAspectRatio,
    isLocked: Boolean,
    onRatioSelected: (MzAspectRatio) -> Unit,
    onLockedChange: (Boolean) -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    val isVis = remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    val ratios = MzAspectRatio.entries
    val selectedIndex = ratios.indexOf(currentRatio).takeIf { it >= 0 } ?: 0

    LaunchedEffect(currentRatio) {
        listState.animateScrollToItem(index = selectedIndex + 1)
    }

    Column {
        Text(
            text = stringResource(R.string.ui_label_aspect_ratio),
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
            color = Color.White,
            fontSize = 24.sp,
            modifier = Modifier.padding(15.dp, 10.dp)
        )
        LazyColumn(
            modifier = Modifier
                .width(360.dp)
                .focusRequester(focusRequester),
            state = listState
        ) {
            item {
                ListItem(
                    modifier = Modifier
                        .padding(start = 15.dp, end = 2.dp, top = 10.dp, bottom = 10.dp),
                    selected = false,
                    colors = ListItemDefaults.colors(
                        containerColor = Color(0, 0, 0),
                        contentColor = Color(255, 255, 255),
                        focusedContainerColor = Color(255, 255, 255),
                        focusedContentColor = Color(0, 0, 0)
                    ),
                    headlineContent = {
                        Text(stringResource(R.string.setting_lock_video_ratio))
                    },
                    trailingContent = {
                        Switch(checked = isLocked, onCheckedChange = null)
                    },
                    onClick = {
                        onLockedChange(!isLocked)
                    }
                )
            }
            items(ratios.size) { index ->
                val ratio = ratios[index]
                val isSelected = ratio == currentRatio

                ListItem(
                    modifier = Modifier
                        .padding(start = 15.dp, end = 2.dp, top = 10.dp, bottom = 10.dp)
                        .let {
                            if (index == selectedIndex) it.focusOnInitialVisibility(isVis) else it
                        },
                    selected = false,
                    colors = ListItemDefaults.colors(
                        containerColor = Color(0, 0, 0),
                        contentColor = Color(255, 255, 255),
                        selectedContainerColor = Color(255, 255, 255),
                        selectedContentColor = Color(255, 255, 255),
                        focusedSelectedContentColor = Color(255, 255, 255),
                        focusedSelectedContainerColor = Color(255, 255, 255),
                        focusedContainerColor = Color(255, 255, 255),
                        focusedContentColor = Color(0, 0, 0)
                    ),
                    headlineContent = {
                        Text(ratio.description)
                    },
                    leadingContent = if (isSelected) {
                        { Icon(Icons.Filled.Check, contentDescription = "已选择") }
                    } else null,
                    onClick = {
                        onRatioSelected(ratio)
                    }
                )
            }
        }
    }
}