package org.mz.mzdkplayer.data.model

import com.kuaishou.akdanmaku.data.DanmakuItemData

/**
 * 弹幕类型枚举
 */
enum class DanmakuType(val typeName: String, private val typeValue: Int) {
    ROLLING("滚动", DanmakuItemData.Companion.DANMAKU_MODE_ROLLING),      // 1
    BOTTOM("底部", DanmakuItemData.Companion.DANMAKU_MODE_CENTER_BOTTOM), // 4
    TOP("顶部", DanmakuItemData.Companion.DANMAKU_MODE_CENTER_TOP),       // 5
    COLORFUL("彩色", 0);                                       // 0 - 默认显示模式

    companion object {
        // 显示名称列表，用于UI显示
        val displayNames = listOf("滚动", "底部", "顶部", "彩色")

        // 根据显示名称获取枚举值
        fun fromDisplayName(displayName: String): DanmakuType {
            return when (displayName) {
                "滚动" -> ROLLING
                "底部" -> BOTTOM
                "顶部" -> TOP
                "彩色" -> COLORFUL
                else -> ROLLING // 默认值
            }
        }

        // 根据类型值获取枚举值
        fun fromTypeValue(typeValue: Int): DanmakuType {
            return entries.find { it.typeValue == typeValue } ?: ROLLING
        }

        // 获取与DanmakuItemData模式对应的类型
        fun fromDanmakuItemMode(mode: Int): DanmakuType {
            return entries.find { it.typeValue == mode } ?: ROLLING
        }
    }

    fun getDisplayName(): String {
        return typeName
    }

    fun getTypeValue(): Int {
        return typeValue
    }
}