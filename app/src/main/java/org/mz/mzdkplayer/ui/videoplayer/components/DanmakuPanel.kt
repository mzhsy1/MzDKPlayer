package org.mz.mzdkplayer.ui.videoplayer.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Icon
import androidx.tv.material3.ListItem
import androidx.tv.material3.ListItemDefaults
import androidx.tv.material3.Switch
import androidx.tv.material3.Text

@Composable
@Preview
fun DanmakuPanel() {
    val focusRequester = remember { FocusRequester() }
    var isSwitch by remember { mutableStateOf(false) }
    val screenRatios = listOf("1/2屏", "1/6屏", "1/4屏", "全屏")
    var selectedRatio by remember { mutableStateOf("1/2屏") }
    Column (modifier = Modifier.background(Color.Black)) {
        Text("弹幕开关", color = Color.White)
        Switch(checked = isSwitch, onCheckedChange = { isSwitch = it })
        Text("弹幕显示区域", color = Color.White)
        Row (){
            screenRatios.forEach { ratio ->
                ListItem(
                    selected = ratio == selectedRatio,
                    onClick = { selectedRatio = ratio },
                    modifier = Modifier.padding(horizontal = 2.dp).widthIn(120.dp,120.dp),
                    colors = ListItemDefaults.colors(
                        containerColor = Color(0, 0, 0),
                        contentColor = Color(255, 255, 255),
                        selectedContainerColor = Color(0, 0, 0),
                        selectedContentColor = Color(255, 255, 255),
                        focusedSelectedContentColor = Color(0, 0, 0),
                        focusedSelectedContainerColor = Color(255, 255, 255),
                        focusedContainerColor = Color(255, 255, 255),
                        focusedContentColor = Color(0, 0, 0)
                    ),
                    scale = ListItemDefaults.scale(scale = 1.0f, focusedScale = 1.0f),

                    leadingContent = if (ratio == selectedRatio) {
                        {
                            Icon(

                                imageVector = Icons.Default.Check,
                                contentDescription = "已选中",
                            )
                        }
                    } else null,
                    headlineContent = { Text(text = ratio) }
                )
            }
        }
    }



}