package org.mz.mzdkplayer

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.cache.Cache
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import org.mz.mzdkplayer.data.model.AudioItem
import java.io.File

@UnstableApi
class MzDkPlayerApplication: Application() {
    companion object {
        @SuppressLint("StaticFieldLeak")
        lateinit var context: Context
        lateinit var downloadCache: Cache

        // 为不同类型的共享数据使用不同的 Map
        private val stringListMap = mutableMapOf<String, List<AudioItem>>()
        private val stringMap = mutableMapOf<String, String>()
        private val intMap = mutableMapOf<String, Int>()

        fun setStringList(key: String, list: List<AudioItem>) {
            stringListMap[key] = list
        }

        fun getStringList(key: String): List<AudioItem> {
            return stringListMap[key] ?: emptyList()
        }

        fun clearStringList(key: String) {
            stringListMap.remove(key)
        }
    }

    override fun onCreate() {
        super.onCreate()
        context = applicationContext
        val cacheDir = File(filesDir, "exoplayer_cache")
        val evictor = LeastRecentlyUsedCacheEvictor(5000 * 1024 * 1024)
        val databaseProvider = StandaloneDatabaseProvider(this)
        downloadCache = SimpleCache(cacheDir, evictor, databaseProvider)
    }
}