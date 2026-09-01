package org.mz.mzdkplayer.ui.videoplayer.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Icon
import androidx.tv.material3.ListItem
import androidx.tv.material3.ListItemDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import kotlinx.coroutines.launch
import org.mz.mzdkplayer.R
import org.mz.mzdkplayer.player.core.MzBasicTrack
import org.mz.mzdkplayer.tool.Tools
import org.mz.mzdkplayer.tool.focusOnInitialVisibility
import java.util.Locale
import androidx.compose.ui.platform.LocalLocale

@Composable
fun AudioTrackPanel(
    lists: List<MzBasicTrack>, // 替换为统一接口数据
    onTrackSelected: (MzBasicTrack) -> Unit // 选中回调，丢给播放器处理
) {
    val focusRequester = remember { FocusRequester() }
    val isVis = remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    // 找出当前选中的 index
    val selectedIndex = lists.indexOfFirst { it.isSelected }.takeIf { it >= 0 } ?: 0

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = stringResource(R.string.ui_label_audio_track),
            fontWeight = FontWeight.Bold,
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
        if (lists.isEmpty()) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().height(300.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.ui_label_no_audio_tracks_in_file),
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 20.sp
                    )
                }
            }
        } else {
            coroutineScope.launch {
                listState.animateScrollToItem(index = selectedIndex)
            }
            items(lists.size) { index ->
                val track = lists[index]
                // 逻辑判断直接用封装好的数据
                //val channelCount = if (track.mimeType.contains("ec", true)) 6 else track.channelCount

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
                        Text("${Tools.getFullLanguageName(track.language)} ${track.bitrate / 1000}Kbps")
                    },
                    overlineContent = {
                        val sampleRateText = String.format(LocalLocale.current.platformLocale, "%.1f kHz", track.sampleRate / 1000.0)
                        // 注意：这里需要把你的 Tools.inferAudioFormatType 改造为接收 mimeType 字符串
                        Text(
                            text = stringResource(
                                id = R.string.ui_label_audio_track_details,
                                track.channelCount,                          // %1$d
                                Tools.inferAudioFormatType(track.mimeType), // %2$s
                                sampleRateText                               // %3$s
                            )
                        )
                    },
                    leadingContent = if (selectedIndex == index) {
                        { Icon(Icons.Filled.Check, contentDescription = "已选择") }
                    } else null,
                    trailingContent = {
                        // 注意：这里需要把你的 Tools.audioFormatIconType 改造为接收 mimeType 字符串
                        Icon(
                            painterResource(id = Tools.audioFormatIconType(track.mimeType)),
                            contentDescription = "格式图标",
                        )
                    },
                    onClick = {
                        onTrackSelected(track) // 纯粹把事件抛出
                    }
                )
            }
        }
    }
}
}