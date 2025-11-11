package org.mz.mzdkplayer.ui.screen.localfile

import NoSearchResult
import android.content.Context
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavHostController
import androidx.tv.material3.Icon
import androidx.tv.material3.ListItem
import androidx.tv.material3.ListItemDefaults
import androidx.tv.material3.Text
import kotlinx.coroutines.delay
import org.mz.mzdkplayer.MzDkPlayerApplication
import org.mz.mzdkplayer.R
import org.mz.mzdkplayer.logic.model.AudioItem
import org.mz.mzdkplayer.logic.model.LocalFileLoadStatus
import org.mz.mzdkplayer.tool.Tools
import org.mz.mzdkplayer.tool.Tools.VideoBigIcon
import org.mz.mzdkplayer.ui.screen.common.FileEmptyScreen
import org.mz.mzdkplayer.ui.screen.common.LoadingScreen
import org.mz.mzdkplayer.ui.screen.common.VAErrorScreen


import org.mz.mzdkplayer.ui.theme.myTTFColor
import org.mz.mzdkplayer.ui.theme.MyFileListItemColor

import org.mz.mzdkplayer.ui.screen.common.TvTextField
import java.io.File
import java.net.URLDecoder
import java.net.URLEncoder

@OptIn(UnstableApi::class)
@Composable
fun LocalFileScreen(path: String?, navController: NavHostController) {
    val context = LocalContext.current
    val files = remember { mutableStateListOf<File>() }
    var status by remember { mutableStateOf<LocalFileLoadStatus>(LocalFileLoadStatus.LoadingFile) }
    var focusedFileName by remember { mutableStateOf<String?>(null) }
    var focusedIsDir by remember { mutableStateOf(false) }
    var seaText by remember { mutableStateOf("") }

    val filteredFiles = remember(files, seaText) {
        if (seaText.isBlank()) {
            files
        } else {
            files.filter { file ->
                file.name.contains(seaText, ignoreCase = true)
            }
        }
    }
    LaunchedEffect(path) {
        status = LocalFileLoadStatus.LoadingFile
        files.clear()
        delay(300)

        val decodedPath = path?.let { URLDecoder.decode(it, "UTF-8") } ?: ""

        if (decodedPath.isEmpty()) {
            status = LocalFileLoadStatus.Error("路径为空")
            return@LaunchedEffect
        }

        try {
            // 1. 尝试 MediaStore 查询
            val mediaStoreFiles = queryMediaStore(context, decodedPath)

            if (mediaStoreFiles.isNotEmpty()) {
                files.addAll(mediaStoreFiles)
                status = LocalFileLoadStatus.FilesLoaded
                return@LaunchedEffect
            }

            // 2. 降级到文件系统 API
            val dir = File(decodedPath)
            if (dir.exists() && dir.isDirectory) {
                dir.listFiles()?.let {
                    files.addAll(it.toList())
                    status = LocalFileLoadStatus.FilesLoaded
                } ?: run {
                    status = LocalFileLoadStatus.Error("无法读取目录内容")
                }
            } else {
                status = LocalFileLoadStatus.Error("目录不存在或不可访问")
            }
        } catch (e: Exception) {
            Log.e("LocalFileScreen", "加载文件失败", e)
            status = LocalFileLoadStatus.Error(e.message ?: "未知错误")
        }
    }

    // 根据状态显示不同 UI
    when (status) {
        is LocalFileLoadStatus.LoadingFile -> {
            LoadingScreen(
                "正在加载本地文件", Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            )
        }

        is LocalFileLoadStatus.Error -> {
            val error = status as LocalFileLoadStatus.Error
            VAErrorScreen("加载失败: ${error.message}")
        }

        LocalFileLoadStatus.FilesLoaded -> {
            if (files.isEmpty()) {
                FileEmptyScreen("此目录为空")
            } else {
                Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                    LazyColumn(
                        modifier = Modifier
                            .padding(10.dp)
                            .fillMaxHeight()
                            .weight(0.7f)
                    ) {
                        when {
                            // 搜索无结果
                            filteredFiles.isEmpty() && seaText.isNotBlank() -> {
                                item {
                                    NoSearchResult(text = "没有匹配 \"$seaText\" 的文件")
                                }
                            }
                            // 目录本身为空（未搜索时）
                            else -> {
                                items(filteredFiles) { file ->
                                    ListItem(
                                        selected = false,
                                        onClick = {
                                            if (file.isDirectory) {
                                                val encoded = URLEncoder.encode(file.path, "UTF-8")
                                                navController.navigate("LocalFileScreen/$encoded")
                                            } else {
                                                if (Tools.containsVideoFormat(
                                                        Tools.extractFileExtension(file.name)
                                                    )
                                                ) {
                                                    navController.navigate(
                                                        "VideoPlayer/${
                                                            URLEncoder.encode(
                                                                "file://${file.path}",
                                                                "UTF-8"
                                                            )
                                                        }/LOCAL/${
                                                            URLEncoder.encode(
                                                                file.name,
                                                                "UTF-8"
                                                            )
                                                        }"
                                                    )
                                                } else if (Tools.containsAudioFormat(
                                                        Tools.extractFileExtension(file.name)
                                                    )
                                                ) {
                                                    val audioFiles = files.filter { localFile ->
                                                        Tools.containsAudioFormat(
                                                            Tools.extractFileExtension(localFile.name)
                                                        )
                                                    }
                                                    val nameToIndexMap =
                                                        audioFiles.withIndex()
                                                            .associateBy(
                                                                { it.value.name },
                                                                { it.index })
                                                    val currentAudioIndex =
                                                        nameToIndexMap[file.name] ?: -1
                                                    if (currentAudioIndex == -1) {
                                                        Log.e(
                                                            "LocalFileScreen",
                                                            "未找到文件在音频列表中: ${file.name}"
                                                        )
                                                        return@ListItem
                                                    }

                                                    val audioItems = audioFiles.map { localFile ->
                                                        AudioItem(
                                                            uri = localFile.path,
                                                            fileName = localFile.name,
                                                            dataSourceType = "LOCAL"
                                                        )
                                                    }
                                                    MzDkPlayerApplication.clearStringList("audio_playlist")
                                                    MzDkPlayerApplication.setStringList(
                                                        "audio_playlist",
                                                        audioItems
                                                    )
                                                    navController.navigate(
                                                        "AudioPlayer/${
                                                            URLEncoder.encode(
                                                                "file://${file.path}",
                                                                "UTF-8"
                                                            )
                                                        }/LOCAL/${
                                                            URLEncoder.encode(
                                                                file.name,
                                                                "UTF-8"
                                                            )
                                                        }/$currentAudioIndex"
                                                    )
                                                } else {
                                                    Toast.makeText(
                                                        context,
                                                        "不支持的格式",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                }
                                            }
                                        },
                                        colors = MyFileListItemColor(),
                                        modifier = Modifier
                                            .padding(end = 10.dp)
                                            .height(40.dp)
                                            .onFocusChanged {
                                                if (it.isFocused) {
                                                    focusedFileName = file.name
                                                    focusedIsDir = file.isDirectory
                                                }
                                            },
                                        scale = ListItemDefaults.scale(
                                            scale = 1.0f,
                                            focusedScale = 1.02f
                                        ),
                                        leadingContent = {
                                            Icon(
                                                painter = if (file.isDirectory) {
                                                    painterResource(R.drawable.baseline_folder_24)
                                                } else if (Tools.containsVideoFormat(
                                                        Tools.extractFileExtension(file.name)
                                                    )
                                                ) {
                                                    painterResource(R.drawable.moviefileicon)
                                                } else if (Tools.containsAudioFormat(
                                                        Tools.extractFileExtension(file.name)
                                                    )
                                                ) {
                                                    painterResource(R.drawable.baseline_music_note_24)
                                                } else {
                                                    painterResource(R.drawable.baseline_insert_drive_file_24)
                                                },
                                                contentDescription = null,
                                            )
                                        },
                                        headlineContent = {
                                            Text(
                                                file.name,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                fontSize = 10.sp
                                            )
                                        }
                                    )
                                }
                            }
                        }
                    }
                    Column(
                        modifier = Modifier
                            .fillMaxHeight()
                            .weight(0.3f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        TvTextField(
                            value = seaText,
                            onValueChange = { seaText = it },
                            modifier = Modifier.fillMaxWidth(),
                            colors = myTTFColor(),
                            placeholder = "请输入文件名",
                            textStyle = TextStyle(color = Color.White),
                        )
                        VideoBigIcon(
                            focusedIsDir,
                            focusedFileName,
                            modifier = Modifier
                                .height(200.dp)
                                .fillMaxWidth()
                        )
                        focusedFileName?.let {
                            Text(
                                it,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun queryMediaStore(context: Context, path: String): List<File> {
    val normalizedPath = if (path.endsWith("/")) path else "$path/"
    val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL)
    } else {
        MediaStore.Files.getContentUri("external")
    }
    val selection = """
        ${MediaStore.Files.FileColumns.DATA} LIKE ? 
        AND ${MediaStore.Files.FileColumns.DATA} NOT LIKE ?
    """.trimIndent()
    val selectionArgs = arrayOf("$normalizedPath%", "$normalizedPath%/%")

    return context.contentResolver.query(
        collection,
        arrayOf(MediaStore.Files.FileColumns.DATA),
        selection,
        selectionArgs,
        null
    )?.use { cursor ->
        generateSequence { if (cursor.moveToNext()) cursor.getString(0) else null }
            .mapNotNull { it -> File(it).takeIf { it.exists() } }
            .toList()
    } ?: emptyList()
}
//navOptions {
//                        popUpTo("FilePage/${URLEncoder.encode(pathUri, "UTF-8")}") {
//                            inclusive = true
//                            saveState = false
//                        }
//                        launchSingleTop = true
//                        restoreState = false
//                    }