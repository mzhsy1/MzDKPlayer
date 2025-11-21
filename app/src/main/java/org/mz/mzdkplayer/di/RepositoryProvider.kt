package org.mz.mzdkplayer.di

import android.content.Context
import org.mz.mzdkplayer.data.local.AppDatabase
import org.mz.mzdkplayer.data.repository.TmdbRepository
import org.mz.mzdkplayer.ui.screen.vm.MovieViewModel



object RepositoryProvider {

    // 直接使用 Repository 的单例
    private val tmdbRepository = TmdbRepository.instance

    // 持有数据库实例
    private var database: AppDatabase? = null

    /**
     * 在 Application 的 onCreate 中调用此方法进行初始化
     */
    fun init(context: Context) {
        if (database == null) {
            database = AppDatabase.getDatabase(context)
        }
    }

    fun createMovieViewModel(): MovieViewModel {
        val db = database ?: throw IllegalStateException("RepositoryProvider.init(context) must be called before creating ViewModels")

        // 将 DAO 注入到 ViewModel 中
        return MovieViewModel(tmdbRepository, db.mediaDao())
    }
}