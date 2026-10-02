package org.mz.mzdkplayer.tool

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mz.mzdkplayer.tool.logic.ScrapeCircuitBreaker

/**
 * [ScrapeCircuitBreaker] 的 JVM 单测。
 *
 * 时间全部由参数传入，所以这里不依赖 `System.currentTimeMillis()`，用例是确定的。
 * 关键契约：①比阈值少一次失败**不能**短路；②短路期内一律跳过；③冷却结束后放行一次探测；
 * ④探测失败继续短路、探测成功立刻恢复（"成功即恢复"）。
 */
class ScrapeCircuitBreakerTest {

    /** 阈值 2、冷却 1000ms，便于把时间写成整齐的数字 */
    private fun newBreaker() = ScrapeCircuitBreaker(failureThreshold = 2, cooldownMillis = 1000L)

    @Test
    fun `初始状态不跳过`() {
        val breaker = newBreaker()
        assertFalse(breaker.shouldSkip(0L))
        assertFalse(breaker.shouldSkip(999_999L))
    }

    @Test
    fun `失败次数没到阈值不短路`() {
        val breaker = newBreaker()
        breaker.onNetworkFailure(0L)
        assertFalse(breaker.shouldSkip(0L))
        assertFalse(breaker.shouldSkip(500L))
    }

    @Test
    fun `连续失败到阈值就短路一个冷却周期`() {
        val breaker = newBreaker()
        breaker.onNetworkFailure(0L)
        breaker.onNetworkFailure(100L)
        assertTrue(breaker.shouldSkip(100L))
        assertTrue(breaker.shouldSkip(500L))
        // 冷却 1000ms：到点（>=100）放行一次探测
        assertTrue(breaker.shouldSkip(1099L))
        assertFalse(breaker.shouldSkip(1100L))
    }

    @Test
    fun `冷却后探测失败会继续短路`() {
        val breaker = newBreaker()
        breaker.onNetworkFailure(0L)
        breaker.onNetworkFailure(0L)   // 触发短路：0 ~ 1000
        assertFalse(breaker.shouldSkip(1000L))   // 放行探测
        breaker.onNetworkFailure(1000L)          // 探测还是失败
        assertTrue(breaker.shouldSkip(1000L))
        assertTrue(breaker.shouldSkip(1500L))
        assertFalse(breaker.shouldSkip(2000L))   // 再等一个冷却周期
    }

    @Test
    fun `拿到可解析响应立刻恢复`() {
        val breaker = newBreaker()
        breaker.onNetworkFailure(0L)
        breaker.onNetworkFailure(0L)
        assertTrue(breaker.shouldSkip(0L))

        breaker.onReachable(600L)   // 短路期内（比如别处调了一次）拿到响应
        assertFalse(breaker.shouldSkip(600L))
        // 恢复后计数也清零：再失败一次不应该立刻又被短路
        breaker.onNetworkFailure(700L)
        assertFalse(breaker.shouldSkip(700L))
    }

    @Test
    fun `中间成功一次会打断连续计数`() {
        val breaker = newBreaker()
        breaker.onNetworkFailure(0L)
        breaker.onReachable(10L)
        breaker.onNetworkFailure(20L)
        assertFalse("成功过就不该累计成连续失败", breaker.shouldSkip(20L))
    }
}
