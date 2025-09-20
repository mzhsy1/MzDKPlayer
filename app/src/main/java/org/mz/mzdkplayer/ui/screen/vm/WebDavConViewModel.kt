package org.mz.mzdkplayer.ui.screen.vm

import android.annotation.SuppressLint
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thegrizzlylabs.sardineandroid.DavResource // 使用正确的包名
import com.thegrizzlylabs.sardineandroid.impl.OkHttpSardine // 使用正确的包名
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

class WebDavConViewModel : ViewModel() {

    // 明确指定 MutableStateFlow 的泛型类型
    private val _connectionStatus: MutableStateFlow<WebDavConnectionStatus> = MutableStateFlow(WebDavConnectionStatus.Disconnected)
    val connectionStatus: StateFlow<WebDavConnectionStatus> = _connectionStatus

    // 存储文件/文件夹列表
    private val _fileList: MutableStateFlow<List<DavResource>> = MutableStateFlow(emptyList())
    val fileList: StateFlow<List<DavResource>> = _fileList

    // 存储当前工作目录的路径 (相对于根URL)
    private val _currentPath: MutableStateFlow<String> = MutableStateFlow("")
    val currentPath: StateFlow<String> = _currentPath

    private var sardine: OkHttpSardine? = null // 使用具体的实现类
    private var baseUrl: String = "" // 存储基础 WebDAV URL

    private val mutex = Mutex() // 用于协程同步

    /**
     * 连接到 WebDAV 服务器
     * @param baseUrl WebDAV 服务器的基础 URL (e.g., "https://example.com/remote.php/dav/files/username/")
     * @param username 用户名
     * @param password 密码
     */
    fun connectToWebDav(baseUrl: String, username: String, password: String) {
        viewModelScope.launch {
            // 注意：移除了开头的 disconnectWebDav() 调用
            mutex.withLock {
                // --- 关键修改：确保赋值类型匹配 ---
                _connectionStatus.value = WebDavConnectionStatus.Connecting
                // --- 关键修改结束 ---
                try {
                    withContext(Dispatchers.IO) {
                        // 创建不验证证书的 TrustManager
                        val trustAllCerts = arrayOf<TrustManager>(@SuppressLint("CustomX509TrustManager")
                        object : X509TrustManager {
                            @SuppressLint("TrustAllX509TrustManager")
                            override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                            @SuppressLint("TrustAllX509TrustManager")
                            override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                            override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
                        })

                        // 配置 OkHttpClient 忽略 SSL 验证
                        val sslContext = SSLContext.getInstance("SSL")
                        sslContext.init(null, trustAllCerts, SecureRandom())

                        val okHttpClient = OkHttpClient.Builder()
                            .sslSocketFactory(sslContext.socketFactory, trustAllCerts[0] as X509TrustManager)
                            .hostnameVerifier { _, _ -> true } // 忽略主机名验证
                            .build()
                        // 初始化 OkHttpSardine 客户端
                        sardine = OkHttpSardine(okHttpClient)
                        // 设置认证信息
                        sardine?.setCredentials(username, password)

                        // 验证连接 - 尝试列出根目录
                        val trimmedBaseUrl = baseUrl.trimEnd('/')
                        this@WebDavConViewModel.baseUrl = trimmedBaseUrl
                        val rootResources = sardine?.list("$trimmedBaseUrl/") ?: throw Exception("无法列出根目录")

                        _fileList.value = rootResources.filter { it.name != ".." } // 过滤掉 ..
                    }
                    // --- 关键修改：确保赋值类型匹配 ---
                    _currentPath.value = "" // 重置路径
                    _connectionStatus.value = WebDavConnectionStatus.Connected
                    // --- 关键修改结束 ---
                    Log.d("WebDavConViewModel", "连接成功到 $baseUrl")
                } catch (e: Exception) {
                    Log.e("WebDavConViewModel", "连接失败", e)
                    // --- 关键修改：确保赋值类型匹配 ---
                    _connectionStatus.value = WebDavConnectionStatus.Error("连接失败: ${e.message}")
                    // 保持 fileList 为当前状态，或者清空，取决于你的 UI 需求。
                    // 如果希望失败时保留上次列表，就注释掉下面这行
                    // _fileList.value = emptyList()
                    // --- 关键修改结束 ---
                    // 不再调用 disconnectWebDav()，因为连接失败本身就意味着没有连接上。
                    // disconnectWebDav() // 连接失败时清理 - 注释掉这行
                }
            }
        }
    }

