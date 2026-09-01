package org.mz.mzdkplayer.ui.videoplayer.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
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

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = stringResource(R.string.ui_label_aspect_ratio),
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
            color = Color.White,
            fontSize = 24.sp,
            modifier = Modifier.padding(start = 20.dp, top = 20.dp, bottom = 10.dp)
        )
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .focusRequester(focusRequester),
            state = listState
        ) {
            item {
                ListItem(
                    modifier = Modifier
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    selected = false,
                    shape = ListItemDefaults.shape(RoundedCornerShape(12.dp)),
                    colors = ListItemDefaults.colors(
                        containerColor = Color.Transparent,
                        contentColor = Color.White.copy(alpha = 0.8f),
                        focusedContainerColor = Color.White,
                        focusedContentColor = Color.Black
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
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                        .let {
                            if (index == selectedIndex) it.focusOnInitialVisibility(isVis) else it
                        },
                    selected = false,
                    shape = ListItemDefaults.shape(RoundedCornerShape(12.dp)),
                    colors = ListItemDefaults.colors(
                        containerColor = Color.Transparent,
                        contentColor = Color.White.copy(alpha = 0.8f),
                        focusedContainerColor = Color.White,
                        focusedContentColor = Color.Black
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