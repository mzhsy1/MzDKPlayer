package org.mz.mzdkplayer.tool

import java.util.Locale
object Tools {
    fun extractFileExtension(fileName: String?): String {
        if (fileName == null || fileName.isEmpty()) {
            return ""
        }
        // 处理可能以点结尾的文件名或隐藏文件（无扩展名）
        val lastDotIndex = fileName.lastIndexOf('.')
        if (lastDotIndex > 0 && lastDotIndex < fileName.length - 1) {
            // 确保点不在字符串的开头（隐藏文件）且不是最后一个字符
            return fileName.substring(lastDotIndex + 1).lowercase(Locale.getDefault())
        }
        return "" // 没有扩展名

    }
    fun containsVideoFormat(input: String): Boolean {
        val videoFormats = listOf("MP4", "MKV", "M2TS", "3GP", "AVI", "MOV", "TS", "FLV")
        return videoFormats.any { format ->
            input.contains(format, ignoreCase = true)
        }
    }
}