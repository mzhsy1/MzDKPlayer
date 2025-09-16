package org.mz.mzdkplayer.danmaku
data class DanmakuResponse(
    val chatServer: String,
    val chatId: Long,
    val maxLimit: Int,
    val state: Int,
    val realName: Int,
    val source: String,
    val data: List<DanmakuData>
)

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
            require(parts.size >= 8) { "Invalid danmaku p attribute: $p" }
            return DanmakuData(
                time = parts[0].toFloat(),
                mode = parts[1].toInt(),
                size = parts[2].toInt(),
                color = parts[3].toInt(),
                timestamp = parts[4].toLong(),
                pool = parts[5].toInt(),
                userIdHash = parts[6],
                rowId = parts[7].toLong(),
                content = text
            )
        }
    }
}