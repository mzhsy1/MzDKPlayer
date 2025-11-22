package org.mz.mzdkplayer.ui.screen.setting

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable

import androidx.navigation.NavHostController
import androidx.tv.material3.Button
import org.mz.mzdkplayer.R
import org.mz.mzdkplayer.di.RepositoryProvider
import org.mz.mzdkplayer.tool.viewModelWithFactory
import org.mz.mzdkplayer.ui.screen.common.FilePermissionScreen
import org.mz.mzdkplayer.ui.screen.common.MyIconButton
import org.mz.mzdkplayer.ui.screen.vm.MovieViewModel

@Composable
fun SettingsScreen(mainNavController: NavHostController){
   val movieViewModel: MovieViewModel = viewModelWithFactory {
       RepositoryProvider.createMovieViewModel()
   }
    Column() { FilePermissionScreen()
        MyIconButton(
            text = "清理媒体资料库",
            onClick = {
                movieViewModel.clearMediaLibrary()
                // 可以在这里添加 Toast 提示，或监听 ViewModel 的清理状态
            },
            icon = R.drawable.close24dp

        )
    }



}