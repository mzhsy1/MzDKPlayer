package org.mz.mzdkplayer.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import org.mz.mzdkplayer.R
import org.mz.mzdkplayer.player.core.MzAspectRatio

/**
 * 播放相关选项的展示文案。
 *
 * 设置页与播放页浮层（画面比例、播放完成动作）共用这一份实现：
 * 同一个选项在两个入口必须显示同一套文案，且都要跟着 App 语言走。
 * 往 MzAspectRatio 里加新枚举时会触发 when 分支不穷尽的编译错误，
 * 强制在这里补上对应字串。
 */

// 播放完成动作的可选值个数：0 循环播放 / 1 播放暂停 / 2 播放下一个
const val VIDEO_FINISH_ACTION_COUNT = 3

/** 把设置里存的 MzAspectRatio 枚举名还原成枚举；脏数据或枚举被删时回退 FIT，不抛异常。 */
fun aspectRatioFromName(name: String): MzAspectRatio =
    runCatching { MzAspectRatio.valueOf(name) }.getOrDefault(MzAspectRatio.FIT)

@Composable
fun formatAspectRatio(ratio: MzAspectRatio): String = when (ratio) {
    MzAspectRatio.FIT -> stringResource(R.string.ratio_auto_fit)
    MzAspectRatio.STRETCH -> stringResource(R.string.ratio_stretch)
    MzAspectRatio.RATIO_16_9 -> stringResource(R.string.ratio_16_9)
    MzAspectRatio.RATIO_4_3 -> stringResource(R.string.ratio_4_3)
    MzAspectRatio.ZOOM -> stringResource(R.string.ratio_zoom)
}

@Composable
fun formatVideoFinishAction(action: Int): String = when (action) {
    0 -> stringResource(R.string.video_finish_loop)
    1 -> stringResource(R.string.video_finish_pause)
    2 -> stringResource(R.string.video_finish_next)
    else -> stringResource(R.string.ui_label_unknown)
}
