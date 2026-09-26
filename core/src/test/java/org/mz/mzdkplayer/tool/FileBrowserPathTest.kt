package org.mz.mzdkplayer.tool

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * [FileBrowserLogic] 里「路径拼接 / 上级目录 / 地址拼装」部分的 JVM 单元测试。
 *
 * 文件浏览的核心交互就是「进子目录 / 退上一级 / 把条目拼成可播放的地址」，
 * 五个协议各自有一套口径（尤其是根目录时返回 `""` 还是 `"/"`），最容易在改动时被弄错。
 *
 * 期望值均与抽取前的原实现逐字对齐；FTP 的上级目录口径对照 `FTPConViewModel.goBack`，
 * NFS 对照 `NFSConViewModel.getParentPath`，HTTP 对照 `HTTPLinkConViewModel.navigateToParent`。
 */
class FileBrowserPathTest {

    // ────────────────────────────── 目录条目过滤 ──────────────────────────────

    @Test
    fun `目录条目 - 点与双点被过滤`() {
        assertTrue(FileBrowserLogic.isHiddenDirEntry("."))
        assertTrue(FileBrowserLogic.isHiddenDirEntry(".."))
    }

    @Test
    fun `目录条目 - 空名与 null 被过滤`() {
        assertTrue(FileBrowserLogic.isHiddenDirEntry(""))
        assertTrue(FileBrowserLogic.isHiddenDirEntry("   "))
        assertTrue(FileBrowserLogic.isHiddenDirEntry(null))
    }

    @Test
    fun `目录条目 - 普通文件名与点开头的隐藏文件都保留`() {
        assertFalse(FileBrowserLogic.isHiddenDirEntry("电影.mkv"))
        assertFalse(FileBrowserLogic.isHiddenDirEntry("影片 2019"))
        // 点开头但不是 . 或 .. 的条目（如 SMB 里的隐藏文件）应当照常显示
        assertFalse(FileBrowserLogic.isHiddenDirEntry(".gitignore"))
        assertFalse(FileBrowserLogic.isHiddenDirEntry("..."))
    }

    // ────────────────────────────── SMB ──────────────────────────────

    @Test
    fun `SMB 属性位 - 目录位 0x10 判定为目录`() {
        // 0x10 等同 smbj 的 FileAttributes.FILE_ATTRIBUTE_DIRECTORY
        assertTrue(FileBrowserLogic.isSmbDirectory(0x00000010L))
        // 目录 + 存档位
        assertTrue(FileBrowserLogic.isSmbDirectory(0x00000010L or 0x00000020L))
        // 目录 + 普通文件位
        assertTrue(FileBrowserLogic.isSmbDirectory(0x00000010L or 0x00000080L))
    }

    @Test
    fun `SMB 属性位 - 没有目录位时判为文件`() {
        assertFalse(FileBrowserLogic.isSmbDirectory(0x00000000L))
        assertFalse(FileBrowserLogic.isSmbDirectory(0x00000020L)) // 存档
        assertFalse(FileBrowserLogic.isSmbDirectory(0x00000080L)) // 普通文件
    }

    @Test
    fun `SMB 路径 - 根目录换成单个反斜杠`() {
        assertEquals("\\", FileBrowserLogic.normalizeSmbDirectory("/"))
    }

    @Test
    fun `SMB 路径 - 正斜杠换反斜杠并去掉结尾斜杠`() {
        assertEquals("\\影片\\动作", FileBrowserLogic.normalizeSmbDirectory("/影片/动作"))
        assertEquals("\\影片\\动作", FileBrowserLogic.normalizeSmbDirectory("/影片/动作/"))
        assertEquals("影片", FileBrowserLogic.normalizeSmbDirectory("影片/"))
    }

    @Test
    fun `SMB 路径 - 拼接子条目`() {
        assertEquals("\\电影.mkv", FileBrowserLogic.joinSmbPath("\\", "电影.mkv"))
        assertEquals("\\影片\\电影.mkv", FileBrowserLogic.joinSmbPath("\\影片", "电影.mkv"))
    }

    @Test
    fun `SMB 路径 - 回到展示用的正斜杠形式`() {
        assertEquals("/影片/电影.mkv", FileBrowserLogic.toSmbDisplayPath("\\影片\\电影.mkv"))
        assertEquals("/电影.mkv", FileBrowserLogic.toSmbDisplayPath("\\电影.mkv"))
    }

    @Test
    fun `SMB 地址 - 带账号密码与子目录`() {
        val parts = FileBrowserLogic.parseSmbUrl("smb://user:pass@192.168.1.4/share/影片/动作")!!
        assertEquals("192.168.1.4", parts.server)
        assertEquals("share", parts.share)
        assertEquals("/影片/动作", parts.path)
        assertEquals("user", parts.username)
        assertEquals("pass", parts.password)
    }

