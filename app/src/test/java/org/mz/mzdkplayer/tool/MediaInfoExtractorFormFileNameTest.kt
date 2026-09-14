package org.mz.mzdkplayer.tool

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * [MediaInfoExtractorFormFileName] 的 JVM 单元测试。
 *
 * 覆盖：电影 / 剧集 / 中文数字 / 分辨率 / 噪音词 / 边界输入，
 * 以及调用方真正依赖的契约（`season.toInt()`、`episode.toInt()` 不会抛异常）。
 */
class MediaInfoExtractorFormFileNameTest {

    /** 一次性断言全部字段，失败信息里带上原始文件名，便于定位 */
    private fun assertParsed(
        fileName: String,
        title: String,
        year: String = "",
        season: String = "",
        episode: String = "",
        mediaType: String = "movie",
        resolution: String = ""
    ) {
        val actual = MediaInfoExtractorFormFileName.extract(fileName)
        assertEquals("<$fileName> title", title, actual.title)
        assertEquals("<$fileName> year", year, actual.year)
        assertEquals("<$fileName> season", season, actual.season)
        assertEquals("<$fileName> episode", episode, actual.episode)
        assertEquals("<$fileName> mediaType", mediaType, actual.mediaType)
        assertEquals("<$fileName> resolution", resolution, actual.resolution)
    }

    // ────────────────────────────── 电影 ──────────────────────────────

    @Test
    fun `电影 - 点分隔的年份与画质标记`() {
        assertParsed(
            "流浪地球.2019.1080p.BluRay.x264.mkv",
            title = "流浪地球",
            year = "2019",
            resolution = "1080P"
        )
    }

    @Test
    fun `电影 - 英文片名与一堆发布组标记`() {
        assertParsed(
            "The.Matrix.1999.2160p.UHD.BluRay.REMUX.HDR.HEVC.Atmos.mkv",
            title = "The Matrix",
            year = "1999",
            resolution = "2160P"
        )
    }

    @Test
    fun `电影 - 空格与方括号包裹的年份画质`() {
        assertParsed(
            "Interstellar (2014) [1080p] [BluRay].mkv",
            title = "Interstellar",
            year = "2014",
            resolution = "1080P"
        )
    }

    @Test
    fun `电影 - 全角括号年份`() {
        assertParsed(
            "肖申克的救赎（1994）.mp4",
            title = "肖申克的救赎",
            year = "1994"
        )
    }

    @Test
    fun `电影 - 中英混排片名`() {
        assertParsed(
            "泰坦尼克号.Titanic.1997.720p.BD.mkv",
            title = "泰坦尼克号 Titanic",
            year = "1997",
            resolution = "720P"
        )
    }

    @Test
    fun `电影 - 没有年份时整串都是片名`() {
        assertParsed("春光乍泄.mkv", title = "春光乍泄")
    }

    @Test
    fun `电影 - 【发布组】前缀会被整段摘掉`() {
        assertParsed(
            "【高清影视】流浪地球.2019.1080p.mkv",
            title = "流浪地球",
            year = "2019",
            resolution = "1080P"
        )
    }

    @Test
    fun `电影 - 片名自带数字要保留`() {
        assertParsed("沙丘2.2024.2160p.mkv", title = "沙丘2", year = "2024", resolution = "2160P")
        assertParsed(
            "Blade.Runner.2049.2017.1080p.mkv",
            title = "Blade Runner 2049",
            year = "2017",
            resolution = "1080P"
        )
    }

    @Test
    fun `电影 - 只有四位数字的片名不能被当成集号`() {
        assertParsed("1917.mkv", title = "1917", year = "1917")
        assertParsed("2012.mkv", title = "2012", year = "2012")
    }

    @Test
    fun `电影 - 年份紧贴片名也能识别`() {
        assertParsed("Movie2019.mkv", title = "Movie", year = "2019")
    }

    @Test
    fun `电影 - WEB-DL 之类的连字符标记不会污染片名`() {
        assertParsed(
            "Oppenheimer.2023.1080p.WEB-DL.x264.mkv",
            title = "Oppenheimer",
            year = "2023",
            resolution = "1080P"
        )
    }

