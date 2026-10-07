package org.mz.mzdkplayer.tool

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mz.mzdkplayer.tool.logic.SeekAccelerationLogic
import org.mz.mzdkplayer.tool.logic.SeekAccelerationPolicy
import org.mz.mzdkplayer.tool.logic.SeekDirection

/**
 * [SeekAccelerationLogic] 的 JVM 单测。
 *
 * 关键契约：
 * 1. 第一次步进 = 设置里的基础步长（短按不能因为「加速」变样）；
 * 2. 之后按片长换算出的档位链放大，每 [SeekAccelerationLogic.TICKS_PER_TIER] 拍进一档，
 *    走到链尾就停住，且**步长单调不减**（用户基础步长设得比档位大时不许回退）；
 * 3. 关闭档（空链）永远只跳基础步长；
 * 4. HUD 的累计值必须等于每次步长之和，目标位置必须夹在 `[0, 总时长]`。
 *
 * 档位链本身（每一档多少秒、怎么按时长换算）由 `SeekAccelerationPolicyTest` 覆盖，
 * 这里固定用「2 小时片长 → 标准档」，只验证换算。
 */
class SeekAccelerationTest {

    private val twoHours = 2 * 60 * 60 * 1000L

    /** 2 小时片长、标准档：36 / 72 / 144 / 300 秒 */
    private val steps = SeekAccelerationPolicy.stepsFor(twoHours, SeekAccelerationPolicy.LEVEL_STANDARD)

    private fun step(baseSeconds: Int, tickIndex: Int) =
        SeekAccelerationLogic.stepSeconds(baseSeconds, steps, tickIndex)

    @Test
    fun `片长换算出的档位链符合预期`() {
        assertEquals(listOf(36, 72, 144, 300), steps)
    }

    @Test
    fun `第一步就是设置里的基础步长`() {
        assertEquals(15, step(15, 0))
        assertEquals(30, step(30, 0))
        assertEquals(5, step(5, 0))
    }

    @Test
    fun `每三拍进一档且走到链尾就停住`() {
        val base = 10
        // 第 0 拍：短按的基础步长
        assertEquals(10, step(base, 0))
        // 1-3 拍 → 第一档 36 秒
        (1..3).forEach { assertEquals("tick=$it", 36, step(base, it)) }
        // 4-6 拍 → 第二档 72 秒
        (4..6).forEach { assertEquals("tick=$it", 72, step(base, it)) }
        // 7-9 拍 → 第三档 144 秒
        (7..9).forEach { assertEquals("tick=$it", 144, step(base, it)) }
        // 10 拍起 → 第四档 300 秒，并且一直封顶
        (10..40).forEach { assertEquals("tick=$it", 300, step(base, it)) }
    }

    @Test
    fun `关闭档（空链）每拍都只跳基础步长`() {
        (0..20).forEach { tick ->
            assertEquals("tick=$tick", 15, SeekAccelerationLogic.stepSeconds(15, emptyList(), tick))
        }
    }

    @Test
    fun `基础步长比档位大时不回退`() {
        // 用户把基础步长设成 120 秒，而片长换算出的前几档只有 36 / 72 秒
        assertEquals(120, step(120, 0))
        assertEquals(120, step(120, 1))
        assertEquals(120, step(120, 4))
        // 档位超过基础步长后继续放大
        assertEquals(144, step(120, 7))
        assertEquals(300, step(120, 12))
    }

    @Test
    fun `步长单调不减`() {
        var previous = 0
        listOf(5, 15, 120, 600).forEach { base ->
            previous = 0
            for (tickIndex in 0..60) {
                val current = step(base, tickIndex)
                assertTrue("base=$base tick=$tickIndex: $current 不应小于 $previous", current >= previous)
                previous = current
            }
        }
    }

    @Test
    fun `用户把基础步长设到设置上限也照常生效`() {
        // 基础步长 600 秒（设置项上限）是用户自己的选择，不能被加速逻辑悄悄压到 300
        assertEquals(600, step(600, 0))
        assertEquals(600, step(600, 20))
        // 负数 / 0 按 1 秒处理，不允许出现 0 步长
        assertEquals(1, SeekAccelerationLogic.stepSeconds(0, emptyList(), 0))
        assertEquals(1, SeekAccelerationLogic.stepSeconds(-30, emptyList(), 5))
        // 脏数据链（混进 0 / 负数）也不能算出 0 步长
        assertEquals(15, SeekAccelerationLogic.stepSeconds(15, listOf(0), 5))
        assertEquals(15, SeekAccelerationLogic.stepSeconds(15, listOf(-4), 5))
    }

