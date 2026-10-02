package org.mz.mzdkplayer.ui.phone.screen

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import org.mz.mzdkplayer.BuildConfig
import org.mz.mzdkplayer.R
import org.mz.mzdkplayer.data.repository.Resource
import org.mz.mzdkplayer.data.repository.SettingsRepository
import org.mz.mzdkplayer.tool.logic.ScrapeSourcePolicy
import org.mz.mzdkplayer.ui.common.formatRecursiveScanLevel
import org.mz.mzdkplayer.ui.common.formatScrapeSource
import org.mz.mzdkplayer.ui.common.formatTmdbLang
import org.mz.mzdkplayer.ui.phone.PhoneStoragePermission
import org.mz.mzdkplayer.ui.phone.model.PhoneFileProtocol
import org.mz.mzdkplayer.ui.phone.scrapeKey
import org.mz.mzdkplayer.ui.phone.sourceEnabled
import org.mz.mzdkplayer.viewmodel.AudioViewModel
import org.mz.mzdkplayer.viewmodel.MediaHistoryViewModel
import org.mz.mzdkplayer.viewmodel.MovieViewModel
import org.mz.mzdkplayer.viewmodel.SettingsViewModel

/**
 * 手机端设置的「刮削与媒体库 / 工具 / 关于」三个分类页（第八阶段）。
 *
 * 刮削与媒体库这一页的存储键与电视端**完全共用**（`SettingsRepository` 里那六个
 * `source_*` 开关与 `phone_auto_scrape` 总开关），只是手机端多了一个「进目录自动刮」的
 * 总开关 —— 电视端是手动点「扫描」，没有「进目录自动开始」这个概念。
 */

@Composable
internal fun PhoneLibrarySettingsPage(
    settingsViewModel: SettingsViewModel,
    onBack: () -> Unit,
) {
    val state by settingsViewModel.uiState.collectAsState()
    var showTmdbDialog by remember { mutableStateOf(false) }

    PhoneSettingsPageScaffold(stringResource(R.string.cat_metadata), onBack) {
        SettingsSectionTitle(stringResource(R.string.phone_settings_section_scrape))
        SettingsCard {
            SettingsSwitchRow(
                title = stringResource(R.string.phone_setting_auto_scrape),
                subtitle = stringResource(R.string.phone_setting_auto_scrape_sub),
                checked = state.phoneAutoScrape,
                onCheckedChange = settingsViewModel::togglePhoneAutoScrape,
            )
        }

        SettingsSectionTitle(stringResource(R.string.phone_setting_scrape_sources))
        SettingsCard {
            Text(
                text = stringResource(R.string.phone_setting_scrape_sources_sub),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp),
            )
            PhoneFileProtocol.entries.forEachIndexed { index, protocol ->
                if (index > 0) SettingsDivider()
                SettingsSwitchRow(
                    title = protocol.displayLabel(),
                    checked = state.sourceEnabled(protocol),
                    onCheckedChange = { enabled ->
                        settingsViewModel.toggleSource(protocol.scrapeKey, enabled)
                    },
                )
            }
        }

        SettingsSectionTitle(stringResource(R.string.cat_metadata))
        SettingsCard {
            SettingsSwitchRow(
                title = stringResource(R.string.setting_prioritize_nfo),
                subtitle = stringResource(R.string.setting_prioritize_nfo_sub),
                checked = state.prioritizeLocalNfo,
                onCheckedChange = settingsViewModel::togglePrioritizeLocalNfo,
            )
            SettingsDivider()
            SettingsOptionsRow(
                title = stringResource(R.string.setting_scrape_source),
                subtitle = stringResource(R.string.setting_scrape_source_sub),
                value = formatScrapeSource(state.scrapeSourcePriority),
                selected = state.scrapeSourcePriority,
                options = scrapeSourceOptions(),
                onSelect = settingsViewModel::setScrapeSourcePriority,
            )
            SettingsDivider()
            SettingsOptionsRow(
                title = stringResource(R.string.setting_tmdb_search_lang),
                value = formatTmdbLang(state.tmdbSearchLang),
                selected = state.tmdbSearchLang,
                options = tmdbLangOptions(),
                onSelect = settingsViewModel::setTmdbSearchLang,
            )
            SettingsDivider()
            SettingsOptionsRow(
                title = stringResource(R.string.setting_tmdb_result_lang),
                value = formatTmdbLang(state.tmdbResultLang),
                selected = state.tmdbResultLang,
                options = tmdbLangOptions(),
                onSelect = settingsViewModel::setTmdbResultLang,
            )
            SettingsDivider()
            SettingsEntryRow(
                title = stringResource(R.string.setting_tmdb_api_mirror),
                subtitle = stringResource(R.string.setting_tmdb_api_mirror_sub),
                value = if (state.tmdbBaseUrl == SettingsRepository.DEFAULT_TMDB_URL) {
                    stringResource(R.string.phone_setting_tmdb_official)
                } else {
                    state.tmdbBaseUrl
                },
                onClick = { showTmdbDialog = true },
            )
            SettingsDivider()
            SettingsOptionsRow(
                title = stringResource(R.string.setting_recursive_scan_level),
                subtitle = stringResource(R.string.setting_recursive_scan_level_sub),
                value = formatRecursiveScanLevel(state.recursiveScanLevel),
                selected = state.recursiveScanLevel,
                options = (0..5).map { SettingsOption(it, formatRecursiveScanLevel(it)) },
                onSelect = settingsViewModel::setRecursiveScanLevel,
            )
            SettingsDivider()
            SettingsSwitchRow(
                title = stringResource(R.string.setting_webdav_remove_first_item),
                subtitle = stringResource(R.string.setting_webdav_remove_first_item_sub),
                checked = state.removeWebDavFirstItem,
                onCheckedChange = settingsViewModel::toggleRemoveWebDavFirstItem,
            )
        }
    }

    if (showTmdbDialog) {
        TmdbUrlDialog(
            settingsViewModel = settingsViewModel,
            onDismiss = { showTmdbDialog = false },
        )
    }
}

