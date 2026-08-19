package org.mz.mzdkplayer.ui.screen.vm

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.mz.mzdkplayer.data.repository.Resource
import org.mz.mzdkplayer.data.repository.SettingsRepository
import org.mz.mzdkplayer.tool.LanguageManager

// 简单的数据类用于 UI 状态
data class SettingsUiState(
    val hideDetails: Boolean = false,
    val hideNetworkSpeed: Boolean = true,
    val audioLang: String = "",
    val subLang: String = "",
    val enableTunneling: Boolean = false,
    val enablePassthrough: Boolean = false,
    val exoAudioDecodeMode: Int = 1, // 新增：音频解码模式
    val subFontSize: Float = 22f,
    val subColor: Long = 0xFFFFFFFF,
    val subBgColor: Long = 0x80000000,
    val subBottomPadding: Float = 30f,
    val subFontPath: String = "",
    val forcePgsCenter: Boolean = false,
    val autoLoadSubtitle: Boolean = true,
    val lockVideoRatio: Boolean = false,
    val globalVideoRatio: String = "FIT",
    val defaultPlayer: String = "exo",
    val isoPlaybackMode: Int = 0, // 新增：ISO 播放模式
    // 新增：遥控器上下键功能 (默认: 上=弹幕设置 D, 下=音轨选择 A)
    val dpadUpAction: String = "D",
    val dpadDownAction: String = "A",
    // 刮削
    val smb: Boolean = true,
    val webdav: Boolean = true,
    val ftp: Boolean = false,
    val nfs: Boolean = false,
    val local: Boolean = false,
    val http: Boolean = false,
    val appLang: String = "",
    val prioritizeLocalNfo: Boolean = false,
    val tmdbBaseUrl: String = SettingsRepository.DEFAULT_TMDB_URL,
    val recursiveScanLevel: Int = 1,
    val tmdbSearchLang: String = "",
    val tmdbResultLang: String = ""
)

class SettingsViewModel : ViewModel() {
    private val repo = SettingsRepository // 假设已在 App 启动时 init
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState = _uiState.asStateFlow()

    init {
        refreshState()
    }

    private fun refreshState() {
        _uiState.update {
            SettingsUiState(
                hideDetails = repo.hideDetails,
                hideNetworkSpeed = repo.hideNetworkSpeed,
                audioLang = repo.audioLanguage,
                subLang = repo.subtitleLanguage,
                enableTunneling = repo.enableTunneling,
                enablePassthrough = repo.enablePassthrough,
                exoAudioDecodeMode = repo.exoAudioDecodeMode, // 新增这一行
                subFontSize = repo.subtitleFontSize,
                subColor = repo.subtitleColorHex,
                subBgColor = repo.subtitleBgColorHex,
                subBottomPadding = repo.subtitleBottomPadding,
                subFontPath = repo.subFontPath,
                forcePgsCenter = repo.forcePgsCenter,
                autoLoadSubtitle = repo.autoLoadSubtitle,
                lockVideoRatio = repo.lockVideoRatio,
                globalVideoRatio = repo.globalVideoRatio,
                defaultPlayer = repo.defaultPlayer,
                isoPlaybackMode = repo.isoPlaybackMode,
                dpadUpAction = repo.dpadUpAction,
                dpadDownAction = repo.dpadDownAction,
                smb = repo.enableSmb,
                webdav = repo.enableWebDav,
                ftp = repo.enableFtp,
                nfs = repo.enableNfs,
                local = repo.enableLocal,
                http = repo.enableHttp,
                appLang = repo.appLanguage,
                prioritizeLocalNfo = repo.prioritizeLocalNfo,
                tmdbBaseUrl = repo.tmdbBaseUrl,
                recursiveScanLevel = repo.recursiveScanLevel,
                tmdbSearchLang = repo.tmdbSearchLang,
                tmdbResultLang = repo.tmdbResultLang
            )
        }
    }

    // 通用的更新方法
    fun toggleHideDetails(v: Boolean) { repo.hideDetails = v; refreshState() }
    fun toggleHideNetWorkSpeed(v: Boolean) { repo.hideNetworkSpeed = v; refreshState() }
    fun setAudioLanguage(v: String) { repo.audioLanguage = v; refreshState() }
    fun setSubLanguage(v: String) { repo.subtitleLanguage = v; refreshState() }
    fun toggleTunneling(v: Boolean) { repo.enableTunneling = v; refreshState() }
    fun togglePassthrough(v: Boolean) { repo.enablePassthrough = v; refreshState() }

