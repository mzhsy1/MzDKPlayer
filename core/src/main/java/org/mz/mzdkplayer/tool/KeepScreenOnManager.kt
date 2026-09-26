package org.mz.mzdkplayer.tool

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.Window
import android.view.WindowManager
import java.util.Collections
import java.util.WeakHashMap

/**
 * 从任意 Context 中取出宿主 Activity。
 *
 * Compose 里 `LocalContext.current` 在某些情况下拿到的是被包装过的 Context
 * （ContextThemeWrapper / ContextWrapper），直接 `as? Activity` 会得到 null，
 * 导致 FLAG_KEEP_SCREEN_ON 根本没设置上。这里逐层解包，保证能拿到 Activity。
 */
fun Context.findActivity(): Activity? {
    var ctx: Context = this
    while (true) {
        when (ctx) {
            is Activity -> return ctx
            is ContextWrapper -> ctx = ctx.baseContext
            else -> return null
        }
    }
}

/**
 * 引用计数式的「保持屏幕常亮」管理器。
 *
 * 为什么不能简单地 addFlags / clearFlags？
 * NavHost 切换目的地时（导航到下一集、手动选集都会 navigate 一个新目的地，
 * 并用 popUpTo inclusive 把旧的播放页弹掉），默认有约 700ms 的交叉淡入淡出动画，
 * 这段时间里 **新旧两个播放页同时处于组合中**。执行顺序是：
 *
 *   1. 新播放页进入组合  -> addFlags(FLAG_KEEP_SCREEN_ON)
 *   2. 约 700ms 后动画结束，旧播放页离开组合 -> clearFlags(FLAG_KEEP_SCREEN_ON)
 *
 * 结果：正在播放的新页面反而把常亮标志清掉了。屏幕休眠/屏保超时是从最后一次
 * 按键算起的，所以播到一半（约等于系统的休眠超时时间）机顶盒就弹出屏保了。
 *
 * 这也解释了「第一集正常、第二集开始就跳屏保」——从详情页首次进入播放器时
 * 没有任何页面会去清标志，而只要发生过一次切集就会被清掉。
 *
 * 这里按 Window 计数：只有最后一个持有者释放时才真正清除标志。
 */
object KeepScreenOnManager {

    private const val FLAG = WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON

    private val counters: MutableMap<Window, Int> =
        Collections.synchronizedMap(WeakHashMap<Window, Int>())

    /** 申请常亮，可重复调用，需与 [release] 成对出现。 */
    fun acquire(window: Window?) {
        val w = window ?: return
        synchronized(counters) {
            val count = (counters[w] ?: 0) + 1
            counters[w] = count
            w.addFlags(FLAG)
        }
    }

    /** 释放常亮，计数归零时才真正清除标志。 */
    fun release(window: Window?) {
        val w = window ?: return
        synchronized(counters) {
            val count = (counters[w] ?: 1) - 1
            if (count > 0) {
                counters[w] = count
            } else {
                counters.remove(w)
                w.clearFlags(FLAG)
            }
        }
    }

    /** 强制清除（例如 Activity 销毁时兜底），忽略引用计数。 */
    fun reset(window: Window?) {
        val w = window ?: return
        synchronized(counters) {
            counters.remove(w)
        }
        w.clearFlags(FLAG)
    }
}
