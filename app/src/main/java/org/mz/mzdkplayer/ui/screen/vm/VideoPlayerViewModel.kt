package org.mz.mzdkplayer.ui.screen.vm

import android.view.View
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import androidx.media3.common.Tracks
import com.kuaishou.akdanmaku.DanmakuConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch


class VideoPlayerViewModel:ViewModel() {



    // 播放状态（使用 StateFlow，推荐用于 Compose）
    private val _playerStatus = MutableStateFlow<VideoPlayerStatus>(VideoPlayerStatus.IDLE)
    val playerStatus: StateFlow<VideoPlayerStatus> = _playerStatus.asStateFlow()

    val mutableSetOfAudioTrackGroups = mutableListOf<Tracks.Group>()
    val mutableSetOfVideoTrackGroups = mutableListOf<Tracks.Group>()

    val mutableSetOfTextTrackGroups = mutableListOf<Tracks.Group>()
    var selectedAtIndex by  mutableIntStateOf(0)
    var selectedVtIndex by mutableIntStateOf(0)

    var selectedStIndex by mutableIntStateOf(0)
    var onTracksChangedState by  mutableIntStateOf(0)

    var isSubtitleViewVis by mutableIntStateOf(View.VISIBLE)

    var isCusSubtitleViewVis by mutableStateOf(false)

    var isSubtitlePanelVis by mutableStateOf("S")

    var selectedAorVorS by mutableStateOf("A")

    var atpVisibility by mutableStateOf(false)
    var atpFocus by mutableStateOf(false)

    // 弹幕配置相关
    var danmakuConfig by mutableStateOf(DanmakuConfig())
    var danmakuVisibility by mutableStateOf(true)

    fun updateSubtitleVisibility(visible: Int) {
        isSubtitleViewVis = visible
    }
    fun updateCusSubtitleVisibility(visible: Boolean) {
        isCusSubtitleViewVis = visible
    }
    // 公共方法：供外部（如 Player.Listener）更新状态
    fun updatePlayerStatus(status: VideoPlayerStatus) {
        viewModelScope.launch {
            _playerStatus.value = status
        }
    }
    fun setPlayerError(message: String) = updatePlayerStatus(VideoPlayerStatus.Error(message))

    // 更新弹幕配置的方法
    fun updateDanmakuConfig(config: DanmakuConfig) {
        danmakuConfig = config
    }

    // 更新弹幕可见性的方法
    fun updateDanmakuVisibility(visibility: Boolean) {
        danmakuVisibility = visibility
    }
}
// 播放状态密封类（你已定义，稍作补充）
sealed class VideoPlayerStatus {
    object IDLE : VideoPlayerStatus()
    object BUFFERING : VideoPlayerStatus()
    object READY : VideoPlayerStatus() // 建议加上 READY
    object ENDED : VideoPlayerStatus() // 建议加上 ENDED
    data class Error(val message: String) : VideoPlayerStatus()

    // 用于 UI 显示的描述（可选）
    override fun toString(): String {
        return when (this) {
            IDLE -> "正在初始化"
            BUFFERING -> "正在缓冲..."
            READY -> "播放中"
            ENDED -> "播放结束"
            is Error -> "播放出错: $message"
        }
    }
}