    @Test
    fun `SMB 地址 - 省略账号密码时账号兜底为 guest`() {
        val parts = FileBrowserLogic.parseSmbUrl("smb://192.168.1.4/share/影片")!!
        assertEquals("192.168.1.4", parts.server)
        assertEquals("share", parts.share)
        assertEquals("/影片", parts.path)
        assertEquals("guest", parts.username)
        assertEquals("", parts.password)
    }

    @Test
    fun `SMB 地址 - 省略路径段时兜底为共享根目录`() {
        val parts = FileBrowserLogic.parseSmbUrl("smb://192.168.1.4/share")!!
        assertEquals("share", parts.share)
        assertEquals("/", parts.path)
        assertEquals("guest", parts.username)
    }

    @Test
    fun `SMB 地址 - 密码里含冒号时只在第一个冒号处拆账号`() {
        val parts = FileBrowserLogic.parseSmbUrl("smb://user:p:ss@192.168.1.4/share/影片")!!
        assertEquals("user", parts.username)
        assertEquals("p:ss", parts.password)
    }

    @Test
    fun `SMB 地址 - 格式不匹配时返回 null`() {
        assertNull(FileBrowserLogic.parseSmbUrl(""))
        assertNull(FileBrowserLogic.parseSmbUrl("/sdcard/电影"))
        assertNull(FileBrowserLogic.parseSmbUrl("ftp://192.168.1.4/share"))
        assertNull(FileBrowserLogic.parseSmbUrl("smb://192.168.1.4")) // 缺少 share 段
    }

    @Test
    fun `SMB 地址 - 有账号密码才写进 userInfo`() {
        assertEquals(
            "smb://user:pass@192.168.1.4/share/影片",
            FileBrowserLogic.buildSmbUrl("192.168.1.4", "share", "/影片", "user", "pass")
        )
    }

    @Test
    fun `SMB 地址 - 缺账号或缺密码时不写 userInfo`() {
        val expected = "smb://192.168.1.4/share/影片"
        assertEquals(expected, FileBrowserLogic.buildSmbUrl("192.168.1.4", "share", "/影片", "guest", ""))
        assertEquals(expected, FileBrowserLogic.buildSmbUrl("192.168.1.4", "share", "/影片", "", "pass"))
        assertEquals(expected, FileBrowserLogic.buildSmbUrl("192.168.1.4", "share", "/影片", "", ""))
    }

    @Test
    fun `SMB 地址 - 批量扫描用的地址始终带 userInfo`() {
        assertEquals(
            "smb://:@192.168.1.4/share/影片",
            FileBrowserLogic.buildSmbUrlWithCredentials("192.168.1.4", "share", "/影片", "", "")
        )
    }

    @Test
    fun `SMB 地址 - 解析后再拼装可还原原地址`() {
        val url = "smb://user:pass@192.168.1.4/share/影片/动作"
        val parts = FileBrowserLogic.parseSmbUrl(url)!!
        assertEquals(
            url,
            FileBrowserLogic.buildSmbUrl(
                parts.server, parts.share, parts.path, parts.username, parts.password
            )
        )
    }

    // ────────────────────────────── FTP ──────────────────────────────

    @Test
    fun `FTP 路径 - 请求路径补齐前后斜杠`() {
        assertEquals("/", FileBrowserLogic.normalizeFtpDirectory(""))
        assertEquals("/movies/", FileBrowserLogic.normalizeFtpDirectory("movies"))
        assertEquals("/movies/", FileBrowserLogic.normalizeFtpDirectory("/movies"))
        assertEquals("/movies/", FileBrowserLogic.normalizeFtpDirectory("/movies/"))
        assertEquals("/movies/action/", FileBrowserLogic.normalizeFtpDirectory("movies/action"))
    }

    @Test
    fun `FTP 路径 - 请求路径与界面显示路径可以互推`() {
        assertEquals("movies/action", FileBrowserLogic.ftpDisplayPath("/movies/action/"))
        assertEquals("movies", FileBrowserLogic.ftpDisplayPath("/movies/"))
        assertEquals("", FileBrowserLogic.ftpDisplayPath("/"))
    }

    @Test
    fun `FTP 路径 - 上一级目录`() {
        assertEquals("movies", FileBrowserLogic.ftpParentPath("movies/action"))
        assertEquals("movies", FileBrowserLogic.ftpParentPath("movies/action/"))
        assertEquals("", FileBrowserLogic.ftpParentPath("movies"))
        assertEquals("", FileBrowserLogic.ftpParentPath(""))
    }

