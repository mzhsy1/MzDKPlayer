package org.mz.mzdkplayer.common

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * [MzToastState] 的 JVM 单元测试。
 *
 * 这条链路出问题时的表现特别扎眼：提示框浮在画面上再也不消失，遥控器怎么按都收不掉
 * （电视端的 `MzToast` 没有任何「手动关闭」的入口，只能等它自己的倒计时）。
 * 两种卡住的成因各钉一条：作用域已经死了不许弹、显示期间作用域死掉必须自己收掉；
 * 另外钉一条容易改错的相邻行为 ——「新提示把旧提示顶掉」时不能连新提示一起被收掉。
 */
class MzToastStateTest {

    private fun scopeOn(parent: Job) = CoroutineScope(parent + Dispatchers.Default)

    @Test
    fun `作用域存活时正常弹出提示`() = runBlocking {
        val state = MzToastState()
        val parent = Job()

        state.show("再按一次退出", scopeOn(parent))

        assertTrue(state.isVisible)
        assertEquals("再按一次退出", state.message)

        parent.cancelAndJoin()
    }

    @Test
    fun `倒计时结束后自动收掉提示`() = runBlocking {
        val state = MzToastState()
        val parent = Job()

        state.show("稍等", scopeOn(parent), duration = 50L)
        assertTrue(state.isVisible)

        delay(500)

        assertFalse(state.isVisible)
        parent.cancelAndJoin()
    }

    @Test
    fun `作用域已被取消时干脆不弹`() {
        val state = MzToastState()
        val parent = Job()
        parent.cancel()

        state.show("再按一次退出", scopeOn(parent))

        // 弹了就没人负责收，isVisible 会停在 true；宁可不提示
        assertFalse(state.isVisible)
    }

    @Test
    fun `显示期间作用域被取消也要收掉提示`() = runBlocking {
        val state = MzToastState()
        val parent = Job()

        state.show("再按一次退出", scopeOn(parent), duration = 60_000L)
        assertTrue(state.isVisible)

        // 模拟 Activity 重建 / 页面销毁：倒计时协程被掐断，收尾必须照样把提示收掉
        parent.cancelAndJoin()

        assertFalse(state.isVisible)
    }

    @Test
    fun `新提示顶掉旧提示时不会被旧提示的收尾一起收掉`() = runBlocking {
        val state = MzToastState()
        val parent = Job()
        val scope = scopeOn(parent)

        state.show("第一条", scope, duration = 60_000L)
        state.show("第二条", scope, duration = 60_000L)

        // 留出时间让被取消的旧协程跑完它的收尾
        delay(300)

        assertTrue(state.isVisible)
        assertEquals("第二条", state.message)

        parent.cancelAndJoin()
    }
}
