package org.mz.mzdkplayer.data.repository

import android.content.Context
import android.util.Log
import androidx.core.content.edit
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.mz.mzdkplayer.data.model.SMBConnection

class SMBConnectionRepository(private val context: Context) {
    private val prefs = context.getSharedPreferences("smb_connections_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    // 保存所有连接
    fun saveConnections(connections: List<SMBConnection>) {
        prefs.edit { putString("connections", gson.toJson(connections)) }
    }

    // 获取所有连接
    fun getConnections(): List<SMBConnection> {
        return try {
            val json = prefs.getString("connections", null)
            if (!json.isNullOrEmpty()) {
                val loadedConnections = gson.fromJson<List<SMBConnection>>(json, object : TypeToken<List<SMBConnection>>() {}.type)

                // 对加载的数据进行空值保护处理
                loadedConnections.map { connection ->
                    SMBConnection(
                        id = connection.id,
                        name = connection.name ?: "未命名连接",
                        ip = connection.ip ?: "未知IP",
                        username = connection.username ?: "未知用户",
                        password = connection.password ?: "",
                        shareName = connection.shareName ?: "未知路径"
                    )
                }
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            Log.e("SMBConnectionRepository", "解析连接列表失败", e)
            emptyList()
        }
    }

    // 添加新连接
    fun addConnection(connection: SMBConnection) {
        val current = getConnections().toMutableList()
        // 确保新连接的字段不是 null
        val safeConnection = SMBConnection(
            id = connection.id,
            name = connection.name ?: "未命名连接",
            ip = connection.ip ?: "未知IP",
            username = connection.username ?: "未知用户",
            password = connection.password ?: "",
            shareName = connection.shareName ?: "未知路径"
        )
        current.add(safeConnection)
        saveConnections(current)
    }

    // 删除连接
    fun deleteConnection(id: String) {
        saveConnections(getConnections().filter { it.id != id })
    }
}