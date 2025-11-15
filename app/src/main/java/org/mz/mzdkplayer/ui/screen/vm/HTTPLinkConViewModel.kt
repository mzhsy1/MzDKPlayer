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
import org.mz.mzdkplayer.data.model.FileConnectionStatus
import java.io.IOException
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.regex.Pattern

class HTTPLinkConViewModel : ViewModel() {

    private val _connectionStatus: MutableStateFlow<FileConnectionStatus> = MutableStateFlow(FileConnectionStatus.Disconnected)
    val connectionStatus: StateFlow<FileConnectionStatus> = _connectionStatus

    private val _fileList: MutableStateFlow<List<HTTPLinkResource>> = MutableStateFlow(emptyList())
    val fileList: StateFlow<List<HTTPLinkResource>> = _fileList

    // 注意：现在 baseUrl 是完整的目标目录 URL，例如 "http://.../nas/movies/action/"
    private var baseUrl: String = ""
    private val mutex = Mutex()

    /**
     * 连接到指定的完整目录 URL
     */
    fun connectToHTTPLink(fullDirectoryUrl: String)
    {
        viewModelScope.launch {
            mutex.withLock {
                _connectionStatus.value = FileConnectionStatus.Connecting
                try {
                    withContext(Dispatchers.IO) {
                        val normalizedUrl = fullDirectoryUrl.trimEnd('/') + "/"
                        Log.d("HTTPLinkConViewModel", "连接到完整目录: $normalizedUrl")
                        this@HTTPLinkConViewModel.baseUrl = normalizedUrl
                        _connectionStatus.value = FileConnectionStatus.Connected
                        listFiles(normalizedUrl)
                    }
                } catch (e: Exception) {
                    Log.e("HTTPLinkConViewModel", "连接失败", e)
                    _connectionStatus.value = FileConnectionStatus.Error("连接失败: ${e.message}")
                }
            }
        }
    }

    /**
     * 切换到子目录（传入的是子目录名，不是完整路径！）
     */
    fun navigateToSubdirectory(dirName: String) {
        val newUrl = "${baseUrl.trimEnd('/')}/${dirName}/"
        connectToHTTPLink(newUrl) // 递归复用连接逻辑
    }

    /**
     * 返回上一级目录
     */
    fun navigateToParent() {
        val parentUrl = getParentUrl(baseUrl)
        if (parentUrl == baseUrl) {
            // 已在根目录，无法再返回
            Log.w("HTTPLinkConViewModel", "已在根目录，无法返回上级")
            return
        }
        connectToHTTPLink(parentUrl)
    }

    /**
     * 获取当前逻辑路径（用于 UI 显示，从 baseUrl 推导）
     */
    fun getCurrentLogicalPath(): String {
        return extractLogicalPath(baseUrl)
    }

    /**
     * 内部列出文件方法，直接使用完整 URL
     */
    fun listFiles(fullUrl: String) {
        viewModelScope.launch {

            _connectionStatus.value = FileConnectionStatus.LoadingFile
            mutex.withLock {
                try {
                    withContext(Dispatchers.IO) {
                        val resources = listDirectoryFromUrl(fullUrl)
                        val filteredResources = resources.filter { it.name != "." && it.name != ".." }
                        _fileList.value = filteredResources
                        _connectionStatus.value = FileConnectionStatus.FilesLoaded
                    }
                    Log.d("HTTPLinkConViewModel", "列出文件成功: $fullUrl")
                } catch (e: Exception) {
                    Log.e("HTTPLinkConViewModel", "获取文件列表失败: $fullUrl", e)
                    _connectionStatus.value = FileConnectionStatus.Error("获取文件失败: ${e.message}")
                }
            }
        }
    }

    fun disconnectHTTPLink() {
        viewModelScope.launch(Dispatchers.IO) {
            mutex.withLock {
                try {
                    baseUrl = ""
                } finally {
                    withContext(Dispatchers.Main) {
                        _connectionStatus.value = FileConnectionStatus.Disconnected
                        _fileList.value = emptyList()
                    }
                }
            }
        }
    }

    fun isConnected(): Boolean {
        return _connectionStatus.value == FileConnectionStatus.Connected ||
                _connectionStatus.value == FileConnectionStatus.FilesLoaded ||
                _connectionStatus.value is FileConnectionStatus.LoadingFile
    }

