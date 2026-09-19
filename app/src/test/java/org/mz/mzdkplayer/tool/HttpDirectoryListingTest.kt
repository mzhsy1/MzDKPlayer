package org.mz.mzdkplayer.tool

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * [FileBrowserLogic.parseHttpDirectoryListing] 的 JVM 单元测试。
 *
 * 这段逻辑负责把 Nginx autoindex / Apache 目录页的 HTML 变成条目列表，
 * 正则、跨域过滤、大小提取都是「改一个字就全崩」的地方，所以覆盖得细一些。
 *
 * 测试数据用的是 Nginx autoindex 的真实排版：
 * `<pre><a href="x/">x/</a>                 21-Sep-2026 10:00                  -`
 */
class HttpDirectoryListingTest {

    private val baseUrl = "http://192.168.1.4/movies/"

    /** 目录行：Nginx 对目录的大小列固定是 `-` */
    private fun dirRow(name: String): String =
        "<a href=\"$name/\">$name/</a>" + " ".repeat(20) + "21-Sep-2026 10:00                  -"

    /** 文件行：末尾是字节数 */
    private fun fileRow(name: String, size: Long): String =
        "<a href=\"$name\">$name</a>" + " ".repeat(20) + "21-Sep-2026 10:05            $size"

    /** 拼一份最小可用的目录页，包含 Nginx 一定会输出的 `../` 行 */
    private fun nginxPage(vararg rows: String): String = buildString {
        append("<html><head><title>Index of /movies/</title></head><body>\n")
        append("<h1>Index of /movies/</h1><hr><pre><a href=\"../\">../</a>\n")
        rows.forEach { append(it).append('\n') }
        append("</pre><hr></body></html>")
    }

    // ────────────────────────────── 基本解析 ──────────────────────────────

    @Test
    fun `目录页 - 识别目录与文件并分别取大小`() {
        val entries = FileBrowserLogic.parseHttpDirectoryListing(
            nginxPage(dirRow("action"), fileRow("电影.mkv", 1048576L)),
            baseUrl
        )

        assertEquals(listOf("action", "电影.mkv"), entries.map { it.name })
        assertTrue(entries[0].isDirectory)
        assertEquals(0L, entries[0].size)
        assertFalse(entries[1].isDirectory)
        assertEquals(1048576L, entries[1].size)
    }

    @Test
    fun `目录页 - 上级目录链接不会出现在结果里`() {
        val entries = FileBrowserLogic.parseHttpDirectoryListing(
            nginxPage(fileRow("a.mkv", 100L)),
            baseUrl
        )

        assertFalse(entries.any { it.name == ".." })
        assertEquals(listOf("a.mkv"), entries.map { it.name })
    }

    @Test
    fun `目录页 - 点与双点链接被过滤`() {
        val html = nginxPage(
            "<a href=\"./\">./</a>" + " ".repeat(20) + "21-Sep-2026 10:00                  -",
            dirRow("action")
        )
        assertEquals(listOf("action"), FileBrowserLogic.parseHttpDirectoryListing(html, baseUrl).map { it.name })
    }

    @Test
    fun `目录页 - 目录条目即使附带了数字也不解析大小`() {
        val html = nginxPage(
            "<a href=\"action/\">action/</a>" + " ".repeat(20) + "21-Sep-2026 10:00            4096"
        )
        val entry = FileBrowserLogic.parseHttpDirectoryListing(html, baseUrl).single()
        assertTrue(entry.isDirectory)
        assertEquals(0L, entry.size)
    }

    @Test
    fun `目录页 - 保留原始 href 供播放时再按当前目录解析`() {
        val entry = FileBrowserLogic.parseHttpDirectoryListing(
            nginxPage(fileRow("a.mkv", 100L)),
            baseUrl
        ).single()

        assertEquals("a.mkv", entry.href)
        assertEquals("http://192.168.1.4/movies/a.mkv", FileBrowserLogic.resolveHttpUrl(entry.href, baseUrl))
    }

    // ────────────────────────────── 过滤规则 ──────────────────────────────

    @Test
    fun `目录页 - 锚点与 javascript 链接被过滤`() {
        val html = nginxPage(
            "<a href=\"#top\">回到顶部</a>",
            "<a href=\"javascript:void(0)\">坏链接</a>",
            fileRow("电影.mkv", 100L)
        )
        assertEquals(listOf("电影.mkv"), FileBrowserLogic.parseHttpDirectoryListing(html, baseUrl).map { it.name })
    }

    @Test
    fun `目录页 - 跨域与跳出当前目录的链接被过滤`() {
        val html = nginxPage(
            fileRow("a.mkv", 100L),
            "<a href=\"http://cdn.example.com/b.mkv\">b.mkv</a>",
            "<a href=\"/other/c.mkv\">c.mkv</a>",
            "<a href=\"https://192.168.1.4/movies/d.mkv\">d.mkv</a>"
        )
        assertEquals(listOf("a.mkv"), FileBrowserLogic.parseHttpDirectoryListing(html, baseUrl).map { it.name })
    }

