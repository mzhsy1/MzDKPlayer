package org.mz.mzdkplayer.ui.videoplayer.components


import androidx.annotation.OptIn

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.toLowerCase
import androidx.compose.ui.unit.dp
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.tv.material3.Icon
import androidx.tv.material3.ListItem
import androidx.tv.material3.ListItemDefaults
import androidx.tv.material3.Text
import org.mz.mzdkplayer.R
import org.mz.mzdkplayer.tool.Tools
import org.mz.mzdkplayer.tool.focusOnInitialVisibility


@OptIn(UnstableApi::class)
@Composable
fun SubtitleTrackPanel(
    selectedIndex: Int,
    onSelectedIndexChange: (currentIndex: Int) -> Unit,
    lists: MutableList<Tracks.Group>,
    exoPlayer: ExoPlayer
) {

    val focusRequester = remember { FocusRequester() }
    // 每次进入获取焦点
    val isVis = remember { mutableStateOf(false) }
//    var channelCount = 2
//    var audioCode: String
    LazyColumn(modifier = Modifier
        .width(360.dp)
        .focusRequester(focusRequester)) {
        items(lists.size) { index ->
            //audioCode = lists[index].getTrackFormat(0).sampleMimeType.toString()
            //channelCount = if (audioCode.contains("ec", true)) 6 else lists[index].getTrackFormat(0).channelCount
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
                } else Modifier.padding(start = 15.dp, end = 15.dp, top = 10.dp, bottom = 10.dp),
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
                headlineContent = { Text("${(lists[index].getTrackFormat(0).language?.lowercase())}") },
//                overlineContent = {
//                    Text(
//                        "${channelCount}声道 · ${Tools.inferAudioFormatType(lists[index].getTrackFormat(0))}"
//                    )
//                },

                leadingContent = if (selectedIndex == index) {
                    {
                        Icon(
                            Icons.Filled.Check,
                            contentDescription = "Localized description",
                        )
                    }
                } else null,
//                trailingContent = {
//
//                        Icon(
//                            painterResource(id = Tools.audioFormatIconType(lists[index].getTrackFormat(0))),
//                            contentDescription = "Localized description",
//                        )
//
//
//                },
                onClick = {
                    onSelectedIndexChange(index)
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