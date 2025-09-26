package org.mz.mzdkplayer.ui.screen.vm

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
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okio.Buffer
import java.io.IOException
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.regex.Pattern

class HTTPLinkConViewModel : ViewModel() {

    // 明确指定 MutableStateFlow 的泛型类型
    private val _connectionStatus: MutableStateFlow<HTTPLinkConnectionStatus> = MutableStateFlow(HTTPLinkConnectionStatus.Disconnected)
    val connectionStatus: StateFlow<HTTPLinkConnectionStatus> = _connectionStatus

    // 存储文件/文件夹列表
    private val _fileList: MutableStateFlow<List<HTTPLinkResource>> = MutableStateFlow(emptyList())
    val fileList: StateFlow<List<HTTPLinkResource>> = _fileList

    // 存储当前工作目录的路径 (相对于根URL)
    private val _currentPath: MutableStateFlow<String> = MutableStateFlow("")
    val currentPath: StateFlow<String> = _currentPath

    private var okHttpClient: OkHttpClient = OkHttpClient()
    private var baseUrl: String = "" // 存储基础 HTTP Link URL

    private val mutex = Mutex() // 用于协程同步

    /**
     * 连接到 HTTP Link 服务器
     * @param baseUrl HTTP Link 服务器的基础 URL (e.g., "http://192.168.1.4:81/movies/")
     */
    fun connectToHTTPLink(baseUrl: String) {
        viewModelScope.launch {
            mutex.withLock {
                _connectionStatus.value = HTTPLinkConnectionStatus.Connecting
                try {
                    withContext(Dispatchers.IO) {
                        val trimmedBaseUrl = baseUrl.trimEnd('/')
                        this@HTTPLinkConViewModel.baseUrl = trimmedBaseUrl
                        // 尝试访问根目录以验证连接
                        val rootResources = listDirectoryFromUrl(trimmedBaseUrl)
                        _fileList.value = rootResources.filter { it.name != ".." && it.name != "." }
                    }
                    _currentPath.value = "" // 重置路径
                    _connectionStatus.value = HTTPLinkConnectionStatus.Connected
                    Log.d("HTTPLinkConViewModel", "连接成功到 $baseUrl")
                } catch (e: Exception) {
                    Log.e("HTTPLinkConViewModel", "连接失败", e)
                    _connectionStatus.value = HTTPLinkConnectionStatus.Error("连接失败: ${e.message}")
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
            if (_connectionStatus.value != HTTPLinkConnectionStatus.Connected) {
                _connectionStatus.value = HTTPLinkConnectionStatus.Error("未连接到服务器")
                return@launch
            }

            mutex.withLock {
                try {
                    withContext(Dispatchers.IO) {
                        val resources = listDirectoryFromUrl(fullUrl)
                        // 过滤掉 "." 和 ".."
                        val filteredResources = resources.filter { it.name != "." && it.name != ".." }
                        _fileList.value = filteredResources
                        _currentPath.value = effectivePath // 更新当前路径
                    }
                    Log.d("HTTPLinkConViewModel", "列出文件成功: $fullUrl")
                } catch (e: Exception) {
                    Log.e("HTTPLinkConViewModel", "获取文件列表失败: $fullUrl", e)
                    _connectionStatus.value = HTTPLinkConnectionStatus.Error("获取文件失败: ${e.message}")
                }
            }
        }
    }

    /**
     * 断开与 HTTP Link 服务器的连接
     */
    fun disconnectHTTPLink() {
        viewModelScope.launch(Dispatchers.IO) {
            mutex.withLock {
                try {
                    // OkHttpClient 通常不需要显式关闭连接，它会管理底层连接
                    // 但我们可以将 baseUrl 置空
                    baseUrl = ""
                } catch (e: Exception) {
                    Log.w("HTTPLinkConViewModel", "断开连接时发生异常", e)
                } finally {
                    // 确保在 Main 线程更新状态
                    withContext(Dispatchers.Main) {
                        _connectionStatus.value = HTTPLinkConnectionStatus.Disconnected
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
        return _connectionStatus.value == HTTPLinkConnectionStatus.Connected
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

    /**
     * 从指定 URL 获取目录列表
     * @param url 目录的 URL
     * @return 解析出的资源列表
     */
    private fun listDirectoryFromUrl(url: String): List<HTTPLinkResource> {
        val request = Request.Builder().url(url).build()
        val response: Response = okHttpClient.newCall(request).execute()

        if (!response.isSuccessful) {
            throw IOException("HTTP error code: ${response.code}")
        }

        val responseBody = response.body?.string()
        if (responseBody == null || responseBody.isEmpty()) {
            Log.w("HTTPLinkConViewModel", "Empty response body for URL: $url")
            return emptyList()
        }

        return parseHtmlDirectoryListing(responseBody, url)
    }

    /**
     * 解析 HTML 目录列表页面
     * @param html HTML 内容
     * @param baseUrl 当前目录的基础 URL
     * @return 解析出的资源列表
     */
    private fun parseHtmlDirectoryListing(html: String, baseUrl: String): List<HTTPLinkResource> {
        val resources = mutableListOf<HTTPLinkResource>()
        val baseUrlObj = java.net.URL(baseUrl)
        val protocol = baseUrlObj.protocol
        val host = baseUrlObj.host
        val port = if (baseUrlObj.port != -1) baseUrlObj.port else baseUrlObj.defaultPort
        val baseDir = baseUrlObj.path

        // 正则表达式匹配 href 属性和链接文本
        // 匹配 <a> 标签内的 href 和文本内容
        val linkPattern = Pattern.compile(
            "<a\\s+[^>]*href\\s*=\\s*[\"']([^\"']*)[\"'][^>]*>([^<]*)</a>",
            Pattern.CASE_INSENSITIVE or Pattern.DOTALL
        )
        val matcher = linkPattern.matcher(html)

        while (matcher.find()) {
            var href = matcher.group(1)
            var linkText = matcher.group(2)?.trim() ?: ""

            // 解码 URL 编码的 href 和 linkText
            href = URLDecoder.decode(href, StandardCharsets.UTF_8.name())
            linkText = URLDecoder.decode(linkText, StandardCharsets.UTF_8.name())

            if (href != null && !href.startsWith("#") && !href.startsWith("javascript:") && !href.startsWith("mailto:")) {
                val fullHref = resolveUrl(href, baseUrl)
                if (fullHref.startsWith("$protocol://$host:$port") && fullHref.startsWith(baseUrl)) {
                    // 计算相对于 baseUrl 的路径
                    val relativePath = fullHref.substring(baseUrl.length).trim('/')

                    // 判断是否为目录：href 以 '/' 结尾，或者在 linkText 中有明显的目录标记（如 / 或 >）
                    val isDirectory = href.endsWith("/") || linkText.endsWith("/") || linkText.contains(">")

                    // 提取显示名称
                    var name = linkText
                    // 如果 linkText 为空或与 href 相同，使用 href 的最后部分作为名称
                    if (name.isEmpty() || name == href) {
                        name = href.substringAfterLast("/", href).trimEnd('/')
                    }
                    // 如果名称过长，截取一部分（例如，保留前 50 个字符并添加省略号）
                    if (name.length > 50) {
                        name = name.take(50) + "..."
                    }

                    // 过滤掉 "../" 这种导航链接
                    if (relativePath != ".." && href != "../") {
                        resources.add(HTTPLinkResource(name, isDirectory, relativePath))
                    }
                }
            }
        }

        return resources.distinctBy { it.name } // 去重
    }

    /**
     * 根据基础 URL 解析相对 URL
     * @param relativeUrl 相对 URL 或绝对 URL
     * @param baseUrl 基础 URL
     * @return 完整的 URL
     */
    private fun resolveUrl(relativeUrl: String, baseUrl: String): String {
        val base = java.net.URL(baseUrl)
        val resolved = java.net.URL(base, relativeUrl)
        return resolved.toString()
    }

    override fun onCleared() {
        super.onCleared()
        // ViewModel 被清除时确保断开连接
        viewModelScope.launch(Dispatchers.IO) {
            disconnectHTTPLink()
        }
    }
}

// --- 状态枚举 ---
sealed class HTTPLinkConnectionStatus {
    object Disconnected : HTTPLinkConnectionStatus()
    object Connecting : HTTPLinkConnectionStatus()
    object Connected : HTTPLinkConnectionStatus()
    data class Error(val message: String) : HTTPLinkConnectionStatus()

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

// --- 资源数据类 ---
data class HTTPLinkResource(
    val name: String,
    val isDirectory: Boolean,
    val path: String // 相对于当前目录的路径
)



