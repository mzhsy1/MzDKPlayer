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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Icon
import androidx.tv.material3.ListItem
import androidx.tv.material3.ListItemDefaults
import androidx.tv.material3.Text
import org.mz.mzdkplayer.tool.focusOnInitialVisibility
import org.mz.mzdkplayer.ui.screen.vm.SettingsViewModel

@Composable
fun VideoFinishActionPanel(settingsViewModel: SettingsViewModel) {
    val settingsState by settingsViewModel.uiState.collectAsState()
    val currentAction = settingsState.videoFinishAction
    val actions = listOf(
        0 to "循环播放",
        1 to "播放暂停",
        2 to "播放下一个"
    )

    val focusRequester = remember { FocusRequester() }
    val isVis = remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    val selectedIndex = actions.indexOfFirst { it.first == currentAction }.takeIf { it >= 0 } ?: 0

    LaunchedEffect(currentAction) {
        listState.animateScrollToItem(index = selectedIndex)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "播放完成动作",
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
            items(actions.size) { index ->
                val (actionValue, actionName) = actions[index]
                val isSelected = currentAction == actionValue
                ListItem(
                    selected = false,
                    onClick = { settingsViewModel.setVideoFinishAction(actionValue) },
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
                    headlineContent = { Text(actionName, fontWeight = FontWeight.Medium) },
                    leadingContent = if (isSelected) {
                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(20.dp)) }
                    } else null
                )
            }
        }
    }
}
