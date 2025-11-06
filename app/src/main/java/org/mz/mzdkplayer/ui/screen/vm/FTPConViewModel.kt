package org.mz.mzdkplayer.ui.screen.vm // 请根据你的实际包名修改

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.apache.commons.net.ftp.FTP
import org.apache.commons.net.ftp.FTPClient
import org.apache.commons.net.ftp.FTPFile
import org.mz.mzdkplayer.logic.model.FileConnectionStatus
import java.io.IOException

class FTPConViewModel : ViewModel() {

    private val _connectionStatus: MutableStateFlow<FileConnectionStatus> = MutableStateFlow(FileConnectionStatus.Disconnected)
    val connectionStatus: StateFlow<FileConnectionStatus> = _connectionStatus

    // 使用 FTPFile 来表示 FTP 服务器上的文件/目录
    private val _fileList: MutableStateFlow<List<FTPFile>> = MutableStateFlow(emptyList())
    val fileList: StateFlow<List<FTPFile>> = _fileList

    // 存储当前工作目录的路径 (相对于根目录)
    private val _currentPath: MutableStateFlow<String> = MutableStateFlow("")
    val currentPath: StateFlow<String> = _currentPath

    private var ftpClient: FTPClient? = null
    private var server: String = "" // 存储基础 FTP 服务器地址 (用于显示)
    private var username: String = ""
    private var password: String = ""

    private var port: Int = 21

    private val mutex = Mutex()

    /**
     * 连接到 FTP 服务器
     *
     * @param server FTP 服务器地址 (e.g., "ftp.example.com")
     * @param port FTP 服务器端口 (e.g., 21)
     * @param username 用户名
     * @param password 密码
     */
    fun connectToFTP(
        server: String?,
        port: Int?,
        username: String?,
        password: String?,
        shareName: String?, // 例如 "movies" 或 "documents/shared"
    ) {
       // Log.d("path", "$server:$port")
        Log.d("shareName", shareName.toString())
        viewModelScope.launch {
            mutex.withLock {
                _connectionStatus.value = FileConnectionStatus.Connecting
                try {
                    withContext(Dispatchers.IO) {
                        // 初始化 FTPClient
                        ftpClient = FTPClient()
                        ftpClient?.controlEncoding = "UTF-8" //ftpClient.sendSiteCommand("OPTS UTF8 ON")
                        if (username != null) {
                            this@FTPConViewModel.username = username
                        }
                        if (password != null) {
                            this@FTPConViewModel.password = password
                        }
                        if (server != null) {
                            this@FTPConViewModel.server = server
                        } // 仅用于显示或构建完整 URL

                        if (port != null) {
                            this@FTPConViewModel.port = port
                        } // 仅用于显示或构建完整 URL

                        // 连接并登录
                        ftpClient?.connect(server, port?:21)
                        val loginSuccess = ftpClient?.login(username, password) ?: false
                        if (!loginSuccess) {
                            throw IOException("FTP 登录失败")
                        }

                        // 设置文件类型为二进制 (推荐用于兼容性)
                        ftpClient?.setFileType(FTP.BINARY_FILE_TYPE)
                        // 进入被动模式 (PASV) - 通常对客户端防火墙更友好

                        ftpClient?.enterLocalPassiveMode()
                        _connectionStatus.value = FileConnectionStatus.Connected
                        // --- 修改部分开始 ---
                        // 确保 shareName 以 '/' 开头，便于构建路径
                        val initialPath = if (shareName?.startsWith("/") ?: true) shareName else "/$shareName"
                        // 确保路径以 '/' 结尾，以便正确列出目录内容 (如果它是目录的话)
                        val initialDirPath = if (initialPath?.endsWith("/") ?: true) initialPath else "$initialPath/"

                        // 尝试列出 shareName 指定的目录内容
                        //val initialFiles = ftpClient?.listFiles(initialDirPath) ?: throw IOException("无法列出初始目录: $initialDirPath")
                        //.value = initialFiles.toList()
                        if (initialDirPath!=null){
                            listFiles(initialDirPath)
                        }

                        // 更新当前路径为 shareName (去除开头的 '/' 以便于后续路径拼接)
                        _currentPath.value = initialPath?.removePrefix("/").toString()
                        // --- 修改部分结束 ---
                    }
                    // _currentPath 已在 IO 线程中设置

                    Log.d("FTPConViewModel", "连接成功到 $server:$port, 初始路径: ${_currentPath.value}")
                } catch (e: Exception) {
                    Log.e("FTPConViewModel", "连接失败", e)
                    _connectionStatus.value = FileConnectionStatus.Error("连接失败: ${e.message}")
                    // 连接失败时清理
                    cleanupFTPClient()
                }
            }
        }
    }

