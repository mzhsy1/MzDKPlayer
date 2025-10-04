package org.mz.mzdkplayer

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import androidx.media3.common.util.UnstableApi

import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.cache.Cache
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
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
        val cacheDir = File(filesDir, "exoplayer_cache")
        val evictor = LeastRecentlyUsedCacheEvictor(5000 * 1024 * 1024) // 1000MB
        // 3. 数据库提供者（关键！替换弃用构造函数）
        val databaseProvider = StandaloneDatabaseProvider(this)
        downloadCache = SimpleCache(cacheDir, evictor,databaseProvider)
    }
}