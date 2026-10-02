package org.mz.mzdkplayer.tool.logic

import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * 手机端设置里「取值必须落在固定档位上」的那几项（只依赖 JDK，可直接单测）。
 *
 * 手机端的设置项分两种存法：
 * - **布尔开关**（左右滑动调亮度、长按倍速……）直接存原值，没什么可算的；
 * - **档位选择**（控制栏自动隐藏秒数、长按倍速值）存的是数值，但界面上只能选固定几档，
 *   手改 prefs 或历史脏数据可能落到档位之外 —— 收敛规则写在这里，UI 与播放页共用同一份。
 *
 * 单测见 `core/src/test/.../PhoneSettingsLogicTest.kt`。
 */
object PhoneSettingsLogic {

    // ---------------- 控制栏自动隐藏 ----------------

    /**
     * 控制栏自动隐藏可选的秒数档位。
     *
     * 默认 5 秒是第七阶段播放页写死的值，这里只是把它变成可配置项、并保持默认不变。
     */
    val CONTROLS_HIDE_OPTIONS = listOf(3, 5, 8, 10)

    /** 默认隐藏延迟（秒）：与第七阶段播放页的硬编码值一致 */
    const val DEFAULT_CONTROLS_HIDE_SECONDS = 5

    /** 单位是秒 */
    const val MIN_CONTROLS_HIDE_SECONDS = 3
    const val MAX_CONTROLS_HIDE_SECONDS = 10

    /** 把存储里的值收敛到 [CONTROLS_HIDE_OPTIONS] 里的某一档，非法值回退默认 */
    fun normalizeControlsHideSeconds(value: Int): Int =
        if (value in CONTROLS_HIDE_OPTIONS) value else DEFAULT_CONTROLS_HIDE_SECONDS

    // ---------------- 长按倍速 ----------------

    /**
     * 长按画面时临时切换到的倍速档位。
     *
     * 只给 2 倍与 3 倍：更高（4 倍以上）在手机上基本只能看画面，没有实用价值；
     * 而播放页浮层里的 [PhonePlayerLogic.speedOptions] 是「整段视频的倍速」，两者用途不同。
     */
    val LONG_PRESS_SPEED_OPTIONS = listOf(2f, 3f)

    /** 默认长按倍速 */
    const val DEFAULT_LONG_PRESS_SPEED = 2f

    /** 把存储里的值收敛到 [LONG_PRESS_SPEED_OPTIONS] 里的某一档，非法值回退默认 */
    fun normalizeLongPressSpeed(value: Float): Float =
        LONG_PRESS_SPEED_OPTIONS.firstOrNull { abs(it - value) < 0.01f } ?: DEFAULT_LONG_PRESS_SPEED

    /** 倍速文案，与播放页浮层里的 `"${speed}x"` 保持同一口径 */
    fun formatLongPressSpeed(value: Float): String = "${normalizeLongPressSpeed(value)}x"

    // ---------------- 左右滑动调亮度 / 音量 ----------------

    /**
     * 手势调音量的最小可闻步进（百分比）。
     *
     * 手指在整块屏幕上滑完一次（按 1 个屏幕高度估算）大约改变 100%，
     * 所以只做「每个像素改变多少」的换算，步进本身由手指位移决定；
     * 这里只用来把「一次滑完全屏」的上限写死，避免 hyper-sensitive 设备上跳变。
     */
    const val GESTURE_FULL_SCREEN_SWING_PERCENT = 100

    /**
     * 计算滑动亮度 / 音量后的百分比结果。
     *
     * @param startPercent 手势开始时的百分比（0..100）
     * @param totalDyPx 手势累计的纵向位移（屏幕坐标向下为正，因此上滑是负数）
     * @param screenHeightPx 屏幕高度（像素）
     * @return 收敛到 0..100 的百分比
     */
    fun applyVerticalSwing(
        startPercent: Int,
        totalDyPx: Float,
        screenHeightPx: Int,
    ): Int {
        if (screenHeightPx <= 0) return startPercent.coerceIn(0, 100)
        // 向上滑（dy 为负）应该变大，所以取负号
        val delta = -totalDyPx / screenHeightPx * GESTURE_FULL_SCREEN_SWING_PERCENT
        // 用四舍五入而不是截断：浮点误差会让 50 - 30 算成 19.999998，
        // 截断之后每滑一次都少 1%，滑几下就能看出来「没滑到位」
        return (startPercent + delta).coerceIn(0f, 100f).roundToInt()
    }
}
