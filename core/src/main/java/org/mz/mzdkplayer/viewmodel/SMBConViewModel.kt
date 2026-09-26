package org.mz.mzdkplayer.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hierynomus.msfscc.fileinformation.FileIdBothDirectoryInformation
import com.hierynomus.protocol.transport.TransportException
import com.hierynomus.smbj.SMBClient
import com.hierynomus.smbj.SmbConfig
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
import org.mz.mzdkplayer.data.model.FileConnectionStatus
import org.mz.mzdkplayer.tool.FileBrowserLogic
import java.util.concurrent.TimeUnit

import kotlin.collections.forEach

class SMBConViewModel : ViewModel() {
    private val _connectionStatus: MutableStateFlow<FileConnectionStatus> =
        MutableStateFlow(FileConnectionStatus.Disconnected)
    val connectionStatus: StateFlow<FileConnectionStatus> = _connectionStatus
    private val _fileList = MutableStateFlow<List<SMBFileItem>>(emptyList())
    val fileList: StateFlow<List<SMBFileItem>> = _fileList

    private var connection: Connection? = null
    private var session: Session? = null
    private var share: DiskShare? = null
    private var client: SMBClient? = null
    private val mutex = Mutex()  // 协程互斥锁
    fun connectToSMB(ip: String, username: String, password: String, shareName: String) {

        viewModelScope.launch {
            _connectionStatus.value = FileConnectionStatus.Connecting
            mutex.withLock {
                try {

                    withContext(Dispatchers.IO) {

                        if (!isConnected()) {  // 避免重复连接
                            client = SMBClient()
                            Log.d("_connectionStatus1", _connectionStatus.value.toString())
                            connection = client?.connect(ip)
                            val auth = AuthenticationContext(username, password.toCharArray(), null)
                            session = connection!!.authenticate(auth)
                            share = session!!.connectShare(shareName) as DiskShare
                        }

                    }


                    _connectionStatus.value = FileConnectionStatus.Connected

                } catch (e: Exception) {
                    Log.e("SMB", "连接失败$e", e)
                    _connectionStatus.value = FileConnectionStatus.Error("连接失败: ${e.message}")
                    //disconnectSMB()
                }
            }
        }
    }

    fun testConnectSMB(ip: String, username: String, password: String, shareName: String) {

        viewModelScope.launch {

            mutex.withLock {
                try {

                    withContext(Dispatchers.IO) {
                        _connectionStatus.value = FileConnectionStatus.Connecting
                        if (!isConnected()) {  // 避免重复连接
                            val clientConfig = SmbConfig.builder()
                                .withTimeout(15, TimeUnit.SECONDS) // 连接超时
                                .build()
                            client = SMBClient(clientConfig)
                            connection = client?.connect(ip)
                            val auth = AuthenticationContext(username, password.toCharArray(), null)
                            session = connection!!.authenticate(auth)
                            share = session!!.connectShare(shareName) as DiskShare
                        }

                    }


                    _connectionStatus.value = FileConnectionStatus.Connected
                    listSMBFiles(SMBConfig(ip, shareName, "/", username, password)) // 获取文件列表
                    Log.d("_connectionStatus1", _connectionStatus.value.toString())
                } catch (e: Exception) {
                    Log.e("SMB", "连接失败$e", e)
                    _connectionStatus.value = FileConnectionStatus.Error("连接失败: ${e.message}")
                    _fileList.value = emptyList()
                    //disconnectSMB()
                }
            }
        }
    }

