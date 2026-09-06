package org.mz.mzdkplayer.data.repository


import android.content.Context
import android.content.SharedPreferences
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.edit

// 定义一个单例或者通过 Hilt 注入，这里用简单的单例模式
object SettingsRepository {
    private const val PREF_NAME = "app_settings"
    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    // --- 常规设置Keys ---
    private const val KEY_HIDE_DETAILS = "hide_details"

    private const val KEY_HIDE_NETWORK_SPEED = "hide_network_speed"

    // --- 播放/视频/音频 Keys ---
    private const val KEY_AUDIO_LANG = "audio_lang" // ""=Auto, "zh", "en"
    private const val KEY_SUB_LANG = "sub_lang"     // ""=Auto, "zh", "en"
    private const val KEY_VIDEO_TUNNELING = "video_tunneling" // 视频隧道模式(ExoPlayer TV常用)
    private const val KEY_AUDIO_PASSTHROUGH = "audio_passthrough" // 音频透传

    // --- 字幕设置 Keys ---
    private const val KEY_SUB_SIZE = "sub_size_sp"
    private const val KEY_SUB_COLOR = "sub_color_hex"
    private const val KEY_SUB_BG_COLOR = "sub_bg_color_hex" // 存 Hex 字符串
    private const val KEY_SUB_BOTTOM_PADDING = "sub_bottom_padding_dp"
    private const val KEY_SUB_PGS_CENTER = "sub_pgs_center"

    // 第三方自定义字幕字体文件绝对路径（仅对 CustomSubtitleView 纯文本字幕生效）
    private const val KEY_SUB_FONT_PATH = "sub_font_path"

    // 🔥 新增：自动加载同名字幕
    private const val KEY_AUTO_LOAD_SUBTITLE = "auto_load_subtitle"

    // 🔥 新增：默认播放器内核
    private const val KEY_DEFAULT_PLAYER = "default_player"

    // 🔥 新增：ISO 播放模式 (0: 默认/菜单, 1: 直接播放正片)
    private const val KEY_ISO_PLAYBACK_MODE = "iso_playback_mode"

    // 🔥 新增：遥控器上下键功能
    private const val KEY_DPAD_UP_ACTION = "dpad_up_action"
    private const val KEY_DPAD_DOWN_ACTION = "dpad_down_action"

    // 🔥 新增：快进快退时长 (秒)
    private const val KEY_FF_DURATION = "ff_duration"
    private const val KEY_RW_DURATION = "rw_duration"

    // 🔥 新增：视频播放完成动作 (0: 循环播放, 1: 播放暂停, 2: 播放下一个)
    private const val KEY_VIDEO_FINISH_ACTION = "video_finish_action"

    // --- 刮削设置 Keys ---
    private const val KEY_SOURCE_SMB = "source_smb"
    private const val KEY_SOURCE_WEBDAV = "source_webdav"
    private const val KEY_SOURCE_FTP = "source_ftp"
    private const val KEY_SOURCE_NFS = "source_nfs"
    private const val KEY_SOURCE_LOCAL = "source_local"
    private const val KEY_SOURCE_HTTP = "source_http"
    private const val KEY_APP_LANGUAGE = "app_language"

    // 🔥 新增：优先选择本地 nfo
    private const val KEY_PRIORITIZE_LOCAL_NFO = "prioritize_local_nfo"

    // 🔥 新增：TMDB API Base URL
    private const val KEY_TMDB_BASE_URL = "tmdb_base_url"
    const val DEFAULT_TMDB_URL = "https://api.themoviedb.org/3/"

    // 🔥 新增：Exo音频解码模式 (0=纯硬解, 1=硬解优先, 2=软解优先)
    private const val KEY_EXO_AUDIO_DECODE_MODE = "exo_audio_decode_mode"

    // 🔥 新增：锁定视频比例
    private const val KEY_LOCK_VIDEO_RATIO = "lock_video_ratio"
    private const val KEY_GLOBAL_VIDEO_RATIO = "global_video_ratio"

    // 🔥 新增：递归扫描层级 (0=当前文件夹, 1=当前+1层子文件夹, ...)
    private const val KEY_RECURSIVE_SCAN_LEVEL = "recursive_scan_level"

    private const val KEY_TMDB_SEARCH_LANG = "tmdb_search_lang"
    private const val KEY_TMDB_RESULT_LANG = "tmdb_result_lang"

