package org.mz.mzdkplayer.tool

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

/**
 * 字幕时间轴偏移的纯逻辑测试。
 *
 * 注意展示文案走 `String.format(Locale.getDefault(), ...)`，期望值也用同一个 Locale 生成，
 * 否则在德语等用逗号做小数点的机器上会误报。
 */
class SubtitleOffsetTest {

    private fun expectSeconds(value: Double): String =
        String.format(Locale.getDefault(), "%.1f", value)

    // ---------- step / clamp ----------

    @Test
    fun `步进 - 每次调整半秒`() {
        assertEquals(500, SubtitleOffsetLogic.step(0, 1))
        assertEquals(-500, SubtitleOffsetLogic.step(0, -1))
        assertEquals(1500, SubtitleOffsetLogic.step(500, 2))
    }

    @Test
    fun `步进 - 越界收敛到正负 30 秒`() {
        assertEquals(SubtitleOffsetLogic.MAX_MS, SubtitleOffsetLogic.step(29_500, 5))
        assertEquals(-SubtitleOffsetLogic.MAX_MS, SubtitleOffsetLogic.step(-29_500, -5))
    }

    @Test
    fun `收敛 - 超界值被夹回合法区间`() {
        assertEquals(0, SubtitleOffsetLogic.clamp(0))
        assertEquals(30_000, SubtitleOffsetLogic.clamp(999_999))
        assertEquals(-30_000, SubtitleOffsetLogic.clamp(-999_999))
    }

    @Test
    fun `能否继续 - 到达边界后返回 false`() {
        assertTrue(SubtitleOffsetLogic.canStep(0, 1))
        assertTrue(SubtitleOffsetLogic.canStep(0, -1))
        assertFalse(SubtitleOffsetLogic.canStep(SubtitleOffsetLogic.MAX_MS, 1))
        assertFalse(SubtitleOffsetLogic.canStep(-SubtitleOffsetLogic.MAX_MS, -1))
        // 已经顶到上界时，反方向仍然可以调
        assertTrue(SubtitleOffsetLogic.canStep(SubtitleOffsetLogic.MAX_MS, -1))
    }

    // ---------- formatSeconds ----------

    @Test
    fun `展示文案 - 零值不带符号`() {
        assertEquals(expectSeconds(0.0), SubtitleOffsetLogic.formatSeconds(0))
    }

    @Test
    fun `展示文案 - 正值带加号保留一位小数`() {
        assertEquals("+" + expectSeconds(1.5), SubtitleOffsetLogic.formatSeconds(1500))
        assertEquals("+" + expectSeconds(0.5), SubtitleOffsetLogic.formatSeconds(500))
    }

    @Test
    fun `展示文案 - 负值带减号`() {
        assertEquals("-" + expectSeconds(0.5), SubtitleOffsetLogic.formatSeconds(-500))
        assertEquals("-" + expectSeconds(2.0), SubtitleOffsetLogic.formatSeconds(-2000))
    }

    @Test
    fun `展示文案 - 超界值先收敛再展示`() {
        assertEquals("+" + expectSeconds(30.0), SubtitleOffsetLogic.formatSeconds(999_999))
    }

    // ---------- isDefault / toMicroseconds ----------

    @Test
    fun `默认值判定 - 只有零算默认`() {
        assertTrue(SubtitleOffsetLogic.isDefault(0))
        assertFalse(SubtitleOffsetLogic.isDefault(500))
        assertFalse(SubtitleOffsetLogic.isDefault(-500))
    }

    @Test
    fun `微秒换算 - 毫秒乘一千且先收敛`() {
        assertEquals(500_000L, SubtitleOffsetLogic.toMicroseconds(500))
        assertEquals(-1_500_000L, SubtitleOffsetLogic.toMicroseconds(-1500))
        assertEquals(30_000_000L, SubtitleOffsetLogic.toMicroseconds(999_999))
    }

    // ---------- shiftStartTimeUs ----------

    @Test
    fun `平移 - 正值把字幕往后推`() {
        assertEquals(5_500_000L, SubtitleOffsetLogic.shiftStartTimeUs(5_000_000L, 500_000L))
    }

    @Test
    fun `平移 - 负值把字幕往前拉`() {
        assertEquals(4_500_000L, SubtitleOffsetLogic.shiftStartTimeUs(5_000_000L, -500_000L))
    }

    @Test
    fun `平移 - 提前到负数收敛到零`() {
        // 把 2 秒处的字幕提前 30 秒，只能表示成「一开始就显示」
        assertEquals(0L, SubtitleOffsetLogic.shiftStartTimeUs(2_000_000L, -30_000_000L))
        assertEquals(0L, SubtitleOffsetLogic.shiftStartTimeUs(0L, -500_000L))
    }

    @Test
    fun `平移 - 零偏移原样返回`() {
        assertEquals(1234L, SubtitleOffsetLogic.shiftStartTimeUs(1234L, 0L))
    }
}
