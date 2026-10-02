package org.mz.mzdkplayer.ui.phone.component

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kuaishou.akdanmaku.ui.DanmakuPlayer
import org.mz.mzdkplayer.R
import org.mz.mzdkplayer.danmaku.danmakuConfigFor
import org.mz.mzdkplayer.danmaku.pushDanmakuConfig
import org.mz.mzdkplayer.data.model.DanmakuScreenRatio
import org.mz.mzdkplayer.data.model.DanmakuSettings
import org.mz.mzdkplayer.data.model.DanmakuType
import org.mz.mzdkplayer.data.model.VideoItem
import org.mz.mzdkplayer.data.repository.DanmakuSettingsManager
import org.mz.mzdkplayer.data.repository.VideoPlaylistRepository
import org.mz.mzdkplayer.player.core.IMzPlayer
import org.mz.mzdkplayer.player.core.MzAspectRatio
import org.mz.mzdkplayer.player.core.MzBasicTrack
import org.mz.mzdkplayer.player.core.MzIsoTitle
import org.mz.mzdkplayer.player.core.MzVideoTrack
import org.mz.mzdkplayer.tool.Tools
import org.mz.mzdkplayer.tool.logic.PhonePlayerLogic
import org.mz.mzdkplayer.tool.logic.SubtitleOffsetLogic
import org.mz.mzdkplayer.ui.common.VIDEO_FINISH_ACTION_COUNT
import org.mz.mzdkplayer.ui.common.formatAspectRatio
import org.mz.mzdkplayer.ui.common.formatVideoFinishAction
import org.mz.mzdkplayer.ui.phone.PhoneIcons
import org.mz.mzdkplayer.viewmodel.SettingsViewModel
import org.mz.mzdkplayer.viewmodel.VideoPlayerViewModel
import java.util.Locale

/**
 * 播放页的设置面板。手机端做成底部弹出的 `ModalBottomSheet`：根面板列条目，
 * 点进去换成子面板；子面板的返回键回到根面板，根面板的返回键关掉整张表。
 *
 * 与电视端的右侧抽屉一一对应（视频轨 / 音轨 / 字幕 / 字幕时间轴 / 倍速 / 画面比例 /
 * 弹幕 / 播放列表 / 播放完成动作 / 自定义字幕开关，外加 VLC 下的 ISO 标题）。
 * 每个面板只负责「展示 + 抛回调」，作用到播放器的动作由调用方传入。
 */
internal enum class PhonePlayerPanel {
    ROOT,
    VIDEO,
    AUDIO,
    SUBTITLE,
    SUBTIME,
    SPEED,
    RATIO,
    DANMAKU,
    PLAYLIST,
    ACTION,
    ISO,
}

/** 面板里列表的最大高度：够放内容，又不至于把播放画面全遮住 */
private val PanelListMaxHeight = 420.dp

