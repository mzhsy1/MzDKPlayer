package org.mz.mzdkplayer.logic.model

/**
 * 数据类，用于封装从音频文件中提取出的基本信息、歌词和专辑封面。
 *
 * @property title 标题
 * @property artist 艺术家
 * @property album 专辑
 * @property year 年份 (字符串形式)
 * @property track 曲目号 (字符串形式，可能包含总曲目数，如 "3/12")
 * @property genre 流派
 * @property durationSeconds 持续时间（秒）
 * @property lyrics 歌词
 * @property artworkData 专辑封面的原始字节数据 (ByteArray)。可以为 null。
 */
data class AudioInfo(
    val title: String? = "未知标题",
    val artist: String?="未知艺术家",
    val album: String?="未知专辑",
    val year: String?= "",
    val track: String?= "",
    val genre: String?= "",
    val durationSeconds: Long? =0L,
    val bit: Long? =0,
    val lyrics: String ?="",
    val sampleRate: String? ="",
    val bitsPerSample: Int?= 16,
    val artworkData: ByteArray? = byteArrayOf()
) {
    // 重写 equals 和 hashCode 以优雅地处理 ByteArray 的比较
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AudioInfo

        if (title != other.title) return false
        if (artist != other.artist) return false
        if (album != other.album) return false
        if (year != other.year) return false
        if (track != other.track) return false
        if (genre != other.genre) return false
        if (durationSeconds != other.durationSeconds) return false
        if (lyrics != other.lyrics) return false
        if (!artworkData.contentEquals(other.artworkData)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = title.hashCode()
        result = 31 * result + artist.hashCode()
        result = 31 * result + album.hashCode()
        result = 31 * result + (year?.hashCode() ?: 0)
        result = 31 * result + (track?.hashCode() ?: 0)
        result = 31 * result + (genre?.hashCode() ?: 0)
        result = 31 * result + durationSeconds.hashCode()
        result = 31 * result + lyrics.hashCode()
        result = 31 * result + artworkData.contentHashCode()
        return result
    }
}
