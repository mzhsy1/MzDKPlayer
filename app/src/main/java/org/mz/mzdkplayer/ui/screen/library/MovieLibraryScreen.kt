package org.mz.mzdkplayer.ui.screen.library

import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import org.mz.mzdkplayer.ui.screen.common.MediaCard
import org.mz.mzdkplayer.ui.screen.vm.MediaLibraryViewModel
import java.net.URLEncoder

// === 电影屏幕 ===
@Composable
fun MovieLibraryScreen(
    viewModel: MediaLibraryViewModel,
    navController: NavController
) {
    val movies = viewModel.pagedMovies.collectAsLazyPagingItems()

    Column(modifier = Modifier.padding(start = 32.dp, top = 24.dp, end = 32.dp)) {
        Text(
            text = "电影库",
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(5), // 一行5个，可根据实际TV分辨率调整
            contentPadding = PaddingValues(bottom = 50.dp)
        ) {
            items(movies.itemCount) { index ->
                val movie = movies[index]
                if (movie != null) {
                    MediaCard(
                        title = movie.title,
                        posterPath = movie.posterPath,
                        year = movie.releaseDate?.take(4) ?: "",
                        onClick = {
                            // 电影直接跳转播放或详情
                            val encodedUri = URLEncoder.encode(movie.videoUri, "UTF-8")
                            val encodedName = URLEncoder.encode(movie.title, "UTF-8")
                            Log.d("MovieLibraryScreen",movie.dataSourceType)
                            // 假设这里跳转到电影详情，参数根据你的路由定义
                            //navController.navigate("MovieDetailsScreen/$encodedUri/local/$encodedName/local/0/0/0")
                            // 注意：这里如果是电影，你的 DetailsScreen 需要做适配，或者用单独的 MovieDetailsScreen
                        }
                    )
                }
            }
        }
    }
}