package org.mz.mzdkplayer.data.model

import android.R
import kotlin.math.abs

/**
 * 弹幕显示区域枚举
 */
enum class DanmakuScreenRatio(val ratioValue: Float, private val strRes: Int) {
    HALF(0.5f, R.string.untitled), // 实际应使用应用内的字符串资源
    QUARTER(0.25f, R.string.untitled),
    ONE_SIXTH(0.166f, R.string.untitled),
    ONE_EIGHTH(0.125f, R.string.untitled),
    ONE_TENTH(0.1f, R.string.untitled),
    ONE_TWELFTH(0.083f, R.string.untitled),
    FULL(1.0f, R.string.untitled);

    companion object {
        // 显示名称列表，用于UI显示
        val displayNames = listOf("1/2", "1/4", "1/6", "1/8", "1/10", "1/12", "全屏")

        // 根据显示名称获取枚举值
        fun fromDisplayName(displayName: String): DanmakuScreenRatio {
            return when (displayName) {
                "1/2" -> HALF
                "1/4" -> QUARTER
                "1/6" -> ONE_SIXTH
                "1/8" -> ONE_EIGHTH
                "1/10" -> ONE_TENTH
                "1/12" -> ONE_TWELFTH
                "全屏" -> FULL
                else -> ONE_TWELFTH // 默认值
            }
        }

        // 根据比例值获取枚举值
        fun fromRatioValue(ratioValue: Float): DanmakuScreenRatio {
            return entries.find { abs(it.ratioValue - ratioValue) < 0.001f } ?: ONE_TWELFTH
        }
    }

    fun getDisplayName(): String {
        val index = entries.indexOf(this)
        return displayNames.getOrNull(index) ?: "1/12"
    }
}