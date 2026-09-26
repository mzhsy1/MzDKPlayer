package org.mz.mzdkplayer.ui.phone.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import org.mz.mzdkplayer.R
import org.mz.mzdkplayer.data.repository.SettingsRepository
import org.mz.mzdkplayer.player.core.IMzPlayer
import org.mz.mzdkplayer.player.core.autoLoadSameNameSubtitles
import org.mz.mzdkplayer.player.exo.MzExoPlayer
import org.mz.mzdkplayer.tool.Tools
import org.mz.mzdkplayer.ui.phone.PhoneIcons
import org.mz.mzdkplayer.viewmodel.SettingsViewModel
import org.mz.mzdkplayer.ui.common.getDisplayString

/** 进度轮询间隔：`IMzPlayer` 只暴露 `currentPosition` 快照，第一阶段不做可拖动进度条 */
private const val POSITION_POLL_INTERVAL_MS = 500L

/**
 * 手机端播放页（第一阶段：只验证「能不能播」）。
 *
 * 播放内核直接复用电视端的 [MzExoPlayer]（`IMzPlayer` 唯一实现），
 * 数据源工厂由 `selectedDataSourceFactory` 按 `dataSourceType` 决定，SMB 走 smbj 的 DataSource。
 *
 * 弹幕、轨道切换、倍速、画面比例等面板都**没有**搬过来，属于后续阶段。
 */
@Composable
fun PhonePlayerScreen(
    mediaUri: String,
    dataSourceType: String,
    title: String,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val settingsViewModel: SettingsViewModel = viewModel()

    val player: IMzPlayer = remember(mediaUri, dataSourceType) {
        MzExoPlayer(
            context = context,
            mediaUri = mediaUri,
            dataSourceType = dataSourceType,
            settingsViewModel = settingsViewModel,
        )
    }

    var errorMessage by remember { mutableStateOf<String?>(null) }
    var positionMs by remember { mutableLongStateOf(0L) }
    val isPlaying by player.isPlayingFlow.collectAsState()
    val playerStatus by player.playerStatus.collectAsState()

    DisposableEffect(player) {
        player.onError = { message -> errorMessage = message }
        onDispose { player.release() }
    }

    // 同名字幕自动加载，口径与电视端一致；找不到字幕只是没有字幕，不能影响播放
    LaunchedEffect(player) {
        if (SettingsRepository.autoLoadSubtitle) {
            runCatching { autoLoadSameNameSubtitles(mediaUri, dataSourceType, player) }
        }
    }

    LaunchedEffect(player) {
        while (true) {
            positionMs = player.currentPosition
            delay(POSITION_POLL_INTERVAL_MS)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        player.PlayerView(Modifier.fillMaxSize())

        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.35f))
                .safeDrawingPadding()
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.phone_action_back),
                    tint = Color.White,
                )
            }
            Text(
                text = title,
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.35f))
                .safeDrawingPadding()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            val duration = player.duration
            val progress = if (duration > 0L) {
                (positionMs.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
            } else {
                0f
            }

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth(),
            )

            Row(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = Tools.formatTime(positionMs),
                    color = Color.White,
                    style = MaterialTheme.typography.labelMedium,
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = Tools.formatTime(duration),
                    color = Color.White,
                    style = MaterialTheme.typography.labelMedium,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
            ) {
                FilledIconButton(
                    onClick = { if (isPlaying) player.pause() else player.play() },
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = Color.White,
                        contentColor = Color.Black,
                    ),
                ) {
                    Icon(
                        imageVector = if (isPlaying) PhoneIcons.Pause else Icons.Filled.PlayArrow,
                        contentDescription = null,
                    )
                }
            }

            Text(
                text = playerStatus.getDisplayString(context),
                color = Color.White.copy(alpha = 0.8f),
                style = MaterialTheme.typography.labelSmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }

        val currentError = errorMessage
        if (currentError != null) {
            Surface(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(24.dp),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer,
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = stringResource(R.string.phone_player_error),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = currentError,
                        style = MaterialTheme.typography.bodySmall,
                    )
                    FilledTonalButton(onClick = onBack) {
                        Text(stringResource(R.string.phone_action_back))
                    }
                }
            }
        }
    }
}
