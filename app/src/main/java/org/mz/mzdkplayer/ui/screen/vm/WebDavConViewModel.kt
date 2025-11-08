package org.mz.mzdkplayer.ui.screen.vm

import android.annotation.SuppressLint
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thegrizzlylabs.sardineandroid.DavResource
import com.thegrizzlylabs.sardineandroid.impl.OkHttpSardine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import org.mz.mzdkplayer.logic.model.FileConnectionStatus
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
import androidx.core.net.toUri
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class WebDavConViewModel : ViewModel() {

    // 状态流
    private val _connectionStatus: MutableStateFlow<FileConnectionStatus> =
        MutableStateFlow(FileConnectionStatus.Disconnected)
    val connectionStatus: StateFlow<FileConnectionStatus> = _connectionStatus

    private val _fileList: MutableStateFlow<List<WebDavFileItem>> = MutableStateFlow(emptyList())
    val fileList: StateFlow<List<WebDavFileItem>> = _fileList
    var fileConverList: List<WebDavFileItem> by mutableStateOf(emptyList())
    private val _currentPath: MutableStateFlow<String> = MutableStateFlow("")
    val currentPath: StateFlow<String> = _currentPath

    private var sardine: OkHttpSardine? = null
    private var baseUrl: String = "" // 存储基础认证URL

    private val mutex = Mutex()

    /**
     * 连接到 WebDAV 服务器
     * @param fullPath 完整的 WebDAV URL 路径
     * @param username 用户名
     * @param password 密码
     */
    fun connectToWebDav(fullPath: String?, username: String?, password: String?,isTest: Boolean =false) {
        viewModelScope.launch {
            mutex.withLock {
                _connectionStatus.value = FileConnectionStatus.Connecting
                try {
                    withContext(Dispatchers.IO) {
                        // 创建不验证证书的 TrustManager
                        val trustAllCerts = arrayOf<TrustManager>(
                            @SuppressLint("CustomX509TrustManager")
                            object : X509TrustManager {
                                @SuppressLint("TrustAllX509TrustManager")
                                override fun checkClientTrusted(
                                    chain: Array<out X509Certificate>?,
                                    authType: String?
                                ) {
                                }

                                @SuppressLint("TrustAllX509TrustManager")
                                override fun checkServerTrusted(
                                    chain: Array<out X509Certificate>?,
                                    authType: String?
                                ) {
                                }

                                override fun getAcceptedIssuers(): Array<X509Certificate> =
                                    arrayOf()
                            })

                        val sslContext = SSLContext.getInstance("SSL")
                        sslContext.init(null, trustAllCerts, SecureRandom())

                        val okHttpClient = OkHttpClient.Builder()
                            .sslSocketFactory(
                                sslContext.socketFactory,
                                trustAllCerts[0] as X509TrustManager
                            )
                            .hostnameVerifier { _, _ -> true }
                            .build()

                        sardine = OkHttpSardine(okHttpClient)
                        sardine?.setCredentials(username, password)

                        // 存储基础URL用于后续认证


                        _connectionStatus.value = FileConnectionStatus.Connected

                       //  连接成功后立即列出文件
                        if (!fullPath.isNullOrEmpty()&&isTest) {
                            listFiles(fullPath, username, password)
                        }
                    }
                    Log.d("WebDavConViewModel", "连接成功到 $fullPath")
                } catch (e: Exception) {
                    Log.e("WebDavConViewModel", "连接失败", e)
                    _connectionStatus.value = FileConnectionStatus.Error("连接失败: ${e.message}")
                }
            }
        }
    }



    /**
     * 列出指定完整路径下的文件和文件夹
     * @param fullPath 完整的 WebDAV URL 路径
     */
    fun listFiles(fullPath: String, username: String?, password: String?) {
        viewModelScope.launch {
            _connectionStatus.value = FileConnectionStatus.LoadingFile
            mutex.withLock {
                try {
                    withContext(Dispatchers.IO) {
                        Log.d("WebDavCon","fullPath ${encodeWebDavPath(fullPath)}")
                        val resources = sardine?.list(encodeWebDavPath(fullPath))
                            ?: throw Exception("Sardine 未初始化或连接失败")

                        // 先去掉第一个元素（如果存在）
                        val withoutFirst = if (resources.isNotEmpty()) resources.drop(1) else emptyList()

                        // 再过滤掉 "." 和 ".."
                        val filteredResources = withoutFirst.filter { it.name != "." && it.name != ".." }

                        // 构建 WebDavFileItem 列表
                        val webDavFileItemList = filteredResources.map { resource ->
                            WebDavFileItem(
                                name = resource.name,
                                fullPath = fullPath,
                                isDirectory = resource.isDirectory,
                                path = resource.path,
                                username = username ?: "",
                                password = password ?: "",
                                size = resource.contentLength
                            )
                        }.toMutableList()

                        _fileList.value = webDavFileItemList
                        _currentPath.value = fullPath

                        Log.d(
                            "WebDavConViewModel",
                            "列出文件成功: $fullPath, 文件数量: ${webDavFileItemList.size}"
                        )
                    }
                    _connectionStatus.value = FileConnectionStatus.FilesLoaded
                } catch (e: Exception) {
                    Log.e("WebDavConViewModel", "获取文件列表失败: $fullPath", e)
                    _connectionStatus.value =
                        FileConnectionStatus.Error("获取文件失败: ${e.message}")
                }
            }
        }
    }

    // 添加 URL 编码函数
    private fun encodeWebDavPath(path: String): String {
        return try {
            // 分割协议和路径部分
            val protocolSeparator = "://"
            if (path.contains(protocolSeparator)) {
                val parts = path.split(protocolSeparator)
                val protocol = parts[0]
                val hostAndPath = parts[1]

                val hostPathParts = hostAndPath.split("/", limit = 2)
                val host = hostPathParts[0]
                val pathPart = if (hostPathParts.size > 1) hostPathParts[1] else ""

                // 对路径部分进行编码，使用 %20 而不是 +
                val encodedPath = pathPart.split("/").joinToString("/") { segment ->
                    URLEncoder.encode(segment, "UTF-8").replace("+", "%20")
                }

                "$protocol$protocolSeparator$host/$encodedPath"
            } else {
                // 如果没有协议，直接编码整个路径
                path.split("/").joinToString("/") { segment ->
                    URLEncoder.encode(segment, "UTF-8").replace("+", "%20")
                }
            }
        } catch (e: Exception) {
            Log.e("WebDavCon", "URL编码失败: $path", e)
            path // 如果编码失败，返回原路径
        }
    }
    /**
     * 断开与 WebDAV 服务器的连接
     */
    fun disconnectWebDav() {
        viewModelScope.launch(Dispatchers.IO) {
            mutex.withLock {
                try {
                    sardine = null
                    baseUrl = ""
                } catch (e: Exception) {
                    Log.w("WebDavConViewModel", "断开连接时发生异常", e)
                } finally {
                    withContext(Dispatchers.Main) {
                        _connectionStatus.value = FileConnectionStatus.Disconnected
                        _fileList.value = emptyList()
                        _currentPath.value = ""
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

    fun buildAuthenticatedUrl(
        baseUrl: String,
        username: String,
        password: String
    ): String {
        val uri = baseUrl.toUri()
        val userInfo = "$username:$password"
        val newAuthority = "$userInfo@${uri.authority}"
        return uri.buildUpon().encodedAuthority(newAuthority).build().toString()
    }

    fun buildFileUrl(
        parentPath: String,
        fileName: String,
        authenticatedBaseUrl: String
    ): String {
        // 确保路径拼接正确（避免双斜杠）
        val cleanParent = parentPath.trimEnd('/')
        val cleanFile = fileName.trimStart('/').trimEnd('/')
        return "$cleanParent/$cleanFile"
    }

    /**
     * 获取当前完整的工作目录 URL
     */
    fun getCurrentFullUrl(): String {
        return _currentPath.value
    }

    /**
     * 获取父目录路径
     */
    fun getParentPath(): String {
        val current = _currentPath.value
        if (current.isEmpty() || current == "/") {
            return "" // 已经在根目录
        }

        try {
            val uri = java.net.URI.create(current)
            val path = uri.path

            if (path.isEmpty() || path == "/") {
                return ""
            }

            // 找到最后一个 '/' 并截取前面的部分
            val lastSlashIndex = path.lastIndexOf('/')
            return if (lastSlashIndex >= 0) {
                val parentPath = path.take(lastSlashIndex)
                "${uri.scheme}://${uri.host}${if (uri.port != -1) ":${uri.port}" else ""}$parentPath"
            } else {
                "${uri.scheme}://${uri.host}${if (uri.port != -1) ":${uri.port}" else ""}/"
            }
        } catch (e: Exception) {
            Log.e("WebDavConViewModel", "获取父目录路径失败", e)
            return ""
        }
    }

    /**
     * 获取文件或文件夹的完整 URL
     * @param resourceName 文件或文件夹名
     */
    fun getResourceFullUrl(resourceName: String): String {
        val currentFullUrl = getCurrentFullUrl()
        // 确保 URL 以 '/' 结尾
        val baseUrlWithSlash =
            if (currentFullUrl.endsWith("/")) currentFullUrl else "$currentFullUrl/"
        return "$baseUrlWithSlash$resourceName"
    }

    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch(Dispatchers.IO) {
            disconnectWebDav()
        }
    }
}

data class WebDavFileItem(
    val name: String,
    val fullPath: String,
    val isDirectory: Boolean,
    val path: String,
    val username: String,
    val password: String,
    val size: Long
)