    @Test
    fun `目录页 - 未知协议的链接被跳过而不影响整页解析`() {
        // mailto: / ftp: 这类 href 无法用 URL(base, href) 解析，历史上会让整个列表解析抛异常
        val html = nginxPage(
            "<a href=\"mailto:admin@example.com\">联系管理员</a>",
            "<a href=\"ftp://192.168.1.4/movies/x.mkv\">x.mkv</a>",
            fileRow("a.mkv", 100L)
        )
        assertEquals(listOf("a.mkv"), FileBrowserLogic.parseHttpDirectoryListing(html, baseUrl).map { it.name })
    }

    @Test
    fun `目录页 - baseUrl 缺少结尾斜杠时同级条目会被判成不在子树下`() {
        // 调用方必须传带结尾 '/' 的目录地址：URL("http://host/movies", "a.mkv") 会解析成
        // http://host/a.mkv，于是被「子树校验」挡掉。字幕扫描那边就是靠补 '/' 规避的。
        val html = nginxPage(fileRow("a.mkv", 100L))
        assertEquals(
            emptyList<String>(),
            FileBrowserLogic.parseHttpDirectoryListing(html, "http://192.168.1.4/movies").map { it.name }
        )
    }

    @Test
    fun `目录页 - 同名条目只保留第一次出现的那个`() {
        val entries = FileBrowserLogic.parseHttpDirectoryListing(
            nginxPage(fileRow("a.mkv", 100L), fileRow("a.mkv", 200L)),
            baseUrl
        )
        assertEquals(1, entries.size)
        assertEquals(100L, entries.first().size)
    }

    // ────────────────────────────── 编码与容错 ──────────────────────────────

    @Test
    fun `目录页 - 百分号编码的文件名会解码`() {
        val html = nginxPage(
            "<a href=\"%E7%94%B5%E5%BD%B1.mkv\">%E7%94%B5%E5%BD%B1.mkv</a>" +
                    " ".repeat(20) + "21-Sep-2026 10:05            100"
        )
        assertEquals(listOf("电影.mkv"), FileBrowserLogic.parseHttpDirectoryListing(html, baseUrl).map { it.name })
    }

    @Test
    fun `目录页 - 非法百分号转义时按原样使用而不抛异常`() {
        // 裸 % 不是合法的转义序列，解码会失败；这里要求降级成原字符串而不是让整个列表报错
        val html = nginxPage(
            "<a href=\"100%.mkv\">100%.mkv</a>" + " ".repeat(20) + "21-Sep-2026 10:05            100"
        )
        assertEquals(listOf("100%.mkv"), FileBrowserLogic.parseHttpDirectoryListing(html, baseUrl).map { it.name })
    }

    @Test
    fun `目录页 - 空页面与无链接页面返回空列表`() {
        assertTrue(FileBrowserLogic.parseHttpDirectoryListing("", baseUrl).isEmpty())
        assertTrue(
            FileBrowserLogic.parseHttpDirectoryListing("<html><body>empty</body></html>", baseUrl).isEmpty()
        )
    }

    // ────────────────────────────── 标签写法兼容 ──────────────────────────────

    @Test
    fun `目录页 - 大写标签与单引号属性也能解析`() {
        val html = nginxPage(
            "<A HREF='b.mkv'>b.mkv</A>" + " ".repeat(20) + "21-Sep-2026 10:05            2048"
        )
        val entries = FileBrowserLogic.parseHttpDirectoryListing(html, baseUrl)
        assertEquals(listOf("b.mkv"), entries.map { it.name })
        assertEquals(2048L, entries.first().size)
    }

    @Test
    fun `目录页 - href 前面还有别的属性也能解析`() {
        val html = nginxPage(
            "<a class=\"link\" href=\"c.mkv\" title=\"c\">c.mkv</a>" +
                    " ".repeat(20) + "21-Sep-2026 10:05            4096"
        )
        val entries = FileBrowserLogic.parseHttpDirectoryListing(html, baseUrl)
        assertEquals(listOf("c.mkv"), entries.map { it.name })
        assertEquals(4096L, entries.first().size)
    }

    // ────────────────────────────── 大小解析 ──────────────────────────────

    @Test
    fun `nginx 大小 - 取行尾的字节数`() {
        assertEquals(1048576L, FileBrowserLogic.parseNginxSize("      21-Sep-2026 10:05            1048576"))
        assertEquals(0L, FileBrowserLogic.parseNginxSize("      21-Sep-2026 10:05                  -"))
    }

    @Test
    fun `nginx 大小 - 取不到数字时返回 0`() {
        assertEquals(0L, FileBrowserLogic.parseNginxSize(""))
        assertEquals(0L, FileBrowserLogic.parseNginxSize("   "))
        assertEquals(0L, FileBrowserLogic.parseNginxSize("21-Sep-2026 10:05"))
        assertEquals(0L, FileBrowserLogic.parseNginxSize("21-Sep-2026 10:05  12.5K"))
    }
}
