package org.mz.mzdkplayer.tool

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mz.mzdkplayer.data.local.MediaCacheEntity
import java.util.Calendar

/**
 * [PlayerMediaText] 的 JVM 单元测试。
 *
 * 覆盖播放界面标题组装的完整规则：
 * 刮削标题优先 → 电视剧追加季集 → 追加年份 → 无刮削信息时回落文件名；
 * 以及文件时间格式化的边界（非正数、补零、形状）。
 */
class PlayerMediaTextTest {

    private val fileName = "怪奇物语.S01E02.1080p.mkv"

    /** 构造一条刮削缓存记录，只暴露本测试关心的字段 */
    private fun meta(
        title: String = "标题",
        mediaType: String = "movie",
        releaseDate: String? = null,
        episodeAirDate: String? = null,
        seasonNumber: Int = 0,
        episodeNumber: Int = 0
    ) = MediaCacheEntity(
        videoUri = "smb://192.168.1.4/downloads/剧集/怪奇物语.S01E02.mkv",
        dataSourceType = "SMB",
        fileName = fileName,
        connectionName = "MyNas",
        tmdbId = 66732,
        mediaType = mediaType,
        title = title,
        overview = "",
        posterPath = null,
        backdropPath = null,
        releaseDate = releaseDate,
        voteAverage = 0.0,
        seasonNumber = seasonNumber,
        episodeNumber = episodeNumber,
        episodeAirDate = episodeAirDate,
        groupKey = "tv_66732_s01"
    )

    // ────────────────────────────── 电影 ──────────────────────────────

    @Test
    fun `电影 - 标题后面带上映年份`() {
        val m = meta(title = "流浪地球", releaseDate = "2019-02-05")
        assertEquals("流浪地球 (2019)", PlayerMediaText.buildTitle(m, fileName))
    }

    @Test
    fun `电影 - 没有年份时只显示标题`() {
        assertEquals("流浪地球", PlayerMediaText.buildTitle(meta(title = "流浪地球"), fileName))
    }

    @Test
    fun `电影 - 年份只取前四位`() {
        val m = meta(title = "流浪地球", releaseDate = "2019/02/05")
        assertEquals("流浪地球 (2019)", PlayerMediaText.buildTitle(m, fileName))
    }

    @Test
    fun `电影 - 即使有单集播出日期也不使用`() {
        val m = meta(title = "流浪地球", mediaType = "movie", episodeAirDate = "2019-02-05")
        assertEquals("流浪地球", PlayerMediaText.buildTitle(m, fileName))
    }

    // ────────────────────────────── 剧集 ──────────────────────────────

    @Test
    fun `剧集 - 标题加季集加年份`() {
        val m = meta(
            title = "怪奇物语",
            mediaType = "tv",
            releaseDate = "2016-07-15",
            seasonNumber = 1,
            episodeNumber = 2
        )
        assertEquals("怪奇物语 S01E02 (2016)", PlayerMediaText.buildTitle(m, fileName))
    }

    @Test
    fun `剧集 - 没有年份时只加季集`() {
        val m = meta(title = "怪奇物语", mediaType = "tv", seasonNumber = 1, episodeNumber = 2)
        assertEquals("怪奇物语 S01E02", PlayerMediaText.buildTitle(m, fileName))
    }

    @Test
    fun `剧集 - 季集补零到两位`() {
        val m = meta(title = "某剧", mediaType = "tv", seasonNumber = 2, episodeNumber = 5)
        assertEquals("某剧 S02E05", PlayerMediaText.buildTitle(m, fileName))
    }

    @Test
    fun `剧集 - 集号超过两位时原样输出不截断`() {
        val m = meta(title = "某剧", mediaType = "tv", seasonNumber = 5, episodeNumber = 100)
        assertEquals("某剧 S05E100", PlayerMediaText.buildTitle(m, fileName))
    }

    @Test
    fun `剧集 - 季集都是 0 时不出现 S00E00`() {
        val m = meta(title = "怪奇物语", mediaType = "tv", releaseDate = "2016-07-15")
        assertEquals("怪奇物语 (2016)", PlayerMediaText.buildTitle(m, fileName))
    }

    /**
     * 记录当前行为：只拿到季号时仍会拼出 E00。
     * 若后续改成「季集必须同时有效才拼接」，需要同步修改此用例。
     */
    @Test
    fun `剧集 - 只有季号时当前行为会补出 E00`() {
        val m = meta(title = "某剧", mediaType = "tv", seasonNumber = 2)
        assertEquals("某剧 S02E00", PlayerMediaText.buildTitle(m, fileName))
    }

    @Test
    fun `剧集 - 剧集首播日期缺失时退回单集播出日期`() {
        val m = meta(
            title = "怪奇物语",
            mediaType = "tv",
            episodeAirDate = "2016-07-15",
            seasonNumber = 1,
            episodeNumber = 2
        )
        assertEquals("怪奇物语 S01E02 (2016)", PlayerMediaText.buildTitle(m, fileName))
    }

    @Test
    fun `剧集 - 剧集首播日期为空串时退回单集播出日期`() {
        val m = meta(
            title = "怪奇物语",
            mediaType = "tv",
            releaseDate = "",
            episodeAirDate = "2016-07-15",
            seasonNumber = 1,
            episodeNumber = 2
        )
        assertEquals("怪奇物语 S01E02 (2016)", PlayerMediaText.buildTitle(m, fileName))
    }

    @Test
    fun `剧集 - 剧集首播日期优先于单集播出日期`() {
        val m = meta(
            title = "怪奇物语",
            mediaType = "tv",
            releaseDate = "2016-07-15",
            episodeAirDate = "2019-01-01",
            seasonNumber = 1,
            episodeNumber = 2
        )
        assertEquals("怪奇物语 S01E02 (2016)", PlayerMediaText.buildTitle(m, fileName))
    }