/** 外部加载的字幕在轨道名里带的标记（与电视端一致，用来区分内嵌 / 外挂） */
private const val EXTERNAL_SUBTITLE_MARK = "[外部加载]"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PhonePlayerSheet(
    panel: PhonePlayerPanel,
    onPanelChange: (PhonePlayerPanel) -> Unit,
    onDismiss: () -> Unit,
    player: IMzPlayer,
    settingsViewModel: SettingsViewModel,
    videoPlayerViewModel: VideoPlayerViewModel,
    danmakuPlayer: DanmakuPlayer,
    videoTracks: List<MzVideoTrack>,
    audioTracks: List<MzBasicTrack>,
    subtitleTracks: List<MzBasicTrack>,
    isoTitles: List<MzIsoTitle>,
    playbackSpeed: Float,
    aspectRatio: MzAspectRatio,
    isPassthroughEnabled: Boolean,
    mediaUri: String,
    showCustomSubtitle: Boolean,
    onShowCustomSubtitleChange: (Boolean) -> Unit,
    onLoadExternalSubtitles: () -> Unit,
    onVideoSelected: (VideoItem) -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        // 默认从 `SheetValue.Hidden` 起弹；跳过「半展开」那一档，否则手机上会先弹到一半
        // 再要用户往上拖（音频播放列表同款处理）。
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        // 子面板里按返回键回到根面板；根面板本身交给系统返回键关掉整张表
        BackHandler(enabled = panel != PhonePlayerPanel.ROOT) {
            onPanelChange(PhonePlayerPanel.ROOT)
        }

        Column(modifier = Modifier.fillMaxWidth()) {
            when (panel) {
                PhonePlayerPanel.ROOT -> RootPanel(
                    mediaUri = mediaUri,
                    isoTitles = isoTitles,
                    showCustomSubtitle = showCustomSubtitle,
                    onShowCustomSubtitleChange = onShowCustomSubtitleChange,
                    onPanelChange = onPanelChange,
                )

                PhonePlayerPanel.VIDEO -> VideoTrackPanel(
                    tracks = videoTracks,
                    onBack = { onPanelChange(PhonePlayerPanel.ROOT) },
                    onSelect = { track -> player.selectVideoTrack(track) },
                )

                PhonePlayerPanel.AUDIO -> AudioTrackPanel(
                    tracks = audioTracks,
                    onBack = { onPanelChange(PhonePlayerPanel.ROOT) },
                    onSelect = { track -> player.selectAudioTrack(track) },
                )

                PhonePlayerPanel.SUBTITLE -> SubtitleTrackPanel(
                    tracks = subtitleTracks,
                    onBack = { onPanelChange(PhonePlayerPanel.ROOT) },
                    onSelect = { track -> player.selectSubtitleTrack(track) },
                    onLoadExternal = onLoadExternalSubtitles,
                )

                PhonePlayerPanel.SUBTIME -> SubtitleTimingPanel(
                    settingsViewModel = settingsViewModel,
                    onBack = { onPanelChange(PhonePlayerPanel.ROOT) },
                )

                PhonePlayerPanel.SPEED -> PlaybackSpeedPanel(
                    currentSpeed = playbackSpeed,
                    isPassthroughEnabled = isPassthroughEnabled,
                    onBack = { onPanelChange(PhonePlayerPanel.ROOT) },
                    onSelect = { speed -> player.setPlaybackSpeed(speed) },
                )

                PhonePlayerPanel.RATIO -> AspectRatioPanel(
                    currentRatio = aspectRatio,
                    settingsViewModel = settingsViewModel,
                    onBack = { onPanelChange(PhonePlayerPanel.ROOT) },
                    onSelect = { ratio -> player.setAspectRatio(ratio) },
                )

                PhonePlayerPanel.DANMAKU -> DanmakuPanel(
                    videoPlayerViewModel = videoPlayerViewModel,
                    danmakuPlayer = danmakuPlayer,
                    onBack = { onPanelChange(PhonePlayerPanel.ROOT) },
                )

                PhonePlayerPanel.PLAYLIST -> PlaylistPanel(
                    mediaUri = mediaUri,
                    onBack = { onPanelChange(PhonePlayerPanel.ROOT) },
                    onSelect = onVideoSelected,
                )

                PhonePlayerPanel.ACTION -> FinishActionPanel(
                    settingsViewModel = settingsViewModel,
                    onBack = { onPanelChange(PhonePlayerPanel.ROOT) },
                )

                PhonePlayerPanel.ISO -> IsoTitlePanel(
                    titles = isoTitles,
                    onBack = { onPanelChange(PhonePlayerPanel.ROOT) },
                    onSelect = { title -> player.selectIsoTitle(title.index) },
                )
            }
            Spacer(modifier = Modifier.size(8.dp))
        }
    }
}

// ────────────────────────────── 根面板 ──────────────────────────────

