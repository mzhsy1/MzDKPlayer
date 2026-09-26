package org.mz.mzdkplayer.data.repository

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import org.mz.mzdkplayer.tool.PlaybackPreference
import org.mz.mzdkplayer.tool.PlaybackPreferenceLogic

/**
 * 「按文件记住播放偏好」的落盘层。
 *
 * 每条记录以视频 URI 为键单独存一份纯文本，另外用一个索引键维护「最近使用顺序」，
 * 超过 [PlaybackPreferenceLogic.MAX_ENTRIES] 个文件时淘汰最久没用过的，避免无限膨胀。
 * 编解码与淘汰规则都在 [PlaybackPreferenceLogic] 里（纯 JDK，可单测），这里只管读写。
 *
 * 所有方法在未初始化或传入空 URI 时都安静返回，不影响播放。
 *
 * 注意：`init` 由 app 的 Application 调用，所以这里是 public（原先是 internal）。
 */
object PlaybackPreferenceRepository {

    private const val PREF_NAME = "playback_preferences"

    private var prefs: SharedPreferences? = null

    fun init(context: Context) {
        if (prefs != null) return
        prefs = context.applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        pruneIndex()
    }

    /** 读某个文件的偏好，没有记录或解析失败返回 null */
    fun load(videoUri: String): PlaybackPreference? {
        if (videoUri.isEmpty()) return null
        val store = prefs ?: return null
        return PlaybackPreferenceLogic.decode(
            store.getString(PlaybackPreferenceLogic.storageKey(videoUri), null)
        )
    }

    /**
     * 写入某个文件的偏好。四项全空时等价于「删掉这个文件的记录」，
     * 这样用户把设置改回默认后不会留下无用条目。
     */
    fun save(videoUri: String, preference: PlaybackPreference) {
        if (videoUri.isEmpty()) return
        val store = prefs ?: return

        val key = PlaybackPreferenceLogic.storageKey(videoUri)
        if (preference.isEmpty) {
            store.edit { remove(key) }
        } else {
            store.edit { putString(key, PlaybackPreferenceLogic.encode(preference)) }
        }

        val (kept, evicted) = PlaybackPreferenceLogic.touchIndex(readIndex(store), key)
        store.edit {
            putString(PlaybackPreferenceLogic.INDEX_KEY, kept.joinToString(INDEX_LINE_SEPARATOR))
            evicted.forEach { remove(it) }
        }
    }

    /** 清空所有文件的播放偏好（供「重置」类入口使用） */
    fun clear() {
        val store = prefs ?: return
        store.edit { clear() }
    }

    private fun readIndex(store: SharedPreferences): List<String> =
        store.getString(PlaybackPreferenceLogic.INDEX_KEY, null)
            ?.split(INDEX_LINE_SEPARATOR)
            ?.filter { it.isNotEmpty() }
            ?: emptyList()

    /**
     * 索引里可能残留已经被删掉的键（例如应用被强杀在写值之后），
     * 启动时顺手清一遍，保证顺序列表和实际记录一致。
     */
    private fun pruneIndex() {
        val store = prefs ?: return
        val alive = readIndex(store).filter { store.contains(it) }
        store.edit { putString(PlaybackPreferenceLogic.INDEX_KEY, alive.joinToString(INDEX_LINE_SEPARATOR)) }
    }

    private const val INDEX_LINE_SEPARATOR = "\n"
}
