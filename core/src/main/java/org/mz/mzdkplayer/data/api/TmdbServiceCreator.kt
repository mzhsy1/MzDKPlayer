package org.mz.mzdkplayer.data.api

import org.mz.mzdkplayer.core.BuildConfig
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import org.mz.mzdkplayer.data.repository.SettingsRepository
import java.util.concurrent.TimeUnit

object TmdbServiceCreator {

    // 👇 核心：带 API Key 自动注入的拦截器
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

        chain.proceed(request)
    }

    private val okHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(apiKeyInterceptor)
            .build()
    }

    /**
     * 因为 BASE_URL 可能在设置中改变，所以我们不能使用 lazy retrofit。
     * 每次请求时获取当前的 BASE_URL。
     * 虽然频繁创建 Retrofit 实例有一定开销，但对于刮削这种非高频操作是可以接受的。
     * 或者我们可以缓存实例，当 URL 改变时清除。
     */
    private var currentRetrofit: Retrofit? = null
    private var currentBaseUrl: String? = null

    @Synchronized
    private fun getRetrofit(): Retrofit {
        val baseUrl = SettingsRepository.tmdbBaseUrl
        if (baseUrl != currentBaseUrl || currentRetrofit == null) {
            currentBaseUrl = baseUrl
            currentRetrofit = Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
        }
        return currentRetrofit!!
    }

    fun <T> create(serviceClass: Class<T>): T = getRetrofit().create(serviceClass)
    inline fun <reified T> create(): T = create(T::class.java)
}