@Composable
private fun RootPanel(
    mediaUri: String,
    isoTitles: List<MzIsoTitle>,
    showCustomSubtitle: Boolean,
    onShowCustomSubtitleChange: (Boolean) -> Unit,
    onPanelChange: (PhonePlayerPanel) -> Unit,
) {
    // 蓝光原盘（ISO）在 Exo 下连标题流都拿不到，只有 VLC 才有标题可选
    val isIso = Tools.extractFileExtension(mediaUri).uppercase() == "ISO"
    val showIsoTitles = isIso && isoTitles.isNotEmpty()

    Column(modifier = Modifier.fillMaxWidth()) {
        PanelHeader(title = stringResource(R.string.ui_label_settings))
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = PanelListMaxHeight),
            contentPadding = PaddingValues(bottom = 8.dp),
        ) {
            item {
                PanelEntry(
                    title = stringResource(R.string.ui_label_video_track),
                    icon = PhoneIcons.Movie,
                    onClick = {
                        onPanelChange(
                            if (showIsoTitles) PhonePlayerPanel.ISO else PhonePlayerPanel.VIDEO
                        )
                    },
                )
            }
            item {
                PanelEntry(
                    title = stringResource(R.string.ui_label_audio_track),
                    icon = PhoneIcons.Music,
                    onClick = { onPanelChange(PhonePlayerPanel.AUDIO) },
                )
            }
            item {
                PanelEntry(
                    title = stringResource(R.string.ui_label_subtitle_select),
                    icon = PhoneIcons.Subtitles,
                    onClick = { onPanelChange(PhonePlayerPanel.SUBTITLE) },
                )
            }
            item {
                PanelEntry(
                    title = stringResource(R.string.ui_label_subtitle_delay),
                    icon = PhoneIcons.Sync,
                    onClick = { onPanelChange(PhonePlayerPanel.SUBTIME) },
                )
            }
            item {
                PanelEntry(
                    title = stringResource(R.string.ui_label_speed),
                    icon = PhoneIcons.Speed,
                    onClick = { onPanelChange(PhonePlayerPanel.SPEED) },
                )
            }
            item {
                PanelEntry(
                    title = stringResource(R.string.ui_label_aspect_ratio),
                    icon = PhoneIcons.AspectRatio,
                    onClick = { onPanelChange(PhonePlayerPanel.RATIO) },
                )
            }
            item {
                PanelEntry(
                    title = stringResource(R.string.ui_label_danmaku_settings),
                    icon = PhoneIcons.Danmaku,
                    onClick = { onPanelChange(PhonePlayerPanel.DANMAKU) },
                )
            }
            item {
                PanelEntry(
                    title = stringResource(R.string.ui_label_playlist),
                    icon = PhoneIcons.Playlist,
                    onClick = { onPanelChange(PhonePlayerPanel.PLAYLIST) },
                )
            }
            item {
                PanelEntry(
                    title = stringResource(R.string.setting_video_finish_action),
                    icon = PhoneIcons.FinishAction,
                    onClick = { onPanelChange(PhonePlayerPanel.ACTION) },
                )
            }
            item {
                PanelEntry(
                    title = stringResource(
                        if (showCustomSubtitle) {
                            R.string.ui_label_hide_custom_subtitle
                        } else {
                            R.string.ui_label_show_custom_subtitle
                        }
                    ),
                    icon = PhoneIcons.Subtitles,
                    onClick = { onShowCustomSubtitleChange(!showCustomSubtitle) },
                    trailing = {
                        Switch(checked = showCustomSubtitle, onCheckedChange = null)
                    },
                )
            }
        }
    }
}

// ────────────────────────────── 轨道面板 ──────────────────────────────

