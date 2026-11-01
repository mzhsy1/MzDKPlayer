package org.mz.mzdkplayer.tool.logic

/**
 * 遥控器左右键**单击**之后的反馈策略（纯逻辑，单测见 `SeekFeedbackPolicyTest`）。
 *
 * 「单击」= 这一轮只跳了一步（[ShortPressFeedback] 的判定用 `tickCount <= 1`）且没有被抽屉面板接管。
 * 长按（步进 ≥ 2 次）由 `VideoPlayerSeekHud` 的迷你提示条负责，不在这里判断。
 *
 * 为什么要拆这一层：底部控制栏自带一整屏「黑 → 透明」的渐变压暗，而左右键单击只是为了跳几秒，
 * 弹控制栏的观感代价明显大于收益。把「单击的反馈组合」收敛成纯逻辑后，设置项（默认开）
 * 与长按路径可以各自独立演进，回归时也能用单测直接钉住。
 */
object SeekFeedbackPolicy {

    /** 设置项「方向键快进唤起底部进度条」的默认值：开，保持老用户熟悉的行为 */
    const val DEFAULT_SHOW_CONTROLS = true

    /**
     * 单击后的反馈组合。
     *
     * @param pulse 是否闪一下方向箭头（屏幕中央的小图标，不压暗画面）
     * @param showControls 是否唤起底部控制栏（连带整屏的黑色渐变遮罩）
     */
    data class ShortPressFeedback(
        val pulse: Boolean,
        val showControls: Boolean
    )

    /**
     * @param showControlsOnSeek 设置项：单击左右键是否唤起底部控制栏
     * @param tickCount 本次按下已经步进的次数，1 表示只跳了一步
     * @param panelTakenOver 抽屉面板是否接管了这次按键（接管时播放页什么都不该做）
     */
    fun shortPressFeedback(
        showControlsOnSeek: Boolean,
        tickCount: Int,
        panelTakenOver: Boolean
    ): ShortPressFeedback {
        // 与旧实现保持一致：只有步进 ≥ 2 次才算长按，其余（含没拿到计数的 0）都按单击处理，
        // 宁可多闪一下箭头，也不要在松手时毫无反馈
        val shortPress = tickCount <= 1 && !panelTakenOver
        return ShortPressFeedback(
            pulse = shortPress,
            showControls = shortPress && showControlsOnSeek
        )
    }
}
