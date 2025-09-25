package org.mz.mzbi.ui.videoplayer.components

import android.util.Log
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.tv.material3.Icon
import androidx.tv.material3.ListItem
import androidx.tv.material3.ListItemColors
import androidx.tv.material3.ListItemDefaults
import androidx.tv.material3.ListItemScale
import androidx.tv.material3.Text
import kotlinx.coroutines.launch
import org.mz.mzdkplayer.R
import org.mz.mzdkplayer.tool.focusOnInitialVisibility
import java.util.Locale


@androidx.annotation.OptIn(UnstableApi::class)
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun VideoTrackPanel(
    selectedIndex: Int,
    onSelectedIndexChange: (currentIndex: Int) -> Unit,
    lists: MutableList<Tracks.Group>,
    exoPlayer: ExoPlayer
) {
    val focusRequester = remember { FocusRequester() }
    val isVis = remember { mutableStateOf(false) }
    var videoCode = ""
    var videoHeight = 0
    var videoFrame = 0f
    var videoBitmap = 0
    var videoId: String? = "120"
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()

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
                        text = "该文件无视频轨道",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 20.sp
                    )
                }
            }
        } else {
            //LazyColumn滚到到当前选择位置
            coroutineScope.launch {
                listState.animateScrollToItem(index = selectedIndex)
            }
            items(lists.size) { index ->
                videoCode = lists[index].getTrackFormat(0).codecs.toString()
                videoHeight = lists[index].getTrackFormat(0).height
                videoFrame = lists[index].getTrackFormat(0).frameRate
                videoId = lists[index].getTrackFormat(0).id
                videoBitmap = lists[index].getTrackFormat(0).bitrate
                ListItem(
                    modifier = if (index == selectedIndex /*选中的获取焦点*/) {
                        Modifier
                            .padding(
                                start = 15.dp,
                                end = 15.dp,
                                top = 10.dp,
                                bottom = 10.dp
                            )
                            .focusOnInitialVisibility(isVis)
                    } else Modifier.padding(
                        start = 15.dp,
                        end = 15.dp,
                        top = 10.dp,
                        bottom = 10.dp
                    ),
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
                    //scale = ListItemScale(1.03f,1.03f,1.03f,1.03f,1.03f,1.03f,1.03f,1.03f),
                    headlineContent = {
                        if (videoCode != "" && videoId != null) {
                            if (!videoCode.contains("dvh", true) && "127" == videoId
                            ) {
                                Text(
                                    "8K 超高清 ${videoHeight}P ${
                                        String.format(
                                            Locale.CHINA,
                                            "%.1f",
                                            videoBitmap / 1000.0 / 1000.0
                                        )
                                    }Mbps"
                                )
                            } else if (videoCode.contains("dvh", true) && "126" == videoId) {
                                Text(
                                    "杜比视界 ${videoHeight}P ${
                                        String.format(
                                            Locale.CHINA,
                                            "%.1f",
                                            videoBitmap / 1000.0 / 1000.0
                                        )
                                    }Mbps"
                                )
                            } else if (!videoCode.contains("dvh", true) && "125" == videoId
                            ) {
                                Text(
                                    "HDR ${videoHeight}P ${
                                        String.format(
                                            Locale.CHINA,
                                            "%.1f",
                                            videoBitmap / 1000.0 / 1000.0
                                        )
                                    }Mbps"
                                )
                            } else if (!videoCode.contains("dvh", true) && "120" == videoId
                            ) {
                                Text(
                                    "4K 超高清 ${videoHeight}P ${
                                        String.format(
                                            Locale.CHINA,
                                            "%.1f",
                                            videoBitmap / 1000.0 / 1000.0
                                        )
                                    }Mbps"
                                )
                            } else if (!videoCode.contains("dvh", true) && "116" == videoId
                            ) {
                                Text(
                                    "1080P60 高帧率 ${videoHeight}P ${
                                        String.format(
                                            Locale.CHINA,
                                            "%.1f",
                                            videoBitmap / 1000.0 / 1000.0
                                        )
                                    }Mbps"
                                )
                            } else if (!videoCode.contains("dvh", true) && "112" == videoId
                            ) {
                                Text(
                                    "1080P+ 高码率 ${videoHeight}P ${
                                        String.format(
                                            Locale.CHINA,
                                            "%.1f",
                                            videoBitmap / 1000.0 / 1000.0
                                        )
                                    }Mbps"
                                )
                            } else if (!videoCode.contains("dvh", true) && "100" == videoId
                            ) {
                                Text(
                                    "智能修复 ${videoHeight}P ${
                                        String.format(
                                            Locale.CHINA,
                                            "%.1f",
                                            videoBitmap / 1000.0 / 1000.0
                                        )
                                    }Mbps"
                                )
                            } else if (!videoCode.contains("dvh", true) && "80" == videoId
                            ) {
                                Text(
                                    "1080P 高清 ${videoHeight}P ${
                                        String.format(
                                            Locale.CHINA,
                                            "%.1f",
                                            videoBitmap / 1000.0 / 1000.0
                                        )
                                    }Mbps"
                                )
                            } else if (!videoCode.contains("dvh", true) && "74" == videoId
                            ) {
                                Text(
                                    "720P60 高帧率 ${videoHeight}P ${
                                        String.format(
                                            Locale.CHINA,
                                            "%.1f",
                                            videoBitmap / 1000.0 / 1000.0
                                        )
                                    }Mbps"
                                )
                            } else if (!videoCode.contains("dvh", true) && "64" == videoId
                            ) {
                                Text(
                                    "720P 高清 ${videoHeight}P ${
                                        String.format(
                                            Locale.CHINA,
                                            "%.1f",
                                            videoBitmap / 1000.0 / 1000.0
                                        )
                                    }Mbps"
                                )
                            } else if (!videoCode.contains("dvh", true) && "32" == videoId
                            ) {
                                Text(
                                    "480P 清晰 ${videoHeight}P ${
                                        String.format(
                                            Locale.CHINA,
                                            "%.1f",
                                            videoBitmap / 1000.0 / 1000.0
                                        )
                                    }Mbps"
                                )
                            } else if (!videoCode.contains("dvh", true) && "16" == videoId
                            ) {
                                Text(
                                    "360P 流畅 ${videoHeight}P ${
                                        String.format(
                                            Locale.CHINA,
                                            "%.1f",
                                            videoBitmap / 1000.0 / 1000.0
                                        )
                                    }Mbps"
                                )
                            } else {
                                Text(
                                    " ${videoHeight}P ${
                                        String.format(
                                            Locale.CHINA,
                                            "%.1f",
                                            videoBitmap / 1000.0 / 1000.0
                                        )
                                    }Mbps"
                                )
                            }
                        }
                    },
                    leadingContent = if (selectedIndex == index) {
                        {
                            Icon(
                                Icons.Filled.Check,
                                contentDescription = "Localized description",
                            )
                        }
                    } else null,
                    trailingContent = {

                        if (videoCode != "") {
                            if (videoCode.contains("dvh", true)) {
                                Icon(
                                    painterResource(id = R.drawable.dolby_vision_seeklogo),
                                    contentDescription = "Localized description",
                                )
                            }
                            if (videoCode.contains("hev", true)) {
                                Icon(
                                    painterResource(id = R.drawable.h265),
                                    contentDescription = "Localized description",
                                )
                            }
                            if (videoCode.contains("avc", true)) {
                                Icon(
                                    painterResource(id = R.drawable.h264),
                                    contentDescription = "Localized description",
                                )
                            }
                            if (videoCode.contains("av0", true)) {
                                Icon(
                                    painterResource(id = R.drawable.av1),
                                    contentDescription = "Localized description",
                                )
                            }
                        }
                    },
                    onClick = {
                        onSelectedIndexChange(index);
                        exoPlayer.trackSelectionParameters =
                            exoPlayer.trackSelectionParameters.buildUpon().setOverrideForType(
                                TrackSelectionOverride(
                                    lists[index].mediaTrackGroup,
                                    0
                                )
                            ).build();
                    }
                )
            }

        }
    }
}