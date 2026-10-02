package org.mz.mzdkplayer.ui.phone.screen

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.mz.mzdkplayer.R
import org.mz.mzdkplayer.player.core.MzAspectRatio
import org.mz.mzdkplayer.tool.logic.PhoneSettingsLogic
import org.mz.mzdkplayer.tool.logic.SubtitleOffsetLogic
import org.mz.mzdkplayer.ui.common.VIDEO_FINISH_ACTION_COUNT
import org.mz.mzdkplayer.ui.common.aspectRatioFromName
import org.mz.mzdkplayer.ui.common.formatAspectRatio
import org.mz.mzdkplayer.ui.common.formatAudioDecodeMode
import org.mz.mzdkplayer.ui.common.formatIsoPlaybackMode
import org.mz.mzdkplayer.ui.common.formatLang
import org.mz.mzdkplayer.ui.common.formatSubFontName
import org.mz.mzdkplayer.ui.common.formatVideoFinishAction
import org.mz.mzdkplayer.ui.common.parseBgColorName
import org.mz.mzdkplayer.viewmodel.SettingsViewModel
import java.io.File

/**
 * 手机端设置的「播放 / 音频 / 字幕 / 界面」四个分类页（第八阶段）。
 *
 * 内容口径与电视端 `SettingsScreen` 的对应分类**完全一致**（都写同一份 `SettingsRepository`），
 * 差异只在交互：
 * - 枚举型选项（内核、语言、画面比例、完成动作、解码模式……）改成底部弹出的单选列表，
 *   电视端是「确定键循环切换」——手机上那么点要按五六次才能选中最后一个；
 * - 数值型选项（快进时长、字号、底距、字幕偏移）用带 ± 的按钮，步进按手指点得动来定；
 * - 第三方字幕字体用系统的文件选择器（SAF）挑一个文件，**复制进应用私有目录**后再存路径：
 *   播放页的 `Font(File)` 只认真实文件路径，直接存 `content://` 是读不出来的。
 *
 * 「刮削与媒体库 / 工具 / 关于」三页在 `PhoneSettingsLibrarySections.kt`。
 */