    fun getResourceFullUrl(resourceName: String): String {
        return "${baseUrl.trimEnd('/')}/$resourceName"
    }

    // --- 工具方法 ---

    /**
     * 从完整 URL 提取逻辑路径（用于显示）
     * 例如: http://x/nas/movies/action/ → /movies/action
     */
    private fun extractLogicalPath(fullUrl: String): String {
        try {
            val urlObj = java.net.URL(fullUrl)
            val path = urlObj.path
            // 假设服务器根是固定的，但我们不知道，所以只能返回整个 path
            // 如果你知道服务器根（如 /nas/），可以在这里裁剪
            return path.trim('/').let { if (it.isEmpty()) "" else "/$it" }
        } catch (e: Exception) {
            Log.e("HTTPLinkConViewModel", "解析路径失败: $fullUrl", e)
            return ""
        }
    }

    /**
     * 获取父级完整 URL
     */
    private fun getParentUrl(currentUrl: String): String {
        try {
            val urlObj = java.net.URL(currentUrl)
            val path = urlObj.path
            if (path == "/" || path.count { it == '/' } <= 1) {
                return currentUrl // 已在根
            }
            val parentPath = path.trimEnd('/').substringBeforeLast("/", "")
            val parentUrlStr = if (parentPath.isEmpty()) {
                "${urlObj.protocol}://${urlObj.host}${if (urlObj.port != -1) ":${urlObj.port}" else ""}/"
            } else {
                "${urlObj.protocol}://${urlObj.host}${if (urlObj.port != -1) ":${urlObj.port}" else ""}$parentPath/"
            }
            return parentUrlStr
        } catch (e: Exception) {
            Log.e("HTTPLinkConViewModel", "获取父目录失败: $currentUrl", e)
            return currentUrl
        }
    }

    // --- 保留原有解析逻辑 ---
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

    private fun parseHtmlDirectoryListing(html: String, baseUrl: String): List<HTTPLinkResource> {
        val resources = mutableListOf<HTTPLinkResource>()
        val linkPattern = Pattern.compile(
            "<a\\s+[^>]*href\\s*=\\s*[\"']([^\"']*)[\"'][^>]*>([^<]*)</a>",
            Pattern.CASE_INSENSITIVE or Pattern.DOTALL
        )
        val matcher = linkPattern.matcher(html)

        while (matcher.find()) {
            var href = matcher.group(1)
            href = URLDecoder.decode(href, StandardCharsets.UTF_8.name())

            if (href != null && !href.startsWith("#") && !href.startsWith("javascript:") && !href.startsWith("mailto:")) {
                val fullHref = resolveUrl(href, baseUrl)
                if (isSubPathOf(fullHref, baseUrl)) {
                    val isDirectory = href.endsWith("/")
                    val cleanHref = href.trimEnd('/')
                    val name = cleanHref.substringAfterLast("/", cleanHref)
                    val finalName = name.trimEnd('/')

                    // 注意：现在 path 字段可以存 href（或 relative path），但 UI 可能不需要
                    if (finalName != ".." && finalName != ".") {
                        resources.add(HTTPLinkResource(finalName, isDirectory, href))
                    }
                }
            }
        }

        return resources.distinctBy { it.name }
    }

    private fun isSubPathOf(url: String, baseUrl: String): Boolean {
        try {
            val baseUrlObj = java.net.URL(baseUrl)
            val urlObj = java.net.URL(url)
            if (urlObj.protocol != baseUrlObj.protocol || urlObj.host != baseUrlObj.host || urlObj.port != baseUrlObj.port) {
                return false
            }
            return urlObj.path.startsWith(baseUrlObj.path)
        } catch (e: Exception) {
            return false
        }
    }

    private fun resolveUrl(relativeUrl: String, baseUrl: String): String {
        return java.net.URL(java.net.URL(baseUrl), relativeUrl).toString()
    }

    private val okHttpClient = OkHttpClient()

    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch(Dispatchers.IO) {
            disconnectHTTPLink()
        }
    }
}

data class HTTPLinkResource(
    val name: String,
    val isDirectory: Boolean,
    val path: String // 这里可以是 href 值，如 "movie.mp4" 或 "subdir/"
)