package org.mz.mzdkplayer.ui.phone.model

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.ui.graphics.vector.ImageVector
import org.mz.mzdkplayer.R
import org.mz.mzdkplayer.ui.phone.PhoneIcons

/**
 * 手机端设置页的二级分类（第八阶段）。
 *
 * 电视端设置是「左侧分类栏 + 右侧内容」的两栏布局，搬到手机上会挤成两条窄柱，
 * 所以改成手机通用的形态：**设置首页列出分类入口，点进去是独立的一页**。
 *
 * 这里只声明「有哪些分类、叫什么、什么图标」；每一页的实际内容在
 * `screen/PhoneSettingsSections.kt` 与 `screen/PhoneSettingsLibrarySections.kt`。
 *
 * [routeValue] 同时是路由参数（`phone/settings/{category}`），一旦发布就不能改，
 * 否则从通知 / 深链进来的旧地址会落到空页；枚举名与它保持一致是为了少一层映射。
 */
internal enum class PhoneSettingCategory(
    val routeValue: String,
    @param:StringRes val titleRes: Int,
    val icon: ImageVector,
) {
    /** 播放内核、画面比例、完成动作、快进快退时长，以及手机端的触屏手势 */
    PLAYBACK("playback", R.string.cat_playback, PhoneIcons.Movie),

    /** 音频语言、音频直通、Exo 音频解码模式 */
    AUDIO("audio", R.string.cat_audio, PhoneIcons.Music),

    /** 字幕语言、时间轴、字号 / 字色 / 背景色 / 底距、第三方字体 */
    SUBTITLE("subtitle", R.string.cat_subtitle, PhoneIcons.Subtitles),

    /** 首页区块显示与播放页网速（纯手机端的界面项，电视端没有对应分类） */
    INTERFACE("interface", R.string.phone_settings_cat_interface, Icons.Filled.Home),

    /** 刮削总开关与来源、本地 NFO、TMDB 地址与语言、递归层级 */
    LIBRARY("library", R.string.cat_metadata, PhoneIcons.Scrape),

    /** 权限状态、清理影视 / 音乐库、清空播放历史 */
    TOOLS("tools", R.string.cat_tools, Icons.Filled.Build),

    /** 版本、作者、官网与仓库地址、免责声明 */
    ABOUT("about", R.string.cat_about, Icons.Filled.Info);

    companion object {
        /** 把路由参数还原成分类；未知值返回 null，由导航层决定退回设置首页 */
        fun fromRouteValue(value: String?): PhoneSettingCategory? =
            entries.firstOrNull { it.routeValue == value }
    }
}
