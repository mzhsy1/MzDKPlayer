package org.mz.mzdkplayer.tool

import java.io.EOFException
import java.io.InputStream
import java.nio.charset.Charset

/**
 * 从 ID3v2 标签里读出来的东西（只保留播放页用得上的字段）。
 *
 * 存在的理由是 jaudiotagger 有两个覆盖不到的口子（实测于 2026-09-26）：
 * 1. **WAV 的 ID3 chunk 读不出来** —— 规范的写法是在 `data` chunk 之后放一个 id 为小写
 *    `id3 ` 的 chunk，jaudiotagger 对这种排布直接给了「无标签」，于是标题、封面、歌词全丢；
 * 2. **USLT（内嵌歌词）读不出来** —— `我觉得.mp3` 里明明有 2650 字节的 USLT，
 *    `tag.getFirst(FieldKey.LYRICS)` 仍然返回 null。
 *
 * 所以这里自己解一遍，在 jaudiotagger 的结果上做**兜底填充**（已有的值不覆盖）。
 * 纯 JDK 实现，可以进 JVM 单测。
 */
data class Id3Tags(
    val title: String? = null,
    val artist: String? = null,
    val album: String? = null,
    val year: String? = null,
    val track: String? = null,
    val genre: String? = null,
    val lyrics: String? = null,
    val artwork: ByteArray? = null,
    val artworkMimeType: String? = null,
) {
    /** 什么都没读出来（调用方据此省掉一次无用功） */
    fun isEmpty(): Boolean =
        title == null && artist == null && album == null && year == null && track == null &&
            genre == null && lyrics == null && artwork == null
}

/**
 * 精简的 ID3v2 读取器：只认播放页需要的那几种帧。
 *
 * 支持 v2.2（3 字节帧头）/ v2.3 / v2.4、unsynchronisation，以及三种编码
 * （ISO-8859-1 / UTF-16 带或不带 BOM / UTF-8）。
 * 帧数据带压缩或加密的会被整帧跳过（正常音乐文件不会用）。
 *
 * 解析是**逐字节自愈**的：遇到不合法的帧头就把游标往前挪一个字节继续找，
 * 这样扩展头、不规范写入、padding 都不会让整段解析崩掉。
 */
object Id3TagReader {

    private const val TAG_HEADER_SIZE = 10
    private const val READ_HEAD_SIZE = 12 // 够放下 ID3 头（10）和 RIFF 头（12）
    private const val MAX_TAG_BYTES = 32 * 1024 * 1024

    /** 连续这么多字节为 0 就认定进入 padding，停止扫描（免得在几 MB 的填充里空转） */
    private const val PADDING_RUN = 16

    /** 封面帧里的图片类型：3 = 正面封面，优先取它 */
    private const val APIC_TYPE_FRONT_COVER = 3

    /** [`parse`] 的中间结果：APIC 可能有多张，留一张「最像封面」的 */
    private class Builder {
        var title: String? = null
        var artist: String? = null
        var album: String? = null
        var year: String? = null
        var track: String? = null
        var genre: String? = null
        var lyrics: String? = null
        var artwork: ByteArray? = null
        var artworkMimeType: String? = null
        var artworkPictureType: Int = -1

        fun takeArtwork(mime: String?, bytes: ByteArray, pictureType: Int) {
            val better = artwork == null ||
                (pictureType == APIC_TYPE_FRONT_COVER && artworkPictureType != APIC_TYPE_FRONT_COVER)
            if (better) {
                artwork = bytes
                artworkMimeType = mime
                artworkPictureType = pictureType
            }
        }

        fun build(): Id3Tags = Id3Tags(
            title = title,
            artist = artist,
            album = album,
            year = year,
            track = track,
            genre = genre,
            lyrics = lyrics,
            artwork = artwork,
            artworkMimeType = artworkMimeType,
        )
    }

    // ==================== 对外入口 ====================

