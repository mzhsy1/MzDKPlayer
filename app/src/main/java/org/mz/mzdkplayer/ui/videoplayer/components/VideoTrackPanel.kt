package org.mz.mzdkplayer.ui.videoplayer.components

import android.util.Log
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.C
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.tv.material3.Icon
import androidx.tv.material3.ListItem
import androidx.tv.material3.ListItemDefaults
import androidx.tv.material3.Text
import kotlinx.coroutines.launch
import org.mz.mzdkplayer.R // 确保 R.drawable.hdr_1 和 R.drawable.dolby_vision_seeklogo 等存在
import org.mz.mzdkplayer.player.core.MzVideoTrack
import org.mz.mzdkplayer.tool.focusOnInitialVisibility
import java.util.Locale


@androidx.annotation.OptIn(UnstableApi::class)
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun VideoTrackPanel(
    lists: List<MzVideoTrack>, // 使用统一的模型
    onTrackSelected: (MzVideoTrack) -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    val isVis = remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val selectedIndex = lists.indexOfFirst { it.isSelected }.takeIf { it >= 0 } ?: 0
    
    Column {
        Text(
            text = stringResource(R.string.ui_label_video_track),
            fontWeight = FontWeight.Bold,
            color = Color.White,
            fontSize = 24.sp,
            modifier = Modifier.padding(15.dp, 10.dp)
        )
        LazyColumn(
            modifier = Modifier
                .widthIn(200.dp, 500.dp)
                .heightIn(200.dp, 500.dp),
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
                    Text(
                        text = stringResource(R.string.ui_label_no_video_tracks_in_file),
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 20.sp
                    )
                }
            }
        } else {
            // LazyColumn滚到到当前选择位置
            // 找到选中的 index 用于自动滚动和打勾

            coroutineScope.launch {
                listState.animateScrollToItem(index = selectedIndex)
            }
            items(lists.size) { index ->
                val track = lists[index]

                val qualityPrefix = when {
                    track.isDolbyVision -> stringResource(R.string.ui_label_dolby_vision)
                    track.isHdr10 -> "HDR"
                    track.height >= 2160 -> "4K/UHD"
                    track.height >= 1440 -> "2K/1440P"
                    track.height >= 1080 -> "1080P"
                    track.height >= 720 -> "720P"
                    else -> stringResource(R.string.ui_label_standard_definition)
                }

                ListItem(
                    modifier = Modifier.padding(start = 15.dp, end = 2.dp, top = 10.dp, bottom = 10.dp) .let {
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
                        Text(
                            "$qualityPrefix " +
                                    "${String.format(Locale.getDefault(), "%.1f", track.bitrate / 1000.0 / 1000.0)}Mbps"
                        )
                    },
                    leadingContent = if (selectedIndex == index) {
                        { Icon(Icons.Filled.Check, contentDescription = "已选择") }
                    } else null,
                    trailingContent = {
                        if (track.isDolbyVision) {
                            Icon(painterResource(id = R.drawable.dolby_vision_seeklogo), "杜比视界", Modifier.size(38.dp))
                        } else if (track.isHdr10) {
                            Icon(painterResource(id = R.drawable.hdr_1), "HDR", Modifier.size(38.dp))
                        }

                        val isHevc = track.codecs.contains("hev", true)
                        val isAvc = track.codecs.contains("avc", true)
                        val isAv1 = track.codecs.contains("av0", true)

                        if (isHevc) {
                            Icon(painterResource(id = R.drawable.h265), "H.265/HEVC", Modifier.size(46.dp, 23.dp))
                        } else if (isAvc) {
                            Icon(painterResource(id = R.drawable.h264), "H.264/AVC", Modifier.size(46.dp, 23.dp))
                        } else if (isAv1) {
                            Icon(painterResource(id = R.drawable.av1), "AV1", Modifier.size(46.dp, 23.dp))
                        }
                    },
                    onClick = {
                        onTrackSelected(track) // 把选中的轨道丢给外层，Player会处理具体的切换逻辑
                    }
                )
            }

        }
    }
}
}