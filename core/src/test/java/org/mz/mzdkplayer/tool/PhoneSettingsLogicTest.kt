package org.mz.mzdkplayer.tool

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mz.mzdkplayer.tool.logic.PhoneSettingsLogic

/**
 * 手机端设置档位收敛与滑动换算的纯逻辑。
 *
 * 这两处都直接决定「播到一半的设置值被手改后会不会把播放页带崩」：
 * 档位收敛保证拿到的永远是合法值，滑动换算保证百分比不会越界。
 */
class PhoneSettingsLogicTest {

    // ---------------- 控制栏自动隐藏 ----------------

    @Test
    fun `控制栏隐藏秒数只接受固定档位`() {
        PhoneSettingsLogic.CONTROLS_HIDE_OPTIONS.forEach { option ->
            assertEquals(option, PhoneSettingsLogic.normalizeControlsHideSeconds(option))
        }
    }

    @Test
    fun `非法隐藏秒数回退默认`() {
        assertEquals(
            PhoneSettingsLogic.DEFAULT_CONTROLS_HIDE_SECONDS,
            PhoneSettingsLogic.normalizeControlsHideSeconds(0),
        )
        assertEquals(
            PhoneSettingsLogic.DEFAULT_CONTROLS_HIDE_SECONDS,
            PhoneSettingsLogic.normalizeControlsHideSeconds(-5),
        )
        assertEquals(
            PhoneSettingsLogic.DEFAULT_CONTROLS_HIDE_SECONDS,
            PhoneSettingsLogic.normalizeControlsHideSeconds(7),
        )
        assertEquals(
            PhoneSettingsLogic.DEFAULT_CONTROLS_HIDE_SECONDS,
            PhoneSettingsLogic.normalizeControlsHideSeconds(60),
        )
    }

    @Test
    fun `默认隐藏秒数本身是合法档位`() {
        assertTrue(
            PhoneSettingsLogic.DEFAULT_CONTROLS_HIDE_SECONDS in PhoneSettingsLogic.CONTROLS_HIDE_OPTIONS
        )
        assertEquals(
            PhoneSettingsLogic.DEFAULT_CONTROLS_HIDE_SECONDS,
            PhoneSettingsLogic.normalizeControlsHideSeconds(
                PhoneSettingsLogic.DEFAULT_CONTROLS_HIDE_SECONDS
            ),
        )
    }

    @Test
    fun `隐藏秒数档位在上下限之间且递增`() {
        val options = PhoneSettingsLogic.CONTROLS_HIDE_OPTIONS
        assertEquals(PhoneSettingsLogic.MIN_CONTROLS_HIDE_SECONDS, options.first())
        assertEquals(PhoneSettingsLogic.MAX_CONTROLS_HIDE_SECONDS, options.last())
        assertEquals(options, options.sorted())
    }

    // ---------------- 长按倍速 ----------------

    @Test
    fun `长按倍速只接受固定档位`() {
        PhoneSettingsLogic.LONG_PRESS_SPEED_OPTIONS.forEach { option ->
            assertEquals(option, PhoneSettingsLogic.normalizeLongPressSpeed(option), 0.001f)
        }
    }

    @Test
    fun `非法长按倍速回退默认`() {
        assertEquals(
            PhoneSettingsLogic.DEFAULT_LONG_PRESS_SPEED,
            PhoneSettingsLogic.normalizeLongPressSpeed(1.0f),
            0.001f,
        )
        assertEquals(
            PhoneSettingsLogic.DEFAULT_LONG_PRESS_SPEED,
            PhoneSettingsLogic.normalizeLongPressSpeed(10f),
            0.001f,
        )
        assertEquals(
            PhoneSettingsLogic.DEFAULT_LONG_PRESS_SPEED,
            PhoneSettingsLogic.normalizeLongPressSpeed(0f),
            0.001f,
        )
    }

    @Test
    fun `长按倍速文案与播放页浮层同一口径`() {
        assertEquals("2.0x", PhoneSettingsLogic.formatLongPressSpeed(2f))
        assertEquals("3.0x", PhoneSettingsLogic.formatLongPressSpeed(3f))
        // 脏数据先收敛再显示，不会出现 "7.0x" 这种界面上选不到的档位
        assertEquals("2.0x", PhoneSettingsLogic.formatLongPressSpeed(7f))
    }

    // ---------------- 上下滑动调亮度 / 音量 ----------------

    @Test
    fun `向上滑增大 向下滑减小`() {
        assertEquals(
            80,
            PhoneSettingsLogic.applyVerticalSwing(startPercent = 50, totalDyPx = -300f, screenHeightPx = 1000),
        )
        assertEquals(
            20,
            PhoneSettingsLogic.applyVerticalSwing(startPercent = 50, totalDyPx = 300f, screenHeightPx = 1000),
        )
    }

    @Test
    fun `滑满整屏约改变一屏的量`() {
        assertEquals(
            100,
            PhoneSettingsLogic.applyVerticalSwing(startPercent = 0, totalDyPx = -1000f, screenHeightPx = 1000),
        )
        assertEquals(
            0,
            PhoneSettingsLogic.applyVerticalSwing(startPercent = 100, totalDyPx = 1000f, screenHeightPx = 1000),
        )
    }

    @Test
    fun `结果收敛在 0 到 100 之间`() {
        assertEquals(
            100,
            PhoneSettingsLogic.applyVerticalSwing(startPercent = 90, totalDyPx = -5000f, screenHeightPx = 1000),
        )
        assertEquals(
            0,
            PhoneSettingsLogic.applyVerticalSwing(startPercent = 10, totalDyPx = 5000f, screenHeightPx = 1000),
        )
    }

    @Test
    fun `没有位移时保持原值`() {
        assertEquals(
            42,
            PhoneSettingsLogic.applyVerticalSwing(startPercent = 42, totalDyPx = 0f, screenHeightPx = 1000),
        )
    }

    @Test
    fun `屏幕高度非法时原样返回并收敛`() {
        assertEquals(
            42,
            PhoneSettingsLogic.applyVerticalSwing(startPercent = 42, totalDyPx = -123f, screenHeightPx = 0),
        )
        assertEquals(
            100,
            PhoneSettingsLogic.applyVerticalSwing(startPercent = 300, totalDyPx = 0f, screenHeightPx = 0),
        )
    }
}
