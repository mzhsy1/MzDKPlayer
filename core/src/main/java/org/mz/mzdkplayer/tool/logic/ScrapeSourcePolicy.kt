package org.mz.mzdkplayer.tool.logic

/**
 * 刮削主数据源的选择（纯逻辑，无 Android 依赖，单测见 `ScrapeSourcePolicyTest`）。
 *
 * 两个数据源始终都在用，这个设置只决定**谁先谁后**：首选那个搜不到时，就用另一个兜底。
 * 顺序只在 [order] 里定义一次，业务侧（`MovieViewModel.searchAndFetchFullDetails`）按返回的顺序
 * 依次尝试、命中即停，避免 "谁先谁后" 的判断散落在业务代码里。
 */
object ScrapeSourcePolicy {

    /** 豆瓣（中文条目更全） */
    const val DOUBAN = "douban"

    /** TMDB */
    const val TMDB = "tmdb"

    /**
     * 默认首选豆瓣。
     *
     * 中文影视在豆瓣上条目更全：TMDB 对冷门国产剧 / 纪录片 / 综艺要么没有条目，要么只有外文名，
     * 而这正是这类用户的日常内容。反过来的代价（豆瓣没有分集信息、评分口径与 TMDB 不同）
     * 由设置项兜住 —— 想要 TMDB 主源随时可以切。
     */
    const val DEFAULT = DOUBAN

    /** 把存储 / UI 传来的值收敛成两个合法值之一，非法值（脏数据、旧版本残留）回退 [DEFAULT] */
    fun normalize(value: String?): String =
        if (value?.trim()?.lowercase() == TMDB) TMDB else DEFAULT

    /** 实际尝试顺序：首选在前、兜底在后 */
    fun order(preferred: String?): List<String> =
        if (normalize(preferred) == TMDB) listOf(TMDB, DOUBAN) else listOf(DOUBAN, TMDB)
}
