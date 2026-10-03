package org.mz.mzdkplayer.tool.logic

/**
 * 刮削数据源的单选（纯逻辑，无 Android 依赖，单测见 `ScrapeSourcePolicyTest`）。
 *
 * 设置里选谁就**只用谁**：豆瓣搜不到时不再去问 TMDB（反之亦然）。
 *
 * 为什么不做兜底（这是刻意的取舍，别再改回去）：
 * 1. 两个源的信息口径不一样 —— 豆瓣没有分集标题/简介/剧照、没有上映状态，兜底会让同一批文件
 *    里「豆瓣命中的」和「TMDB 命中的」混在媒体库里，展示不一致；
 * 2. 直连（无代理）环境下 `api.themoviedb.org` 不可达，兜底等于给每个文件白加一次超时等待，
 *    批量刮削会慢到不可用；
 * 3. 用户要的是「选的哪个源，就是哪个源」这种可预期的行为。
 *
 * 「用哪个源」只在 [resolve] 里定义一次，业务侧（`MovieViewModel.searchAndFetchFullDetails`）
 * 拿到返回值直接走对应抓取分支，不要再自己拼兜底顺序。
 */
object ScrapeSourcePolicy {

    /** 豆瓣：中文条目更全，但没有分集信息，且走公开接口（可能被限流） */
    const val DOUBAN = "douban"

    /** TMDB：字段最全（分集标题/简介/剧照、季数、真实上映状态），但需要能访问 api.themoviedb.org */
    const val TMDB = "tmdb"

    /**
     * 默认选豆瓣。
     *
     * 中文影视在豆瓣上条目更全：TMDB 对冷门国产剧 / 纪录片 / 综艺要么没有条目，要么只有外文名，
     * 而这正是这类用户的日常内容；而且豆瓣在国内网络下不需要代理就能访问。
     */
    const val DEFAULT = DOUBAN

    /** 把存储 / UI 传来的值收敛成两个合法值之一，非法值（脏数据、旧版本残留）回退 [DEFAULT] */
    fun normalize(value: String?): String =
        if (value?.trim()?.lowercase() == TMDB) TMDB else DEFAULT

    /** 设置值 → 本次刮削唯一使用的数据源（[DOUBAN] 或 [TMDB]，不会有第二个） */
    fun resolve(preferred: String?): String = normalize(preferred)
}
