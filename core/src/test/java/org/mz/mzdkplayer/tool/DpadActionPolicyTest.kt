package org.mz.mzdkplayer.tool

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mz.mzdkplayer.tool.logic.DpadActionPolicy

/**
 * [DpadActionPolicy] 的 JVM 单测。
 *
 * 关键契约：
 * 1. 新功能（「底部进度条」「播放列表」）依次追加在末尾，老 code 的「下一个」保持不变；
 * 2. 一圈能走遍所有功能且回到起点（设置页是单击轮转，转不回去就等于丢选项）；
 * 3. 未知 code 落到第一个，不会让播放页拿到空功能；
 * 4. [DpadActionPolicy.isControls] / [DpadActionPolicy.isPlaylist] 只认自己。
 */
class DpadActionPolicyTest {

    @Test
    fun `老功能的下一个保持不变`() {
        assertEquals(DpadActionPolicy.SUBTITLE, DpadActionPolicy.next(DpadActionPolicy.AUDIO))
        assertEquals(DpadActionPolicy.DANMAKU, DpadActionPolicy.next(DpadActionPolicy.SUBTITLE))
        assertEquals(DpadActionPolicy.VIDEO, DpadActionPolicy.next(DpadActionPolicy.DANMAKU))
        assertEquals(DpadActionPolicy.SPEED, DpadActionPolicy.next(DpadActionPolicy.VIDEO))
        assertEquals(DpadActionPolicy.ASPECT_RATIO, DpadActionPolicy.next(DpadActionPolicy.SPEED))
    }

    @Test
    fun `新选项追加在末尾`() {
        // 1.19.0 依次追加了「底部进度条」「播放列表」，顺序不能颠倒：
        // 老用户存的 CONTROLS 再按一下必须落到后加的 PLAYLIST，而不是跳回第一个
        assertEquals(DpadActionPolicy.CONTROLS, DpadActionPolicy.next(DpadActionPolicy.ASPECT_RATIO))
        assertEquals(DpadActionPolicy.PLAYLIST, DpadActionPolicy.next(DpadActionPolicy.CONTROLS))
        assertEquals(DpadActionPolicy.PLAYLIST, DpadActionPolicy.ORDER.last())
    }

    @Test
    fun `轮转一圈回到起点且覆盖全部功能`() {
        var code = DpadActionPolicy.ORDER.first()
        val visited = mutableListOf(code)
        repeat(DpadActionPolicy.ORDER.size - 1) {
            code = DpadActionPolicy.next(code)
            visited += code
        }
        assertEquals("一圈应覆盖全部功能且不重复", DpadActionPolicy.ORDER.toSet(), visited.toSet())
        assertEquals(
            "转完一圈应回到起点",
            DpadActionPolicy.ORDER.first(),
            DpadActionPolicy.next(code)
        )
    }

    @Test
    fun `未知 code 落到第一个`() {
        listOf("", "UNKNOWN", "CONTROL").forEach { code ->
            assertEquals("未知 code <$code> 应回落到第一个功能", DpadActionPolicy.ORDER.first(), DpadActionPolicy.next(code))
        }
    }

    @Test
    fun `默认值仍在清单里`() {
        // SettingsRepository 的默认值：上键 "D"（弹幕）、下键 "A"（音轨）
        assertTrue(DpadActionPolicy.ORDER.contains(DpadActionPolicy.DANMAKU))
        assertTrue(DpadActionPolicy.ORDER.contains(DpadActionPolicy.AUDIO))
    }

    @Test
    fun `isControls 只认底部进度条`() {
        assertTrue(DpadActionPolicy.isControls(DpadActionPolicy.CONTROLS))
        (DpadActionPolicy.ORDER - DpadActionPolicy.CONTROLS).forEach {
            assertFalse("<$it> 不应被当成底部进度条", DpadActionPolicy.isControls(it))
        }
    }

    @Test
    fun `isPlaylist 只认播放列表`() {
        assertTrue(DpadActionPolicy.isPlaylist(DpadActionPolicy.PLAYLIST))
        (DpadActionPolicy.ORDER - DpadActionPolicy.PLAYLIST).forEach {
            assertFalse("<$it> 不应被当成播放列表", DpadActionPolicy.isPlaylist(it))
        }
    }
}
