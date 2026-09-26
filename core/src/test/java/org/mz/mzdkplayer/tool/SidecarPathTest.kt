package org.mz.mzdkplayer.tool

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * [SidecarPathLogic] 的 JVM 单元测试 —— 由视频路径推出伴生文件（弹幕 `.xml` / 刮削信息 `.nfo`）的路径。
 *
 * 这块逻辑原本在项目里有四份独立实现（SMB 弹幕、NFS 弹幕、非 NFS 的 NFO、NFS 的 NFO），
 * 「路径没有扩展名时怎么办」四处口径不同。现在统一走 [SidecarPathLogic.withExtension]：
 * **有扩展名就换、没有扩展名就在末尾追加，永不返回 null、永不丢掉原文件名。**
 *
 * 用例分两段：
 * - 第一段钉住 [withExtension] 本身的行为（含「点落在目录名上」「点开头的隐藏文件」两个易错点）；
 * - 第二段用这些原语**复刻四个调用方的组合口径**（调用方本身绑着 `android.net.Uri`，
 *   无法在 JVM 上直接跑），确认四处已经完全一致。
 */
class SidecarPathTest {

    // ────────────────────────────── withExtension ──────────────────────────────

    @Test
    fun `换后缀 - 常规路径只换最后一段扩展名`() {
        assertEquals("/share/Movies/影片.xml", SidecarPathLogic.withExtension("/share/Movies/影片.mkv", ".xml"))
        assertEquals("/share/Movies/影片.nfo", SidecarPathLogic.withExtension("/share/Movies/影片.mkv", ".nfo"))
        // 语言后缀保留，不会被一起吃
        assertEquals("/m/影片.chs.nfo", SidecarPathLogic.withExtension("/m/影片.chs.mkv", ".nfo"))
        assertEquals("/m/a.b.nfo", SidecarPathLogic.withExtension("/m/a.b.c", ".nfo"))
        // 后缀传空串时等价于「去掉扩展名」
        assertEquals("/m/a.b", SidecarPathLogic.withExtension("/m/a.b.c", ""))
    }

    @Test
    fun `换后缀 - 没有扩展名时在末尾追加`() {
        assertEquals("/share/Movies/影片.xml", SidecarPathLogic.withExtension("/share/Movies/影片", ".xml"))
        assertEquals("影片.nfo", SidecarPathLogic.withExtension("影片", ".nfo"))
    }

    @Test
    fun `换后缀 - 点落在目录名上时不算扩展名`() {
        // 关键修复点：只看「最后一段路径」里的点，否则 "/movies.v2/影片" 会被切成 "/movies.xml"
        assertEquals(
            "/movies.v2/影片.xml",
            SidecarPathLogic.withExtension("/movies.v2/影片", ".xml")
        )
        assertEquals(
            "/movies.v2/影片.nfo",
            SidecarPathLogic.withExtension("/movies.v2/影片.mkv", ".nfo")
        )
    }

    @Test
    fun `换后缀 - 点开头的隐藏文件不当作扩展名`() {
        // 最后一个点就是首字符时，整串视为「没有扩展名」，追加而不是替换
        assertEquals(".hidden.xml", SidecarPathLogic.withExtension(".hidden", ".xml"))
        assertEquals("/m/.hidden.xml", SidecarPathLogic.withExtension("/m/.hidden", ".xml"))
    }

    @Test
    fun `换后缀 - 点结尾时按替换处理`() {
        assertEquals("/m/trailing.xml", SidecarPathLogic.withExtension("/m/trailing.", ".xml"))
    }

    @Test
    fun `换后缀 - 根目录下的视频`() {
        assertEquals("/影片.xml", SidecarPathLogic.withExtension("/影片.mkv", ".xml"))
        // 没有斜杠时 lastSlashIndex 为 -1，点仍然算扩展名
        assertEquals("影片.xml", SidecarPathLogic.withExtension("影片.mkv", ".xml"))
    }

