package org.mz.mzdkplayer.ui.videoplayer.components


import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.ListItem
import androidx.tv.material3.ListItemDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Switch
import androidx.tv.material3.Text
import kotlinx.coroutines.launch

import org.mz.mzdkplayer.logic.model.DanmakuSettingsManager

// 公共圆形按钮组件
@Composable
fun CircularIconButton(
    onClick: () -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    size: Int = 32,
    iconSize: Int = 18,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        modifier = modifier.size(size.dp),
        shape = ButtonDefaults.shape(shape = CircleShape),
        scale = ButtonDefaults.scale(1.0f),
        enabled = enabled,
        colors = ButtonDefaults.colors(
            containerColor = Color(0xFF444444), // 深灰色按钮背景
            contentColor = Color(0xFFFFFFFF),  // 白色图标
            focusedContainerColor = Color(0xFFFFFFFF), // 聚焦时白色背景
            focusedContentColor = Color(0xFF444444)    // 聚焦时深灰色图标
        ),
        contentPadding = PaddingValues(0.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Icon(
                imageVector = icon,
                modifier = Modifier.size(iconSize.dp),
                contentDescription = null
            )
        }
    }
}

// 公共数值控制组件
@Composable
fun NumberControl(
    value: Int,
    onValueChange: (Int) -> Unit,
    maxValue: Int = Int.MAX_VALUE,
    minValue: Int = Int.MIN_VALUE,
    label: String = ""
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .padding(vertical = 8.dp)
    ) {
        if (label.isNotEmpty()) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .background(Color(0xFF333333), shape = RoundedCornerShape(6.dp)) // 中灰色背景
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    label,
                    color = Color(0xFFFFFFFF), // 纯白色文字
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))
        }

        // 减少按钮
        CircularIconButton(
            onClick = {
                if (value > minValue) {
                    onValueChange(value - 1)
                }
            },
            icon = Icons.Outlined.KeyboardArrowDown
        )

        Spacer(modifier = Modifier.width(8.dp))

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .background(Color(0xFF333333), shape = RoundedCornerShape(6.dp)) // 中灰色背景
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text(
                value.toString(),
                color = Color(0xFFFFFFFF), // 纯白色文字
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // 增加按钮
        CircularIconButton(
            onClick = {
                if (value < maxValue) {
                    onValueChange(value + 1)
                }
            },
            icon = Icons.Outlined.KeyboardArrowUp
        )
    }
}

