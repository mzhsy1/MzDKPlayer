package org.mz.mzdkplayer.tool

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.mz.mzdkplayer.tool.logic.ProtocolUriParser

/**
 * [ProtocolUriParser] 的 JVM 单元测试。
 *
 * SMB 的「共享名是第几段」原本内联在 `SmbUtils.openSmbFileInputStream` 里，
 * 只有连上真服务器才会执行到 —— 写错的表现是**拿着错误的 share 去连接**，
 * 报错信息还只是一句 `Failed to open SMB file`。拆出来之后这里逐条钉住。
 */
class ProtocolUriParserTest {

    // ────────────────────────────── SMB 路径拆分 ──────────────────────────────

    @Test
    fun `smb 路径 - 第一段是共享名 其余是共享内路径`() {
        val path = ProtocolUriParser.parseSmbPath("/share/movies/影片.mkv")
        assertEquals(ProtocolUriParser.SmbPath("share", "movies/影片.mkv"), path)
    }

    @Test
    fun `smb 路径 - 只有共享名时共享内路径为空串`() {
        // 浏览「连接根目录」时就是这个形态
        val path = ProtocolUriParser.parseSmbPath("/movies")
        assertEquals(ProtocolUriParser.SmbPath("movies", ""), path)
    }

    @Test
    fun `smb 路径 - 结尾多余斜杠不产生空路径段`() {
        assertEquals(ProtocolUriParser.SmbPath("movies", ""), ProtocolUriParser.parseSmbPath("/movies/"))
        assertEquals(
            ProtocolUriParser.SmbPath("share", "dir"),
            ProtocolUriParser.parseSmbPath("/share/dir/")
        )
    }

    @Test
    fun `smb 路径 - 连续斜杠被折叠`() {
        assertEquals(
            ProtocolUriParser.SmbPath("share", "movies/影片.mkv"),
            ProtocolUriParser.parseSmbPath("//share//movies//影片.mkv")
        )
    }

    @Test
    fun `smb 路径 - 共享内路径保留原始层数`() {
        assertEquals(
            ProtocolUriParser.SmbPath("share", "a/b/c/d.mkv"),
            ProtocolUriParser.parseSmbPath("/share/a/b/c/d.mkv")
        )
    }

    @Test
    fun `smb 路径 - 空路径与纯斜杠返回 null`() {
        // Uri.path 理论上是空串（而不是 null）时就是这种形态，调用方据此报 no share or path
        assertNull(ProtocolUriParser.parseSmbPath(""))
        assertNull(ProtocolUriParser.parseSmbPath("/"))
        assertNull(ProtocolUriParser.parseSmbPath("///"))
    }

    @Test
    fun `smb 路径 - 没有前导斜杠也照常拆（调用方已保证过形态）`() {
        assertEquals(ProtocolUriParser.SmbPath("share", "a.mkv"), ProtocolUriParser.parseSmbPath("share/a.mkv"))
    }

    @Test
    fun `smb 路径 - 空格与百分号编码不在这里解码`() {
        // 解码交给 Uri 层，这里保证原样带过去（否则会二次解码）
        assertEquals(
            ProtocolUriParser.SmbPath("share", "my%20movie.mkv"),
            ProtocolUriParser.parseSmbPath("/share/my%20movie.mkv")
        )
        assertEquals(
            ProtocolUriParser.SmbPath("share", "我的 电影.mkv"),
            ProtocolUriParser.parseSmbPath("/share/我的 电影.mkv")
        )
    }

    @Test
    fun `smb 路径 - 点开头的隐藏共享名也算共享名`() {
        assertEquals(
            ProtocolUriParser.SmbPath(".hidden", "a.mkv"),
            ProtocolUriParser.parseSmbPath("/.hidden/a.mkv")
        )
    }

    // ────────────────────────────── FTP 端口 ──────────────────────────────

    @Test
    fun `ftp 端口 - Uri 未写端口（-1）落到 21`() {
        assertEquals(21, ProtocolUriParser.ftpPort(-1))
        assertEquals(21, ProtocolUriParser.DEFAULT_FTP_PORT)
    }

    @Test
    fun `ftp 端口 - 显式端口原样使用`() {
        assertEquals(2121, ProtocolUriParser.ftpPort(2121))
        assertEquals(990, ProtocolUriParser.ftpPort(990))
    }

    @Test
    fun `ftp 端口 - 端口 0 不被当成缺省`() {
        // 只把 -1 当「没写」，0 是显式值（Uri 不会给 0，但口径要明确）
        assertEquals(0, ProtocolUriParser.ftpPort(0))
    }
}