@Composable
private fun VideoTrackPanel(
    tracks: List<MzVideoTrack>,
    onBack: () -> Unit,
    onSelect: (MzVideoTrack) -> Unit,
) {
    PanelScaffold(title = stringResource(R.string.ui_label_video_track), onBack = onBack) {
        if (tracks.isEmpty()) {
            item { EmptyEntry(stringResource(R.string.ui_label_no_video_tracks_in_file)) }
            return@PanelScaffold
        }
        itemsIndexed(tracks, key = { _, track -> "v-${track.id}" }) { _, track ->
            ListItem(
                onClick = { onSelect(track) },
                leadingContent = { SelectedMark(track.isSelected) },
                supportingContent = {
                    Text(
                        text = videoTrackDetails(track),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
            ) {
                Text(videoTrackQualityLabel(track))
            }
        }
    }
}

@Composable
private fun AudioTrackPanel(
    tracks: List<MzBasicTrack>,
    onBack: () -> Unit,
    onSelect: (MzBasicTrack) -> Unit,
) {
    PanelScaffold(title = stringResource(R.string.ui_label_audio_track), onBack = onBack) {
        if (tracks.isEmpty()) {
            item { EmptyEntry(stringResource(R.string.ui_label_no_audio_tracks_in_file)) }
            return@PanelScaffold
        }
        itemsIndexed(tracks, key = { _, track -> "a-${track.id}" }) { _, track ->
            ListItem(
                onClick = { onSelect(track) },
                leadingContent = { SelectedMark(track.isSelected) },
                supportingContent = {
                    Text(
                        // 「几声道 · 什么格式 · 采样率」与电视端共用同一条文案
                        text = stringResource(
                            R.string.ui_label_audio_track_details,
                            track.channelCount,
                            Tools.inferAudioFormatType(track.mimeType),
                            String.format(
                                Locale.getDefault(),
                                "%.1f kHz",
                                track.sampleRate / 1000.0,
                            ),
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
            ) {
                Text("${Tools.getFullLanguageName(track.language)} ${track.bitrate / 1000}Kbps")
            }
        }
    }
}

@Composable
private fun SubtitleTrackPanel(
    tracks: List<MzBasicTrack>,
    onBack: () -> Unit,
    onSelect: (MzBasicTrack) -> Unit,
    onLoadExternal: () -> Unit,
) {
    val unknownFormat = stringResource(R.string.ui_label_unknown_format)
    val externalMark = stringResource(R.string.ui_label_externally_loaded)
    val languageSubtitles = stringResource(R.string.ui_label_language_subtitles)

    PanelScaffold(
        title = stringResource(R.string.ui_label_subtitle_tracks),
        onBack = onBack,
        trailing = {
            TextButton(onClick = onLoadExternal) {
                Text(stringResource(R.string.ui_label_load_external_subtitles))
            }
        },
    ) {
        if (tracks.isEmpty()) {
            item { EmptyEntry(stringResource(R.string.ui_label_no_subtitle_tracks_in_file)) }
            return@PanelScaffold
        }
        itemsIndexed(tracks, key = { _, track -> "s-${track.id}" }) { _, track ->
            // id 为 "-1" 是「关闭字幕」那条虚拟轨，标题与副标题都与真实轨道不一样
            val isOff = track.id == "-1"
            val label = track.name
            val isExternal = label.contains(EXTERNAL_SUBTITLE_MARK)
            val cleanedLabel = if (isExternal) {
                label.substringAfter(EXTERNAL_SUBTITLE_MARK).trim()
            } else {
                label
            }
            val language = Tools.getFullLanguageName(track.language)
            val headline = when {
                isOff -> stringResource(R.string.ui_label_subtitle_off)
                isExternal -> "$language $externalMark $cleanedLabel"
                cleanedLabel.isNotEmpty() -> "$language $cleanedLabel"
                else -> "$language $languageSubtitles"
            }

            ListItem(
                onClick = { onSelect(track) },
                leadingContent = { SelectedMark(track.isSelected) },
                supportingContent = {
                    if (!isOff) {
                        Text(
                            stringResource(
                                R.string.ui_label_subtitle_format,
                                track.mimeType.ifEmpty { unknownFormat },
                            )
                        )
                    }
                },
            ) {
                Text(headline)
            }
        }
    }
}

@Composable
private fun IsoTitlePanel(
    titles: List<MzIsoTitle>,
    onBack: () -> Unit,
    onSelect: (MzIsoTitle) -> Unit,
) {
    PanelScaffold(
        title = "ISO " + stringResource(R.string.ui_label_video_track),
        onBack = onBack,
    ) {
        itemsIndexed(titles, key = { _, title -> "iso-${title.index}" }) { _, title ->
            ListItem(
                onClick = { onSelect(title) },
                leadingContent = { SelectedMark(title.isSelected) },
                supportingContent = {
                    Text(
                        text = title.durationText,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
            ) {
                Text(title.name)
            }
        }
    }
}

// ────────────────────────────── 其余面板 ──────────────────────────────

@Composable
private fun SubtitleTimingPanel(
    settingsViewModel: SettingsViewModel,
    onBack: () -> Unit,
) {
    val settingsState by settingsViewModel.uiState.collectAsState()
    val secondsUnit = stringResource(R.string.unit_seconds)

    // 本地即时显示：点一下数值立刻变，不等设置仓库回调；以外部值为 key，
    // 设置页（或恢复的记录）改过之后自动对齐
    var displayDelayMs by remember(settingsState.subtitleDelayMs) {
        mutableIntStateOf(settingsState.subtitleDelayMs)
    }

    fun apply(newDelayMs: Int) {
        displayDelayMs = newDelayMs
        // 设置页与播放页共用同一个存储值，顺手同步给播放器即时生效
        settingsViewModel.setSubtitleDelayMs(newDelayMs)
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        PanelHeader(
            title = stringResource(R.string.ui_label_subtitle_delay),
            onBack = onBack,
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 4.dp),
        ) {
            Text(
                text = stringResource(R.string.ui_label_subtitle_delay_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                // Exo 侧只能偏移文本字幕（PGS 等图形字幕的解析结果不带绝对时间戳）
                text = stringResource(R.string.ui_label_subtitle_delay_text_only),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(
                    onClick = { apply(SubtitleOffsetLogic.step(displayDelayMs, -1)) },
                    enabled = SubtitleOffsetLogic.canStep(displayDelayMs, -1),
                ) {
                    Text("-0.5$secondsUnit")
                }

                Text(
                    text = SubtitleOffsetLogic.formatSeconds(displayDelayMs) + secondsUnit,
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )

                TextButton(
                    onClick = { apply(SubtitleOffsetLogic.step(displayDelayMs, 1)) },
                    enabled = SubtitleOffsetLogic.canStep(displayDelayMs, 1),
                ) {
                    Text("+0.5$secondsUnit")
                }
            }

            TextButton(
                onClick = { apply(0) },
                enabled = !SubtitleOffsetLogic.isDefault(displayDelayMs),
            ) {
                Text(stringResource(R.string.ui_label_reset))
            }
        }
    }
}

@Composable
private fun PlaybackSpeedPanel(
    currentSpeed: Float,
    isPassthroughEnabled: Boolean,
    onBack: () -> Unit,
    onSelect: (Float) -> Unit,
) {
    PanelScaffold(title = stringResource(R.string.ui_label_speed), onBack = onBack) {
        if (isPassthroughEnabled) {
            item {
                Text(
                    text = stringResource(R.string.ui_label_speed_passthrough_locked),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
        }
        itemsIndexed(PhonePlayerLogic.speedOptions) { _, speed ->
            val selected = speed == currentSpeed
            ListItem(
                onClick = { onSelect(speed) },
                leadingContent = { SelectedMark(selected) },
            ) {
                Text("${speed}x")
            }
        }
    }
}

@Composable
private fun AspectRatioPanel(
    currentRatio: MzAspectRatio,
    settingsViewModel: SettingsViewModel,
    onBack: () -> Unit,
    onSelect: (MzAspectRatio) -> Unit,
) {
    val settingsState by settingsViewModel.uiState.collectAsState()
    val locked = settingsState.lockVideoRatio

    PanelScaffold(title = stringResource(R.string.ui_label_aspect_ratio), onBack = onBack) {
        item {
            ListItem(
                onClick = {
                    settingsViewModel.toggleLockVideoRatio(!locked)
                    // 打开锁定时把当前比例记成全局比例，否则「锁定」会锁到一个没意义的值
                    if (!locked) settingsViewModel.setGlobalVideoRatio(currentRatio.name)
                },
                trailingContent = {
                    Switch(checked = locked, onCheckedChange = null)
                },
            ) {
                Text(stringResource(R.string.setting_lock_video_ratio))
            }
            HorizontalDivider()
        }
        items(MzAspectRatio.entries) { ratio ->
            val selected = ratio == currentRatio
            ListItem(
                onClick = {
                    onSelect(ratio)
                    // 锁定状态下这一次改动就是新的全局比例
                    if (locked) settingsViewModel.setGlobalVideoRatio(ratio.name)
                },
                leadingContent = { SelectedMark(selected) },
            ) {
                Text(formatAspectRatio(ratio))
            }
        }
    }
}

@Composable
private fun FinishActionPanel(
    settingsViewModel: SettingsViewModel,
    onBack: () -> Unit,
) {
    val settingsState by settingsViewModel.uiState.collectAsState()
    val current = settingsState.videoFinishAction

    PanelScaffold(
        title = stringResource(R.string.setting_video_finish_action),
        onBack = onBack,
    ) {
        items((0 until VIDEO_FINISH_ACTION_COUNT).toList()) { index ->
            ListItem(
                onClick = { settingsViewModel.setVideoFinishAction(index) },
                leadingContent = { SelectedMark(index == current) },
            ) {
                Text(formatVideoFinishAction(index))
            }
        }
    }
}

@Composable
private fun PlaylistPanel(
    mediaUri: String,
    onBack: () -> Unit,
    onSelect: (VideoItem) -> Unit,
) {
    val playlist by VideoPlaylistRepository.playlist.collectAsState()
    val currentIndex = PhonePlayerLogic.playlistIndexOf(playlist.map { it.uri }, mediaUri)

    PanelScaffold(title = stringResource(R.string.ui_label_playlist), onBack = onBack) {
        if (playlist.isEmpty()) {
            item { EmptyEntry(stringResource(R.string.ui_label_no_content)) }
            return@PanelScaffold
        }
        itemsIndexed(playlist, key = { index, item -> "$index-${item.uri}" }) { index, item ->
            val current = index == currentIndex
            ListItem(
                onClick = { onSelect(item) },
                leadingContent = { SelectedMark(current) },
                trailingContent = {
                    Text(
                        text = (index + 1).toString(),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
            ) {
                Text(
                    text = item.fileName,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = if (current) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                )
            }
        }
    }
}

/**
 * 弹幕面板：开关 / 显示区域 / 按类型过滤 / 字号 / 透明度。
 *
 * 每改一项都做两件事：存进 `DanmakuSettingsManager`（下次进页面还是这一套）、
 * 推给渲染器（当场生效）。拼装与推送走 `:core` 里两端共用的 `danmakuConfigFor` /
 * `pushDanmakuConfig`，与电视端弹幕面板是同一份实现。
 *
 * 两个滑块只在松手时落盘 + 推送：拖动中每帧都推会反复重建弹幕布局。
 */
@Composable
private fun DanmakuPanel(
    videoPlayerViewModel: VideoPlayerViewModel,
    danmakuPlayer: DanmakuPlayer,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val settingsManager = remember { DanmakuSettingsManager(context) }
    var settings by remember { mutableStateOf(settingsManager.loadSettings()) }
    var previousScreenPart by remember {
        mutableFloatStateOf(videoPlayerViewModel.danmakuConfig.screenPart)
    }

    fun push(updated: DanmakuSettings) {
        settings = updated
        settingsManager.saveSettings(
            isSwitchEnabled = updated.isSwitchEnabled,
            selectedRatio = updated.selectedRatio,
            fontSize = updated.fontSize,
            transparency = updated.transparency,
            selectedTypes = updated.selectedTypes,
        )
        videoPlayerViewModel.danmakuConfig =
            videoPlayerViewModel.danmakuConfigFor(updated, visibility = updated.isSwitchEnabled)
        videoPlayerViewModel.danmakuVisibility = updated.isSwitchEnabled
        previousScreenPart = danmakuPlayer.pushDanmakuConfig(
            videoPlayerViewModel.danmakuConfig,
            previousScreenPart,
        )
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        PanelHeader(
            title = stringResource(R.string.ui_label_danmaku_settings),
            onBack = onBack,
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = PanelListMaxHeight)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            ListItem(
                onClick = { push(settings.copy(isSwitchEnabled = !settings.isSwitchEnabled)) },
                trailingContent = {
                    Switch(
                        checked = settings.isSwitchEnabled,
                        onCheckedChange = { checked ->
                            push(settings.copy(isSwitchEnabled = checked))
                        },
                    )
                },
            ) {
                Text(stringResource(R.string.ui_label_danmaku_toggle))
            }

            PanelSectionTitle(stringResource(R.string.ui_label_danmaku_display_area))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(DanmakuScreenRatio.displayNames) { name ->
                    FilterChip(
                        selected = name == settings.selectedRatio,
                        onClick = { push(settings.copy(selectedRatio = name)) },
                        label = { Text(name) },
                    )
                }
            }

            PanelSectionTitle(stringResource(R.string.ui_label_filter_by_type))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(DanmakuType.displayNames) { name ->
                    val selected = name in settings.selectedTypes
                    FilterChip(
                        selected = selected,
                        onClick = {
                            val next = if (selected) {
                                settings.selectedTypes - name
                            } else {
                                settings.selectedTypes + name
                            }
                            push(settings.copy(selectedTypes = next))
                        },
                        label = { Text(name) },
                    )
                }
            }

            PanelSectionTitle(
                "${stringResource(R.string.ui_label_danmaku_font_size)} · ${settings.fontSize}"
            )
            Slider(
                value = settings.fontSize.toFloat(),
                onValueChange = { value ->
                    settings = settings.copy(fontSize = value.toInt())
                },
                onValueChangeFinished = { push(settings) },
                valueRange = 10f..200f,
            )

            PanelSectionTitle(
                "${stringResource(R.string.ui_label_danmaku_opacity)} · ${settings.transparency}"
            )
            Slider(
                value = settings.transparency.toFloat(),
                onValueChange = { value ->
                    settings = settings.copy(transparency = value.toInt())
                },
                onValueChangeFinished = { push(settings) },
                valueRange = 0f..100f,
            )
        }
    }
}

// ────────────────────────────── 公共零件 ──────────────────────────────

/** 子面板外壳：标题行（带返回、可选右上角动作）+ 一个限高的列表 */
@Composable
private fun PanelScaffold(
    title: String,
    onBack: () -> Unit,
    trailing: (@Composable () -> Unit)? = null,
    content: LazyListScope.() -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        PanelHeader(title = title, onBack = onBack, trailing = trailing)
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = PanelListMaxHeight),
            contentPadding = PaddingValues(bottom = 8.dp),
            content = content,
        )
    }
}

@Composable
private fun PanelHeader(
    title: String,
    onBack: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 4.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.phone_action_back),
                )
            }
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .padding(start = if (onBack == null) 16.dp else 0.dp),
        )
        trailing?.invoke()
    }
}

