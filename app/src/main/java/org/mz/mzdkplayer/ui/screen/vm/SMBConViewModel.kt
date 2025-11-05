package org.mz.mzdkplayer.ui.screen.vm

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hierynomus.msfscc.FileAttributes
import com.hierynomus.msfscc.fileinformation.FileIdBothDirectoryInformation
import com.hierynomus.protocol.transport.TransportException
import com.hierynomus.smbj.SMBClient
import com.hierynomus.smbj.auth.AuthenticationContext
import com.hierynomus.smbj.connection.Connection
import com.hierynomus.smbj.session.Session
import com.hierynomus.smbj.share.DiskShare
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

import kotlin.collections.forEach

class SMBConViewModel : ViewModel() {
    private val _connectionStatus: MutableStateFlow<SMBConnectionStatus> =
        MutableStateFlow(SMBConnectionStatus.Disconnected)
    val connectionStatus: StateFlow<SMBConnectionStatus> = _connectionStatus
    private val _fileList = MutableStateFlow<List<SMBFileItem>>(emptyList())
    val fileList: StateFlow<List<SMBFileItem>> = _fileList

    private var connection: Connection? = null
    private var session: Session? = null
    private var share: DiskShare? = null
    private var client: SMBClient? = null
    private val mutex = Mutex()  // 协程互斥锁
    fun connectToSMB(ip: String, username: String, password: String, shareName: String) {

        viewModelScope.launch {
            disconnectSMB()  // 先清理旧连接
//            withContext(Dispatchers.Main) {
//                _connectionStatus.value = "正在尝试连接"
//            }
            mutex.withLock {
                try {

                    withContext(Dispatchers.IO) {

                        if (!isConnected()) {  // 避免重复连接
                            client = SMBClient()

                            connection = client?.connect(ip)
                            val auth = AuthenticationContext(username, password.toCharArray(), null)
                            session = connection!!.authenticate(auth)
                            share = session!!.connectShare(shareName) as DiskShare
                        }

                    }


                    _connectionStatus.value = SMBConnectionStatus.Connected
                    Log.d("_connectionStatus1", _connectionStatus.value.toString())
                } catch (e: Exception) {
                    Log.e("SMB", "连接失败$e", e)
                    _connectionStatus.value = SMBConnectionStatus.Error("连接失败: ${e.message}")
                    //disconnectSMB()
                }
            }
        }
    }

    fun testConnectSMB(ip: String, username: String, password: String, shareName: String) {

        viewModelScope.launch {
            //disconnectSMB()  // 先清理旧连接
//            withContext(Dispatchers.Main) {
//                _connectionStatus.value = "正在尝试连接"
//            }
            _connectionStatus.value = SMBConnectionStatus.Connecting
            mutex.withLock {
                try {

                    withContext(Dispatchers.IO) {

                        if (!isConnected()) {  // 避免重复连接
                            client = SMBClient()

                            connection = client?.connect(ip)
                            val auth = AuthenticationContext(username, password.toCharArray(), null)
                            session = connection!!.authenticate(auth)
                            share = session!!.connectShare(shareName) as DiskShare
                        }

                    }


                    _connectionStatus.value = SMBConnectionStatus.Connected
                    listSMBFiles(SMBConfig(ip, shareName, "/", username, password)) // 获取文件列表
                    Log.d("_connectionStatus1", _connectionStatus.value.toString())
                } catch (e: Exception) {
                    Log.e("SMB", "连接失败$e", e)
                    _connectionStatus.value = SMBConnectionStatus.Error("连接失败: ${e.message}")
                    _fileList.value = emptyList()
                    //disconnectSMB()
                }
            }
        }
    }