    @Test
    fun `FTP 地址 - 当前目录缺结尾斜杠时自动补上`() {
        val expected = "ftp://user:pass@192.168.1.4:21/movies/action/电影.mkv"
        assertEquals(
            expected,
            FileBrowserLogic.buildFtpResourceUrl("192.168.1.4", 21, "user", "pass", "movies/action", "电影.mkv")
        )
        assertEquals(
            expected,
            FileBrowserLogic.buildFtpResourceUrl("192.168.1.4", 21, "user", "pass", "movies/action/", "电影.mkv")
        )
        assertEquals(
            expected,
            FileBrowserLogic.buildFtpResourceUrl("192.168.1.4", 21, "user", "pass", "movies/action/", "/电影.mkv")
        )
    }

    @Test
    fun `FTP 地址 - 根目录下不会拼出双斜杠`() {
        assertEquals(
            "ftp://user:pass@192.168.1.4:21/电影.mkv",
            FileBrowserLogic.buildFtpResourceUrl("192.168.1.4", 21, "user", "pass", "", "电影.mkv")
        )
    }

    @Test
    fun `FTP 地址 - 递归扫描用绝对路径拼装`() {
        assertEquals(
            "ftp://user:pass@192.168.1.4:21/movies/action/电影.mkv",
            FileBrowserLogic.buildFtpUrl("192.168.1.4", 21, "user", "pass", "/movies/action/电影.mkv")
        )
    }

    // ────────────────────────────── NFS ──────────────────────────────

    @Test
    fun `NFS 路径 - 上一级目录`() {
        assertEquals("/movies", FileBrowserLogic.nfsParentPath("/movies/action"))
        assertEquals("/movies", FileBrowserLogic.nfsParentPath("/movies/action/"))
        assertEquals("/", FileBrowserLogic.nfsParentPath("/movies"))
        assertEquals("/", FileBrowserLogic.nfsParentPath("movies"))
        assertEquals("/", FileBrowserLogic.nfsParentPath("/movies/"))
    }

    @Test
    fun `NFS 路径 - 挂载根没有上一级`() {
        assertEquals("", FileBrowserLogic.nfsParentPath(""))
        assertEquals("", FileBrowserLogic.nfsParentPath("/"))
    }

    @Test
    fun `NFS 路径 - 进入子目录`() {
        assertEquals("/影片", FileBrowserLogic.nfsChildPath("", "影片"))
        // 连接后 _currentPath 可能停在 "/"，这里必须按挂载根处理，不能拼成 "//影片"
        assertEquals("/影片", FileBrowserLogic.nfsChildPath("/", "影片"))
        assertEquals("/影片/动作", FileBrowserLogic.nfsChildPath("/影片", "动作"))
    }

    @Test
    fun `NFS 路径 - 逐级进入后逐级返回可回到挂载根`() {
        val level1 = FileBrowserLogic.nfsChildPath("", "影片")
        val level2 = FileBrowserLogic.nfsChildPath(level1, "动作")
        assertEquals("/影片/动作", level2)
        assertEquals("/影片", FileBrowserLogic.nfsParentPath(level2))
        assertEquals("/", FileBrowserLogic.nfsParentPath("/影片"))
        assertEquals("", FileBrowserLogic.nfsParentPath("/"))
    }

    // ────────────────────────────── HTTP ──────────────────────────────

    @Test
    fun `HTTP 上级 - 逐级回退并保留结尾斜杠`() {
        assertEquals("http://192.168.1.4/", FileBrowserLogic.httpParentUrl("http://192.168.1.4/movies/"))
        assertEquals(
            "http://192.168.1.4/movies/",
            FileBrowserLogic.httpParentUrl("http://192.168.1.4/movies/action/")
        )
        assertEquals(
            "http://192.168.1.4/nas/movies/",
            FileBrowserLogic.httpParentUrl("http://192.168.1.4/nas/movies/action/")
        )
    }

    @Test
    fun `HTTP 上级 - 已在站点根时原样返回`() {
        assertEquals("http://192.168.1.4/", FileBrowserLogic.httpParentUrl("http://192.168.1.4/"))
    }

    @Test
    fun `HTTP 上级 - 保留自定义端口`() {
        assertEquals("http://192.168.1.4:8080/", FileBrowserLogic.httpParentUrl("http://192.168.1.4:8080/movies/"))
    }

    @Test
    fun `HTTP 上级 - 地址非法时原样返回`() {
        assertEquals("not-a-url", FileBrowserLogic.httpParentUrl("not-a-url"))
    }