    @Test
    fun `电影 - 片名里的连字符要保留`() {
        assertParsed(
            "Spider-Man.No.Way.Home.2021.2160p.mkv",
            title = "Spider-Man No Way Home",
            year = "2021",
            resolution = "2160P"
        )
    }

    // ────────────────────────────── 分辨率 ──────────────────────────────

    @Test
    fun `分辨率 - 各种写法`() {
        assertParsed("某剧.S01E01.1080p.mkv", title = "某剧", season = "01", episode = "01", mediaType = "tv", resolution = "1080P")
        assertParsed("某剧.S01E01.720p.mkv", title = "某剧", season = "01", episode = "01", mediaType = "tv", resolution = "720P")
        assertParsed("某剧.S01E01.2160p.mkv", title = "某剧", season = "01", episode = "01", mediaType = "tv", resolution = "2160P")
        assertParsed("某剧.S01E01.4K.mkv", title = "某剧", season = "01", episode = "01", mediaType = "tv", resolution = "4K")
    }

    @Test
    fun `分辨率 - 没有分辨率信息时为空串`() {
        assertParsed("流浪地球.2019.mkv", title = "流浪地球", year = "2019", resolution = "")
    }

    // ────────────────────────────── 剧集：标准写法 ──────────────────────────────

    @Test
    fun `剧集 - SxxExx 大写`() {
        assertParsed(
            "庆余年.S02E05.2019.1080p.WEB-DL.mkv",
            title = "庆余年",
            year = "2019",
            season = "02",
            episode = "05",
            mediaType = "tv",
            resolution = "1080P"
        )
    }

    @Test
    fun `剧集 - SxxExx 小写且扩展名大写`() {
        assertParsed("movie.s01e02.MKV", title = "movie", season = "01", episode = "02", mediaType = "tv")
    }

    @Test
    fun `剧集 - 无年份的 SxxExx`() {
        assertParsed("怪奇物语.S04E09.mkv", title = "怪奇物语", season = "04", episode = "09", mediaType = "tv")
    }

    @Test
    fun `剧集 - S01_E02 带分隔符`() {
        assertParsed("某剧.S01.E02.mkv", title = "某剧", season = "01", episode = "02", mediaType = "tv")
        assertParsed("某剧.S01 E02.mkv", title = "某剧", season = "01", episode = "02", mediaType = "tv")
    }

    @Test
    fun `剧集 - 1x03 写法`() {
        assertParsed("某剧.1x03.mkv", title = "某剧", season = "01", episode = "03", mediaType = "tv")
    }

    @Test
    fun `剧集 - 分辨率里的 1920x1080 不能被当成 1x01`() {
        assertParsed("流浪地球.2019.1920x1080.mkv", title = "流浪地球", year = "2019")
        assertParsed("某剧.S01E01.1280x720.mkv", title = "某剧", season = "01", episode = "01", mediaType = "tv")
        assertParsed("某剧.S01E01.3840x2160.mkv", title = "某剧", season = "01", episode = "01", mediaType = "tv")
    }

    @Test
    fun `剧集 - Season 与 Episode 全拼`() {
        assertParsed(
            "某剧.Season.03.Episode.11.mkv",
            title = "某剧",
            season = "03",
            episode = "11",
            mediaType = "tv"
        )
    }

    @Test
    fun `剧集 - 只有季号`() {
        assertParsed("某剧.第二季.mkv", title = "某剧", season = "02", episode = "01", mediaType = "tv")
        assertParsed("某剧.Season.2.mkv", title = "某剧", season = "02", episode = "01", mediaType = "tv")
        assertParsed("某剧.S02.mkv", title = "某剧", season = "02", episode = "01", mediaType = "tv")
    }

    @Test
    fun `剧集 - EP 与 E 写法`() {
        assertParsed("某剧.EP05.mkv", title = "某剧", season = "01", episode = "05", mediaType = "tv")
        assertParsed("某剧.E05.mkv", title = "某剧", season = "01", episode = "05", mediaType = "tv")
    }

    @Test
    fun `剧集 - Se7en 这类片名不能被当成 E7`() {
        assertParsed("Se7en.1995.1080p.mkv", title = "Se7en", year = "1995", resolution = "1080P")
    }

    // ────────────────────────────── 剧集：中文写法 ──────────────────────────────

