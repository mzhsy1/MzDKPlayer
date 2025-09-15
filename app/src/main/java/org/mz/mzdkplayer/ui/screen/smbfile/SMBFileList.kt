package org.mz.mzdkplayer.ui.screen.smbfile

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
import androidx.navigation.NavHostController
import androidx.tv.material3.Icon
import androidx.tv.material3.ListItem
import androidx.tv.material3.ListItemDefaults
import androidx.tv.material3.Text
import com.hierynomus.msfscc.FileAttributes
import com.hierynomus.msfscc.fileinformation.FileIdBothDirectoryInformation
import com.hierynomus.smbj.SMBClient
import com.hierynomus.smbj.auth.AuthenticationContext
import com.hierynomus.smbj.connection.Connection
import com.hierynomus.smbj.session.Session
import com.hierynomus.smbj.share.DiskShare
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.mz.mzdkplayer.R
import org.mz.mzdkplayer.tool.Tools
import org.mz.mzdkplayer.tool.Tools.containsVideoFormat
import org.mz.mzdkplayer.ui.style.myListItemColor
import java.net.URLDecoder
import java.net.URLEncoder

@Composable
fun SMBFileListScreen(path: String?, navController: NavHostController) {
    val context = LocalContext.current
    val files = remember { mutableStateListOf<SMBFileItem>() }

    LaunchedEffect(path) {
        files.clear()
        val decodedPath = URLDecoder.decode(path ?: "", "UTF-8")
        if (decodedPath.isEmpty()) return@LaunchedEffect

        // 解析SMB路径格式: smb://username:password@server/share/path
        val smbConfig = parseSMBPath(decodedPath)
        if (smbConfig == null) {
            Log.e("FileScreen", "Invalid SMB path format")
            return@LaunchedEffect
        }

        try {
            // 关键修复：使用withContext(Dispatchers.IO)在后台线程执行网络操作
            val smbFiles = withContext(Dispatchers.IO) {
                listSMBFiles(smbConfig)
            }
            files.addAll(smbFiles)
        } catch (e: Exception) {
            Log.e("FileScreen", "Error listing SMB files", e)
        }
    }

    LazyColumn(modifier = Modifier.padding(16.dp).fillMaxSize()) {
        items(files) { file ->
            ListItem(
                selected = false,
                onClick = {

                    if (file.isDirectory) {
                        val newPath = buildSMBPath(
                            file.server,
                            file.share,
                            file.fullPath,
                            file.username,
                            file.password
                        )
                        val encoded = URLEncoder.encode(newPath, "UTF-8")
                        navController.navigate("SMBFileListScreen/$encoded")
                    } else {
                        // 处理文件点击，比如播放视频
                        Log.d("file.fullPath",file.fullPath)
                         navController.navigate("VideoPlayer/${URLEncoder.encode("smb://${file.username}:${file.password}@${file.server}/${file.share}${file.fullPath}", "UTF-8")}")
                    }
                },
                colors = myListItemColor(),
                modifier = Modifier.padding(10.dp),
                scale = ListItemDefaults.scale(scale = 1.0f, focusedScale = 1.02f),
                leadingContent = {
                    Icon(
                        painter = if (file.isDirectory) {
                            painterResource(R.drawable.baseline_folder_24)
                        } else if (containsVideoFormat(Tools.extractFileExtension(file.name))) {
                            painterResource(R.drawable.baseline_video_file_24)
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

// 方法1：使用 FileAttributes 常量进行位运算判断
fun isDirectory(fileAttributes: Long): Boolean {
    return (fileAttributes and FileAttributes.FILE_ATTRIBUTE_DIRECTORY.value) != 0L
}


data class SMBConfig(
    val server: String,
    val share: String,
    val path: String,
    val username: String,
    val password: String
)

data class SMBFileItem(
    val name: String,
    val fullPath: String,
    val isDirectory: Boolean,
    val server: String,
    val share: String,
    val username: String,
    val password: String
)

private fun parseSMBPath(path: String): SMBConfig? {
    // 格式: smb://username:password@server/share/path/to/directory
    val pattern = Regex("^smb://(?:([^:]+):([^@]+)@)?([^/]+)/([^/]+)(/.*)?$")
    val match = pattern.find(path) ?: return null

    val (username, password, server, share, rawPath) = match.destructured
    val cleanPath = rawPath.trim().let {
        it.ifEmpty { "/" }
    }

    return SMBConfig(
        server = server,
        share = share,
        path = cleanPath,
        username = username.ifEmpty { "guest" },
        password = password.ifEmpty { "" }
    )
}

private fun buildSMBPath(
    server: String,
    share: String,
    path: String,
    username: String,
    password: String
): String {
    return if (username.isNotEmpty() && password.isNotEmpty()) {
        "smb://$username:$password@$server/$share$path"
    } else {
        "smb://$server/$share$path"
    }
}

private fun listSMBFiles(config: SMBConfig): List<SMBFileItem> {
    val client = SMBClient()
    val connection: Connection = client.connect(config.server)

    try {
        val authContext =
            AuthenticationContext(config.username, config.password.toCharArray(), null)
        val session: Session = connection.authenticate(authContext)

        val share = session.connectShare(config.share) as DiskShare
        try {
            // 确保路径以/开头且不以/结尾（除了根路径）
            val cleanPath = config.path.let {
                if (it == "/") "\\" else it.replace("/", "\\").trimEnd('\\')
            }

            val files = mutableListOf<SMBFileItem>()

            share.list(cleanPath).forEach { fileInfo: FileIdBothDirectoryInformation ->
                val fileName = fileInfo.fileName
                // 跳过当前目录和父目录
                if (fileName != "." && fileName != "..") {
                    val isDirectory = isDirectory(fileInfo.fileAttributes)
                    val filePath = if (cleanPath == "\\") {
                        "\\$fileName"
                    } else {
                        "$cleanPath\\$fileName"
                    }

                    files.add(
                        SMBFileItem(
                            name = fileName,
                            fullPath = filePath.replace("\\", "/"),
                            isDirectory = isDirectory,
                            server = config.server,
                            share = config.share,
                            username = config.username,
                            password = config.password,
                        )
                    )
                }
            }

            return files.sortedBy { it.name }
        } finally {
            share.close()
        }
    } finally {
        connection.close()
    }
}

// 可选：添加一个连接测试函数
private fun testSMBConnection(config: SMBConfig): Boolean {
    return try {
        val client = SMBClient()
        val connection = client.connect(config.server)
        val authContext =
            AuthenticationContext(config.username, config.password.toCharArray(), null)
        val session = connection.authenticate(authContext)
        session.connectShare(config.share).close()
        session.close()
        connection.close()
        true
    } catch (e: Exception) {
        false
    }
}



//                            when (Tools.extractFileExtension(file.name)) {
//                                "mp4" -> painterResource(R.drawable.mp4icon)
//                                "mkv" -> painterResource(R.drawable.smb)
//                                "m2ts" -> painterResource(R.drawable.aviicon)
//                                "3gp" -> painterResource(R.drawable.aviicon)
//                                "avi" -> painterResource(R.drawable.aviicon)
//                                "mov" -> painterResource(R.drawable.movicon)
//                                "ts" -> painterResource(R.drawable.tsicon)
//                                "flv" -> painterResource(R.drawable.flvicon)
//                                else -> painterResource(R.drawable.baseline_insert_drive_file_24)
//                            }