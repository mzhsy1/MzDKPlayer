package org.mz.mzdkplayer.tool

import android.util.Log
import org.mz.mzdkplayer.tool.Tools.toBase64

object ProxyManager {
    private const val TAG = "ProxyManager"
    private var proxyServer: LocalProxyServer? = null
    private var proxyPort: Int = 8081 // 默认代理端口

    fun startProxy(): Int {
        if (proxyServer == null) {
            try {
                proxyServer = LocalProxyServer(proxyPort)
                proxyServer?.start()
                Log.i(TAG, "LocalProxyServer started on port $proxyPort")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start LocalProxyServer on port $proxyPort, trying another", e)
                // 简单尝试下一个端口
                proxyPort++
                return startProxy()
            }
        }
        return proxyPort
    }

    fun stopProxy() {
        proxyServer?.stop()
        proxyServer = null
    }

    fun getProxyUrl(originalUrl: String): String {
        val port = startProxy()
        return "http://127.0.0.1:$port/proxy?url=${originalUrl.toBase64()}"
    }

    /**
     * 判断是否需要通过代理播放
     */
    fun shouldProxy(url: String, dataSourceType: String): Boolean {
        val lowerUrl = url.lowercase()
        // 目前主要针对 WebDAV 的 ISO 文件，或者所有 WebDAV 也可以尝试
        return (dataSourceType == "WEBDAV" || lowerUrl.startsWith("http")) && 
                (lowerUrl.endsWith(".iso") || lowerUrl.endsWith(".iso/"))
    }
}
