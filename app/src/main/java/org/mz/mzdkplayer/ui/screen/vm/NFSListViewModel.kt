package org.mz.mzdkplayer.ui.screen.vm

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.mz.mzdkplayer.data.model.NFSConnection
import org.mz.mzdkplayer.data.repository.NFSConnectionRepository

/**
 * NFS 连接 ViewModel，用来操作储存在本地的 NFS 连接数据
 * @param application Application Context
 */
class NFSListViewModel(application: Application) : AndroidViewModel(application) {

    // 依赖注入或直接实例化 Repository
    private val repository = NFSConnectionRepository(application)

    // 管理连接列表
    private val _connections = MutableStateFlow<List<NFSConnection>>(emptyList())
    val connections: StateFlow<List<NFSConnection>> = _connections

    // 控制操作面板（如添加/编辑对话框）的显示
    private val _isOPanelShow = MutableStateFlow<Boolean>(false)
    val isOPanelShow: StateFlow<Boolean> = _isOPanelShow

    // 当前选中的连接（可用于连接或编辑）
    private val _selectedConnection = MutableStateFlow<NFSConnection?>(null)
    val selectedConnection: StateFlow<NFSConnection?> = _selectedConnection

    // 用于列表项选择状态 (可选，如果 UI 需要)
    private val _selectedIndex = MutableStateFlow(-1) // 初始无选中
    val selectedIndex = _selectedIndex.asStateFlow()

    private val _selectedId = MutableStateFlow("") // 初始无选中
    val selectedId = _selectedId.asStateFlow()

    // 用于长按选择模式 (可选，如果 UI 需要)
    private val _isLongPressInProgress = MutableStateFlow(false)
    val isLongPressInProgress = _isLongPressInProgress.asStateFlow()

    init {
        loadConnections()
    }

    /**
     * 从 Repository 加载所有连接到 StateFlow
     */
    private fun loadConnections() {
        _connections.value = repository.getConnections()
    }

    /**
     * 添加一个新的 NFS 连接
     * @param connection 要添加的连接对象
     * @return Boolean 添加是否成功 (例如，检查重复)
     */
    fun addConnection(connection: NFSConnection): Boolean {
        // 检查是否为重复连接（基于 ID 或所有字段）
        if (!hasDuplicateConnection(_connections.value, connection)) {
            repository.addConnection(connection)
            loadConnections() // 重新加载列表
            return true
        } else {
            return false // 或者抛出异常，取决于业务逻辑
        }
    }

    /**
     * 更新一个已存在的 NFS 连接
     * @param connection 更新后的连接对象 (必须包含有效的 ID)
     */
    fun updateConnection(connection: NFSConnection) {
        repository.updateConnection(connection)
        loadConnections() // 重新加载列表
    }

    /**
     * 根据 ID 删除一个 NFS 连接
     * @param id 要删除的连接的 ID
     */
    fun deleteConnection(id: String) {
        repository.deleteConnection(id)
        loadConnections() // 重新加载列表
        // 如果删除的是当前选中的连接，则取消选中
        if (_selectedConnection.value?.id == id) {
            _selectedConnection.value = null
        }
        // 如果删除的是选中的 ID，则重置
        if (_selectedId.value == id) {
            _selectedId.value = ""
            _selectedIndex.value = -1
        }
    }

    /**
     * 选中一个连接（例如，用于连接或编辑）
     * @param connection 要选中的连接对象
     */
    fun selectConnection(connection: NFSConnection) {
        _selectedConnection.value = connection
    }

    /**
     * 打开操作面板
     */
    fun openOPlane() {
        _isOPanelShow.value = true
    }

    /**
     * 关闭操作面板
     */
    fun closeOPanel() {
        _isOPanelShow.value = false
    }

    /**
     * 设置选中项的索引
     * @param index 选中项的索引
     */
    fun setSelectedIndex(index: Int) {
        _selectedIndex.value = index
    }

    /**
     * 设置选中项的 ID
     * @param id 选中项的 ID
     */
    fun setSelectedId(id: String?) {
        if (id != null) {
            _selectedId.value = id
        }
    }

    /**
     * 设置长按选择模式状态
     * @param isLongPressInProgress 是否处于长按选择模式
     */
    fun setIsLongPressInProgress(isLongPressInProgress: Boolean) {
        _isLongPressInProgress.value = isLongPressInProgress
    }

    /**
     * 检查新连接是否与现有连接重复
     * @param connections 现有连接列表
     * @param newConnection 新连接
     * @return Boolean 是否重复
     */
    fun hasDuplicateConnection(connections: List<NFSConnection>, newConnection: NFSConnection): Boolean {
        return connections.any { existing ->
            // 基于 ID 检查重复
            existing.id == newConnection.id ||
                    // 或者基于关键字段检查逻辑重复 (可根据需求调整)
                    (existing.serverAddress == newConnection.serverAddress )
            // 注意：比较密码可能不合适
        }
    }

    /**
     * 根据 ID 查找连接
     * @param id 连接 ID
     * @return 找到的连接，如果未找到则返回 null
     */
    fun getConnectionById(id: String): NFSConnection? {
        return repository.getConnectionById(id)
    }
}



