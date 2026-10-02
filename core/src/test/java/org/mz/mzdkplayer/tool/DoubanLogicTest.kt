package org.mz.mzdkplayer.tool

import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mz.mzdkplayer.data.douban.DoubanPhotosDto
import org.mz.mzdkplayer.data.douban.DoubanSubjectDto
import org.mz.mzdkplayer.data.douban.DoubanSuggestDto
import org.mz.mzdkplayer.data.douban.firstWideImageUrl
import org.mz.mzdkplayer.data.douban.toCandidate
import org.mz.mzdkplayer.data.douban.toSubject
import org.mz.mzdkplayer.tool.logic.DoubanLogic

/**
 * 豆瓣刮削纯逻辑 + DTO 映射的 JVM 单测。
 *
 * 样例数据不是编的，是从实测响应里截出来的（`https://movie.douban.com/j/subject_suggest` 与
 * `https://m.douban.com/rexxar/api/v2/...`），所以这些用例同时钉住了“接口字段名没变”这件事。
 */
class DoubanLogicTest {

    private val gson = Gson()

    // ────────────────────────────── 地址与请求头 ──────────────────────────────

    @Test
    fun `搜索地址 - 中文关键词必须 URL 编码`() {
        val url = DoubanLogic.searchUrl("流浪地球")
        assertTrue(url, url.startsWith("https://movie.douban.com/j/subject_suggest?q="))
        assertTrue(url, url.contains("%E6%B5%81%E6%B5%AA%E5%9C%B0%E7%90%83"))
    }

    @Test
    fun `详情地址 - 电影与剧集走不同路径`() {
        assertEquals(
            "https://m.douban.com/rexxar/api/v2/movie/26266893",
            DoubanLogic.detailUrl("26266893", isTv = false)
        )
        assertEquals(
            "https://m.douban.com/rexxar/api/v2/tv/26647087",
            DoubanLogic.detailUrl("26647087", isTv = true)
        )
    }

    @Test
    fun `剧照地址 - 背景图取宽幅剧照`() {
        assertEquals(
            "https://m.douban.com/rexxar/api/v2/movie/26266893/photos?type=W&start=0&count=1",
            DoubanLogic.photosUrl("26266893", isTv = false)
        )
    }

    @Test
    fun `请求头 - 统一用桌面浏览器 UA 并分别带上两个域名的 Referer`() {
        assertTrue(DoubanLogic.BROWSER_UA, DoubanLogic.BROWSER_UA.contains("Mozilla/5.0"))
        assertFalse(DoubanLogic.BROWSER_UA, DoubanLogic.BROWSER_UA.contains("frodo"))
        assertEquals("https://movie.douban.com/", DoubanLogic.SEARCH_REFERER)
        assertEquals("https://m.douban.com/", DoubanLogic.DETAIL_REFERER)
    }

    @Test
    fun `域名判断 - 只有豆瓣域名需要补图片 Referer`() {
        assertTrue(DoubanLogic.isDoubanHost("img3.doubanio.com"))
        assertTrue(DoubanLogic.isDoubanHost("img9.doubanio.com"))
        assertTrue(DoubanLogic.isDoubanHost("m.douban.com"))
        assertTrue(DoubanLogic.isDoubanHost("douban.com"))
        assertFalse(DoubanLogic.isDoubanHost("image.tmdb.org"))
        // 别把「长得像」的域名也带上 Referer
        assertFalse(DoubanLogic.isDoubanHost("douban.com.evil.test"))
        assertFalse(DoubanLogic.isDoubanHost("mydoubanio.com"))
    }

    // ────────────────────────────── 图片地址规整 ──────────────────────────────

    @Test
    fun `海报地址 - 小图与中图都升到大图`() {
        assertEquals(
            "https://img3.doubanio.com/view/photo/l_ratio_poster/public/p2545472803.jpg",
            DoubanLogic.largePosterUrl("https://img3.doubanio.com/view/photo/s_ratio_poster/public/p2545472803.jpg")
        )
        assertEquals(
            "https://img3.doubanio.com/view/photo/l_ratio_poster/public/p2545472803.jpg",
            DoubanLogic.largePosterUrl("https://img3.doubanio.com/view/photo/m_ratio_poster/public/p2545472803.jpg")
        )
    }

