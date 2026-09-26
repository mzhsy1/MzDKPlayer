package org.mz.mzdkplayer.tool

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * LRC 歌词解析与「当前唱到哪一行」的纯逻辑。
 *
 * 这一份是从电视端搬过来的（搬到 `:core` 后手机端与电视端共用），
 * 顺带补了原来没有的边界用例。
 */
class LrcParserTest {

    @Test
    fun `解析毫秒精度的时间戳`() {
        assertEquals(62_340L, parseTime("01:02.34")?.inWholeMilliseconds)
        assertEquals(62_340L, parseTime("1:02.340")?.inWholeMilliseconds)
        assertEquals(62_300L, parseTime("01:02.3")?.inWholeMilliseconds)
        assertEquals(62_000L, parseTime("01:02")?.inWholeMilliseconds)
    }

    @Test
    fun `无法识别的时间戳返回空`() {
        assertNull(parseTime("abc"))
        assertNull(parseTime("01"))
        assertNull(parseTime("aa:bb"))
    }

    @Test
    fun `解析整段歌词并按时间排序`() {
        val entries = parseLrc(
            """
            [ti:标题]
            [ar:歌手]
            [00:10.00]第二句
            [00:05.50]第一句
            [00:15.00]第三句
            """.trimIndent()
        )

        assertEquals(listOf("第一句", "第二句", "第三句"), entries.map { it.text })
        assertEquals(listOf(5_500L, 10_000L, 15_000L), entries.map { it.time.inWholeMilliseconds })
    }

    @Test
    fun `空内容与全是元信息时返回空列表`() {
        assertEquals(emptyList<LyricEntry>(), parseLrc(null))
        assertEquals(emptyList<LyricEntry>(), parseLrc("   "))
        assertEquals(emptyList<LyricEntry>(), parseLrc("[ti:没有正文]"))
    }

    @Test
    fun `同一句歌词保留空文本也照常解析`() {
        val entries = parseLrc("[00:01.00]\n[00:02.00]有词")

        assertEquals(listOf("", "有词"), entries.map { it.text })
    }

    @Test
    fun `当前歌词取最后一条不晚于播放位置的行`() {
        val entries = parseLrc(
            """
            [00:05.00]第一句
            [00:10.00]第二句
            [00:15.00]第三句
            """.trimIndent()
        )

        assertEquals(-1, lyricIndexAt(entries, 0))
        assertEquals(-1, lyricIndexAt(entries, 4_999))
        assertEquals(0, lyricIndexAt(entries, 5_000))
        assertEquals(0, lyricIndexAt(entries, 9_999))
        assertEquals(1, lyricIndexAt(entries, 10_000))
        assertEquals(2, lyricIndexAt(entries, 999_999))
    }

    @Test
    fun `没有歌词时不会抛异常`() {
        assertEquals(-1, lyricIndexAt(emptyList(), 1_000))
    }
}
