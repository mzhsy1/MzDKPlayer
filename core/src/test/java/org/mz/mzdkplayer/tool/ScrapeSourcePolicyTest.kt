package org.mz.mzdkplayer.tool

import org.junit.Assert.assertEquals
import org.junit.Test
import org.mz.mzdkplayer.tool.logic.ScrapeSourcePolicy

/**
 * [ScrapeSourcePolicy] 的 JVM 单测。
 *
 * 关键契约：**默认首选豆瓣**；两个源永远都在顺序里（首选失败要用另一个兜底）；
 * 脏数据不能把顺序算成空列表（那样就一个源都不查了）。
 */
class ScrapeSourcePolicyTest {

    @Test
    fun `默认首选豆瓣`() {
        assertEquals(ScrapeSourcePolicy.DOUBAN, ScrapeSourcePolicy.DEFAULT)
        assertEquals(ScrapeSourcePolicy.DOUBAN, ScrapeSourcePolicy.normalize(null))
        assertEquals(ScrapeSourcePolicy.DOUBAN, ScrapeSourcePolicy.normalize(""))
        assertEquals(ScrapeSourcePolicy.DOUBAN, ScrapeSourcePolicy.normalize("   "))
    }

    @Test
    fun `首选豆瓣时顺序是豆瓣在前`() {
        assertEquals(
            listOf(ScrapeSourcePolicy.DOUBAN, ScrapeSourcePolicy.TMDB),
            ScrapeSourcePolicy.order(ScrapeSourcePolicy.DOUBAN)
        )
    }

    @Test
    fun `首选 TMDB 时顺序是 TMDB 在前`() {
        assertEquals(
            listOf(ScrapeSourcePolicy.TMDB, ScrapeSourcePolicy.DOUBAN),
            ScrapeSourcePolicy.order(ScrapeSourcePolicy.TMDB)
        )
    }

    @Test
    fun `脏数据回退默认且顺序不会缺源`() {
        listOf("", "  ", "tmdb ", "TMDB", "douban", "netflix", "预置").forEach { raw ->
            val order = ScrapeSourcePolicy.order(raw)
            assertEquals("<$raw> 顺序长度", 2, order.size)
            assertEquals("<$raw> 含豆瓣", true, order.contains(ScrapeSourcePolicy.DOUBAN))
            assertEquals("<$raw> 含 TMDB", true, order.contains(ScrapeSourcePolicy.TMDB))
        }
        // 大小写与空白按 TMDB 处理
        assertEquals(listOf(ScrapeSourcePolicy.TMDB, ScrapeSourcePolicy.DOUBAN), ScrapeSourcePolicy.order(" TMDB "))
        // 无法识别的值一律按默认（豆瓣优先）
        assertEquals(listOf(ScrapeSourcePolicy.DOUBAN, ScrapeSourcePolicy.TMDB), ScrapeSourcePolicy.order("netflix"))
    }
}
