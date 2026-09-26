package org.mz.mzdkplayer.tool

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * [SubtitleMatchLogic] 的 JVM 单元测试 —— 即「哪些文件算视频的同名字幕」这条规则。
 *
 * 播放时能否自动挂上字幕，全看这里的判定结果：判宽了会把无关文件当字幕加载，
 * 判窄了用户明明放了 `.chs.srt` 却不生效。因此对「相等 / 前缀 / 扩展名」三条边界都做了钉住。
 *
 * 期望值均与抽取前的 `SubtitleScanner` 原实现逐字对齐。
 */
class SubtitleMatchTest {

    // ────────────────────────────── 命中：基础与后缀变体 ──────────────────────────────

    @Test
    fun `同名 - 完全同名的 srt 命中`() {
        assertTrue(SubtitleMatchLogic.isSameNameSubtitle("影片.srt", "影片.mkv"))
    }

    @Test
    fun `同名 - 语言后缀变体命中`() {
        // 以「视频名.」开头即可，后面挂多少段都不影响
        assertTrue(SubtitleMatchLogic.isSameNameSubtitle("影片.chs.srt", "影片.mkv"))
        assertTrue(SubtitleMatchLogic.isSameNameSubtitle("影片.zh-CN.ass", "影片.mkv"))
        assertTrue(SubtitleMatchLogic.isSameNameSubtitle("影片.国语.繁体.vtt", "影片.mkv"))
    }

    @Test
    fun `同名 - 中文名与空格不受影响`() {
        assertTrue(SubtitleMatchLogic.isSameNameSubtitle("我的电影 2019.srt", "我的电影 2019.mkv"))
        assertTrue(SubtitleMatchLogic.isSameNameSubtitle("我的电影 2019.简中.sup", "我的电影 2019.mp4"))
    }

    @Test
    fun `同名 - 扩展名大小写不敏感`() {
        assertTrue(SubtitleMatchLogic.isSameNameSubtitle("影片.SRT", "影片.mkv"))
        assertTrue(SubtitleMatchLogic.isSameNameSubtitle("影片.Chs.Ass", "影片.mkv"))
    }

    @Test
    fun `同名 - 视频名不含扩展名时按整名匹配`() {
        // videoName 没有 '.' 时 baseName 就是它自身
        assertTrue(SubtitleMatchLogic.isSameNameSubtitle("影片.srt", "影片"))
    }

    @Test
    fun `同名 - 全部支持的字幕扩展名都能命中`() {
        SubtitleMatchLogic.SUBTITLE_EXTENSIONS.forEach { ext ->
            assertTrue("扩展名 $ext 应当命中", SubtitleMatchLogic.isSameNameSubtitle("影片.$ext", "影片.mkv"))
        }
    }

    // ────────────────────────────── 不命中：边界 ──────────────────────────────

    @Test
    fun `不命中 - 视频自身不算字幕`() {
        assertFalse(SubtitleMatchLogic.isSameNameSubtitle("影片.mkv", "影片.mkv"))
        assertFalse(SubtitleMatchLogic.isSameNameSubtitle("影片.MKV", "影片.mkv"))
    }

    @Test
    fun `不命中 - 仅前缀相似但有额外字符`() {
        // 必须以「视频名 + .」开头，"影片2.srt" / "影片A.srt" 都不算
        assertFalse(SubtitleMatchLogic.isSameNameSubtitle("影片2.srt", "影片.mkv"))
        assertFalse(SubtitleMatchLogic.isSameNameSubtitle("影片A.srt", "影片.mkv"))
        assertFalse(SubtitleMatchLogic.isSameNameSubtitle("影片加长版.srt", "影片.mkv"))
    }

    @Test
    fun `不命中 - 扩展名不属于字幕集合`() {
        assertFalse(SubtitleMatchLogic.isSameNameSubtitle("影片.txt", "影片.mkv"))
        assertFalse(SubtitleMatchLogic.isSameNameSubtitle("影片.nfo", "影片.mkv"))
        assertFalse(SubtitleMatchLogic.isSameNameSubtitle("影片.mkv", "影片.mkv"))
        // 字幕后面又挂了别的后缀，按最后一段判定
        assertFalse(SubtitleMatchLogic.isSameNameSubtitle("影片.srt.bak", "影片.mkv"))
    }

