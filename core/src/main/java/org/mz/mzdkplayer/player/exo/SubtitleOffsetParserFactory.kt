package org.mz.mzdkplayer.player.exo

import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.util.Consumer
import androidx.media3.common.util.UnstableApi
import androidx.media3.extractor.text.CuesWithTiming
import androidx.media3.extractor.text.SubtitleParser
import org.mz.mzdkplayer.tool.SubtitleOffsetLogic

/**
 * 给字幕整体加时间偏移的 [SubtitleParser.Factory] 包装。
 *
 * 背景：Media3（ExoPlayer）**没有**提供「字幕延迟」这种开关，但 1.11 起字幕默认在
 * 解封装阶段就被解析成 [CuesWithTiming]（`DefaultMediaSourceFactory` 内部
 * `parseSubtitlesDuringExtraction = true`），而解析用的 [SubtitleParser.Factory]
 * 是可替换的 —— 于是只要在解析结果出去之前把每条字幕的起始时间整体平移，
 * 就等于调了字幕时间轴。这一处注入同时覆盖：
 *
 * - **内嵌字幕**（MKV / TS 里的 SRT、ASS、PGS 等）：走 `ExtractorsFactory` 的转码路径；
 * - **外挂字幕**（`MediaItem.SubtitleConfiguration`）：`DefaultMediaSourceFactory`
 *   会用这个工厂造 `SubtitleExtractor`。
 *
 * 偏移量为正 = 字幕延后出现，为负 = 字幕提前出现（与 VLC 的 `spu-delay` 同口径）。
 *
 * 注意：字幕是**一次性解析**好的，改动偏移量只影响之后解析出来的字幕，
 * 所以改变偏移后需要重建媒体源才能生效，这在 [MzExoPlayer.setSubtitleDelay] 里处理。
 */
@OptIn(UnstableApi::class)
class SubtitleOffsetParserFactory(
    private val delegate: SubtitleParser.Factory,
    private val offsetUsProvider: () -> Long
) : SubtitleParser.Factory {

    override fun supportsFormat(format: Format): Boolean = delegate.supportsFormat(format)

    override fun getCueReplacementBehavior(format: Format): Int =
        delegate.getCueReplacementBehavior(format)

    override fun create(format: Format): SubtitleParser =
        SubtitleOffsetParser(delegate.create(format), offsetUsProvider)
}

/** 单个字幕轨的解析器：把 delegate 吐出来的每条字幕平移一次 */
@OptIn(UnstableApi::class)
private class SubtitleOffsetParser(
    private val delegate: SubtitleParser,
    private val offsetUsProvider: () -> Long
) : SubtitleParser {

    override fun parse(
        data: ByteArray,
        offset: Int,
        length: Int,
        outputOptions: SubtitleParser.OutputOptions,
        output: Consumer<CuesWithTiming>
    ) {
        // 每次解析时实时取偏移量，保证用户改完值之后重建媒体源就能拿到新值
        val shiftUs = offsetUsProvider()
        delegate.parse(data, offset, length, outputOptions) { cues ->
            output.accept(shiftCues(cues, shiftUs))
        }
    }

    override fun getCueReplacementBehavior(): Int = delegate.getCueReplacementBehavior()

    override fun reset() = delegate.reset()
}

/**
 * 把一条 [CuesWithTiming] 整条往后挪 [shiftUs]。
 *
 * `startTimeUs == C.TIME_UNSET` 表示时间未知（此时时序由 `Format.subsampleOffsetUs` 决定），
 * 动不了也不能动，原样返回。
 */
@OptIn(UnstableApi::class)
internal fun shiftCues(cues: CuesWithTiming, shiftUs: Long): CuesWithTiming {
    if (shiftUs == 0L || cues.startTimeUs == C.TIME_UNSET) return cues
    return CuesWithTiming(
        cues.cues,
        SubtitleOffsetLogic.shiftStartTimeUs(cues.startTimeUs, shiftUs),
        cues.durationUs
    )
}