    /**
     * 列出指定路径下的文件和文件夹
     * @param path 相对于 baseUrl 的路径
     */
    fun listFiles(path: String = "") {
        val effectivePath = path.trimStart('/')
        val fullUrl = if (effectivePath.isEmpty()) {
            "$baseUrl/"
        } else {
            "$baseUrl/$effectivePath/"
        }

        viewModelScope.launch {
            // 检查连接状态
            // --- 关键修改：确保赋值类型匹配 ---
            if (_connectionStatus.value != WebDavConnectionStatus.Connected) {
                _connectionStatus.value = WebDavConnectionStatus.Error("未连接到服务器")
                return@launch
            }
            // --- 关键修改结束 ---

            mutex.withLock {
                try {
                    withContext(Dispatchers.IO) {
                        val resources = sardine?.list(fullUrl) ?: throw Exception("Sardine 未初始化或连接失败")
                        // 过滤掉 "." 和 ".."
                        val filteredResources = resources.filter { it.name != "." && it.name != ".." }
                        _fileList.value = filteredResources
                        _currentPath.value = effectivePath // 更新当前路径
                    }
                    Log.d("WebDavConViewModel", "列出文件成功: $fullUrl")
                } catch (e: Exception) {
                    Log.e("WebDavConViewModel", "获取文件列表失败: $fullUrl", e)
                    // --- 关键修改：确保赋值类型匹配 ---
                    _connectionStatus.value = WebDavConnectionStatus.Error("获取文件失败: ${e.message}")
                    // --- 关键修改结束 ---
                    // 可以考虑在严重错误时断开连接
                    // if (e is SomeSpecificNetworkException) {
                    //     disconnectWebDav()
                    // }
                }
            }
        }
    }

    /**
     * 断开与 WebDAV 服务器的连接
     */
    fun disconnectWebDav() {
        viewModelScope.launch(Dispatchers.IO) {
            mutex.withLock {
                try {
                    // OkHttpSardine 通常不需要显式关闭连接，它会管理底层连接
                    // 但我们可以将引用置为 null
                    sardine = null
                    baseUrl = ""
                } catch (e: Exception) {
                    Log.w("WebDavConViewModel", "断开连接时发生异常", e)
                } finally {
                    // 确保在 Main 线程更新状态
                    withContext(Dispatchers.Main) {
                        // --- 关键修改：确保赋值类型匹配 ---
                        _connectionStatus.value = WebDavConnectionStatus.Disconnected
                        // --- 关键修改结束 ---
                        _fileList.value = emptyList() // 断开连接时清空列表
                        _currentPath.value = ""
                    }
                }
            }
        }
    }

    /**
     * 检查当前是否已连接 (基于 ViewModel 内部状态)
     */
    fun isConnected(): Boolean {
        return _connectionStatus.value == WebDavConnectionStatus.Connected
    }

    /**
     * 获取当前完整的工作目录 URL
     */
    fun getCurrentFullUrl(): String {
        val effectivePath = _currentPath.value.trimStart('/')
        return if (effectivePath.isEmpty()) {
            "$baseUrl/"
        } else {
            "$baseUrl/$effectivePath/"
        }
    }

    /**
     * 获取父目录路径
     */
    fun getParentPath(): String {
        val current = _currentPath.value
        if (current.isEmpty() || current == "/") {
            return "" // 已经在根目录
        }
        // 找到最后一个 '/' 并截取前面的部分
        val lastSlashIndex = current.lastIndexOf('/')
        return if (lastSlashIndex >= 0) {
            current.substring(0, lastSlashIndex)
        } else {
            "" // 回到根目录
        }
    }

    /**
     * 获取文件或文件夹的完整 URL
     * @param resourceName 文件或文件夹名
     */
    fun getResourceFullUrl(resourceName: String): String {
        val currentFullUrl = getCurrentFullUrl()
        // 确保 URL 以 '/' 结尾
        val baseUrlWithSlash = if (currentFullUrl.endsWith("/")) currentFullUrl else "$currentFullUrl/"
        return "$baseUrlWithSlash$resourceName"
    }

    override fun onCleared() {
        super.onCleared()
        // ViewModel 被清除时确保断开连接
        viewModelScope.launch(Dispatchers.IO) {
            disconnectWebDav()
        }
    }
}

// --- 状态枚举 ---
sealed class WebDavConnectionStatus {
    object Disconnected : WebDavConnectionStatus()
    object Connecting : WebDavConnectionStatus()
    object Connected : WebDavConnectionStatus()
    data class Error(val message: String) : WebDavConnectionStatus()

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



