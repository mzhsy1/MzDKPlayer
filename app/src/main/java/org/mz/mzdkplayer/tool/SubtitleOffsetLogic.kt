package org.mz.mzdkplayer.tool

import java.util.Locale
import kotlin.math.abs

/**
 * 字幕时间轴偏移的纯逻辑。
 *
 * 口径：**正值 = 字幕延后出现，负值 = 字幕提前出现**（与 VLC 的 `spu-delay` 一致）。
 * 两个内核都按这个口径实现，UI 只需关心正负号的含义。
 *
 * 这里只依赖 JDK，方便直接跑 JVM 单测（项目单测环境不允许触碰 `android.*`）。
 */
internal object SubtitleOffsetLogic {

    /** 每按一次 ± 的步进：0.5 秒。电视遥控器按键次数有限，步进太细没意义 */
    const val STEP_MS = 500

    /** 允许的偏移上限：±30 秒。片源与字幕帧率不同时最长也就差到这个量级 */
    const val MAX_MS = 30_000

    /** 把偏移量收敛到合法区间 */
    fun clamp(offsetMs: Int): Int = offsetMs.coerceIn(-MAX_MS, MAX_MS)

    /**
     * 在当前偏移量上按 [steps] 个步进调整，越界自动收敛到 ±[MAX_MS]。
     * steps 为正是「字幕延后」，为负是「字幕提前」。
     */
    fun step(currentMs: Int, steps: Int = 1): Int = clamp(currentMs + steps * STEP_MS)

    /**
     * 该方向还能不能再调（用于把按钮置灰）。
     * 已经顶到 ±[MAX_MS] 时返回 false。
     */
    fun canStep(currentMs: Int, steps: Int): Boolean {
        val current = clamp(currentMs)
        return step(current, steps) != current
    }

    /**
     * 展示用文案：带符号 + 一位小数，如 `+1.5` / `-0.5` / `0.0`。
     * 只负责数字部分，单位交给调用方拼多语言字串，避免在这里写死中文。
     */
    fun formatSeconds(offsetMs: Int): String {
        val seconds = clamp(offsetMs) / 1000.0
        val text = String.format(Locale.getDefault(), "%.1f", abs(seconds))
        return when {
            seconds > 0 -> "+$text"
            seconds < 0 -> "-$text"
            else -> text
        }
    }

    /** 偏移量是否已经归零 */
    fun isDefault(offsetMs: Int): Boolean = clamp(offsetMs) == 0

    /** 毫秒 → 微秒。字幕解析/播放内核内部统一用微秒 */
    fun toMicroseconds(offsetMs: Int): Long = clamp(offsetMs) * 1000L

    /**
     * 把一条字幕的起始时间平移 [shiftUs]（微秒），结果不会小于 0。
     *
     * 「提前」把开头几条字幕推到负数是没有意义的，收敛到 0 表示「一开头就显示」。
     */
    fun shiftStartTimeUs(startTimeUs: Long, shiftUs: Long): Long =
        (startTimeUs + shiftUs).coerceAtLeast(0L)
}