    @Test
    fun `海报地址 - 已经是大图或非 poster 尺寸段时原样返回`() {
        val alreadyLarge = "https://img3.doubanio.com/view/photo/l_ratio_poster/public/p2545472803.jpg"
        assertEquals(alreadyLarge, DoubanLogic.largePosterUrl(alreadyLarge))
        // `cover.image.large` 这种 `/l/public/` 本身就是大图，不能被改写成 poster 段
        val rawLarge = "https://img3.doubanio.com/view/photo/l/public/p2542069917.jpg"
        assertEquals(rawLarge, DoubanLogic.largePosterUrl(rawLarge))
    }

    @Test
    fun `海报地址 - 空值返回 null`() {
        assertNull(DoubanLogic.largePosterUrl(null))
        assertNull(DoubanLogic.largePosterUrl("   "))
    }

    @Test
    fun `背景图地址 - 只去空不改写`() {
        assertNull(DoubanLogic.backdropUrl(""))
        assertEquals(
            "https://img3.doubanio.com/view/photo/l/public/p2542069917.jpg",
            DoubanLogic.backdropUrl(" https://img3.doubanio.com/view/photo/l/public/p2542069917.jpg ")
        )
    }

    // ────────────────────────────── 文本归一化 ──────────────────────────────

    @Test
    fun `标题清洗 - 去掉不可见方向符与尾巴上的年份`() {
        assertEquals("流浪地球", DoubanLogic.cleanTitle("流浪地球\u200e (2019)"))
        assertEquals("流浪地球", DoubanLogic.cleanTitle("流浪地球(2019)"))
        assertEquals("流浪地球", DoubanLogic.cleanTitle("  流浪地球  "))
    }

    @Test
    fun `标题清洗 - 片名里的数字不能当成年份尾巴被删掉`() {
        assertEquals("你好1983", DoubanLogic.cleanTitle("你好1983"))
        assertEquals("1921", DoubanLogic.cleanTitle("1921"))
    }

    @Test
    fun `标题清洗 - 空值返回空串`() {
        assertEquals("", DoubanLogic.cleanTitle(null))
        assertEquals("", DoubanLogic.cleanTitle("   "))
    }

    @Test
    fun `年份解析`() {
        assertEquals(2019, DoubanLogic.toYear("2019"))
        assertEquals(2019, DoubanLogic.toYear("2019-02-05"))
        assertNull(DoubanLogic.toYear(""))
        assertNull(DoubanLogic.toYear("abcd"))
        assertNull(DoubanLogic.toYear("12"))
    }

    @Test
    fun `上映日期归一化 - 去掉地区括号，拿不到日期退回年份`() {
        assertEquals("2019-02-05", DoubanLogic.releaseDate(listOf("2019-02-05(中国大陆)"), 2019))
        assertEquals("2019", DoubanLogic.releaseDate(null, 2019))
        assertNull(DoubanLogic.releaseDate(emptyList(), null))
    }

    @Test
    fun `时长与演职员归一化`() {
        assertEquals("125分钟", DoubanLogic.firstDuration(listOf("125分钟", "137分钟(重映版)")))
        assertNull(DoubanLogic.firstDuration(null))

        assertEquals(
            listOf("郭帆", "吴京"),
            DoubanLogic.creditNames(listOf("郭帆", "吴京", "郭帆", "", "  ", null), limit = 5)
        )
        assertEquals(
            listOf("张鲁一", "于和伟"),
            DoubanLogic.creditNames(listOf("张鲁一", "于和伟", "陈瑾"), limit = 2)
        )
    }

    // ────────────────────────────── 候选打分 ──────────────────────────────

    private fun candidate(id: String, title: String, year: Int?, episode: Boolean) =
        DoubanLogic.Candidate(id = id, title = title, year = year, hasEpisode = episode, posterUrl = null)

    @Test
    fun `候选打分 - 返回空列表时给 null`() {
        assertNull(DoubanLogic.pickBest(emptyList(), "流浪地球", 2019, preferTv = false))
    }

    @Test
    fun `候选打分 - 同名同年的优先`() {
        val best = DoubanLogic.pickBest(
            listOf(
                candidate("1", "流浪地球2", 2023, false),
                candidate("2", "流浪地球", 2019, false),
                candidate("3", "流浪地球3(上)", 2027, false),
            ),
            expectedTitle = "流浪地球",
            expectedYear = 2019,
            preferTv = false,
        )
        assertEquals("2", best?.id)
    }

    @Test
    fun `候选打分 - 年份不一致会被压到后面`() {
        val best = DoubanLogic.pickBest(
            listOf(
                candidate("1", "三体", 2030, true),
                candidate("2", "三体", 2023, true),
            ),
            expectedTitle = "三体",
            expectedYear = 2023,
            preferTv = true,
        )
        assertEquals("2", best?.id)
    }

