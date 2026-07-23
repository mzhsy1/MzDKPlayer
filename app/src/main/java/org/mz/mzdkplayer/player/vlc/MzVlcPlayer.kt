package org.mz.mzdkplayer.player.vlc

import android.content.Context
import android.util.Log
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import androidx.media3.common.util.UnstableApi
import org.mz.mzdkplayer.player.core.IMzPlayer
import org.mz.mzdkplayer.player.core.MzBasicTrack
import org.mz.mzdkplayer.player.core.MzIsoTitle
import org.mz.mzdkplayer.player.core.MzVideoTrack
import org.mz.mzdkplayer.tool.FtpDataSource
import org.mz.mzdkplayer.tool.SmbDataSource
import org.mz.mzdkplayer.tool.Tools
import org.mz.mzdkplayer.tool.WebDavDataSource
import org.mz.mzdkplayer.ui.screen.common.MzToastManager
import org.mz.mzdkplayer.ui.screen.vm.SettingsViewModel
import org.mz.mzdkplayer.ui.screen.vm.VideoPlayerStatus
import org.videolan.libvlc.LibVLC
import org.videolan.libvlc.Media
import org.videolan.libvlc.MediaPlayer
import org.videolan.libvlc.interfaces.IMedia
import org.videolan.libvlc.util.VLCVideoLayout
import kotlin.time.Duration.Companion.milliseconds

