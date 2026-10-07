package org.mz.mzdkplayer.ui.videoplayer.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import org.mz.mzdkplayer.R
import org.mz.mzdkplayer.tool.logic.SeekAccelerationLogic
import org.mz.mzdkplayer.tool.logic.SeekAccelerationPolicy
import org.mz.mzdkplayer.tool.logic.SeekDirection

/** 松手后 HUD 再停留多久（毫秒）。太短看不清，太长会挡字幕 */
const val SEEK_HUD_LINGER_MS = 900L

/**
 * 长按快进/快退的状态。
 *
 * 由播放页在按下左/右键时 [begin]，每次步进 [onTick]，松手后 [hide]。
 * HUD 只在**真正长按**（步进 ≥ 2 次）时出现：单击仍然沿用原来的「箭头脉冲 + 弹出控制栏」，
 * 两种反馈不会同时出现在屏幕中央。
 */
class SeekHoldState internal constructor() {

    /** HUD 是否可见 */
    var visible by mutableStateOf(false)
        private set

    /** 本次长按的方向，默认快进（不可见时无意义） */
    var direction by mutableStateOf(SeekDirection.FORWARD)
        private set

    /** 本次长按累计跳过的秒数 */
    var accumulatedSeconds by mutableIntStateOf(0)
        private set

    /** 本次长按开始时的播放位置，用来算 HUD 上的目标时间 */
    var startPositionMs by mutableLongStateOf(0L)
        private set

    /** 片长（毫秒），0 表示还没拿到 */
    var durationMs by mutableLongStateOf(0L)
        private set

    /** 本次长按已经步进的次数：1 表示只跳了一次（短按） */
    var tickCount by mutableIntStateOf(0)
        private set

    /** 设置项里的基础步长（秒），加速档位在它之上放大 */
    var baseSeconds by mutableIntStateOf(15)
        private set

    /**
     * 本次长按使用的加速档位（设置项「长按快进挡位」）。
     * 长按开始时固定，中途改设置不影响已经在跑的这一轮。
     */
    var accelerationLevel by mutableIntStateOf(SeekAccelerationPolicy.DEFAULT_LEVEL)
        private set

    /**
     * 本次长按的步长链：按片长与档位换算出的「每一档跳多少秒」，直接算给 `SeekAccelerationLogic`。
     * 片长在 [begin] 时定格，所以整轮长按的档位不会因为播放位置变化而漂移。
     */
    val steps: List<Int>
        get() = SeekAccelerationPolicy.stepsFor(durationMs, accelerationLevel)

    /** HUD 上的目标时间 = 起点 ± 累计步长，已收敛到片长范围内 */
    val targetPositionMs: Long
        get() = SeekAccelerationLogic.targetPositionMs(
            startPositionMs = startPositionMs,
            accumulatedSeconds = accumulatedSeconds,
            forward = direction.isForward,
            durationMs = durationMs
        )

    /** 按下时调用：记录起点，重置累计值。第一次步进由调用方紧接着执行 */
    fun begin(
        direction: SeekDirection,
        baseSeconds: Int,
        accelerationLevel: Int,
        startPositionMs: Long,
        durationMs: Long
    ) {
        this.direction = direction
        this.baseSeconds = baseSeconds
        this.accelerationLevel = SeekAccelerationPolicy.normalize(accelerationLevel)
        this.startPositionMs = startPositionMs.coerceAtLeast(0L)
        this.durationMs = durationMs.coerceAtLeast(0L)
        accumulatedSeconds = 0
        tickCount = 0
        visible = false
    }

    /** 每次步进后调用。[tickIndex] 从 0 开始，第二次步进才把 HUD 亮出来 */
    fun onTick(tickIndex: Int, accumulatedSeconds: Int) {
        this.accumulatedSeconds = accumulatedSeconds
        tickCount = tickIndex + 1
        if (tickCount > 1) visible = true
    }

    fun hide() {
        visible = false
    }
}

@Composable
fun rememberSeekHoldState() = remember { SeekHoldState() }

/**
 * 快进/快退 HUD：方向箭头 + 累计跳过量 + 目标时间/片长 + 一条细进度条。
 *
 * 只在遥控器长按左右键、控制栏未弹出时显示，替代「不知道跳了多少」的纯图标脉冲。
 * 文案全部是数字和符号，不需要多语言资源。
 */
@Composable
fun VideoPlayerSeekHud(
    state: SeekHoldState,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = state.visible,
        modifier = modifier,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .background(Color.Black.copy(alpha = 0.62f), RoundedCornerShape(12.dp))
                .padding(horizontal = 28.dp, vertical = 18.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(
                        id = if (state.direction.isForward) {
                            R.drawable.baseline_arrow_forward_ios_24
                        } else {
                            R.drawable.baseline_arrow_back_ios_new_24
                        }
                    ),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(26.dp)
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = SeekAccelerationLogic.formatDelta(
                        seconds = state.accumulatedSeconds,
                        forward = state.direction.isForward
                    ),
                    color = Color.White,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(Modifier.height(8.dp))
            Text(
                text = "${SeekAccelerationLogic.formatDuration(state.targetPositionMs / 1000)} / " +
                    SeekAccelerationLogic.formatDuration(state.durationMs / 1000),
                color = Color.White.copy(alpha = 0.78f),
                fontSize = 16.sp
            )

            Spacer(Modifier.height(10.dp))
            Box(
                Modifier
                    .width(320.dp)
                    .height(4.dp)
                    .background(Color.White.copy(alpha = 0.25f), RoundedCornerShape(2.dp))
            ) {
                val fraction = if (state.durationMs > 0) {
                    (state.targetPositionMs.toFloat() / state.durationMs.toFloat()).coerceIn(0f, 1f)
                } else {
                    0f
                }
                Box(
                    Modifier
                        .fillMaxWidth(fraction)
                        .fillMaxHeight()
                        .background(Color.White, RoundedCornerShape(2.dp))
                )
            }
        }
    }
}