    @Test
    fun `剧集 - 第X季第Y集`() {
        assertParsed("某剧.第1季第12集.mkv", title = "某剧", season = "01", episode = "12", mediaType = "tv")
        assertParsed("某剧.第2季.第3集.mkv", title = "某剧", season = "02", episode = "03", mediaType = "tv")
    }

    @Test
    fun `剧集 - 中文数字`() {
        assertParsed("某剧.第一季第五集.mkv", title = "某剧", season = "01", episode = "05", mediaType = "tv")
        assertParsed("某剧.第十二集.mkv", title = "某剧", season = "01", episode = "12", mediaType = "tv")
        assertParsed("某剧.第二十集.mkv", title = "某剧", season = "01", episode = "20", mediaType = "tv")
    }

    @Test
    fun `剧集 - 第X集 与 第X话 第X期`() {
        assertParsed("某剧.第08集.mkv", title = "某剧", season = "01", episode = "08", mediaType = "tv")
        assertParsed("某番.第03话.mkv", title = "某番", season = "01", episode = "03", mediaType = "tv")
        assertParsed("某综艺.第07期.mkv", title = "某综艺", season = "01", episode = "07", mediaType = "tv")
    }

    @Test
    fun `剧集 - 行首 01 dot 标题`() {
        assertParsed("01.师徒.mp4", title = "师徒", season = "01", episode = "01", mediaType = "tv")
    }

    @Test
    fun `剧集 - 行首 01 空格 标题`() {
        assertParsed("02 高潜.mp4", title = "高潜", season = "01", episode = "02", mediaType = "tv")
        assertParsed("12 大汉天子.mp4", title = "大汉天子", season = "01", episode = "12", mediaType = "tv")
    }

    @Test
    fun `剧集 - 行首集号后紧跟画质标记时不当作标题`() {
        assertParsed("01 4K.国&粤.mp4", title = "01", season = "01", episode = "01", mediaType = "tv", resolution = "4K")
    }

    @Test
    fun `剧集 - 夹在分隔符中间的带零集号`() {
        assertParsed(
            "某剧.01.国语中字.1080p.mkv",
            title = "某剧",
            season = "01",
            episode = "01",
            mediaType = "tv",
            resolution = "1080P"
        )
        assertParsed("某剧 03 1080p.mkv", title = "某剧", season = "01", episode = "03", mediaType = "tv", resolution = "1080P")
    }

    @Test
    fun `剧集 - 合集范围取起始集号`() {
        assertParsed("某剧.01-04.mkv", title = "某剧", season = "01", episode = "01", mediaType = "tv")
        assertParsed("某某剧.01-04.1080p.mkv", title = "某某剧", season = "01", episode = "01", mediaType = "tv", resolution = "1080P")
        assertParsed("某剧.12~15.mkv", title = "某剧", season = "01", episode = "12", mediaType = "tv")
    }

    @Test
    fun `剧集 - 整串只有一个编号`() {
        assertParsed("01.mp4", title = "01", season = "01", episode = "01", mediaType = "tv")
        assertParsed("007.mkv", title = "007", season = "01", episode = "07", mediaType = "tv")
    }

    @Test
    fun `剧集 - 三位数集号不补零到两位以外`() {
        assertParsed("某剧.S05E100.mkv", title = "某剧", season = "05", episode = "100", mediaType = "tv")
    }

    // ────────────────────────────── 噪音词 ──────────────────────────────

    @Test
    fun `噪音词 - 不会把 Accident 砍成 Aident`() {
        assertParsed("Accident.2009.720p.mkv", title = "Accident", year = "2009", resolution = "720P")
    }

    @Test
    fun `噪音词 - Final 是片名的一部分，不该被剔除`() {
        assertParsed(
            "Final.Destination.2000.1080p.mkv",
            title = "Final Destination",
            year = "2000",
            resolution = "1080P"
        )
    }

    @Test
    fun `噪音词 - 中文音轨字幕标记不进入片名`() {
        assertParsed(
            "流浪地球.2019.国语中字.1080p.mkv",
            title = "流浪地球",
            year = "2019",
            resolution = "1080P"
        )
    }

    // ────────────────────────────── 路径与 URL ──────────────────────────────

