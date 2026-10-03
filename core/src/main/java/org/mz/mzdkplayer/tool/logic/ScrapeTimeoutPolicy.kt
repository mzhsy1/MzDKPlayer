package org.mz.mzdkplayer.tool.logic

/**
 * 刮削请求的单次超时（纯逻辑，无 Android 依赖，单测见 `ScrapeTimeoutPolicyTest`）。
 *
 * 豆瓣与 TMDB 共用同一个值（设置项「刮削超时时间」）。为什么做成可调：
 * 两个源在正常网络下都是亚秒级响应，但**连不上时每个文件都要白等满这个超时** ——
 * 直连（无代理）访问 `api.themoviedb.org` 时尤其明显。给用户一个旋钮：
 * 网络好的人调小让批量刮削更快，网络差的人调大容忍抖动。
 *
 * 只提供固定档位而不是自由输入：太短会误杀慢速网络，太长又会让断网时的批量刮削非常难受，
 * 档位把选择限制在合理区间内，两端（电视端循环切换 / 手机端弹单选）也能共用同一份定义。
 */
object ScrapeTimeoutPolicy {

    /** 默认 5 秒 */
    const val DEFAULT_SECONDS = 5

    /** 可选档位（秒） */
    val OPTIONS = listOf(3, 5, 8, 10, 15, 30)

    /** 脏数据（旧版本残留 / 被手改的存储）收敛到合法档位，非法值回退 [DEFAULT_SECONDS] */
    fun normalize(seconds: Int): Int =
        if (seconds in OPTIONS) seconds else DEFAULT_SECONDS

    /** 下一档（电视端设置项是「按一次换一个」的交互）；到末尾回到第一档 */
    fun next(current: Int): Int =
        OPTIONS[(OPTIONS.indexOf(normalize(current)) + 1) % OPTIONS.size]
}
