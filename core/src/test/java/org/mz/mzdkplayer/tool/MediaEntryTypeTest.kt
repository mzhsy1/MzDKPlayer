package org.mz.mzdkplayer.tool

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

/**
 * 文件列表里「按扩展名决定图标和点击行为」的那组判断（[Tools] 里的纯函数）的 JVM 单元测试。
 *
 * 这些判断同时被列表图标（`FileIcon`）、焦点高亮和点击路由用到，判错就会
 * 「显示成普通文件、点了没反应」或者「把图片丢给播放器」。
 *
 * 注意三个函数的匹配口径并不一致，用例里专门把差异钉住：
 * - `containsVideoFormat` / `containsAudioFormat` 是**子串**匹配；
 * - `containsImageFileExtension` 是**扩展名全集**比对，必须先 `extractFileExtension`。
 */
class MediaEntryTypeTest {

    // ────────────────────────────── 取扩展名 ──────────────────────────────

    @Test
    fun `扩展名 - 常见文件名`() {
        assertEquals("mkv", Tools.extractFileExtension("电影.mkv"))
        assertEquals("mp4", Tools.extractFileExtension("电影.MP4"))       // 统一转小写
        assertEquals("srt", Tools.extractFileExtension("电影.zh.srt"))    // 只取最后一个点之后
        assertEquals("m2ts", Tools.extractFileExtension("电影 2019.m2ts"))
        assertEquals("flac", Tools.extractFileExtension("周杰伦 - 晴天.flac"))
    }

    @Test
    fun `扩展名 - 没有扩展名时返回空串`() {
        assertEquals("", Tools.extractFileExtension("README"))
        assertEquals("", Tools.extractFileExtension("电影."))          // 点结尾
        assertEquals("", Tools.extractFileExtension(".gitignore"))     // 点开头视为无扩展名
        assertEquals("", Tools.extractFileExtension(""))
        assertEquals("", Tools.extractFileExtension(null))
    }

    // ────────────────────────────── 视频 ──────────────────────────────

    @Test
    fun `视频类型 - 项目当前支持的扩展名`() {
        val supported = listOf("mp4", "mkv", "m2ts", "3gp", "avi", "mov", "ts", "flv", "iso")
        supported.forEach { ext ->
            assertTrue(
                "$ext 应被识别为视频",
                Tools.containsVideoFormat(Tools.extractFileExtension("电影.$ext"))
            )
        }
    }

    @Test
    fun `视频类型 - 大写扩展名同样识别`() {
        assertTrue(Tools.containsVideoFormat(Tools.extractFileExtension("电影.MKV")))
        assertTrue(Tools.containsVideoFormat(Tools.extractFileExtension("电影.Mp4")))
        assertTrue(Tools.containsVideoFormat(Tools.extractFileExtension("电影.AVI")))
    }

    @Test
    fun `视频类型 - 未收录的常见视频扩展名当前不识别`() {
        // 已知缺口：白名单里没有这些格式，列表里它们会显示成普通文件、点了也没有播放器。
        // 把格式补进 Tools.containsVideoFormat 的白名单后，需要同步修改本用例。
        listOf("webm", "wmv", "m4v", "mpg", "mpeg", "rmvb").forEach { ext ->
            assertFalse("$ext 目前不在视频白名单里", Tools.containsVideoFormat(ext))
        }
    }

    @Test
    fun `视频类型 - 是子串匹配而不是全等匹配`() {
        // 这是白名单用 `contains` 实现的副作用：只要包含任一格式名就判为视频。
        // 这里钉住现有行为，改动匹配方式时本用例会失败并提醒确认。
        assertTrue(Tools.containsVideoFormat("ts"))
        assertTrue(Tools.containsVideoFormat("xxxMP4xxx"))
        assertFalse(Tools.containsVideoFormat("pdf"))
        assertFalse(Tools.containsVideoFormat(""))
    }

    // ────────────────────────────── 音频 ──────────────────────────────

