package org.mz.mzdkplayer.ui.screen.vm // 请根据你的实际包名修改

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.mz.mzdkplayer.data.repository.FTPConnectionRepository // 引用 FTP 的 Repository
import org.mz.mzdkplayer.data.model.FTPConnection // 引用 FTP 的数据模型

/**
 * FTP连接vm,用来操作储存在本地的ftp连接数据
 */
class FTPListViewModel(application: Application) : AndroidViewModel(application) {

    // 使用 FTPConnectionRepository 替代 ConnectionRepository
    private val repository = FTPConnectionRepository(application)

    // 使用 FTPConnection 替代 SMBConnection
    private val _connections = MutableStateFlow<List<FTPConnection>>(emptyList())
    private val _isOPanelShow = MutableStateFlow<Boolean>(false)

    val connections: StateFlow<List<FTPConnection>> = _connections
    val isOPanelShow: StateFlow<Boolean> = _isOPanelShow

    // 当前选中的连接（用于文件列表）
    private val _selectedConnection = MutableStateFlow<FTPConnection?>(null)
    val selectedConnection: StateFlow<FTPConnection?> = _selectedConnection

    // 选中项索引
    private val _selectedIndex = MutableStateFlow(-1) // 初始无选中
    val selectedIndex = _selectedIndex.asStateFlow()

    // 选中项ID
    private val _selectedId = MutableStateFlow("") // 初始无选中
    val selectedId = _selectedId.asStateFlow()

    // 长按状态
    private val _isLongPressInProgress = MutableStateFlow(false)
    val isLongPressInProgress = _isLongPressInProgress.asStateFlow()

    init {
        loadConnections()
    }

    /**
     * 从 Repository 加载所有 FTP 连接
     */
    private fun loadConnections() {
        _connections.value = repository.getConnections()
    }

    /**
     * 添加一个新的 FTP 连接
     * @param connection 要添加的 FTPConnection 对象
     * @return 添加成功返回 true，如果存在重复连接则返回 false
     */
    fun addConnection(connection: FTPConnection): Boolean {
        if (!hasDuplicateConnection(_connections.value, connection)) {
            repository.addConnection(connection)
            loadConnections() // 重新加载列表
            return true
        } else {
            return false
        }
    }

    /**
     * 根据 ID 删除一个 FTP 连接
     * @param id 要删除的连接的 ID
     */
    fun deleteConnection(id: String) {
        repository.deleteConnection(id)
        loadConnections() // 重新加载列表
        // 可选：如果删除的是当前选中的连接，则清除选中状态
        if (_selectedConnection.value?.id == id) {
            _selectedConnection.value = null
        }
        // 可选：重置选中索引或ID
        // _selectedIndex.value = -1
        // _selectedId.value = ""
    }

    /**
     * 选中一个 FTP 连接
     * @param connection 要选中的 FTPConnection 对象
     */
    fun selectConnection(connection: FTPConnection) {
        _selectedConnection.value = connection
    }

    /**
     * 打开操作面板 (例如添加/编辑连接的对话框)
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
     * 设置当前选中的连接索引
     * @param index 选中的索引
     */
    fun setSelectedIndex(index: Int) {
        _selectedIndex.value = index
    }

    /**
     * 设置当前选中的连接 ID
     * @param id 选中的连接 ID
     */
    fun setSelectedId(id: String?) {
        if (id != null) {
            _selectedId.value = id
        }
    }

    /**
     * 设置长按状态
     * @param isLongPressInProgress 是否正在进行长按操作
     */
    fun setIsLongPressInProgress(isLongPressInProgress: Boolean) {
        _isLongPressInProgress.value = isLongPressInProgress
    }

    /**
     * 检查新连接是否与现有连接重复
     * 重复定义为：ID 相同，或者 IP、用户名、共享名、名称、密码全部相同
     * @param connections 现有的连接列表
     * @param newConnection 要检查的新连接
     * @return 如果存在重复则返回 true，否则返回 false
     */
    fun hasDuplicateConnection(connections: List<FTPConnection>, newConnection: FTPConnection): Boolean {
        return connections.any { existing ->
            existing.id == newConnection.id ||
                    (existing.ip == newConnection.ip &&
                            existing.username == newConnection.username &&
                            existing.shareName == newConnection.shareName && // 注意：FTPConnection 中是 shareName
                            existing.password == newConnection.password)
        }
    }
}



