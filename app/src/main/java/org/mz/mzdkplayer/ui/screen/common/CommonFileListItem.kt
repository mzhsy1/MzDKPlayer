// File: CommonFileListItem.kt
package org.mz.mzdkplayer.ui.screen.common

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.tv.material3.Icon
import androidx.tv.material3.ListItem
import androidx.tv.material3.ListItemDefaults
import androidx.tv.material3.Text
import org.mz.mzdkplayer.R
import org.mz.mzdkplayer.tool.Tools
import org.mz.mzdkplayer.ui.style.myListItemColor
import java.net.URLEncoder

/**
 * CommonFileListItem
 */
@Composable
fun CommonFileListItem(
    fileListItemData: FileListItemData,
    context: Context,
    navController: NavHostController,
    onFocused: () -> Unit = {}, // 新增：焦点获得时的回调
    onUnfocused: () -> Unit = {} // 可选：失去焦点时的回调
){

    ListItem(
        selected = false,

        onClick = {
            if (fileListItemData.isDirectory) {
                val encodedPath = try {
                    URLEncoder.encode(fileListItemData.filePath, "UTF-8")
                } catch (e: Exception) {
                    Log.e("${fileListItemData.dataSourceName}FileListScreen", "路径编码失败: $e")
                    Toast.makeText(context, "路径编码失败", Toast.LENGTH_SHORT).show()

                }
                navController.navigate("${fileListItemData.dataSourceName}FileListScreen/$encodedPath")
            } else {
                // 处理文件点击
                val fileExtension = Tools.extractFileExtension(fileListItemData.fileName)

                when {
                    Tools.containsVideoFormat(fileExtension) -> {
                        val encodedUri = try {
                            URLEncoder.encode(
                                fileListItemData.filePath ,"UTF-8"
                            )
                        } catch (e: Exception) {
                            Log.e("${fileListItemData.dataSourceName}FileListScreen", "视频URI编码失败: $e")
                            Toast.makeText(context, "视频路径编码失败", Toast.LENGTH_SHORT).show()
                            return@ListItem

                        }
                        val encodedFileName = try {
                            URLEncoder.encode(fileListItemData.fileName, "UTF-8")
                        } catch (e: Exception) {
                            Log.e("${fileListItemData.dataSourceName}FileListScreen", "文件名编码失败: $e")
                            Toast.makeText(context, "文件名编码失败", Toast.LENGTH_SHORT).show()
                            return@ListItem

                        }
                        navController.navigate("VideoPlayer/$encodedUri/${fileListItemData.dataSourceName}/$encodedFileName")
                    }
                    Tools.containsAudioFormat(fileExtension) -> {



                        val encodedUri = try {
                            URLEncoder.encode(
                                fileListItemData.filePath,
                                "UTF-8"
                            )
                        } catch (e: Exception) {
                            Log.e("${fileListItemData.dataSourceName}FileListScreen", "音频URI编码失败: $e")
                            Toast.makeText(context, "音频路径编码失败", Toast.LENGTH_SHORT).show()
                            return@ListItem
                        }

                        val encodedFileName = try {
                            URLEncoder.encode(fileListItemData.fileName, "UTF-8")
                        } catch (e: Exception) {
                            Log.e("${fileListItemData.dataSourceName}FileListScreen", "文件名编码失败: $e")
                            Toast.makeText(context, "文件名编码失败", Toast.LENGTH_SHORT).show()
                            return@ListItem
                        }

                        // ✅ 传递当前音频项在播放列表中的索引
                        navController.navigate("AudioPlayer/$encodedUri/${fileListItemData.dataSourceName}/$encodedFileName/${fileListItemData.currentAudioIndex}")
                    }
                    else -> {
                        Toast.makeText(
                            context,
                            "不支持的文件格式: $fileExtension",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        },
        colors = myListItemColor(),
        modifier = Modifier
            .padding(end = 10.dp).height(40.dp)
            .onFocusChanged { focusState ->
                if (focusState.isFocused) {
                    onFocused()
                } else {
                    onUnfocused()
                }
            }
            ,
        scale = ListItemDefaults.scale(
            scale = 1.0f,
            focusedScale = 1.01f
        ),
        leadingContent = {
            Icon(
                painter = when {
                    fileListItemData.isDirectory -> painterResource(R.drawable.baseline_folder_24)
                    Tools.containsVideoFormat(Tools.extractFileExtension(fileListItemData.fileName)) ->
                        painterResource(R.drawable.moviefileicon)
                    Tools.containsAudioFormat(Tools.extractFileExtension(fileListItemData.fileName)) ->
                        painterResource(R.drawable.baseline_music_note_24)
                    else -> painterResource(R.drawable.baseline_insert_drive_file_24)
                },
                contentDescription = null
            )
        },
        headlineContent = {
            Text(
                fileListItemData.fileName,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontSize = 10.sp
            )
        }
    )
}

/**
 * FileListItemData
 */
data class FileListItemData(
    val filePath: String = "/",
    val fileName: String = "",
    val serverAddress : String = "",
    val shareName : String = "",
    val username : String = "",
    val password : String = "",
    val isDirectory : Boolean = true,
    val dataSourceName : String = "SMB" ,
    val currentAudioIndex : Int = 0

)
