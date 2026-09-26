package org.mz.mzdkplayer.ui.screen.common

import androidx.compose.foundation.Image
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import org.mz.mzdkplayer.R

@Composable
fun LibraryEmpty(type: String ="movie", navController: NavController){
    // 👇 添加一个 FocusRequester
    val buttonFocusRequester = remember { FocusRequester() }

    // 👇 关键：页面加载后，主动把焦点丢给按钮
    LaunchedEffect(Unit) {
        buttonFocusRequester.requestFocus()
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp) // 添加整体内边距
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .verticalScroll(rememberScrollState()) // 允许内容滚动以防文本过长
                .padding(24.dp)// 给内容区域添加内边距 // 👇 关键：让整个内容区成为一个焦点组，并确保焦点能自动恢复到按钮上
                .focusGroup()
                .focusRestorer(),
            horizontalAlignment = Alignment.CenterHorizontally, // 水平居中 Column 内的元素
            verticalArrangement = Arrangement.spacedBy(16.dp) // 设置元素间的垂直间距
        ) {
            // 图标
            Icon(
                painter =   when (type){
                    "movie"-> painterResource(id = R.drawable.movieoff24dp)
                    "tv"-> painterResource(id = R.drawable.tvoff24dp)
                    else ->painterResource(id = R.drawable.musicoff24dp)}, // 请确保资源存在
                contentDescription = "Error Icon",
                tint = Color.Gray,
                modifier = Modifier.size(100.dp) // 可选：调整图标大小
            )


            // 标题
            Text(
                text = when (type) {"movie"->stringResource(R.string.ui_label_no_movie_info_yet) "tv"->stringResource(R.string.ui_label_no_tv_show_info_yet) else -> stringResource(R.string.ui_label_no_music_info_yet)},
                style = MaterialTheme.typography.headlineSmall, // 使用 Material3 标题样式
                fontWeight = FontWeight.Bold, // 加粗
                color = Color.White, // 使用主题文字颜色
                textAlign = TextAlign.Center
            )

            //
            MyIconButton(
                text = stringResource(R.string.ui_label_go_to_file_section_to_add),
                modifier = Modifier.focusRequester(buttonFocusRequester), // 👇 绑定请求器
                icon = when (type) {"movie","tv"->R.drawable.videoadd24dp
                    else -> {R.drawable.musicnoteadd_24dp}
                },
                onClick = {navController.navigate("FileHomePage")}
            )
        }
    }}