@Composable
internal fun PhonePlaybackSettingsPage(
    settingsViewModel: SettingsViewModel,
    onBack: () -> Unit,
) {
    val state by settingsViewModel.uiState.collectAsState()

    PhoneSettingsPageScaffold(stringResource(R.string.cat_playback), onBack) {
        SettingsSectionTitle(stringResource(R.string.phone_settings_group_playback_core))
        SettingsCard {
            SettingsOptionsRow(
                title = stringResource(R.string.setting_default_player),
                value = stringResource(
                    if (state.defaultPlayer == "vlc") {
                        R.string.setting_default_player_vlc
                    } else {
                        R.string.setting_default_player_exo
                    }
                ),
                selected = state.defaultPlayer,
                options = listOf(
                    SettingsOption("exo", stringResource(R.string.setting_default_player_exo)),
                    SettingsOption("vlc", stringResource(R.string.setting_default_player_vlc)),
                ),
                onSelect = settingsViewModel::setDefaultPlayer,
            )
            SettingsDivider()
            SettingsOptionsRow(
                title = stringResource(R.string.setting_iso_playback_mode),
                subtitle = stringResource(R.string.setting_iso_playback_mode_sub),
                value = formatIsoPlaybackMode(state.isoPlaybackMode),
                selected = state.isoPlaybackMode,
                options = listOf(
                    SettingsOption(0, stringResource(R.string.iso_playback_mode_default)),
                    SettingsOption(1, stringResource(R.string.iso_playback_mode_main_movie)),
                ),
                onSelect = settingsViewModel::setIsoPlaybackMode,
            )
        }

        SettingsSectionTitle(stringResource(R.string.phone_settings_group_display))
        SettingsCard {
            SettingsOptionsRow(
                title = stringResource(R.string.setting_global_video_ratio),
                subtitle = stringResource(R.string.setting_global_video_ratio_sub),
                value = formatAspectRatio(aspectRatioFromName(state.globalVideoRatio)),
                selected = aspectRatioFromName(state.globalVideoRatio),
                options = MzAspectRatio.entries.map { SettingsOption(it, formatAspectRatio(it)) },
                onSelect = { settingsViewModel.setGlobalVideoRatio(it.name) },
            )
            SettingsDivider()
            SettingsSwitchRow(
                title = stringResource(R.string.setting_lock_video_ratio),
                checked = state.lockVideoRatio,
                onCheckedChange = settingsViewModel::toggleLockVideoRatio,
            )
        }

        SettingsSectionTitle(stringResource(R.string.phone_settings_group_behavior))
        SettingsCard {
            SettingsOptionsRow(
                title = stringResource(R.string.setting_video_finish_action),
                value = formatVideoFinishAction(state.videoFinishAction),
                selected = state.videoFinishAction,
                options = (0 until VIDEO_FINISH_ACTION_COUNT).map {
                    SettingsOption(it, formatVideoFinishAction(it))
                },
                onSelect = settingsViewModel::setVideoFinishAction,
            )
            SettingsDivider()
            SettingsNumberRow(
                title = stringResource(R.string.setting_ff_duration),
                // 手机上一格 5 秒：遥控器可以连着按，手指不行
                value = state.ffDuration,
                onValueChange = settingsViewModel::setFFDuration,
                step = 5,
                minValue = 5,
                maxValue = 600,
                valueText = stringResource(R.string.phone_setting_seconds_format, state.ffDuration),
            )
            SettingsDivider()
            SettingsNumberRow(
                title = stringResource(R.string.setting_rw_duration),
                value = state.rwDuration,
                onValueChange = settingsViewModel::setRWDuration,
                step = 5,
                minValue = 5,
                maxValue = 600,
                valueText = stringResource(R.string.phone_setting_seconds_format, state.rwDuration),
            )
            SettingsDivider()
            SettingsSwitchRow(
                title = stringResource(R.string.setting_remember_playback_pref),
                subtitle = stringResource(R.string.setting_remember_playback_pref_sub),
                checked = state.rememberPlaybackPreference,
                onCheckedChange = settingsViewModel::toggleRememberPlaybackPreference,
            )
        }

        SettingsSectionTitle(stringResource(R.string.phone_settings_group_gesture))
        SettingsCard {
            SettingsSwitchRow(
                title = stringResource(R.string.phone_setting_gesture_brightness),
                checked = state.phoneGestureBrightness,
                onCheckedChange = settingsViewModel::togglePhoneGestureBrightness,
            )
            SettingsDivider()
            SettingsSwitchRow(
                title = stringResource(R.string.phone_setting_gesture_volume),
                checked = state.phoneGestureVolume,
                onCheckedChange = settingsViewModel::togglePhoneGestureVolume,
            )
            SettingsDivider()
            SettingsSwitchRow(
                title = stringResource(R.string.phone_setting_long_press_speed),
                subtitle = stringResource(R.string.phone_setting_long_press_speed_sub),
                checked = state.phoneLongPressSpeed,
                onCheckedChange = settingsViewModel::togglePhoneLongPressSpeed,
            )
            SettingsDivider()
            SettingsOptionsRow(
                title = stringResource(R.string.phone_setting_long_press_speed_value),
                value = PhoneSettingsLogic.formatLongPressSpeed(state.phoneLongPressSpeedValue),
                selected = PhoneSettingsLogic.normalizeLongPressSpeed(state.phoneLongPressSpeedValue),
                enabled = state.phoneLongPressSpeed,
                options = PhoneSettingsLogic.LONG_PRESS_SPEED_OPTIONS.map {
                    SettingsOption(it, PhoneSettingsLogic.formatLongPressSpeed(it))
                },
                onSelect = settingsViewModel::setPhoneLongPressSpeedValue,
            )
            SettingsDivider()
            SettingsOptionsRow(
                title = stringResource(R.string.phone_setting_controls_hide_delay),
                subtitle = stringResource(R.string.phone_setting_controls_hide_delay_sub),
                value = stringResource(
                    R.string.phone_setting_seconds_format,
                    state.phoneControlsHideSeconds,
                ),
                selected = PhoneSettingsLogic.normalizeControlsHideSeconds(state.phoneControlsHideSeconds),
                options = PhoneSettingsLogic.CONTROLS_HIDE_OPTIONS.map {
                    SettingsOption(it, stringResource(R.string.phone_setting_seconds_format, it))
                },
                onSelect = settingsViewModel::setPhoneControlsHideSeconds,
            )
        }
    }
}

