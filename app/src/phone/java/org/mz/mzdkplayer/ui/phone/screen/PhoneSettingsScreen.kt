package org.mz.mzdkplayer.ui.phone.screen

import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.mz.mzdkplayer.R
import org.mz.mzdkplayer.data.model.PhoneThemeMode
import org.mz.mzdkplayer.ui.phone.model.PhoneSettingCategory

/**
 * 手机端设置**首页**（第八阶段重做）。
 *
 * 第一阶段时这一页只有「外观 + 刮削 + 版本」三组，现在设置项补齐到约 40 条，
 * 全铺在一页上要滑很久，于是改成手机通用的两层结构：
 * **首页 = 外观（内联，最常用）+ 七个分类入口**，点进分类是独立一页。
 *
 * 底部标签栏在分类页会收起（见 `PhoneApp` 的 `PhoneTab` 判定），
 * 所以分类页的返回箭头就是「回到设置首页」。
 *
 * 外观留在这里而不是单独开一页：主题是手机上改得最勤的一项，
 * 多一层跳转纯属添堵。
 */
@Composable
internal fun PhoneSettingsScreen(
    themeMode: PhoneThemeMode,
    onThemeModeChange: (PhoneThemeMode) -> Unit,
    dynamicColor: Boolean,
    onDynamicColorChange: (Boolean) -> Unit,
    onOpenCategory: (PhoneSettingCategory) -> Unit,
) {
    PhoneSettingsHomeScaffold(title = stringResource(R.string.ui_label_settings)) {
        SettingsSectionTitle(stringResource(R.string.phone_settings_section_appearance))
        ThemeModeCard(themeMode = themeMode, onThemeModeChange = onThemeModeChange)
        DynamicColorCard(dynamicColor = dynamicColor, onDynamicColorChange = onDynamicColorChange)

        SettingsSectionTitle(stringResource(R.string.phone_settings_section_more))
        SettingsCard {
            PhoneSettingCategory.entries.forEachIndexed { index, category ->
                if (index > 0) SettingsDivider()
                ListItem(
                    leadingContent = {
                        Icon(
                            imageVector = category.icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp),
                        )
                    },
                    trailingContent = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { onOpenCategory(category) },
                ) {
                    Text(stringResource(category.titleRes))
                }
            }
        }
    }
}

@Composable
private fun ThemeModeCard(
    themeMode: PhoneThemeMode,
    onThemeModeChange: (PhoneThemeMode) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.phone_setting_theme_mode),
                style = MaterialTheme.typography.titleMedium,
            )
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                PhoneThemeMode.entries.forEachIndexed { index, mode ->
                    SegmentedButton(
                        selected = mode == themeMode,
                        onClick = { onThemeModeChange(mode) },
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = PhoneThemeMode.entries.size,
                        ),
                        // 三段式开关里再塞一个对勾会把中文标签挤断行，这里只要文案
                        icon = {},
                        label = { Text(stringResource(mode.labelRes())) },
                    )
                }
            }
        }
    }
}

@Composable
private fun DynamicColorCard(
    dynamicColor: Boolean,
    onDynamicColorChange: (Boolean) -> Unit,
) {
    // 动态取色要 Android 12（API 31）才有系统调色板
    val supported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    SettingsCard {
        ListItem(
            supportingContent = { Text(stringResource(R.string.phone_setting_dynamic_color_sub)) },
            trailingContent = {
                Switch(
                    checked = supported && dynamicColor,
                    enabled = supported,
                    onCheckedChange = onDynamicColorChange,
                )
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.phone_setting_dynamic_color))
        }
    }
}

private fun PhoneThemeMode.labelRes(): Int = when (this) {
    PhoneThemeMode.SYSTEM -> R.string.phone_theme_system
    PhoneThemeMode.LIGHT -> R.string.phone_theme_light
    PhoneThemeMode.DARK -> R.string.phone_theme_dark
}