/**
 * TMDB 地址编辑弹窗。
 *
 * 与电视端的 `TMDBConfigDialog` 是同一件事，但那个对话框是遥控器风格（一大块文本框 +
 * 自定义键盘导航），手机上直接用系统输入法更自然。
 *
 * 保存时补一个结尾的 `/`：Retrofit 的 `baseUrl` 要求以 `/` 结尾，
 * 不补的话用户得自己记住这个规则，忘了就是一串看不懂的异常。
 */
@Composable
private fun TmdbUrlDialog(
    settingsViewModel: SettingsViewModel,
    onDismiss: () -> Unit,
) {
    val state by settingsViewModel.uiState.collectAsState()
    val testResult by settingsViewModel.tmdbTestResult.collectAsState()
    var text by remember { mutableStateOf(state.tmdbBaseUrl) }

    // 每次打开都从干净状态开始，免得看到上一次遗留的「连接失败」
    LaunchedEffect(Unit) { settingsViewModel.clearTmdbTestResult() }

    fun normalize(url: String): String = url.trim().let {
        if (it.isEmpty() || it.endsWith("/")) it else "$it/"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.setting_tmdb_api_mirror)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    singleLine = true,
                    label = { Text(stringResource(R.string.phone_setting_tmdb_url_hint)) },
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    text = stringResource(R.string.setting_tmdb_api_mirror_sub),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                when (val result = testResult) {
                    Resource.Loading -> Text(
                        text = stringResource(R.string.phone_setting_tmdb_testing),
                        style = MaterialTheme.typography.bodySmall,
                    )

                    is Resource.Success -> Text(
                        text = stringResource(R.string.phone_setting_tmdb_test_ok),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                    )

                    is Resource.Error -> Text(
                        text = stringResource(R.string.phone_setting_tmdb_test_failed, result.message),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )

                    null -> Unit
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    settingsViewModel.setTmdbBaseUrl(normalize(text))
                    onDismiss()
                },
            ) {
                Text(stringResource(R.string.phone_action_save))
            }
        },
        dismissButton = {
            Row {
                TextButton(
                    onClick = {
                        text = SettingsRepository.DEFAULT_TMDB_URL
                        settingsViewModel.clearTmdbTestResult()
                    },
                ) {
                    Text(stringResource(R.string.phone_setting_tmdb_reset))
                }
                TextButton(
                    onClick = { settingsViewModel.testTmdbConnection(normalize(text)) },
                ) {
                    Text(stringResource(R.string.phone_setting_tmdb_test))
                }
            }
        },
    )
}