    fun setSubFontSize(v: Float) { repo.subtitleFontSize = v; refreshState() }
    fun setSubColor(v: Long) { repo.subtitleColorHex = v; refreshState() }
    fun setSubBgColor(v: Long) { repo.subtitleBgColorHex = v; refreshState() }
    fun setSubBottomPadding(v: Float) { repo.subtitleBottomPadding = v; refreshState() }
    fun setSubFontPath(v: String) { repo.subFontPath = v; refreshState() }
    fun togglePgsCenter(v: Boolean) { repo.forcePgsCenter = v; refreshState() }
    fun toggleAutoLoadSubtitle(v: Boolean) { repo.autoLoadSubtitle = v; refreshState() }
    fun toggleLockVideoRatio(v: Boolean) { repo.lockVideoRatio = v; refreshState() }
    fun setGlobalVideoRatio(v: String) { repo.globalVideoRatio = v; refreshState() }
    fun setDefaultPlayer(kernel: String) {
        repo.defaultPlayer = kernel
        refreshState()
    }
    fun setIsoPlaybackMode(mode: Int) {
        repo.isoPlaybackMode = mode
        refreshState()
    }
    fun setDpadUpAction(v: String) { repo.dpadUpAction = v; refreshState() }
    fun setDpadDownAction(v: String) { repo.dpadDownAction = v; refreshState() }
    fun setAppLanguage(context: Context, lang: String) {
        repo.appLanguage = lang
        LanguageManager.setLanguage(context, lang)
        refreshState()
    }
    // 刮削开关
    fun toggleSource(source: String, v: Boolean) {
        when(source) {
            "SMB" -> repo.enableSmb = v
            "WebDav" -> repo.enableWebDav = v
            "FTP" -> repo.enableFtp = v
            "NFS" -> repo.enableNfs = v
            "Local" -> repo.enableLocal = v
            "HTTP" -> repo.enableHttp = v
        }
        refreshState()
    }
    fun setExoAudioDecodeMode(mode: Int) {
        repo.exoAudioDecodeMode = mode
        refreshState()
    }
    fun togglePrioritizeLocalNfo(v: Boolean) {
        repo.prioritizeLocalNfo = v
        refreshState()
    }

    private val _tmdbTestResult = MutableStateFlow<Resource<String>?>(null)
    val tmdbTestResult = _tmdbTestResult.asStateFlow()

    fun testTmdbConnection(url: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _tmdbTestResult.value = Resource.Loading
            try {
                // 使用临时 Retrofit 实例进行测试
                val testClient = okhttp3.OkHttpClient.Builder()
                    .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
                    .build()

                val retrofit = retrofit2.Retrofit.Builder()
                    .baseUrl(url)
                    .client(testClient)
                    .addConverterFactory(retrofit2.converter.gson.GsonConverterFactory.create())
                    .build()

                val service = retrofit.create(org.mz.mzdkplayer.data.api.TmdbApiService::class.java)
                val response = service.getPopularMovies(page = 1) // 这是一个简单的 GET 请求

                if (response.isSuccessful) {
                    _tmdbTestResult.value = Resource.Success("Success")
                } else {
                    _tmdbTestResult.value = Resource.Error("HTTP ${response.code()}")
                }
            } catch (e: Exception) {
                _tmdbTestResult.value = Resource.Error(e.message ?: "Unknown Error")
            }
        }
    }

    fun clearTmdbTestResult() {
        _tmdbTestResult.value = null
    }

    fun setTmdbBaseUrl(v: String) {
        repo.tmdbBaseUrl = v
        refreshState()
    }

    fun setRecursiveScanLevel(v: Int) {
        repo.recursiveScanLevel = v
        refreshState()
    }

    fun setTmdbSearchLang(v: String) {
        repo.tmdbSearchLang = v
        refreshState()
    }

    fun setTmdbResultLang(v: String) {
        repo.tmdbResultLang = v
        refreshState()
    }
}