    // 🔥 新增：是否移除 WebDAV 列表的首个元素
    private const val KEY_REMOVE_WEBDAV_FIRST_ITEM = "remove_webdav_first_item"

    // --- Getters & Setters ---

    // 常规
    var hideDetails: Boolean
        get() = prefs.getBoolean(KEY_HIDE_DETAILS, false)
        set(value) = prefs.edit { putBoolean(KEY_HIDE_DETAILS, value) }
    var hideNetworkSpeed: Boolean
        get() = prefs.getBoolean(KEY_HIDE_NETWORK_SPEED, true)
        set(value) = prefs.edit { putBoolean(KEY_HIDE_NETWORK_SPEED, value) }

    // 播放 - 语言
    var audioLanguage: String // "" = Auto
        get() = prefs.getString(KEY_AUDIO_LANG, "") ?: ""
        set(value) = prefs.edit { putString(KEY_AUDIO_LANG, value) }

    var subtitleLanguage: String
        get() = prefs.getString(KEY_SUB_LANG, "") ?: ""
        set(value) = prefs.edit { putString(KEY_SUB_LANG, value) }

    // 视频 (ExoPlayer 建议)
    var enableTunneling: Boolean
        get() = prefs.getBoolean(KEY_VIDEO_TUNNELING, false)
        set(value) = prefs.edit { putBoolean(KEY_VIDEO_TUNNELING, value) }

    // 音频 (ExoPlayer 建议)
    var enablePassthrough: Boolean
        get() = prefs.getBoolean(KEY_AUDIO_PASSTHROUGH, false)
        set(value) = prefs.edit { putBoolean(KEY_AUDIO_PASSTHROUGH, value) }
    var exoAudioDecodeMode: Int
        get() = prefs.getInt(KEY_EXO_AUDIO_DECODE_MODE, 1) // 默认 1 (硬解优先)
        set(value) = prefs.edit { putInt(KEY_EXO_AUDIO_DECODE_MODE, value) }

    var lockVideoRatio: Boolean
        get() = prefs.getBoolean(KEY_LOCK_VIDEO_RATIO, false)
        set(value) = prefs.edit { putBoolean(KEY_LOCK_VIDEO_RATIO, value) }

    var globalVideoRatio: String
        get() = prefs.getString(KEY_GLOBAL_VIDEO_RATIO, "FIT") ?: "FIT"
        set(value) = prefs.edit { putString(KEY_GLOBAL_VIDEO_RATIO, value) }

    // 字幕外观
    var subtitleFontSize: Float
        get() = prefs.getFloat(KEY_SUB_SIZE, 22f)
        set(value) = prefs.edit { putFloat(KEY_SUB_SIZE, value) }

    var subtitleColorHex: Long
        get() = prefs.getLong(KEY_SUB_COLOR, 0xFFFFFFFF) // White
        set(value) = prefs.edit { putLong(KEY_SUB_COLOR, value) }

    // 默认黑色 50%透明 (ARGB: 0x80000000)
    var subtitleBgColorHex: Long
        get() = prefs.getLong(KEY_SUB_BG_COLOR, 0x80000000)
        set(value) = prefs.edit { putLong(KEY_SUB_BG_COLOR, value) }

    var subtitleBottomPadding: Float
        get() = prefs.getFloat(KEY_SUB_BOTTOM_PADDING, 30f)
        set(value) = prefs.edit { putFloat(KEY_SUB_BOTTOM_PADDING, value) }

    var forcePgsCenter: Boolean
        get() = prefs.getBoolean(KEY_SUB_PGS_CENTER, false)
        set(value) = prefs.edit { putBoolean(KEY_SUB_PGS_CENTER, value) }

    var subFontPath: String
        get() = prefs.getString(KEY_SUB_FONT_PATH, "") ?: ""
        set(value) = prefs.edit { putString(KEY_SUB_FONT_PATH, value) }

    var autoLoadSubtitle: Boolean
        get() = prefs.getBoolean(KEY_AUTO_LOAD_SUBTITLE, true)
        set(value) = prefs.edit { putBoolean(KEY_AUTO_LOAD_SUBTITLE, value) }

    var defaultPlayer: String
        get() = prefs.getString(KEY_DEFAULT_PLAYER, "exo") ?: "exo"
        set(value) = prefs.edit { putString(KEY_DEFAULT_PLAYER, value) }

