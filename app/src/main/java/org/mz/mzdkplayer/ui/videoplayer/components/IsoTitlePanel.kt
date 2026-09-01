package org.mz.mzdkplayer.ui.videoplayer.components


import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Icon
import androidx.tv.material3.ListItem
import androidx.tv.material3.ListItemDefaults
import androidx.tv.material3.Text
import kotlinx.coroutines.launch
import androidx.compose.ui.res.stringResource
import org.mz.mzdkplayer.R
import org.mz.mzdkplayer.player.core.MzIsoTitle
import org.mz.mzdkplayer.tool.focusOnInitialVisibility

@Composable
fun IsoTitlePanel(
    lists: List<MzIsoTitle>,
    onTitleSelected: (MzIsoTitle) -> Unit
) {
    val isVis = remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val selectedIndex = lists.indexOfFirst { it.isSelected }.takeIf { it >= 0 } ?: 0

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "ISO " + stringResource(R.string.ui_label_video_track),
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
            color = Color.White,
            fontSize = 24.sp,
            modifier = Modifier.padding(start = 20.dp, top = 20.dp, bottom = 10.dp)
        )
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = listState
        ) {
        coroutineScope.launch {
            listState.animateScrollToItem(index = selectedIndex)
        }

        items(lists.size) { index ->
            val title = lists[index]

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
                    Text(text = title.name)
                },
                // 🌟 在标题下方显示时长
                supportingContent = {
                    Text(
                        text = title.durationText,
                        color = Color.Gray, // 调成灰色显得不那么抢眼
                        fontSize = 12.sp
                    )
                },
                leadingContent = if (selectedIndex == index) {
                    { Icon(Icons.Filled.Check, contentDescription = stringResource(R.string.ui_label_selected)) }
                } else null,
                onClick = {
                    onTitleSelected(title)
                }
            )
        }
    }
}
}