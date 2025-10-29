package org.mz.mzdkplayer.ui.screen.vm

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.emc.ecs.nfsclient.nfs.io.Nfs3File
import com.emc.ecs.nfsclient.nfs.nfs3.Nfs3

import com.emc.ecs.nfsclient.rpc.CredentialUnix
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.mz.mzdkplayer.logic.model.NFSConnection
import java.io.IOException

/**
 * ViewModel 用于管理 NFS 连接状态和操作。
 * 使用 com.emc.ecs:nfs-client 库进行真实的 NFS 交互。
 */
class NFSConViewModel(application: Application) : AndroidViewModel(application) {

    private val appContext = application

    // 明确指定 MutableStateFlow 的泛型类型
    private val _connectionStatus: MutableStateFlow<NFSConnectionStatus> = MutableStateFlow(NFSConnectionStatus.Disconnected)
    val connectionStatus: StateFlow<NFSConnectionStatus> = _connectionStatus

    // 存储 NFS 文件/文件夹列表 (使用 NfsFile)
    private val _fileList: MutableStateFlow<List<Nfs3File>> = MutableStateFlow(emptyList())
    val fileList: StateFlow<List<Nfs3File>> = _fileList

    // 存储当前工作目录的路径 (相对于 NFS 根目录)
    private val _currentPath: MutableStateFlow<String> = MutableStateFlow("")
    val currentPath: StateFlow<String> = _currentPath

    // 存储当前的 NFS 客户端实例
    private var nfsClient: Nfs3? = null

    private val mutex = Mutex() // 用于协程同步

    /**
     * 尝试连接到 NFS 服务器
     * 使用 com.emc.ecs.nfsclient.nfs.nfs3.Nfs3 进行实际连接。
     *
     * @param connection NFS 连接配置对象
     */
    fun connectToNFS(connection: NFSConnection) {
        viewModelScope.launch {
            mutex.withLock {
                _connectionStatus.value = NFSConnectionStatus.Connecting
                try {
                    withContext(Dispatchers.IO) {
                        // 验证配置
                        if (connection.serverAddress?.isBlank() == true || connection.serverAddress?.isBlank() == true) {
                            throw IllegalArgumentException("服务器地址或导出路径不能为空")
                        }
                        // 准备认证信息 (使用 UID/GID 或 CredentialNone)
                        val credential = CredentialUnix(

                        )
                        Log.d("connection.se","$connection")
                        // 使用 Nfs3 构造函数进行连接和挂载
                        // 这会自动处理 RPC MOUNT 协议交互
                        val client = Nfs3(
                            connection.serverAddress,
                            connection.shareName,
                            credential,
                            3 // 最大重试次数
                        )
                        // 测试连接是否成功 (尝试访问根目录)
                        val rootDir = Nfs3File(client, "/")
                        if (!rootDir.exists()) {
                            throw IOException("无法访问 NFS 根目录")
                        }

                        // 连接成功，保存客户端实例和连接配置
                        nfsClient = client
                        // 初始列出根目录内容
                        listFilesInternal("/")
                    }
                    _currentPath.value = "" // 重置路径为根目录
                    _connectionStatus.value = NFSConnectionStatus.Connected
                    Log.d("NfsConViewModel", "连接成功到 ${connection.name} (${connection.serverAddress}:${connection.shareName})")
                } catch (e: IOException) {
                    Log.e("NfsConViewModel", "NFS IO 连接失败", e)
                    _connectionStatus.value = NFSConnectionStatus.Error("IO 错误: ${e.message ?: "网络或挂载失败"}")
                    _fileList.value = emptyList()
                } catch (e: Exception) {
                    Log.e("NfsConViewModel", "NFS 连接失败", e)
                    _connectionStatus.value = NFSConnectionStatus.Error("连接失败: ${e.message ?: "未知错误"}")
                    _fileList.value = emptyList()
                }
            }
        }
    }

