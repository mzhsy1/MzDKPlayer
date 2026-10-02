package org.mz.mzdkplayer.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import org.mz.mzdkplayer.R
import org.mz.mzdkplayer.tool.logic.ScrapeSourcePolicy

/**
 * 「设置里存的原始值 → 界面文案」的统一映射。
 *
 * 第八阶段从 tv 源集搬到 main：手机端设置页补齐了内核选择、音频直通、字幕外观、
 * TMDB 语言这些条目，用到的正是同一批文案 —— 两端各写一份迟早会漂移
 * （第七阶段的 `SettingOptionText.kt` 也是同样的搬家理由）。
 *
 * 这些函数都是 `@Composable`（要读 `stringResource`），所以调用点必须在 Compose 作用域内；
 * 「存的是数值、要算一下」的纯逻辑不在这里（例如字幕偏移的步进在 `:core` 的 `SubtitleOffsetLogic`）。
 */

/** 音频 / 字幕首选语言：`""` = 自动 */
@Composable
fun formatLang(code: String): String = when (code) {
    "zh" -> stringResource(R.string.ui_label_chinese_language)
    "en" -> stringResource(R.string.lang_english)
    else -> stringResource(R.string.lang_auto)
}

/** 字幕背景色：把 ARGB 值映射回「黑色 (50%)」这类文案 */
@Composable
fun parseBgColorName(color: Long): String = when (color) {
    0x80000000 -> stringResource(R.string.color_black_50)
    0x80FFFFFF -> stringResource(R.string.color_white_50)
    0x80FFFF00 -> stringResource(R.string.color_yellow_50)
    0x00000000L -> stringResource(R.string.color_transparent)
    else -> stringResource(R.string.ui_label_custom)
}

/** 第三方字幕字体：只显示文件名，路径本身对用户没有意义 */
@Composable
fun formatSubFontName(path: String): String {
    if (path.isBlank()) {
        return stringResource(R.string.font_default)
    }
    return path.substringAfterLast('/').ifBlank { path }
}

/** Exo 音频解码模式：0 纯硬解 / 1 硬解优先 / 2 软解优先 */
@Composable
fun formatAudioDecodeMode(mode: Int): String = when (mode) {
    0 -> stringResource(R.string.setting_audio_decode_pure_hw)
    1 -> stringResource(R.string.setting_audio_decode_hw_priority)
    2 -> stringResource(R.string.setting_audio_decode_sw_priority)
    else -> stringResource(R.string.ui_label_unknown)
}

/** ISO 蓝光播放行为：0 尝试显示菜单 / 1 直接播放正片 */
@Composable
fun formatIsoPlaybackMode(mode: Int): String = when (mode) {
    0 -> stringResource(R.string.iso_playback_mode_default)
    1 -> stringResource(R.string.iso_playback_mode_main_movie)
    else -> stringResource(R.string.ui_label_unknown)
}

/** 批量扫描的递归层级：0～5 层 */
@Composable
fun formatRecursiveScanLevel(level: Int): String = when (level) {
    0 -> stringResource(R.string.recursive_scan_level_0)
    1 -> stringResource(R.string.recursive_scan_level_1)
    2 -> stringResource(R.string.recursive_scan_level_2)
    3 -> stringResource(R.string.recursive_scan_level_3)
    4 -> stringResource(R.string.recursive_scan_level_4)
    5 -> stringResource(R.string.recursive_scan_level_5)
    else -> "Level $level"
}

/** TMDB 搜索 / 详情语言：`""` = 跟随系统，其余是固定的几个语言标签 */
@Composable
fun formatTmdbLang(code: String): String = when (code) {
    "" -> stringResource(R.string.lang_auto_system)
    "zh-CN" -> "简体中文"
    "zh-TW" -> "繁體中文"
    "en-US" -> "English"
    "ja-JP" -> "日本語"
    "ko-KR" -> "한국어"
    else -> code
}

/** 刮削首选数据源：`douban` / `tmdb`（两个源都在用，只是谁先谁后），见 `ScrapeSourcePolicy` */
@Composable
fun formatScrapeSource(code: String): String = when (code) {
    ScrapeSourcePolicy.TMDB -> stringResource(R.string.setting_scrape_source_tmdb)
    else -> stringResource(R.string.setting_scrape_source_douban)
}

/** App 语言：`""` = 跟随系统 */
@Composable
fun formatAppLang(code: String): String = when (code) {
    "" -> stringResource(R.string.lang_auto_system)
    "zh" -> stringResource(R.string.ui_label_chinese_language)
    "en" -> stringResource(R.string.lang_english)
    "ja" -> stringResource(R.string.ui_label_japanese_language)
    else -> stringResource(R.string.lang_auto)
}
