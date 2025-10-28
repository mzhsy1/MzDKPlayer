//package org.mz.mzdkplayer.ui.screen.vm
//
//import android.util.Log
//import androidx.lifecycle.ViewModel
//import androidx.lifecycle.viewModelScope
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.flow.MutableStateFlow
//import kotlinx.coroutines.flow.StateFlow
//import kotlinx.coroutines.launch
//import kotlinx.coroutines.sync.Mutex
//import kotlinx.coroutines.sync.withLock
//import kotlinx.coroutines.withContext
//import org.codelibs.jcifs.smb.CIFSContext
//import org.codelibs.jcifs.smb.config.PropertyConfiguration
//import org.codelibs.jcifs.smb.context.BaseContext
//import org.codelibs.jcifs.smb.impl.NtlmPasswordAuthenticator
//import org.codelibs.jcifs.smb.impl.SmbFile
//import java.net.MalformedURLException
//import java.util.Properties
//
//
//class SMBConViewModel : ViewModel() {
//    private val _connectionStatus: MutableStateFlow<SMBConnectionStatus> =
//        MutableStateFlow(SMBConnectionStatus.Disconnected)
//    val connectionStatus: StateFlow<SMBConnectionStatus> = _connectionStatus
//    private val _fileList = MutableStateFlow<List<SMBFileItem>>(emptyList())
//    val fileList: StateFlow<List<SMBFileItem>> = _fileList
//
//    private var smbRoot: SmbFile? = null
//    private val mutex = Mutex()  // 协程互斥锁
//
//    fun connectToSMB(ip: String, username: String, password: String, shareName: String) {
//        viewModelScope.launch {
//            disconnectSMB()  // 先清理旧连接
//            mutex.withLock {
//                try {
//                    withContext(Dispatchers.IO) {
//                        if (!isConnected()) {  // 避免重复连接
//                            val props = Properties()
//
//// Optional: Set SMB protocol preferences
//                            props.setProperty("jcifs.smb.client.minVersion", "SMB202")
//                            props.setProperty("jcifs.smb.client.maxVersion", "SMB311")
//
//                            val baseContext: CIFSContext = BaseContext(PropertyConfiguration(props))
//                            val auth = NtlmPasswordAuthenticator(
//                                "DOMAIN",  // Domain name
//                                "username",  // Username
//                                "password" // Password
//                            )
//
//                            val authContext = baseContext.withCredentials(auth)
//                            val url = "smb://$ip/$shareName/"
//                            smbRoot = SmbFile(url, authContext)
//                        }
//                    }
//                    _connectionStatus.value = SMBConnectionStatus.Connected
//                    Log.d("_connectionStatus1", _connectionStatus.value.toString())
//                } catch (e: Exception) {
//                    Log.e("SMB", "连接失败${e.message}", e)
//                    _connectionStatus.value = SMBConnectionStatus.Error("连接失败: ${e.message}")
//                }
//            }
//        }
//    }
//
//    fun testConnectSMB(ip: String, username: String, password: String, shareName: String) {
//        viewModelScope.launch {
//            mutex.withLock {
//                try {
//                    withContext(Dispatchers.IO) {
//                        if (!isConnected()) {  // 避免重复连接
//
//
//                            // Create context with domain credentials
//                            val props = Properties()
//
//// Optional: Set SMB protocol preferences
//                            props.setProperty("jcifs.smb.client.minVersion", "SMB202")
//                            props.setProperty("jcifs.smb.client.maxVersion", "SMB311")
//
//                            val baseContext: CIFSContext = BaseContext(PropertyConfiguration(props))
//                            val auth = NtlmPasswordAuthenticator(
//                                "DOMAIN",  // Domain name
//                                "username",  // Username
//                                "password" // Password
//                            )
//
//                            val authContext = baseContext.withCredentials(auth)
//                            val url = "smb://$ip/$shareName/"
//                            smbRoot = SmbFile(url, authContext)
//                        }
//                    }
//                    _connectionStatus.value = SMBConnectionStatus.Connected
//                    listSMBFiles(SMBConfig(ip, shareName, "/", username, password)) // 获取文件列表
//                    Log.d("_connectionStatus1", _connectionStatus.value.toString())
//                } catch (e: Exception) {
//                    Log.e("SMB", "连接失败${e.message}", e)
//                    _connectionStatus.value = SMBConnectionStatus.Error("连接失败: ${e.message}")
//                }
//            }
//        }
//    }
//
//    fun listSMBFiles(config: SMBConfig) {
//        Log.d("listSMBFiles", config.toString())
//        Log.d("_connectionStatus2", _connectionStatus.value.toString())
//        viewModelScope.launch {
//            if (_connectionStatus.value != SMBConnectionStatus.Connected) {
//                _connectionStatus.value = SMBConnectionStatus.Error("未连接到服务器")
//                return@launch
//            }
//            Log.d("listSMBFiles", "正在列出文件")
//            mutex.withLock {
//                try {
//                    withContext(Dispatchers.IO) {
//                        _connectionStatus.value = SMBConnectionStatus.LoadingFile
//                        // 构建SMB路径
//                        val path = if (config.path == "/") {
//                            "smb://${config.server}/${config.share}/"
//                        } else {
//                            val cleanPath = config.path.trimStart('/').replace("/", "/")
//                            "smb://${config.server}/${config.share}/$cleanPath/"
//                        }
//
//                        val props = Properties()
//
//// Optional: Set SMB protocol preferences
//                        props.setProperty("jcifs.smb.client.minVersion", "SMB202")
//                        props.setProperty("jcifs.smb.client.maxVersion", "SMB311")
//
//                        val baseContext: CIFSContext = BaseContext(PropertyConfiguration(props))
//                        val auth = NtlmPasswordAuthenticator(
//                            "DOMAIN",  // Domain name
//                            "username",  // Username
//                            "password" // Password
//                        )
//
//                        val authContext = baseContext.withCredentials(auth)
//                        //val url = "smb://$ip/$shareName/"
//                        val smbDir = SmbFile(path, authContext)
//
//                        val files = mutableListOf<SMBFileItem>()
//                        val smbFiles = smbDir.listFiles()
//
//                        smbFiles.forEach { file ->
//                            val fileName = file.name
//                            // 跳过当前目录和父目录
//                            if (fileName != "." && fileName != "..") {
//                                val isDirectory = file.isDirectory
//                                val filePath = if (config.path == "/") {
//                                    "/$fileName"
//                                } else {
//                                    "${config.path}/$fileName"
//                                }
//
//                                files.add(
//                                    SMBFileItem(
//                                        name = fileName.trimEnd('/'), // 去掉目录名末尾的斜杠
//                                        fullPath = filePath,
//                                        isDirectory = isDirectory,
//                                        server = config.server,
//                                        share = config.share,
//                                        username = config.username,
//                                        password = config.password,
//                                    )
//                                )
//                            }
//                        }
//
//                        _fileList.value = files.sortedBy { it.name }
//                        _connectionStatus.value = SMBConnectionStatus.LoadingFiled
//                    }
//                } catch (e: Exception) {
//                    Log.e("SMBConViewModel", "连接失败", e)
//                    _connectionStatus.value = SMBConnectionStatus.Error("连接失败: ${e.message}")
//                    // 连接失败时清理
//                    disconnectSMB()
//                }
//            }
//        }
//    }
//
//    // 断开连接
//    fun disconnectSMB() {
//        viewModelScope.launch(Dispatchers.IO) {
//            try {
//                smbRoot = null
//            } catch (e: Exception) {
//                Log.w("SMB", "清理SMB连接时出错", e)
//            } finally {
//                withContext(Dispatchers.Main) {
//                    _connectionStatus.value = SMBConnectionStatus.Disconnected
//                    _fileList.value = emptyList()
//                }
//            }
//        }
//    }
//
//    fun isConnected(): Boolean {
//        return smbRoot != null
//    }
//
//    fun isSessionActive(): Boolean {
//        return try {
//            smbRoot?.exists() == true
//        } catch (e: Exception) {
//            false  // 抛出异常说明连接已断开
//        }
//    }
//
//    fun parseSMBPath(path: String): SMBConfig {
//        // 格式: smb://username:password@server/share/path/to/directory
//        return try {
//            val smbFile = SmbFile(path)
//            val pathParts = smbFile.path.split("/")
//            if (pathParts.size >= 4) {
//                val server = smbFile.server
//                val share = pathParts[2] // 第三个部分是share名称
//                val path = "/" + pathParts.drop(3).joinToString("/")
//
//                // 从URL中提取用户名和密码
//                val userInfo = smbFile.userInfo
//                val (username, password) = if (userInfo != null) {
//                    val parts = userInfo.split(":")
//                    Pair(parts[0], if (parts.size > 1) parts[1] else "")
//                } else {
//                    Pair("", "")
//                }
//
//                SMBConfig(server, share, if (path.isEmpty()) "/" else path, username, password)
//            } else {
//                SMBConfig("", "", "", "", "")
//            }
//        } catch (e: MalformedURLException) {
//            Log.e("SMB", "Invalid SMB URL", e)
//            SMBConfig("", "", "", "", "")
//        }
//    }
//
//    fun buildSMBPath(
//        server: String,
//        share: String,
//        path: String,
//        username: String,
//        password: String
//    ): String {
//        return if (username.isNotEmpty() && password.isNotEmpty()) {
//            "smb://$username:$password@$server/$share$path"
//        } else {
//            "smb://$server/$share$path"
//        }
//    }
//}
//
//// --- 状态枚举 ---
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


//