    /**
     * 从一个音频文件流里读 ID3v2。
     *
     * 两种排布都认：
     * - 流开头就是 `ID3`（MP3 的常见写法）；
     * - `RIFF....WAVE` 且某个 chunk（大小写 `ID3 ` / `id3 ` 都认）里装着完整标签（WAV 的写法）。
     *
     * 传入的如果是 `FileInputStream`，跳过几十 MB 的 `data` chunk 只是一次 seek，
     * 不会真把音频读一遍。
     */
    fun read(input: InputStream): Id3Tags? {
        val head = ByteArray(READ_HEAD_SIZE)
        if (!readFully(input, head)) return null

        return when {
            isId3Header(head) -> {
                val tagSize = synchsafeSize(head, 6)
                if (tagSize <= 0 || tagSize > MAX_TAG_BYTES) return null
                val body = ByteArray(tagSize)
                // 头 10 字节已经读掉了，剩下 tagSize 字节里前 2 个也已经在 head 里
                val carried = minOf(READ_HEAD_SIZE - TAG_HEADER_SIZE, tagSize)
                System.arraycopy(head, TAG_HEADER_SIZE, body, 0, carried)
                if (!readFully(input, body, carried, tagSize - carried)) return null
                parseTag(head, body)
            }

            isRiffWaveHeader(head) -> {
                val chunk = readWaveId3Chunk(input) ?: return null
                parse(chunk)
            }

            else -> null
        }
    }

    /**
     * 文件头以 `ID3` 开头时返回「整个标签占多少字节」（10 字节头 + 标签长度），否则 null。
     *
     * 调用方用它做「读到标签结束就收工」：这类文件的标签一定在开头，没必要为了读元数据
     * 把几十 MB 的音频全下下来。WAV 的 `id3 ` chunk 在 `data` 之后，这里会返回 null，
     * 调用方据此知道必须读到文件末尾。
     */
    fun id3TagTotalSize(head: ByteArray): Int? {
        if (head.size < TAG_HEADER_SIZE || !isId3Header(head)) return null
        val tagSize = synchsafeSize(head, 6)
        if (tagSize <= 0 || tagSize > MAX_TAG_BYTES) return null
        return TAG_HEADER_SIZE + tagSize
    }

    /** 解析一段以 `ID3` 开头的字节 */
    fun parse(blob: ByteArray): Id3Tags? {
        if (blob.size < TAG_HEADER_SIZE || !isId3Header(blob)) return null
        val major = blob[3].toInt() and 0xFF
        if (major !in 2..4) return null
        val tagSize = synchsafeSize(blob, 6)
        if (tagSize <= 0) return null
        val available = minOf(tagSize, blob.size - TAG_HEADER_SIZE)
        if (available <= 0) return null
        return parseTag(blob, blob.copyOfRange(TAG_HEADER_SIZE, TAG_HEADER_SIZE + available))
    }

    // ==================== 标签体 ====================

    private fun parseTag(header: ByteArray, bodyRaw: ByteArray): Id3Tags? {
        val major = header[3].toInt() and 0xFF
        val flags = header[5].toInt() and 0xFF
        // 标签级 unsynchronisation（v2.3 常见）：**不能**先把整段 body 还原再切帧 ——
        // 帧长度字段记的是「存储时的长度」，还原后字节数会变少，切出来的帧全部错位。
        // 正确做法是在存储字节里切帧，再对每一帧的数据单独还原。
        val tagUnsynchronised = flags and 0x80 != 0

        val body = bodyRaw
        val builder = Builder()
        val frameHeaderSize = if (major == 2) 6 else 10
        var offset = 0
        var zeroRun = 0

        while (offset + frameHeaderSize <= body.size) {
            if (body[offset] == 0.toByte()) {
                zeroRun++
                // padding（或者扩展头，但扩展头前 4 字节里带着长度，不会连着 16 个 0）
                if (zeroRun >= PADDING_RUN) break
                offset++
                continue
            }
            zeroRun = 0

            if (!isValidFrameId(body, offset, major)) {
                offset++
                continue
            }

            val id = String(body, offset, if (major == 2) 3 else 4, Charsets.ISO_8859_1)
            val size = when (major) {
                2 -> bigEndian24(body, offset + 3)
                4 -> synchsafeSize(body, offset + 4)
                else -> bigEndianInt(body, offset + 4)
            }
            val dataStart = offset + frameHeaderSize
            val dataEnd = dataStart + size
            if (size <= 0 || dataEnd > body.size) {
                offset++
                continue
            }
            // 帧标志位非 0 意味着数据带了压缩 / 加密 / 分组 / 同步头之类的加工，
            // 这些加工都要额外解析前缀，索性整帧跳过（正常音乐文件这里都是 0）
            val frameFlags = if (major == 2) 0 else bigEndianShort(body, offset + 8)
            if (frameFlags == 0) {
                val raw = body.copyOfRange(dataStart, dataEnd)
                handleFrame(builder, id, if (tagUnsynchronised) deUnsynchronise(raw) else raw)
            }
            offset = dataEnd
        }

        val result = builder.build()
        return if (result.isEmpty()) null else result
    }

