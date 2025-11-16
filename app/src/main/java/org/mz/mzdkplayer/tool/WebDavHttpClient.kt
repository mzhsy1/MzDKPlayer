package org.mz.mzdkplayer.tool

import android.annotation.SuppressLint
import okhttp3.OkHttpClient
import java.security.SecureRandom
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

class WebDavHttpClient {

    companion object {
        // 单例实例，可以在任何地方调用
        val restrictedTrustOkHttpClient: OkHttpClient by lazy {
            createRestrictedTrustClient()
        }

        private fun createRestrictedTrustClient(): OkHttpClient {
            return try {
                val trustManager = createRestrictedTrustManager()
                val sslContext = SSLContext.getInstance("TLS")
                sslContext.init(null, arrayOf<TrustManager>(trustManager), SecureRandom())

                OkHttpClient.Builder()
                    .sslSocketFactory(sslContext.socketFactory, trustManager)
                    .hostnameVerifier { hostname, session ->
                        // 严格的主机名验证 - 只允许局域网IP
                        isValidHostname(hostname)
                    }
                    .connectTimeout(10, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(10, TimeUnit.SECONDS)
                    .followRedirects(true)
                    .followSslRedirects(true)
                    .build()
            } catch (e: Exception) {
                throw RuntimeException("Failed to create restricted trust HTTP client", e)
            }
        }

        @SuppressLint("CustomX509TrustManager")
        private fun createRestrictedTrustManager(): X509TrustManager {
            return object : X509TrustManager {
                override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {
                    // 客户端证书验证 - 如果需要可以添加具体逻辑
                    if (chain.isEmpty()) {
                        throw CertificateException("No client certificate found")
                    }
                }

                override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {
                    if (chain.isEmpty()) {
                        throw CertificateException("No server certificate found")
                    }

                    try {
                        // 基础验证：检查证书是否在有效期内
                        val certificate = chain[0]
                        certificate.checkValidity()

                        // 可以在这里添加额外的证书验证逻辑
                        // 例如：检查证书主题、颁发者等


                    } catch (e: Exception) {
                        throw CertificateException("Certificate validation failed: ${e.message}")
                    }
                }

                override fun getAcceptedIssuers(): Array<X509Certificate> {
                    return arrayOf()
                }
            }
        }

        /**
         * 验证主机名是否在允许的局域网范围内
         * @param hostname 要验证的主机名或IP地址
         * @return 如果主机名在允许范围内返回true，否则返回false
         */
        fun isValidHostname(hostname: String): Boolean {
            return when {
                // 192.168.x.x
                hostname.matches(Regex("^192\\.168\\.(\\d{1,3})\\.(\\d{1,3})$")) -> {
                    val parts = hostname.split(".")
                    parts[2].toInt() in 0..255 && parts[3].toInt() in 0..255
                }
                // 10.x.x.x
                hostname.matches(Regex("^10\\.(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})$")) -> {
                    val parts = hostname.split(".")
                    parts[1].toInt() in 0..255 && parts[2].toInt() in 0..255 && parts[3].toInt() in 0..255
                }
                // 172.16.x.x - 172.31.x.x
                hostname.matches(Regex("^172\\.(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})$")) -> {
                    val parts = hostname.split(".")
                    parts[1].toInt() in 16..31 && parts[2].toInt() in 0..255 && parts[3].toInt() in 0..255
                }
                // 本地地址
                hostname == "localhost" || hostname == "127.0.0.1" -> true
                // 其他情况一律拒绝
                else -> false
            }
        }
    }
}