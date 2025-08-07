package org.mz.mzdkplayer.ui.screen.vm

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.mz.mzdkplayer.logic.model.ConnectionRepository
import org.mz.mzdkplayer.logic.model.SMBConnection

/**
 * SMB连接vm,用来操作储存在本地的smb连接数据
 */
class SMBListViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = ConnectionRepository(application)
    private val _connections = MutableStateFlow<List<SMBConnection>>(emptyList())
    val connections: StateFlow<List<SMBConnection>> = _connections

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
    fun addConnection(connection: SMBConnection) {
        repository.addConnection(connection)
        loadConnections()
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
}