// 这些类和函数通常会放在独立的文件中，例如 LrcParser.kt
package org.mz.mzdkplayer.ui.audioplayer.components
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * 表示一行歌词及其对应的时间戳
 */
data class LyricEntry(val time: Duration, val text: String)

/**
 * 简单的 LRC 正则表达式匹配器
 */
private val LRC_LINE_REGEX = """\[([0-9:.]+)](.*)""".toRegex()

/**
 * 将时间字符串 (mm:ss.SS 或 mm:ss) 转换为 Duration
 */
fun parseTime(timeStr: String): Duration? {
    return try {
        val parts = timeStr.split(":")
        if (parts.size >= 2) {
            val minutes = parts[0].toDoubleOrNull() ?: return null
            val secondsAndMillis = parts[1].split(".")
            val seconds = secondsAndMillis[0].toDoubleOrNull() ?: return null
            val millis = if (secondsAndMillis.size > 1) {
                // 处理毫秒，假设是两位数或三位数
                val milliPart = secondsAndMillis[1]
                when (milliPart.length) {
                    1 -> (milliPart.toDoubleOrNull() ?: 0.0) * 100 // 假设是十分位
                    2 -> (milliPart.toDoubleOrNull() ?: 0.0) * 10  // 假设是百分位
                    else -> milliPart.toDoubleOrNull() ?: 0.0       // 默认认为是毫秒
                }
            } else 0.0

            (minutes * 60.0 + seconds).seconds + millis.milliseconds
        } else {
            null
        }
    } catch (e: NumberFormatException) {
        null
    }
}

/**
 * 解析整个 LRC 内容字符串
 *
 * @param lrcContent 完整的 LRC 文件内容
 * @return 按时间排序的歌词条目列表
 */
fun parseLrc(lrcContent: String?): List<LyricEntry> {
    if (lrcContent.isNullOrBlank()) return emptyList()

    val entries = mutableListOf<LyricEntry>()
    val lines = lrcContent.lines()

    for (line in lines) {
        val matchResult = LRC_LINE_REGEX.find(line.trim())
        if (matchResult != null) {
            val (timeStr, lyricText) = matchResult.destructured
            val time = parseTime(timeStr)
            if (time != null) {
                entries.add(LyricEntry(time, lyricText.trim()))
            }
        }
        // 忽略无法识别的行或元信息行 ([ar:], [ti:] 等)
    }

    return entries.sortedBy { it.time } // 确保按时间顺序排列
}