    @Test
    fun `累计秒数等于各次步长之和`() {
        assertEquals(0, SeekAccelerationLogic.accumulatedSeconds(15, 0, steps))
        // 第一次 15s（基础步长）
        assertEquals(15, SeekAccelerationLogic.accumulatedSeconds(15, 1, steps))
        // 第 2 拍 36s
        assertEquals(15 + 36, SeekAccelerationLogic.accumulatedSeconds(15, 2, steps))
        // 前 4 拍 = 15 + 36 × 3
        assertEquals(15 + 36 * 3, SeekAccelerationLogic.accumulatedSeconds(15, 4, steps))
        // 逐次累加，不能出现回退
        var previous = 0
        for (tickCount in 0..40) {
            val current = SeekAccelerationLogic.accumulatedSeconds(15, tickCount, steps)
            assertTrue("tickCount=$tickCount: $current 不应小于 $previous", current >= previous)
            previous = current
        }
    }

    @Test
    fun `长按按绝对位置步进，与播放器回读的位置无关`() {
        // 起点 10 分钟，快进 3 拍（15 + 36 + 36 = 87 秒）
        val accumulated = SeekAccelerationLogic.accumulatedSeconds(15, 3, steps)
        assertEquals(15 + 36 + 36, accumulated)
        // 目标 = 起点 + 累计，跟播放器当前报的位置无关（这是「连续 seek 时步长退化」的修法）
        assertEquals(
            600_000L + accumulated * 1000L,
            SeekAccelerationLogic.targetPositionMs(
                startPositionMs = 600_000L,
                accumulatedSeconds = accumulated,
                forward = true,
                durationMs = twoHours
            )
        )
    }

    @Test
    fun `目标位置被夹在全片范围内`() {
        // 正常前进
        assertEquals(75_000L, SeekAccelerationLogic.targetPositionMs(60_000L, 15, forward = true, durationMs = 600_000L))
        // 超过片长 → 收敛到片长
        assertEquals(600_000L, SeekAccelerationLogic.targetPositionMs(590_000L, 15, forward = true, durationMs = 600_000L))
        // 后退到负数 → 收敛到 0
        assertEquals(0L, SeekAccelerationLogic.targetPositionMs(5_000L, 15, forward = false, durationMs = 600_000L))
        // 时长未知（还没 READY）时只保证不小于 0
        assertEquals(75_000L, SeekAccelerationLogic.targetPositionMs(60_000L, 15, forward = true, durationMs = 0L))
        assertEquals(0L, SeekAccelerationLogic.targetPositionMs(5_000L, 15, forward = false, durationMs = 0L))
    }

    @Test
    fun `时间文案与进度条口径一致`() {
        assertEquals("00:00", SeekAccelerationLogic.formatDuration(0))
        assertEquals("00:15", SeekAccelerationLogic.formatDuration(15))
        assertEquals("01:00", SeekAccelerationLogic.formatDuration(60))
        assertEquals("59:59", SeekAccelerationLogic.formatDuration(3599))
        assertEquals("1:00:00", SeekAccelerationLogic.formatDuration(3600))
        assertEquals("1:02:05", SeekAccelerationLogic.formatDuration(3725))
        // 负数按 0 处理，不要在 HUD 上出现 -00:01
        assertEquals("00:00", SeekAccelerationLogic.formatDuration(-5))
    }

    @Test
    fun `增量文案带方向符号`() {
        // 分秒口径与进度条一致（分钟补零），所以是 +00:15 而不是 +0:15
        assertEquals("+00:15", SeekAccelerationLogic.formatDelta(15, forward = true))
        assertEquals("-01:30", SeekAccelerationLogic.formatDelta(90, forward = false))
        // 超过一小时会多出小时段
        assertEquals("+1:00:00", SeekAccelerationLogic.formatDelta(3600, forward = true))
    }

    @Test
    fun `方向枚举语义`() {
        assertTrue(SeekDirection.FORWARD.isForward)
        assertTrue(!SeekDirection.BACK.isForward)
    }
}
