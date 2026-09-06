package org.mz.mzdkplayer.ui.screen.vm

import android.annotation.SuppressLint
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thegrizzlylabs.sardineandroid.impl.OkHttpSardine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import org.mz.mzdkplayer.data.model.FileConnectionStatus
import org.mz.mzdkplayer.data.repository.SettingsRepository
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
import androidx.core.net.toUri
import okhttp3.Dns
import org.mz.mzdkplayer.tool.Tools
import org.mz.mzdkplayer.tool.WebDavHttpClient
import org.mz.mzdkplayer.tool.WebDavHttpClient.Companion.restrictedTrustOkHttpClient
import java.net.Inet4Address
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

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
    private val webDavClient by  lazy{
        restrictedTrustOkHttpClient
    }
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

                        val client = OkHttpClient.Builder()
                            .connectTimeout(5, TimeUnit.SECONDS) // 缩短连接超时
                            .readTimeout(10, TimeUnit.SECONDS)
                            .writeTimeout(10, TimeUnit.SECONDS)
                            // 强制使用 IPv4 避免 IPv6 导致的 40 秒等待（如果怀疑是 IPv6 的锅）
                            .build()
                        sardine = OkHttpSardine()
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

                        // 根据设置决定是否移除第一个元素（通常是当前目录本身）
                        val resourcesToProcess = if (SettingsRepository.removeWebDavFirstItem && resources.isNotEmpty()) {
                            resources.drop(1)
                        } else {
                            resources
                        }

                        // 再过滤掉 "." 和 ".."
                        val filteredResources = resourcesToProcess.filter { it.name != "." && it.name != ".." }

                        // 构建 WebDavFileItem 列表并按名称排序
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
                        }.sortedBy { it.name }

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
            val uri = current.toUri()
            val path = uri.path ?: ""

            if (path.isEmpty() || path == "/") {
                return ""
            }

            // 去掉末尾的斜杠再找上级目录
            val cleanPath = path.trimEnd('/')
            val lastSlashIndex = cleanPath.lastIndexOf('/')
            
            return if (lastSlashIndex >= 0) {
                val parentPath = cleanPath.take(lastSlashIndex)
                "${uri.scheme}://${uri.host}${if (uri.port != -1) ":${uri.port}" else ""}${if (parentPath.isEmpty()) "/" else parentPath}"
            } else {
                "${uri.scheme}://${uri.host}${if (uri.port != -1) ":${uri.port}" else ""}/"
            }
        } catch (e: Exception) {
            Log.e("WebDavConViewModel", "获取父目录路径失败", e)
            return ""
        }
    }
//    fun buildProperFullUrl(
//        authenticatedBase: String,   // 当前文件夹的 authenticatedUrl
//        segment: String              // 文件名/文件夹名
//    ): String {
//        val base = authenticatedBase.trimEnd('/')
//        val encodedSegment = Tools.encodePathSegment(
//            segment.trimStart('/').trimEnd('/')
//        )
//        return "$base/$encodedSegment"
//    }
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

    suspend fun scanVideosRecursive(fullPath: String, maxDepth: Int, username: String?, password: String?): List<Pair<String, String>> = withContext(Dispatchers.IO) {
        val result = mutableListOf<Pair<String, String>>()
        if (sardine == null) return@withContext emptyList()

        suspend fun scanRecursive(currentPath: String, currentDepth: Int) {
            if (currentDepth > maxDepth) return

            try {
                val encodedPath = encodeWebDavPath(currentPath)
                val resources = sardine?.list(encodedPath) ?: return

                resources.forEach { resource ->
                    if (resource.name == "." || resource.name == "..") return@forEach

                    // sardine.list(path) 返回的第一个元素通常是 path 本身
                    // 我们通过比较 path 来跳过它。注意 resource.path 是绝对路径/完整路径。
                    // 这里的逻辑需要小心处理，因为 sardine 返回的 resource.path 可能是编码过的或带协议的。
                    // 简单起见，如果 resource.name 为空或与当前路径最后一部分相同（且不是第一个元素），我们可能需要跳过。
                    // 实际上，sardine 返回的第一个元素的 path 通常就是传入的 path。
                    // 更可靠的方式：如果 resource.name 为空（根目录）或者 resource.path 与 list 的路径一致，跳过
                    // 但是 resource.name 在 sardine 中通常是最后一部分。

                    // 一个常用的技巧是检查 resource.path 是否与当前目录 path 相同
                    // 这里我们尝试通过 resource.name 是否与 list 的路径末尾一致来判断（粗略）
                }

                // 重新审视：sardine.list(url) 返回 List<DavResource>，第一个是 url 本身。
                val filteredResources = if (resources.size > 1) resources.drop(1) else emptyList()

                filteredResources.forEach { resource ->
                    val fileName = resource.name
                    val isDir = resource.isDirectory
                    
                    // 使用更加健壮的方式构建完整 URL，避免 java.net.URI 在处理包含空格或中文字符的路径时崩溃
                    // resource.name 通常是未编码的名称，直接拼接在 currentPath 后面
                    val fullUrl = if (currentPath.endsWith("/")) {
                        "$currentPath$fileName"
                    } else {
                        "$currentPath/$fileName"
                    }

                    if (isDir) {
                        scanRecursive(fullUrl, currentDepth + 1)
                    } else if (Tools.containsVideoFormat(Tools.extractFileExtension(fileName))) {
                        // 构建带认证信息的 URL，并确保进行 URL 编码
                        val encodedFullUrl = encodeWebDavPath(fullUrl)
                        val authenticatedUrl = if (username != null && password != null) {
                            buildAuthenticatedUrl(encodedFullUrl, username, password)
                        } else {
                            encodedFullUrl
                        }
                        result.add(fileName to authenticatedUrl)
                    }
                }
            } catch (e: Exception) {
                Log.e("WebDavConViewModel", "Error scanning $currentPath", e)
            }
        }

        scanRecursive(fullPath, 0)
        result
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