@Composable
internal fun PhoneToolsSettingsPage(
    movieViewModel: MovieViewModel,
    audioViewModel: AudioViewModel,
    mediaHistoryViewModel: MediaHistoryViewModel,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    var granted by remember { mutableStateOf(PhoneStoragePermission.isGranted(context)) }

    // 用「带结果的启动」而不是普通 startActivity：用户从系统授权页返回时回调会触发，
    // 这时再判一次权限，状态栏就会立刻从「未授权」变成「已授权」
    val settingsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { granted = PhoneStoragePermission.isGranted(context) }

    val runtimeLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted = PhoneStoragePermission.isGranted(context) }

    var showClearMovieDialog by remember { mutableStateOf(false) }
    var showClearAudioDialog by remember { mutableStateOf(false) }
    var showClearHistoryDialog by remember { mutableStateOf(false) }

    PhoneSettingsPageScaffold(stringResource(R.string.cat_tools), onBack) {
        SettingsSectionTitle(stringResource(R.string.perm_section_storage))
        SettingsCard {
            SettingsEntryRow(
                title = stringResource(R.string.perm_title_all_files),
                subtitle = stringResource(R.string.perm_desc_all_files),
                value = stringResource(
                    if (granted) R.string.perm_status_granted else R.string.perm_status_denied
                ),
                onClick = {
                    if (granted) {
                        // 已授权时点一下仍然跳系统页，方便随时收回权限（不做拦截，避免「点了没反应」）
                        granted = PhoneStoragePermission.isGranted(context)
                    }
                    if (PhoneStoragePermission.needsAllFilesAccess) {
                        settingsLauncher.launch(PhoneStoragePermission.allFilesAccessIntent(context))
                    } else {
                        runtimeLauncher.launch(PhoneStoragePermission.runtimePermission)
                    }
                },
                trailingChevron = true,
            )
        }

        SettingsSectionTitle(stringResource(R.string.tool_section_database))
        SettingsCard {
            SettingsEntryRow(
                title = stringResource(R.string.btn_clear_movie_db),
                subtitle = stringResource(R.string.msg_clear_movie_db_confirm),
                onClick = { showClearMovieDialog = true },
            )
            SettingsDivider()
            SettingsEntryRow(
                title = stringResource(R.string.btn_clear_audio_db),
                subtitle = stringResource(R.string.msg_clear_audio_db_confirm),
                onClick = { showClearAudioDialog = true },
            )
            SettingsDivider()
            SettingsEntryRow(
                title = stringResource(R.string.phone_setting_clear_history),
                subtitle = stringResource(R.string.phone_setting_clear_history_confirm),
                onClick = { showClearHistoryDialog = true },
            )
        }
    }

    if (showClearMovieDialog) {
        SettingsConfirmDialog(
            title = stringResource(R.string.btn_clear_movie_db),
            message = stringResource(R.string.msg_clear_movie_db_confirm),
            confirmText = stringResource(R.string.phone_action_clear),
            onConfirm = movieViewModel::clearMediaLibrary,
            onDismiss = { showClearMovieDialog = false },
        )
    }

    if (showClearAudioDialog) {
        SettingsConfirmDialog(
            title = stringResource(R.string.btn_clear_audio_db),
            message = stringResource(R.string.msg_clear_audio_db_confirm),
            confirmText = stringResource(R.string.phone_action_clear),
            onConfirm = audioViewModel::clearLibrary,
            onDismiss = { showClearAudioDialog = false },
        )
    }

    if (showClearHistoryDialog) {
        SettingsConfirmDialog(
            title = stringResource(R.string.phone_setting_clear_history),
            message = stringResource(R.string.phone_setting_clear_history_confirm),
            confirmText = stringResource(R.string.phone_action_clear),
            onConfirm = mediaHistoryViewModel::clearAll,
            onDismiss = { showClearHistoryDialog = false },
        )
    }
}

@Composable
internal fun PhoneAboutSettingsPage(onBack: () -> Unit) {
    val context = LocalContext.current

    PhoneSettingsPageScaffold(stringResource(R.string.cat_about), onBack) {
        SettingsCard {
            SettingsEntryRow(
                title = stringResource(R.string.phone_setting_version),
                value = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                onClick = { },
            )
            SettingsDivider()
            SettingsEntryRow(
                title = stringResource(R.string.ui_label_author),
                value = "@MZHSY",
                onClick = { },
            )
        }

        SettingsSectionTitle(stringResource(R.string.ui_label_official_website))
        SettingsCard {
            AboutLinkRow(
                title = stringResource(R.string.ui_label_official_website),
                url = OFFICIAL_WEBSITE,
                context = context,
            )
            SettingsDivider()
            AboutLinkRow(
                title = stringResource(R.string.ui_label_github),
                url = GITHUB_URL,
                context = context,
            )
            SettingsDivider()
            AboutLinkRow(
                title = stringResource(R.string.ui_label_gitee),
                url = GITEE_URL,
                context = context,
            )
        }

        Text(
            text = stringResource(R.string.ui_label_copyright),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp),
        )
        Text(
            text = stringResource(R.string.ui_label_disclaimer),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun AboutLinkRow(title: String, url: String, context: Context) {
    ListItem(
        supportingContent = { Text(url) },
        leadingContent = {
            Icon(
                imageVector = Icons.Filled.Share,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
            )
        },
        modifier = Modifier.fillMaxWidth(),
        onClick = {
            // 没装浏览器会抛 ActivityNotFoundException，这里静默吞掉即可：
            // 地址本身已经显示在行里，用户还能手动复制
            runCatching {
                context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
            }
        },
    ) {
        Text(title)
    }
}

@Composable
private fun tmdbLangOptions(): List<SettingsOption<String>> = listOf(
    "" to stringResource(R.string.lang_auto_system),
    "zh-CN" to "简体中文",
    "zh-TW" to "繁體中文",
    "en-US" to "English",
    "ja-JP" to "日本語",
    "ko-KR" to "한국어",
).map { (value, label) -> SettingsOption(value, label) }

/** 刮削首选数据源：两个源都在用，这里只选谁先谁后 */
@Composable
private fun scrapeSourceOptions(): List<SettingsOption<String>> = listOf(
    ScrapeSourcePolicy.DOUBAN to stringResource(R.string.setting_scrape_source_douban),
    ScrapeSourcePolicy.TMDB to stringResource(R.string.setting_scrape_source_tmdb),
).map { (value, label) -> SettingsOption(value, label) }

private const val OFFICIAL_WEBSITE = "https://mzdkplayer.pages.dev/"
private const val GITHUB_URL = "https://github.com/mzhsy1/MzDKPlayer"
private const val GITEE_URL = "https://gitee.com/mzhsy/MzDKPlayer"
