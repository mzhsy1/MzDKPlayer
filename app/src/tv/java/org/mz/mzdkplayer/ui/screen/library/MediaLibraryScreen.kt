package org.mz.mzdkplayer.ui.screen.library

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import org.mz.mzdkplayer.R
import org.mz.mzdkplayer.viewmodel.AudioViewModel
import org.mz.mzdkplayer.viewmodel.MediaLibraryViewModel
import org.mz.mzdkplayer.viewmodel.SettingsViewModel

/**
 * 媒体库的三个分页。
 *
 * 电视端侧边栏原来把「电影 / 电视 / 音乐」摊成三个平级入口，占了导航栏一多半的位置；
 * 现在合成一个「媒体库」，用页面顶部的一排标签切换。标签的顺序与取色都集中在这里定义。
 */
enum class MediaLibraryTab(
    val key: String,
    @StringRes val labelRes: Int,
    @DrawableRes val iconRes: Int,
    val accent: Color
) {
    MOVIE("movie", R.string.ui_label_movies, R.drawable.moviefileicon, Color(0xFF64B5F6)),
    TV("tv", R.string.ui_label_series, R.drawable.tv24dp, Color(0xFFBA68C8)),
    MUSIC("music", R.string.ui_label_music, R.drawable.librarymusic24dp, Color(0xFF81C784));

    companion object {
        val DEFAULT = MOVIE

        /** 路由参数（`MediaLibraryPage?tab=tv`）→ 分页，非法值回退电影 */
        fun fromKey(key: String?): MediaLibraryTab =
            values().firstOrNull { it.key == key } ?: DEFAULT
    }
}

/**
 * 电视端「媒体库」：把电影 / 剧集 / 音乐三个库收进一个页面，顶部一排标签切换。
 *
 * 三个库原来的实现（`MovieLibraryScreen` / `TvLibraryScreen` / `AudioLibraryScreen`）
 * 一字未改，仍是整屏的沉浸式布局 —— 所以标签栏是**浮在它们上方**的一层，而不是插进去的
 * 一行：插进布局会动到它们各自的海报/背景尺寸，而这三个页面都是照着整屏调的。
 *
 * 与三块内容的几何关系：影剧库的信息区自带 150~160dp 的顶部留白，音乐库的封面垂直居中，
 * 标签栏只占顶部约 60dp，都不会压到内容。
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun MediaLibraryScreen(
    libraryViewModel: MediaLibraryViewModel,
    audioViewModel: AudioViewModel,
    navController: NavHostController,
    homeNavController: NavHostController,
    settingsViewModel: SettingsViewModel,
    initialTab: String = MediaLibraryTab.DEFAULT.key
) {
    var selectedTab by remember { mutableStateOf(MediaLibraryTab.fromKey(initialTab)) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        when (selectedTab) {
            MediaLibraryTab.MOVIE -> MovieLibraryScreen(
                viewModel = libraryViewModel,
                navController = navController,
                homeNavController = homeNavController,
                settingsViewModel = settingsViewModel
            )

            MediaLibraryTab.TV -> TvLibraryScreen(
                viewModel = libraryViewModel,
                navController = navController,
                homeNavController = homeNavController,
                settingsViewModel = settingsViewModel
            )

            MediaLibraryTab.MUSIC -> AudioLibraryScreen(
                viewModel = audioViewModel,
                homeNavController = homeNavController,
                mainNavController = navController
            )
        }

        MediaLibraryTabBar(
            selected = selectedTab,
            onSelect = { selectedTab = it },
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 32.dp, top = 18.dp)
        )
    }
}

/** 页面顶部的分页标签：选中的用暖白底黑字（跟侧边栏选中态一致），焦点态再叠一圈协议色描边 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun MediaLibraryTabBar(
    selected: MediaLibraryTab,
    onSelect: (MediaLibraryTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        MediaLibraryTab.values().forEach { tab ->
            val isSelected = tab == selected
            val pillShape = RoundedCornerShape(percent = 50)
            Surface(
                onClick = { onSelect(tab) },
                shape = ClickableSurfaceDefaults.shape(shape = pillShape),
                colors = ClickableSurfaceDefaults.colors(
                    containerColor = if (isSelected) {
                        Color(0xFFFFF6E5)
                    } else {
                        Color.White.copy(alpha = 0.07f)
                    },
                    contentColor = if (isSelected) Color.Black else Color.White.copy(alpha = 0.72f),
                    focusedContainerColor = if (isSelected) {
                        Color(0xFFFFF6E5)
                    } else {
                        Color.White.copy(alpha = 0.22f)
                    },
                    focusedContentColor = if (isSelected) Color.Black else Color.White
                ),
                scale = ClickableSurfaceDefaults.scale(focusedScale = 1.06f),
                border = ClickableSurfaceDefaults.border(
                    focusedBorder = Border(
                        border = BorderStroke(2.dp, tab.accent),
                        inset = 0.dp
                    )
                )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(id = tab.iconRes),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(tab.labelRes),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