    /**
     * 关键回归：`subject_suggest` 对剧集也返回 `type=movie`（实测《三体》剧版就是），
     * 所以“期望剧集”只能作为加权项，绝不能拿 type 去过滤，否则剧集会一条都选不出来。
     */
    @Test
    fun `候选打分 - 期望剧集时不会因为 type 字段是 movie 而漏选`() {
        val candidates = listOf(
            candidate("26647087", "三体", 2023, episode = true),
            candidate("21335171", "三体", 2030, episode = false),
        )
        val best = DoubanLogic.pickBest(candidates, "三体", 2023, preferTv = true)
        assertEquals("26647087", best?.id)
        assertTrue(best!!.hasEpisode)
    }

    @Test
    fun `候选打分 - 同分时保留豆瓣自己的相关度顺序`() {
        val best = DoubanLogic.pickBest(
            listOf(
                candidate("a", "沙丘", 2021, false),
                candidate("b", "沙丘", 2021, false),
            ),
            expectedTitle = "沙丘",
            expectedYear = 2021,
            preferTv = false,
        )
        assertEquals("a", best?.id)
    }

    @Test
    fun `候选打分 - 标题带标点也能匹配上`() {
        val best = DoubanLogic.pickBest(
            listOf(
                candidate("1", "甄嬛传", 2011, true),
                candidate("2", "后宫·甄嬛传", 2011, true),
            ),
            expectedTitle = "甄嬛传",
            expectedYear = 2011,
            preferTv = true,
        )
        assertEquals("1", best?.id)
    }

    // ────────────────────────────── DTO 映射（实测响应） ──────────────────────────────

    /** 实测：`j/subject_suggest?q=三体`（对剧集也返回 type=movie，episode 非空） */
    private val suggestJson = """
        [
          {"episode":"30","img":"https://img2.doubanio.com/view/photo/s_ratio_poster/public/p2886492021.jpg","title":"三体","url":"https://movie.douban.com/subject/26647087/?suggest=三体","type":"movie","year":"2023","sub_title":"三体","id":"26647087"},
          {"episode":"","img":"https://img9.doubanio.com/view/photo/s_ratio_poster/public/p2916835424.webp","title":"流浪地球2","url":"https://movie.douban.com/subject/35267208/?suggest=流浪地球","type":"movie","year":"2023","sub_title":"流浪地球2","id":"35267208"}
        ]
    """.trimIndent()

    private val movieJson = """
        {
          "id":"26266893",
          "title":"流浪地球",
          "original_title":"The Wandering Earth",
          "year":"2019",
          "subtype":"movie",
          "is_tv":false,
          "intro":"近未来，科学家们发现太阳急速衰老膨胀……\n本片根据刘慈欣的同名小说改编。",
          "rating":{"value":7.9,"max":10,"count":2058501,"star_count":3.5},
          "genres":["科幻","冒险","灾难"],
          "countries":["中国大陆"],
          "languages":["汉语普通话","英语"],
          "durations":["125分钟","137分钟(重映版)"],
          "pubdate":["2019-02-05(中国大陆)"],
          "pic":{"normal":"https://img3.doubanio.com/view/photo/s_ratio_poster/public/p2545472803.jpg","large":"https://img3.doubanio.com/view/photo/m_ratio_poster/public/p2545472803.jpg"},
          "cover_url":"https://img3.doubanio.com/view/photo/m_ratio_poster/public/p2545472803.jpg",
          "card_subtitle":"2019 / 中国大陆 / 科幻 冒险 灾难 / 郭帆 / 吴京 屈楚萧",
          "aka":["流浪地球：飞跃2020特别版","The Wandering Earth"],
          "directors":[{"id":"1274508","name":"郭帆"}],
          "actors":[{"id":"1000523","name":"吴京"},{"id":"1315700","name":"屈楚萧"}],
          "episodes_count":0,
          "comment_count":742255
        }
    """.trimIndent()

    private val tvJson = """
        {
          "id":"26647087",
          "title":"三体",
          "original_title":"Three-Body",
          "year":"2023",
          "subtype":"tv",
          "is_tv":true,
          "intro":"21世纪初，地球基础科学遭到前所未有的动摇……",
          "rating":{"value":8.7,"max":10,"count":539406},
          "genres":["剧情","科幻"],
          "countries":["中国大陆"],
          "languages":["汉语普通话","英语"],
          "durations":["45分钟"],
          "pubdate":["2023-01-15(中国大陆)"],
          "pic":{"normal":"https://img2.doubanio.com/view/photo/s_ratio_poster/public/p2886492021.jpg","large":"https://img2.doubanio.com/view/photo/m_ratio_poster/public/p2886492021.jpg"},
          "directors":[{"name":"杨磊"}],
          "actors":[{"name":"张鲁一"},{"name":"于和伟"}],
          "episodes_count":30,
          "last_episode_number":30
        }
    """.trimIndent()

