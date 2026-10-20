package org.mz.mzdkplayer.ui.common

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
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

/**
 * 播放状态胶囊：文案 + 前景色 + 底色，播放页第二行用。
 *
 * [VideoPlayerStatus] 里没有「暂停」这一态：暂停时状态仍是 READY，只是播放器不再前进，
 * 所以判断暂停必须额外看 [isPlaying]，否则暂停会一直显示成「播放中」。
 */
data class PlayerStatusChip(
    val text: String,
    val contentColor: Color,
    val containerColor: Color
)

private val StatusPlaying = Color(0xFF12EA89)
private val StatusPaused = Color(0xFFE9C46A)
private val StatusBuffering = Color(0xFF64B5F6)
private val StatusIdle = Color(0xFF9A96A6)
private val StatusEnded = Color(0xFF9A96A6)
private val StatusError = Color(0xFFEF5350)

@Composable
fun playerStatusChip(status: VideoPlayerStatus, isPlaying: Boolean): PlayerStatusChip {
    val (text, color) = when (status) {
        is VideoPlayerStatus.IDLE -> stringResource(R.string.ui_label_initializing) to StatusIdle
        is VideoPlayerStatus.BUFFERING -> stringResource(R.string.ui_label_buffering) to StatusBuffering
        is VideoPlayerStatus.READY ->
            if (isPlaying) {
                stringResource(R.string.ui_label_playing) to StatusPlaying
            } else {
                stringResource(R.string.ui_label_paused) to StatusPaused
            }
        is VideoPlayerStatus.ENDED -> stringResource(R.string.ui_label_playback_ended) to StatusEnded
        // 错误详情太长，胶囊里只放一句短文案
        is VideoPlayerStatus.Error -> stringResource(R.string.ui_label_playback_error_short) to StatusError
    }
    return PlayerStatusChip(
        text = text,
        contentColor = color,
        containerColor = color.copy(alpha = 0.18f)
    )
}
