package org.mz.mzdkplayer.tool

import org.junit.Assert.assertEquals
import org.junit.Test
import org.mz.mzdkplayer.tool.logic.ScrapeSourcePolicy

/**
 * [ScrapeSourcePolicy] 的 JVM 单测。
 *
 * 关键契约：**默认豆瓣**；设置里选谁就只返回谁 —— 不再有「兜底顺序」，也不会返回第二个源；
 * 脏数据（空值 / 旧版本残留 / 大小写与空白）一律回退默认值。
 */
class ScrapeSourcePolicyTest {

    @Test
    fun `默认选豆瓣`() {
        assertEquals(ScrapeSourcePolicy.DOUBAN, ScrapeSourcePolicy.DEFAULT)
        assertEquals(ScrapeSourcePolicy.DOUBAN, ScrapeSourcePolicy.normalize(null))
        assertEquals(ScrapeSourcePolicy.DOUBAN, ScrapeSourcePolicy.normalize(""))
        assertEquals(ScrapeSourcePolicy.DOUBAN, ScrapeSourcePolicy.normalize("   "))
    }

    @Test
    fun `选豆瓣就只刮豆瓣`() {
        assertEquals(ScrapeSourcePolicy.DOUBAN, ScrapeSourcePolicy.resolve(ScrapeSourcePolicy.DOUBAN))
    }

    @Test
    fun `选 TMDB 就只刮 TMDB`() {
        assertEquals(ScrapeSourcePolicy.TMDB, ScrapeSourcePolicy.resolve(ScrapeSourcePolicy.TMDB))
        // 大小写与空白按 TMDB 处理
        assertEquals(ScrapeSourcePolicy.TMDB, ScrapeSourcePolicy.resolve(" TMDB "))
        assertEquals(ScrapeSourcePolicy.TMDB, ScrapeSourcePolicy.resolve("tmdb"))
    }

    @Test
    fun `脏数据回退默认源`() {
        listOf("", "  ", "douban ", "netflix", "预置").forEach { raw ->
            assertEquals("<$raw> 应回退豆瓣", ScrapeSourcePolicy.DOUBAN, ScrapeSourcePolicy.resolve(raw))
        }
    }
}