    @Test
    fun `换后缀 - 空路径退化成只有后缀（调用方需自行拦住空文件名）`() {
        // 单测把这条边界显式钉住：调用方在传空串前应先判空
        assertEquals(".xml", SidecarPathLogic.withExtension("", ".xml"))
    }

    // ────────────────────────────── directoryOf / fileNameOf ──────────────────────────────

    @Test
    fun `目录与文件名 - 常规拆分`() {
        assertEquals("/movies/", SidecarPathLogic.directoryOf("/movies/a.mkv"))
        assertEquals("a.mkv", SidecarPathLogic.fileNameOf("/movies/a.mkv"))
    }

    @Test
    fun `目录与文件名 - 没有斜杠时整串当文件名`() {
        assertEquals("", SidecarPathLogic.directoryOf("a.mkv"))
        assertEquals("a.mkv", SidecarPathLogic.fileNameOf("a.mkv"))
    }

    @Test
    fun `目录与文件名 - 单层与结尾斜杠`() {
        assertEquals("/", SidecarPathLogic.directoryOf("/a.mkv"))
        assertEquals("a.mkv", SidecarPathLogic.fileNameOf("/a.mkv"))
        assertEquals("/movies/", SidecarPathLogic.directoryOf("/movies/"))
        assertEquals("", SidecarPathLogic.fileNameOf("/movies/"))
    }

    // ────────────────────────────── NFS 拆分与拼接 ──────────────────────────────

    @Test
    fun `NFS - 按第一个冒号拆分导出目录与导出内路径`() {
        assertEquals(
            "fs/1000/nfs" to "/movies/影片.mkv",
            SidecarPathLogic.splitNfsRaw("/fs/1000/nfs:/movies/影片.mkv")
        )
    }

    @Test
    fun `NFS - 导出内路径原样返回不补前导斜杠`() {
        // 这一点与 FileTimeParse.splitNfsPath 不同（那个会补齐 '/'），不能混用
        assertEquals(
            "export" to "movies/影片.mkv",
            SidecarPathLogic.splitNfsRaw("/export:movies/影片.mkv")
        )
    }

    @Test
    fun `NFS - 导出目录为空时不报错由调用方判定`() {
        assertEquals("" to "x", SidecarPathLogic.splitNfsRaw("/:x"))
        assertEquals("export" to "", SidecarPathLogic.splitNfsRaw("/export:"))
    }

    @Test
    fun `NFS - 没有冒号时返回 null`() {
        assertNull(SidecarPathLogic.splitNfsRaw("/no/colon/here.mkv"))
    }

    @Test
    fun `NFS - 拼回路径保留冒号且带前导斜杠`() {
        assertEquals(
            "/fs/1000/nfs:/movies/影片.xml",
            SidecarPathLogic.joinNfsPath("fs/1000/nfs", "/movies/影片.xml")
        )
    }

    // ══════════════════════════ 四个调用方的组合口径 ══════════════════════════
    //
    // 以下私有函数复刻调用方（SmbUtils / NfoReader）里的组合方式。
    // 四个实现现在用的是同一条 withExtension 规则，因此下面四组用例的期望值应当完全同构。

    /** SMB 弹幕：`SmbUtils.getDanmakuSmbUri` —— 直接对整个 path 换后缀。 */
    private fun danmakuSmbPath(videoPath: String): String =
        SidecarPathLogic.withExtension(videoPath, ".xml")

    /** NFS 弹幕：`SmbUtils.getDanmakuNfsUri` —— 目录与文件名分开处理；文件名为空属结构错误。 */
    private fun danmakuNfsPath(nfsPath: String): String? {
        val (exportedPath, pathWithinExport) =
            SidecarPathLogic.splitNfsRaw(nfsPath) ?: return null
        val fileName = SidecarPathLogic.fileNameOf(pathWithinExport)
        if (pathWithinExport.isEmpty() || fileName.isEmpty()) return null
        val danmakuInner = SidecarPathLogic.directoryOf(pathWithinExport) +
                SidecarPathLogic.withExtension(fileName, ".xml")
        return SidecarPathLogic.joinNfsPath(exportedPath, danmakuInner)
    }

