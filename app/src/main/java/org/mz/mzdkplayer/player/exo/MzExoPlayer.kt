package org.mz.mzdkplayer.player.exo

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.annotation.OptIn
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.common.text.CueGroup
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.extractor.DefaultExtractorsFactory
import androidx.media3.extractor.mkv.MatroskaExtractor
import androidx.media3.extractor.ts.DefaultTsPayloadReaderFactory
import androidx.media3.extractor.ts.TsExtractor
import androidx.media3.ui.compose.ContentFrame
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.mz.mzdkplayer.ui.screen.vm.SettingsViewModel
import org.mz.mzdkplayer.ui.videoplayer.components.selectedDataSourceFactory

import org.mz.mzdkplayer.player.core.IMzPlayer
import org.mz.mzdkplayer.player.core.MzAspectRatio
import org.mz.mzdkplayer.player.core.MzBasicTrack
import org.mz.mzdkplayer.player.core.MzIsoTitle
import org.mz.mzdkplayer.player.core.MzVideoTrack
import org.mz.mzdkplayer.tool.FtpDataSource
import org.mz.mzdkplayer.tool.SmbDataSource
import org.mz.mzdkplayer.tool.WebDavDataSource
import org.mz.mzdkplayer.ui.screen.vm.VideoPlayerStatus
import androidx.core.net.toUri

