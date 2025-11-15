package org.mz.mzdkplayer.ui.screen.vm

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

import org.mz.mzdkplayer.data.model.SMBConnection
import org.mz.mzdkplayer.data.repository.SMBConnectionRepository

/**
 * SMB连接vm,用来操作储存在本地的smb连接数据
 */
class SMBListViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = SMBConnectionRepository(application)
    private val _connections = MutableStateFlow<List<SMBConnection>>(emptyList())
    private val _isOPanelShow = MutableStateFlow<Boolean>(false)
    val connections: StateFlow<List<SMBConnection>> = _connections

    val isOPanelShow: StateFlow<Boolean> = _isOPanelShow

    // 当前选中的连接（用于文件列表）
    private val _selectedConnection = MutableStateFlow<SMBConnection?>(null)
    val selectedConnection: StateFlow<SMBConnection?> = _selectedConnection

    init {
        loadConnections()
    }

    /**
     * load
     */
    private fun loadConnections() {
        _connections.value = repository.getConnections()
    }



    /**
     * add
     */
    fun addConnection(connection: SMBConnection): Boolean{
        if (!hasDuplicateConnection(_connections.value,connection)){
            repository.addConnection(connection)
            loadConnections()
            return true
        }else{
            return false
        }
    }

    /**
     * delete
     */
    fun deleteConnection(id: String) {
        repository.deleteConnection(id)
        loadConnections()
    }

    /**
     * select
     */

    fun selectConnection(connection: SMBConnection) {
        _selectedConnection.value = connection
    }

    fun openOPlane(){
        _isOPanelShow.value = true
    }
    fun closeOPanel(){
        _isOPanelShow.value = false
    }

    private val _selectedIndex = MutableStateFlow(-1) // 初始无选中
    val selectedIndex = _selectedIndex.asStateFlow()

    fun setSelectedIndex(index: Int) {
        _selectedIndex.value = index
    }

    private val _selectedId = MutableStateFlow("") // 初始无选中
    val selectedId = _selectedId.asStateFlow()

    fun setSelectedId(id: String?) {
        if (id != null) {
            _selectedId.value = id
        }
    }

    private val _isLongPressInProgress = MutableStateFlow(false) // 初始无选中
    val isLongPressInProgress = _isLongPressInProgress.asStateFlow()

    fun setIsLongPressInProgress(isLongPressInProgress: Boolean) {
        _isLongPressInProgress.value = isLongPressInProgress
    }
    fun hasDuplicateConnection(connections: List<SMBConnection>, newConnection: SMBConnection): Boolean {
        return connections.any { existing ->
            existing.id == newConnection.id ||
                    (existing.ip == newConnection.ip &&
                            existing.username == newConnection.username &&
                            existing.shareName == newConnection.shareName &&
                            existing.password == newConnection.password)
        }
    }
}