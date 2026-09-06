package org.mz.mzdkplayer.player.vlc



import android.content.Context

import android.net.Uri

import android.util.Log

import org.mz.mzdkplayer.ui.screen.common.MzToastManager

import androidx.compose.foundation.layout.fillMaxSize

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

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

import org.mz.mzdkplayer.ui.screen.vm.SettingsViewModel



import org.mz.mzdkplayer.ui.screen.vm.VideoPlayerStatus

import org.videolan.libvlc.LibVLC

import org.videolan.libvlc.Media

import org.videolan.libvlc.MediaPlayer

import org.videolan.libvlc.interfaces.IMedia

import org.videolan.libvlc.util.VLCVideoLayout

import kotlin.collections.mapIndexed

import kotlin.time.Duration.Companion.milliseconds

import androidx.core.net.toUri
import org.mz.mzdkplayer.tool.Tools.prepareSubtitleFont


@UnstableApi

class MzVlcPlayer(

    private val context: Context,

    private val mediaUri: String,

    dataSourceType: String,

// 如果需要，可以把语言偏好也传进来

    settingsViewModel: SettingsViewModel

) : IMzPlayer {

    val isPassthroughEnabled = settingsViewModel.uiState.value.enablePassthrough // 获取当前的设置状态

    val preferredAudioLang: String = settingsViewModel.uiState.value.audioLang

    val preferredTextLang: String =settingsViewModel.uiState.value.subLang

    private val isNetworkProtocol: Boolean by lazy {

        val lower = mediaUri.lowercase()

        lower.startsWith("http://") || lower.startsWith("https://") ||

                lower.startsWith("ftp://") || lower.startsWith("smb://") ||

                lower.startsWith("nfs://") || lower.startsWith("rtsp://") ||

                lower.startsWith("rtmp://") ||

                !lower.startsWith("file:///") // 其他全部当网络处理

    }

// 1. 初始化 VLC 命令行参数

    private val options = arrayListOf(

        "-vvv",

        "--mediacodec-dr",

        "--vout=android_display", // 强制使用安卓原生显示输出，HDR 关键

        "--no-video-deco", // 禁用装饰，防止 GLES 混合导致 HDR 失效

        "--aout=audiotrack",

// 动态 caching（本地快，网络稳）

        "--file-caching=${if (!isNetworkProtocol) 500 else 1200}",

        "--network-caching=200",

        "--clock-jitter=0",



        "--clock-synchro=0",

        "--sub-autodetect-file",

        "--sub-autodetect-fuzzy=2" // 模糊匹配：1=始终匹配，2=匹配文件名（含后缀）

    ).apply {

// 如果开启了直通，在全局参数里也加上支持

        if (isPassthroughEnabled) {

            add("--spdif")



        }

//add(":no-bluray-menu")

// 1. 获取解压后的字体绝对路径

        // 1. 先把字体准备好，拿到文件对象
    //    val fontFile = prepareSubtitleFont(context, "SmileySans-Oblique.ttf")
//        Log.d("sds",fontFile.absolutePath)
//        if (fontFile.exists()) {
//
//            add("--freetype-font=monospace")
//        }
//// （可选）顺手把字幕设为黄色、加粗、带黑色描边，在电视上看着更清晰
//        add("--freetype-color=16776960") // 黄色
//        add("--freetype-bold")
//        add("--freetype-outline-color=0") // 黑色描边
//        add("--freetype-outline-thickness=4") // 描边粗细
// VLC 的语言设置通常在初始化时通过参数传入

        add("--audio-language=$preferredAudioLang")

        add("--sub-language=$preferredTextLang")



    }



    private val libVLC = LibVLC(context, options)

    private val mediaPlayer = MediaPlayer(libVLC)



    private var lastSubtitleCount = 0



// 2. 实现接口要求的状态流

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

// 1. 增加变量

    private val _isoTitles = MutableStateFlow<List<MzIsoTitle>>(emptyList())

    override val isoTitles: StateFlow<List<MzIsoTitle>> = _isoTitles.asStateFlow()



    private val _playbackSpeed = MutableStateFlow(1.0f)

    override val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()

    private val _aspectRatio = MutableStateFlow(
        if (settingsViewModel.uiState.value.lockVideoRatio) {
            runCatching { org.mz.mzdkplayer.player.core.MzAspectRatio.valueOf(settingsViewModel.uiState.value.globalVideoRatio) }
                .getOrDefault(org.mz.mzdkplayer.player.core.MzAspectRatio.FIT)
        } else {
            org.mz.mzdkplayer.player.core.MzAspectRatio.FIT
        }
    )
    override val aspectRatio: StateFlow<org.mz.mzdkplayer.player.core.MzAspectRatio> = _aspectRatio.asStateFlow()


// 3. 接口回调

    override var onError: ((String) -> Unit)? = null
    override var onPlaybackEnded: (() -> Unit)? = null
    override var onCuesChanged: ((Any) -> Unit)? = null // VLC 不需要，留空



    init {



        mediaPlayer.setAudioDigitalOutputEnabled(isPassthroughEnabled)

        Log.e("VLCPlayer", "isPassthroughEnabled → $isPassthroughEnabled")

// if (isPassthroughEnabled) {

        mediaPlayer.setAudioOutput("audiotrack")

// }

// 设置事件监听器同步状态

        Log.d("VLCPlayer", "mediaStr → $mediaUri")



// 🟢 针对 ISO 文件，如果是网络流且协议不支持，则使用本地 HTTP 代理
        val finalUrl = if (org.mz.mzdkplayer.tool.ProxyManager.shouldProxy(mediaUri, dataSourceType)) {
            val proxyUrl = org.mz.mzdkplayer.tool.ProxyManager.getProxyUrl(mediaUri)
            Log.d("MzVlcPlayer", "Using Proxy URL for ISO: $proxyUrl")
            proxyUrl
        } else {
            val encodedMrl = Tools.encodeUrlForPlayer(mediaUri)
            Log.d("MzVlcPlayer", "Final VLC MRL: $encodedMrl")
            encodedMrl
        }

        val media = Media(libVLC, finalUrl.toUri()).apply {
            setHWDecoderEnabled(true, true)
            // 🟢 处理 ISO 蓝光播放逻辑
            if (mediaUri.lowercase().endsWith(".iso") || mediaUri.lowercase().endsWith(".iso/")) {
                if (settingsViewModel.uiState.value.isoPlaybackMode == 1) {
                    addOption(":no-bluray-menu") // 直接播放正片
                } else {
                    addOption(":bluray-menu") // 显示菜单
                }
            }

//addOption(":codec=mediacodec_ndk")



// // 3. 只有开启直通时，才注入这些 Media Option

// if (isPassthroughEnabled) {

// addOption(":audio-passthrough=1")

// addOption(":spdif=hdmi")

// addOption(":audio-passthrough=hdmi")

//// addOption(":http-user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")

// // 注意：如果电视不支持 TrueHD，VLC 只要看到这几个参数就会尝试透传

// // 如果透传失败就会没声音。目前最稳妥是让用户在设置里切开关。

// } else {

// addOption(":audio-passthrough=0")

// }

//addOption(":demux=ts")

            addOption(":http-user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")

            addOption(":no-osd")

            addOption(":file-caching=${if (!isNetworkProtocol) 500 else 1200}") // 实验代码里有的，建议加上

// 🟢 针对 WebDAV/HTTP 的优化，允许连接复用和快速失败

            if (isNetworkProtocol) {

                addOption(":http-reconnect=1")

                addOption(":prefetch-buffer-size=1048576") // 限制预读探测大小为 1MB，防止探头过深

            }

        }

// 针对网络流优化





        mediaPlayer.media = media

// 3. ⭐️ 关键：在这里立即释放局部引用

        media.release()

        setupMediaParseListener()

        setupEventListener()



    }

// 在 MzVlcPlayer 类中添加成员

    private var _videoWidth = 1920

    private var _videoHeight = 1080



// 在 VLC 视频尺寸变化回调里更新（通常是 onNewVideoLayout 或 IVLCVout.Callback）

    override val videoWidth: Int get() = _videoWidth

    override val videoHeight: Int get() = _videoHeight

    private fun setupEventListener() {

        mediaPlayer.setEventListener { event ->

            when (event.type) {

                MediaPlayer.Event.Playing -> {

                    _isPlayingFlow.value = true

                    _playerStatus.value = VideoPlayerStatus.READY

// CoroutineScope(Dispatchers.Main).launch {

// delay(1000)

// updateTracks()

// }

                    updateTracks() // 播放开始后刷新轨道信息

                }

                MediaPlayer.Event.LengthChanged -> {

// 当 VLC 解析出总时长时会触发这里

                    Log.d("VLCPlayer", "检测到总时长变化: ${event.lengthChanged} ms")

// 这里可以触发 UI 刷新，确保进度条的总长不再是 0

                }

// ⭐️ 新增：监听 Title 改变（蓝光切换章节/正片最有效的信号）

// 当从菜单进入正片，Title 通常会从 0 变更为正片的索引

                MediaPlayer.Event.Paused -> {

                    _isPlayingFlow.value = false

                }

                MediaPlayer.Event.Stopped -> {

                    _isPlayingFlow.value = false

                    _playerStatus.value = VideoPlayerStatus.ENDED

                }

                MediaPlayer.Event.Buffering -> {

// VLC 的 buffering 状态包含百分比，缓冲到 100% 时切回 READY

                    if (event.buffering == 100f) {

                        _isPlayingFlow.value = true // 缓冲开始，视为停止播放

                        _playerStatus.value = VideoPlayerStatus.READY

                    } else {

                        _isPlayingFlow.value = false // 缓冲结束

                        _playerStatus.value = VideoPlayerStatus.BUFFERING

                    }

                }

                MediaPlayer.Event.EndReached -> {
                    onPlaybackEnded?.invoke()
                    _playerStatus.value = VideoPlayerStatus.ENDED
                }

                MediaPlayer.Event.EncounteredError -> {

                    onError?.invoke("VLC 播放出错")

                }

// 当轨道发生变化时（例如添加了外部字幕），刷新列表

                MediaPlayer.Event.ESAdded, MediaPlayer.Event.ESDeleted -> {

//Log.i("ESAdded", "字幕轨道变化 → 更新列表, 当前数量: ${mediaPlayer.media?.trackCount}")

// updateTracks()

//Toast.makeText(context,"添加成功，找到外部字幕", Toast.LENGTH_SHORT).show()

                    CoroutineScope(Dispatchers.Main).launch {

                        delay(1000.milliseconds)

// val media = mediaPlayer.media ?: return@launch

// val trackCount = media.trackCount

//

// val allTracks = (0 until trackCount).mapNotNull { media.getTrack(it) }

// val subtitleCount = allTracks.count { it.type == IMedia.Track.Type.Text }

//

// Log.i("SubtitleDebug", "字幕数量 = $subtitleCount")

//

// if (subtitleCount > 0) {

// Toast.makeText(context, "已加载外部字幕", Toast.LENGTH_SHORT).show()

// } else {

// Toast.makeText(context, "未找到可用字幕", Toast.LENGTH_SHORT).show()

// }



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

                Log.d("MzVlcPlayer", " Media 解析完成！协议: ${if(isNetworkProtocol) "网络(SMB/NFS/FTP)" else "本地 file:///"}")

                updateTracks()

            }

        }



// 立即启动解析（关键修复）

        if (!media.isParsed) {

            if (!media.isParsed) {

                if (!isNetworkProtocol) {

// 本地文件随便解析，速度极快

                    media.parseAsync(IMedia.Parse.ParseLocal)

                } else {

                    Log.d("MzVlcPlayer", " 网络流跳过预解析，交由 play() 自动接管")

                }

            }

        }

    }

    private fun updateTracks() {

        val media = mediaPlayer.media ?: return



// 1. 获取 Media 的元数据映射表（解析 ES 编码、语言等）

        val trackMetadataMap = (0 until media.trackCount)

            .mapNotNull { media.getTrack(it) }

            .associateBy { it.id }



// 2. 🎧 音频轨道

        val audioDescriptions = mediaPlayer.audioTracks ?: emptyArray()

        _audioTracks.value = audioDescriptions.mapIndexed { index, desc ->

            val meta = trackMetadataMap[desc.id] as? IMedia.AudioTrack

            val isDisableTrack = desc.id == -1

            val displayName = if (isDisableTrack) "关闭音频" else (desc.name ?: "音轨 $index")



            MzBasicTrack(

                id = desc.id.toString(),

                index = index,

                language = meta?.language ?: "",

                channelCount = meta?.channels ?: 0,

                mimeType = meta?.codec ?: "",

                sampleRate = meta?.rate ?: 0,

                bitrate = meta?.bitrate ?: 0,

                name = displayName,

                isSelected = desc.id == mediaPlayer.audioTrack,

                rawData = desc.id

            )

        }



// 3. 📝 字幕轨道

        val spuDescriptions = mediaPlayer.spuTracks ?: emptyArray()

        _subtitleTracks.value = spuDescriptions.mapIndexed { index, desc ->

            val meta = trackMetadataMap[desc.id] as? IMedia.SubtitleTrack

            val isDisableTrack = desc.id == -1

            val displayName = if (isDisableTrack) "关闭字幕" else (desc.name ?: "字幕 $index")



            MzBasicTrack(

                id = desc.id.toString(),

                index = index,

                language = meta?.language ?: "",

                mimeType = meta?.codec ?: "",

                name = displayName,

                isSelected = desc.id == mediaPlayer.spuTrack,

                rawData = desc.id

            )

        }



// 4. 📺 视频轨道

        val videoDescriptions = mediaPlayer.videoTracks ?: emptyArray()

        _videoTracks.value = videoDescriptions.mapIndexed { index, desc ->

            MzVideoTrack(

                id = desc.id.toString(),

                index = index,

                height = 0,

                bitrate = 0,

                codecs = "",

                isSelected = desc.id == mediaPlayer.videoTrack,

                rawData = desc.id

            )

        }



// 5. 💿 蓝光/DVD Titles (新增回位)

        val titles = mediaPlayer.titles

        if (titles != null && titles.isNotEmpty()) {

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

            _isoTitles.value = emptyList() // 非光盘介质清空列表

        }

    }



// 3. 实现接口方法

    override fun selectIsoTitle(index: Int) {

        mediaPlayer.title = index

        updateTracks() // 切换后刷新一下选中状态

    }



    override val isPlaying: Boolean get() = mediaPlayer.isPlaying

    override val currentPosition: Long get() = mediaPlayer.time

    override val duration: Long get() = mediaPlayer.length



    override fun play() { mediaPlayer.play() }

    override fun pause() { mediaPlayer.pause() }

    override fun seekTo(positionMs: Long) { mediaPlayer.time = positionMs }

    override fun seekForward(ms: Long) { mediaPlayer.time = currentPosition + ms }

    override fun seekBack(ms: Long) { mediaPlayer.time = currentPosition - ms }



    override fun selectVideoTrack(track: MzVideoTrack) {

        mediaPlayer.videoTrack = track.rawData as? Int ?: -1

    }



    override fun selectAudioTrack(track: MzBasicTrack) {

        mediaPlayer.audioTrack = track.rawData as? Int ?: -1

        updateTracks()

    }



    override fun selectSubtitleTrack(track: MzBasicTrack) {

        mediaPlayer.spuTrack = track.rawData as? Int ?: -1

        updateTracks()

    }





    override fun release() {

        mediaPlayer.stop()

        mediaPlayer.release()



        libVLC.release()



// 虽然 VLC 可能有自己的连接管理，但如果共用了 DataSources 或为了保险，统一清理

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

    override fun setAspectRatio(ratio: org.mz.mzdkplayer.player.core.MzAspectRatio) {
        _aspectRatio.value = ratio
    }

    @Composable
    override fun PlayerView(modifier: Modifier) {
        val currentRatio by aspectRatio.collectAsState()

        AndroidView(
            factory = { ctx ->
                VLCVideoLayout(ctx).apply {
                    mediaPlayer.attachViews(this, null, true, false)
                    if (!mediaPlayer.isPlaying) {
                        mediaPlayer.play()
                    }
                }
            },
            modifier = modifier.fillMaxSize(),
            update = { view ->
                when (currentRatio) {
                    org.mz.mzdkplayer.player.core.MzAspectRatio.FIT -> {
                        mediaPlayer.aspectRatio = null
                        mediaPlayer.scale = 0f
                    }
                    org.mz.mzdkplayer.player.core.MzAspectRatio.STRETCH -> {
                        mediaPlayer.aspectRatio = null
                        mediaPlayer.scale = 1f // Stretch to fill
                    }
                    org.mz.mzdkplayer.player.core.MzAspectRatio.RATIO_16_9 -> {
                        mediaPlayer.aspectRatio = "16:9"
                        mediaPlayer.scale = 0f
                    }
                    org.mz.mzdkplayer.player.core.MzAspectRatio.RATIO_4_3 -> {
                        mediaPlayer.aspectRatio = "4:3"
                        mediaPlayer.scale = 0f
                    }
                    org.mz.mzdkplayer.player.core.MzAspectRatio.ZOOM -> {
                        // VLC doesn't have a direct "ZOOM" that crops, but we can set aspect ratio and scale
                        mediaPlayer.aspectRatio = null
                        mediaPlayer.scale = 0f // This might need more specific logic
                    }
                }
            },
            onRelease = {
                mediaPlayer.detachViews()
            }
        )
    }



    override fun addExternalSubtitles(subtitles: List<Pair<String, String>>) {

        val media = mediaPlayer.media ?: return



        lastSubtitleCount = media.trackCount // ⭐️记录之前数量（或只数 Text 轨）

        subtitles.forEach { (uri, _) ->

// 对字幕路径也进行编码，防止包含空格或中文导致加载失败

            val encodedSubUri = Tools.encodeUrlForPlayer(uri)

// true 表示优先选中最后添加的那个

            mediaPlayer.addSlave(IMedia.Slave.Type.Subtitle, encodedSubUri.toUri(), true)



        }

        MzToastManager.show("加载外部字幕中...")



    }

}