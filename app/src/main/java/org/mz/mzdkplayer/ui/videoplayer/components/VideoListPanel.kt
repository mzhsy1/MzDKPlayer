package org.mz.mzdkplayer.ui.videoplayer.components

import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.util.UnstableApi
import androidx.tv.material3.Icon
import androidx.tv.material3.ListItem
import androidx.tv.material3.ListItemDefaults
import androidx.tv.material3.Text
import org.mz.mzdkplayer.data.model.VideoItem
import org.mz.mzdkplayer.tool.focusOnInitialVisibility

@OptIn(UnstableApi::class)
@Composable
fun VideoListPanel(
    selectedIndex: Int,
    onVideoSelected: (VideoItem, Int) -> Unit,
    lists: List<VideoItem>
) {
    val focusRequester = remember { FocusRequester() }
    val isVis = remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    LaunchedEffect(selectedIndex) {
        if (lists.isNotEmpty() && selectedIndex in lists.indices) {
            listState.animateScrollToItem(selectedIndex)
        }
    }

    val density = LocalDensity.current
    val blurHeight = 25.dp
    val topGradient = Brush.verticalGradient(
        colors = listOf(Color.Black.copy(alpha = 0.8f), Color.Black.copy(alpha = 0.4f), Color.Transparent),
        startY = 0f,
        endY = with(density) { blurHeight.toPx() }
    )
    val bottomGradient = Brush.verticalGradient(
        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.4f), Color.Black.copy(alpha = 0.8f)),
        startY = 0f,
        endY = with(density) { blurHeight.toPx() }
    )

    Box(Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Text(
                text = "播放列表",
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontSize = 24.sp,
                modifier = Modifier.padding(start = 20.dp, top = 20.dp, bottom = 10.dp)
            )
            LazyColumn(
                modifier = Modifier
                    .width(360.dp)
                    .focusRequester(focusRequester),
                state = listState
            ) {
                if (lists.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .width(360.dp)
                                .height(300.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "无内容", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 20.sp)
                        }
                    }
                } else {
                    items(lists.size) { index ->
                        ListItem(
                            modifier = Modifier
                                .padding(horizontal = 15.dp)
                                .height(40.dp)
                                .then(if (index == selectedIndex) Modifier.focusOnInitialVisibility(isVis) else Modifier),
                            selected = false,
                            colors = ListItemDefaults.colors(
                                containerColor = Color.Black,
                                contentColor = Color.White,
                                focusedContainerColor = Color.White,
                                focusedContentColor = Color.Black
                            ),
                            headlineContent = {
                                Text(lists[index].fileName, maxLines = 1, fontSize = 12.sp)
                            },
                            leadingContent = if (selectedIndex == index) {
                                { Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(12.dp)) }
                            } else null,
                            onClick = { onVideoSelected(lists[index], index) }
                        )
                    }
                }
            }
        }
        Box(modifier = Modifier.fillMaxWidth().height(blurHeight).background(topGradient))
        Box(modifier = Modifier.fillMaxWidth().height(blurHeight).align(Alignment.BottomCenter).background(bottomGradient))
    }
}
