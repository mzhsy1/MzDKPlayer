package org.mz.mzdkplayer.tool

import org.junit.Assert.assertEquals
import org.junit.Test
import org.mz.mzdkplayer.tool.logic.FileSortField
import org.mz.mzdkplayer.tool.logic.FileSortLogic
import org.mz.mzdkplayer.tool.logic.FileSortOption

/**
 * [FileSortLogic] 的 JVM 单元测试。
 *
 * 排序看起来简单，踩坑点集中在三处，测试就围着这三处写：
 * 1. 「文件夹优先」是叠加在字段排序之上的，不能和字段/方向互相顶掉；
 * 2. 时间/大小取不到时是 0，不能因此把条目排到意想不到的位置，主键相同必须有稳定兜底；
 * 3. 排序键只能取一次（NFS 的 getAttributes / length 是 RPC），这条用计数器守住。
 */
class FileSortLogicTest {

    private data class Entry(
        val name: String,
        val isDirectory: Boolean = false,
        val modified: Long = 0L,
        val size: Long = 0L,
    )

    private fun sort(items: List<Entry>, option: FileSortOption): List<String> =
        FileSortLogic.sort(
            items = items,
            option = option,
            nameOf = { it.name },
            isDirectoryOf = { it.isDirectory },
            modifiedOf = { it.modified },
            sizeOf = { it.size },
        ).map { it.name }

    private val byNameAsc = FileSortOption(FileSortField.NAME, descending = false, foldersFirst = false)
    private val byNameDesc = FileSortOption(FileSortField.NAME, descending = true, foldersFirst = false)
    private val byTimeDesc = FileSortOption(FileSortField.TIME, descending = true, foldersFirst = false)
    private val byTimeAsc = FileSortOption(FileSortField.TIME, descending = false, foldersFirst = false)
    private val bySizeDesc = FileSortOption(FileSortField.SIZE, descending = true, foldersFirst = false)
    private val bySizeAsc = FileSortOption(FileSortField.SIZE, descending = false, foldersFirst = false)
    private val byType = FileSortOption(FileSortField.TYPE, descending = false, foldersFirst = false)

    // ────────────────────────────── 默认档 ──────────────────────────────

    @Test
    fun `默认档 - 文件夹优先且名称升序`() {
        val items = listOf(
            Entry("movie.mp4"),
            Entry("Zeta", isDirectory = true),
            Entry("audio.mp3"),
            Entry("Avatar", isDirectory = true),
        )
        assertEquals(
            listOf("Avatar", "Zeta", "audio.mp3", "movie.mp4"),
            sort(items, FileSortLogic.DEFAULT),
        )
    }

    @Test
    fun `默认档 - 名称比较忽略大小写`() {
        val items = listOf(Entry("beta"), Entry("Alpha"), Entry("gamma"))
        assertEquals(listOf("Alpha", "beta", "gamma"), sort(items, FileSortLogic.DEFAULT))
    }

    @Test
    fun `关闭文件夹优先 - 目录与文件按名称混排`() {
        val items = listOf(
            Entry("Zeta", isDirectory = true),
            Entry("alpha"),
            Entry("Beta", isDirectory = true),
        )
        assertEquals(listOf("alpha", "Beta", "Zeta"), sort(items, byNameAsc))
    }

    @Test
    fun `名称降序 - 文件夹仍置顶`() {
        val items = listOf(
            Entry("a.txt"),
            Entry("B", isDirectory = true),
            Entry("c.txt"),
            Entry("D", isDirectory = true),
        )
        assertEquals(
            listOf("D", "B", "c.txt", "a.txt"),
            sort(items, FileSortOption(FileSortField.NAME, descending = true, foldersFirst = true)),
        )
    }

    // ────────────────────────────── 时间 ──────────────────────────────

    @Test
    fun `时间降序 - 新的在前`() {
        val items = listOf(
            Entry("old.mkv", modified = 1_000L),
            Entry("new.mkv", modified = 9_000L),
            Entry("mid.mkv", modified = 5_000L),
        )
        assertEquals(listOf("new.mkv", "mid.mkv", "old.mkv"), sort(items, byTimeDesc))
    }

    @Test
    fun `时间升序 - 旧的在前`() {
        val items = listOf(
            Entry("new.mkv", modified = 9_000L),
            Entry("old.mkv", modified = 1_000L),
        )
        assertEquals(listOf("old.mkv", "new.mkv"), sort(items, byTimeAsc))
    }

    @Test
    fun `时间相同 - 回落名称升序保证稳定`() {
        val items = listOf(
            Entry("c.mkv", modified = 5_000L),
            Entry("a.mkv", modified = 5_000L),
            Entry("b.mkv", modified = 5_000L),
        )
        assertEquals(listOf("a.mkv", "b.mkv", "c.mkv"), sort(items, byTimeDesc))
    }

