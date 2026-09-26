package org.mz.mzdkplayer.tool

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.nio.charset.Charset

/**
 * 自研 ID3v2 读取器的用例。
 *
 * 字节全部手工拼装（而不是读真实文件），这样每个分支的输入都是确定的；
 * 用例的形状照着 2026-09-26 实测的那几个真文件来：
 * - `我觉得.mp3`：v2.3 + UTF-16 的 USLT（jaudiotagger 读不到的那种）
 * - `看月亮爬上来 - 张杰.wav`：小写 `id3 ` chunk 挂在 `data` 之后
 * - `开启新征程2 - 阿鲲.mp3`：歌词藏在描述符为 `USLT` 的 TXXX 帧里
 */
class Id3TagReaderTest {

    // ==================== 拼字节的工具 ====================

    private fun synchsafe(value: Int): ByteArray = byteArrayOf(
        ((value shr 21) and 0x7F).toByte(),
        ((value shr 14) and 0x7F).toByte(),
        ((value shr 7) and 0x7F).toByte(),
        (value and 0x7F).toByte(),
    )

    private fun beInt(value: Int): ByteArray = byteArrayOf(
        ((value shr 24) and 0xFF).toByte(),
        ((value shr 16) and 0xFF).toByte(),
        ((value shr 8) and 0xFF).toByte(),
        (value and 0xFF).toByte(),
    )

    private fun leInt(value: Int): ByteArray = byteArrayOf(
        (value and 0xFF).toByte(),
        ((value shr 8) and 0xFF).toByte(),
        ((value shr 16) and 0xFF).toByte(),
        ((value shr 24) and 0xFF).toByte(),
    )

    private fun frame(id: String, payload: ByteArray, major: Int = 3): ByteArray =
        id.toByteArray(Charset.forName("ISO-8859-1")) +
            (if (major >= 4) synchsafe(payload.size) else beInt(payload.size)) +
            byteArrayOf(0, 0) +
            payload

    private fun tag(frames: List<ByteArray>, major: Int = 3, flags: Int = 0): ByteArray {
        val body = frames.fold(ByteArray(0)) { acc, item -> acc + item }
        return "ID3".toByteArray(Charset.forName("ISO-8859-1")) +
            byteArrayOf(major.toByte(), 0, flags.toByte()) +
            synchsafe(body.size) +
            body
    }

    /** enc=0 的文本帧（ISO-8859-1） */
    private fun latinTextFrame(id: String, text: String, major: Int = 3): ByteArray =
        frame(id, byteArrayOf(0) + text.toByteArray(Charset.forName("ISO-8859-1")), major)

    /** enc=1 的文本帧（UTF-16LE + BOM，和真文件一致） */
    private fun utf16TextFrame(id: String, text: String, major: Int = 3): ByteArray =
        frame(id, byteArrayOf(1) + ("\uFEFF" + text).toByteArray(Charsets.UTF_16LE), major)

    /** enc=1 的 USLT：`<编码><语言3><描述符(UTF-16 空 = 00 00)><歌词>` */
    private fun usltFrame(lyrics: String, language: String = "XXX", major: Int = 3): ByteArray =
        frame(
            "USLT",
            byteArrayOf(1) +
                language.toByteArray(Charset.forName("ISO-8859-1")) +
                byteArrayOf(0, 0) +
                ("\uFEFF" + lyrics).toByteArray(Charsets.UTF_16LE),
            major,
        )

    /** APIC：`<编码0><mime\0><图片类型><描述符\0><图片数据>` */
    private fun apicFrame(picture: ByteArray, pictureType: Int = 3, mime: String = "image/jpeg"): ByteArray =
        frame(
            "APIC",
            byteArrayOf(0) +
                mime.toByteArray(Charset.forName("ISO-8859-1")) +
                byteArrayOf(0) +
                byteArrayOf(pictureType.toByte()) +
                byteArrayOf(0) +
                picture,
        )

