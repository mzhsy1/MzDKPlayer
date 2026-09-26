package org.mz.mzdkplayer.ui.videoplayer.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import org.mz.mzdkplayer.R
import org.mz.mzdkplayer.tool.focusOnInitialVisibility
import org.mz.mzdkplayer.ui.common.VIDEO_FINISH_ACTION_COUNT
import org.mz.mzdkplayer.ui.common.formatVideoFinishAction
import org.mz.mzdkplayer.viewmodel.SettingsViewModel

@Composable
fun VideoFinishActionPanel(settingsViewModel: SettingsViewModel) {
    val settingsState by settingsViewModel.uiState.collectAsState()
    val currentAction = settingsState.videoFinishAction

    val focusRequester = remember { FocusRequester() }
    val isVis = remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    // 0 循环播放 / 1 播放暂停 / 2 播放下一个，与设置页「播放完成动作」共用同一套文案
    val selectedIndex = currentAction.takeIf { it in 0 until VIDEO_FINISH_ACTION_COUNT } ?: 0

    LaunchedEffect(currentAction) {
        listState.animateScrollToItem(index = selectedIndex)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = stringResource(R.string.setting_video_finish_action),
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
            items(VIDEO_FINISH_ACTION_COUNT) { index ->
                val isSelected = currentAction == index
                ListItem(
                    selected = false,
                    onClick = { settingsViewModel.setVideoFinishAction(index) },
                    modifier = Modifier
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                        .let {
                            if (index == selectedIndex) it.focusOnInitialVisibility(isVis) else it
                        },
                    shape = ListItemDefaults.shape(RoundedCornerShape(12.dp)),
                    colors = ListItemDefaults.colors(
                        containerColor = Color.Transparent,
                        contentColor = Color.White.copy(alpha = 0.8f),
                        focusedContainerColor = Color.White,
                        focusedContentColor = Color.Black
                    ),
                    headlineContent = { Text(formatVideoFinishAction(index), fontWeight = FontWeight.Medium) },
                    leadingContent = if (isSelected) {
                        {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = stringResource(R.string.ui_label_selected),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    } else null
                )
            }
        }
    }
}