    /**
     * 列出指定路径下的文件和文件夹
     *
     * @param path 相对于 FTP 服务器根目录的路径 (e.g., "videos/", "documents/reports")
     */
    fun listFiles(path: String = "") {
        viewModelScope.launch {
            if (_connectionStatus.value != FileConnectionStatus.Connected &&
                _connectionStatus.value !is FileConnectionStatus.FilesLoaded) {
                _connectionStatus.value = FileConnectionStatus.Error("未连接")
                return@launch
            }
            _connectionStatus.value = FileConnectionStatus.LoadingFile
            mutex.withLock {
                try {
                    withContext(Dispatchers.IO) {
                        val targetPath = if (path.startsWith("/")) path else "/$path"
                        // 确保路径以 '/' 结尾，以便正确列出目录内容
                        val dirPath = if (targetPath.endsWith("/")) targetPath else "$targetPath/"

                        val files = ftpClient?.listFiles(dirPath) ?: throw Exception("FTP 客户端未初始化或连接失败")
                        // FTP 通常不会像 WebDAV 那样返回 "." 和 ".."，
                        // 但如果返回了，可以选择过滤掉
                        // val filteredFiles = files.filter { it.name != "." && it.name != ".." }
                        _fileList.value = files.toList()
                        _currentPath.value = path // 更新当前路径 (不带开头的 /)
                        _connectionStatus.value = FileConnectionStatus.FilesLoaded
                    }
                    Log.d("FTPConViewModel", "列出文件成功: $path")
                } catch (e: Exception) {
                    Log.e("FTPConViewModel", "获取文件列表失败: $path", e)
                    _connectionStatus.value = FileConnectionStatus.Error("获取文件失败: ${e.message}")
                }
            }
        }
    }

    /**
     * 断开与 FTP 服务器的连接
     */
    fun disconnectFTP() {
        viewModelScope.launch(Dispatchers.IO) {
            mutex.withLock {
                try {
                    cleanupFTPClient()
                } catch (e: Exception) {
                    Log.w("FTPConViewModel", "断开连接时发生异常", e)
                } finally {
                    withContext(Dispatchers.Main) {
                        _connectionStatus.value = FileConnectionStatus.Disconnected
                        _fileList.value = emptyList()
                        _currentPath.value = ""
                        this@FTPConViewModel.username = ""
                        this@FTPConViewModel.password = ""
                        this@FTPConViewModel.server = ""
                    }
                }
            }
        }
    }

    /**
     * 检查当前是否已连接
     */
    fun isConnected(): Boolean {
        return _connectionStatus.value == FileConnectionStatus.Connected ||
                _connectionStatus.value == FileConnectionStatus.FilesLoaded ||
                _connectionStatus.value is FileConnectionStatus.LoadingFile
    }

    /**
     * 获取当前完整的工作目录路径 (相对于根目录)
     */
    fun getCurrentPath(): String {
        val path = _currentPath.value
        return if (path.startsWith("/")) path else "/$path"
    }

    /**
     * 获取父目录路径
     */
    fun getParentPath(): String {
        val current = _currentPath.value
        if (current.isEmpty() || current == "/" || !current.contains("/")) {
            return "" // 已经在根目录
        }
        // 找到最后一个 '/' 并截取前面的部分
        val lastSlashIndex = current.lastIndexOf('/')
        return if (lastSlashIndex >= 0) {
            current.substring(0, lastSlashIndex).ifEmpty { "" } // 确保不是 "/"
        } else {
            "" // 回到根目录
        }
    }

    /**
     * 获取文件或文件夹的完整 URL (FTP URL scheme)
     * 注意：这会暴露用户名和密码，主要用于内部标识或直接播放。公开显示需谨慎。
     * 格式: ftp://username:password@server:port/path/to/file
     *
     * @param resourceName 文件或文件夹名
     */
    fun getResourceFullUrl(resourceName: String): String {
        val server = this.server
        val port = this.port // 获取连接时使用的端口
        val path = getCurrentPath()
        val cleanPath = if (path.endsWith("/")) path else "$path/"
        val cleanResourceName = resourceName.removePrefix("/") // 确保资源名不以 / 开头
        return "ftp://$username:$password@$server:$port$cleanPath$cleanResourceName"
    }

    /**
     * 内部辅助函数：安全地断开并清理 FTPClient
     */
    private fun cleanupFTPClient() {
        try {
            ftpClient?.logout()
        } catch (e: IOException) {
            Log.w("FTPConViewModel", "FTP logout error", e)
        }
        try {
            if (ftpClient?.isConnected == true) {
                ftpClient?.disconnect()
            }
        } catch (e: IOException) {
            Log.w("FTPConViewModel", "FTP disconnect error", e)
        }
        ftpClient = null
    }

    override fun onCleared() {
        super.onCleared()
        // ViewModel 被清除时确保断开连接
        viewModelScope.launch(Dispatchers.IO) {
            disconnectFTP()
        }
    }
}

// --- 状态枚举 ---




