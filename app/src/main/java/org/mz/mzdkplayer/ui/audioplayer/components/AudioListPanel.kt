package org.mz.mzdkplayer.ui.audioplayer.components


import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
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
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.media3.exoplayer.ExoPlayer
import androidx.tv.material3.Icon
import androidx.tv.material3.ListItem
import androidx.tv.material3.ListItemDefaults
import androidx.tv.material3.Text
import org.mz.mzdkplayer.logic.model.AudioItem
import org.mz.mzdkplayer.tool.focusOnInitialVisibility
import org.mz.mzdkplayer.ui.screen.vm.AudioPlayerViewModel


@OptIn(UnstableApi::class)
@Composable
fun AudioListPanel(
    selectedIndex: Int,
    onSelectedIndexChange: (currentIndex: Int) -> Unit,
    lists: MutableList<AudioItem>,
    exoPlayer: ExoPlayer,
    audioPlayerViewModel: AudioPlayerViewModel
) {

    val focusRequester = remember { FocusRequester() }
    // 每次进入获取焦点
    val isVis = remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    LaunchedEffect(selectedIndex) {
        if (lists.isNotEmpty() && selectedIndex in lists.indices) {
            listState.animateScrollToItem(selectedIndex)
        }
    }
    val density = LocalDensity.current
    // 定义自然过渡效果的渐变色 - 适配黑色背景
    val blurHeight = 25.dp
    val topGradient = Brush.verticalGradient(
        colors = listOf(
            Color.Black.copy(alpha = 0.8f),           // 深色开始
            Color.Black.copy(alpha = 0.4f),           // 中间透明度
            Color.Transparent                         // 结束透明
        ),
        startY = 0f,
        endY = with(density) { blurHeight.toPx() }
    )
    val bottomGradient = Brush.verticalGradient(
        colors = listOf(
            Color.Transparent,                       // 开始透明
            Color.Black.copy(alpha = 0.4f),           // 中间透明度
            Color.Black.copy(alpha = 0.8f)            // 深色结束
        ),
        startY = 0f,
        endY = with(density) { blurHeight.toPx() }
    )
    Box() {
        //Log.d("sampleRate",lists[index].getTrackFormat(0).sampleRate.toString())
        LazyColumn(
            modifier = Modifier
                .width(360.dp)
                .focusRequester(focusRequester), state = listState
        ) {

            if (lists.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .width(360.dp)
                            .height(300.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "无内容",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 20.sp
                        )
                    }
                }
            } else {
                //LazyColumn滚到到当前选择位置

                items(lists.size) { index ->


                    ListItem(
                        modifier = if (index == selectedIndex /*选中的获取焦点*/) {
                            Modifier
                                .padding(
                                    start = 15.dp,
                                    end = 15.dp,
                                )
                                .height(40.dp)
                                .focusOnInitialVisibility(isVis)
                        } else Modifier
                            .padding(
                                start = 15.dp,
                                end = 15.dp,
                            )
                            .height(40.dp),
                        selected = index == selectedIndex,
                        colors = ListItemDefaults.colors(
                            containerColor = Color(0, 0, 0),
                            contentColor = Color(255, 255, 255),
                            selectedContainerColor = Color(255, 255, 255),
                            selectedContentColor = Color(0, 0, 0),
                            focusedSelectedContentColor = Color(255, 255, 255),
                            focusedSelectedContainerColor = Color(0, 0, 0),
                            focusedContainerColor = Color(255, 255, 255),
                            focusedContentColor = Color(0, 0, 0)


                        ),
                        headlineContent = {
                            Text(
                                lists[index].fileName,
                                maxLines = 1,
                                fontSize = 10.sp

                            )
                        },
                        overlineContent = {

                        },

                        leadingContent = if (selectedIndex == index) {
                            {
                                Icon(
                                    Icons.Filled.Check,
                                    contentDescription = "Localized description",
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        } else null,
                        trailingContent = {


                        },
                        onClick = {
                            onSelectedIndexChange(index)
                            exoPlayer.seekTo(index, 0)

                        }
                    )
                }

            }
        }
        // 顶部过渡效果 - 覆盖在内容上方，但不增加额外高度
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(blurHeight)
                .background(topGradient)
        )

        // 底部过渡效果 - 覆盖在内容下方，但不增加额外高度
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(blurHeight)
                .align(Alignment.BottomCenter)
                .background(bottomGradient)
        )
    }
}