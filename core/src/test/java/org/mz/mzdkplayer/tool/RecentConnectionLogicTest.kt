package org.mz.mzdkplayer.tool

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mz.mzdkplayer.data.model.RecentConnection
import org.mz.mzdkplayer.tool.logic.RecentConnectionLogic

/**
 * [RecentConnectionLogic] 的 JVM 单测。
 *
 * 关键契约：
 * 1. 最新打开的排最前，最多 [RecentConnectionLogic.MAX_ITEMS] 条；
 * 2. 同一个连接（协议 + 名称）只留一条，且路由与时间被最新那次覆盖；
 * 3. 不同协议下的同名连接互不覆盖；
 * 4. 脏数据（空名称 / 空路由 / 未知协议）一律不展示。
 */
class RecentConnectionLogicTest {

    private fun conn(
        type: String = RecentConnection.TYPE_SMB,
        name: String = "家中的NAS",
        route: String = "SMBFileListScreen/abc/def",
        time: Long = 1L
    ) = RecentConnection(type = type, name = name, route = route, lastVisited = time)

    // ---------- merge ----------

    @Test
    fun `新记录排在最前`() {
        val old = listOf(conn(name = "A", time = 10L), conn(name = "B", time = 20L))
        val result = RecentConnectionLogic.merge(old, conn(name = "C", time = 30L))
        assertEquals(listOf("C", "B", "A"), result.map { it.name })
    }

    @Test
    fun `同协议同名只保留一条并刷新路由和时间`() {
        val old = listOf(conn(name = "NAS", route = "old-route", time = 10L))
        val result = RecentConnectionLogic.merge(
            old,
            conn(name = "NAS", route = "new-route", time = 99L)
        )
        assertEquals(1, result.size)
        assertEquals("new-route", result.first().route)
        assertEquals(99L, result.first().lastVisited)
    }

    @Test
    fun `去重对大小写和首尾空白不敏感`() {
        val old = listOf(conn(name = "MyNas", time = 10L))
        val result = RecentConnectionLogic.merge(old, conn(name = "  mynas ", time = 20L))
        assertEquals(1, result.size)
        assertEquals("  mynas ", result.first().name)
    }

    @Test
    fun `不同协议的同名连接互不覆盖`() {
        val old = listOf(conn(type = RecentConnection.TYPE_SMB, name = "NAS", time = 10L))
        val result = RecentConnectionLogic.merge(
            old,
            conn(type = RecentConnection.TYPE_FTP, name = "NAS", route = "FTPFileListScreen/x", time = 20L)
        )
        assertEquals(2, result.size)
        assertEquals(RecentConnection.TYPE_FTP, result.first().type)
    }

    @Test
    fun `超过上限时裁掉最旧的`() {
        var list = emptyList<RecentConnection>()
        repeat(RecentConnectionLogic.MAX_ITEMS + 3) { index ->
            list = RecentConnectionLogic.merge(
                list,
                conn(name = "连接$index", route = "route/$index", time = index.toLong())
            )
        }
        assertEquals(RecentConnectionLogic.MAX_ITEMS, list.size)
        assertEquals("连接${RecentConnectionLogic.MAX_ITEMS + 2}", list.first().name)
        // 最旧的几条被裁掉
        assertFalse(list.any { it.name == "连接0" })
    }

    @Test
    fun `脏记录不会被写进列表`() {
        val old = emptyList<RecentConnection>()
        assertEquals(0, RecentConnectionLogic.merge(old, conn(name = "  ", time = 5L)).size)
        assertEquals(0, RecentConnectionLogic.merge(old, conn(route = "", time = 5L)).size)
        assertEquals(0, RecentConnectionLogic.merge(old, conn(type = "UPNP", time = 5L)).size)
    }

    @Test
    fun `合并脏记录时原列表仍做一次清洗`() {
        val old = listOf(conn(name = "", time = 10L), conn(name = "好的", time = 5L))
        val result = RecentConnectionLogic.merge(old, conn(name = "", time = 9L))
        assertEquals(listOf("好的"), result.map { it.name })
    }

    // ---------- sanitize ----------

    @Test
    fun `清洗会去重并按时间倒序`() {
        val raw = listOf(
            conn(name = "A", route = "a-old", time = 10L),
            conn(name = "B", route = "b", time = 30L),
            conn(name = "A", route = "a-new", time = 50L),
            conn(name = "C", route = "c", time = 20L)
        )
        val result = RecentConnectionLogic.sanitize(raw)
        assertEquals(listOf("A", "B", "C"), result.map { it.name })
        assertEquals("a-new", result.first().route)
    }

    @Test
    fun `清洗后只保留已知协议的记录`() {
        val raw = listOf(
            conn(type = RecentConnection.TYPE_WEBDAV, name = "W", route = "w", time = 1L),
            conn(type = "本地", name = "L", route = "l", time = 2L)
        )
        assertEquals(listOf("W"), RecentConnectionLogic.sanitize(raw).map { it.name })
    }

    // ---------- remove ----------

    @Test
    fun `remove 只删掉匹配的那一条`() {
        val raw = listOf(
            conn(name = "A", time = 3L),
            conn(type = RecentConnection.TYPE_NFS, name = "B", route = "nfs", time = 2L),
            conn(name = "C", time = 1L)
        )
        val result = RecentConnectionLogic.remove(raw, RecentConnection.TYPE_NFS, " b ")
        assertEquals(listOf("A", "C"), result.map { it.name })
    }

    // ---------- keyOf / isValid ----------

    @Test
    fun `keyOf 归一化后可作为去重键`() {
        assertEquals(
            RecentConnectionLogic.keyOf(conn(name = "NAS")),
            RecentConnectionLogic.keyOf(RecentConnection.TYPE_SMB, " nas ")
        )
    }

    @Test
    fun `isValid 只认已知协议加非空名称与路由`() {
        assertTrue(RecentConnectionLogic.isValid(conn()))
        assertFalse(RecentConnectionLogic.isValid(conn(route = "   ")))
        assertFalse(RecentConnectionLogic.isValid(conn(name = "\n")))
    }
}
