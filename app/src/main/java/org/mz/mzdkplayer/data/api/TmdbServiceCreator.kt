package org.mz.mzdkplayer.data.api


import org.mz.mzdkplayer.BuildConfig
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object TmdbServiceCreator {

    private const val BASE_URL = "https://api.themoviedb.org/3/"

    // 可选：添加日志拦截器（仅 Debug）
//    private val loggingInterceptor = run {
//        if (BuildConfig.DEBUG) {
//            okhttp3.logging.HttpLoggingInterceptor().apply {
//                level = okhttp3.logging.HttpLoggingInterceptor.Level.BODY
//            }
//        } else null
//    }

    // 👇 核心：带 API Key 自动注入的拦截器
    private val apiKeyInterceptor = Interceptor { chain ->
        val originalUrl = chain.request().url
        val url = originalUrl.newBuilder()
            .addQueryParameter("api_key", BuildConfig.TMDB_API_KEY)
            .build()

        val request = chain.request().newBuilder()
            .url(url)
            .header("Accept", "application/json")
            .header("Content-Type", "application/json")
            .build()

        chain.proceed(request)
    }

    private val okHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(apiKeyInterceptor)
            .apply {

            }
            .build()
    }

    private val retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }


    fun <T> create(serviceClass: Class<T>): T = retrofit.create(serviceClass)
    inline fun <reified T> create(): T = create(T::class.java)
}