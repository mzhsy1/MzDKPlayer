package org.mz.mzdkplayer.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.core.content.edit
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.mz.mzdkplayer.data.model.HTTPLinkConnection

/**
 * 管理 HTTPLinkConnection 对象的本地存储 (使用 SharedPreferences)
 * @param context Application 或 Activity Context
 */
class HTTPLinkConnectionRepository(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("http_link_connections_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val connectionListType = object : TypeToken<List<HTTPLinkConnection>>() {}.type

    companion object {
        private const val KEY_CONNECTIONS = "connections"
    }

    /**
     * 保存所有连接列表到 SharedPreferences
     * @param connections 连接列表
     */
    fun saveConnections(connections: List<HTTPLinkConnection>) {
        prefs.edit {
            putString(KEY_CONNECTIONS, gson.toJson(connections))
        }
    }

    /**
     * 从 SharedPreferences 获取所有连接列表
     * @return 连接列表，如果不存在则返回空列表
     */
    fun getConnections(): List<HTTPLinkConnection> {
        val json = prefs.getString(KEY_CONNECTIONS, null)
        return if (!json.isNullOrEmpty()) {
            try {
                val loadedConnections = gson.fromJson<List<HTTPLinkConnection>>(json, connectionListType)

                // 对加载的数据进行空值保护处理，确保不会返回包含 null 字段的对象
                loadedConnections.map { connection ->
                    HTTPLinkConnection(
                        id = connection.id,
                        name = connection.name ?: "未命名连接",
                        serverAddress = connection.serverAddress ?: "未知地址",
                        shareName = connection.shareName ?: "未知路径"
                    )
                }
            } catch (e: Exception) {
                // 如果 JSON 解析失败（例如数据损坏），返回空列表
                Log.e("HTTPLinkRepo", "解析连接列表失败", e)
                emptyList()
            }
        } else {
            emptyList()
        }
    }

    /**
     * 添加一个新的 HTTP Link 连接
     * @param connection 要添加的连接对象
     */
    fun addConnection(connection: HTTPLinkConnection) {
        val currentConnections = getConnections().toMutableList()
        // 确保新连接的字段不是 null（使用默认值）
        val safeConnection = HTTPLinkConnection(
            id = connection.id,
            name = connection.name ?: "未命名连接",
            serverAddress = connection.serverAddress ?: "未知地址",
            shareName = connection.shareName ?: "未知路径"
        )

        // 检查 ID 是否已存在（理论上 UUID 不会冲突，但作为预防）
        val existingIndex = currentConnections.indexOfFirst { it.id == safeConnection.id }
        if (existingIndex >= 0) {
            // 如果 ID 存在，替换旧的连接
            currentConnections[existingIndex] = safeConnection
        } else {
            // 如果 ID 不存在，添加新的连接
            currentConnections.add(safeConnection)
        }
        saveConnections(currentConnections)
    }

    /**
     * 根据 ID 删除一个 HTTP Link 连接
     * @param id 要删除的连接的 ID
     */
    fun deleteConnection(id: String) {
        val updatedConnections = getConnections().filter { it.id != id }
        saveConnections(updatedConnections)
    }

    /**
     * 根据 ID 查找一个 HTTP Link 连接
     * @param id 要查找的连接的 ID
     * @return 找到的连接对象，如果未找到则返回 null
     */
    fun getConnectionById(id: String): HTTPLinkConnection? {
        return getConnections().find { it.id == id }
    }

    /**
     * 更新一个已存在的 HTTP Link 连接
     * @param connection 更新后的连接对象 (必须包含有效的 ID)
     */
    fun updateConnection(connection: HTTPLinkConnection) {
        val currentConnections = getConnections().toMutableList()
        // 确保更新的连接字段不是 null
        val safeConnection = HTTPLinkConnection(
            id = connection.id,
            name = connection.name ?: "未命名连接",
            serverAddress = connection.serverAddress ?: "未知地址",
            shareName = connection.shareName ?: "未知路径"
        )

        val index = currentConnections.indexOfFirst { it.id == safeConnection.id }
        if (index >= 0) {
            currentConnections[index] = safeConnection
            saveConnections(currentConnections)
        } else {
            Log.w("HTTPLinkRepo", "尝试更新一个不存在的连接 ID: ${safeConnection.id}")
            // 或者可以选择添加它？取决于业务逻辑
            // addConnection(safeConnection)
        }
    }
}





