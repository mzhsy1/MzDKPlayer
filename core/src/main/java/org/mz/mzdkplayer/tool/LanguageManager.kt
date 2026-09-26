package org.mz.mzdkplayer.tool

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import org.mz.mzdkplayer.data.repository.SettingsRepository

object LanguageManager {

    fun applyLanguage(context: Context) {
        val lang = SettingsRepository.appLanguage

        val locales = if (lang.isEmpty()) {
            LocaleListCompat.getEmptyLocaleList() // 跟随系统
        } else {
            LocaleListCompat.forLanguageTags(lang)
        }

        AppCompatDelegate.setApplicationLocales(locales)
    }

    fun setLanguage(context: Context, lang: String) {
        SettingsRepository.appLanguage = lang
        applyLanguage(context)
    }
}