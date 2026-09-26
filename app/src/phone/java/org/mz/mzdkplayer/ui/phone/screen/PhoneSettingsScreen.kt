package org.mz.mzdkplayer.ui.phone.screen

import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.mz.mzdkplayer.BuildConfig
import org.mz.mzdkplayer.R
import org.mz.mzdkplayer.data.model.PhoneThemeMode

/**
 * 手机端设置（第一阶段）。
 *
 * 目前只放**外观**这一类（白天/黑夜切换是手机端的硬需求），
 * 其余 8 类设置项仍留在电视端 `SettingsScreen`，后续阶段再逐个迁移；
 * 底层存储统一走 `SettingsRepository`，两边不会各存一份。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhoneSettingsScreen(
    themeMode: PhoneThemeMode,
    onThemeModeChange: (PhoneThemeMode) -> Unit,
    dynamicColor: Boolean,
    onDynamicColorChange: (Boolean) -> Unit,
    /** 进入目录时是否自动刮削（手机端独有，默认关） */
    autoScrape: Boolean,
    onAutoScrapeChange: (Boolean) -> Unit,
    /** 各来源当前是否参与刮削；顺序即展示顺序 */
    scrapeSources: List<Pair<PhoneFileProtocol, Boolean>>,
    onScrapeSourceChange: (PhoneFileProtocol, Boolean) -> Unit,
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text(stringResource(R.string.ui_label_settings)) },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SectionTitle(stringResource(R.string.phone_settings_section_appearance))
            ThemeModeCard(themeMode = themeMode, onThemeModeChange = onThemeModeChange)
            DynamicColorCard(dynamicColor = dynamicColor, onDynamicColorChange = onDynamicColorChange)

            SectionTitle(stringResource(R.string.phone_settings_section_scrape))
            AutoScrapeCard(autoScrape = autoScrape, onAutoScrapeChange = onAutoScrapeChange)
            ScrapeSourceCard(
                sources = scrapeSources,
                onSourceChange = onScrapeSourceChange,
            )

            SectionTitle(stringResource(R.string.phone_setting_about))
            VersionCard()

            Text(
                text = stringResource(R.string.phone_setting_more_coming),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 8.dp),
    )
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

/** 一整组开关的卡片：统一圆角与配色，组内用分隔线断开 */
@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Column(content = content)
    }
}

/**
 * 「进入目录时自动刮削」总开关（手机端独有，默认关）。
 *
 * 与下面的来源开关是**两件事**：这个决定「进目录要不要自动开始」，
 * 来源开关决定「这个来源参不参与刮削」——两者都满足才会自动联网。
 */
@Composable
private fun AutoScrapeCard(autoScrape: Boolean, onAutoScrapeChange: (Boolean) -> Unit) {
    SettingsCard {
        ListItem(
            supportingContent = { Text(stringResource(R.string.phone_setting_auto_scrape_sub)) },
            trailingContent = {
                Switch(checked = autoScrape, onCheckedChange = onAutoScrapeChange)
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.phone_setting_auto_scrape))
        }
    }
}

@Composable
private fun ScrapeSourceCard(
    sources: List<Pair<PhoneFileProtocol, Boolean>>,
    onSourceChange: (PhoneFileProtocol, Boolean) -> Unit,
) {
    SettingsCard {
        Column(
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = stringResource(R.string.phone_setting_scrape_sources),
                style = MaterialTheme.typography.titleSmall,
            )
            Text(
                text = stringResource(R.string.phone_setting_scrape_sources_sub),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        sources.forEachIndexed { index, (protocol, enabled) ->
            if (index > 0) {
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            }
            ListItem(
                trailingContent = {
                    Switch(
                        checked = enabled,
                        onCheckedChange = { onSourceChange(protocol, it) },
                    )
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(protocol.displayLabel())
            }
        }
    }
}

@Composable
private fun VersionCard() {
    ListItem(
        supportingContent = { Text("${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})") },
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(stringResource(R.string.phone_setting_version))
    }
}

private fun PhoneThemeMode.labelRes(): Int = when (this) {
    PhoneThemeMode.SYSTEM -> R.string.phone_theme_system
    PhoneThemeMode.LIGHT -> R.string.phone_theme_light
    PhoneThemeMode.DARK -> R.string.phone_theme_dark
}
