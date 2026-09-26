package org.mz.mzdkplayer.viewmodel

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
import org.mz.mzdkplayer.tool.FileBrowserLogic
import java.io.IOException

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
        val parentUrl = FileBrowserLogic.httpParentUrl(baseUrl)
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
        return FileBrowserLogic.httpLogicalPath(baseUrl)
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
                        val filteredResources = resources.filter { !FileBrowserLogic.isHiddenDirEntry(it.name) }
                        _fileList.value = filteredResources
                        _connectionStatus.value = FileConnectionStatus.FilesLoaded
                    }
                    Log.d("HTTPLinkConViewModel", "列出文件成功: $fullUrl")
                } catch (e: Exception) {
                    Log.e("HTTPLinkConViewModel", "获取文件列表失败: $fullUrl", e)
                    _connectionStatus.value = FileConnectionStatus.Error("File listing failed: ${e.message}")
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

    private fun listDirectoryFromUrl(url: String): List<HTTPLinkResource> {
        val request = Request.Builder().url(url).build()
        val response: Response = okHttpClient.newCall(request).execute()

        if (!response.isSuccessful) {
            throw IOException("HTTP error code: ${response.code}")
        }

        val responseBody = response.body?.string()
        if (responseBody.isNullOrEmpty()) {
            Log.w("HTTPLinkConViewModel", "Empty response body for URL: $url")
            return emptyList()
        }

        return parseHtmlDirectoryListing(responseBody, url)
    }

    private fun parseHtmlDirectoryListing(html: String, baseUrl: String): List<HTTPLinkResource> {
        // 解析逻辑（正则、过滤、大小提取）已下沉到 FileBrowserLogic，便于单测
        return FileBrowserLogic.parseHttpDirectoryListing(html, baseUrl).map { entry ->
            HTTPLinkResource(
                name = entry.name,
                isDirectory = entry.isDirectory,
                path = entry.href, // 保留服务端原始链接，播放时再按当前目录解析
                fileSize = entry.size
            )
        }
    }

    private fun resolveUrl(relativeUrl: String, baseUrl: String): String {
        return FileBrowserLogic.resolveHttpUrl(relativeUrl, baseUrl)
    }

    private val okHttpClient = OkHttpClient()

    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch(Dispatchers.IO) {
            disconnectHTTPLink()
        }
    }

    suspend fun scanVideosRecursive(fullUrl: String, maxDepth: Int): List<Pair<String, String>> = withContext(Dispatchers.IO) {
        val result = mutableListOf<Pair<String, String>>()

        fun scanRecursive(currentUrl: String, currentDepth: Int) {
            if (currentDepth > maxDepth) return

            try {
                val normalizedUrl = if (currentUrl.endsWith("/")) currentUrl else "$currentUrl/"
                val resources = listDirectoryFromUrl(normalizedUrl)

                resources.forEach { resource ->
                    if (FileBrowserLogic.isHiddenDirEntry(resource.name)) return@forEach

                    val resourceUrl = resolveUrl(resource.path, normalizedUrl)

                    if (resource.isDirectory) {
                        scanRecursive(resourceUrl, currentDepth + 1)
                    } else if (org.mz.mzdkplayer.tool.Tools.containsVideoFormat(org.mz.mzdkplayer.tool.Tools.extractFileExtension(resource.name))) {
                        result.add(resource.name to resourceUrl)
                    }
                }
            } catch (e: Exception) {
                Log.e("HTTPLinkConViewModel", "Error scanning $currentUrl", e)
            }
        }

        scanRecursive(fullUrl, 0)
        result
    }
}
data class HTTPLinkResource(
    val name: String,
    val isDirectory: Boolean,
    val path: String ,// 这里可以是 href 值，如 "movie.mp4" 或 "subdir/"
    val fileSize : Long = 1L
)