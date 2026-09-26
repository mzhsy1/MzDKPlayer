package org.mz.mzdkplayer.tool

import android.util.Log
import fi.iki.elonen.NanoHTTPD
import okhttp3.Credentials
import okhttp3.Request
import java.io.IOException
import java.io.InputStream
import java.net.URLDecoder
import org.mz.mzdkplayer.tool.Tools.fromBase64
import androidx.core.net.toUri

/**
 * 本地 HTTP 代理服务器，用于解决 LibVLC 对某些协议（如 WebDAV）支持不佳的问题。
 */
class LocalProxyServer(port: Int) : NanoHTTPD("127.0.0.1", port) {

    companion object {
        private const val TAG = "LocalProxyServer"
        private val okHttpClient by lazy { WebDavHttpClient.restrictedTrustOkHttpClient }
    }

    override fun serve(session: IHTTPSession): Response {
        val uri = session.uri
        if (uri != "/proxy") {
            return newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_PLAINTEXT, "Not Found")
        }

        val params = session.parameters
        val encodedUrl = params["url"]?.get(0) ?: return newFixedLengthResponse(Response.Status.BAD_REQUEST, MIME_PLAINTEXT, "Missing url")
        
        // 解码真实的 URL (之前可能是 Base64 或 URL 编码)
        // 建议使用 Base64 传递，因为 URL 可能包含特殊字符导致解析混乱
        val targetUrl = try {
            // 这里我们先尝试用 Tools.fromBase64，如果不是 Base64 可能会报错或返回空
            val decoded = encodedUrl.fromBase64()
            if (decoded.isBlank()) URLDecoder.decode(encodedUrl, "UTF-8") else decoded
        } catch (e: Exception) {
            URLDecoder.decode(encodedUrl, "UTF-8")
        }

        val rangeHeader = session.headers["range"]
        Log.d(TAG, "Proxying request for: $targetUrl, Range: $rangeHeader")

        return try {
            proxyRequest(targetUrl, rangeHeader, session)
        } catch (e: Exception) {
            Log.e(TAG, "Proxy error: ${e.message}", e)
            newFixedLengthResponse(Response.Status.INTERNAL_ERROR, MIME_PLAINTEXT, "Proxy Error: ${e.message}")
        }
    }

    private fun proxyRequest(url: String, range: String?, session: IHTTPSession): Response {
        val requestBuilder = Request.Builder().url(url)
        
        // 提取凭证 (如果有)
        val uri = url.toUri()
        val userInfo = uri.userInfo
        if (!userInfo.isNullOrBlank()) {
            val parts = userInfo.split(":")
            if (parts.size == 2) {
                requestBuilder.header("Authorization", Credentials.basic(parts[0], parts[1]))
            }
        }

        if (range != null) {
            requestBuilder.header("Range", range)
        }

        val okResponse = okHttpClient.newCall(requestBuilder.build()).execute()
        if (!okResponse.isSuccessful && okResponse.code != 206) {
            val code = okResponse.code
            okResponse.close()
            return newFixedLengthResponse(Response.Status.lookup(code) ?: Response.Status.INTERNAL_ERROR, MIME_PLAINTEXT, "Remote server returned $code")
        }

        val responseBody = okResponse.body ?: throw IOException("Empty response body")
        val contentType = okResponse.header("Content-Type") ?: "video/octet-stream"
        val contentLength = responseBody.contentLength()
        
        // 构造响应
        val status = if (okResponse.code == 206) Response.Status.PARTIAL_CONTENT else Response.Status.OK
        
        // 使用 newFixedLengthResponse 而不是 newChunkedResponse，因为我们知道长度
        // 这对播放器定位非常重要
        val response = newFixedLengthResponse(status, contentType, ProxyInputStream(okResponse), contentLength)
        
        // 传递必要的头部
        okResponse.headers.toMultimap().forEach { (name, values) ->
            if (name.equals("Content-Range", ignoreCase = true) || 
                name.equals("Accept-Ranges", ignoreCase = true)) {
                response.addHeader(name, values.joinToString(", "))
            }
        }

        return response
    }

    /**
     * 包装 InputStream，确保在关闭时同时也关闭 OkHttp 的 Response
     */
    private class ProxyInputStream(private val okResponse: okhttp3.Response) : InputStream() {
        private val source = okResponse.body?.byteStream() ?: throw IOException("Null body stream")

        override fun read(): Int = source.read()
        override fun read(b: ByteArray): Int = source.read(b)
        override fun read(b: ByteArray, off: Int, len: Int): Int = source.read(b, off, len)
        override fun available(): Int = source.available()
        override fun close() {
            source.close()
            okResponse.close()
        }
    }
}
