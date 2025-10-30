// File: CommonFileListScreen.kt
package org.mz.mzdkplayer.ui.screen.common

//import android.util.Log
//import android.widget.Toast
//import androidx.compose.foundation.background
//import androidx.compose.foundation.layout.*
//import androidx.compose.foundation.lazy.LazyColumn
//import androidx.compose.foundation.lazy.items
//import androidx.compose.runtime.*
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.focus.onFocusChanged
//import androidx.compose.ui.graphics.Color
//import androidx.compose.ui.platform.LocalContext
//import androidx.compose.ui.res.painterResource
//import androidx.compose.ui.text.font.FontWeight
//import androidx.compose.ui.text.style.TextOverflow
//import androidx.compose.ui.unit.dp
//import androidx.compose.ui.unit.sp
//import androidx.navigation.NavHostController
//import androidx.tv.material3.Icon
//import androidx.tv.material3.ListItem
//import androidx.tv.material3.ListItemDefaults
//import androidx.tv.material3.Text
//import kotlinx.coroutines.launch
//import org.mz.mzdkplayer.R
//import org.mz.mzdkplayer.tool.Tools
//import org.mz.mzdkplayer.ui.style.myListItemColor
//import java.net.URLEncoder
//
//@Composable
//fun CommonFileListScreen(
//    state: FileListState,
//    onFileClick: (FileItem) -> Unit,
//    onDirectoryClick: (FileItem) -> Unit,
//    getFileIcon: (FileItem) -> Int = { file ->
//        when {
//            file.isDirectory -> R.drawable.baseline_folder_24
//            Tools.containsVideoFormat(file.extension) -> R.drawable.moviefileicon
//            Tools.containsAudioFormat(file.extension) -> R.drawable.baseline_music_note_24
//            else -> R.drawable.baseline_insert_drive_file_24
//        }
//    },
//    navController: NavHostController? = null,
//    modifier: Modifier = Modifier
//) {
//    val context = LocalContext.current
//    val coroutineScope = rememberCoroutineScope()
//
//    var focusedFileName by remember { mutableStateOf<String?>(null) }
//    var focusedIsDir by remember { mutableStateOf(false) }
//    var focusedFileItem by remember { mutableStateOf<FileItem?>(null) }
//
//    Box(
//        modifier = modifier
//            .fillMaxSize()
//            .padding(16.dp)
//    ) {
//        when (state.connectionStatus) {
//            is ConnectionStatus.Connecting -> {
//                LoadingScreen(
//                    "正在连接服务器",
//                    Modifier.fillMaxSize().background(Color.Black)
//                )
//            }
//
//            is ConnectionStatus.Error -> {
//                val errorMessage = state.connectionStatus.message
//                Column(
//                    modifier = Modifier.align(Alignment.Center),
//                    horizontalAlignment = Alignment.CenterHorizontally
//                ) {
//                    Text(
//                        "加载失败: $errorMessage",
//                        color = Color.White,
//                        fontWeight = FontWeight.Bold,
//                        fontSize = 18.sp
//                    )
//                }
//            }
//
//            is ConnectionStatus.Connected -> {
//                if (state.files.isEmpty()) {
//                    FileEmptyScreen("此目录为空")
//                } else {
//                    Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
//                        // 文件列表
//                        LazyColumn(
//                            modifier = Modifier
//                                .padding(10.dp)
//                                .fillMaxHeight()
//                                .weight(0.7f)
//                        ) {
//                            items(state.files) { file ->
//                                FileListItem(
//                                    file = file,
//                                    getIcon = getFileIcon,
//                                    onFocusChanged = { focused ->
//                                        if (focused) {
//                                            focusedFileName = file.name
//                                            focusedIsDir = file.isDirectory
//                                            focusedFileItem = file
//                                        }
//                                    },
//                                    onClick = {
//                                        coroutineScope.launch {
//                                            if (file.isDirectory) {
//                                                onDirectoryClick(file)
//                                            } else {
//                                                onFileClick(file)
//                                            }
//                                        }
//                                    }
//                                )
//                            }
//                        }
//
//                        // 预览面板
//                        PreviewPanel(
//                            focusedFileName = focusedFileName,
//                            focusedIsDir = focusedIsDir,
//                            modifier = Modifier
//                                .fillMaxHeight()
//                                .weight(0.3f)
//                        )
//                    }
//                }
//            }
//
//            is ConnectionStatus.Disconnected -> {
//                Column(
//                    modifier = Modifier.align(Alignment.Center),
//                    horizontalAlignment = Alignment.CenterHorizontally
//                ) {
//                    Text(
//                        "未连接到服务器",
//                        color = Color.White,
//                        fontSize = 20.sp
//                    )
//                }
//            }
//        }
//    }
//}
//
//@Composable
//fun FileListItem(
//    file: FileItem,
//    getIcon: (FileItem) -> Int,
//    onFocusChanged: (Boolean) -> Unit,
//    onClick: () -> Unit,
//    modifier: Modifier = Modifier
//) {
//    ListItem(
//        selected = false,
//        onClick = onClick,
//        colors = myListItemColor(),
//        modifier = modifier
//            .padding(end = 10.dp)
//            .onFocusChanged { focusState ->
//                onFocusChanged(focusState.isFocused)
//            },
//        scale = ListItemDefaults.scale(
//            scale = 1.0f,
//            focusedScale = 1.02f
//        ),
//        leadingContent = {
//            Icon(
//                painter = painterResource(getIcon(file)),
//                contentDescription = null
//            )
//        },
//        headlineContent = {
//            Text(
//                file.name,
//                maxLines = 1,
//                overflow = TextOverflow.Ellipsis,
//                fontSize = 12.sp
//            )
//        }
//    )
//}
//
//@Composable
//fun PreviewPanel(
//    focusedFileName: String?,
//    focusedIsDir: Boolean,
//    modifier: Modifier = Modifier
//) {
//    Column(
//        modifier = modifier,
//        horizontalAlignment = Alignment.CenterHorizontally,
//        verticalArrangement = Arrangement.Center
//    ) {
//        Tools.VideoBigIcon(
//            focusedIsDir,
//            focusedFileName,
//            modifier = Modifier
//                .height(200.dp)
//                .fillMaxWidth()
//        )
//        focusedFileName?.let {
//            Text(
//                it,
//                color = Color.White,
//                fontWeight = FontWeight.Bold,
//                fontSize = 18.sp,
//                modifier = Modifier.padding(start = 8.dp)
//            )
//        }
//    }
//}
//
//// 导航辅助函数
//fun navigateToMediaPlayer(
//    file: FileItem,
//    mediaType: String, // "video" or "audio"
//    protocol: String, // "FTP", "HTTP", "NFS", "SMB", "WEBDAV"
//    navController: NavHostController,
//    context: android.content.Context
//) {
//    try {
//        val fullUrl = when (protocol.uppercase()) {
//            "SMB" -> "smb://${file.username}:${file.password}@${file.server}/${file.share}${file.path}"
//            "FTP" -> "ftp://${file.username}:${file.password}@${file.server}:${file.share}/${file.path}"
//            "HTTP" -> "${file.server}${file.path}"
//            "NFS" -> "nfs://${file.server}:${file.share}:${file.path}"
//            "WEBDAV" -> "${file.server}${file.path}"
//            else -> ""
//        }
//
//        val encodedUrl = URLEncoder.encode(fullUrl, "UTF-8")
//        val encodedFileName = URLEncoder.encode(file.name, "UTF-8")
//
//        val destination = when (mediaType) {
//            "video" -> "VideoPlayer/$encodedUrl/$protocol/$encodedFileName"
//            "audio" -> "AudioPlayer/$encodedUrl/$protocol/$encodedFileName"
//            else -> return
//        }
//
//        navController.navigate(destination)
//    } catch (e: Exception) {
//        Log.e("CommonFileListScreen", "导航失败: ${e.message}")
//        Toast.makeText(context, "导航失败", Toast.LENGTH_SHORT).show()
//    }
//}