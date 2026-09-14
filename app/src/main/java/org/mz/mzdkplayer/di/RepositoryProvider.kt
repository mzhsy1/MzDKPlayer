package org.mz.mzdkplayer.di



import android.content.Context
import org.mz.mzdkplayer.data.local.AppDatabase
import org.mz.mzdkplayer.data.repository.TmdbRepository
import org.mz.mzdkplayer.data.repository.RoomMediaHistoryRepository // 👈 记得导入新的 Repository
import org.mz.mzdkplayer.ui.screen.vm.AudioViewModel
import org.mz.mzdkplayer.ui.screen.vm.MediaHistoryViewModel
import org.mz.mzdkplayer.ui.screen.vm.MediaLibraryViewModel
import org.mz.mzdkplayer.ui.screen.vm.MediaMetaViewModel
import org.mz.mzdkplayer.ui.screen.vm.MovieViewModel
import org.mz.mzdkplayer.ui.screen.vm.PerformanceTestViewModel
import org.mz.mzdkplayer.ui.screen.vm.SearchViewModel

object RepositoryProvider {

    private val tmdbRepository = TmdbRepository.instance
    private var database: AppDatabase? = null

    fun init(context: Context) {
        if (database == null) {
            database = AppDatabase.getDatabase(context)
        }
    }

    fun createMovieViewModel(): MovieViewModel {
        val db = database ?: throw IllegalStateException("RepositoryProvider.init(context) must be called before creating ViewModels")
        return MovieViewModel(tmdbRepository, db.mediaDao())
    }
    fun createAudioViewModel(): AudioViewModel {
        val db = database ?: throw IllegalStateException("RepositoryProvider.init(context) must be called before creating ViewModels")
        return AudioViewModel(audioDao = db.audioDao())
    }
    fun createMediaLibraryViewModel(): MediaLibraryViewModel {
        val db = database ?: throw IllegalStateException("RepositoryProvider.init(context) must be called before creating ViewModels")
        return MediaLibraryViewModel(db.mediaDao(), RoomMediaHistoryRepository(db.mediaHistoryDao()))
    }

    fun createMediaMetaViewModel(): MediaMetaViewModel {
        val db = database ?: throw IllegalStateException("RepositoryProvider.init(context) must be called before creating ViewModels")
        return MediaMetaViewModel(db.mediaDao())
    }

    fun createSearchViewModel(): SearchViewModel {
        val db = database ?: throw IllegalStateException("RepositoryProvider.init(context) must be called before creating ViewModels")
        return SearchViewModel(db.mediaDao())
    }

    fun createPerformanceTestViewModel(): PerformanceTestViewModel {
        val db = database ?: throw IllegalStateException("RepositoryProvider.init(context) must be called before creating ViewModels")
        return PerformanceTestViewModel(db.mediaDao())
    }

    // 👇 【新增】 MediaHistoryViewModel 的注入方法
    fun createMediaHistoryViewModel(): MediaHistoryViewModel {
        val db = database ?: throw IllegalStateException("RepositoryProvider.init(context) must be called before creating ViewModels")

        // 1. 获取 DAO (前提：你已经在 AppDatabase 中添加了 abstract fun mediaHistoryDao(): MediaHistoryDao)
        val historyDao = db.mediaHistoryDao()

        // 2. 创建 Repository
        val repository = RoomMediaHistoryRepository(historyDao)

        // 3. 创建 ViewModel
        return MediaHistoryViewModel(repository)
    }
}