    /**
     * 列出指定 NFS 路径下的文件和文件夹
     * @param path 相对于 NFS 根目录的路径 (例如 "/home/user" 或 "")
     */
    fun listFiles(path: String = "") {
        viewModelScope.launch {
            if (_connectionStatus.value != NFSConnectionStatus.Connected) {
                _connectionStatus.value = NFSConnectionStatus.Error("未连接到服务器")
                return@launch
            }

            mutex.withLock {
                try {
                    withContext(Dispatchers.IO) {
                        listFilesInternal(path)
                    }
                    _currentPath.value = path // 更新当前路径状态
                    Log.d("NfsConViewModel", "列出文件成功: $path")
                } catch (e: Exception) {
                    Log.e("NfsConViewModel", "获取文件列表失败: $path", e)
                    _connectionStatus.value = NFSConnectionStatus.Error("获取文件失败: ${e.message}")
                    _fileList.value = emptyList()
                }
            }
        }
    }

    /**
     * 在 IO 线程中执行实际的 NFS 文件列表获取
     * @param path 相对于 NFS 根目录的路径
     */
    private fun listFilesInternal(path: String) {
        val client = nfsClient ?: throw IllegalStateException("NFS 客户端未初始化")
        val dir = Nfs3File(client, path)
        if (!dir.exists() || !dir.isDirectory) {
            throw IOException("路径不存在或不是目录: $path")
        }
        val files = dir.listFiles() // 返回 Array<Nfs3File?>
        _fileList.value = files?.filterNotNull()?.map { it }?.toList() ?: emptyList()
    }

    /**
     * 断开与 NFS 服务器的连接
     */
    fun disconnectNfs() {
        viewModelScope.launch(Dispatchers.IO) {
            mutex.withLock {
                try {
                    // Nfs3 客户端本身没有显式的 "close" 或 "unmount" 方法
                    // 通常，当 Nfs3 实例不再被引用时，其内部资源会被垃圾回收
                    // 但为了明确状态，我们可以将其置为 null
                    nfsClient = null
                    Log.d("NfsConViewModel", "NFS 连接已断开")
                } catch (e: Exception) {
                    Log.w("NfsConViewModel", "断开连接时发生异常", e)
                } finally {
                    // 确保在 Main 线程更新状态
                    withContext(Dispatchers.Main) {
                        _connectionStatus.value = NFSConnectionStatus.Disconnected
                        _fileList.value = emptyList() // 清空列表
                        _currentPath.value = "" // 重置路径
                    }
                }
            }
        }
    }

    /**
     * 检查当前是否已连接
     */
    fun isConnected(): Boolean {
        return _connectionStatus.value == NFSConnectionStatus.Connected
    }

    /**
     * 获取父目录路径
     * @param currentPath 当前相对路径
     */
    fun getParentPath(currentPath: String): String {
        if (currentPath.isEmpty() || currentPath == "/") {
            return "" // 已经在根目录
        }
        // 规范化路径：确保以 '/' 开头，不以 '/' 结尾 (除非是根目录)
        var normalizedPath = if (currentPath.startsWith("/")) currentPath else "/$currentPath"
        normalizedPath = normalizedPath.trimEnd('/')

        // 找到最后一个 '/' 并截取前面的部分
        val lastSlashIndex = normalizedPath.lastIndexOf('/')
        return if (lastSlashIndex >= 0) {
            normalizedPath.substring(0, lastSlashIndex).ifEmpty { "/" } // 确保根目录是 "/"
        } else {
            "/" // fallback 到根目录
        }
    }

    /**
     * 进入子目录
     * @param fileName 子目录名称
     */
    fun navigateToSubdirectory(fileName: String) {
        val newPath = if (_currentPath.value.isEmpty()) {
            "/$fileName"
        } else {
            "${_currentPath.value}/$fileName"
        }
        listFiles(newPath)
    }

    /**
     * 返回上一级目录
     */
    fun navigateToParent() {
        val parentPath = getParentPath(_currentPath.value)
        listFiles(parentPath)
    }


    override fun onCleared() {
        super.onCleared()
        // ViewModel 被清除时确保断开连接
        viewModelScope.launch(Dispatchers.IO) {
            disconnectNfs()
        }
    }
}

// --- 状态密封类 ---
sealed class NFSConnectionStatus {
    object Disconnected : NFSConnectionStatus()
    object Connecting : NFSConnectionStatus()
    object Connected : NFSConnectionStatus()
    data class Error(val message: String) : NFSConnectionStatus()

    // 添加一个用于 UI 显示的描述方法
    override fun toString(): String {
        return when (this) {
            Disconnected -> "已断开"
            Connecting -> "连接中..."
            Connected -> "已连接"
            is Error -> "错误: $message"
        }
    }
}



