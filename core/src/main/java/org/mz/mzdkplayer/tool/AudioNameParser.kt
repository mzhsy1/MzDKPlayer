package org.mz.mzdkplayer.tool

data class AudioFileNameMetadata(
    val title: String,
    val artist: String = "未知艺术家",
    val album: String = "未知专辑"
)

object AudioNameParser {

    /**
     * 解析文件名
     * 常见格式：
     * 1. 歌手 - 歌名 - 专辑.mp3
     * 2. 歌手 - 歌名.mp3
     * 3. 歌名.mp3
     */
    fun parse(originalFileName: String): AudioFileNameMetadata {
        // 1. 去掉文件后缀 (如 .mp3, .wav, .flac)
        val nameWithoutExt = originalFileName.substringBeforeLast(".")

        // 2. 定义分隔符：支持 " - ", "-", "_", " " (按优先级尝试分割)
        // 这里主要以 " - " 或 "-" 为主，避免歌名里带空格被误切
        // 你可以根据自己库的习惯调整正则
        val separators = listOf(" - ", "-", "_")

        var parts = emptyList<String>()

        // 尝试找到合适的分隔符
        for (sep in separators) {
            if (nameWithoutExt.contains(sep)) {
                parts = nameWithoutExt.split(sep).map { it.trim() }
                break
            }
        }

        return when {
            // 情况 A: 歌名 - 歌手 - 专辑 (3段及以上，取前3段)
            parts.size >= 3 -> {
                AudioFileNameMetadata(
                    title = parts[0],
                    artist = parts[1],
                    album = parts[2]
                )
            }
            // 情况 B: 歌名 - 歌手 (2段)
            parts.size == 2 -> {
                AudioFileNameMetadata(
                    title = parts[0],
                    artist = parts[1],
                    album = "未知专辑"
                )
            }
            // 情况 C: 只有歌名 (或分割失败)
            else -> {
                AudioFileNameMetadata(
                    title = nameWithoutExt,
                    artist = "未知艺术家",
                    album = "未知专辑"
                )
            }
        }
    }
}