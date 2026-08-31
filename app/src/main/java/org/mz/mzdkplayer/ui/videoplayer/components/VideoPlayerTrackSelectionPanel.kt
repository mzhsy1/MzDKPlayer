package org.mz.mzdkplayer.ui.videoplayer.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.tv.material3.Icon
import androidx.tv.material3.ListItem
import androidx.tv.material3.ListItemDefaults
import androidx.tv.material3.Text
import org.mz.mzdkplayer.R
import org.mz.mzdkplayer.player.core.IMzPlayer
import org.mz.mzdkplayer.player.core.MzBasicTrack
import org.mz.mzdkplayer.player.core.MzIsoTitle
import org.mz.mzdkplayer.player.core.MzVideoTrack
import org.mz.mzdkplayer.player.core.MzAspectRatio
import org.mz.mzdkplayer.tool.Tools
import org.mz.mzdkplayer.tool.handleDPadKeyEvents
import org.mz.mzdkplayer.ui.screen.vm.SettingsViewModel
import org.mz.mzdkplayer.ui.screen.vm.VideoPlayerViewModel
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kuaishou.akdanmaku.ui.DanmakuPlayer
import kotlinx.coroutines.flow.StateFlow
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester

@Composable
fun RootSettingsPanel(
    videoPlayerViewModel: VideoPlayerViewModel,
    mediaUri: String,
    useVlc: Boolean,
    isoTitles: List<MzIsoTitle>
) {
    val isIso = Tools.extractFileExtension(mediaUri).uppercase() == "ISO"
    val showBluRayIcon = useVlc && isIso
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Column(modifier = Modifier.width(360.dp).fillMaxHeight()) {
        Text(
            text = stringResource(R.string.ui_label_settings),
            fontWeight = FontWeight.Bold,
            color = Color.White,
            fontSize = 24.sp,
            modifier = Modifier.padding(15.dp, 10.dp)
        )
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            item {
                SettingItem(
                    title = stringResource(R.string.ui_label_video_track),
                    icon = if (showBluRayIcon) R.drawable.blu_ray_disc else R.drawable.baseline_hd_24,
                    modifier = Modifier.focusRequester(focusRequester),
                    onClick = {
                        if (useVlc && isIso && isoTitles.isNotEmpty()) {
                            videoPlayerViewModel.selectedAorVorS = "ISO"
                        } else {
                            videoPlayerViewModel.selectedAorVorS = "V"
                        }
                    }
                )
            }
            item {
                SettingItem(
                    title = stringResource(R.string.ui_label_audio_track),
                    icon = R.drawable.baseline_speaker_24,
                    onClick = { videoPlayerViewModel.selectedAorVorS = "A" }
                )
            }
            item {
                SettingItem(
                    title = stringResource(R.string.ui_label_subtitle_select),
                    icon = R.drawable.baseline_subtitles_24,
                    onClick = { videoPlayerViewModel.selectedAorVorS = "S" }
                )
            }
            item {
                SettingItem(
                    title = stringResource(R.string.ui_label_speed),
                    icon = R.drawable.baseline_speed_24,
                    onClick = { videoPlayerViewModel.selectedAorVorS = "SPEED" }
                )
            }
            item {
                SettingItem(
                    title = stringResource(R.string.ui_label_aspect_ratio),
                    icon = R.drawable.aspect_ratio_24dp,
                    onClick = { videoPlayerViewModel.selectedAorVorS = "R" }
                )
            }
            item {
                SettingItem(
                    title = stringResource(R.string.ui_label_seek_duration),
                    icon = R.drawable.baseline_speed_24,
                    onClick = { videoPlayerViewModel.selectedAorVorS = "DURATION" }
                )
            }
            item {
                SettingItem(
                    title = stringResource(R.string.ui_label_danmaku_settings),
                    icon = R.drawable.video_danmu_config,
                    onClick = { videoPlayerViewModel.selectedAorVorS = "D" }
                )
            }
            item {
                SettingItem(
                    title = if (videoPlayerViewModel.isCusSubtitleViewVis) 
                        stringResource(R.string.ui_label_hide_custom_subtitle) 
                        else stringResource(R.string.ui_label_show_custom_subtitle),
                    icon = R.drawable.subtitles_off_24dp,
                    onClick = { 
                        videoPlayerViewModel.isCusSubtitleViewVis = !videoPlayerViewModel.isCusSubtitleViewVis 
                    }
                )
            }
        }
    }
}

@Composable
fun SettingItem(
    title: String,
    icon: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ListItem(
        selected = false,
        onClick = onClick,
        modifier = modifier.padding(start = 15.dp, end = 2.dp, top = 5.dp, bottom = 5.dp),
        colors = ListItemDefaults.colors(
            containerColor = Color.Transparent,
            contentColor = Color.White,
            focusedContainerColor = Color.White,
            focusedContentColor = Color.Black
        ),
        headlineContent = { Text(title) },
        leadingContent = { Icon(painterResource(icon), contentDescription = null, modifier = Modifier.size(24.dp)) },
        trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null) }
    )
}

