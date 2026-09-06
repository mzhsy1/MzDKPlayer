package org.mz.mzdkplayer

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.cache.Cache
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import org.mz.mzdkplayer.data.repository.AudioPlaylistRepository
import org.mz.mzdkplayer.data.repository.VideoPlaylistRepository
import org.mz.mzdkplayer.data.repository.SettingsRepository
import org.mz.mzdkplayer.di.RepositoryProvider
import org.mz.mzdkplayer.tool.LanguageManager
import java.io.File

@UnstableApi
class MzDkPlayerApplication: Application() {
    companion object {
        @SuppressLint("StaticFieldLeak")
        lateinit var context: Context
        lateinit var downloadCache: Cache
    }

    override fun onCreate() {
        super.onCreate()
        context = applicationContext

        // 1. 初始化设置（最优先，因为其他组件可能依赖它）
        SettingsRepository.init(this)

        // 2. 应用语言设置
        LanguageManager.applyLanguage(this)

        // 3. 初始化数据库/仓库提供者
        RepositoryProvider.init(this)

        // 4. 初始化音频播放列表
        AudioPlaylistRepository.init(this)
        VideoPlaylistRepository.init(this)

        val cacheDir = File(filesDir, "exoplayer_cache")
        val evictor = LeastRecentlyUsedCacheEvictor(5000 * 1024 * 1024)
        val databaseProvider = StandaloneDatabaseProvider(this)
        downloadCache = SimpleCache(cacheDir, evictor, databaseProvider)
    }
}