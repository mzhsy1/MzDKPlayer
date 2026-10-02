package org.mz.mzdkplayer.tool

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mz.mzdkplayer.tool.logic.PhonePlayerLogic

/**
 * 手机端视频播放页的纯逻辑：内核判定、连播下标、进度换算、继续播放阈值。
 *
 * 这几条写错的症状都很隐蔽（跳到毫不相干的影片、进度条 NaN 崩掉、每次进页面都被问
 * 「要不要继续」），所以逐条锁死。
 */
class PhonePlayerLogicTest {

    // ────────────────────────────── 内核选择 ──────────────────────────────

    @Test
    fun `传输流与蓝光原盘强制走 VLC`() {
        assertTrue(PhonePlayerLogic.forceVlcByExtension("影片.ts"))
        assertTrue(PhonePlayerLogic.forceVlcByExtension("影片.M2TS"))
        assertTrue(PhonePlayerLogic.forceVlcByExtension("蓝光原盘.iso"))
        assertTrue(PhonePlayerLogic.forceVlcByExtension("smb://host/share/影片.m2t"))
    }

    @Test
    fun `普通格式仍走 Exo`() {
        assertFalse(PhonePlayerLogic.forceVlcByExtension("影片.mkv"))
        assertFalse(PhonePlayerLogic.forceVlcByExtension("影片.MP4"))
        // `.ts` 出现在文件名中间（`test.mkv`）时不能误判
        assertFalse(PhonePlayerLogic.forceVlcByExtension("test.mkv"))
        assertFalse(PhonePlayerLogic.forceVlcByExtension("没有扩展名"))
    }

    @Test
    fun `带查询串的直链按路径取扩展名`() {
        // 不截 `?` 的话这里会被判成扩展名 `b`，于是明明能硬解的 mp4 被丢给 VLC
        assertFalse(PhonePlayerLogic.forceVlcByExtension("http://host/a.mp4?token=x.y"))
        assertTrue(PhonePlayerLogic.forceVlcByExtension("http://host/a.ts?token=x.y"))
        assertFalse(PhonePlayerLogic.forceVlcByExtension("http://host/a.mkv#part.iso"))
    }

    // ────────────────────────────── 连播下标 ──────────────────────────────

    @Test
    fun `连播只往后一条`() {
        assertEquals(3, PhonePlayerLogic.nextPlaylistIndex(playlistSize = 5, currentIndex = 2))
        assertEquals(1, PhonePlayerLogic.nextPlaylistIndex(playlistSize = 5, currentIndex = 0))
    }

    @Test
    fun `最后一条与不在列表里都不连播`() {
        assertNull(PhonePlayerLogic.nextPlaylistIndex(playlistSize = 5, currentIndex = 4))
        // 从首页历史记录直接播放时，当前文件不在播放列表里，不能猜成「下一个是第一集」
        assertNull(PhonePlayerLogic.nextPlaylistIndex(playlistSize = 5, currentIndex = -1))
        assertNull(PhonePlayerLogic.nextPlaylistIndex(playlistSize = 0, currentIndex = 0))
    }

    @Test
    fun `播放列表里找当前文件`() {
        val uris = listOf("file:///a.mkv", "smb://host/b.mkv")
        assertEquals(1, PhonePlayerLogic.playlistIndexOf(uris, "smb://host/b.mkv"))
        assertEquals(-1, PhonePlayerLogic.playlistIndexOf(uris, "file:///c.mkv"))
    }

    // ────────────────────────────── 快进快退 ──────────────────────────────

    @Test
    fun `快进快退收敛在零与总时长之间`() {
        assertEquals(45_000L, PhonePlayerLogic.seekTargetMs(30_000L, 15_000L, 600_000L))
        assertEquals(0L, PhonePlayerLogic.seekTargetMs(5_000L, -15_000L, 600_000L))
        assertEquals(600_000L, PhonePlayerLogic.seekTargetMs(595_000L, 15_000L, 600_000L))
    }

    @Test
    fun `时长未知时不给跳`() {
        assertNull(PhonePlayerLogic.seekTargetMs(30_000L, 15_000L, 0L))
        assertNull(PhonePlayerLogic.seekTargetMs(30_000L, 15_000L, -1L))
    }

    // ────────────────────────────── 进度与继续播放 ──────────────────────────────

    @Test
    fun `进度比例被夹在零到一`() {
        assertEquals(0f, PhonePlayerLogic.progressOf(0L, 0L), 0.0001f)
        assertEquals(0.5f, PhonePlayerLogic.progressOf(300_000L, 600_000L), 0.0001f)
        // 播放位置偶尔会短暂超过时长（换源那一瞬间），不能让 Slider 收到 > 1 的值
        assertEquals(1f, PhonePlayerLogic.progressOf(700_000L, 600_000L), 0.0001f)
    }

    @Test
    fun `只看了片头不提示继续播放`() {
        assertFalse(PhonePlayerLogic.shouldOfferResume(0L))
        assertFalse(PhonePlayerLogic.shouldOfferResume(5_000L))
        assertTrue(PhonePlayerLogic.shouldOfferResume(5_001L))
        assertTrue(PhonePlayerLogic.shouldOfferResume(1_800_000L))
    }

    @Test
    fun `倍速档位与电视端一致`() {
        assertEquals(listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f), PhonePlayerLogic.speedOptions)
    }
}
