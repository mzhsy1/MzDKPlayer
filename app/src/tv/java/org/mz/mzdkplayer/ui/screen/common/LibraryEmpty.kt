package org.mz.mzdkplayer.ui.screen.common

import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import org.mz.mzdkplayer.R

/**
 * 媒体库空态：图标 + 标题 + 「去文件浏览添加」。
 *
 * 图标与强调色跟着库类型走（电影蓝 / 剧集紫 / 音乐绿），和媒体库顶部分页标签同一套取色；
 * 版式交给 [EmptyStateView]，不再单独维护一份间距。
 */
@Composable
fun LibraryEmpty(type: String = "movie", navController: NavController) {
    val buttonFocusRequester = remember { FocusRequester() }
    val accent = when (type) {
        "movie" -> Color(0xFF64B5F6)
        "tv" -> Color(0xFFBA68C8)
        else -> Color(0xFF81C784)
    }
    val iconRes = when (type) {
        "movie" -> R.drawable.movieoff24dp
        "tv" -> R.drawable.tvoff24dp
        else -> R.drawable.musicoff24dp
    }
    val title = when (type) {
        "movie" -> stringResource(R.string.ui_label_no_movie_info_yet)
        "tv" -> stringResource(R.string.ui_label_no_tv_show_info_yet)
        else -> stringResource(R.string.ui_label_no_music_info_yet)
    }

    LaunchedEffect(Unit) {
        buttonFocusRequester.requestFocus()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .focusGroup()
                .focusRestorer(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            EmptyStateView(
                iconRes = iconRes,
                title = title,
                accent = accent,
                action = {
                    MyIconButton(
                        text = stringResource(R.string.ui_label_go_to_file_section_to_add),
                        modifier = Modifier.focusRequester(buttonFocusRequester),
                        icon = when (type) {
                            "movie", "tv" -> R.drawable.videoadd24dp
                            else -> R.drawable.musicnoteadd_24dp
                        },
                        onClick = { navController.navigate("FileHomePage") }
                    )
                }
            )
        }
    }
}
