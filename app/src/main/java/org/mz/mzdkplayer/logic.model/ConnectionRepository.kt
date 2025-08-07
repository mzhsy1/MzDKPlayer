package org.mz.mzdkplayer.logic.model

import android.content.Context
import androidx.core.content.edit
import com.google.common.reflect.TypeToken
import com.google.gson.Gson

class ConnectionRepository(private val context: Context) {
    private val prefs = context.getSharedPreferences("smb_connections", Context.MODE_PRIVATE)
    private val gson = Gson()

    // 保存所有连接
    fun saveConnections(connections: List<SMBConnection>) {
        prefs.edit { putString("connections", gson.toJson(connections)) }
    }

    // 获取所有连接
    fun getConnections(): List<SMBConnection> {
        return prefs.getString("connections", null)?.let {
            gson.fromJson(it, object : TypeToken<List<SMBConnection>>() {}.type)
        } ?: emptyList()
    }

    // 添加新连接
    fun addConnection(connection: SMBConnection) {
        val current = getConnections().toMutableList()
        current.add(connection)
        saveConnections(current)
    }

    // 删除连接
    fun deleteConnection(id: String) {
        saveConnections(getConnections().filter { it.id != id })
    }
}