@UnstableApi
class MzVlcPlayer(
    private val context: Context,
    private val mediaUri: String,
    dataSourceType: String,
    settingsViewModel: SettingsViewModel
) : IMzPlayer {

    val isPassthroughEnabled = settingsViewModel.uiState.value.enablePassthrough
    val preferredAudioLang: String = settingsViewModel.uiState.value.audioLang
    val preferredTextLang: String = settingsViewModel.uiState.value.subLang

    private val isNetworkProtocol: Boolean by lazy {
        val lower = mediaUri.lowercase()
        lower.startsWith("http://") || lower.startsWith("https://") ||
                lower.startsWith("ftp://") || lower.startsWith("smb://") ||
                lower.startsWith("nfs://") || lower.startsWith("rtsp://") ||
                lower.startsWith("rtmp://") ||
                !lower.startsWith("file:///")
    }

    // 1. 初始化 VLC 4.0 命令行参数（已精简并修复黑屏问题）
    private val options = arrayListOf(
        "-vvv",
        "--aout=audiotrack",
        // 🟢 修复黑屏：移除了 --vout=android_display 和 --no-video-deco
        "--file-caching=${if (!isNetworkProtocol) 500 else 1200}",
        "--network-caching=5000",
        "--clock-jitter=0",
        "--clock-synchro=0",
        "--sub-autodetect-file",
        "--sub-autodetect-fuzzy=2",
        "--freetype-font=Noto Serif CJK SC",
        "--freetype-rel-fontsize=20",
        "--freetype-opacity=255",
        "--freetype-color=0xFFFFFFFF",
        "--freetype-background-opacity=180",
        "--freetype-background-color=0x000000",
        "--text-renderer=freetype",
        "--audio-language=$preferredAudioLang",
        "--sub-language=$preferredTextLang"
    ).apply {
        if (isPassthroughEnabled) {
            add("--spdif")
        }
    }

    private val libVLC = LibVLC(context, options)
    private val mediaPlayer = MediaPlayer(libVLC)

    private val _playerStatus = MutableStateFlow<VideoPlayerStatus>(VideoPlayerStatus.IDLE)
    override val playerStatus: StateFlow<VideoPlayerStatus> = _playerStatus.asStateFlow()

    private val _isPlayingFlow = MutableStateFlow(false)
    override val isPlayingFlow: StateFlow<Boolean> = _isPlayingFlow.asStateFlow()

    private val _videoTracks = MutableStateFlow<List<MzVideoTrack>>(emptyList())
    override val videoTracks: StateFlow<List<MzVideoTrack>> = _videoTracks.asStateFlow()

    private val _audioTracks = MutableStateFlow<List<MzBasicTrack>>(emptyList())
    override val audioTracks: StateFlow<List<MzBasicTrack>> = _audioTracks.asStateFlow()

    private val _subtitleTracks = MutableStateFlow<List<MzBasicTrack>>(emptyList())
    override val subtitleTracks: StateFlow<List<MzBasicTrack>> = _subtitleTracks.asStateFlow()

    private val _isoTitles = MutableStateFlow<List<MzIsoTitle>>(emptyList())
    override val isoTitles: StateFlow<List<MzIsoTitle>> = _isoTitles.asStateFlow()

    private val _playbackSpeed = MutableStateFlow(1.0f)
    override val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()

    override var onError: ((String) -> Unit)? = null
    override var onCuesChanged: ((Any) -> Unit)? = null

    init {
        mediaPlayer.setAudioDigitalOutputEnabled(isPassthroughEnabled)
        mediaPlayer.setAudioOutput("audiotrack")

        val media = Media(libVLC, mediaUri.toUri()).apply {
            setHWDecoderEnabled(true, true)
            addOption(":http-user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
            addOption(":no-osd")
            addOption(":file-caching=${if (!isNetworkProtocol) 500 else 1200}")
        }

        mediaPlayer.media = media
        media.release()

        setupMediaParseListener()
        setupEventListener()
    }

    private var _videoWidth = 1920
    private var _videoHeight = 1080

    override val videoWidth: Int get() = _videoWidth
    override val videoHeight: Int get() = _videoHeight

    private fun setupEventListener() {
        mediaPlayer.setEventListener { event ->
            when (event.type) {
                MediaPlayer.Event.Playing -> {
                    _isPlayingFlow.value = true
                    _playerStatus.value = VideoPlayerStatus.READY
                }
                MediaPlayer.Event.LengthChanged -> {
                    Log.d("VLCPlayer", "总时长变更: ${event.lengthChanged} ms")
                }
                MediaPlayer.Event.Paused -> {
                    _isPlayingFlow.value = false
                }
                MediaPlayer.Event.Stopped -> {
                    _isPlayingFlow.value = false
                    _playerStatus.value = VideoPlayerStatus.ENDED
                }
                MediaPlayer.Event.Buffering -> {
                    if (event.buffering == 100f) {
                        _isPlayingFlow.value = true
                        _playerStatus.value = VideoPlayerStatus.READY
                    } else {
                        _isPlayingFlow.value = false
                        _playerStatus.value = VideoPlayerStatus.BUFFERING
                    }
                }
                MediaPlayer.Event.EndReached -> {
                    _playerStatus.value = VideoPlayerStatus.ENDED
                }
                MediaPlayer.Event.EncounteredError -> {
                    onError?.invoke("VLC 播放出错")
                }
                MediaPlayer.Event.ESAdded, MediaPlayer.Event.ESDeleted, MediaPlayer.Event.ESSelected -> {
                    CoroutineScope(Dispatchers.Main).launch {
                        delay(500.milliseconds)
                        updateTracks()
                    }
                }
            }
        }
    }

    private fun setupMediaParseListener() {
        val media = mediaPlayer.media ?: return

        media.setEventListener { event ->
            if (event.type == IMedia.Event.ParsedChanged &&
                event.parsedStatus == IMedia.ParsedStatus.Done) {
                Log.d("MzVlcPlayer", "Media 解析完成！")
                updateTracks()
            }
        }

        if (!media.isParsed) {
            val parseFlag = if (isNetworkProtocol)
                IMedia.Parse.ParseNetwork
            else
                IMedia.Parse.ParseLocal

            media.parseAsync(parseFlag)
        }
    }

    // 🟢 2. 重构 LibVLC 4.0 轨道信息获取逻辑
    private fun updateTracks() {
        // --- 🎧 音频轨道 ---
        val audioTracks4 = mediaPlayer.getTracks(IMedia.Track.Type.Audio) ?: emptyArray()
        val selectedAudioTrack = mediaPlayer.getSelectedTrack(IMedia.Track.Type.Audio)

        val audioList = mutableListOf<MzBasicTrack>()
        // 手动添加禁用轨选项
        audioList.add(
            MzBasicTrack(
                id = "-1",
                index = 0,
                name = "关闭音频",
                isSelected = selectedAudioTrack == null,
                rawData = "-1"
            )
        )
        audioTracks4.forEachIndexed { index, track ->
            val audioMeta = track as? IMedia.AudioTrack
            audioList.add(
                MzBasicTrack(
                    id = track.id,
                    index = index + 1,
                    language = track.language ?: "",
                    channelCount = audioMeta?.channels ?: 0,
                    mimeType = audioMeta?.codec ?: "",
                    sampleRate = audioMeta?.rate ?: 0,
                    bitrate = audioMeta?.bitrate ?: 0,
                    name = track.name.ifBlank { "音轨 ${index + 1}" },
                    isSelected = selectedAudioTrack?.id == track.id,
                    rawData = track.id
                )
            )
        }
        _audioTracks.value = audioList

        // --- 📝 字幕轨道 ---
        val spuTracks4 = mediaPlayer.getTracks(IMedia.Track.Type.Text) ?: emptyArray()
        val selectedSpuTrack = mediaPlayer.getSelectedTrack(IMedia.Track.Type.Text)

        val subtitleList = mutableListOf<MzBasicTrack>()
        subtitleList.add(
            MzBasicTrack(
                id = "-1",
                index = 0,
                name = "关闭字幕",
                isSelected = selectedSpuTrack == null,
                rawData = "-1"
            )
        )
        spuTracks4.forEachIndexed { index, track ->
            val subMeta = track as? IMedia.SubtitleTrack
            subtitleList.add(
                MzBasicTrack(
                    id = track.id,
                    index = index + 1,
                    language = track.language ?: "",
                    mimeType = subMeta?.codec ?: "",
                    name = track.name.ifBlank { "字幕 ${index + 1}" },
                    isSelected = selectedSpuTrack?.id == track.id,
                    rawData = track.id
                )
            )
        }
        _subtitleTracks.value = subtitleList

        // --- 📺 视频轨道 ---
        val videoTracks4 = mediaPlayer.getTracks(IMedia.Track.Type.Video) ?: emptyArray()
        val selectedVideoTrack = mediaPlayer.getSelectedTrack(IMedia.Track.Type.Video)

        _videoTracks.value = videoTracks4.mapIndexed { index, track ->
            val videoMeta = track as? IMedia.VideoTrack
            if (selectedVideoTrack?.id == track.id && videoMeta != null) {
                if (videoMeta.width > 0 && videoMeta.height > 0) {
                    _videoWidth = videoMeta.width
                    _videoHeight = videoMeta.height
                }
            }
            MzVideoTrack(
                id = track.id,
                index = index,
                height = videoMeta?.height ?: 0,
                bitrate = videoMeta?.bitrate ?: 0,
                codecs = videoMeta?.codec ?: "",
                isSelected = selectedVideoTrack?.id == track.id,
                rawData = track.id
            )
        }

        // --- 💿 蓝光/DVD Titles ---
        val titles = mediaPlayer.titles
        if (!titles.isNullOrEmpty()) {
            val currentTitleIdx = mediaPlayer.title
            _isoTitles.value = titles.mapIndexed { index, title ->
                MzIsoTitle(
                    index = index,
                    name = if (title.name.isNullOrBlank()) "视频片段 ${index + 1}" else title.name,
                    isSelected = index == currentTitleIdx,
                    durationText = Tools.formatTime(title.duration)
                )
            }
        } else {
            _isoTitles.value = emptyList()
        }
    }

    override fun selectIsoTitle(index: Int) {
        mediaPlayer.title = index
        updateTracks()
    }

    override val isPlaying: Boolean get() = mediaPlayer.isPlaying
    override val currentPosition: Long get() = mediaPlayer.time
    override val duration: Long get() = mediaPlayer.length

    override fun play() { mediaPlayer.play() }
    override fun pause() { mediaPlayer.pause() }
    override fun seekTo(positionMs: Long) { mediaPlayer.time = positionMs }
    override fun seekForward(ms: Long) { mediaPlayer.time = currentPosition + ms }
    override fun seekBack(ms: Long) { mediaPlayer.time = currentPosition - ms }

    // 🟢 3. 重构 LibVLC 4.0 轨道选择 API
    override fun selectVideoTrack(track: MzVideoTrack) {
        val trackId = track.rawData as? String ?: return
        mediaPlayer.selectTrack(trackId)
        updateTracks()
    }

    override fun selectAudioTrack(track: MzBasicTrack) {
        val trackId = track.rawData as? String ?: return
        if (trackId == "-1") {
            mediaPlayer.unselectTrackType(IMedia.Track.Type.Audio)
        } else {
            mediaPlayer.selectTrack(trackId)
        }
        updateTracks()
    }

    override fun selectSubtitleTrack(track: MzBasicTrack) {
        val trackId = track.rawData as? String ?: return
        if (trackId == "-1") {
            mediaPlayer.unselectTrackType(IMedia.Track.Type.Text)
        } else {
            mediaPlayer.selectTrack(trackId)
        }
        updateTracks()
    }

    override fun release() {
        mediaPlayer.stop()
        mediaPlayer.release()
        libVLC.release()

        SmbDataSource.releaseGlobalResources()
        FtpDataSource.releaseGlobalResources()
        WebDavDataSource.releaseGlobalResources()
    }

    override fun setPlaybackSpeed(speed: Float) {
        if (isPassthroughEnabled && speed != 1.0f) {
            return
        }
        _playbackSpeed.value = speed
        mediaPlayer.rate = speed
    }

    @Composable
    override fun PlayerView(modifier: Modifier) {
        AndroidView(
            factory = { ctx ->
                VLCVideoLayout(ctx).apply {
                    // 4.0 中 attachViews 会自动处理 SurfaceView/TextureView 的图层挂载
                    mediaPlayer.attachViews(this, null, true, false)
                    if (!mediaPlayer.isPlaying) {
                        mediaPlayer.play()
                    }
                }
            },
            modifier = modifier.fillMaxSize(),
            onRelease = {
                mediaPlayer.detachViews()
            }
        )
    }

    override fun addExternalSubtitles(subtitles: List<Pair<String, String>>) {
        subtitles.forEach { (uri, _) ->
            mediaPlayer.addSlave(IMedia.Slave.Type.Subtitle, uri.toUri(), true)
        }
        MzToastManager.show("加载外部字幕中...")
    }
}