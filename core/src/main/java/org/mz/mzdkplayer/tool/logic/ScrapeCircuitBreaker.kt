package org.mz.mzdkplayer.tool.logic

/**
 * 数据源「临时不可达」的熔断器（纯逻辑：状态与时间都由外部传入，可直接单测）。
 *
 * 存在的原因：设置里可以把豆瓣选成首选源，而豆瓣走的是公开接口 —— 一旦连不上 / 被反爬挡住，
 * 每个文件都要先白等一次超时（最长 15 秒）才轮到另一个源兜底，批量刮削会慢到不可用。
 *
 * 规则：**连续**多次网络层失败后把这个源短路一段时间 —— 短路期内直接跳过（立刻走另一个源），
 * 冷却结束后放行**一次**探测请求：探测成功就立刻恢复正常，失败则继续短路一轮。
 *
 * 只处理「网络层失败」（连不上 / 超时 / 非 2xx / 拿到反爬空壳页）；「搜不到结果」是正常结局，
 * 不算失败，配合同一次请求里「拿到可解析响应就算能通」的判定，见 `DoubanScraper`。
 */
class ScrapeCircuitBreaker(
    private val failureThreshold: Int = DEFAULT_FAILURE_THRESHOLD,
    private val cooldownMillis: Long = DEFAULT_COOLDOWN_MILLIS,
) {

    private var consecutiveFailures = 0
    private var skipUntilMillis = 0L

    /** 现在是否应该跳过这个源（处于短路期）。到点后返回 false，即放行一次探测 */
    fun shouldSkip(nowMillis: Long): Boolean = nowMillis < skipUntilMillis

    /** 拿到了可解析的响应（结果是空也算）→ 判定「能通」，计数清零、立即恢复 */
    fun onReachable(nowMillis: Long) {
        consecutiveFailures = 0
        skipUntilMillis = 0L
    }

    /** 网络层失败：连续到阈值就短路一段时间 */
    fun onNetworkFailure(nowMillis: Long) {
        consecutiveFailures++
        if (consecutiveFailures >= failureThreshold) {
            skipUntilMillis = nowMillis + cooldownMillis
        }
    }

    companion object {
        /**
         * 连续失败几次算「这个源现在不可用」。
         *
         * 取 2：一次可能是网络抖动，连续两次基本就是断了 —— 而每多等一次都要白付一个超时。
         */
        const val DEFAULT_FAILURE_THRESHOLD = 2

        /**
         * 短路时长。
         *
         * 到点会放行一次探测，所以取「够久别再骚扰它、又不至于一整场会话都用不上」的 5 分钟。
         */
        const val DEFAULT_COOLDOWN_MILLIS = 5 * 60 * 1000L
    }
}
