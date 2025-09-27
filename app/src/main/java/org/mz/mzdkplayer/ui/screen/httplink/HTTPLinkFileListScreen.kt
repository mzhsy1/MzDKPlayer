// File: HTTPLinkFileListScreen.kt

package org.mz.mzdkplayer.ui.screen.http // 请根据你的实际包名修改

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.tv.material3.Icon
import androidx.tv.material3.ListItem
import androidx.tv.material3.ListItemDefaults
import androidx.tv.material3.Text
import kotlinx.coroutines.launch
import org.mz.mzdkplayer.R
import org.mz.mzdkplayer.tool.Tools
import org.mz.mzdkplayer.ui.screen.common.FileEmptyScreen
import org.mz.mzdkplayer.ui.screen.common.LoadingScreen
import org.mz.mzdkplayer.ui.screen.vm.HTTPLinkConViewModel
import org.mz.mzdkplayer.ui.screen.vm.HTTPLinkConnectionStatus
import org.mz.mzdkplayer.ui.style.myListItemColor
import java.net.URLEncoder


/**
 * HTTP 链接文件列表屏幕
 *
 * @param serverAddressAndShare HTTP 服务器地址和共享路径 (e.g., "http://192.168.1.100:8080/nas")
 *                              该参数包含协议、主机、端口、根共享路径
 * @param subPath 当前浏览的子路径，相对于 serverAddressAndShare (e.g., "movies/action")
 * @param navController 导航控制器
 */
