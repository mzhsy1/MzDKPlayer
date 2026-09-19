package org.mz.mzdkplayer.tool

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Locale

/**
 * [Tools] 里「产出给用户看的字符串」那组纯函数的 JVM 单元测试。
 *
 * 这一组函数本身没有依赖 Android，但被进度条、轨道面板、媒体库卡片、音频播放页到处调用：
 * 时间显示错一位、语言/国家名映射漏了分支、音频格式名认不出来，都是直接摆在用户脸上的问题，
 * 而且改动起来最容易「顺手删掉一个 else 分支」而没人发现。
 *
 * 期望值一律用同一个 `Locale.getDefault()` 生成，避免测试机语言环境影响 `String.format`。
 */
class ToolsDisplayTextTest {

    /** 用与实现相同的 Locale 生成时间期望值，避免小数点/数字形态随测试机环境漂移。 */
    private fun hhmmss(hours: Int, minutes: Int, seconds: Int) =
        String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)

    private fun mmss(minutes: Int, seconds: Int) =
        String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)

    // ────────────────────────────── formatTime ──────────────────────────────

    @Test
    fun `时长 - 非正数一律显示 00 比 00`() {
        assertEquals("00:00", Tools.formatTime(0L))
        assertEquals("00:00", Tools.formatTime(-1L))
    }

    @Test
    fun `时长 - 不足一分钟进位前不跳秒`() {
        assertEquals(mmss(0, 1), Tools.formatTime(1_000L))
        assertEquals(mmss(0, 59), Tools.formatTime(59_999L))
    }

    @Test
    fun `时长 - 不足一小时只显示分秒`() {
        assertEquals(mmss(1, 5), Tools.formatTime(65_000L))
        assertEquals(mmss(59, 59), Tools.formatTime(3_599_000L))
    }

    @Test
    fun `时长 - 满一小时加上小时段`() {
        assertEquals(hhmmss(1, 0, 0), Tools.formatTime(3_600_000L))
        assertEquals(hhmmss(1, 1, 1), Tools.formatTime(3_661_000L))
        assertEquals(hhmmss(2, 30, 0), Tools.formatTime(9_000_000L))
    }

    // ────────────────────────────── formatFriendlyTime ──────────────────────────────

    @Test
    fun `友好时长 - 超过一小时按小时分钟显示`() {
        assertEquals("2h 5m", Tools.formatFriendlyTime(7_500_000L))
        assertEquals("1h 0m", Tools.formatFriendlyTime(3_600_000L))
    }

    @Test
    fun `友好时长 - 不足一小时退回分秒显示`() {
        assertEquals(mmss(1, 30), Tools.formatFriendlyTime(90_000L))
        assertEquals("00:00", Tools.formatFriendlyTime(0L))
    }

    // ────────────────────────────── 语言名映射 ──────────────────────────────

    @Test
    fun `语言 - 空值与 und 视为未知`() {
        assertEquals("未知语言", Tools.getFullLanguageName(null))
        assertEquals("未知语言", Tools.getFullLanguageName(""))
        assertEquals("未知语言", Tools.getFullLanguageName("und"))
    }

    @Test
    fun `语言 - 简体中文的各路写法`() {
        listOf("zh-hans", "zh-CN", "zh-sg", "chs", "sc", "chi_sim").forEach { code ->
            assertEquals("简体中文 -> $code", "简体中文", Tools.getFullLanguageName(code))
        }
    }

    @Test
    fun `语言 - 繁体中文的各路写法`() {
        listOf("zh-hant", "zh-TW", "zh-HK", "zh-MO", "cht", "tc", "chi_tra").forEach { code ->
            assertEquals("繁体中文 -> $code", "繁体中文", Tools.getFullLanguageName(code))
        }
    }

    @Test
    fun `语言 - 无法区分简繁时显示中文`() {
        listOf("zh", "zho", "chi").forEach { code ->
            assertEquals("中文 -> $code", "中文", Tools.getFullLanguageName(code))
        }
    }

    @Test
    fun `语言 - 常见外语的两字母与三字母写法`() {
        assertEquals("英语", Tools.getFullLanguageName("en"))
        assertEquals("英语", Tools.getFullLanguageName("eng"))
        assertEquals("日语", Tools.getFullLanguageName("ja"))
        assertEquals("日语", Tools.getFullLanguageName("jpn"))
        assertEquals("韩语", Tools.getFullLanguageName("ko"))
        assertEquals("法语", Tools.getFullLanguageName("fra"))
        assertEquals("德语", Tools.getFullLanguageName("ger"))
    }

    @Test
    fun `语言 - 未收录的代码原样大写返回`() {
        assertEquals("XX", Tools.getFullLanguageName("xx"))
        assertEquals("ABC", Tools.getFullLanguageName("abc"))
    }

    @Test
    fun `语言 - und 只有小写才被识别（已知缺口）`() {
        // "und" 的判空发生在 lowercase 之前，大写写法会掉进「原样大写返回」分支
        assertEquals("UND", Tools.getFullLanguageName("UND"))
    }

    // ────────────────────────────── 国家/地区名映射 ──────────────────────────────

    @Test
    fun `国家 - 常见代码映射`() {
        assertEquals("中国", Tools.getCountryName("CN"))
        assertEquals("美国", Tools.getCountryName("US"))
        assertEquals("日本", Tools.getCountryName("JP"))
        assertEquals("韩国", Tools.getCountryName("KR"))
    }

    @Test
    fun `国家 - 港台澳明确标注归属`() {
        assertEquals("中国香港", Tools.getCountryName("HK"))
        assertEquals("中国台湾", Tools.getCountryName("TW"))
        assertEquals("中国澳门", Tools.getCountryName("MO"))
    }

    @Test
    fun `国家 - 小写代码同样能命中`() {
        assertEquals("中国", Tools.getCountryName("cn"))
        assertEquals("中国香港", Tools.getCountryName("hk"))
    }

    @Test
    fun `国家 - 未收录的代码原样返回`() {
        assertEquals("ZZ", Tools.getCountryName("ZZ"))
    }

    // ────────────────────────────── 音频格式名 ──────────────────────────────

    @Test
    fun `音频格式 - DTS 家族`() {
        assertEquals("DTS", Tools.inferAudioFormatType("audio/vnd.dts"))
        assertEquals("DTS HD", Tools.inferAudioFormatType("audio/vnd.dts.hd"))
    }

    @Test
    fun `音频格式 - 杜比家族`() {
        assertEquals("Dolby TrueHD", Tools.inferAudioFormatType("audio/true-hd"))
        assertEquals("Dolby Digital (AC3)", Tools.inferAudioFormatType("audio/ac3"))
        assertEquals("Dolby Digital Plus (E-AC3)", Tools.inferAudioFormatType("audio/eac3"))
        assertEquals(
            "Dolby Digital Plus with Atmos (E-AC3 JOC)",
            Tools.inferAudioFormatType("audio/eac3-joc")
        )
    }

    @Test
    fun `音频格式 - 其余常见编码`() {
        assertEquals("AAC (Advanced Audio Coding)", Tools.inferAudioFormatType("audio/mp4a-latm"))
        assertEquals("Opus", Tools.inferAudioFormatType("audio/opus"))
        assertEquals("Vorbis", Tools.inferAudioFormatType("audio/vorbis"))
        assertEquals("FLAC (Free Lossless Audio Codec)", Tools.inferAudioFormatType("audio/flac"))
        assertEquals("PCM (Uncompressed)", Tools.inferAudioFormatType("audio/raw"))
        assertEquals("WAV (PCM)", Tools.inferAudioFormatType("audio/x-wav"))
        assertEquals("MP3 (MPEG-1 Audio Layer III)", Tools.inferAudioFormatType("audio/mpeg"))
        assertEquals("MP3 (MPEG-1 Audio Layer III)", Tools.inferAudioFormatType("audio/mp3"))
    }

    @Test
    fun `音频格式 - 未收录时去掉 audio 前缀并大写`() {
        assertEquals("X-WEIRD", Tools.inferAudioFormatType("audio/x-weird"))
        // 不是 audio/ 开头时前缀去不掉，整串被大写
        assertEquals("VIDEO/MP4", Tools.inferAudioFormatType("video/mp4"))
    }

    // ────────────────────────────── 文件名与图片地址 ──────────────────────────────

    @Test
    fun `文件名 - 取最后一个斜杠之后的部分`() {
        assertEquals("影片.mkv", Tools.extractFileNameFromUri("smb://host/share/影片.mkv"))
        assertEquals("影片 2019.mp4", Tools.extractFileNameFromUri("/movies/影片 2019.mp4"))
    }

    @Test
    fun `文件名 - 没有斜杠或斜杠在末尾时原样返回`() {
        assertEquals("影片.mkv", Tools.extractFileNameFromUri("影片.mkv"))
        // 斜杠是最后一个字符时没有「之后的部分」，按原样返回而不是空串
        assertEquals("smb://host/share/", Tools.extractFileNameFromUri("smb://host/share/"))
        assertEquals("", Tools.extractFileNameFromUri(""))
    }

    @Test
    fun `图片地址 - 空值返回 null`() {
        assertNull(Tools.formatImageUrl(null, "w200"))
        assertNull(Tools.formatImageUrl("", "w200"))
    }

    @Test
    fun `图片地址 - 完整地址原样返回相对路径补 TMDB 前缀`() {
        assertEquals(
            "http://example.com/a.jpg",
            Tools.formatImageUrl("http://example.com/a.jpg", "w200")
        )
        assertEquals(
            "https://image.tmdb.org/t/p/w500/abc.jpg",
            Tools.formatImageUrl("/abc.jpg", "w500")
        )
    }
}
