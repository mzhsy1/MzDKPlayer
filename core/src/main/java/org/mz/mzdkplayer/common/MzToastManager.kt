package org.mz.mzdkplayer.common

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

/**
 * 全局轻提示的「状态机」，与具体的设计系统无关：
 * 这里只维护要显示什么、显示多久，怎么画由各端 UI 自己决定
 * （电视端 `ui/screen/common/Custom.kt` 的 `MzToast`，手机端用 Snackbar）。
 *
 * 原本定义在 app 的 `ui/screen/common/Custom.kt` 里，但它被业务层
 * （`Tools.validateXxxConnectionParams`、`MzVlcPlayer`）反向引用，
 * 所以下沉到 :core 打断 core → app 的依赖环。
 */
object MzToastManager {
    val state = MzToastState()
    private var scope: CoroutineScope? = null

    fun init(scope: CoroutineScope) {
        this.scope = scope
    }

    fun show(message: String) {
        scope?.let {
            state.show(message, it)
        }
    }
}

class MzToastState {
    var message by mutableStateOf("")
    var isVisible by mutableStateOf(false)
    private var job: Job? = null

    /** 第几次提示：旧提示的收尾不能把新提示一起收掉，用它比对 */
    private var token = 0

    fun show(msg: String, scope: CoroutineScope, duration: Long = 3000L) {
        // 作用域已经死了（Activity 重建 / 页面销毁后 UI 还没重新 init）就直接不弹：
        // 弹了就没人负责收，isVisible 会永远停在 true，界面上留一个怎么都消不掉的提示框
        if (!scope.isActive) return

        message = msg
        isVisible = true
        val currentToken = ++token
        job?.cancel()
        // 收尾放在 invokeOnCompletion 而不是协程体的 finally 里：协程可能**在跑起来之前**就被取消
        // （刚 launch 就被 cancel，协程体一行都不执行，finally 自然也不会跑），那样 isVisible 会
        // 永远停在 true —— 界面上的提示框就再也消不掉了。invokeOnCompletion 无论协程有没有真正
        // 开始执行都一定会触发。
        job = scope.launch {
            delay(duration.milliseconds)
        }.also { newJob ->
            newJob.invokeOnCompletion {
                // 只有仍是「当前这一次」提示才有权收自己，否则被新提示取消的旧 job
                // 会在收尾时把新提示一起抹掉
                if (currentToken == token) {
                    isVisible = false
                }
            }
        }
    }
}