@Composable
internal fun PhoneAudioSettingsPage(
    settingsViewModel: SettingsViewModel,
    onBack: () -> Unit,
) {
    val state by settingsViewModel.uiState.collectAsState()

    PhoneSettingsPageScaffold(stringResource(R.string.cat_audio), onBack) {
        SettingsCard {
            SettingsOptionsRow(
                title = stringResource(R.string.setting_audio_lang),
                value = formatLang(state.audioLang),
                selected = state.audioLang,
                options = languageOptions(),
                onSelect = settingsViewModel::setAudioLanguage,
            )
            SettingsDivider()
            SettingsSwitchRow(
                title = stringResource(R.string.setting_passthrough),
                subtitle = stringResource(R.string.setting_passthrough_sub),
                checked = state.enablePassthrough,
                onCheckedChange = settingsViewModel::togglePassthrough,
            )
            SettingsDivider()
            SettingsOptionsRow(
                title = stringResource(R.string.setting_exo_audio_decode_mode),
                value = formatAudioDecodeMode(state.exoAudioDecodeMode),
                selected = state.exoAudioDecodeMode,
                options = listOf(0, 1, 2).map { SettingsOption(it, formatAudioDecodeMode(it)) },
                onSelect = settingsViewModel::setExoAudioDecodeMode,
            )
        }
    }
}

@Composable
internal fun PhoneSubtitleSettingsPage(
    settingsViewModel: SettingsViewModel,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val state by settingsViewModel.uiState.collectAsState()

    // 导入失败时给一行说明：从 SAF 拿到的流读不出来（文件损坏 / 权限被回收）是常有的事，
    // 静默失败会让人以为「点了没反应」
    var importFailed by remember { mutableStateOf(false) }

    val pickFont = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val path = installCustomSubtitleFont(context, uri)
            importFailed = path == null
            if (path != null) settingsViewModel.setSubFontPath(path)
        }
    }

    PhoneSettingsPageScaffold(stringResource(R.string.cat_subtitle), onBack) {
        SettingsCard {
            SettingsOptionsRow(
                title = stringResource(R.string.setting_sub_lang),
                value = formatLang(state.subLang),
                selected = state.subLang,
                options = languageOptions(),
                onSelect = settingsViewModel::setSubLanguage,
            )
            SettingsDivider()
            SettingsNumberRow(
                title = stringResource(R.string.setting_subtitle_delay),
                subtitle = stringResource(R.string.ui_label_subtitle_delay_text_only),
                value = state.subtitleDelayMs,
                onValueChange = settingsViewModel::setSubtitleDelayMs,
                step = SubtitleOffsetLogic.STEP_MS,
                minValue = -SubtitleOffsetLogic.MAX_MS,
                maxValue = SubtitleOffsetLogic.MAX_MS,
                valueText = SubtitleOffsetLogic.formatSeconds(state.subtitleDelayMs) +
                        stringResource(R.string.unit_seconds),
            )
            SettingsDivider()
            SettingsNumberRow(
                title = stringResource(R.string.setting_font_size),
                // 一格 2sp：16～100 全铺开要点 40 多次，手指受不了
                value = state.subFontSize.toInt(),
                onValueChange = { settingsViewModel.setSubFontSize(it.toFloat()) },
                step = 2,
                minValue = 16,
                maxValue = 100,
            )
            SettingsDivider()
            SettingsOptionsRow(
                title = stringResource(R.string.setting_font_color),
                value = stringResource(
                    if (state.subColor == 0xFFFFFFFFL) R.string.color_white else R.string.color_yellow
                ),
                selected = state.subColor,
                options = listOf(
                    SettingsOption(0xFFFFFFFFL, stringResource(R.string.color_white)),
                    SettingsOption(0xFFFFFF00L, stringResource(R.string.color_yellow)),
                ),
                onSelect = settingsViewModel::setSubColor,
            )
            SettingsDivider()
            SettingsOptionsRow(
                title = stringResource(R.string.setting_bg_color),
                value = parseBgColorName(state.subBgColor),
                selected = state.subBgColor,
                options = listOf(0x80000000L, 0x80FFFFFFL, 0x80FFFF00L, 0x00000000L).map {
                    SettingsOption(it, parseBgColorName(it))
                },
                onSelect = settingsViewModel::setSubBgColor,
            )
            SettingsDivider()
            SettingsNumberRow(
                title = stringResource(R.string.setting_bottom_padding),
                value = state.subBottomPadding.toInt(),
                onValueChange = { settingsViewModel.setSubBottomPadding(it.toFloat()) },
                step = 10,
                minValue = -100,
                maxValue = 200,
            )
            SettingsDivider()
            SettingsSwitchRow(
                title = stringResource(R.string.setting_force_pgs_center),
                subtitle = stringResource(R.string.setting_force_pgs_center_sub),
                checked = state.forcePgsCenter,
                onCheckedChange = settingsViewModel::togglePgsCenter,
            )
            SettingsDivider()
            SettingsSwitchRow(
                title = stringResource(R.string.setting_auto_load_subtitle),
                subtitle = stringResource(R.string.setting_auto_load_subtitle_sub),
                checked = state.autoLoadSubtitle,
                onCheckedChange = settingsViewModel::toggleAutoLoadSubtitle,
            )
        }

        SettingsSectionTitle(stringResource(R.string.setting_sub_font))
        SettingsCard {
            SettingsEntryRow(
                title = stringResource(R.string.phone_setting_sub_font_pick),
                subtitle = stringResource(R.string.setting_sub_font_sub),
                value = formatSubFontName(state.subFontPath),
                onClick = { pickFont.launch(arrayOf("*/*")) },
            )
            if (state.subFontPath.isNotBlank()) {
                SettingsDivider()
                SettingsEntryRow(
                    title = stringResource(R.string.phone_setting_sub_font_reset),
                    onClick = {
                        importFailed = false
                        settingsViewModel.setSubFontPath("")
                    },
                )
            }
            if (importFailed) {
                Text(
                    text = stringResource(R.string.phone_setting_sub_font_failed),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
                )
            }
        }
    }
}

