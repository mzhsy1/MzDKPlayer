package org.mz.mzdkplayer.data.api

import org.mz.mzdkplayer.core.BuildConfig
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import org.mz.mzdkplayer.data.repository.SettingsRepository
import java.io.IOException
import java.util.concurrent.TimeUnit

object TmdbServiceCreator {

    /**
     * 带 API Key 自动注入的拦截器，顺带把每次请求的响应状态与耗时打进日志。
     *
     * 这里**不做熔断**（曾经做过一轮：连续网络失败就把这个源短路一段时间）。去掉的原因是它会误伤 ——
     * 一次网络抖动就能让随后几分钟内本来能成功的请求直接失败；而刮削本来就是「一个文件一两次请求」
     * 的低频操作，靠超时值（[TIMEOUT_SECONDS]）兜住体感就够了。
     */
    private val apiKeyInterceptor = Interceptor { chain ->
        val originalUrl = chain.request().url
        val url = originalUrl.newBuilder()
            .addQueryParameter("api_key", BuildConfig.TMDB_API_KEY)
            .build()

        android.util.Log.d("TmdbService", "Requesting URL: $url")

        val request = chain.request().newBuilder()
            .url(url)
            .header("Accept", "application/json")
            .header("Content-Type", "application/json")
            .header("User-Agent", "MzDKPlayer/${BuildConfig.APP_VERSION_NAME} (Android)")
            .build()

        val start = System.currentTimeMillis()
        try {
            chain.proceed(request).also { response ->
                android.util.Log.d(
                    "TmdbService",
                    "[SCRAPE] TMDB 响应 HTTP ${response.code} (${System.currentTimeMillis() - start}ms)"
                )
            }
        } catch (e: IOException) {
            android.util.Log.w(
                "TmdbService",
                "[SCRAPE] TMDB 请求失败（${System.currentTimeMillis() - start}ms）: ${e.message}"
            )
            throw e
        }
    }

    /**
     * 基础 client：只挂 API Key 拦截器。
     *
     * 超时**不在这里定**：它按设置动态取值（`ScrapeTimeoutPolicy`，默认 5 秒），由 [getRetrofit]
     * 用 [OkHttpClient.newBuilder] 克隆一份带上当前超时 —— 这样用户在设置里改完超时不必重启应用。
     * 克隆共享连接池与线程池，开销可以忽略。
     */
    private val baseClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(apiKeyInterceptor)
            .build()
    }

    /**
     * 因为 BASE_URL 可能在设置中改变，所以我们不能使用 lazy retrofit。
     * 每次请求时获取当前的 BASE_URL。
     * 虽然频繁创建 Retrofit 实例有一定开销，但对于刮削这种非高频操作是可以接受的。
     * 或者我们可以缓存实例，当 URL 改变时清除。
     *
     * **超时同理**：设置里的「刮削超时时间」改了以后也要立刻生效，所以把它一起纳入缓存 key
     * （变化时重建 Retrofit，并用当前超时克隆一份 client）。
     */
    private var currentRetrofit: Retrofit? = null
    private var currentBaseUrl: String? = null
    private var currentTimeoutSeconds: Int = -1

    @Synchronized
    private fun getRetrofit(): Retrofit {
        val baseUrl = SettingsRepository.tmdbBaseUrl
        val timeoutSeconds = SettingsRepository.scrapeTimeoutSeconds
        if (baseUrl != currentBaseUrl ||
            timeoutSeconds != currentTimeoutSeconds ||
            currentRetrofit == null
        ) {
            currentBaseUrl = baseUrl
            currentTimeoutSeconds = timeoutSeconds
            currentRetrofit = Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(
                    baseClient.newBuilder()
                        .connectTimeout(timeoutSeconds.toLong(), TimeUnit.SECONDS)
                        .readTimeout(timeoutSeconds.toLong(), TimeUnit.SECONDS)
                        .writeTimeout(timeoutSeconds.toLong(), TimeUnit.SECONDS)
                        .build()
                )
                .addConverterFactory(GsonConverterFactory.create())
                .build()
        }
        return currentRetrofit!!
    }

    fun <T> create(serviceClass: Class<T>): T = getRetrofit().create(serviceClass)
    inline fun <reified T> create(): T = create(T::class.java)
}
