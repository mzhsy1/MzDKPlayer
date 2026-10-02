package org.mz.mzdkplayer.ui.phone.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.mz.mzdkplayer.R
import org.mz.mzdkplayer.ui.phone.PhoneIcons

/**
 * 手机端设置页的公共骨架与基础控件（第八阶段）。
 *
 * 这一层的存在理由很直接：设置项从原来的 8 条涨到 40 条左右，如果每条都手写
 * `ListItem` + `Switch`，改一处间距就要改四十处；而且「选档位」这类交互在手机上
 * 应该统一成底部弹出的单选列表（电视端是「确定键循环切换」，手机上那么做很别扭）。
 *
 * 于是把「长得一样的东西」全部收在这里：
 * - [PhoneSettingsPageScaffold] / [PhoneSettingsHomeScaffold]：外壳与滚动；
 * - [SettingsCard] / [SettingsSectionTitle]：分组视觉；
 * - [SettingsSwitchRow] / [SettingsEntryRow]：开关行与「点进去 / 点一下执行」行；
 * - [SettingsNumberRow]：带 ± 的数值调节（替代电视端遥控器上的左右键累计）；
 * - [SettingsOptionsRow]：点开底部单选面板选档位；
 * - [SettingsConfirmDialog]：不可撤销操作（清理数据库）前的确认。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PhoneSettingsHomeScaffold(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text(title) },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { innerPadding ->
        SettingsContentColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            content = content,
        )
    }
}

/** 设置二级页的外壳：普通顶栏 + 返回箭头 + 可滚动内容（底部栏在二级页会收起） */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PhoneSettingsPageScaffold(
    title: String,
    onBack: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.phone_action_back),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        SettingsContentColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            content = content,
        )
    }
}

/** 设置页内容的统一留白与行距；首页与二级页共用，视觉上不会随页面漂移 */
@Composable
private fun SettingsContentColumn(
    modifier: Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        content = content,
    )
}

@Composable
internal fun SettingsSectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 8.dp),
    )
}

/** 一整组设置的卡片：统一圆角与配色，组内用分隔线断开 */
@Composable
internal fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Column(content = content)
    }
}

/** 开关行 */
@Composable
internal fun SettingsSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    subtitle: String? = null,
    enabled: Boolean = true,
) {
    ListItem(
        supportingContent = subtitle?.let { { Text(it) } },
        trailingContent = {
            Switch(
                checked = checked,
                enabled = enabled,
                onCheckedChange = onCheckedChange,
            )
        },
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(title, color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/**
 * 「点一下有反应」的行：可能跳到别的页面，也可能执行一个动作。
 *
 * [value] 非空时显示在右侧（当前选中项的文案），[trailingChevron] 用于「点进去」的语义。
 */
@Composable
internal fun SettingsEntryRow(
    title: String,
    onClick: () -> Unit,
    subtitle: String? = null,
    value: String? = null,
    enabled: Boolean = true,
    trailingChevron: Boolean = false,
) {
    ListItem(
        supportingContent = subtitle?.let { { Text(it) } },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (value != null) {
                    Text(
                        text = value,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.End,
                        modifier = Modifier.widthIn(max = 160.dp),
                    )
                }
                if (trailingChevron) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        modifier = Modifier
                            .size(20.dp)
                            .padding(start = 4.dp),
                    )
                }
            }
        },
        enabled = enabled,
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(title)
    }
}

/**
 * 带 ± 的数值调节行。
 *
 * 电视端用遥控器左右键累计（`NumberControl`），手机上那套没法用：
 * 这里给「1」个明确的可点区域，[step] 由调用方按「手指点得动」来定
 * （例如快进时长 5 秒一档、字幕字号 2sp 一档）。
 */
@Composable
internal fun SettingsNumberRow(
    title: String,
    value: Int,
    onValueChange: (Int) -> Unit,
    step: Int = 1,
    minValue: Int = Int.MIN_VALUE,
    maxValue: Int = Int.MAX_VALUE,
    subtitle: String? = null,
    valueText: String = value.toString(),
    enabled: Boolean = true,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        IconButton(
            onClick = { onValueChange((value - step).coerceAtLeast(minValue)) },
            enabled = enabled && value > minValue,
        ) {
            Icon(imageVector = PhoneIcons.Minus, contentDescription = null)
        }
        Text(
            text = valueText,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(min = 64.dp),
        )
        IconButton(
            onClick = { onValueChange((value + step).coerceAtMost(maxValue)) },
            enabled = enabled && value < maxValue,
        ) {
            Icon(imageVector = Icons.Filled.Add, contentDescription = null)
        }
    }
}

/** 一个可选档位 */
internal data class SettingsOption<T>(val value: T, val label: String)

/**
 * 点开底部面板选档位的行。
 *
 * 手机上没有「确定键循环切换」，把 5 个选项的枚举一路点过去体验很差，
 * 所以统一改成弹出单选列表 —— 当前值直接显示在行右侧，一眼能看到。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun <T> SettingsOptionsRow(
    title: String,
    value: String,
    selected: T,
    options: List<SettingsOption<T>>,
    onSelect: (T) -> Unit,
    subtitle: String? = null,
    enabled: Boolean = true,
) {
    var sheetVisible by remember { mutableStateOf(false) }

    SettingsEntryRow(
        title = title,
        subtitle = subtitle,
        value = value,
        enabled = enabled,
        onClick = { sheetVisible = true },
    )

    if (sheetVisible) {
        ModalBottomSheet(
            onDismissRequest = { sheetVisible = false },
            sheetState = rememberModalBottomSheetState(),
        ) {
            Column(modifier = Modifier.navigationBarsPadding()) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 8.dp),
                )
                options.forEach { option ->
                    ListItem(
                        leadingContent = {
                            RadioButton(
                                selected = option.value == selected,
                                onClick = null,
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        // 整行可点：手机上再让用户去瞄那个小圆点属于自找麻烦
                        onClick = {
                            onSelect(option.value)
                            sheetVisible = false
                        },
                    ) {
                        Text(option.label)
                    }
                }
            }
        }
    }
}

/** 一组的最后一行之后不要分隔线，调用方按需在行之间插 [SettingsDivider] */
@Composable
internal fun SettingsDivider() {
    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
}

/** 不可撤销操作前的确认弹窗（清理数据库 / 清空播放历史 / 删除连接） */
@Composable
internal fun SettingsConfirmDialog(
    title: String,
    message: String,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm()
                    onDismiss()
                },
            ) {
                Text(confirmText)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.ui_label_cancel))
            }
        },
    )
}
