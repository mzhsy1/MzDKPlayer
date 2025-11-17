package org.mz.mzdkplayer.ui



import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items

import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import okhttp3.internal.wait
import org.mz.mzdkplayer.data.model.Movie
import org.mz.mzdkplayer.data.repository.Resource
import org.mz.mzdkplayer.di.RepositoryProvider
import org.mz.mzdkplayer.tool.viewModelWithFactory
import org.mz.mzdkplayer.ui.screen.common.TvTextField
import org.mz.mzdkplayer.ui.screen.vm.MovieViewModel
import org.mz.mzdkplayer.ui.theme.myTTFColor
import org.mz.mzdkplayer.R
@Preview
@Composable
fun MovieScreen(

) {
    val viewModel: MovieViewModel = viewModelWithFactory {
        RepositoryProvider.createMovieViewModel()
    }
    val popularMovies by viewModel.popularMovies.collectAsState()
    val topRatedMovies by viewModel.topRatedMovies.collectAsState()
    var searchQuery by remember { mutableStateOf(TextFieldValue("")) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // 搜索栏
//        SearchBar(
//            query = searchQuery,
//            onQueryChange = { searchQuery = it },
//            onSearch = { viewModel.searchMovies(it.text) },
//            modifier = Modifier.fillMaxWidth()
//        )

        // 热门电影
        SectionTitle("🔥 Popular Movies")
        MovieListSection(movies = popularMovies)

        // 评分最高
        SectionTitle("🏆 Top Rated Movies")
        MovieListSection(movies = topRatedMovies)
    }
}

//@Composable
//private fun SearchBar(
//    query: TextFieldValue,
//    onQueryChange: (TextFieldValue) -> Unit,
//    onSearch: (TextFieldValue) -> Unit,
//    modifier: Modifier = Modifier
//) {
//    val focusRequester = remember { FocusRequester() }
//
//    TvTextField(
//        value = query,
//        onValueChange = { query = it },
//        modifier = Modifier.fillMaxWidth(),
//        placeholder = "完整 WebDAV 路径 (e.g., https://192.168.1.4:5006)",
//        colors = myTTFColor(),
//        textStyle = TextStyle(color = Color.White),
//    )
//
//    LaunchedEffect(Unit) {
//        focusRequester.requestFocus()
//    }
//}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.headlineSmall,
        color = Color.White,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}

@Composable
private fun MovieListSection(movies: Resource<List<Movie>>) {
    Box(modifier = Modifier.fillMaxWidth()) {
        when (movies) {
            is Resource.Loading -> {
                Text("正在加载", color = Color.White)
            }
            is Resource.Error -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth().height(200.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.error24),
                        contentDescription = "Error",
                        tint = Color.Red,
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        text = "Error: ${movies.message}",
                        color = Color.Red,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
            is Resource.Success -> {
                if (movies.data.isEmpty()) {
                    Text(
                        text = "No movies found",
                        color = Color.Gray,
                        modifier = Modifier.align(Alignment.Center)
                    )
                } else {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(movies.data) { movie ->
                            MovieCard(movie = movie)
                        }
                    }
                }
            }
        }

    }
}


@Composable
fun MovieCard(
    movie: Movie,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    Card(
        modifier = modifier
            .width(180.dp)
            .height(280.dp),

        onClick = onClick
    ) {
        Column {
            // 海报图片
            AsyncImage(
                model = "https://image.tmdb.org/t/p/w185${movie.posterPath}",
                contentDescription = movie.title,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(3f),
                contentScale = ContentScale.Crop
            )

            // 电影信息
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(8.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                movie.title?.let {
                    Text(
                        text = it,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        color = Color.White
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = "⭐ ${String.format("%.1f", movie.voteAverage)}",
                        fontSize = 12.sp,
                        color = Color.Yellow
                    )

                    if (!movie.releaseDate.isNullOrBlank()) {
                        Text(
                            text = movie.releaseDate.take(4), // 只显示年份
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                }
            }
        }
    }
}