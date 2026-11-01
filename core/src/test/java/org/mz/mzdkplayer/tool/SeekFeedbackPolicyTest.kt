package org.mz.mzdkplayer.tool

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mz.mzdkplayer.tool.logic.SeekFeedbackPolicy

/**
 * [SeekFeedbackPolicy] 的 JVM 单测。
 *
 * 关键契约：
 * 1. 默认设置（唤起控制栏）下单击仍然「箭头脉冲 + 弹出控制栏」，不改老行为；
 * 2. 关掉设置后单击只保留箭头脉冲，绝不唤起控制栏（控制栏会带整屏渐变压暗）；
 * 3. 长按（步进 ≥ 2 次）走 HUD，不触发单击反馈；
 * 4. 抽屉面板接管时什么都不做。
 */
class SeekFeedbackPolicyTest {

    @Test
    fun `默认值为开且保持原有单击反馈`() {
        assertTrue(SeekFeedbackPolicy.DEFAULT_SHOW_CONTROLS)

        val feedback = SeekFeedbackPolicy.shortPressFeedback(
            showControlsOnSeek = SeekFeedbackPolicy.DEFAULT_SHOW_CONTROLS,
            tickCount = 1,
            panelTakenOver = false
        )
        assertTrue("单击应闪一下箭头", feedback.pulse)
        assertTrue("默认设置下单击应弹出控制栏", feedback.showControls)
    }

    @Test
    fun `关闭设置后单击只闪箭头不弹控制栏`() {
        val feedback = SeekFeedbackPolicy.shortPressFeedback(
            showControlsOnSeek = false,
            tickCount = 1,
            panelTakenOver = false
        )
        assertTrue("箭头脉冲与设置项无关，仍然保留", feedback.pulse)
        assertFalse("设置关闭时不应唤起控制栏", feedback.showControls)
    }

    @Test
    fun `长按不触发单击反馈`() {
        listOf(2, 3, 10, Int.MAX_VALUE).forEach { ticks ->
            val feedback = SeekFeedbackPolicy.shortPressFeedback(
                showControlsOnSeek = true,
                tickCount = ticks,
                panelTakenOver = false
            )
            assertFalse("<$ticks> 次步进属于长按，不应闪箭头", feedback.pulse)
            assertFalse("<$ticks> 次步进属于长按，不应弹控制栏", feedback.showControls)
        }
    }

    @Test
    fun `未拿到步进计数时按单击保守处理`() {
        // 实际松手时至少步进过一次，这里只固定旧实现的行为：tickCount = 0 也当单击
        val feedback = SeekFeedbackPolicy.shortPressFeedback(
            showControlsOnSeek = true,
            tickCount = 0,
            panelTakenOver = false
        )
        assertTrue(feedback.pulse)
        assertTrue(feedback.showControls)
    }

    @Test
    fun `抽屉面板接管时不做任何反馈`() {
        val feedback = SeekFeedbackPolicy.shortPressFeedback(
            showControlsOnSeek = true,
            tickCount = 1,
            panelTakenOver = true
        )
        assertFalse("面板接管时松手不应闪箭头", feedback.pulse)
        assertFalse("面板接管时松手不应弹控制栏", feedback.showControls)
    }
}