    @Test
    fun `时间未知 - 0 视为最旧，不参与乱序`() {
        val items = listOf(
            Entry("unknown.mkv", modified = 0L),
            Entry("known.mkv", modified = 7_000L),
        )
        assertEquals(listOf("known.mkv", "unknown.mkv"), sort(items, byTimeDesc))
        assertEquals(listOf("unknown.mkv", "known.mkv"), sort(items, byTimeAsc))
    }

    // ────────────────────────────── 大小 ──────────────────────────────

    @Test
    fun `大小降序与升序`() {
        val items = listOf(
            Entry("small.mkv", size = 10L),
            Entry("big.mkv", size = 900L),
            Entry("mid.mkv", size = 100L),
        )
        assertEquals(listOf("big.mkv", "mid.mkv", "small.mkv"), sort(items, bySizeDesc))
        assertEquals(listOf("small.mkv", "mid.mkv", "big.mkv"), sort(items, bySizeAsc))
    }

    // ────────────────────────────── 类型 ──────────────────────────────

    @Test
    fun `类型排序 - 按扩展名分组，同类型按名称`() {
        val items = listOf(
            Entry("b.mkv"),
            Entry("a.mp4"),
            Entry("c.mp4"),
            Entry("d.avi"),
        )
        assertEquals(listOf("d.avi", "b.mkv", "a.mp4", "c.mp4"), sort(items, byType))
    }

    @Test
    fun `类型排序 - 无扩展名排在最前`() {
        val items = listOf(
            Entry("b.mkv"),
            Entry("README"),
            Entry("a.mp4"),
        )
        // 扩展名字母序：""（无扩展名）< mkv < mp4
        assertEquals(listOf("README", "b.mkv", "a.mp4"), sort(items, byType))
    }

    // ────────────────────────────── 取值次数 ──────────────────────────────

    @Test
    fun `排序键 - 每个条目只取一次`() {
        var timeCalls = 0
        var sizeCalls = 0
        val items = (1..20).map { Entry("f$it.mkv", modified = it.toLong(), size = it.toLong()) }
        FileSortLogic.sort(
            items = items,
            option = byTimeDesc,
            nameOf = { it.name },
            isDirectoryOf = { it.isDirectory },
            modifiedOf = { timeCalls++; it.modified },
            sizeOf = { sizeCalls++; it.size },
        )
        assertEquals(20, timeCalls)
        assertEquals(0, sizeCalls)

        FileSortLogic.sort(
            items = items,
            option = byNameAsc,
            nameOf = { it.name },
            isDirectoryOf = { it.isDirectory },
            modifiedOf = { timeCalls++; it.modified },
            sizeOf = { sizeCalls++; it.size },
        )
        assertEquals(20, timeCalls)
        assertEquals(0, sizeCalls)
    }

    // ────────────────────────────── 编码 / 档位 ──────────────────────────────

    @Test
    fun `编码解码 - 往返一致`() {
        FileSortLogic.PRESETS.forEach { preset ->
            listOf(true, false).forEach { foldersFirst ->
                val option = preset.copy(foldersFirst = foldersFirst)
                assertEquals(option, FileSortLogic.decode(FileSortLogic.encode(option)))
            }
        }
    }

    @Test
    fun `编码 - 空值与非法值都收敛回默认档`() {
        assertEquals(FileSortLogic.DEFAULT, FileSortLogic.decode(null))
        assertEquals(FileSortLogic.DEFAULT, FileSortLogic.decode(""))
        assertEquals(FileSortLogic.DEFAULT, FileSortLogic.decode("garbage"))
        assertEquals(FileSortLogic.DEFAULT, FileSortLogic.decode("name-asc"))
        assertEquals(FileSortLogic.DEFAULT, FileSortLogic.decode("name-sideways-dirs"))
        assertEquals(
            FileSortLogic.encode(FileSortLogic.DEFAULT),
            FileSortLogic.normalize("name-sideways-dirs"),
        )
    }

    @Test
    fun `档位循环 - 走完固定档位后回到起点，文件夹开关不被改动`() {
        var option = FileSortOption(FileSortField.NAME, descending = false, foldersFirst = true)
        val seen = mutableListOf(FileSortLogic.encode(option))
        repeat(FileSortLogic.PRESETS.size) {
            option = FileSortLogic.next(option)
            assertEquals(true, option.foldersFirst)
            seen += FileSortLogic.encode(option)
        }
        assertEquals(FileSortLogic.PRESETS.size + 1, seen.size)
        assertEquals(seen.first(), seen.last())
        assertEquals(FileSortLogic.PRESETS.size, seen.distinct().size)
    }

    // ────────────────────────────── 边界 ──────────────────────────────

    @Test
    fun `空列表与单条列表 - 原样返回`() {
        val single = listOf(Entry("only.mkv"))
        assertEquals(single, FileSortLogic.sort(single, byTimeDesc, { it.name }, { it.isDirectory }, { it.modified }, { it.size }))
        assertEquals(
            emptyList<Entry>(),
            FileSortLogic.sort(emptyList<Entry>(), byTimeDesc, { it.name }, { it.isDirectory }),
        )
    }
}
