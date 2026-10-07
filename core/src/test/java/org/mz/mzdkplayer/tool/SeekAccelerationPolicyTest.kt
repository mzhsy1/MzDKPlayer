package org.mz.mzdkplayer.tool

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mz.mzdkplayer.tool.logic.SeekAccelerationPolicy

/**
 * [SeekAccelerationPolicy] 的 JVM 单测。
 *
 * 关键契约：
 * 1. 三档（关闭 / 标准 / 快速），默认「标准」，脏数据一律回退默认；
 * 2. `next` 在档位里循环（电视端「按一次换一个」的设置项不能在最后一档卡住）；
 * 3. 步长链按片长换算：单调不减、夹在 `[MIN_STEP_SECONDS, MAX_STEP_SECONDS]`、片长越长每档越大；
 * 4. 关闭档返回空链（调用方据此退回基础步长），片长未知时也要给出可用的兜底链。
 */
class SeekAccelerationPolicyTest {

    private val halfHour = 30 * 60 * 1000L
    private val twoHours = 2 * 60 * 60 * 1000L
    private val oneDay = 24 * 60 * 60 * 1000L

    @Test
    fun `默认档位是标准`() {
        assertEquals(SeekAccelerationPolicy.LEVEL_STANDARD, SeekAccelerationPolicy.DEFAULT_LEVEL)
        assertEquals(3, SeekAccelerationPolicy.LEVEL_COUNT)
    }

    @Test
    fun `非法值一律回退默认档位`() {
        listOf(-1, 3, 5, 100, Int.MAX_VALUE, Int.MIN_VALUE).forEach { raw ->
            assertEquals(
                "<$raw> 应回退默认",
                SeekAccelerationPolicy.DEFAULT_LEVEL,
                SeekAccelerationPolicy.normalize(raw)
            )
        }
    }

    @Test
    fun `合法档位原样返回`() {
        (0 until SeekAccelerationPolicy.LEVEL_COUNT).forEach { level ->
            assertEquals(level, SeekAccelerationPolicy.normalize(level))
        }
    }

    @Test
    fun `下一档按顺序前进且末档回到首档`() {
        assertEquals(
            SeekAccelerationPolicy.LEVEL_STANDARD,
            SeekAccelerationPolicy.next(SeekAccelerationPolicy.LEVEL_OFF)
        )
        assertEquals(
            SeekAccelerationPolicy.LEVEL_FAST,
            SeekAccelerationPolicy.next(SeekAccelerationPolicy.LEVEL_STANDARD)
        )
        assertEquals(
            SeekAccelerationPolicy.LEVEL_OFF,
            SeekAccelerationPolicy.next(SeekAccelerationPolicy.LEVEL_FAST)
        )
        // 脏值先收敛再前进：非法值 → 默认（标准）→ 快速
        assertEquals(SeekAccelerationPolicy.LEVEL_FAST, SeekAccelerationPolicy.next(99))
    }

    @Test
    fun `关闭档返回空链且不算开启加速`() {
        assertTrue(
            SeekAccelerationPolicy.stepsFor(twoHours, SeekAccelerationPolicy.LEVEL_OFF).isEmpty()
        )
        assertTrue(!SeekAccelerationPolicy.isEnabled(SeekAccelerationPolicy.LEVEL_OFF))
        assertTrue(SeekAccelerationPolicy.isEnabled(SeekAccelerationPolicy.LEVEL_STANDARD))
        assertTrue(SeekAccelerationPolicy.isEnabled(SeekAccelerationPolicy.LEVEL_FAST))
    }

    @Test
    fun `步长链单调不减且都在上下限内`() {
        val durations = listOf(0L, 5 * 60_000L, halfHour, twoHours, oneDay)
        listOf(SeekAccelerationPolicy.LEVEL_STANDARD, SeekAccelerationPolicy.LEVEL_FAST).forEach { level ->
            durations.forEach { duration ->
                val steps = SeekAccelerationPolicy.stepsFor(duration, level)
                assertTrue("level=$level duration=$duration 不能为空", steps.isNotEmpty())
                steps.forEach { step ->
                    assertTrue(
                        "level=$level duration=$duration step=$step 越界",
                        step in SeekAccelerationPolicy.MIN_STEP_SECONDS..SeekAccelerationPolicy.MAX_STEP_SECONDS
                    )
                }
                steps.zipWithNext().forEach { (previous, current) ->
                    assertTrue(
                        "level=$level duration=$duration 应单调不减：$steps",
                        current >= previous
                    )
                }
            }
        }
    }

    @Test
    fun `片长越长每档跳得越多`() {
        val short = SeekAccelerationPolicy.stepsFor(10 * 60_000L, SeekAccelerationPolicy.LEVEL_STANDARD)
        val long = SeekAccelerationPolicy.stepsFor(twoHours, SeekAccelerationPolicy.LEVEL_STANDARD)
        assertEquals(short.size, long.size)
        long.zip(short).forEach { (longStep, shortStep) ->
            assertTrue("$longStep 应不小于 $shortStep", longStep >= shortStep)
        }
        assertTrue("长片的第一档应明显大于短片", long.first() > short.first())
    }

    @Test
    fun `快速档不小于标准档`() {
        listOf(halfHour, twoHours).forEach { duration ->
            val standard = SeekAccelerationPolicy.stepsFor(duration, SeekAccelerationPolicy.LEVEL_STANDARD)
            val fast = SeekAccelerationPolicy.stepsFor(duration, SeekAccelerationPolicy.LEVEL_FAST)
            fast.zip(standard).forEach { (fastStep, standardStep) ->
                assertTrue("$fastStep 应不小于 $standardStep", fastStep >= standardStep)
            }
        }
    }

    @Test
    fun `片长未知时给出可用的兜底链`() {
        listOf(0L, -1L).forEach { duration ->
            listOf(SeekAccelerationPolicy.LEVEL_STANDARD, SeekAccelerationPolicy.LEVEL_FAST).forEach { level ->
                val steps = SeekAccelerationPolicy.stepsFor(duration, level)
                assertTrue("duration=$duration level=$level 兜底链不能为空", steps.isNotEmpty())
                assertTrue("duration=$duration level=$level 兜底链必须为正", steps.all { it > 0 })
            }
        }
    }

    @Test
    fun `超长片也不会一步跳掉半部电影`() {
        val steps = SeekAccelerationPolicy.stepsFor(oneDay, SeekAccelerationPolicy.LEVEL_FAST)
        assertEquals(SeekAccelerationPolicy.MAX_STEP_SECONDS, steps.last())
    }

    @Test
    fun `默认档位的兜底链与标准档一致`() {
        assertEquals(
            SeekAccelerationPolicy.stepsFor(twoHours, SeekAccelerationPolicy.LEVEL_STANDARD),
            SeekAccelerationPolicy.defaultSteps(twoHours)
        )
    }
}