// 公共多选列表组件
@Composable
fun MultiSelectList(
    items: List<String>,
    selectedItems: Set<String>,
    onSelectionChange: (Set<String>) -> Unit,
    title: String
) {
    Text(
        title,
        color = Color(0xFFFFFFFF), // 纯白色文字
        modifier = Modifier
            .padding(vertical = 8.dp)
    )

    LazyRow(
        modifier = Modifier
            .widthIn(max = 300.dp)
            .padding(vertical = 8.dp)
    ) {
        items(items.size) { index ->
            val item = items[index]
            val isSelected = selectedItems.contains(item)

            ListItem(
                selected = isSelected,
                onClick = {
                    val newSelection = if (isSelected) {
                        selectedItems - item
                    } else {
                        selectedItems + item
                    }
                    onSelectionChange(newSelection)
                },
                modifier = Modifier
                    .padding(horizontal = 2.dp)
                    .widthIn(min = 60.dp, max = 100.dp)
                    .heightIn(30.dp, 45.dp),
                colors = ListItemDefaults.colors(
                    containerColor = Color(0xFF2C2C2C),      // 深灰色背景
                    contentColor = Color(0xFFFFFFFF),       // 白色文字
                    selectedContainerColor = Color(0xFF555555), // 选中时的灰色
                    selectedContentColor = Color(0xFFFFFFFF),   // 选中时的白色文字
                    focusedSelectedContentColor = Color(0xFF121212), // 聚焦选中时的文字颜色
                    focusedSelectedContainerColor = Color(0xFFFFFFFF), // 聚焦选中时的背景
                    focusedContainerColor = Color(0xFFFFFFFF), // 聚焦时的背景
                    focusedContentColor = Color(0xFF121212)    // 聚焦时的文字颜色
                ),
                shape = ListItemDefaults.shape(
                    shape = RoundedCornerShape(6.dp)
                ),
                scale = ListItemDefaults.scale(scale = 1.0f, focusedScale = 1.0f),

                leadingContent = if (isSelected) {
                    {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "已选中",
                        )
                    }
                } else null,
                headlineContent = {
                    Text(
                        text = item,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            )
        }
    }
}



// 数据类


@Composable
@Preview
fun DanmakuPanel() {
    val context = LocalContext.current
    val settingsManager = remember { DanmakuSettingsManager(context) }

    // 从本地加载设置
    val initialSettings = remember { settingsManager.loadSettings() }

    val focusRequester = remember { FocusRequester() }
    var isSwitch by rememberSaveable { mutableStateOf(initialSettings.isSwitchEnabled) }
    val screenRatios = remember{listOf("1/2", "1/4","1/6", "1/8",  "1/10","1/12","全屏")}
    var selectedRatio by rememberSaveable { mutableStateOf(initialSettings.selectedRatio) }
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    // 弹幕字号和透明度状态
    var fontSize by rememberSaveable { mutableIntStateOf(initialSettings.fontSize) }
    var transparency by rememberSaveable { mutableIntStateOf(initialSettings.transparency) }

    // 弹幕类型过滤状态
    var selectedTypes by rememberSaveable { mutableStateOf(initialSettings.selectedTypes) }
    val danmakuTypes = remember { listOf("滚动", "底部", "顶部", "彩色") }

    // 保存设置的函数
    val saveSettings = remember {
        {
            settingsManager.saveSettings(
                isSwitch,
                selectedRatio,
                fontSize,
                transparency,
                selectedTypes
            )
        }
    }

    // 使用 DisposableEffect 在组件销毁时保存设置
    DisposableEffect(isSwitch, selectedRatio, fontSize, transparency, selectedTypes) {
        // 当状态变化时保存设置
        saveSettings()

        onDispose { }
    }

    Column (
        modifier = Modifier
            .background(Color(0xFF121212)) // 深灰色背景
            .fillMaxWidth(0.3f)
            .padding(16.dp)
    ) {
        // 弹幕开关区域
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(vertical = 8.dp)
        )
        {
            Text(
                "弹幕开关",
                color = Color(0xFFFFFFFF), // 纯白色文字
                modifier = Modifier.padding(end = 8.dp)
            )
            Switch(
                checked = isSwitch,
                onCheckedChange = { isSwitch = it },
                colors = androidx.tv.material3.SwitchDefaults.colors(
                    uncheckedThumbColor = Color(0xFFB0B0B0), // 未选中时的灰色
                    uncheckedTrackColor = Color(0xFF555555),  // 未选中时的轨道颜色
                    checkedThumbColor = Color(0xFFEEEEEE),   // 选中时的灰色
                    checkedTrackColor = Color(0xFF999999)    // 选中时的轨道颜色
                )
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 弹幕显示区域标题
        Text(
            "弹幕显示区域",
            color = Color(0xFFFFFFFF), // 纯白色文字
            modifier = Modifier
                .padding(vertical = 8.dp)
        )

        LazyRow(
            state = listState,
            modifier = Modifier
                .widthIn(max = 300.dp)
                .padding(vertical = 8.dp)
        )
        {
            //LazyColumn滚到到当前选择位置
            coroutineScope.launch {
                listState.animateScrollToItem(index = 5)
            }
            items(screenRatios.size) { index ->

                ListItem(
                    selected = screenRatios[index] == selectedRatio,
                    onClick = {
                        selectedRatio = screenRatios[index]
                    },
                    modifier = Modifier
                        .padding(horizontal = 2.dp)
                        .widthIn(min = 60.dp, max = 100.dp)
                        .heightIn(30.dp, 45.dp),
                    colors = ListItemDefaults.colors(
                        containerColor = Color(0xFF2C2C2C),      // 深灰色背景
                        contentColor = Color(0xFFFFFFFF),       // 白色文字
                        selectedContainerColor = Color(0xFF555555), // 选中时的灰色
                        selectedContentColor = Color(0xFFFFFFFF),   // 选中时的白色文字
                        focusedSelectedContentColor = Color(0xFF121212), // 聚焦选中时的文字颜色
                        focusedSelectedContainerColor = Color(0xFFFFFFFF), // 聚焦选中时的背景
                        focusedContainerColor = Color(0xFFFFFFFF), // 聚焦时的背景
                        focusedContentColor = Color(0xFF121212)    // 聚焦时的文字颜色
                    ),
                    shape = ListItemDefaults.shape(
                        shape = RoundedCornerShape(6.dp)
                    ),
                    scale = ListItemDefaults.scale(scale = 1.0f, focusedScale = 1.0f),

                    leadingContent = if (screenRatios[index] == selectedRatio) {
                        {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "已选中",
                            )
                        }
                    } else null,
                    headlineContent = {
                        Text(
                            text = screenRatios[index],
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                )

            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 按类型过滤
        MultiSelectList(
            items = danmakuTypes,
            selectedItems = selectedTypes,
            onSelectionChange = { selectedTypes = it },
            title = "按类型过滤"
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 弹幕字号控制
        NumberControl(
            value = fontSize,
            onValueChange = { fontSize = it },
            maxValue = 200,
            minValue = 10,
            label = "弹幕字号"
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 弹幕透明度控制
        NumberControl(
            value = transparency,
            onValueChange = { transparency = it },
            maxValue = 100,
            minValue = 0,
            label = "弹幕透明度"
        )
    }
}



