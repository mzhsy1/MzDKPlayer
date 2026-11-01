package org.mz.mzdkplayer.common

import android.view.View
import android.view.Window
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import java.util.Collections
import java.util.WeakHashMap

/**
 * 引用计数式的「沉浸式」管理器：进入播放页时隐藏系统栏（状态栏 / 导航栏），退出时还给系统。
 *
 * 为什么需要这个？
 * 电视端 MainActivity 只调了 `setDecorFitsSystemWindows(window, false)`，即「让内容画到系统栏下面」，
 * 但**没有隐藏**系统栏。大多数电视固件本来就不显示状态栏，所以看不出问题；而部分固件
 * （例如 Slimbox，可在设置里打开顶部状态栏）会常驻一条状态栏，全屏播视频时就压在画面上。
 * 沉浸式视频的标准做法是在播放页 hide(systemBars())。
 *
 * 为什么用引用计数而不是 addFlags/clearFlags 那种直接改？
 * NavHost 切集时有约 700ms 的交叉淡入淡出，**新旧两个播放页同时处于组合中**：
 *
 *   1. 新播放页进入组合 -> hide(systemBars())
 *   2. 700ms 后旧播放页离开组合 -> show(systemBars())
 *
 * 结果就是切集之后状态栏又冒出来了。这里按 Window 计数，只有最后一个持有者释放时才真正恢复。
 *
 * 用法：`DisposableEffect` 里 acquire，onDispose 里 release，成对出现。
 */
object ImmersiveModeManager {

    private val counters: MutableMap<Window, Int> =
        Collections.synchronizedMap(WeakHashMap<Window, Int>())

    /**
     * 申请隐藏系统栏，可重复调用，需与 [release] 成对出现。
     *
     * @param window 宿主窗口，一般为 `context.findActivity()?.window`
     * @param view 窗口内任意已 attach 的 View（Compose 里用 `LocalView.current`）
     */
    fun acquire(window: Window?, view: View?) {
        val w = window ?: return
        val v = view ?: return
        synchronized(counters) {
            counters[w] = (counters[w] ?: 0) + 1
            applyImmersive(w, v)
        }
    }

    /** 释放沉浸式，计数归零时恢复系统栏。 */
    fun release(window: Window?, view: View?) {
        val w = window ?: return
        val v = view ?: return
        synchronized(counters) {
            val count = (counters[w] ?: 1) - 1
            if (count > 0) {
                counters[w] = count
            } else {
                counters.remove(w)
                WindowCompat.getInsetsController(w, v)
                    .show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    /** 强制恢复（例如 Activity 销毁时兜底），忽略引用计数。 */
    fun reset(window: Window?, view: View?) {
        val w = window ?: return
        val v = view ?: return
        synchronized(counters) { counters.remove(w) }
        WindowCompat.getInsetsController(w, v)
            .show(WindowInsetsCompat.Type.systemBars())
    }

    private fun applyImmersive(window: Window, view: View) {
        WindowCompat.getInsetsController(window, view).apply {
            // 遥控器没有「滑动」，该行为等价于「只在系统主动要求时才临时出现」，播放中保持隐藏
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            hide(WindowInsetsCompat.Type.systemBars())
        }
    }
}