    /** [data] 是**已经还原过 unsynchronisation** 的帧数据，所以偏移一律从 0 起算 */
    private fun handleFrame(builder: Builder, id: String, data: ByteArray) {
        when (id) {
            "APIC", "PIC" -> readPictureFrame(builder, id, data)
            "USLT", "ULT" -> if (builder.lyrics.isNullOrBlank()) {
                readLyricsFrame(data)?.let { builder.lyrics = it }
            }

            "TXXX", "TXX" -> if (builder.lyrics.isNullOrBlank()) {
                readUserTextFrame(data)?.let { builder.lyrics = it }
            }

            else -> if (id.startsWith("T")) readTextFrame(builder, id, data)
        }
    }

    private fun readTextFrame(builder: Builder, id: String, data: ByteArray) {
        val text = decodeEncodedString(data)?.let(::joinMultiValue) ?: return
        when (id) {
            "TIT2", "TT2" -> builder.title = builder.title ?: text
            "TPE1", "TP1" -> builder.artist = builder.artist ?: text
            "TALB", "TAL" -> builder.album = builder.album ?: text
            "TYER", "TYE", "TDRC", "TDA" -> builder.year = builder.year ?: text
            "TRCK", "TRK" -> builder.track = builder.track ?: text
            "TCON", "TCO" -> builder.genre = builder.genre ?: text
        }
    }

    /**
     * `TXXX` 里也可能是歌词：国内一些工具（酷狗 / QQ 音乐）把 LRC 写进
     * 「描述符 = USLT」的 TXXX 帧，而不是标准的 USLT 帧。
     */
    private fun readUserTextFrame(data: ByteArray): String? {
        if (data.isEmpty()) return null
        val encoding = data[0].toInt() and 0xFF
        val descriptionEnd = indexOfTerminator(data, 1, data.size, encoding)
        val description = decodeRange(data, 1, descriptionEnd.first, encoding).orEmpty().trim()
        val key = description.uppercase()
        if (!key.contains("USLT") && !key.contains("LYRIC")) return null
        return decodeRange(data, descriptionEnd.second, data.size, encoding)?.let(::cleanText)
    }

    private fun readLyricsFrame(data: ByteArray): String? {
        // <编码:1><语言:3><描述符><歌词>
        if (data.size <= 4) return null
        val encoding = data[0].toInt() and 0xFF
        val descriptionEnd = indexOfTerminator(data, 4, data.size, encoding)
        return decodeRange(data, descriptionEnd.second, data.size, encoding)?.let(::cleanText)
    }

    private fun readPictureFrame(builder: Builder, id: String, data: ByteArray) {
        if (data.size <= 2) return
        val encoding = data[0].toInt() and 0xFF

        // v2.2 的 PIC 用 3 字节图片格式（"JPG"/"PNG"），v2.3+ 用以 0 结尾的 MIME
        val mime: String?
        val afterMime: Int
        if (id == "PIC") {
            mime = when (String(data, 1, 3, Charsets.ISO_8859_1).uppercase()) {
                "JPG" -> "image/jpeg"
                "PNG" -> "image/png"
                else -> null
            }
            afterMime = 4
        } else {
            val mimeEnd = indexOfByte(data, 0x00, 1, data.size)
            if (mimeEnd < 0) return
            mime = String(data, 1, mimeEnd - 1, Charsets.ISO_8859_1).ifBlank { null }
            afterMime = mimeEnd + 1
        }
        if (afterMime >= data.size) return

        val pictureType = data[afterMime].toInt() and 0xFF
        val descriptionEnd = indexOfTerminator(data, afterMime + 1, data.size, encoding)
        val dataStart = descriptionEnd.second
        if (dataStart >= data.size) return
        builder.takeArtwork(mime, data.copyOfRange(dataStart, data.size), pictureType)
    }

