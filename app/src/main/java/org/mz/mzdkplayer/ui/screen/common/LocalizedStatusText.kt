package org.mz.mzdkplayer.ui.screen.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text

@Composable
fun LocalizedStatusText(status: String?) {
    val localized = when (status?.trim()?.lowercase()) {
        // 电影状态
        "released" -> "已发布"
        "rumored" -> "传闻中"
        "planned" -> "计划中"

        // 剧集状态
        "returning series" -> "连载中"
        "in production" -> "制作中"
        "post production" -> "后期制作"
        "ended" -> "已完结"
        "canceled" -> "已取消"
        "pilot" -> "试播中"

        else -> status ?: "未知"
    }

    Text(
        text = localized,
        color = Color.LightGray,
        style = MaterialTheme.typography.bodyMedium
    )
}