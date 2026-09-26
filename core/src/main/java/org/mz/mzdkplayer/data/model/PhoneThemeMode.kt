package org.mz.mzdkplayer.data.model

/**
 * 手机端主题模式。
 *
 * 持久化的是**枚举名**（见 [org.mz.mzdkplayer.data.repository.SettingsRepository.phoneThemeMode]），
 * 不用序号：以后往枚举中间插值不会把老用户的设置读错。
 */
enum class PhoneThemeMode {
    /** 跟随系统深色设置 */
    SYSTEM,

    /** 始终浅色 */
    LIGHT,

    /** 始终深色 */
    DARK
}