    // ==================== WAV ====================

    /** chunk id 是 `ID3 `（大写，规范写法）或 `id3 `（小写，实测常见） */
    private fun readWaveId3Chunk(input: InputStream): ByteArray? {
        while (true) {
            val chunkHeader = ByteArray(8)
            if (!readFully(input, chunkHeader)) return null
            val id = String(chunkHeader, 0, 4, Charsets.ISO_8859_1)
            val size = littleEndianInt(chunkHeader, 4)
            if (size < 0) return null
            if (id == "ID3 " || id == "id3 ") {
                if (size <= 0 || size > MAX_TAG_BYTES) return null
                val blob = ByteArray(size)
                return if (readFully(input, blob)) blob else null
            }
            // chunk 数据按 2 字节对齐
            if (!skipFully(input, size.toLong() + (size and 1))) return null
        }
    }

    // ==================== 编码 / 字节工具 ====================

    /** 文本帧是 `<编码:1><文本>`：编码字节自己不算文本，从第 2 个字节开始解 */
    private fun decodeEncodedString(data: ByteArray): String? {
        if (data.isEmpty()) return null
        return decodeRange(data, 1, data.size, data[0].toInt() and 0xFF)
    }

    /** 按 ID3 的编码字节解码 `[start, end)`；[start] 处是编码字节 */
    private fun decodeRange(body: ByteArray, start: Int, end: Int, encoding: Int): String? {
        if (start < 0 || end > body.size || end - start <= 0) return null
        val (charset, offset) = charsetFor(encoding, body, start, end)
        if (offset >= end) return null
        return runCatching { String(body, offset, end - offset, charset) }.getOrNull()
    }

    private fun charsetFor(encoding: Int, body: ByteArray, start: Int, end: Int): Pair<Charset, Int> =
        when (encoding) {
            0 -> Charsets.ISO_8859_1 to start
            1 -> when {
                start + 1 < end && body[start] == 0xFF.toByte() && body[start + 1] == 0xFE.toByte() ->
                    Charsets.UTF_16LE to start + 2

                start + 1 < end && body[start] == 0xFE.toByte() && body[start + 1] == 0xFF.toByte() ->
                    Charsets.UTF_16BE to start + 2

                // 规范要求 enc=1 必须带 BOM，但实际文件常缺；按小端猜（Windows 写入的绝大多数是小端）
                else -> Charsets.UTF_16LE to start
            }

            2 -> Charsets.UTF_16BE to start
            else -> Charsets.UTF_8 to start
        }

    /** 找到编码相关的字符串终止符，返回 (终止符起点, 终止符之后) */
    private fun indexOfTerminator(body: ByteArray, start: Int, end: Int, encoding: Int): Pair<Int, Int> {
        if (encoding == 1 || encoding == 2) {
            var index = start
            while (index + 1 < end) {
                if (body[index] == 0.toByte() && body[index + 1] == 0.toByte()) {
                    return index to index + 2
                }
                index += 2
            }
            return end to end
        }
        val index = indexOfByte(body, 0x00, start, end)
        return if (index < 0) end to end else index to index + 1
    }

    /** 去掉首尾空白与 BOM（TXXX 写的歌词实测带 `\uFEFF`） */
    private fun cleanText(text: String): String? =
        text.removePrefix("\uFEFF").trim().takeIf { it.isNotBlank() }

    /** 文本帧里可能有多个值（用 0 分隔），拼起来显示 */
    private fun joinMultiValue(text: String): String? =
        cleanText(text.split('\u0000').filter { it.isNotBlank() }.joinToString(" / "))

