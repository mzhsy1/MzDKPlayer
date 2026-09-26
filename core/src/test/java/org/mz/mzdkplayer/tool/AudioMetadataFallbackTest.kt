package org.mz.mzdkplayer.tool

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 音频元数据取值的优先级：解析值（非占位）→ ID3 兜底 → 文件名解析 → 占位值。
 *
 * 对着真文件的场景：`看月亮爬上来 - 张杰.wav` 在 jaudiotagger 眼里是「未知标题」，
 * 但 ID3 兜底能读出 `看月亮爬上来`；再退一步，文件名里也写着这个名字。
 */
class AudioMetadataFallbackTest {

    @Test
    fun `标签里读到了就用标签的`() {
        assertEquals("看月亮爬上来", pickMetadata("看月亮爬上来", "未知标题", "ID3 的", "文件名的"))
    }

    @Test
    fun `占位值让位给 ID3 兜底`() {
        assertEquals("ID3 的", pickMetadata("未知标题", "未知标题", "ID3 的", "文件名的"))
    }

    @Test
    fun `ID3 也没有就用文件名解析的`() {
        assertEquals("文件名的", pickMetadata("未知标题", "未知标题", null, "文件名的"))
        assertEquals("文件名的", pickMetadata(null, "未知标题", "   ", "文件名的"))
    }

    @Test
    fun `三个来源都没有时保留占位值而不是空串`() {
        assertEquals("未知标题", pickMetadata("未知标题", "未知标题", null, null))
        assertEquals("未知标题", pickMetadata(null, "未知标题", null, null))
        assertEquals("未知标题", pickMetadata(null, "未知标题", "", ""))
    }
}
