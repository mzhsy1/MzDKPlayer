package org.mz.mzdkplayer.tool

import org.mz.mzdkplayer.data.model.PhoneThemeMode

/**
 * 手机端主题的纯逻辑（只依赖 JDK，可直接单测）。
 *
 * UI（app 的 `ui/phone/PhoneTheme`）只负责把 [PhoneThemeMode] 变成实际配色，
 * 「存储里的字符串 → 枚举」「枚举 + 系统深色 → 是否用深色」这两步的判定都放这里。
 */
object PhoneThemeLogic {

    /**
     * 把 [org.mz.mzdkplayer.data.repository.SettingsRepository.phoneThemeMode] 存下来的
     * 字符串还原成枚举。
     *
     * 非法值（历史脏数据、手改 prefs）一律软降级成 [PhoneThemeMode.SYSTEM]，
     * 不能让一个坏字符串把主题搞崩。
     */
    fun themeModeFromStorage(value: String?): PhoneThemeMode {
        val normalized = value?.trim().orEmpty()
        return PhoneThemeMode.entries.firstOrNull { it.name.equals(normalized, ignoreCase = true) }
            ?: PhoneThemeMode.SYSTEM
    }

    /** 结合系统深色开关，算出最终是否使用深色配色 */
    fun resolveDarkTheme(mode: PhoneThemeMode, systemInDarkTheme: Boolean): Boolean =
        when (mode) {
            PhoneThemeMode.SYSTEM -> systemInDarkTheme
            PhoneThemeMode.LIGHT -> false
            PhoneThemeMode.DARK -> true
        }
}
