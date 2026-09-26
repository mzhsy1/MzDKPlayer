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
import org.mz.mzdkplayer.data.model.AudioInfo
import java.io.*


// 定义默认值常量
private const val DEFAULT_TITLE = "未知标题"
private const val DEFAULT_ARTIST = "未知艺术家"
private const val DEFAULT_ALBUM = "未知专辑"

private const val TAG = "AudioMetaExtractor"
private const val TEMP_FILE_BUFFER_SIZE = 16 * 1024 // 16KB buffer for copying

/** 复制流时先取这么多字节看文件头（够 `ID3` 头和 `RIFF....WAVE` 头用） */
private const val HEAD_PROBE_BYTES = 12

/** 读完 ID3 标签后再多读这么多字节，够 jaudiotagger 认出音频头 */
private const val AUDIO_HEAD_BYTES = 64 * 1024

/**
 * 从音频文件流中提取基本信息、内嵌歌词和专辑封面。
 *
 * 注意：此函数涉及文件 I/O 操作，应在后台线程调用 (例如 Coroutine Dispatcher.IO)。
 *
 * @param context Android Context，用于访问缓存目录创建临时文件。
 * @param inputStream 音频文件的输入流。调用方负责关闭此流。
 *                        如果为 null，则返回 null。
 * @param mimeType 文件的 MIME 类型，用于确定临时文件后缀。
 * @param fileName 文件名。**没有标签的文件**（例如裸 WAV）拿它兜底标题 / 歌手：
 *                 文件名里往往写着「看月亮爬上来 - 张杰」，比「未知标题」有用得多。
 *                 不传就保持原来的占位值行为（电视端的老调用点没有传）。
 * @return 包含提取信息的 AudioInfo 对象，如果发生错误则返回 null。
 */
suspend fun extractAudioInfoAndLyricsFromStream(
    context: Context,
    inputStream: InputStream?,
    mimeType: String?,
    fileName: String? = null,
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

            // 3. 复制到临时文件（标签在开头的文件会提前收工，见 copyToTempFile）
            FileOutputStream(tempAudioFile).use { outputStream ->
                copyToTempFile(inputStream, outputStream)
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
            val bits = audioHeader?.bitRateAsNumber
            val sampleRate = audioHeader?.sampleRate
            val bitsPerSample = audioHeader?.bitsPerSample

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

            // --- 兜底 1：自己再解一遍 ID3v2 ---
            // jaudiotagger 有两个盲区（实测）：WAV 的 `id3 ` chunk（标题/封面/歌词全丢）、
            // 以及 USLT 帧（`我觉得.mp3` 里 2650 字节的歌词读不出来）。
            val id3 = readId3Tags(tempAudioFile)

            // --- 兜底 2：标签里没有的，用文件名顶上 ---
            // 没有任何标签的文件（例如裸 WAV），jaudiotagger 会给「未知标题」这样的占位值，
            // 而文件名里往往写着「看月亮爬上来 - 张杰」，直接用占位值盖掉就太亏了。
            val nameMeta = fileName?.takeIf { it.isNotBlank() }?.let(AudioNameParser::parse)
            val finalTitle = pickMetadata(title, DEFAULT_TITLE, id3?.title, nameMeta?.title)
            val finalArtist = pickMetadata(artist, DEFAULT_ARTIST, id3?.artist, nameMeta?.artist)
            val finalAlbum = pickMetadata(album, DEFAULT_ALBUM, id3?.album, nameMeta?.album)

            val result = AudioInfo(
                title = finalTitle,
                artist = finalArtist,
                album = finalAlbum,
                year = year ?: id3?.year,
                track = track ?: id3?.track,
                genre = genre ?: id3?.genre,
                bit= bits,
                durationSeconds = durationSeconds,
                sampleRate = sampleRate,
                bitsPerSample = bitsPerSample,
                lyrics = lyrics ?: id3?.lyrics,
                artworkData = artworkData ?: id3?.artwork,
                localCoverPath = ""
            )

            Log.d(
                TAG,
                "Extraction successful: Title='${result.title}', Artist='${result.artist}', Album='${result.bit}', HasArtwork=${result.artworkData != null}"
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
 * 把输入流复制到临时文件，能提前收工就提前收工。
 *
 * 读元数据只需要「音频文件的开头」，唯一的例外是 WAV：它的 `id3 ` chunk 挂在几十 MB 的
 * `data` chunk 之后（实测 `看月亮爬上来 - 张杰.wav` 在 33MB 处），必须读到文件末尾才行。所以：
 * - 文件头以 `ID3` 开头（MP3 以及部分容器）→ 读完整个标签再多读一点音频头就停；
 * - 其它（WAVE / MP4 / OGG / FLAC…）→ 老实读到底。
 *
 * 注意流本身可能带 5MB 的 `LimitedInputStream`（见 `SmbUtils`），那种情况下读到底也只是
 * 读到被砍断的位置，所以 `SmbUtils` 里音频的 5MB 限制已经放开了。
 */
private fun copyToTempFile(input: InputStream, output: OutputStream): Long {
    val head = ByteArray(HEAD_PROBE_BYTES)
    var headFilled = 0
    while (headFilled < HEAD_PROBE_BYTES) {
        val count = input.read(head, headFilled, HEAD_PROBE_BYTES - headFilled)
        if (count < 0) break
        headFilled += count
    }
    output.write(head, 0, headFilled)
    var total = headFilled.toLong()
    if (headFilled == 0) return total

    val earlyStop = Id3TagReader.id3TagTotalSize(head)?.plus(AUDIO_HEAD_BYTES) ?: 0
    val buffer = ByteArray(TEMP_FILE_BUFFER_SIZE)
    while (true) {
        if (earlyStop > 0 && total >= earlyStop) break
        val count = input.read(buffer)
        if (count < 0) break
        output.write(buffer, 0, count)
        total += count
    }
    return total
}

/**
 * 元数据取值的优先级：解析值（且不是占位值）→ ID3 兜底 → 文件名解析 → 占位值。
 *
 * 单独拎成顶层函数是为了能进 JVM 单测（主函数要 Context，测不了）。
 */
internal fun pickMetadata(
    parsed: String?,
    placeholder: String,
    id3Value: String?,
    fromFileName: String?,
): String =
    parsed?.takeIf { it.isNotBlank() && it != placeholder }
        ?: id3Value?.takeIf { it.isNotBlank() }
        ?: fromFileName?.takeIf { it.isNotBlank() }
        // 三个来源都空时回到占位值，而不是空串 —— 空标题在列表和详情里都没有意义
        ?: parsed?.takeIf { it.isNotBlank() }
        ?: placeholder

/** 用自研的 [Id3TagReader] 再解一遍；失败不影响主流程（jaudiotagger 的结果照用） */
private fun readId3Tags(file: File): Id3Tags? = try {
    FileInputStream(file).use { Id3TagReader.read(it) }
} catch (e: Exception) {
    Log.w(TAG, "自研 ID3 解析失败，继续用 jaudiotagger 的结果", e)
    null
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
        // m4a 走 audio/mp4（`audioMimeTypeOf` 就是这么给的），漏了它会退化成 .tmp，
        // jaudiotagger 认不出容器
        "audio/mp4", "audio/x-m4a", "audio/m4a" -> ".m4a"
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



