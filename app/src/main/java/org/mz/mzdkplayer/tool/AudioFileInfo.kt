package org.mz.mzdkplayer.tool // 请根据你的实际包名修改

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey

import org.jaudiotagger.tag.images.Artwork
import java.io.*

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
    val lyrics: String ?="",
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

// 定义默认值常量
private const val DEFAULT_TITLE = "未知标题"
private const val DEFAULT_ARTIST = "未知艺术家"
private const val DEFAULT_ALBUM = "未知专辑"

private const val TAG = "AudioMetaExtractor"
private const val TEMP_FILE_BUFFER_SIZE = 16 * 1024 // 16KB buffer for copying

/**
 * 从音频文件流中提取基本信息、内嵌歌词和专辑封面。
 *
 * 注意：此函数涉及文件 I/O 操作，应在后台线程调用 (例如 Coroutine Dispatcher.IO)。
 *
 * @param context Android Context，用于访问缓存目录创建临时文件。
 * @param inputStream 音频文件的输入流。调用方负责关闭此流。
 *                        如果为 null，则返回 null。
 * @param mimeType 文件的 MIME 类型，用于确定临时文件后缀。
 * @return 包含提取信息的 AudioInfo 对象，如果发生错误则返回 null。
 */
suspend fun extractAudioInfoAndLyricsFromStream(
    context: Context,
    inputStream: InputStream?,
    mimeType: String?
): AudioInfo? {
    return withContext(Dispatchers.IO) {
        if (inputStream == null) {
            Log.w(TAG, "Input stream is null.")
            return@withContext null
        }

        var tempAudioFile: File? = null
        try {
            // 1. 获取安全的文件后缀
            val safeSuffix = getSafeSuffixForMimeType(mimeType).also {
                Log.v(TAG, "Determined safe suffix for mimeType '$mimeType': '$it'")
            }

            // 2. 在应用的缓存目录下创建临时文件
            tempAudioFile = File.createTempFile("temp_audio_", safeSuffix, context.cacheDir)
            Log.d(TAG, "Created temporary file: ${tempAudioFile.absolutePath}")

            // 3. 直接将输入流完整地复制到临时文件
            FileOutputStream(tempAudioFile).use { outputStream ->
                val buffer = ByteArray(TEMP_FILE_BUFFER_SIZE)
                var bytesRead: Int
                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                }
            }
            Log.d(TAG, "Finished copying input stream to temporary file.")

            // 4. 使用 jaudiotagger 解析临时文件
            val audioFile = AudioFileIO.read(tempAudioFile)
            Log.d(TAG, "Parsed metadata from temporary file.")

            // --- 提取元数据 ---
            val tag = audioFile.tag
            val audioHeader = audioFile.audioHeader

            val title = tag?.getFirst(FieldKey.TITLE)?.takeIf { it.isNotBlank() } ?: DEFAULT_TITLE
            val artist =
                tag?.getFirst(FieldKey.ARTIST)?.takeIf { it.isNotBlank() } ?: DEFAULT_ARTIST
            val album = tag?.getFirst(FieldKey.ALBUM)?.takeIf { it.isNotBlank() } ?: DEFAULT_ALBUM
            val year = tag?.getFirst(FieldKey.YEAR)?.takeIf { it.isNotBlank() }
            val track = tag?.getFirst(FieldKey.TRACK)?.takeIf { it.isNotBlank() }
            val genre = tag?.getFirst(FieldKey.GENRE)?.takeIf { it.isNotBlank() }
            val durationSeconds = audioHeader?.trackLength?.toLong()

            var lyrics: String? = null
            if (tag != null) {
                lyrics = tag.getFirst(FieldKey.LYRICS)
                if (lyrics.isNullOrBlank()) {
                    lyrics = tag.getFirst("USLT")
                }
                if (lyrics.isNullOrBlank()) {
                    lyrics = tag.getFirst("UNSYNCEDLYRICS")
                }
                lyrics = lyrics?.trim()?.takeIf { it.isNotBlank() }
            }

            var artworkData: ByteArray? = null
            if (tag != null) {
                try {
                    val artwork: Artwork? = tag.firstArtwork
                    if (artwork != null) {
                        artworkData = artwork.binaryData
                        Log.d(TAG, "Extracted artwork data, size: ${artworkData?.size ?: 0} bytes")
                    } else {
                        Log.d(TAG, "No artwork found in the audio file tags.")
                    }
                } catch (artworkEx: Exception) {
                    Log.w(TAG, "Could not extract artwork from tags.", artworkEx)
                }
            }

            val result = AudioInfo(
                title = title,
                artist = artist,
                album = album,
                year = year,
                track = track,
                genre = genre,
                durationSeconds = durationSeconds,
                lyrics = lyrics,
                artworkData = artworkData
            )

            Log.d(
                TAG,
                "Extraction successful: Title='${result.title}', Artist='${result.artist}', Album='${result.album}', HasArtwork=${result.artworkData != null}"
            )
            return@withContext result

        } catch (e: Exception) {
            Log.e(TAG, "Error extracting info from stream with mimeType '$mimeType': ", e)
            return@withContext null
        } finally {
            // 清理临时文件
            tempAudioFile?.let { file ->
                try {
                    if (file.exists() && file.delete()) {
                        Log.d(TAG, "Deleted temporary file: ${file.absolutePath}")
                    } else if (file.exists()) {
                        Log.w(TAG, "Failed to delete temporary file: ${file.absolutePath}")
                    }
                } catch (deleteEx: Exception) {
                    Log.e(
                        TAG,
                        "Exception while deleting temporary file: ${file.absolutePath}",
                        deleteEx
                    )
                }
            }
        }
    }

}

