package org.mz.mzdkplayer.ui.screen.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import org.mz.mzdkplayer.R

@Composable
fun LocalizedStatusText(status: String?) {
    val context = LocalContext.current
    val localized = when (status?.trim()?.lowercase()) {
        // 电影状态
        "released" -> context.getString(R.string.ui_label_released)
        "rumored" -> context.getString(R.string.ui_label_rumored)
        "planned" -> context.getString(R.string.ui_label_planned)

        // 剧集状态
        "returning series" -> context.getString(R.string.ui_label_ongoing_series)
        "in production" -> context.getString(R.string.ui_label_in_production)
        "post production" -> context.getString(R.string.ui_label_post_production)
        "ended" -> context.getString(R.string.ui_label_completed)
        "canceled" -> context.getString(R.string.ui_label_canceled)
        "pilot" -> context.getString(R.string.ui_label_pilot_running)

        else -> status ?: context.getString(R.string.ui_label_unknown)
    }

    Text(
        text = localized,
        color = Color.LightGray,
        style = MaterialTheme.typography.bodyMedium
    )
}