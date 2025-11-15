package org.mz.mzdkplayer.ui.videoplayer.components


import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.media3.exoplayer.ExoPlayer
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.ListItem
import androidx.tv.material3.ListItemDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Switch
import androidx.tv.material3.SwitchDefaults
import androidx.tv.material3.Text

import com.kuaishou.akdanmaku.ext.RETAINER_BILIBILI
import com.kuaishou.akdanmaku.ui.DanmakuPlayer
import org.mz.mzdkplayer.data.model.DanmakuScreenRatio

import org.mz.mzdkplayer.data.repository.DanmakuSettingsManager
import org.mz.mzdkplayer.data.model.DanmakuType
import org.mz.mzdkplayer.ui.screen.vm.VideoPlayerViewModel

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

@Composable
fun DanmakuPanel(
    danmakuPlayer: DanmakuPlayer,
    videoPlayerViewModel: VideoPlayerViewModel,
    exoPlayer: ExoPlayer
) {
    val context = LocalContext.current
    val settingsManager = remember { DanmakuSettingsManager(context) }

    // 从本地加载设置
    val initialSettings = remember { settingsManager.loadSettings() }

    val focusRequester = remember { FocusRequester() }
    var isSwitch by rememberSaveable { mutableStateOf(initialSettings.isSwitchEnabled) }
    // 使用枚举获取显示名称列表
    val screenRatios = remember { DanmakuScreenRatio.displayNames }

    var selectedRatio by rememberSaveable(initialSettings.selectedRatio) { mutableStateOf(initialSettings.selectedRatio) }

    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    // 弹幕字号和透明度状态
    var fontSize by rememberSaveable { mutableIntStateOf(initialSettings.fontSize) }
    var transparency by rememberSaveable { mutableIntStateOf(initialSettings.transparency) }

    // 弹幕类型过滤状态
    var selectedTypes by rememberSaveable { mutableStateOf(initialSettings.selectedTypes) }
    val danmakuTypes = remember { DanmakuType.displayNames }

    // 用于强制更新的临时状态
    var updateTrigger by remember { mutableIntStateOf(0) }

    // 用于标记screenPart是否发生变化
    var previousScreenPart by remember { mutableFloatStateOf(videoPlayerViewModel.danmakuConfig.screenPart) }

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

    // 焦点管理：当焦点离开面板时，重新请求焦点
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    // 处理配置更新，对selectedRatio变化添加延迟以确保生效
    LaunchedEffect(isSwitch, selectedRatio, fontSize, transparency, selectedTypes, updateTrigger) {
        // 从枚举获取比例值
        val screenPartValue = DanmakuScreenRatio.fromDisplayName(selectedRatio).ratioValue

        videoPlayerViewModel.danmakuConfig = videoPlayerViewModel.danmakuConfig.copy(
            retainerPolicy = RETAINER_BILIBILI,
            visibility = isSwitch,
            screenPart = screenPartValue,
            textSizeScale = fontSize.toFloat() / 100,
            alpha = transparency.toFloat() / 100,
            dataFilter = listOf(videoPlayerViewModel.createDanmakuTypeFilter(selectedTypes)) // 添加弹幕过滤器
        )

        Log.d("DanmakuPanel", "Updating config: visibility=$isSwitch, screenPart=$screenPartValue, fontSize=$fontSize, transparency=$transparency, selectedRatio=$selectedRatio, updateTrigger=$updateTrigger")
        videoPlayerViewModel.danmakuConfig.updateFilter()
        // 先更新配置
        danmakuPlayer.updateConfig(videoPlayerViewModel.danmakuConfig)

        // 强制更新过滤器以立即生效

        videoPlayerViewModel.danmakuConfig.updateVisibility()
        //danmakuPlayer.seekTo(exoPlayer.currentPosition)
        videoPlayerViewModel.danmakuVisibility = isSwitch
        // 关键修复：当screenPart变化时，需要更新layoutGeneration和retainerGeneration来触发重新布局和排布
        if (previousScreenPart != screenPartValue) {
            videoPlayerViewModel.danmakuConfig.updateLayout()
            videoPlayerViewModel.danmakuConfig.updateRetainer()
            previousScreenPart = screenPartValue
        }
    }

    // 当selectedRatio变化时，滚动到对应项
    LaunchedEffect(selectedRatio) {
        val selectedIndex = screenRatios.indexOf(selectedRatio)
        if (selectedIndex >= 0) {
            listState.animateScrollToItem(selectedIndex)
        }
    }

    Column (
        modifier = Modifier
            .background(Color(0xFF121212)) // 深灰色背景
            .padding(16.dp)
            .focusRequester(focusRequester) // 使整个Column可获得焦点

    )
    {
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
                onCheckedChange = {
                    isSwitch = it
                    // 当开关状态改变时，也触发一次更新
                    updateTrigger++
                },
                colors = SwitchDefaults.colors(
                    uncheckedThumbColor = Color(0xFFB0B0B0), // 未选中时的灰色
                    uncheckedTrackColor = Color(0xFF555555),  // 未选中时的轨道颜色
                    checkedThumbColor = Color(0xFFEEEEEE),   // 选中时的灰色
                    checkedTrackColor = Color(0xFF999999),    // 选中时的轨道颜色
                    // 通过增加边框颜色来增强焦点效果
                    uncheckedBorderColor = Color(0xFFFDFDFD), // 未选中时的边框颜色
                    checkedBorderColor = Color(0xFFFFA500)    // 选中时的边框颜色（橙色）
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
            items(screenRatios.size) { index ->
                val isSelected = screenRatios[index] == selectedRatio

                ListItem(
                    selected = isSelected,
                    onClick = {
                        selectedRatio = screenRatios[index]
                        // 当选择变化时，触发更新
                        updateTrigger++
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
            onSelectionChange = {
                selectedTypes = it
                // 当类型选择变化时，也触发更新
                updateTrigger++
            },
            title = "按类型过滤"
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 弹幕字号控制
        NumberControl(
            value = fontSize,
            onValueChange = {
                fontSize = it
                // 当字号变化时，也触发更新
                updateTrigger++
            },
            maxValue = 200,
            minValue = 10,
            label = "弹幕字号"
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 弹幕透明度控制
        NumberControl(
            value = transparency,
            onValueChange = {
                transparency = it
                // 当透明度变化时，也触发更新
                updateTrigger++
            },
            maxValue = 100,
            minValue = 0,
            label = "弹幕透明度"
        )
    }
}