@Composable
private fun PanelEntry(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit,
    trailing: (@Composable () -> Unit)? = null,
) {
    ListItem(
        onClick = onClick,
        leadingContent = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        trailingContent = trailing,
    ) {
        Text(title)
    }
}

/** 选中态的对勾；没选中时占一个同宽的空白，避免标题左右跳动 */
@Composable
private fun SelectedMark(selected: Boolean) {
    Box(modifier = Modifier.size(24.dp), contentAlignment = Alignment.Center) {
        if (selected) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = stringResource(R.string.ui_label_selected),
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun EmptyEntry(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun PanelSectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 12.dp, bottom = 6.dp),
    )
}

/** 视频轨的清晰度前缀，口径与电视端一致 */
@Composable
private fun videoTrackQualityLabel(track: MzVideoTrack): String = when {
    track.isDolbyVision -> stringResource(R.string.ui_label_dolby_vision)
    track.isHdr10 -> "HDR"
    track.height >= 2160 -> "4K/UHD"
    track.height >= 1440 -> "2K/1440P"
    track.height >= 1080 -> "1080P"
    track.height >= 720 -> "720P"
    else -> stringResource(R.string.ui_label_standard_definition)
}

/** 视频轨副标题：码率 + 编码（电视端那一行是图标，手机端空间有限改用文字） */
private fun videoTrackDetails(track: MzVideoTrack): String {
    val bitrate = String.format(Locale.getDefault(), "%.1f Mbps", track.bitrate / 1000.0 / 1000.0)
    val codec = when {
        track.codecs.contains("hev", ignoreCase = true) -> "H.265/HEVC"
        track.codecs.contains("avc", ignoreCase = true) -> "H.264/AVC"
        track.codecs.contains("av0", ignoreCase = true) -> "AV1"
        else -> track.codecs
    }
    return listOf(bitrate, codec).filter { it.isNotBlank() }.joinToString(" · ")
}
