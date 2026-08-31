package org.mz.mzdkplayer.ui.videoplayer.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Icon
import androidx.tv.material3.ListItem
import androidx.tv.material3.ListItemDefaults
import androidx.tv.material3.Text
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.mz.mzdkplayer.R
import org.mz.mzdkplayer.player.core.MzBasicTrack
import org.mz.mzdkplayer.tool.Tools
import org.mz.mzdkplayer.tool.focusOnInitialVisibility
import org.mz.mzdkplayer.ui.screen.common.MyIconButton
import org.mz.mzdkplayer.ui.theme.myListItemCoverColor

@Composable
fun SubtitleTrackPanel(
    subtitleTracks: StateFlow<List<MzBasicTrack>>,   // 改成 StateFlow
    onTrackSelected: (MzBasicTrack) -> Unit,
    onLoadExternalSubtitle: () -> Unit // 新增：将加载外部字幕的逻辑交给调用者（PlayerScreen）去处理
) {
    val focusRequester = remember { FocusRequester() }
    val isVis = remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val lists by subtitleTracks.collectAsState()   // ← 关键
    // 找出当前选中的 index
    val selectedIndex = lists.indexOfFirst { it.isSelected }.takeIf { it >= 0 } ?: 0

    Column(modifier = Modifier.width(360.dp).fillMaxHeight()) {
        Row(
            modifier = Modifier.width(360.dp).padding(horizontal = 15.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.ui_label_subtitle_tracks),
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontSize = 24.sp,
                modifier = Modifier.weight(1f)
            )

            MyIconButton(
                text = stringResource(R.string.ui_label_load_external_subtitles),
                icon = R.drawable.baseline_search_24,
                onClick = {
                    onLoadExternalSubtitle() // 直接调用回调，解耦到底层实现
                }
            )
        }
        LazyColumn(
            modifier = Modifier.width(360.dp).fillMaxHeight().focusRequester(focusRequester),
            state = listState
        ) {
            if (lists.isEmpty()) {
                item {
                    Box(modifier = Modifier.width(360.dp), contentAlignment = Alignment.Center) {
                        Text(stringResource(R.string.ui_label_no_subtitle_tracks_in_file), fontWeight = FontWeight.Bold, color = Color.White, fontSize = 20.sp)
                    }
                }
            } else {
                coroutineScope.launch {
                    listState.animateScrollToItem(index = selectedIndex)
                }
                items(lists.size) { index ->
                    val track = lists[index]

                    val trackLabel = track.name
                    val trackLang = track.language
                    val trackMimeType = track.mimeType.ifEmpty {stringResource(R.string.ui_label_unknown_format) }

                    val isExternalAutoSearch = trackLabel.contains("[外部加载]")
                    val labelText = if (isExternalAutoSearch) {
                        trackLabel.substringAfter("[外部加载]").trim()
                    } else {
                        trackLabel
                    }
                    val languageText = Tools.getFullLanguageName(trackLang)

                    val titleText = when {
                        isExternalAutoSearch -> "$languageText $labelText ${stringResource(R.string.ui_label_subtitle_file_not_found_hint)}"
                        trackLabel.isNotEmpty() -> "$languageText $labelText"
                        else -> "$languageText ${stringResource(R.string.ui_label_language_subtitles)}"
                    }
                    println("TrackDebug: name='$trackLabel', isExternal=$isExternalAutoSearch")

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
                            if (track.id!="-1") {
                                Text(
                                    text = titleText,
                                )
                            }else{
                                Text(
                                    text = stringResource(R.string.ui_label_subtitle_off),
                                )
                            }
                        },
                        overlineContent = {
                            if (track.id!="-1") {
                            Text(stringResource(R.string.ui_label_subtitle_format,trackMimeType))
                                }
                        },
                        leadingContent = if (selectedIndex == index) {
                            { Icon(Icons.Filled.Check, contentDescription = stringResource(R.string.ui_label_selected)) }
                        } else null,
                        onClick = {
                            onTrackSelected(track)
                        }
                    )
                }
            }
        }
    }
}