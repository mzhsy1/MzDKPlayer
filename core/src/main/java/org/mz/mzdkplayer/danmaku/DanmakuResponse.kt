package org.mz.mzdkplayer.danmaku

import org.mz.mzdkplayer.tool.Tools.toSafeFloat
import org.mz.mzdkplayer.tool.Tools.toSafeInt
import org.mz.mzdkplayer.tool.Tools.toSafeLong

data class DanmakuResponse(
    val chatServer: String,
    val chatId: Long,
    val maxLimit: Int,
    val state: Int,
    val realName: Int,
    val source: String,
    val data: List<DanmakuData>
)

/**
 *
 *    // 出现时间（秒）
 *     // 弹幕模式（1=滚动，4=底部，5=顶部等）
 *     // 字号
 *     // 颜色（RGB 十进制）
 *     // 发送时间戳
 *     // 弹幕池（0=普通，1=字幕，2=特殊）
 *     // 用户ID哈希（旧版为数字，新版为字符串）
 *      // 弹幕在数据库中的行ID
 *     // 弹幕文本
 *
 */
data class DanmakuData(
    val time: Float,     // 出现时间（秒）
    val mode: Int,        // 弹幕模式（1=滚动，4=底部，5=顶部等）
    val size: Int,        // 字号
    val color: Int,       // 颜色（RGB 十进制）
    val timestamp: Long,  // 发送时间戳
    val pool: Int,        // 弹幕池（0=普通，1=字幕，2=特殊）
    val userIdHash: String, // 用户ID哈希（旧版为数字，新版为字符串）
    val rowId: Long,       // 弹幕在数据库中的行ID
    val content: String   // 弹幕文本
) {
    companion object {
        fun fromString(p: String, text: String): DanmakuData {
            val parts = p.split(",")
            // 至少需要 8 个字段，不足的补空字符串防止 IndexOutOfBoundsException
            val safeParts = List(8) { index ->
                if (index < parts.size) parts[index].trim() else ""
            }
            return DanmakuData(
                time = safeParts[0].toSafeFloat(),
                mode = safeParts[1].toSafeInt(1),       // 默认滚动弹幕
                size = safeParts[2].toSafeInt(25),      // 默认字号
                color = safeParts[3].toSafeInt(0xFFFFFF), // 默认白色
                timestamp = safeParts[4].toSafeLong(),
                pool = safeParts[5].toSafeInt(),
                userIdHash = safeParts[6],
                rowId = safeParts[7].toSafeLong(),
                content = text
            )
        }
    }
}