    /** ID3v2 的 unsynchronisation：把 `FF 00` 还原成 `FF` */
    private fun deUnsynchronise(data: ByteArray): ByteArray {
        val out = ByteArray(data.size)
        var written = 0
        var index = 0
        while (index < data.size) {
            out[written++] = data[index]
            if (data[index] == 0xFF.toByte() && index + 1 < data.size && data[index + 1] == 0.toByte()) {
                index += 2
            } else {
                index++
            }
        }
        return if (written == data.size) out else out.copyOf(written)
    }

    private fun isId3Header(data: ByteArray): Boolean =
        data.size >= 3 && data[0] == 'I'.code.toByte() &&
            data[1] == 'D'.code.toByte() && data[2] == '3'.code.toByte()

    private fun isRiffWaveHeader(data: ByteArray): Boolean =
        data.size >= 12 &&
            String(data, 0, 4, Charsets.ISO_8859_1) == "RIFF" &&
            String(data, 8, 4, Charsets.ISO_8859_1) == "WAVE"

    private fun isValidFrameId(body: ByteArray, offset: Int, major: Int): Boolean {
        val length = if (major == 2) 3 else 4
        if (offset + length > body.size) return false
        for (index in offset until offset + length) {
            val char = body[index].toInt() and 0xFF
            val isUpper = char in 'A'.code..'Z'.code
            val isDigit = char in '0'.code..'9'.code
            if (!isUpper && !isDigit) return false
        }
        return true
    }

    private fun synchsafeSize(data: ByteArray, offset: Int): Int {
        if (offset + 4 > data.size) return -1
        var value = 0
        for (index in offset until offset + 4) {
            value = (value shl 7) or (data[index].toInt() and 0x7F)
        }
        return value
    }

    private fun bigEndianInt(data: ByteArray, offset: Int): Int {
        if (offset + 4 > data.size) return -1
        return ((data[offset].toInt() and 0xFF) shl 24) or
            ((data[offset + 1].toInt() and 0xFF) shl 16) or
            ((data[offset + 2].toInt() and 0xFF) shl 8) or
            (data[offset + 3].toInt() and 0xFF)
    }

    private fun bigEndian24(data: ByteArray, offset: Int): Int {
        if (offset + 3 > data.size) return -1
        return ((data[offset].toInt() and 0xFF) shl 16) or
            ((data[offset + 1].toInt() and 0xFF) shl 8) or
            (data[offset + 2].toInt() and 0xFF)
    }

    private fun bigEndianShort(data: ByteArray, offset: Int): Int {
        if (offset + 2 > data.size) return 0
        return ((data[offset].toInt() and 0xFF) shl 8) or (data[offset + 1].toInt() and 0xFF)
    }

    private fun littleEndianInt(data: ByteArray, offset: Int): Int {
        if (offset + 4 > data.size) return -1
        return (data[offset].toInt() and 0xFF) or
            ((data[offset + 1].toInt() and 0xFF) shl 8) or
            ((data[offset + 2].toInt() and 0xFF) shl 16) or
            ((data[offset + 3].toInt() and 0xFF) shl 24)
    }

    private fun indexOfByte(data: ByteArray, value: Int, start: Int, end: Int): Int {
        for (index in start until end) {
            if ((data[index].toInt() and 0xFF) == value) return index
        }
        return -1
    }

    private fun readFully(input: InputStream, buffer: ByteArray, offset: Int = 0, length: Int = buffer.size): Boolean {
        var read = 0
        try {
            while (read < length) {
                val count = input.read(buffer, offset + read, length - read)
                if (count < 0) return false
                read += count
            }
        } catch (_: EOFException) {
            return false
        }
        return true
    }

    private fun skipFully(input: InputStream, count: Long): Boolean {
        var remaining = count
        val scratch = ByteArray(8 * 1024)
        while (remaining > 0) {
            val skipped = input.skip(remaining)
            if (skipped > 0) {
                remaining -= skipped
                continue
            }
            val read = input.read(scratch, 0, minOf(scratch.size.toLong(), remaining).toInt())
            if (read < 0) return false
            remaining -= read
        }
        return true
    }
}