@Composable
internal fun PhoneInterfaceSettingsPage(
    settingsViewModel: SettingsViewModel,
    onBack: () -> Unit,
) {
    val state by settingsViewModel.uiState.collectAsState()

    PhoneSettingsPageScaffold(stringResource(R.string.phone_settings_cat_interface), onBack) {
        SettingsSectionTitle(stringResource(R.string.phone_settings_group_home))
        SettingsCard {
            SettingsSwitchRow(
                title = stringResource(R.string.phone_setting_home_recently_watched),
                subtitle = stringResource(R.string.phone_setting_home_sections_sub),
                checked = state.phoneHomeRecentlyWatched,
                onCheckedChange = settingsViewModel::togglePhoneHomeRecentlyWatched,
            )
            SettingsDivider()
            SettingsSwitchRow(
                title = stringResource(R.string.phone_setting_home_recently_added),
                checked = state.phoneHomeRecentlyAdded,
                onCheckedChange = settingsViewModel::togglePhoneHomeRecentlyAdded,
            )
            SettingsDivider()
            SettingsSwitchRow(
                title = stringResource(R.string.phone_setting_home_recently_visited),
                checked = state.phoneHomeRecentlyVisited,
                onCheckedChange = settingsViewModel::togglePhoneHomeRecentlyVisited,
            )
        }

        SettingsSectionTitle(stringResource(R.string.phone_settings_group_display))
        SettingsCard {
            SettingsSwitchRow(
                title = stringResource(R.string.setting_hide_net_speed),
                checked = state.hideNetworkSpeed,
                onCheckedChange = settingsViewModel::toggleHideNetWorkSpeed,
            )
        }
    }
}

/** 音频 / 字幕首选语言的三档：自动 / 中文 / 英文（与电视端同一组） */
@Composable
private fun languageOptions(): List<SettingsOption<String>> = listOf(
    SettingsOption("", stringResource(R.string.lang_auto)),
    SettingsOption("zh", stringResource(R.string.ui_label_chinese_language)),
    SettingsOption("en", stringResource(R.string.lang_english)),
)

/** 第三方字体在本应用私有目录下的文件名（不含扩展名；`Typeface` 会自己嗅探格式） */
private const val CUSTOM_SUBTITLE_FONT_NAME = "custom_subtitle_font"

/**
 * 把 SAF 选中的字体复制进应用私有目录，返回可直接给 `Font(File)` 用的绝对路径。
 *
 * 为什么要复制：`content://` 的读取授权只在本次会话内有效（重启后 URI 权限就没了），
 * 而播放页是另一个页面、可能几天后才用到这个字体；复制一份进 `filesDir` 最稳。
 * 每次导入会清掉旧文件，避免换字体后旧文件一直占着空间。
 */
private suspend fun installCustomSubtitleFont(context: Context, uri: Uri): String? =
    withContext(Dispatchers.IO) {
        runCatching {
            val fontDir = File(context.filesDir, "subtitle_fonts")
            if (!fontDir.exists() && !fontDir.mkdirs()) return@runCatching null
            fontDir.listFiles()?.forEach { it.delete() }

            val target = File(fontDir, CUSTOM_SUBTITLE_FONT_NAME)
            context.contentResolver.openInputStream(uri)?.use { input ->
                target.outputStream().use { output -> input.copyTo(output) }
            } ?: return@runCatching null

            if (target.length() <= 0L) null else target.absolutePath
        }.getOrNull()
    }


