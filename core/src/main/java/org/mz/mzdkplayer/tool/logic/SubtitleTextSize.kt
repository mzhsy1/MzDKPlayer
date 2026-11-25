package org.mz.mzdkplayer.tool.logic

import kotlin.math.roundToInt

internal object SubtitleTextSize {
    // 字号以 1280×720 为基准，竖屏视频取短边，与 VLC FreeType 一致。
    private const val REFERENCE_HEIGHT_PX = 720
    const val VLC_HEIGHT_DIVISOR = 18

    fun pixels(fontSize: Float, videoWidthPx: Float, videoHeightPx: Float): Float =
        fontSize * minOf(videoWidthPx, videoHeightPx) / REFERENCE_HEIGHT_PX

    // VLC 先用视频短边除以 18（720p 为 40 px），再应用整数百分比。
    fun vlcScalePercent(fontSize: Float): Int =
        (fontSize * VLC_HEIGHT_DIVISOR * 100 / REFERENCE_HEIGHT_PX).roundToInt()
}