    /** 非 NFS 的 NFO：`NfoReader.constructNfoUri` —— 对整个 path 换后缀。 */
    private fun nfoPath(videoPath: String): String =
        SidecarPathLogic.withExtension(videoPath, ".nfo")

    /** NFS 的 NFO：`NfoReader.getNfoNfsUri` —— 对导出内路径整体换后缀。 */
    private fun nfoNfsPath(nfsPath: String): String? {
        val (exportedPath, pathWithinExport) =
            SidecarPathLogic.splitNfsRaw(nfsPath) ?: return null
        return SidecarPathLogic.joinNfsPath(
            exportedPath,
            SidecarPathLogic.withExtension(pathWithinExport, ".nfo")
        )
    }

    @Test
    fun `弹幕 - SMB 同目录改名`() {
        assertEquals("/share/Movies/影片.xml", danmakuSmbPath("/share/Movies/影片.mkv"))
        assertEquals("/share/Movies/影片.xml", danmakuSmbPath("/share/Movies/影片.mp4"))
    }

    @Test
    fun `弹幕 - NFS 同目录改名并保留导出目录`() {
        assertEquals(
            "/fs/1000/nfs:/movies/影片.xml",
            danmakuNfsPath("/fs/1000/nfs:/movies/影片.mkv")
        )
    }

    @Test
    fun `弹幕 - NFS 文件在导出根目录下时不带目录段`() {
        assertEquals("/export:影片.xml", danmakuNfsPath("/export:影片.mkv"))
    }

    @Test
    fun `弹幕 - 无扩展名时四种实现口径终于一致`() {
        // SMB 以前会悄悄退化成根目录的 ".xml"，NFS 以前直接抛异常；现在都是「同目录 + 追加」
        assertEquals("/share/Movies/影片.xml", danmakuSmbPath("/share/Movies/影片"))
        assertEquals("/fs/1000/nfs:/movies/影片.xml", danmakuNfsPath("/fs/1000/nfs:/movies/影片"))
        assertEquals("/share/Movies/影片.nfo", nfoPath("/share/Movies/影片"))
        assertEquals("/export:影片.nfo", nfoNfsPath("/export:影片"))
    }

    @Test
    fun `弹幕 - NFS 路径以斜杠结尾属结构错误由调用方拦住`() {
        assertNull(danmakuNfsPath("/fs/1000/nfs:/movies/"))
    }

    @Test
    fun `NFO - 非 NFS 协议按后缀改名`() {
        assertEquals("/share/Movies/影片.nfo", nfoPath("/share/Movies/影片.mkv"))
        assertEquals("/share/Movies/影片.chs.nfo", nfoPath("/share/Movies/影片.chs.mkv"))
    }

    @Test
    fun `NFO - NFS 协议改名`() {
        assertEquals(
            "/fs/1000/nfs:/movies/影片.nfo",
            nfoNfsPath("/fs/1000/nfs:/movies/影片.mkv")
        )
    }

    @Test
    fun `NFO - NFS 路径不含冒号时返回 null`() {
        assertNull(nfoNfsPath("/no/colon/影片.mkv"))
    }

    @Test
    fun `NFO - 点落在目录名上不再切错`() {
        // 修复前：四个实现都用「整串最后一个点」，"/movies.v2/影片" 会被切成 "/movies.nfo"
        assertEquals("/export:/movies.v2/影片.nfo", nfoNfsPath("/export:/movies.v2/影片"))
        assertEquals("/export:/movies.v2/影片.nfo", nfoNfsPath("/export:/movies.v2/影片.mkv"))
    }
}
