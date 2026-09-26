package org.mz.mzdkplayer.data.repository

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import org.mz.mzdkplayer.data.model.DanmakuSettings

// 数据保存工具类
class DanmakuSettingsManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("danmaku_settings", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_IS_SWITCH_ENABLED = "is_switch_enabled"
        private const val KEY_SELECTED_RATIO = "selected_ratio"
        private const val KEY_FONT_SIZE = "font_size"
        private const val KEY_TRANSPARENCY = "transparency"
        private const val KEY_SELECTED_TYPES = "selected_types"
    }

    fun saveSettings(
        isSwitchEnabled: Boolean,
        selectedRatio: String,
        fontSize: Int,
        transparency: Int,
        selectedTypes: Set<String>
    ) {
        prefs.edit {
            putBoolean(KEY_IS_SWITCH_ENABLED, isSwitchEnabled)
            putString(KEY_SELECTED_RATIO, selectedRatio)
            putInt(KEY_FONT_SIZE, fontSize)
            putInt(KEY_TRANSPARENCY, transparency)
            putStringSet(KEY_SELECTED_TYPES, selectedTypes)
            apply()
        }
    }

    fun loadSettings(): DanmakuSettings {
        return DanmakuSettings(
            isSwitchEnabled = prefs.getBoolean(KEY_IS_SWITCH_ENABLED, false),
            selectedRatio = prefs.getString(KEY_SELECTED_RATIO, "1/12") ?: "1/12",
            fontSize = prefs.getInt(KEY_FONT_SIZE, 100),
            transparency = prefs.getInt(KEY_TRANSPARENCY, 100),
            selectedTypes = prefs.getStringSet(KEY_SELECTED_TYPES, emptySet())?.toSet()
                ?: emptySet()
        )
    }
}