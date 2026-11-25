package org.mz.mzdkplayer.tool

import org.junit.Assert.assertEquals
import org.junit.Test
import org.mz.mzdkplayer.tool.logic.SubtitleTextSize

class SubtitleTextSizeTest {
    @Test
    fun size44MatchesAcrossCommonVideoResolutions() {
        val scale = SubtitleTextSize.vlcScalePercent(44f)
        for ((height, expected) in listOf(720 to 44, 1080 to 66, 2160 to 132)) {
            val width = height * 16 / 9
            assertEquals(
                expected.toFloat(),
                SubtitleTextSize.pixels(44f, width.toFloat(), height.toFloat()),
                0.01f,
            )

            val vlcPixels = height / SubtitleTextSize.VLC_HEIGHT_DIVISOR * scale / 100
            assertEquals(expected, vlcPixels)
        }
    }

    @Test
    fun portraitVideoUsesItsShortEdge() {
        assertEquals(44f, SubtitleTextSize.pixels(44f, 720f, 1280f), 0.01f)
    }
}
