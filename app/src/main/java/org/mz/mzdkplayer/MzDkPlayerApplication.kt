package org.mz.mzdkplayer

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.cache.Cache
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import okhttp3.OkHttpClient
import org.mz.mzdkplayer.di.AppContext
import org.mz.mzdkplayer.data.repository.AudioPlaylistRepository
import org.mz.mzdkplayer.data.repository.PlaybackPreferenceRepository
import org.mz.mzdkplayer.data.repository.VideoPlaylistRepository
import org.mz.mzdkplayer.data.repository.SettingsRepository
import org.mz.mzdkplayer.di.RepositoryProvider
import org.mz.mzdkplayer.common.LanguageManager
import org.mz.mzdkplayer.tool.logic.DoubanLogic
import java.io.File
import java.util.concurrent.TimeUnit

@UnstableApi
class MzDkPlayerApplication: Application(), SingletonImageLoader.Factory {
    companion object {
        @SuppressLint("StaticFieldLeak")
        lateinit var context: Context
        lateinit var downloadCache: Cache
    }

    /**
     * 全局 Coil ImageLoader。
     *
     * 唯一的定制目的：**豆瓣图床的防盗链**。豆瓣图片在不带 `Referer` 时一律返回 HTTP 418
     * （14 字节占位图），海报/背景会全白，所以这里给豆瓣域名的图片请求补一个 Referer。
     * 只有豆瓣域名会被加头，TMDB 等其它图床走原样请求。
     *
     * 注册方式是 `components {}` 而不是给 Coil 传 HTTP 客户端（Coil3 没有这个入口）：
     * 用户注册的组件排在 Coil 自带组件**之前**（见 `RealImageLoader`：先 user components，
     * 再追加 ServiceLoader 的默认网络 Fetcher；`ComponentRegistry.newFetcher` 按顺序取第一个命中的），
     * 所以这里的 Fetcher 能稳定接管所有 http/https 图片。
     */
    override fun newImageLoader(context: PlatformContext): ImageLoader =
        ImageLoader.Builder(context)
            .components {
                add(OkHttpNetworkFetcherFactory(callFactory = { imageHttpClient }))
            }
            .build()

    /** 图片专用 OkHttp：只比默认多一个「豆瓣域名补 Referer」的拦截器 */
    private val imageHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val request = chain.request()
                if (DoubanLogic.isDoubanHost(request.url.host)) {
                    chain.proceed(
                        request.newBuilder()
                            .header("Referer", DoubanLogic.SEARCH_REFERER)
                            .build()
                    )
                } else {
                    chain.proceed(request)
                }
            }
            .build()
    }

    override fun onCreate() {
        super.onCreate()
        context = applicationContext
        // :core 里的业务层（如 MovieViewModel 读 NFO）通过它拿 Application Context
        AppContext.init(this)

        // 1. 初始化设置（最优先，因为其他组件可能依赖它）
        SettingsRepository.init(this)

        // 2. 应用语言设置
        LanguageManager.applyLanguage(this)

        // 3. 初始化数据库/仓库提供者
        RepositoryProvider.init(this)

        // 4. 初始化音频播放列表
        AudioPlaylistRepository.init(this)
        VideoPlaylistRepository.init(this)

        // 5. 初始化「按文件记住播放偏好」的存储
        PlaybackPreferenceRepository.init(this)

        val cacheDir = File(filesDir, "exoplayer_cache")
        val evictor = LeastRecentlyUsedCacheEvictor(5000 * 1024 * 1024)
        val databaseProvider = StandaloneDatabaseProvider(this)
        downloadCache = SimpleCache(cacheDir, evictor, databaseProvider)
    }
}