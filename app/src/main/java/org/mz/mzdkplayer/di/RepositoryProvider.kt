package org.mz.mzdkplayer.di

import org.mz.mzdkplayer.data.repository.TmdbRepository
import org.mz.mzdkplayer.ui.screen.vm.MovieViewModel



object RepositoryProvider {

    // 直接使用 Repository 的单例
    private val tmdbRepository = TmdbRepository.instance

    fun createMovieViewModel(): MovieViewModel {
        return MovieViewModel(tmdbRepository)
    }
}