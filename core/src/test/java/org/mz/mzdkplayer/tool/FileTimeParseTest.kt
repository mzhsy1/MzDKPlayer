package org.mz.mzdkplayer.tool

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * [FileTimeParse] 的 JVM 单元测试。
 *
 * 覆盖 [FileTimeResolver] 里最容易出错的四段纯字符串处理：
 * HTTP 日期解析、协议推断、账号密码拆分、NFS 路径拆分。
 *
 * 期望值均与项目里的既有实现对齐（NFS 拆分对照 [NFSDataSource.establishConnection]）。
 */
class FileTimeParseTest {

    // ────────────────────────────── HTTP 日期 ──────────────────────────────

    @Test
    fun `HTTP 日期 - RFC1123 标准写法`() {
        assertEquals(
            1445412480000L,
            FileTimeParse.parseHttpDate("Wed, 21 Oct 2015 07:28:00 GMT")
        )
    }

    @Test
    fun `HTTP 日期 - 时区写成偏移量`() {
        assertEquals(
            1445383680000L,
            FileTimeParse.parseHttpDate("Wed, 21 Oct 2015 07:28:00 +0800")
        )
    }

    @Test
    fun `HTTP 日期 - GMT 与加八时区相差八小时`() {
        val gmt = FileTimeParse.parseHttpDate("Wed, 21 Oct 2015 07:28:00 GMT")!!
        val plus8 = FileTimeParse.parseHttpDate("Wed, 21 Oct 2015 07:28:00 +0800")!!
        assertEquals(8 * 60 * 60 * 1000L, gmt - plus8)
    }

    @Test
    fun `HTTP 日期 - asctime 写法也能解析`() {
        // 该写法不带时区，按默认时区解析，因此只断言能解析且回读日期正确
        val parsed = FileTimeParse.parseHttpDate("Wed Oct 21 07:28:00 2015")
        assertNotNull("asctime 写法应能解析", parsed)

        val back = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date(parsed!!))
        assertTrue("回读结果应为 2015-10-21，实际 <$back>", back.startsWith("2015-10-21"))
    }

    @Test
    fun `HTTP 日期 - 前后空白会被裁掉`() {
        assertEquals(
            1445412480000L,
            FileTimeParse.parseHttpDate("  Wed, 21 Oct 2015 07:28:00 GMT \r\n")
        )
    }

    @Test
    fun `HTTP 日期 - 无法识别时返回 null`() {
        assertNull(FileTimeParse.parseHttpDate(null))
        assertNull(FileTimeParse.parseHttpDate(""))
        assertNull(FileTimeParse.parseHttpDate("   "))
        assertNull(FileTimeParse.parseHttpDate("not-a-date"))
    }

    // ────────────────────────────── 协议推断 ──────────────────────────────

    @Test
    fun `协议推断 - 按 scheme 识别`() {
        assertEquals("SMB", FileTimeParse.inferDataSourceType("smb://192.168.1.4/share/a.mkv"))
        assertEquals("FTP", FileTimeParse.inferDataSourceType("ftp://192.168.1.4/a.mkv"))
        assertEquals("NFS", FileTimeParse.inferDataSourceType("nfs://192.168.1.4:/fs/a.mkv"))
        assertEquals("HTTP", FileTimeParse.inferDataSourceType("http://a.com/a.mkv"))
        assertEquals("HTTP", FileTimeParse.inferDataSourceType("https://a.com/a.mkv"))
    }

    @Test
    fun `协议推断 - scheme 大小写不敏感`() {
        assertEquals("SMB", FileTimeParse.inferDataSourceType("SMB://host/share/a.mkv"))
        assertEquals("FTP", FileTimeParse.inferDataSourceType("Ftp://host/a.mkv"))
        assertEquals("HTTP", FileTimeParse.inferDataSourceType("HTTPS://host/a.mkv"))
    }

    @Test
    fun `协议推断 - 本地或未知协议返回 null`() {
        assertNull(FileTimeParse.inferDataSourceType(""))
        assertNull(FileTimeParse.inferDataSourceType("/sdcard/Movies/a.mkv"))
        assertNull(FileTimeParse.inferDataSourceType("file:///sdcard/Movies/a.mkv"))
        assertNull(FileTimeParse.inferDataSourceType("content://media/external/video/1"))
        assertNull(FileTimeParse.inferDataSourceType("rtsp://a.com/a.mkv"))
    }

    // ────────────────────────────── 账号密码 ──────────────────────────────

    @Test
    fun `账号密码 - 标准 userInfo`() {
        assertEquals("user" to "pass", FileTimeParse.credentials("user:pass", "guest"))
        assertEquals("admin" to "123456", FileTimeParse.credentials("admin:123456", "anonymous"))
    }

    @Test
    fun `账号密码 - 只有账号没有密码`() {
        assertEquals("user" to "", FileTimeParse.credentials("user", "guest"))
    }

    @Test
    fun `账号密码 - userInfo 为 null 时用默认账号`() {
        assertEquals("guest" to "", FileTimeParse.credentials(null, "guest"))
        assertEquals("anonymous" to "", FileTimeParse.credentials(null, "anonymous"))
    }

    @Test
    fun `账号密码 - 空账号或空密码`() {
        assertEquals("user" to "", FileTimeParse.credentials("user:", "guest"))
        assertEquals("" to "pass", FileTimeParse.credentials(":pass", "guest"))
    }

    @Test
    fun `账号密码 - 密码含冒号时只在第一个冒号处切分`() {
        assertEquals("user" to "p:ss", FileTimeParse.credentials("user:p:ss", "guest"))
    }

    // ────────────────────────────── NFS 路径 ──────────────────────────────

    @Test
    fun `NFS 路径 - 标准格式拆出导出目录与导出内路径`() {
        assertEquals(
            "fs/1000/nfs" to "/movies/a.mkv",
            FileTimeParse.splitNfsPath("/fs/1000/nfs:/movies/a.mkv")
        )
    }

    @Test
    fun `NFS 路径 - 导出内路径缺少前导斜杠时补齐`() {
        assertEquals(
            "fs/1000/nfs" to "/movies/a.mkv",
            FileTimeParse.splitNfsPath("/fs/1000/nfs:movies/a.mkv")
        )
    }

    @Test
    fun `NFS 路径 - 导出内路径为根目录`() {
        assertEquals("fs/1000/nfs" to "/", FileTimeParse.splitNfsPath("/fs/1000/nfs:"))
    }

    @Test
    fun `NFS 路径 - 带中文与空格的文件名原样保留`() {
        assertEquals(
            "fs/1000/nfs" to "/影片/流浪地球 2019.mkv",
            FileTimeParse.splitNfsPath("/fs/1000/nfs:/影片/流浪地球 2019.mkv")
        )
    }

    @Test
    fun `NFS 路径 - 缺少分隔冒号时返回 null`() {
        assertNull(FileTimeParse.splitNfsPath(""))
        assertNull(FileTimeParse.splitNfsPath("/fs/1000/nfs/movies/a.mkv"))
    }

    @Test
    fun `NFS 路径 - 导出目录为空时返回 null`() {
        assertNull(FileTimeParse.splitNfsPath(":/movies/a.mkv"))
        assertNull(FileTimeParse.splitNfsPath("/:/movies/a.mkv"))
    }
}