    var isoPlaybackMode: Int
        get() = prefs.getInt(KEY_ISO_PLAYBACK_MODE, 1) // 默认 1: 直接播放正片
        set(value) = prefs.edit { putInt(KEY_ISO_PLAYBACK_MODE, value) }

    // 🔥 新增：遥控器上下键功能 (默认: 上=弹幕设置, 下=音轨选择)
    var dpadUpAction: String
        get() = prefs.getString(KEY_DPAD_UP_ACTION, "D") ?: "D"
        set(value) = prefs.edit { putString(KEY_DPAD_UP_ACTION, value) }

    var dpadDownAction: String
        get() = prefs.getString(KEY_DPAD_DOWN_ACTION, "A") ?: "A"
        set(value) = prefs.edit { putString(KEY_DPAD_DOWN_ACTION, value) }

    var ffDuration: Int
        get() = prefs.getInt(KEY_FF_DURATION, 15)
        set(value) = prefs.edit { putInt(KEY_FF_DURATION, value) }

    var rwDuration: Int
        get() = prefs.getInt(KEY_RW_DURATION, 15)
        set(value) = prefs.edit { putInt(KEY_RW_DURATION, value) }

    var videoFinishAction: Int
        get() = prefs.getInt(KEY_VIDEO_FINISH_ACTION, 2) // 默认 2: 播放下一个
        set(value) = prefs.edit { putInt(KEY_VIDEO_FINISH_ACTION, value) }

    // 刮削源
    var enableSmb: Boolean get() = prefs.getBoolean(KEY_SOURCE_SMB, true); set(v) = prefs.edit {
        putBoolean(
            KEY_SOURCE_SMB,
            v
        )
    }
    var enableWebDav: Boolean get() = prefs.getBoolean(KEY_SOURCE_WEBDAV, true); set(v) = prefs.edit {
        putBoolean(
            KEY_SOURCE_WEBDAV,
            v
        )
    }
    var enableFtp: Boolean get() = prefs.getBoolean(KEY_SOURCE_FTP, false); set(v) = prefs.edit {
        putBoolean(
            KEY_SOURCE_FTP,
            v
        )
    }
    var enableNfs: Boolean get() = prefs.getBoolean(KEY_SOURCE_NFS, false); set(v) = prefs.edit {
        putBoolean(
            KEY_SOURCE_NFS,
            v
        )
    }
    var enableLocal: Boolean get() = prefs.getBoolean(KEY_SOURCE_LOCAL, false); set(v) = prefs.edit {
        putBoolean(
            KEY_SOURCE_LOCAL,
            v
        )
    }
    var enableHttp: Boolean get() = prefs.getBoolean(KEY_SOURCE_HTTP, false); set(v) = prefs.edit {
        putBoolean(
            KEY_SOURCE_HTTP,
            v
        )
    }
    var appLanguage: String
        get() = prefs.getString(KEY_APP_LANGUAGE, "") ?: "" // "" = 跟随系统
        set(value) = prefs.edit { putString(KEY_APP_LANGUAGE, value) }

    var prioritizeLocalNfo: Boolean
        get() = prefs.getBoolean(KEY_PRIORITIZE_LOCAL_NFO, false)
        set(value) = prefs.edit { putBoolean(KEY_PRIORITIZE_LOCAL_NFO, value) }

    var tmdbBaseUrl: String
        get() = prefs.getString(KEY_TMDB_BASE_URL, DEFAULT_TMDB_URL) ?: DEFAULT_TMDB_URL
        set(value) = prefs.edit { putString(KEY_TMDB_BASE_URL, value) }

    var recursiveScanLevel: Int
        get() = prefs.getInt(KEY_RECURSIVE_SCAN_LEVEL, 1)
        set(value) = prefs.edit { putInt(KEY_RECURSIVE_SCAN_LEVEL, value) }

    var tmdbSearchLang: String
        get() = prefs.getString(KEY_TMDB_SEARCH_LANG, "") ?: ""
        set(value) = prefs.edit { putString(KEY_TMDB_SEARCH_LANG, value) }

    var tmdbResultLang: String
        get() = prefs.getString(KEY_TMDB_RESULT_LANG, "") ?: ""
        set(value) = prefs.edit { putString(KEY_TMDB_RESULT_LANG, value) }

    var removeWebDavFirstItem: Boolean
        get() = prefs.getBoolean(KEY_REMOVE_WEBDAV_FIRST_ITEM, false)
        set(value) = prefs.edit { putBoolean(KEY_REMOVE_WEBDAV_FIRST_ITEM, value) }
}