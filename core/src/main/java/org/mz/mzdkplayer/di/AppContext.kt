package org.mz.mzdkplayer.di

import android.annotation.SuppressLint
import android.content.Context

/**
 * :core 自己的 Application Context 持有者。
 *
 * 业务层里少数几个拿不到 Context 的地方（例如 `MovieViewModel` 读 NFO）需要 Application Context，
 * 而 `MzDkPlayerApplication` 属于 app 模块 —— 直接引用它就是 core → app 的反向依赖。
 * 改成由 app 的 Application 在 `onCreate` 里调用 [init]，依赖方向就正过来了。
 */
object AppContext {

    @SuppressLint("StaticFieldLeak")
    private var appContext: Context? = null

    val isInitialized: Boolean get() = appContext != null

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    /** 拿不到就抛，避免把「初始化顺序写错」变成难查的 NPE。 */
    val require: Context
        get() = appContext
            ?: error("AppContext 尚未初始化：请在 Application.onCreate() 里调用 AppContext.init(this)")
}