    @Test
    fun `输入 - 带目录的路径只取文件名`() {
        assertParsed("D:/影片/科幻/流浪地球.2019.1080p.mkv", title = "流浪地球", year = "2019", resolution = "1080P")
        assertParsed("C:\\Movies\\流浪地球.2019.mkv", title = "流浪地球", year = "2019")
    }

    @Test
    fun `输入 - 带查询串的 URL`() {
        assertParsed("http://a.com/v/流浪地球.2019.mkv?token=abc", title = "流浪地球", year = "2019")
    }

    @Test
    fun `输入 - 没有扩展名`() {
        assertParsed("流浪地球", title = "流浪地球")
    }

    @Test
    fun `输入 - 特殊符号不会导致异常`() {
        val samples = listOf(
            "剧名(2020)[(1080p)].mkv",
            "剧名 {S01E01}.mkv",
            "某剧.S01E01.[1080p].mkv",
            "流浪地球.2019.1080p.x265.10bit.AAC5.1.mkv"
        )
        for (sample in samples) {
            MediaInfoExtractorFormFileName.extract(sample)
        }
        assertParsed("剧名(2020)[(1080p)].mkv", title = "剧名", year = "2020", resolution = "1080P")
    }

    // ────────────────────────────── 空输入与契约 ──────────────────────────────

    @Test
    fun `空输入 - 返回空信息而不是抛异常`() {
        assertParsed("", title = "")
        assertParsed("   ", title = "")
    }

    @Test
    fun `契约 - 标题永远不为空白`() {
        val samples = listOf(
            "流浪地球.2019.1080p.mkv", "S01E01.mkv", "01.mp4", "第03集.mkv",
            "01 4K.国&粤.mp4", "某剧.01-04.mkv", "007.mkv", "【发布组】.mkv"
        )
        for (sample in samples) {
            val info = MediaInfoExtractorFormFileName.extract(sample)
            assertTrue("<$sample> 的标题不应为空白，实际是 <${info.title}>", info.title.isNotBlank())
        }
    }

    @Test
    fun `契约 - 判定为剧集时季集一定能安全转成 Int`() {
        val samples = listOf(
            "庆余年.S02E05.mkv",
            "某剧.1x03.mkv",
            "某剧.第1季第12集.mkv",
            "某剧.EP05.mkv",
            "某剧.第08集.mkv",
            "01.师徒.mp4",
            "01 4K.国&粤.mp4",
            "某剧.01-04.mkv",
            "某剧.第二季.mkv",
            "某剧.S02.mkv",
            "01.mp4",
            "007.mkv"
        )
        for (sample in samples) {
            val info = MediaInfoExtractorFormFileName.extract(sample)
            assertEquals("<$sample> 应判定为剧集", "tv", info.mediaType)
            // 调用点会直接 toInt()，这里复现一遍，抛异常即失败
            assertTrue("<$sample> season 应可转 Int", info.season.toInt() > 0)
            assertTrue("<$sample> episode 应可转 Int", info.episode.toInt() > 0)
        }
    }

    @Test
    fun `契约 - 判定为电影时季集为空串`() {
        val samples = listOf(
            "流浪地球.2019.1080p.mkv",
            "春光乍泄.mkv",
            "1917.mkv",
            "Se7en.1995.mkv",
            "Spider-Man.No.Way.Home.2021.mkv"
        )
        for (sample in samples) {
            val info = MediaInfoExtractorFormFileName.extract(sample)
            assertEquals("<$sample> 应判定为电影", "movie", info.mediaType)
            assertEquals("<$sample> season 应为空", "", info.season)
            assertEquals("<$sample> episode 应为空", "", info.episode)
        }
    }

    @Test
    fun `契约 - 剧集名可以批量解析且互不干扰`() {
        val fileNames = listOf(
            "庆余年.S01E01.mkv",
            "庆余年.S01E02.mkv",
            "庆余年.S01E03.mkv"
        )
        val results = fileNames.map { MediaInfoExtractorFormFileName.extract(it) }
        assertEquals(listOf("01", "02", "03"), results.map { it.episode })
        assertEquals(listOf("庆余年", "庆余年", "庆余年"), results.map { it.title })
        assertEquals(listOf("tv", "tv", "tv"), results.map { it.mediaType })
    }
}