/**
 * 辅助函数：将提取到的 `artworkData` 转换为 Android `Bitmap`。
 *
 * 注意：此函数涉及图像解码，应在后台线程调用。
 *
 * @param artworkData 从 AudioInfo 中获取的专辑封面字节数组。
 * @return 解码后的 `Bitmap` 对象，如果 `artworkData` 为 null 或解码失败则返回 null。
 */
fun createArtworkBitmap(artworkData: ByteArray?): Bitmap? {
    if (artworkData == null) {
        Log.d(TAG, "Cannot create bitmap, artworkData is null.")
        return null
    }
    return try {
        val options = BitmapFactory.Options().apply {
            // 可在此添加选项，如 inSampleSize 进行缩小以节省内存
            // inPreferredConfig = Bitmap.Config.RGB_565 // 如果不需要 alpha 通道可降低内存占用
        }
        val bitmap = BitmapFactory.decodeByteArray(artworkData, 0, artworkData.size, options)
        if (bitmap != null) {
            Log.d(
                TAG,
                "Successfully decoded artwork bitmap, dimensions: ${bitmap.width}x${bitmap.height}"
            )
        } else {
            Log.w(TAG, "Failed to decode artwork bitmap from byte array (returned null).")
        }
        bitmap
    } catch (e: Exception) {
        Log.e(TAG, "Error decoding artwork bitmap from byte array: ", e)
        null
    }
}

/**
 * 根据 MIME 类型推断合适的文件后缀。
 *
 * 更新：增加了对 ExoPlayer 返回的 'audio/raw' MIME 类型的支持，映射为 '.wav'。
 *
 * @param mimeType 输入的 MIME 类型字符串，可能为 null。
 * @return 一个安全的、有效的文件后缀（例如 ".mp3", ".flac", ".wav", ".tmp"）。
 */
private fun getSafeSuffixForMimeType(mimeType: String?): String {
    if (mimeType == null) {
        Log.v(TAG, "MIME type is null, defaulting to .tmp")
        return ".tmp"
    }

    val normalizedMimeType = mimeType.lowercase().trim()
    Log.v(TAG, "Normalizing MIME type: '$mimeType' -> '$normalizedMimeType'")

    val suffix = when (normalizedMimeType) {
        "audio/mpeg", "audio/mp3" -> ".mp3"
        "audio/flac", "audio/x-flac" -> ".flac"
        "audio/wav", "audio/x-wav",
        "audio/raw" -> ".wav" // 支持 ExoPlayer raw PCM/WAV
        "audio/aac", "audio/aacp" -> ".aac"
        "audio/ogg", "application/ogg" -> ".ogg"
        "audio/webm" -> ".webm"
        // Add more mappings as needed...
        else -> null
    }

    if (!suffix.isNullOrEmpty() && suffix.startsWith('.')) {
        if (suffix.all { it.isLetterOrDigit() || it == '.' }) {
            Log.v(TAG, "Mapped MIME type '$normalizedMimeType' to suffix '$suffix'")
            return suffix
        } else {
            Log.w(
                TAG,
                "Generated suffix '$suffix' contains invalid characters for MIME type '$mimeType'. Falling back to '.tmp'."
            )
        }
    }

    Log.w(
        TAG,
        "Could not determine a valid suffix for MIME type '$mimeType' ('$normalizedMimeType'). Using default '.tmp'."
    )
    return ".tmp"
}