    @Test
    fun `剧集 - 单集播出日期不可用时只加季集`() {
        val m = meta(
            title = "怪奇物语",
            mediaType = "tv",
            episodeAirDate = "待定",
            seasonNumber = 1,
            episodeNumber = 2
        )
        assertEquals("怪奇物语 S01E02", PlayerMediaText.buildTitle(m, fileName))
    }

    @Test
    fun `剧集 - 媒体类型大小写不敏感`() {
        val upper = meta(title = "怪奇物语", mediaType = "TV", seasonNumber = 1, episodeNumber = 2)
        val mixed = meta(title = "怪奇物语", mediaType = "Tv", seasonNumber = 1, episodeNumber = 2)
        assertEquals("怪奇物语 S01E02", PlayerMediaText.buildTitle(upper, fileName))
        assertEquals("怪奇物语 S01E02", PlayerMediaText.buildTitle(mixed, fileName))
    }

    // ────────────────────────────── 年份识别的边界 ──────────────────────────────

    @Test
    fun `年份 - 不足四位时忽略`() {
        assertEquals("流浪地球", PlayerMediaText.buildTitle(meta(title = "流浪地球", releaseDate = "20"), fileName))
        assertEquals("流浪地球", PlayerMediaText.buildTitle(meta(title = "流浪地球", releaseDate = ""), fileName))
        assertEquals("流浪地球", PlayerMediaText.buildTitle(meta(title = "流浪地球", releaseDate = "   "), fileName))
    }

    @Test
    fun `年份 - 前四位不是数字时忽略`() {
        val m = meta(title = "流浪地球", releaseDate = "abcd-01-01")
        assertEquals("流浪地球", PlayerMediaText.buildTitle(m, fileName))
    }

    @Test
    fun `年份 - 字段带空白也能识别`() {
        val m = meta(title = "流浪地球", releaseDate = "  2019-02-05  ")
        assertEquals("流浪地球 (2019)", PlayerMediaText.buildTitle(m, fileName))
    }

    // ────────────────────────────── 回落文件名 ──────────────────────────────

    @Test
    fun `无刮削信息时回落为文件名`() {
        assertEquals(fileName, PlayerMediaText.buildTitle(null, fileName))
    }

    @Test
    fun `刮削标题为空白时回落为文件名`() {
        assertEquals(fileName, PlayerMediaText.buildTitle(meta(title = ""), fileName))
        assertEquals(fileName, PlayerMediaText.buildTitle(meta(title = "   "), fileName))
    }

    @Test
    fun `刮削标题为空白的剧集也回落为文件名`() {
        val m = meta(title = "", mediaType = "tv", releaseDate = "2016-07-15", seasonNumber = 1, episodeNumber = 2)
        assertEquals(fileName, PlayerMediaText.buildTitle(m, fileName))
    }

    @Test
    fun `刮削标题前后空白会被裁掉`() {
        val m = meta(title = "  流浪地球  ", releaseDate = "2019-02-05")
        assertEquals("流浪地球 (2019)", PlayerMediaText.buildTitle(m, fileName))
    }

    // ────────────────────────────── 文件时间 → 日期 ──────────────────────────────

    /** 取本地时区当天中午的时间戳，避开跨时区导致的日期漂移 */
    private fun localNoonOf(year: Int, month: Int, day: Int): Long =
        Calendar.getInstance().apply {
            clear()
            set(year, month, day, 12, 0, 0)
        }.timeInMillis

    @Test
    fun `日期 - 格式化为 yyyy 斜杠 MM 斜杠 dd`() {
        val millis = localNoonOf(2026, Calendar.SEPTEMBER, 14)
        assertEquals("2026/09/14", PlayerMediaText.buildFileDateText(millis))
    }

    @Test
    fun `日期 - 月份与日期补零`() {
        assertEquals(
            "2026/01/04",
            PlayerMediaText.buildFileDateText(localNoonOf(2026, Calendar.JANUARY, 4))
        )
        assertEquals(
            "2026/12/25",
            PlayerMediaText.buildFileDateText(localNoonOf(2026, Calendar.DECEMBER, 25))
        )
    }

    @Test
    fun `日期 - 非正时间戳返回空串`() {
        assertEquals("", PlayerMediaText.buildFileDateText(0L))
        assertEquals("", PlayerMediaText.buildFileDateText(-1L))
        assertEquals("", PlayerMediaText.buildFileDateText(Long.MIN_VALUE))
    }

    @Test
    fun `日期 - 结果始终是四位年两位月两位日的形状`() {
        val shape = Regex("""\d{4}/\d{2}/\d{2}""")
        val samples = listOf(
            1L,
            1_000_000_000_000L,
            1_700_000_000_000L,
            2_000_000_000_000L,
            localNoonOf(2030, Calendar.MARCH, 9)
        )
        for (millis in samples) {
            val text = PlayerMediaText.buildFileDateText(millis)
            assertTrue("<$millis> 的日期 <$text> 不符合 yyyy/MM/dd", shape.matches(text))
        }
    }

    @Test
    fun `日期 - 同一天内不同时刻结果一致`() {
        val morning = Calendar.getInstance().apply { clear(); set(2026, Calendar.SEPTEMBER, 14, 1, 0, 0) }.timeInMillis
        val evening = Calendar.getInstance().apply { clear(); set(2026, Calendar.SEPTEMBER, 14, 23, 30, 0) }.timeInMillis
        assertEquals(PlayerMediaText.buildFileDateText(morning), PlayerMediaText.buildFileDateText(evening))
    }
}
