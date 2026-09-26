package org.mz.mzdkplayer.tool

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 「按文件记住播放偏好」的纯逻辑测试：编解码、存储键、轨道匹配、索引淘汰。
 */
class PlaybackPreferenceTest {

    private val audio = PlaybackTrackRef(id = "2", index = 1)
    private val subtitle = PlaybackTrackRef(id = "-1", index = 0)

    // ---------- encode / decode ----------

    @Test
    fun `编解码 - 四项齐全可往返`() {
        val origin = PlaybackPreference(
            audio = audio,
            subtitle = subtitle,
            playbackSpeed = 1.5f,
            aspectRatio = "ZOOM"
        )
        val decoded = PlaybackPreferenceLogic.decode(PlaybackPreferenceLogic.encode(origin))
        assertEquals(origin, decoded)
    }

    @Test
    fun `编解码 - 只有一项也能往返`() {
        val origin = PlaybackPreference(audio = PlaybackTrackRef("3", 5))
        assertEquals(origin, PlaybackPreferenceLogic.decode(PlaybackPreferenceLogic.encode(origin)))
    }

    @Test
    fun `编解码 - 值为空的项不会被写进文本`() {
        val encoded = PlaybackPreferenceLogic.encode(PlaybackPreference(audio = audio))
        assertEquals("a:1:2", encoded)
    }

    @Test
    fun `解码 - 空值返回 null`() {
        assertNull(PlaybackPreferenceLogic.decode(null))
        assertNull(PlaybackPreferenceLogic.decode(""))
    }

    @Test
    fun `解码 - 无法识别的内容返回 null`() {
        assertNull(PlaybackPreferenceLogic.decode("乱七八糟"))
        assertNull(PlaybackPreferenceLogic.decode("x:1:2|y:3"))
    }

    @Test
    fun `解码 - 未知字段被忽略，已知字段照常解析`() {
        val decoded = PlaybackPreferenceLogic.decode("a:1:2|z:9:9|r:STRETCH")
        assertEquals(audio, decoded?.audio)
        assertEquals("STRETCH", decoded?.aspectRatio)
        assertNull(decoded?.subtitle)
    }

    @Test
    fun `解码 - 倍速非正数视为没存过`() {
        assertNull(PlaybackPreferenceLogic.decode("p:0")?.playbackSpeed)
        assertNull(PlaybackPreferenceLogic.decode("p:-1.5")?.playbackSpeed)
    }

    @Test
    fun `解码 - 下标不是数字时整条轨道作废`() {
        val decoded = PlaybackPreferenceLogic.decode("a:abc:2|r:FIT")
        assertNull(decoded?.audio)
        assertEquals("FIT", decoded?.aspectRatio)
    }

    @Test
    fun `解码 - 空的画面比例不算一项`() {
        // "r:" 解析出来是空串，不能当成「记住了空比例」
        assertNull(PlaybackPreferenceLogic.decode("r:"))
    }

    @Test
    fun `编解码 - 轨道 id 里的竖线与百分号会被转义`() {
        val weird = PlaybackTrackRef(id = "1|2%3%7C", index = 4)
        val origin = PlaybackPreference(audio = weird)
        val encoded = PlaybackPreferenceLogic.encode(origin)
        // 竖线一定要转义掉，否则解码时字段会被切错
        assertFalse(encoded.substringAfter("a:").contains("|"))
        assertEquals(origin, PlaybackPreferenceLogic.decode(encoded))
    }

    @Test
    fun `编解码 - id 为空时只靠下标还原`() {
        val origin = PlaybackPreference(subtitle = PlaybackTrackRef("", 2))
        val decoded = PlaybackPreferenceLogic.decode(PlaybackPreferenceLogic.encode(origin))
        assertEquals(2, decoded?.subtitle?.index)
        assertEquals("", decoded?.subtitle?.id)
    }

    // ---------- isEmpty ----------

    @Test
    fun `空偏好 - 四项全空才算空`() {
        assertTrue(PlaybackPreference().isEmpty)
        assertFalse(PlaybackPreference(audio = audio).isEmpty)
        assertFalse(PlaybackPreference(playbackSpeed = 2.0f).isEmpty)
        assertFalse(PlaybackPreference(aspectRatio = "FIT").isEmpty)
    }

