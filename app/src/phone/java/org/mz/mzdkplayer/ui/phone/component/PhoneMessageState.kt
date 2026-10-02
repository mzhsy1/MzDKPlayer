package org.mz.mzdkplayer.ui.phone.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.mz.mzdkplayer.R

/**
 * 手机端各页共用的「加载中 / 空 / 失败（含缺权限）」提示块。
 *
 * 此前这几套在每个页面里各写一遍（指示器、文案样式、按钮位置都不完全一样），
 * 这里收成一份，页面只负责给文案与动作。电视端对应的是 `LoadingScreen` / `VAErrorScreen`。
 */

/** 居中的变形加载指示器 + 一行说明 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun PhoneLoading(
    modifier: Modifier = Modifier,
    text: String = stringResource(R.string.ui_label_loading),
) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        LoadingIndicator()
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 16.dp),
        )
    }
}

/**
 * 失败 / 缺权限共用的提示块。
 *
 * [onRetry] 为空时不显示重试按钮，改用 [actions] 里的自定义按钮（例如「去授权」）。
 */
@Composable
internal fun PhoneMessageBox(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    onRetry: (() -> Unit)? = null,
    isError: Boolean = true,
    actions: (@Composable () -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = if (isError) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.onSurface
            },
            textAlign = TextAlign.Center,
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp),
        )
        if (onRetry != null) {
            FilledTonalButton(
                onClick = onRetry,
                modifier = Modifier.padding(top = 16.dp),
            ) {
                Text(stringResource(R.string.phone_action_retry))
            }
        }
        actions?.invoke()
    }
}

/** 居中的空态：可选标题与图标，[message] 是必须的说明文案 */
@Composable
internal fun PhoneEmptyState(
    message: String,
    modifier: Modifier = Modifier,
    title: String? = null,
    icon: ImageVector? = null,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (title != null) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = if (icon != null) 16.dp else 0.dp),
            )
        }
        Text(
            text = message,
            style = if (title == null) {
                MaterialTheme.typography.bodyLarge
            } else {
                MaterialTheme.typography.bodySmall
            },
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(
                top = if (icon != null || title != null) 8.dp else 0.dp,
            ),
        )
    }
}