    /** WAV：`RIFF....WAVE` + `fmt ` + `data` + 小写 `id3 ` */
    private fun wav(embedTag: ByteArray, dataBytes: Int = 32, lowercaseId3: Boolean = true): ByteArray {
        val data = ByteArray(dataBytes) { 0x7F }
        val id3Chunk = (if (lowercaseId3) "id3 " else "ID3 ")
            .toByteArray(Charset.forName("ISO-8859-1")) + leInt(embedTag.size) + embedTag
        val body = "fmt ".toByteArray(Charset.forName("ISO-8859-1")) + leInt(16) + ByteArray(16) +
            "data".toByteArray(Charset.forName("ISO-8859-1")) + leInt(data.size) + data +
            id3Chunk
        return "RIFF".toByteArray(Charset.forName("ISO-8859-1")) + leInt(body.size) +
            "WAVE".toByteArray(Charset.forName("ISO-8859-1")) + body
    }

    // ==================== 用例 ====================

    @Test
    fun `能读出标题艺术家专辑年份`() {
        val parsed = Id3TagReader.parse(
            tag(
                listOf(
                    utf16TextFrame("TIT2", "看月亮爬上来"),
                    utf16TextFrame("TPE1", "张杰"),
                    utf16TextFrame("TALB", "穿越三部曲"),
                    latinTextFrame("TYER", "2009"),
                    latinTextFrame("TRCK", "1"),
                )
            )
        )

        assertNotNull(parsed)
        assertEquals("看月亮爬上来", parsed!!.title)
        assertEquals("张杰", parsed.artist)
        assertEquals("穿越三部曲", parsed.album)
        assertEquals("2009", parsed.year)
        assertEquals("1", parsed.track)
    }

    @Test
    fun `读出 UTF-16 的内嵌歌词并把 CRLF 原样保留`() {
        val lyrics = "[00:00.00]作词 : 赵雷\r\n[00:01.00]作曲 : 赵雷"

        val parsed = Id3TagReader.parse(tag(listOf(usltFrame(lyrics))))

        assertEquals(lyrics, parsed?.lyrics)
    }

    @Test
    fun `UTF-16 不带 BOM 时按小端解`() {
        val payload = byteArrayOf(1) + "XXX".toByteArray(Charset.forName("ISO-8859-1")) +
            byteArrayOf(0, 0) + "[00:01.00]第一句".toByteArray(Charsets.UTF_16LE)

        val parsed = Id3TagReader.parse(tag(listOf(frame("USLT", payload))))

        assertEquals("[00:01.00]第一句", parsed?.lyrics)
    }

    @Test
    fun `TXXX 里描述符为 USLT 的歌词也能认`() {
        val payload = byteArrayOf(3) +
            "USLT".toByteArray(Charsets.UTF_8) +
            byteArrayOf(0) +
            "\uFEFF[00:01.58]纯音乐，请欣赏".toByteArray(Charsets.UTF_8)

        val parsed = Id3TagReader.parse(tag(listOf(frame("TXXX", payload))))

        assertEquals("[00:01.58]纯音乐，请欣赏", parsed?.lyrics)
    }

    @Test
    fun `描述符无关的 TXXX 不会被当成歌词`() {
        val payload = byteArrayOf(3) +
            "QMQUALITY".toByteArray(Charsets.UTF_8) +
            byteArrayOf(0) +
            "Premium-Master".toByteArray(Charsets.UTF_8)

        val parsed = Id3TagReader.parse(tag(listOf(frame("TXXX", payload), utf16TextFrame("TIT2", "歌名"))))

        assertNull(parsed?.lyrics)
        assertEquals("歌名", parsed?.title)
    }

    @Test
    fun `读出 APIC 的图片字节与 MIME`() {
        val picture = ByteArray(4096) { (it % 251).toByte() }

        val parsed = Id3TagReader.parse(tag(listOf(apicFrame(picture))))

        assertNotNull(parsed)
        assertEquals("image/jpeg", parsed!!.artworkMimeType)
        assertArrayEquals(picture, parsed.artwork!!)
    }

    @Test
    fun `多张封面优先取正面封面`() {
        val back = ByteArray(16) { 1 }
        val front = ByteArray(32) { 2 }

        val parsed = Id3TagReader.parse(tag(listOf(apicFrame(back, pictureType = 4), apicFrame(front, pictureType = 3))))

        assertNotNull(parsed)
        assertArrayEquals(front, parsed!!.artwork!!)
    }