@OptIn(UnstableApi::class)
class MzExoPlayer(
    context: Context,
    mediaUri: String,
    dataSourceType: String,
    settingsViewModel: SettingsViewModel
    // 可以把你原来的 settingsViewModel 相关的配置通过构造传进来
) : IMzPlayer {
    private val _videoTracks = MutableStateFlow<List<MzVideoTrack>>(emptyList())
    override val videoTracks: StateFlow<List<MzVideoTrack>> = _videoTracks.asStateFlow()

    private val _audioTracks = MutableStateFlow<List<MzBasicTrack>>(emptyList())
    override val audioTracks: StateFlow<List<MzBasicTrack>> = _audioTracks.asStateFlow()

    private val _subtitleTracks = MutableStateFlow<List<MzBasicTrack>>(emptyList())
    override val subtitleTracks: StateFlow<List<MzBasicTrack>> = _subtitleTracks.asStateFlow()

    private val _playerStatus = MutableStateFlow<VideoPlayerStatus>(VideoPlayerStatus.IDLE)
    override val playerStatus = _playerStatus.asStateFlow()

    private val _isPlayingFlow = MutableStateFlow(false)
    override val isPlayingFlow = _isPlayingFlow.asStateFlow()

    private val _playbackSpeed = MutableStateFlow(1.0f)
    override val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()

    private val _aspectRatio = MutableStateFlow(
        if (settingsViewModel.uiState.value.lockVideoRatio) {
            runCatching { MzAspectRatio.valueOf(settingsViewModel.uiState.value.globalVideoRatio) }.getOrDefault(MzAspectRatio.FIT)
        } else {
            MzAspectRatio.FIT
        }
    )
    override val aspectRatio: StateFlow<MzAspectRatio> = _aspectRatio.asStateFlow()

    override var onError: ((String) -> Unit)? = null
    override var onPlaybackEnded: (() -> Unit)? = null
    override var onCuesChanged: ((Any) -> Unit)? = null

    private var isFirstTrackAutoSelected = false

    val settingsState = settingsViewModel.uiState.value // 获取当前的设置状态

    // 这里放入你原本 rememberPlayer 中的 ExoPlayer 初始化逻辑
    // 1. 配置 RenderersFactory
    val renderersFactory: DefaultRenderersFactory = DefaultRenderersFactory(context)
        .forceEnableMediaCodecAsynchronousQueueing()
        .setExtensionRendererMode(settingsState.exoAudioDecodeMode)

    // 3. 配置 TrackSelector (隧道模式)
    val trackSelector = DefaultTrackSelector(context)

    // 3.5 配置 ExtractorsFactory (增强稳定性)
    val extractorsFactory = DefaultExtractorsFactory()
        .setTsExtractorFlags(DefaultTsPayloadReaderFactory.FLAG_ALLOW_NON_IDR_KEYFRAMES)

    // 4. 组装 ExoPlayer
    val exoPlayer: ExoPlayer = ExoPlayer.Builder(context)
        .setRenderersFactory(renderersFactory)
        .setTrackSelector(trackSelector)
        .setSeekForwardIncrementMs(30000)
        .setSeekBackIncrementMs(30000)
        .setMediaSourceFactory(
            DefaultMediaSourceFactory(
                selectedDataSourceFactory(mediaUri, dataSourceType, context),
                extractorsFactory
            )
        )
        .build().apply {
            playWhenReady = true
            repeatMode = Player.REPEAT_MODE_OFF
        }


    init {

        exoPlayer.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _isPlayingFlow.value = isPlaying
            }

            // 🔥🔥 关键修复：错误时立即更新状态为 Error
            override fun onPlayerError(error: PlaybackException) {
                val errMsg = error.message
                    ?: error.cause?.message
                    ?: "未知解码错误"

                _playerStatus.value = VideoPlayerStatus.Error(errMsg)
                onError?.invoke(errMsg)

                Log.e("MzExoPlayer", "播放错误: $errMsg", error)
            }

            override fun onCues(cueGroup: CueGroup) {
                onCuesChanged?.invoke(cueGroup)
            }

            override fun onPlaybackStateChanged(state: Int) {
                // 如果当前已经是错误状态，不要让 IDLE 或 BUFFERING 覆盖掉错误信息
                if (_playerStatus.value is VideoPlayerStatus.Error && state != Player.STATE_READY) {
                    return
                }
                _playerStatus.value = when (state) {
                    Player.STATE_READY -> VideoPlayerStatus.READY
                    Player.STATE_BUFFERING -> VideoPlayerStatus.BUFFERING
                    Player.STATE_ENDED -> {
                        onPlaybackEnded?.invoke()
                        VideoPlayerStatus.ENDED
                    }
                    else -> VideoPlayerStatus.IDLE
                }
            }

            override fun onPositionDiscontinuity(
                oldPosition: Player.PositionInfo,
                newPosition: Player.PositionInfo,
                reason: Int
            ) {
                // 如果需要向外抛出 Seek 事件，可以在接口加个回调
            }
        })
        trackSelector.parameters = trackSelector.buildUponParameters()
            .setTunnelingEnabled(settingsState.enableTunneling)
            .build()

        val preferredAudioLanguage = settingsState.audioLang.ifEmpty { null }
        // --- 字幕语言设置 ---
        val preferredTextLanguage = settingsState.subLang.ifEmpty { null }
        Log.d("preferredTextLanguage", preferredTextLanguage.toString())
        val trackSelectionParameters = exoPlayer.trackSelectionParameters.buildUpon()
            .setPreferredTextLanguage(preferredTextLanguage)
            .setPreferredAudioLanguage(preferredAudioLanguage)
            .setSelectUndeterminedTextLanguage(true)//// 关键：允许选中语言为 "und" 的字幕 将 "zh" 替换为你需要的默认字幕语言代码，例如 "en" 表示英语
            .build()
        exoPlayer.trackSelectionParameters = trackSelectionParameters
        val mediaItem = MediaItem.Builder().setUri(mediaUri).build()
        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()

        exoPlayer.addListener(object : Player.Listener {

            override fun onTracksChanged(tracks: Tracks) {
                // 只有当用户没手动选过，且设置里没有指定语言时，才强制选第一个
                if (!isFirstTrackAutoSelected && preferredTextLanguage.isNullOrEmpty()) {
                    autoSelectFirstSubtitle(tracks)
                }
                updateTracks()
            }
        })
    }
    override val videoWidth: Int
        get() = if (exoPlayer.videoSize.width > 0) exoPlayer.videoSize.width else 1920

    override val videoHeight: Int
        get() = if (exoPlayer.videoSize.height > 0) exoPlayer.videoSize.height else 1080

    override val isoTitles: StateFlow<List<MzIsoTitle>>
        get() = MutableStateFlow(emptyList())

    override fun selectIsoTitle(index: Int) {

    }

    private fun autoSelectFirstSubtitle(tracks: Tracks) {
        val textGroups = tracks.groups.filter { it.type == C.TRACK_TYPE_TEXT }
        if (textGroups.isNotEmpty()) {
            Log.i("MzExoPlayer", "自动选择第一个字幕轨道")
            exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters.buildUpon()
                .setOverrideForType(
                    TrackSelectionOverride(textGroups[0].mediaTrackGroup, 0)
                )
                .build()
            isFirstTrackAutoSelected = true
        }
    }

    private fun updateTracks() {
        val trackGroups = exoPlayer.currentTracks.groups
        val vTracks = mutableListOf<MzVideoTrack>()
        val aTracks = mutableListOf<MzBasicTrack>()
        val sTracks = mutableListOf<MzBasicTrack>()

        for ((groupIndex, trackGroup) in trackGroups.withIndex()) {
            val format = trackGroup.getTrackFormat(0)
            val isSelected = trackGroup.isTrackSelected(0)

            when (trackGroup.type) {
                C.TRACK_TYPE_VIDEO -> {
                    val colorTransfer = format.colorInfo?.colorTransfer ?: C.INDEX_UNSET
                    val colorSpace = format.colorInfo?.colorSpace ?: C.INDEX_UNSET
                    val videoCode = format.codecs.orEmpty()
                    val isDolbyVision = videoCode.contains("dvh", ignoreCase = true)
                    val isHdr10 = (colorTransfer == C.COLOR_TRANSFER_ST2084 ||
                            colorTransfer == C.COLOR_TRANSFER_HLG) &&
                            colorSpace == C.COLOR_SPACE_BT2020 && !isDolbyVision

                    vTracks.add(
                        MzVideoTrack(
                            id = format.id ?: groupIndex.toString(),
                            index = groupIndex,
                            height = format.height,
                            bitrate = format.bitrate,
                            codecs = videoCode,
                            isSelected = isSelected,
                            isDolbyVision = isDolbyVision,
                            isHdr10 = isHdr10,
                            rawData = trackGroup // 保留 TrackGroup 用于切换
                        )
                    )
                }

                C.TRACK_TYPE_AUDIO -> {
                    aTracks.add(
                        MzBasicTrack(
                            format.id ?: groupIndex.toString(),
                            groupIndex,
                            "Audio $groupIndex",
                            isSelected,
                            format.language.orEmpty(),
                            trackGroup.getTrackFormat(0).sampleMimeType.toString(),
                            channelCount = trackGroup.getTrackFormat(0).channelCount,
                            rawData = trackGroup
                        )
                    )
                }

                C.TRACK_TYPE_TEXT -> {
                    // 优先取 label，如果没有 label 再取内置的 id，最后才用兜底名称
                    val displayName = when {
                        !format.label.isNullOrEmpty() -> format.label!! // ✅ 关键：这才会拿到 "[外部加载] ass"
                        !format.id.isNullOrEmpty() -> "Subtitle ${format.id}"
                        else -> "Subtitle $groupIndex"
                    }
                    sTracks.add(
                        MzBasicTrack(
                            format.id ?: groupIndex.toString(),
                            groupIndex,
                            name = displayName,
                            isSelected,
                            format.language.orEmpty(),
                            trackGroup.getTrackFormat(0).codecs.toString(),
                            rawData = trackGroup
                        )
                    )
                }
            }
        }
        _videoTracks.value = vTracks
        _audioTracks.value = aTracks
        _subtitleTracks.value = sTracks
    }

    override val isPlaying: Boolean get() = exoPlayer.isPlaying
    override val currentPosition: Long get() = exoPlayer.currentPosition
    override val duration: Long get() = exoPlayer.duration

    override fun play() = exoPlayer.play()
    override fun pause() = exoPlayer.pause()
    override fun seekTo(positionMs: Long) = exoPlayer.seekTo(positionMs)
    override fun seekForward(ms: Long) = exoPlayer.seekTo(currentPosition + ms)
    override fun seekBack(ms: Long) = exoPlayer.seekTo(currentPosition - ms)

    override fun selectVideoTrack(track: MzVideoTrack) {
        val group = track.rawData as? Tracks.Group ?: return
        exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters.buildUpon()
            .setOverrideForType(TrackSelectionOverride(group.mediaTrackGroup, 0))
            .build()
    }

    override fun selectAudioTrack(track: MzBasicTrack) {
        Log.i("AudioTrackSwitch", "AudioTrackSwitchB")
        val group = track.rawData as? Tracks.Group ?: return
        exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters.buildUpon()
            .setOverrideForType(TrackSelectionOverride(group.mediaTrackGroup, 0))
            .build()
        Log.i("AudioTrackSwitch", "AudioTrackSwitchE")
    }

    override fun selectSubtitleTrack(track: MzBasicTrack) {
        val group = track.rawData as? Tracks.Group ?: return
        exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters.buildUpon()
            .setOverrideForType(TrackSelectionOverride(group.mediaTrackGroup, 0))
            .build()
    }

    override fun release() {
        exoPlayer.release()
    }

    override fun setPlaybackSpeed(speed: Float) {
        if (settingsState.enablePassthrough && speed != 1.0f) {
            return
        }
        _playbackSpeed.value = speed
        exoPlayer.playbackParameters = PlaybackParameters(speed)
    }

    override fun setAspectRatio(ratio: MzAspectRatio) {
        _aspectRatio.value = ratio
    }

    override fun addExternalSubtitles(subtitles: List<Pair<String, String>>) {
        val currentMediaItem = exoPlayer.currentMediaItem ?: return
        val currentPos = exoPlayer.currentPosition

        // 批量生成字幕配置
        val newConfigs = subtitles.map { (uri, name) ->
            val mimeType = when {
                uri.endsWith(".ass", ignoreCase = true) || uri.endsWith(".ssa", ignoreCase = true) -> MimeTypes.TEXT_SSA
                uri.endsWith(".srt", ignoreCase = true) -> MimeTypes.APPLICATION_SUBRIP
                uri.endsWith(".vtt", ignoreCase = true) -> MimeTypes.TEXT_VTT
                else -> MimeTypes.APPLICATION_SUBRIP
            }

            MediaItem.SubtitleConfiguration.Builder(uri.toUri())
                .setMimeType(mimeType)
                .setLanguage("zh")
                .setLabel(name)
                .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
                .build()
        }

        val existingSubtitles = currentMediaItem.localConfiguration?.subtitleConfigurations ?: emptyList()
        val allSubtitles = existingSubtitles + newConfigs

        // 一次性重载，防止卡顿
        val newMediaItem = currentMediaItem.buildUpon()
            .setSubtitleConfigurations(allSubtitles)
            .build()

        exoPlayer.setMediaItem(newMediaItem, currentPos)
        exoPlayer.prepare()
        exoPlayer.play()

        // 重置自动选择标记，让 onTracksChanged 能够为新 MediaItem 选中字幕
        isFirstTrackAutoSelected = false
    }
    @Composable
    override fun PlayerView(modifier: Modifier) {
        val currentRatio by aspectRatio.collectAsState()

        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            when (currentRatio) {
                MzAspectRatio.FIT -> {
                    ContentFrame(
                        player = exoPlayer,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                }
                MzAspectRatio.STRETCH -> {
                    ContentFrame(
                        player = exoPlayer,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.FillBounds
                    )
                }
                MzAspectRatio.RATIO_16_9 -> {
                    ContentFrame(
                        player = exoPlayer,
                        modifier = Modifier.aspectRatio(16f / 9f),
                        contentScale = ContentScale.FillBounds
                    )
                }
                MzAspectRatio.RATIO_4_3 -> {
                    ContentFrame(
                        player = exoPlayer,
                        modifier = Modifier.aspectRatio(4f / 3f),
                        contentScale = ContentScale.FillBounds
                    )
                }
                MzAspectRatio.ZOOM -> {
                    ContentFrame(
                        player = exoPlayer,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }
    }
}