    // ---------- storageKey ----------

    @Test
    fun `存储键 - 不同文件互不覆盖且前缀稳定`() {
        val a = PlaybackPreferenceLogic.storageKey("smb://host/a.mkv")
        val b = PlaybackPreferenceLogic.storageKey("smb://host/b.mkv")
        assertTrue(a != b)
        assertTrue(a.startsWith("playback_pref_"))
        assertEquals(a, PlaybackPreferenceLogic.storageKey("smb://host/a.mkv"))
    }

    // ---------- selectIndex ----------

    @Test
    fun `轨道匹配 - 优先按 id 精确命中`() {
        val tracks = listOf(
            PlaybackTrackRef("7", 0),
            PlaybackTrackRef("2", 1),
            PlaybackTrackRef("5", 2)
        )
        // id 为 2 的轨道在下标 1，虽然存的下标是 2，也应按 id 命中
        assertEquals(1, PlaybackPreferenceLogic.selectIndex(tracks, PlaybackTrackRef("2", 2)))
    }

    @Test
    fun `轨道匹配 - id 找不到时退回下标`() {
        val tracks = listOf(PlaybackTrackRef("7", 0), PlaybackTrackRef("2", 1))
        assertEquals(1, PlaybackPreferenceLogic.selectIndex(tracks, PlaybackTrackRef("99", 1)))
    }

    @Test
    fun `轨道匹配 - id 为空时直接按下标`() {
        val tracks = listOf(PlaybackTrackRef("", 0), PlaybackTrackRef("", 1))
        assertEquals(1, PlaybackPreferenceLogic.selectIndex(tracks, PlaybackTrackRef("", 1)))
    }

    @Test
    fun `轨道匹配 - 下标越界返回负一`() {
        val tracks = listOf(PlaybackTrackRef("7", 0))
        assertEquals(-1, PlaybackPreferenceLogic.selectIndex(tracks, PlaybackTrackRef("99", 5)))
    }

    @Test
    fun `轨道匹配 - 没存过或列表为空返回负一`() {
        assertEquals(-1, PlaybackPreferenceLogic.selectIndex(emptyList(), audio))
        assertEquals(-1, PlaybackPreferenceLogic.selectIndex(listOf(audio), null))
    }

    @Test
    fun `轨道匹配 - 关闭字幕的负一 id 能被正确命中`() {
        val tracks = listOf(
            PlaybackTrackRef("-1", 0),
            PlaybackTrackRef("1", 1)
        )
        assertEquals(0, PlaybackPreferenceLogic.selectIndex(tracks, PlaybackTrackRef("-1", 1)))
    }

    // ---------- touchIndex ----------

    @Test
    fun `索引淘汰 - 新键被放到最前面`() {
        val (kept, evicted) = PlaybackPreferenceLogic.touchIndex(listOf("b", "c"), "a")
        assertEquals(listOf("a", "b", "c"), kept)
        assertTrue(evicted.isEmpty())
    }

    @Test
    fun `索引淘汰 - 重复写入同一个键不会产生重复项`() {
        val (kept, _) = PlaybackPreferenceLogic.touchIndex(listOf("a", "b", "c"), "b")
        assertEquals(listOf("b", "a", "c"), kept)
    }

    @Test
    fun `索引淘汰 - 超出上限时返回需要删除的旧键`() {
        val (kept, evicted) = PlaybackPreferenceLogic.touchIndex(
            listOf("b", "c", "d"),
            "a",
            maxEntries = 3
        )
        assertEquals(listOf("a", "b", "c"), kept)
        assertEquals(listOf("d"), evicted)
    }

    @Test
    fun `索引淘汰 - 上限为零时全部淘汰`() {
        val (kept, evicted) = PlaybackPreferenceLogic.touchIndex(listOf("b"), "a", maxEntries = 0)
        assertTrue(kept.isEmpty())
        assertEquals(listOf("a", "b"), evicted)
    }

    // ---------- 画面比例记忆规则 ----------

    @Test
    fun `画面比例 - 锁定全局比例时不按文件记忆`() {
        assertTrue(PlaybackPreferenceLogic.shouldRememberAspectRatio(lockGlobalRatio = false))
        assertFalse(PlaybackPreferenceLogic.shouldRememberAspectRatio(lockGlobalRatio = true))
    }
}
