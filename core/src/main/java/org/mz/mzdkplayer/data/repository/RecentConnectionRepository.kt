package org.mz.mzdkplayer.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.core.content.edit
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.mz.mzdkplayer.data.model.RecentConnection
import org.mz.mzdkplayer.tool.logic.RecentConnectionLogic

/**
 * 「最近访问的连接」存储层（SharedPreferences + Gson）。
 *
 * 为什么做成全局单例（而不是像 `SMBConnectionRepository` 那样每次 new 一个）：
 * 写入方是五个协议各自的连接列表页，读取方是主页 —— 两边如果各自持有实例，
 * 主页就看不到刚记录的条目。这里用 [recent] 这个 StateFlow 当唯一数据源，
 * 主页 `collectAsState()` 之后返回首页即是最新状态，不用再加刷新回调。
 *
 * 去重 / 排序 / 截断全在 [RecentConnectionLogic] 里，本类只管读写与状态广播。
 */
object RecentConnectionRepository {

    private const val PREFS_NAME = "recent_connections_prefs"
    private const val KEY_RECENT = "recent_connections"

    private val gson = Gson()
    private val listType = object : TypeToken<List<RecentConnection>>() {}.type

    private var prefs: SharedPreferences? = null

    private val _recent = MutableStateFlow<List<RecentConnection>>(emptyList())
    val recent: StateFlow<List<RecentConnection>> = _recent.asStateFlow()

    fun init(context: Context) {
        if (prefs != null) return
        val sp = context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs = sp
        _recent.value = RecentConnectionLogic.sanitize(readFromDisk(sp))
    }

    /**
     * 记录一次「打开某个连接」。
     *
     * @param route 该协议文件列表页的完整路由（含已编码的参数），点击主页卡片时直接导航
     */
    fun record(type: String, name: String, route: String) {
        val incoming = RecentConnection(
            type = type,
            name = name,
            route = route,
            lastVisited = System.currentTimeMillis()
        )
        val merged = RecentConnectionLogic.merge(_recent.value, incoming)
        _recent.value = merged
        writeToDisk(merged)
    }

    /** 删掉某个连接对应的最近访问记录（连接被删除时调用） */
    fun remove(type: String, name: String) {
        val remain = RecentConnectionLogic.remove(_recent.value, type, name)
        if (remain.size == _recent.value.size) return
        _recent.value = remain
        writeToDisk(remain)
    }

    fun clear() {
        _recent.value = emptyList()
        prefs?.edit { remove(KEY_RECENT) }
    }

    private fun readFromDisk(sp: SharedPreferences): List<RecentConnection> {
        val json = sp.getString(KEY_RECENT, null) ?: return emptyList()
        return try {
            gson.fromJson<List<RecentConnection>>(json, listType) ?: emptyList()
        } catch (e: Exception) {
            Log.e("RecentConnectionRepo", "解析最近访问连接失败", e)
            emptyList()
        }
    }

    private fun writeToDisk(list: List<RecentConnection>) {
        prefs?.edit { putString(KEY_RECENT, gson.toJson(list)) }
    }
}
