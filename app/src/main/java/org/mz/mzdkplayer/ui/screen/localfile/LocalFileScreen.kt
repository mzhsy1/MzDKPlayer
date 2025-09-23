package org.mz.mzdkplayer.ui.screen.localfile

import android.content.Context
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.navigation.NavHostController
import androidx.tv.material3.Icon
import androidx.tv.material3.ListItem
import androidx.tv.material3.ListItemDefaults
import androidx.tv.material3.Text
import org.mz.mzdkplayer.R

import org.mz.mzdkplayer.ui.style.myListItemColor
import java.io.File
import java.net.URLDecoder
import java.net.URLEncoder

@Composable
fun LocalFileScreen(path: String?, navController: NavHostController) {
    val context = LocalContext.current
    val files = remember { mutableStateListOf<File>() }

    LaunchedEffect(path) {
        files.clear()
        val decodedPath = URLDecoder.decode(path ?: "", "UTF-8")
        if (decodedPath.isEmpty()) return@LaunchedEffect

        // 1. 尝试 MediaStore 查询
        val mediaStoreFiles = queryMediaStore(context, decodedPath)

        if (mediaStoreFiles.isNotEmpty()) {
            Log.d("mediaStoreFiles",mediaStoreFiles[0].path)
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

    LazyColumn(modifier = Modifier.padding(16.dp).fillMaxSize()) {
        items(files) { file ->
            ListItem(
                selected = false,
                onClick = {
                    if (file.isDirectory) {
                        val encoded = URLEncoder.encode(file.path, "UTF-8")
                        navController.navigate("LocalFileScreen/$encoded")
                    }else{
                        navController.navigate("VideoPlayer/${URLEncoder.encode("file://${file.path}", "UTF-8")}/LOCAL")
                    }
                },
                colors = myListItemColor(),
                modifier = Modifier.padding(10.dp),
                scale = ListItemDefaults.scale(scale = 1.0f,focusedScale=1.02f),
                leadingContent = {
                    Icon(
                        painter = if (file.isDirectory) {
                            painterResource(R.drawable.localfile)
                        } else {
                            painterResource(R.drawable.baseline_insert_drive_file_24)
                        },
                        contentDescription = null
                    )
                },
                headlineContent = { Text(file.name) }
            )
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