    /** 实测：电影 id 走 `/tv/` 时的 404 响应体 */
    private val errorJson =
        """{"request":"GET /v2/tv/26266893","msg":"subject_not_found","code":2215,"localized_message":""}"""

    @Test
    fun `搜索响应 - 顶层数组解析成候选`() {
        val dtos = gson.fromJson(suggestJson, Array<DoubanSuggestDto>::class.java).toList()
        val candidates = dtos.mapNotNull { it.toCandidate() }

        assertEquals(2, candidates.size)
        assertEquals("26647087", candidates[0].id)
        assertEquals("三体", candidates[0].title)
        assertEquals(2023, candidates[0].year)
        // episode 非空 → 有集数
        assertTrue(candidates[0].hasEpisode)
        // 海报升到 l 档
        assertEquals(
            "https://img2.doubanio.com/view/photo/l_ratio_poster/public/p2886492021.jpg",
            candidates[0].posterUrl
        )
        assertFalse(candidates[1].hasEpisode)
    }

    @Test
    fun `电影详情 - 字段映射与海报升级`() {
        val subject = gson.fromJson(movieJson, DoubanSubjectDto::class.java).toSubject()!!

        assertEquals("26266893", subject.id)
        assertEquals("流浪地球", subject.title)
        assertEquals("The Wandering Earth", subject.originalTitle)
        assertEquals(2019, subject.year)
        assertFalse(subject.isTv)
        assertTrue(subject.overview, subject.overview.startsWith("近未来"))
        assertEquals(7.9, subject.rating, 0.0001)
        assertEquals(listOf("科幻", "冒险", "灾难"), subject.genres)
        assertEquals(listOf("中国大陆"), subject.countries)
        assertEquals("2019-02-05", subject.releaseDate)
        assertEquals("125分钟", subject.duration)
        assertNull(subject.episodesCount)
        assertEquals(listOf("郭帆"), subject.directors)
        assertEquals(listOf("吴京", "屈楚萧"), subject.actors)
        assertEquals(
            "https://img3.doubanio.com/view/photo/l_ratio_poster/public/p2545472803.jpg",
            subject.posterUrl
        )
        // 背景图由剧照接口补，映射阶段必须留空，不能拿竖版海报冒充
        assertNull(subject.backdropUrl)
    }

    @Test
    fun `剧集详情 - 识别剧集并带出总集数`() {
        val subject = gson.fromJson(tvJson, DoubanSubjectDto::class.java).toSubject()!!

        assertTrue(subject.isTv)
        assertEquals("三体", subject.title)
        assertEquals(30, subject.episodesCount)
        assertEquals("2023-01-15", subject.releaseDate)
        assertEquals(8.7, subject.rating, 0.0001)
    }

    @Test
    fun `错误响应 - 解析结果为空而不是抛异常`() {
        assertNull(gson.fromJson(errorJson, DoubanSubjectDto::class.java).toSubject())
    }

    @Test
    fun `剧照响应 - 取第一张宽幅图的 large 地址`() {
        val photosJson = """
            {"start":0,"count":2,"total":2016,"photos":[
              {"id":"2542069917","image":{
                 "small":{"url":"https://img3.doubanio.com/view/photo/s/public/p2542069917.jpg","width":600,"height":232},
                 "normal":{"url":"https://img3.doubanio.com/view/photo/m/public/p2542069917.jpg","width":600,"height":232},
                 "large":{"url":"https://img3.doubanio.com/view/photo/l/public/p2542069917.jpg","width":1600,"height":619}}}
            ]}
        """.trimIndent()
        val dto = gson.fromJson(photosJson, DoubanPhotosDto::class.java)
        assertEquals(
            "https://img3.doubanio.com/view/photo/l/public/p2542069917.jpg",
            dto.firstWideImageUrl()
        )
    }

    @Test
    fun `剧照响应 - 没有图片时返回 null`() {
        val dto = gson.fromJson("""{"start":0,"count":0,"total":0,"photos":[]}""", DoubanPhotosDto::class.java)
        assertNull(dto.firstWideImageUrl())
    }
}
