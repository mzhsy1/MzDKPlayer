package org.mz.mzdkplayer.data.douban

/**
 * 豆瓣接口的**网络层**失败：连不上 / 超时 / 非 2xx / 拿到反爬空壳页（200 但不是 JSON）。
 *
 * 用异常而不是 `null` 是刻意的：在这条刮削链路上 `null` 的含义是「没有可用数据」（搜不到、
 * 没有匹配候选），属于正常结局、应当直接交给另一个数据源；而熔断器必须能把「接口现在不可用」
 * 和「接口通了但没这个条目」分开，否则搜不到冷门片也会把数据源判死。
 *
 * 只在 [DoubanScraper] 内部抛出并捕获，不会漏到业务层。
 */
internal class DoubanNetworkException(message: String) : Exception(message)
