package org.mz.mzdkplayer.tool

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * 手机端刮削的纯逻辑：挑出要刮的条目、跳过已刮的、拼列表标题与副标题、算进度。
 *
 * 边界口径见 [PhoneScrapeLogic] 的注释，这里逐个锁死。
 */
class PhoneScrapeLogicTest {

    private fun item(name: String, uri: String?): PhoneScrapeLogic.Item =
        PhoneScrapeLogic.Item(name = name, playbackUri = uri)

    @Test
    fun `只挑视频文件并且要有播放地址`() {
        val targets = PhoneScrapeLogic.scrapeTargets(
            listOf(
                item("电影.mkv", "smb://a/电影.mkv"),
                item("海报.jpg", "smb://a/海报.jpg"),
                item("字幕.srt", "smb://a/字幕.srt"),
                item("子目录", null),
                // 目录本身也带播放地址的话不该被刮（这条靠扩展名过滤兜住）
                item("动作片", "smb://a/动作片"),
                item("没有地址的电影.mp4", null),
                item("空地址的电影.mp4", "   "),
            )
        )

        assertEquals(listOf("电影.mkv" to "smb://a/电影.mkv"), targets)
    }

    @Test
    fun `同一个播放地址只刮一次`() {
        val targets = PhoneScrapeLogic.scrapeTargets(
            listOf(
                item("第一部.mp4", "ftp://host/a.mp4"),
                item("重复的.mp4", "ftp://host/a.mp4"),
            )
        )

        assertEquals(listOf("第一部.mp4" to "ftp://host/a.mp4"), targets)
    }

    @Test
    fun `扩展名大小写不影响识别`() {
        val targets = PhoneScrapeLogic.scrapeTargets(
            listOf(item("MOVIE.MKV", "file:///s/MOVIE.MKV"))
        )

        assertEquals(1, targets.size)
    }

    @Test
    fun `库里已有记录的地址不再重复刮`() {
        val items = listOf(
            item("已刮.mp4", "smb://a/已刮.mp4"),
            item("没刮.mp4", "smb://a/没刮.mp4"),
        )

        val pending = PhoneScrapeLogic.pendingTargets(items, setOf("smb://a/已刮.mp4"))

        assertEquals(listOf("没刮.mp4" to "smb://a/没刮.mp4"), pending)
    }

    @Test
    fun `没有缓存时全部都要刮`() {
        val items = listOf(item("a.mp4", "smb://a/a.mp4"), item("b.mp4", "smb://a/b.mp4"))

        assertEquals(2, PhoneScrapeLogic.pendingTargets(items, emptySet()).size)
    }

    @Test
    fun `没有刮削记录时标题回退到文件名`() {
        assertEquals("无刮削.mp4", PhoneScrapeLogic.displayTitle(null, "无刮削.mp4"))
        assertEquals(
            "标题为空的.mp4",
            PhoneScrapeLogic.displayTitle(meta(title = "  "), "标题为空的.mp4"),
        )
    }

    @Test
    fun `电影标题只用刮削标题`() {
        assertEquals(
            "流浪地球2",
            PhoneScrapeLogic.displayTitle(meta(title = "流浪地球2", year = "2023"), "garbage.mp4"),
        )
    }

    @Test
    fun `剧集标题补零的季集号`() {
        assertEquals(
            "绝命毒师 S01E01",
            PhoneScrapeLogic.displayTitle(
                meta(title = "绝命毒师", mediaType = "tv", season = 1, episode = 1),
                "Breaking.Bad.S01E01.mp4",
            ),
        )
    }

    @Test
    fun `剧集没有季集号时只显示标题`() {
        assertEquals(
            "绝命毒师",
            PhoneScrapeLogic.displayTitle(meta(title = "绝命毒师", mediaType = "tv"), "x.mp4"),
        )
    }

    @Test
    fun `电影即使带了季集号也不拼季集`() {
        assertEquals(
            "电影",
            PhoneScrapeLogic.displayTitle(
                meta(title = "电影", mediaType = "movie", season = 1, episode = 2),
                "x.mp4",
            ),
        )
    }

    @Test
    fun `副标题拼年份与一位小数评分`() {
        assertEquals(
            "2010 · 8.4",
            PhoneScrapeLogic.displaySubtitle(meta(title = "盗梦空间", year = "2010", vote = 8.44)),
        )
    }

    @Test
    fun `副标题缺项时只拼剩下的`() {
        assertEquals("8.4", PhoneScrapeLogic.displaySubtitle(meta(title = "x", vote = 8.44)))
        assertEquals("2010", PhoneScrapeLogic.displaySubtitle(meta(title = "x", year = "2010")))
    }

    @Test
    fun `没有刮削或两项都缺时副标题为空`() {
        assertNull(PhoneScrapeLogic.displaySubtitle(null))
        assertNull(PhoneScrapeLogic.displaySubtitle(meta(title = "x")))
        assertNull(PhoneScrapeLogic.displaySubtitle(meta(title = "x", year = "  ")))
    }

    @Test
    fun `进度百分比按四舍五入取整并在没开始时返回零`() {
        assertEquals(0, PhoneScrapeLogic.progressPercent(0, 0))
        assertEquals(0, PhoneScrapeLogic.progressPercent(0, 10))
        assertEquals(50, PhoneScrapeLogic.progressPercent(5, 10))
        assertEquals(100, PhoneScrapeLogic.progressPercent(10, 10))
    }

    @Test
    fun `进度百分比会夹到零到一百`() {
        assertEquals(100, PhoneScrapeLogic.progressPercent(30, 10))
        assertEquals(0, PhoneScrapeLogic.progressPercent(-3, 10))
    }

    private fun meta(
        title: String,
        year: String? = null,
        vote: Double = 0.0,
        mediaType: String = "movie",
        season: Int = 0,
        episode: Int = 0,
    ) = PhoneScrapeLogic.Meta(
        title = title,
        year = year,
        voteAverage = vote,
        mediaType = mediaType,
        seasonNumber = season,
        episodeNumber = episode,
    )
}
