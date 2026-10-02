package org.mz.mzdkplayer.tool

import org.junit.Assert.assertEquals
import org.junit.Test
import org.mz.mzdkplayer.tool.logic.StreamSizePolicy

/**
 * [StreamSizePolicy.of] 的 JVM 单元测试。
 *
 * 这套判定决定「探测型读取会不会被 5MB 砍掉」，砍错的表现是**静默的**：
 * 封面读不出来、歌词为空、标题退化成文件名，都不会抛异常。
 * 所以这里把三种策略的边界逐条钉住，包括「不走运的 MIME 会落到 LIMITED」这种负向用例。
 */
class StreamSizePolicyTest {

    private val unlimited = StreamSizePolicy.Policy.UNLIMITED
    private val unlimitedRaw = StreamSizePolicy.Policy.UNLIMITED_RAW
    private val limited = StreamSizePolicy.Policy.LIMITED

    // ────────────────────────────── 音频 ──────────────────────────────

    @Test
    fun `音频 - 出现 audio 即放开长度`() {
        assertEquals(unlimited, StreamSizePolicy.of("audio/mpeg"))
        assertEquals(unlimited, StreamSizePolicy.of("audio/wav"))
        assertEquals(unlimited, StreamSizePolicy.of("audio/flac"))
    }

    @Test
    fun `音频 - audio-raw 也放开（ExoPlayer 音频直通给的就是它）`() {
        assertEquals(unlimited, StreamSizePolicy.of("audio/raw"))
    }

    @Test
    fun `音频 - 裸 audio 这个词也能命中（子串匹配而非前缀匹配）`() {
        // 判定用的是 contains，历史上就是这个口径：`Mp3audio` 也会被放开
        assertEquals(unlimited, StreamSizePolicy.of("x-audio-x"))
    }

    // ────────────────────────────── 视频 ──────────────────────────────

    @Test
    fun `视频 - 出现 video 即放开长度`() {
        assertEquals(unlimited, StreamSizePolicy.of("video/mp4"))
        assertEquals(unlimited, StreamSizePolicy.of("video/x-matroska"))
        assertEquals(unlimited, StreamSizePolicy.of("video/mp2t"))
    }

    // ────────────────────────────── 图片 ──────────────────────────────

    @Test
    fun `图片 - pics 归到不限长度但标注为 RAW`() {
        // UNLIMITED 与 UNLIMITED_RAW 只差在 FTP / HTTP 的调用点上（是否套关闭包装），
        // 两者都不限长度
        assertEquals(unlimitedRaw, StreamSizePolicy.of("pics"))
    }

    @Test
    fun `图片 - 标准 MIME image jpeg 反而会被限流（历史拼写遗留）`() {
        // 判定认的是 pics 而不是 image，这是既有行为：想放开图片得先改这里的口径
        assertEquals(limited, StreamSizePolicy.of("image/jpeg"))
        assertEquals(limited, StreamSizePolicy.of("image/png"))
    }

    // ────────────────────────────── 限流 ──────────────────────────────

    @Test
    fun `元数据 - 弹幕 XML 与 NFO 一律限流`() {
        assertEquals(limited, StreamSizePolicy.of("application/xml"))
        assertEquals(limited, StreamSizePolicy.of("text/xml"))
        assertEquals(limited, StreamSizePolicy.of("text/plain"))
        assertEquals(limited, StreamSizePolicy.of("application/octet-stream"))
    }

    @Test
    fun `元数据 - 空 MIME 走限流`() {
        assertEquals(limited, StreamSizePolicy.of(""))
    }

    @Test
    fun `元数据 - 上限是 5MB`() {
        assertEquals(5L * 1024 * 1024, StreamSizePolicy.MAX_BYTES)
    }

    // ────────────────────────────── 匹配口径 ──────────────────────────────

    @Test
    fun `匹配 - 区分大小写`() {
        assertEquals(limited, StreamSizePolicy.of("AUDIO/mpeg"))
        assertEquals(limited, StreamSizePolicy.of("Video/mp4"))
        assertEquals(limited, StreamSizePolicy.of("PICS"))
    }

    @Test
    fun `匹配 - 同时命中多个词时按 audio 到 video 到 pics 的顺序`() {
        assertEquals(unlimited, StreamSizePolicy.of("video/x-pics"))
        assertEquals(unlimited, StreamSizePolicy.of("audio/pics"))
    }
}