    fun listSMBFiles(config: SMBConfig) {
        viewModelScope.launch {
//            if (_connectionStatus.value != FileConnectionStatus.Connected &&
//                _connectionStatus.value !is FileConnectionStatus.FilesLoaded) {
//                _connectionStatus.value = FileConnectionStatus.Error("未连接")
//                return@launch
//            }

            Log.d("listSMBFiles", "正在列出文件")
            mutex.withLock {
                try {
                    // 只更新一次状态为加载中
                    _connectionStatus.value = FileConnectionStatus.LoadingFile

                    // 在 IO 线程执行所有繁重工作
                    val files = withContext(Dispatchers.IO) {
                        try {
                            val cleanPath = FileBrowserLogic.normalizeSmbDirectory(config.path)
                            val startTime = System.currentTimeMillis()
                            val fileList = mutableListOf<SMBFileItem>()
                            share?.list(cleanPath)
                                ?.forEach { fileInfo: FileIdBothDirectoryInformation ->
                                    val fileName = fileInfo.fileName
                                    if (!FileBrowserLogic.isHiddenDirEntry(fileName)) {
                                        val isDirectory = isDirectory(fileInfo.fileAttributes)
                                        val filePath = FileBrowserLogic.joinSmbPath(cleanPath, fileName)

                                        fileList.add(
                                            SMBFileItem(
                                                name = fileName,
                                                fullPath = FileBrowserLogic.toSmbDisplayPath(filePath),
                                                isDirectory = isDirectory,
                                                fileSize = fileInfo.endOfFile,
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

                        }
                    }

                    // 一次性更新最终状态
                    _fileList.value = files
                    _connectionStatus.value = FileConnectionStatus.FilesLoaded

                } catch (e: Exception) {
                    Log.e("SMBConViewModel", "连接失败", e)
                    _connectionStatus.value = FileConnectionStatus.Error("连接失败: ${e.message}")
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
            } catch (e: Exception) {
                Log.w("SMB", "Share 已断开，无需关闭")
            } finally {
                share = null
            }

            try {
                session?.close()
            } catch (e: Exception) {
                Log.w("SMB", "Session 已断开，无需关闭")
            } finally {
                session = null
            }

            try {
                connection?.close()
            } catch (e: Exception) {
                Log.w("SMB", "Connection 已断开，无需关闭")
            } finally {
                connection = null
            }

//            withContext(Dispatchers.Main) {
//                _connectionStatus.value = FileConnectionStatus.Disconnected
//                _fileList.value = emptyList()
//            }
        }
        // 回到主线程更新 UI 状态
        _connectionStatus.value = FileConnectionStatus.Disconnected
        _fileList.value = emptyList()
        Log.i("SMBCON",_connectionStatus.value.toString())
    }

    fun isConnected(): Boolean {
        return connection?.isConnected == true &&
                share != null
    }


    fun parseSMBPath(path: String): SMBConfig {
        // 格式: smb://username:password@server/share/path/to/directory
        val parts = FileBrowserLogic.parseSmbUrl(path) ?: return SMBConfig("", "", "", "", "")
        return SMBConfig(
            server = parts.server,
            share = parts.share,
            path = parts.path,
            username = parts.username,
            password = parts.password
        )
    }

    fun buildSMBPath(
        server: String,
        share: String,
        path: String,
        username: String,
        password: String
    ): String {
        return FileBrowserLogic.buildSmbUrl(server, share, path, username, password)
    }

    // 方法1：使用 FileAttributes 常量进行位运算判断
    fun isDirectory(fileAttributes: Long): Boolean {
        return FileBrowserLogic.isSmbDirectory(fileAttributes)
    }

    suspend fun scanVideosRecursive(config: SMBConfig, maxDepth: Int): List<Pair<String, String>> = withContext(Dispatchers.IO) {
        val result = mutableListOf<Pair<String, String>>()
        if (share == null) return@withContext emptyList()

        val cleanPath = FileBrowserLogic.normalizeSmbDirectory(config.path)

        fun scanRecursive(currentPath: String, currentDepth: Int) {
            if (currentDepth > maxDepth) return

            try {
                share?.list(currentPath)?.forEach { fileInfo ->
                    val fileName = fileInfo.fileName
                    if (FileBrowserLogic.isHiddenDirEntry(fileName)) return@forEach

                    val isDirectory = isDirectory(fileInfo.fileAttributes)
                    val filePath = FileBrowserLogic.joinSmbPath(currentPath, fileName)
                    val fullPath = FileBrowserLogic.toSmbDisplayPath(filePath)

                    if (isDirectory) {
                        scanRecursive(filePath, currentDepth + 1)
                    } else if (org.mz.mzdkplayer.tool.Tools.containsVideoFormat(org.mz.mzdkplayer.tool.Tools.extractFileExtension(fileName))) {
                        val fullUri = FileBrowserLogic.buildSmbUrlWithCredentials(
                            config.server, config.share, fullPath, config.username, config.password
                        )
                        result.add(fileName to fullUri)
                    }
                }
            } catch (e: Exception) {
                Log.e("SMBConViewModel", "Error scanning $currentPath", e)
            }
        }

        scanRecursive(cleanPath, 0)
        result
    }
}

// --- 状态枚举 ---


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
    val fileSize: Long = 0L,
    val server: String,
    val share: String,
    val username: String,
    val password: String
)