    fun listSMBFiles(config: SMBConfig) {
        viewModelScope.launch {
            if (_connectionStatus.value != SMBConnectionStatus.Connected) {
                _connectionStatus.value = SMBConnectionStatus.Error("未连接到服务器")
                return@launch
            }

            Log.d("listSMBFiles", "正在列出文件")
            mutex.withLock {
                try {
                    // 只更新一次状态为加载中
                    _connectionStatus.value = SMBConnectionStatus.LoadingFile

                    // 在 IO 线程执行所有繁重工作
                    val files = withContext(Dispatchers.IO) {
                        try {
                            val cleanPath = config.path.let {
                                if (it == "/") "\\" else it.replace("/", "\\").trimEnd('\\')
                            }
                            val startTime = System.currentTimeMillis()
                            val fileList = mutableListOf<SMBFileItem>()
                            share?.list(cleanPath)
                                ?.forEach { fileInfo: FileIdBothDirectoryInformation ->
                                    val fileName = fileInfo.fileName
                                    if (fileName != "." && fileName != "..") {
                                        val isDirectory = isDirectory(fileInfo.fileAttributes)
                                        val filePath = if (cleanPath == "\\") {
                                            "\\$fileName"
                                        } else {
                                            "$cleanPath\\$fileName"
                                        }

                                        fileList.add(
                                            SMBFileItem(
                                                name = fileName,
                                                fullPath = filePath.replace("\\", "/"),
                                                isDirectory = isDirectory,
                                                server = config.server,
                                                share = config.share,
                                                username = config.username,
                                                password = config.password,
                                            )
                                        )
                                    }
                                } ?: throw Exception("SMB 客户端未初始化或连接失败")
                            val getFilesTime = System.currentTimeMillis()
                            Log.d("Performance", "获取文件耗时: ${getFilesTime - startTime}ms")


                            // 排序
                            val sortedList = fileList.sortedBy { it.name }
                            val sortTime = System.currentTimeMillis()
                            Log.d("Performance", "排序耗时: ${sortTime - getFilesTime}ms")

                            sortedList
                        } finally {
                            share?.close()
                            connection?.close()
                        }
                    }

                    // 一次性更新最终状态
                    _fileList.value = files
                    _connectionStatus.value = SMBConnectionStatus.LoadingFiled

                } catch (e: Exception) {
                    Log.e("SMBConViewModel", "连接失败", e)
                    _connectionStatus.value = SMBConnectionStatus.Error("连接失败: ${e.message}")
                    disconnectSMB()
                }
            }
        }
    }



    // 断开连接
    fun disconnectSMB() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                share?.close()
            } catch (e: TransportException) {
                Log.w("SMB", "Share 已断开，无需关闭")
            } finally {
                share = null
            }

            try {
                session?.close()
            } catch (e: TransportException) {
                Log.w("SMB", "Session 已断开，无需关闭")
            } finally {
                session = null
            }

            try {
                connection?.close()
            } catch (e: TransportException) {
                Log.w("SMB", "Connection 已断开，无需关闭")
            } finally {
                connection = null
            }

            withContext(Dispatchers.Main) {
                _connectionStatus.value = SMBConnectionStatus.Disconnected
                _fileList.value = emptyList()
            }
        }
    }

    fun isConnected(): Boolean {
        return connection?.isConnected == true && isSessionActive()
    }

    fun isSessionActive(): Boolean {
        return try {
            share?.list("")  // 尝试列出根目录（不抛出异常说明连接正常）
            true
        } catch (e: Exception) {
            false  // 抛出异常说明连接已断开
        }
    }

    fun parseSMBPath(path: String): SMBConfig {
        // 格式: smb://username:password@server/share/path/to/directory
        val pattern = Regex("^smb://(?:([^:]+):([^@]+)@)?([^/]+)/([^/]+)(/.*)?$")
        val match = pattern.find(path) ?: return SMBConfig("", "", "", "", "")

        val (username, password, server, share, rawPath) = match.destructured
        val cleanPath = rawPath.trim().let {
            it.ifEmpty { "/" }
        }

        return SMBConfig(
            server = server,
            share = share,
            path = cleanPath,
            username = username.ifEmpty { "guest" },
            password = password.ifEmpty { "" }
        )
    }

    fun buildSMBPath(
        server: String,
        share: String,
        path: String,
        username: String,
        password: String
    ): String {
        return if (username.isNotEmpty() && password.isNotEmpty()) {
            "smb://$username:$password@$server/$share$path"
        } else {
            "smb://$server/$share$path"
        }
    }

    // 方法1：使用 FileAttributes 常量进行位运算判断
    fun isDirectory(fileAttributes: Long): Boolean {
        return (fileAttributes and FileAttributes.FILE_ATTRIBUTE_DIRECTORY.value) != 0L
    }
}

// --- 状态枚举 ---
sealed class SMBConnectionStatus {
    object Disconnected : SMBConnectionStatus()
    object Connecting : SMBConnectionStatus()
    object Connected : SMBConnectionStatus()
    object LoadingFile : SMBConnectionStatus()
    object LoadingFiled : SMBConnectionStatus()
    data class Error(val message: String) : SMBConnectionStatus()

    // 添加一个用于 UI 显示的描述方法
    override fun toString(): String {
        return when (this) {
            Disconnected -> "已断开"
            Connecting -> "连接中..."
            Connected -> "已连接"
            LoadingFile -> "正在加载文件"
            LoadingFiled -> "加载文件完成"
            is Error -> "错误: $message"
        }
    }
}

data class SMBConfig(
    val server: String,
    val share: String,
    val path: String,
    val username: String,
    val password: String
)

data class SMBFileItem(
    val name: String,
    val fullPath: String,
    val isDirectory: Boolean,
    val server: String,
    val share: String,
    val username: String,
    val password: String
)