@Composable
fun HTTPLinkFileListScreen(
    serverAddressAndShare: String, // HTTP 服务器地址和共享路径
    subPath: String?,      // 当前浏览的子路径，可以为 null 或空字符串表示根目录下的共享路径
    navController: NavHostController
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    // 使用 ViewModel
    val viewModel: HTTPLinkConViewModel = viewModel()

    // 收集 ViewModel 中的状态
    val fileList by viewModel.fileList.collectAsState()
    val connectionStatus by viewModel.connectionStatus.collectAsState()
    val currentPath by viewModel.currentPath.collectAsState() // 使用 ViewModel 的 currentPath

    val effectiveSubPath = subPath ?: "" // 处理 null 情况，默认为空字符串
    var hasAttemptedInitialLoad by remember { mutableStateOf(false) } // 标记是否已尝试过初始加载
    var lastAttemptedPath by remember { mutableStateOf<String?>(null) } // 记录上次尝试加载的路径

    // 当传入的 serverAddressAndShare, effectiveSubPath 参数变化时，或者首次进入时，尝试加载文件列表
    LaunchedEffect(serverAddressAndShare, effectiveSubPath) {
        Log.d(
            "HTTPLinkFileListScreen",
            "LaunchedEffect triggered with serverAddressAndShare: $serverAddressAndShare, subPath: $effectiveSubPath, status: $connectionStatus, hasAttempted: $hasAttemptedInitialLoad, lastPath: $lastAttemptedPath"
        )

        if (!hasAttemptedInitialLoad || lastAttemptedPath != effectiveSubPath) {
            Log.d("HTTPLinkFileListScreen", "Initial load or path change detected. Attempting action.")
            hasAttemptedInitialLoad = true
            lastAttemptedPath = effectiveSubPath

            when (connectionStatus) {
                is HTTPLinkConnectionStatus.Connected -> {
                    // 已连接，直接尝试列出指定路径
                    Log.d("HTTPLinkFileListScreen", "Already connected, listing files for path: $effectiveSubPath")
                    viewModel.listFiles(effectiveSubPath)
                }

                is HTTPLinkConnectionStatus.Disconnected,
                is HTTPLinkConnectionStatus.Error -> {
                    // 未连接或之前有错误，尝试连接到根路径
                    Log.d("HTTPLinkFileListScreen", "Disconnected/Error or first load. Attempting to connect to root: $serverAddressAndShare")
                    viewModel.connectToHTTPLink(serverAddressAndShare)
                    // 连接成功后，LaunchedEffect 会再次触发，届时会检查路径并加载
                }

                is HTTPLinkConnectionStatus.Connecting -> {

                    // 正在连接，等待...
                    Log.d("HTTPLinkFileListScreen", "Currently connecting, waiting for status change...")
                }
            }
        } else {
            Log.d("HTTPLinkFileListScreen", "No new load needed, state matches.")
        }
    }

    // 专门监听连接状态变化，连接成功后检查是否需要导航到初始子路径
    LaunchedEffect(connectionStatus) {
        when (connectionStatus) {
            is HTTPLinkConnectionStatus.Connected -> {
                // 连接成功后，检查当前路径是否与目标路径一致
                if (currentPath != effectiveSubPath && lastAttemptedPath == effectiveSubPath) {
                    Log.d("HTTPLinkFileListScreen", "Connected. Current path ($currentPath) differs from target ($effectiveSubPath), listing target path.")
                    viewModel.listFiles(effectiveSubPath)
                }
            }
            is HTTPLinkConnectionStatus.Error -> {
                // 如果连接或加载出错，不再自动重试，等待用户操作或导航离开
                Log.e("HTTPLinkFileListScreen", "Connection or listing failed: ${(connectionStatus as HTTPLinkConnectionStatus.Error).message}")
            }
            else -> {
                // 其他状态，如 Connecting 或 Disconnected，不做特殊处理
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            // 可选：在离开屏幕时断开连接或清理资源
            // ViewModel 的 onCleared 会处理清理，通常不需要在此处手动断开
            Log.d("HTTPLinkFileListScreen", "Screen disposed")
        }
    }

    // 根据连接状态渲染 UI
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        when (connectionStatus) {
            is HTTPLinkConnectionStatus.Connecting -> {
                LoadingScreen("正在链接HTTP服务器")
            }

            is HTTPLinkConnectionStatus.Error -> {
                // 显示错误信息
                val errorMessage = (connectionStatus as HTTPLinkConnectionStatus.Error).message
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.baseline_error_24),
                        contentDescription = null,
                        tint = Color.Red,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "加载失败",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        errorMessage,
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    // 可以添加一个重试按钮，但需要谨慎，避免无限循环
                    /* Button(
                         onClick = {
                             // 重试逻辑应谨慎，例如只重试连接根路径
                             hasAttemptedInitialLoad = false // 重置标志
                             lastAttemptedPath = null // 重置上次尝试路径
                             // 触发 LaunchedEffect 重新执行
                         }
                     ) {
                         Text("重试")
                     }*/
                }
            }

            is HTTPLinkConnectionStatus.Connected -> {
                if (fileList.isEmpty()) {
                    FileEmptyScreen("此目录为空")
                } else {
                    // 已连接，显示文件列表
                    LazyColumn(modifier = Modifier.fillMaxSize()) {

                        Log.d("HTTPLinkFileListScreen", "Displaying fileList: $fileList")

                        items(fileList) { resource ->
                            // 这里假设 resource 有 isDirectory: Boolean 和 name: String, path: String 属性
                            val isDirectory = resource.isDirectory
                            val resourceName = resource.name // 这里应该已经是完整的文件/目录名
                            val resourcePath = resource.path // 相对于 baseUrl 的路径

                            ListItem(
                                selected = false,
                                onClick = {
                                    coroutineScope.launch {
                                        if (isDirectory) {
                                            // 使用 ViewModel 的方法计算新的完整子路径，避免多余的斜杠
                                            val newSubPath = viewModel.calculateNewSubPath(currentPath, resourceName)
                                            Log.d("effectiveSubPath", newSubPath) // 记录计算出的完整路径
                                            Log.d(
                                                "HTTPLinkFileListScreen",
                                                "Navigating to subdirectory: $resourceName (calculated full path: $newSubPath)"
                                            )
                                            // 对新计算出的完整路径进行编码
                                            val encodedNewSubPath = URLEncoder.encode(newSubPath, "UTF-8")
                                            Log.d("HTTPLinkFileListScreen", "Encoded calculated path: $encodedNewSubPath")

                                            // 导航到子目录，传递服务器地址和新的完整子路径
                                            val encodedBaseUrl = URLEncoder.encode(serverAddressAndShare, "UTF-8")
                                            navController.navigate("HTTPLinkFileListScreen/$encodedBaseUrl/$encodedNewSubPath")
                                        } else {
                                            // 处理文件点击 - 导航到 VideoPlayer
                                            // 构造完整的 HTTP URL
                                            val fullFileUrl = viewModel.getResourceFullUrl(resourceName)

                                            Log.d(
                                                "HTTPLinkFileListScreen",
                                                "Full file URL before encoding: $fullFileUrl"
                                            )

                                            val encodedFileUrl = URLEncoder.encode(fullFileUrl, "UTF-8")
                                            Log.d(
                                                "HTTPLinkFileListScreen",
                                                "Encoded file URL: $encodedFileUrl"
                                            )

                                            // 导航到视频播放器，传递编码后的 URL 和来源标识
                                            navController.navigate("VideoPlayer/$encodedFileUrl/HTTP")
                                        }
                                    }
                                },
                                colors = myListItemColor(),
                                modifier = Modifier.padding(10.dp),
                                scale = ListItemDefaults.scale(scale = 1.0f, focusedScale = 1.02f),
                                leadingContent = {
                                    Icon(
                                        painter = if (isDirectory) {
                                            painterResource(R.drawable.baseline_folder_24)
                                        } else if (Tools.containsVideoFormat(
                                                Tools.extractFileExtension(resourceName)
                                            )
                                        ) {
                                            painterResource(R.drawable.moviefileicon)
                                        } else {
                                            painterResource(R.drawable.baseline_insert_drive_file_24)
                                        },
                                        contentDescription = null
                                    )
                                },
                                headlineContent = {
                                    // 显示完整的文件名
                                    Text(resourceName)
                                }
                                // supportingContent = { Text(resource.rawListing ?: "") } // 可以显示原始信息
                            )
                        }
                    }
                }
            }

            is HTTPLinkConnectionStatus.Disconnected -> {
                // 显示未连接提示
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("未连接到 HTTP 服务器")
                    // 连接逻辑已在 LaunchedEffect 中处理
                }
            }
        }
    }
}



