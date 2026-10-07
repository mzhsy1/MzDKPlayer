package org.mz.mzdkplayer.tool.logic

/**
 * 电视端长按左右键时「快进/快退步长逐级放大」的档位策略（纯逻辑，单测见 `SeekAccelerationPolicyTest`）。
 *
 * 档位不让用户填倍数，而是**按片长自动换算**：每一档跳多长 = 片长的千分比。
 * 这样 10 分钟的短片每档跳几秒、2 小时的电影每档跳几十秒，长按的手感不会随时长跑偏
 * —— 原来那套「设置里的基础步长 × 固定倍数」在短片上一跳就过半、在长片上又慢得没感觉。
 *
 * 强度只分三档（关闭 / 标准 / 快速）：遥控器上按不了那么多下，也不值得让用户理解倍数。
 */
object SeekAccelerationPolicy {

    /** 关闭：长按只按设置里的基础步长连续跳，不放大 */
    const val LEVEL_OFF = 0

    /** 标准：每档约为片长的 0.5% / 1% / 2% / 5% */
    const val LEVEL_STANDARD = 1

    /** 快速：每档约为片长的 1% / 2% / 5% / 10% */
    const val LEVEL_FAST = 2

    /** 档位总数（关闭 + 标准 + 快速） */
    const val LEVEL_COUNT = 3

    /** 默认档位：标准 */
    const val DEFAULT_LEVEL = LEVEL_STANDARD

    /** 单步秒数下限：片头很短的视频也要跳得动 */
    const val MIN_STEP_SECONDS = 5

    /** 单步秒数上限：一步跳掉整集就过头了 */
    const val MAX_STEP_SECONDS = 300

    /** 标准档的千分比链（整型运算，避免浮点误差） */
    private val STANDARD_RATIOS = listOf(5, 10, 20, 50)

    /** 快速档的千分比链 */
    private val FAST_RATIOS = listOf(10, 20, 50, 100)

    /** 片长未知（还没 READY / 直播流）时标准档的兜底步长（秒） */
    private val FALLBACK_STANDARD = listOf(10, 20, 30, 60)

    /** 片长未知时快速档的兜底步长（秒） */
    private val FALLBACK_FAST = listOf(15, 30, 60, 120)

    /** 脏数据（旧版本残留 / 被手改的存储）收敛到合法档位，非法值回退 [DEFAULT_LEVEL] */
    fun normalize(level: Int): Int =
        if (level in 0 until LEVEL_COUNT) level else DEFAULT_LEVEL

    /** 下一档（电视端设置项是「按一次换一个」的交互）；到末尾回到第一档 */
    fun next(current: Int): Int = (normalize(current) + 1) % LEVEL_COUNT

    /** 该档位是否开启长按加速（关闭档等价于「只按基础步长连续跳」） */
    fun isEnabled(level: Int): Boolean = normalize(level) != LEVEL_OFF

    /**
     * 片长 → 每一档的步长（秒）。关闭档返回空列表，调用方据此退回基础步长。
     *
     * 结果单调不减，且每一项都夹在 `[MIN_STEP_SECONDS, MAX_STEP_SECONDS]` 内；
     * 片长未知（<= 0）时给一组保守的兜底值，保证长按仍然有加速。
     */
    fun stepsFor(durationMs: Long, level: Int): List<Int> {
        val normalized = normalize(level)
        if (normalized == LEVEL_OFF) return emptyList()

        val isFast = normalized == LEVEL_FAST
        val ratios = if (isFast) FAST_RATIOS else STANDARD_RATIOS
        if (durationMs <= 0L) return if (isFast) FALLBACK_FAST else FALLBACK_STANDARD

        val durationSeconds = durationMs / 1000L
        return ratios.map { ratio ->
            (durationSeconds * ratio / 1000L).toInt()
                .coerceIn(MIN_STEP_SECONDS, MAX_STEP_SECONDS)
        }
    }

    /** 默认档位在已知片长下的步长链，给单测和调用方兜底用 */
    fun defaultSteps(durationMs: Long): List<Int> = stepsFor(durationMs, DEFAULT_LEVEL)
}
