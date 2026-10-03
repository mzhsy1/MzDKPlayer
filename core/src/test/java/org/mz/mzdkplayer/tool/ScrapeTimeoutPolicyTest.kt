package org.mz.mzdkplayer.tool

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mz.mzdkplayer.tool.logic.ScrapeTimeoutPolicy

/**
 * [ScrapeTimeoutPolicy] 的 JVM 单测。
 *
 * 关键契约：默认 5 秒且一定落在档位里；脏数据回退默认值；`next` 在档位里循环（首尾要接上，
 * 否则电视端那个「按一次换一个」的设置项会在最后一档卡住）。
 */
class ScrapeTimeoutPolicyTest {

    @Test
    fun `默认是 5 秒且在档位里`() {
        assertEquals(5, ScrapeTimeoutPolicy.DEFAULT_SECONDS)
        assertTrue(ScrapeTimeoutPolicy.OPTIONS.contains(ScrapeTimeoutPolicy.DEFAULT_SECONDS))
    }

    @Test
    fun `非法值一律回退默认`() {
        listOf(0, -1, 4, 7, 1000, Int.MAX_VALUE, Int.MIN_VALUE).forEach { raw ->
            assertEquals("<$raw> 应回退默认", ScrapeTimeoutPolicy.DEFAULT_SECONDS, ScrapeTimeoutPolicy.normalize(raw))
        }
    }

    @Test
    fun `合法档位原样返回`() {
        ScrapeTimeoutPolicy.OPTIONS.forEach { seconds ->
            assertEquals(seconds, ScrapeTimeoutPolicy.normalize(seconds))
        }
    }

    @Test
    fun `下一档按档位顺序前进`() {
        assertEquals(8, ScrapeTimeoutPolicy.next(5))
        assertEquals(10, ScrapeTimeoutPolicy.next(8))
        // 脏值先收敛再前进：非法值 → 5 → 8
        assertEquals(8, ScrapeTimeoutPolicy.next(7))
    }

    @Test
    fun `最后一档回到第一档`() {
        val options = ScrapeTimeoutPolicy.OPTIONS
        assertEquals(options.first(), ScrapeTimeoutPolicy.next(options.last()))
    }
}