    @Test
    fun `不命中 - 视频名为点开头的隐藏文件`() {
        // ".mkv" 的 baseName 为空，无法构成「视频名.」前缀
        assertFalse(SubtitleMatchLogic.isSameNameSubtitle(".srt", ".mkv"))
    }

    @Test
    fun `不命中 - 其他视频的同名字幕`() {
        assertFalse(SubtitleMatchLogic.isSameNameSubtitle("另一部影片.srt", "影片.mkv"))
    }

    // ────────────────────────────── 列表筛选：去重与排序 ──────────────────────────────

    @Test
    fun `筛选 - 从目录列表中挑出同名字幕并去重排序`() {
        val dir = listOf(
            "影片.mkv",
            "影片.chs.srt",
            "影片.srt",
            "影片.nfo",
            "海报.jpg",
            "影片.srt", // 重复项只保留一次
            "别的影片.srt",
        )
        assertEquals(
            listOf("影片.chs.srt", "影片.srt"),
            SubtitleMatchLogic.matchSameNameSubtitleNames("影片.mkv", dir)
        )
    }

    @Test
    fun `筛选 - 没有同名字幕时返回空列表`() {
        val dir = listOf("影片.mkv", "海报.jpg", "影片.nfo")
        assertEquals(emptyList<String>(), SubtitleMatchLogic.matchSameNameSubtitleNames("影片.mkv", dir))
    }

    @Test
    fun `筛选 - 空目录列表返回空列表`() {
        assertEquals(emptyList<String>(), SubtitleMatchLogic.matchSameNameSubtitleNames("影片.mkv", emptyList()))
    }

    // ────────────────────────────── 目录前缀与 URI 拼装 ──────────────────────────────

    @Test
    fun `前缀 - 截取到最后一个斜杠并保留斜杠`() {
        assertEquals("smb://nas/media/", SubtitleMatchLogic.dirPrefixOf("smb://nas/media/影片.mkv"))
        assertEquals("/storage/movies/", SubtitleMatchLogic.dirPrefixOf("/storage/movies/影片.mkv"))
        assertEquals("nfs://host:/export/", SubtitleMatchLogic.dirPrefixOf("nfs://host:/export/影片.mkv"))
    }

    @Test
    fun `前缀 - 没有斜杠时返回空串`() {
        assertEquals("", SubtitleMatchLogic.dirPrefixOf("影片.mkv"))
    }

    @Test
    fun `拼装 - 字幕 URI 复用视频的目录前缀（含账号密码与端口）`() {
        val pairs = SubtitleMatchLogic.buildSubtitlePairs(
            videoUri = "smb://user:pass@192.168.1.2/share/Movies/影片.mkv",
            videoName = "影片.mkv",
            dirNames = listOf("影片.mkv", "影片.chs.srt")
        )
        assertEquals(
            listOf("smb://user:pass@192.168.1.2/share/Movies/影片.chs.srt" to "影片.chs.srt"),
            pairs
        )
    }

    @Test
    fun `拼装 - FTP 的端口与路径前缀被保留`() {
        val pairs = SubtitleMatchLogic.buildSubtitlePairs(
            videoUri = "ftp://user:pwd@host:2121/anime/影片.mkv",
            videoName = "影片.mkv",
            dirNames = listOf("影片.ass")
        )
        assertEquals(listOf("ftp://user:pwd@host:2121/anime/影片.ass" to "影片.ass"), pairs)
    }

    @Test
    fun `拼装 - 无同名字幕时返回空列表`() {
        val pairs = SubtitleMatchLogic.buildSubtitlePairs(
            videoUri = "smb://nas/share/影片.mkv",
            videoName = "影片.mkv",
            dirNames = listOf("影片.mkv", "海报.jpg")
        )
        assertEquals(emptyList<Pair<String, String>>(), pairs)
    }
}