@Composable
fun BoxScope.VideoPlayerTrackSelectionPanel(
    videoPlayerViewModel: VideoPlayerViewModel,
    settingsViewModel: SettingsViewModel,
    player: IMzPlayer,
    audioTracks: List<MzBasicTrack>,
    videoTracks: List<MzVideoTrack>,
    subtitleTracksFlow: StateFlow<List<MzBasicTrack>>,
    isoTitles: List<MzIsoTitle>,
    playbackSpeed: Float,
    currentAspectRatio: MzAspectRatio,
    enablePassthrough: Boolean,
    mDanmakuPlayer: DanmakuPlayer,
    mediaUri: String,
    useVlc: Boolean,
    onHideControls: () -> Unit
) {
    AnimatedVisibility(
        videoPlayerViewModel.atpVisibility,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = Modifier
            .widthIn(200.dp, 420.dp)
            .fillMaxHeight()
            .align(AbsoluteAlignment.CenterRight)
            .background(
                Color.Black.copy(0.8f), shape = RoundedCornerShape(2.dp)
            )
            .onFocusChanged {
                videoPlayerViewModel.atpFocus = it.isFocused
                if (!it.isFocused && videoPlayerViewModel.atpVisibility) {
                    onHideControls()
                }
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .handleDPadKeyEvents(
                    onRight = { true },
                    onUp = { true },
                    onDown = { true }
                )
        ) {
        when (videoPlayerViewModel.selectedAorVorS) {
            "ROOT" -> RootSettingsPanel(
                videoPlayerViewModel = videoPlayerViewModel,
                mediaUri = mediaUri,
                useVlc = useVlc,
                isoTitles = isoTitles
            )

            "A" -> AudioTrackPanel(
                lists = audioTracks, onTrackSelected = { track ->
                    player.selectAudioTrack(track)
                }
            )

            "V" -> VideoTrackPanel(lists = videoTracks, onTrackSelected = { track ->
                player.selectVideoTrack(track)
            })

            "D" -> DanmakuPanel(
                danmakuPlayer = mDanmakuPlayer,
                videoPlayerViewModel = videoPlayerViewModel,
            )

            "ISO" -> IsoTitlePanel(
                lists = isoTitles,
                onTitleSelected = { title ->
                    player.selectIsoTitle(title.index)
                }
            )

            "SPEED" -> PlaybackSpeedPanel(
                currentSpeed = playbackSpeed,
                onSpeedSelected = { speed ->
                    player.setPlaybackSpeed(speed)
                },
                isPassthroughEnabled = enablePassthrough
            )

            "R" -> {
                val settingsState by settingsViewModel.uiState.collectAsState()
                AspectRatioPanel(
                    currentRatio = currentAspectRatio,
                    isLocked = settingsState.lockVideoRatio,
                    onRatioSelected = { ratio ->
                        player.setAspectRatio(ratio)
                        if (settingsState.lockVideoRatio) {
                            settingsViewModel.setGlobalVideoRatio(ratio.name)
                        }
                    },
                    onLockedChange = { locked ->
                        settingsViewModel.toggleLockVideoRatio(locked)
                        if (locked) {
                            settingsViewModel.setGlobalVideoRatio(currentAspectRatio.name)
                        }
                    }
                )
            }

            "S" -> {
                SubtitleTrackPanel(
                    subtitleTracks = subtitleTracksFlow,
                    onTrackSelected = { track ->
                        player.selectSubtitleTrack(track)
                    },
                    onLoadExternalSubtitle = {
                        val lastDotIndex = mediaUri.lastIndexOf('.')
                        if (lastDotIndex > 0) {
                            val basePath = mediaUri.substring(0, lastDotIndex)
                            val extensions = listOf("ass", "srt", "ssa", "vtt")
                            val subList = extensions.map { ext ->
                                val rawSubtitleUrl = "$basePath.$ext"
                                val safeSubtitleUrl = Tools.encodeUrlForPlayer(rawSubtitleUrl)
                                safeSubtitleUrl to "[外部加载]$ext"
                            }
                            player.addExternalSubtitles(subList)
                        }
                    }
                )
            }

            "DURATION" -> {
                SeekDurationPanel(settingsViewModel = settingsViewModel)
            }
        }
        BackHandler(true) {
            if (videoPlayerViewModel.selectedAorVorS != "ROOT") {
                videoPlayerViewModel.selectedAorVorS = "ROOT"
            } else {
                videoPlayerViewModel.atpVisibility = false
            }
        }
    }
}
}

@Composable
fun SeekDurationPanel(settingsViewModel: SettingsViewModel) {
    val settingsState by settingsViewModel.uiState.collectAsState()
    Column(modifier = Modifier.width(360.dp).fillMaxHeight()) {
        Text(
            text = stringResource(R.string.ui_label_seek_duration),
            fontWeight = FontWeight.Bold,
            color = Color.White,
            fontSize = 24.sp,
            modifier = Modifier.padding(15.dp, 10.dp)
        )
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            item {
                NumberControl(
                    value = settingsState.ffDuration,
                    onValueChange = { settingsViewModel.setFFDuration(it) },
                    maxValue = 600,
                    minValue = 5,
                    label = stringResource(R.string.setting_ff_duration)
                )
            }
            item {
                NumberControl(
                    value = settingsState.rwDuration,
                    onValueChange = { settingsViewModel.setRWDuration(it) },
                    maxValue = 600,
                    minValue = 5,
                    label = stringResource(R.string.setting_rw_duration)
                )
            }
        }
    }
}
