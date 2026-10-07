package org.mz.mzdkplayer.tool.logic

/**
 * 长按遥控器左右键时「快进/快退秒数越按越大」的纯逻辑（类似 Kodi 的长按加速定位）。
 *
 * 为什么不是靠 `KeyEvent.repeatCount`：
 * 各品牌遥控器的按键重复率差别很大（有的首次重复延迟 400ms、之后 30ms 一个，有的干脆只发一次
 * ACTION_DOWN），直接拿重复次数当加速依据会导致不同电视上手感完全不同。
 * 这里改成**固定节拍 + 档位放大**：按下后第一次立刻跳一个基础步长（保证短按一定生效），之后每
 * [TICK_INTERVAL_MS] 毫秒再跳一次，每 [TICKS_PER_TIER] 次进一档。
 *
 * 每一档跳多少由 [SeekAccelerationPolicy.stepsFor] 按片长算出来（倍速链已经换成「片长的千分比」），
 * 本类只做「步长链 + 基础步长 + 第几拍 → 秒数」的换算，不关心档位是怎么存的。
 *
 * 只依赖 JDK，方便直接跑 JVM 单测（项目单测环境不允许触碰 `android.*`）。
 */
object SeekAccelerationLogic {

    /**
     * 长按时的步进节拍（毫秒）。
     *
     * 取值偏保守：电视端 seek 之后往往要重新缓冲，节拍太快会让「长按」变成连续卡顿。
     */
    const val TICK_INTERVAL_MS = 250L

    /** 每个加速档位持续的步进次数：3 次 × 250ms ≈ 每 0.75 秒升一档 */
    const val TICKS_PER_TIER = 3

    /**
     * 第 [tickIndex] 次步进（从 0 开始）的秒数。
     *
     * 第 0 拍（按下瞬间）固定是设置项里的「快进/快退时长」，保证短按的行为不受加速影响；
     * 第 1 拍起按 [steps]（片长换算出的档位链）放大，每 [TICKS_PER_TIER] 拍进一档，
     * 走到链尾就停在最后一档。结果取 `max(基础步长, 当前档位)`，保证单调不减
     * （用户把基础步长调得比档位还大时不会出现「先跳大再跳小」）。
     *
     * [steps] 为空（关闭档 / 脏数据）时一直按基础步长走，不会算出 0 步长。
     */
    fun stepSeconds(baseSeconds: Int, steps: List<Int>, tickIndex: Int): Int {
        val base = baseSeconds.coerceAtLeast(1)
        if (tickIndex <= 0 || steps.isEmpty()) return base
        val tier = (tickIndex - 1) / TICKS_PER_TIER
        val target = steps[tier.coerceAtMost(steps.lastIndex)].coerceAtLeast(1)
        // 只对「档位换算出的值」封顶，用户手设的基础步长是多少就是多少
        return maxOf(base, target)
    }

    /**
     * 前 [tickCount] 次步进累计跳过的秒数（tickCount 为 0 时返回 0）。
     *
     * HUD 上的「+2:15」和目标位置都基于它，保证显示值等于真正 seek 出去的量。
     */
    fun accumulatedSeconds(baseSeconds: Int, tickCount: Int, steps: List<Int>): Int {
        if (tickCount <= 0) return 0
        var total = 0L
        for (tickIndex in 0 until tickCount) {
            total += stepSeconds(baseSeconds, steps, tickIndex)
        }
        return total.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
    }

    /**
     * 从 [startPositionMs] 出发累计前进/后退 [accumulatedSeconds] 秒后的目标位置，
     * 收敛在 `[0, durationMs]` 内（时长未知时只保证不小于 0）。
     *
     * 长按 seek 一律**直接定位到算出来的绝对位置**，而不是调 `seekForward` 让播放器自己加：
     * 连续 seek 时播放器还在缓冲，`currentPosition` 往往还是上一拍的旧值，
     * 累加出来就是「每次都只跳基础步长」，看起来像完全没加速。
     */
    fun targetPositionMs(
        startPositionMs: Long,
        accumulatedSeconds: Int,
        forward: Boolean,
        durationMs: Long
    ): Long {
        val delta = accumulatedSeconds.coerceAtLeast(0).toLong() * 1000L
        val raw = if (forward) startPositionMs + delta else startPositionMs - delta
        val upper = if (durationMs > 0) durationMs else Long.MAX_VALUE
        return raw.coerceIn(0L, upper)
    }

    /**
     * 秒 → `MM:SS` / `H:MM:SS`，与播放页进度条上的时间写法保持一致。
     * 只有数字和冒号，不涉及多语言。
     */
    fun formatDuration(totalSeconds: Long): String {
        val seconds = totalSeconds.coerceAtLeast(0L)
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        val secs = seconds % 60
        return if (hours > 0) {
            "$hours:${minutes.pad2()}:${secs.pad2()}"
        } else {
            "${minutes.pad2()}:${secs.pad2()}"
        }
    }

    /** 带方向的增量文案，如 `+1:30` / `-0:15`。只负责符号和数字，交给 UI 直接显示 */
    fun formatDelta(seconds: Int, forward: Boolean): String =
        (if (forward) "+" else "-") + formatDuration(seconds.toLong())

    private fun Long.pad2(): String = toString().padStart(2, '0')
}

/** 长按 seek 的方向。放在这边是为了让 HUD 与逻辑层共用同一套语义 */
enum class SeekDirection {
    FORWARD,
    BACK;

    val isForward: Boolean get() = this == FORWARD
}
