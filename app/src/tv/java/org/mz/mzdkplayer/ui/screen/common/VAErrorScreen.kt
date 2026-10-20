package org.mz.mzdkplayer.ui.screen.common


import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.mz.mzdkplayer.R

/**
 * 加载失败占位。
 *
 * 原来用的是 100dp 的 error24 位图 + 纯白标题，和其他空态页两套观感；
 * 现在换成统一的 [EmptyStateView]，错误用偏红的强调色，详细错误信息作为说明文字。
 */
@Composable
fun VAErrorScreen(errorText: String = "未知错误") {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            EmptyStateView(
                iconRes = R.drawable.error24,
                title = stringResource(R.string.ui_label_something_went_wrong),
                description = errorText,
                accent = Color(0xFFE57373)
            )
        }
    }
}
