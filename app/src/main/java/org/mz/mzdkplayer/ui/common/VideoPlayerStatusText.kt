package org.mz.mzdkplayer.ui.common

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import org.mz.mzdkplayer.R
import org.mz.mzdkplayer.viewmodel.VideoPlayerStatus

/**
 * 播放状态文案：**资源 id 到字符串的映射只存在于 UI 层**。
 *
 * `VideoPlayerStatus` 本身已经下沉到 :core（给 tv / phone 共用），而 :core 不带 android 资源，
 * 所以文案在这里落地 —— 与 `MzAspectRatio` 的文案放在 `ui/common/SettingOptionText.kt` 是同一口径。
 * 放在 app 的 main sourceSet，两个 flavor 都能用。
 */

fun VideoPlayerStatus.resId(): Int = when (this) {
    is VideoPlayerStatus.IDLE -> R.string.ui_label_initializing
    is VideoPlayerStatus.BUFFERING -> R.string.ui_label_buffering
    is VideoPlayerStatus.READY -> R.string.ui_label_playing
    is VideoPlayerStatus.ENDED -> R.string.ui_label_playback_ended
    is VideoPlayerStatus.Error -> R.string.ui_label_playback_error
}

fun VideoPlayerStatus.getDisplayString(context: Context): String = when (this) {
    is VideoPlayerStatus.Error -> context.getString(resId(), message)
    else -> context.getString(resId())
}

@Composable
fun VideoPlayerStatus.asDisplayString(): String = when (this) {
    is VideoPlayerStatus.Error -> stringResource(resId(), message)
    else -> stringResource(resId())
}