    @Test
    fun `v2_4 的 synchsafe 帧长度也能解`() {
        val parsed = Id3TagReader.parse(
            tag(listOf(utf16TextFrame("TIT2", "我记得", major = 4)), major = 4)
        )

        assertEquals("我记得", parsed?.title)
    }

    @Test
    fun `开了 unsynchronisation 的标签能还原`() {
        // 歌词里带 0xFF 时，写入方会插一个 0x00，解析时必须去掉
        val lyrics = "[00:01.00]带\u00FF的歌词"
        val encoded = ("\uFEFF" + lyrics).toByteArray(Charsets.UTF_16LE)
        val escaped = ByteArray(encoded.size + 4)
        var written = 0
        var index = 0
        while (index < encoded.size) {
            escaped[written++] = encoded[index]
            if (encoded[index] == 0xFF.toByte()) escaped[written++] = 0
            index++
        }
        val payload = byteArrayOf(1) + "XXX".toByteArray(Charset.forName("ISO-8859-1")) +
            byteArrayOf(0, 0) + escaped.copyOf(written)

        val parsed = Id3TagReader.parse(tag(listOf(frame("USLT", payload)), flags = 0x80))

        assertEquals(lyrics, parsed?.lyrics)
    }

    @Test
    fun `v2_2 的三字节帧头也能解`() {
        val payload = byteArrayOf(1) + ("\uFEFF" + "老格式").toByteArray(Charsets.UTF_16LE)
        val frame2 = "TT2".toByteArray(Charset.forName("ISO-8859-1")) + byteArrayOf(0, 0, payload.size.toByte()) + payload

        val parsed = Id3TagReader.parse(tag(listOf(frame2), major = 2))

        assertEquals("老格式", parsed?.title)
    }

    @Test
    fun `没有 ID3 的字节返回 null`() {
        assertNull(Id3TagReader.parse("这是一段普通字节".toByteArray()))
        assertNull(Id3TagReader.parse(ByteArray(0)))
    }

    @Test
    fun `从 WAV 流里读小写 id3 chunk`() {
        val embed = tag(
            listOf(
                utf16TextFrame("TIT2", "看月亮爬上来"),
                usltFrame("[00:31.32]失眠的夜漫漫飘过来"),
                apicFrame(ByteArray(1024) { 3 }),
            )
        )
        val wavBytes = wav(embed)

        val parsed = Id3TagReader.read(ByteArrayInputStream(wavBytes))

        assertNotNull(parsed)
        assertEquals("看月亮爬上来", parsed!!.title)
        assertEquals("[00:31.32]失眠的夜漫漫飘过来", parsed.lyrics)
        assertEquals(1024, parsed.artwork!!.size)
    }

    @Test
    fun `大写 ID3 chunk 与 MP3 开头都能读`() {
        val embed = tag(listOf(utf16TextFrame("TIT2", "大写的")))

        assertEquals("大写的", Id3TagReader.read(ByteArrayInputStream(wav(embed, lowercaseId3 = false)))?.title)
        assertEquals("大写的", Id3TagReader.read(ByteArrayInputStream(embed))?.title)
    }

    @Test
    fun `WAV 里没有 id3 chunk 时返回 null`() {
        val body = "fmt ".toByteArray(Charset.forName("ISO-8859-1")) + leInt(16) + ByteArray(16) +
            "data".toByteArray(Charset.forName("ISO-8859-1")) + leInt(8) + ByteArray(8)
        val wavBytes = "RIFF".toByteArray(Charset.forName("ISO-8859-1")) + leInt(body.size) +
            "WAVE".toByteArray(Charset.forName("ISO-8859-1")) + body

        assertNull(Id3TagReader.read(ByteArrayInputStream(wavBytes)))
    }

    @Test
    fun `空标签体与全是填充的标签返回 null`() {
        assertTrue(Id3TagReader.parse(tag(listOf(latinTextFrame("TIT2", ""))))?.title.isNullOrBlank())
        assertNull(Id3TagReader.parse(tag(listOf(ByteArray(64)))))
    }
}