    @Test
    fun `音频类型 - 项目当前支持的扩展名`() {
        listOf("mp3", "flac", "wav", "aac").forEach { ext ->
            assertTrue("$ext 应被识别为音频", Tools.containsAudioFormat(ext))
        }
    }

    @Test
    fun `音频类型 - 未收录的常见音频扩展名当前不识别`() {
        // 已知缺口：m4a / ogg / wma 都不在白名单里
        listOf("m4a", "ogg", "wma", "ape").forEach { ext ->
            assertFalse("$ext 目前不在音频白名单里", Tools.containsAudioFormat(ext))
        }
    }

    // ────────────────────────────── 图片 ──────────────────────────────

    @Test
    fun `图片类型 - 支持的扩展名`() {
        listOf("bmp", "jpg", "jpeg", "png", "webp", "heif", "heic", "avif").forEach { ext ->
            assertTrue("$ext 应被识别为图片", Tools.containsImageFileExtension(ext))
        }
    }

    @Test
    fun `图片类型 - 大小写与前后空格都能容忍`() {
        assertTrue(Tools.containsImageFileExtension("PNG"))
        assertTrue(Tools.containsImageFileExtension(" jpg "))
        assertFalse(Tools.containsImageFileExtension("gif")) // 已知缺口：gif 未收录
    }

    @Test
    fun `图片类型 - 只认扩展名，传完整文件名不识别`() {
        assertFalse(Tools.containsImageFileExtension("电影.jpg"))
        assertTrue(Tools.containsImageFileExtension(Tools.extractFileExtension("电影.jpg")))
    }

    // ────────────────────────────── 类型之间不串味 ──────────────────────────────

    @Test
    fun `类型判定 - 视频不会被误判成图片或音频`() {
        assertFalse(Tools.containsImageFileExtension("mp4"))
        assertFalse(Tools.containsImageFileExtension("mkv"))
        assertFalse(Tools.containsAudioFormat("mp4"))
        assertFalse(Tools.containsAudioFormat("mkv"))
    }

    @Test
    fun `类型判定 - 字幕与未知格式三类都不属于`() {
        listOf("srt", "ass", "nfo", "txt", "pdf", "exe").forEach { ext ->
            assertFalse("$ext 不应被判为视频", Tools.containsVideoFormat(ext))
            assertFalse("$ext 不应被判为音频", Tools.containsAudioFormat(ext))
            assertFalse("$ext 不应被判为图片", Tools.containsImageFileExtension(ext))
        }
    }

    // ────────────────────────────── 文件大小展示 ──────────────────────────────

    @Test
    fun `文件大小 - 非正数统一显示 0 B`() {
        assertEquals("0 B", Tools.formatFileSize(0))
        assertEquals("0 B", Tools.formatFileSize(-1))
        assertEquals("0 B", Tools.formatFileSize(Long.MIN_VALUE))
    }

    @Test
    fun `文件大小 - 按 1024 进制换算单位`() {
        // 期望值用同一个 Locale 生成，避免测试机语言环境不同导致小数点符号不一致
        val locale = Locale.getDefault()

        assertEquals(String.format(locale, "%.2f B", 1023.0), Tools.formatFileSize(1023))
        assertEquals(String.format(locale, "%.2f KB", 1.0), Tools.formatFileSize(1024))
        assertEquals(String.format(locale, "%.2f KB", 1.5), Tools.formatFileSize(1536))
        assertEquals(String.format(locale, "%.2f KB", 2.0), Tools.formatFileSize(2048))
        assertEquals(String.format(locale, "%.2f MB", 3.0), Tools.formatFileSize(3L * 1024 * 1024))
        assertEquals(String.format(locale, "%.2f GB", 2.0), Tools.formatFileSize(2L * 1024 * 1024 * 1024))
        assertEquals(String.format(locale, "%.2f TB", 2.0), Tools.formatFileSize(2L * 1024 * 1024 * 1024 * 1024))
    }
}
