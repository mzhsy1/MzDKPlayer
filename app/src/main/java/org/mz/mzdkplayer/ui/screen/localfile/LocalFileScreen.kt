package org.mz.mzdkplayer.ui.screen.localfile

import android.content.Context
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.navigation.NavHostController
import androidx.tv.material3.Icon
import androidx.tv.material3.ListItem
import androidx.tv.material3.ListItemDefaults
import androidx.tv.material3.Text
import org.mz.mzdkplayer.R
import org.mz.mzdkplayer.tool.Tools
import org.mz.mzdkplayer.tool.Tools.VideoBigIcon
import org.mz.mzdkplayer.ui.screen.common.FileEmptyScreen

import org.mz.mzdkplayer.ui.style.myListItemColor
import java.io.File
import java.net.URLDecoder
import java.net.URLEncoder

@Composable
fun LocalFileScreen(path: String?, navController: NavHostController) {
    val context = LocalContext.current
    val files = remember { mutableStateListOf<File>() }
    var focusedFileName by remember { mutableStateOf<String?>(null) }
    var focusedIsDir by remember { mutableStateOf(false) }
    var focusedMediaUri by remember { mutableStateOf("") }
    LaunchedEffect(path) {
        files.clear()
        val decodedPath = URLDecoder.decode(path ?: "", "UTF-8")
        if (decodedPath.isEmpty()) return@LaunchedEffect

        // 1. 尝试 MediaStore 查询
        val mediaStoreFiles = queryMediaStore(context, decodedPath)

        if (mediaStoreFiles.isNotEmpty()) {
            Log.d("mediaStoreFiles", mediaStoreFiles[0].path)
            files.addAll(mediaStoreFiles)
            return@LaunchedEffect
        }

        // 2. 降级到文件系统 API
        val dir = File(decodedPath)
        if (dir.exists() && dir.isDirectory) {
            dir.listFiles()?.let {
                files.addAll(it.toList())
            }
        }
    }
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
                items(files) { file ->
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
                                }else if (Tools.containsAudioFormat(
                                        Tools.extractFileExtension(file.name)
                                    )
                                ) {
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
                                        }/0"
                                    )
                                }else {
                                    Toast.makeText(
                                        context,
                                        "不支持的格式",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        },
                        colors = myListItemColor(),
                        modifier = Modifier
                            .padding(end = 10.dp).height(40.dp)
                            .onFocusChanged {
                                if (it.isFocused) {
                                    focusedFileName = file.name;
                                    focusedIsDir = file.isDirectory
                                    focusedMediaUri = "file://${file.path}"
                                }
                            },
                        scale = ListItemDefaults.scale(scale = 1.0f, focusedScale = 1.02f),
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
                        headlineContent = {  Text(
                            file.name, maxLines = 1,
                            overflow = TextOverflow.Ellipsis, fontSize = 10.sp
                        ) }
                    )
                }
            }
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(0.3f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
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