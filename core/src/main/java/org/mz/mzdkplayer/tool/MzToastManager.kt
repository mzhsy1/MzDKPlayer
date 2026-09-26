package org.mz.mzdkplayer.tool

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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

    fun show(msg: String, scope: CoroutineScope, duration: Long = 3000L) {
        message = msg
        isVisible = true
        job?.cancel()
        job = scope.launch {
            delay(duration.milliseconds)
            isVisible = false
        }
    }
}