    @Test
    fun `HTTP 逻辑路径 - 去掉域名与首尾斜杠`() {
        assertEquals(
            "/nas/movies/action",
            FileBrowserLogic.httpLogicalPath("http://192.168.1.4/nas/movies/action/")
        )
        assertEquals("/movies", FileBrowserLogic.httpLogicalPath("http://192.168.1.4/movies"))
        assertEquals("", FileBrowserLogic.httpLogicalPath("http://192.168.1.4/"))
        assertEquals("", FileBrowserLogic.httpLogicalPath("http://192.168.1.4"))
        assertEquals("", FileBrowserLogic.httpLogicalPath(""))
    }

    @Test
    fun `HTTP 同源 - 同主机同端口的子路径通过`() {
        assertTrue(FileBrowserLogic.isHttpSubPath("http://192.168.1.4/movies/a.mkv", "http://192.168.1.4/movies/"))
        assertTrue(FileBrowserLogic.isHttpSubPath("http://192.168.1.4/movies/", "http://192.168.1.4/movies/"))
        assertTrue(
            FileBrowserLogic.isHttpSubPath("http://192.168.1.4/movies/action/", "http://192.168.1.4/movies/")
        )
    }

    @Test
    fun `HTTP 同源 - 跨主机、跨协议、跨端口的链接被拒`() {
        assertFalse(FileBrowserLogic.isHttpSubPath("http://cdn.example.com/movies/a.mkv", "http://192.168.1.4/movies/"))
        assertFalse(FileBrowserLogic.isHttpSubPath("https://192.168.1.4/movies/a.mkv", "http://192.168.1.4/movies/"))
        assertFalse(FileBrowserLogic.isHttpSubPath("http://192.168.1.4:8080/movies/a.mkv", "http://192.168.1.4/movies/"))
    }

    @Test
    fun `HTTP 同源 - 跳出当前目录的链接被拒`() {
        assertFalse(FileBrowserLogic.isHttpSubPath("http://192.168.1.4/other/a.mkv", "http://192.168.1.4/movies/"))
    }

    @Test
    fun `HTTP 同源 - 地址非法时返回 false 而不是抛异常`() {
        assertFalse(FileBrowserLogic.isHttpSubPath(":::", "http://192.168.1.4/movies/"))
        assertFalse(FileBrowserLogic.isHttpSubPath("http://192.168.1.4/movies/a.mkv", ":::"))
    }

    @Test
    fun `HTTP 链接解析 - 相对文件名挂到当前目录下`() {
        assertEquals(
            "http://192.168.1.4/movies/a.mkv",
            FileBrowserLogic.resolveHttpUrl("a.mkv", "http://192.168.1.4/movies/")
        )
    }

    @Test
    fun `HTTP 链接解析 - 以斜杠开头的链接回到站点根下`() {
        assertEquals(
            "http://192.168.1.4/other/a.mkv",
            FileBrowserLogic.resolveHttpUrl("/other/a.mkv", "http://192.168.1.4/movies/")
        )
    }

    @Test
    fun `HTTP 链接解析 - 本身是绝对地址时原样保留`() {
        assertEquals(
            "http://cdn.example.com/a.mkv",
            FileBrowserLogic.resolveHttpUrl("http://cdn.example.com/a.mkv", "http://192.168.1.4/movies/")
        )
    }

    // ────────────────────────────── WebDAV ──────────────────────────────

    @Test
    fun `WebDAV 拼接 - 父子两边的多余斜杠都会被吃掉`() {
        val expected = "http://192.168.1.4:5006/dav/movies/a.mkv"
        assertEquals(expected, FileBrowserLogic.joinUrlPath("http://192.168.1.4:5006/dav/movies", "a.mkv"))
        assertEquals(expected, FileBrowserLogic.joinUrlPath("http://192.168.1.4:5006/dav/movies/", "a.mkv"))
        assertEquals(expected, FileBrowserLogic.joinUrlPath("http://192.168.1.4:5006/dav/movies", "/a.mkv"))
        assertEquals(expected, FileBrowserLogic.joinUrlPath("http://192.168.1.4:5006/dav/movies/", "/a.mkv/"))
    }

    @Test
    fun `WebDAV 拼接 - 子目录结果带结尾斜杠`() {
        assertEquals(
            "http://192.168.1.4:5006/dav/movies/action/",
            FileBrowserLogic.joinUrlDirectory("http://192.168.1.4:5006/dav/movies/", "action")
        )
    }

    @Test
    fun `WebDAV 拼接 - 父路径为空时仍得到绝对路径`() {
        assertEquals("/a.mkv", FileBrowserLogic.joinUrlPath("", "a.mkv"))
    }

    @Test
    fun `WebDAV 结尾斜杠 - 缺则补上，已有则不重复加`() {
        assertEquals("http://host/dav/", FileBrowserLogic.ensureTrailingSlash("http://host/dav"))
        assertEquals("http://host/dav/", FileBrowserLogic.ensureTrailingSlash("http://host/dav/"))
    }
}
