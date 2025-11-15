package org.mz.mzdkplayer.ui

import android.app.Activity
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.tv.material3.DrawerState
import androidx.tv.material3.DrawerValue
import androidx.tv.material3.Icon
import androidx.tv.material3.ListItem
import androidx.tv.material3.ListItemDefaults
import androidx.tv.material3.NavigationDrawer
import org.mz.mzdkplayer.MzDkPlayerApplication
import org.mz.mzdkplayer.R
import org.mz.mzdkplayer.data.model.FTPConnection
import org.mz.mzdkplayer.data.model.NFSConnection
import org.mz.mzdkplayer.data.model.WebDavConnection
import org.mz.mzdkplayer.ui.audioplayer.AudioPlayerScreen
import org.mz.mzdkplayer.ui.screen.history.MediaHistoryScreen
import org.mz.mzdkplayer.ui.screen.home.HomeScreen
import org.mz.mzdkplayer.ui.screen.ftp.FTPConListScreen
import org.mz.mzdkplayer.ui.screen.ftp.FTPConScreen
import org.mz.mzdkplayer.ui.screen.ftp.FTPFileListScreen
import org.mz.mzdkplayer.ui.screen.httplink.HTTPLinkFileListScreen
import org.mz.mzdkplayer.ui.screen.httplink.HTTPLinkConListScreen
import org.mz.mzdkplayer.ui.screen.localfile.LocalFileScreen
import org.mz.mzdkplayer.ui.screen.localfile.LocalFileTypeScreen
import org.mz.mzdkplayer.ui.screen.httplink.HTTPLinkConScreen
import org.mz.mzdkplayer.ui.screen.nfs.NFSConListScreen
import org.mz.mzdkplayer.ui.screen.nfs.NFSConScreen
import org.mz.mzdkplayer.ui.screen.nfs.NFSFileListScreen
import org.mz.mzdkplayer.ui.screen.smbfile.SMBConListScreen
import org.mz.mzdkplayer.ui.screen.smbfile.SMBConScreen
import org.mz.mzdkplayer.ui.screen.smbfile.SMBFileListScreen
import org.mz.mzdkplayer.ui.screen.webdavfile.WebDavConListScreen
import org.mz.mzdkplayer.ui.screen.webdavfile.WebDavConScreen
import org.mz.mzdkplayer.ui.screen.webdavfile.WebDavFileListScreen
import org.mz.mzdkplayer.ui.screen.setting.SettingsScreen
import org.mz.mzdkplayer.ui.theme.MySideListItemColor
import org.mz.mzdkplayer.ui.videoplayer.VideoPlayerScreen
import java.net.URLDecoder

@OptIn(UnstableApi::class)
@Composable
fun MzDKPlayerAPP() {
    val mainNavController = rememberNavController()
    var selectedIndex by remember { mutableIntStateOf(0) }
    var backPressedTime by remember { mutableLongStateOf(0L) }
    val backPressThreshold = 2000L // 2秒内再次按返回键才退出

    val items = listOf(
        "HomePage" to R.drawable.baseline_home_24,
        "HistoryPage" to R.drawable.history24dp,
        "SettingsPage" to R.drawable.baseline_settings_24,
    )

    val len = items.size
    val context = LocalContext.current
    val currentRoute = mainNavController.currentBackStackEntryAsState().value?.destination?.route

    // 判断是否为主页面（需要显示侧边栏）
    val isMainPage = currentRoute in listOf("HomePage", "HistoryPage", "SettingsPage")

    // 用于双击退出
    BackHandler {
        val currentDestination = mainNavController.currentDestination?.route
        if (currentDestination == "HomePage") {
            val currentTime = System.currentTimeMillis()
            if (currentTime - backPressedTime < backPressThreshold) {
                (context as? Activity)?.finishAffinity()
            } else {
                backPressedTime = currentTime
                Toast.makeText(context, "再按一次退出程序", Toast.LENGTH_SHORT).show()
            }
        } else {
            mainNavController.popBackStack()
        }
    }

    val sideFocusRequest = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        sideFocusRequest.requestFocus()
    }

    if (isMainPage) {
        // 显示带侧边栏的布局
        NavigationDrawer(
            modifier = Modifier.fillMaxSize(),
            drawerContent = {
                Column(
                    Modifier
                        .background(Color(38, 38, 42, 255))
                        .fillMaxHeight()
                        .padding(6.dp)
                        .widthIn(50.dp, 50.dp)
                        .selectableGroup()
                        .focusRequester(focusRequester = sideFocusRequest),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items.forEachIndexed { index, (route, iconRes) ->
                        val text = when (route) {
                            "HomePage" -> "主页"
                            "HistoryPage" -> "我的"
                            "SettingsPage" -> "设置"
                            else -> "未知"
                        }
                        ListItem(
                            selected = selectedIndex == index,
                            modifier = if (index == len - 0 || index == len - 1) Modifier
                                .widthIn(50.dp)
                                .height(50.dp)
                                .align(Alignment.End) else Modifier
                                .widthIn(50.dp)
                                .height(50.dp),
                            shape = ListItemDefaults.shape(RoundedCornerShape(50)),
                            onClick = {
                                selectedIndex = index
                                mainNavController.navigate(route) {
                                    popUpTo(mainNavController.graph.startDestinationId) {
                                        inclusive = false
                                    }
                                    launchSingleTop = true
                                }
                            },
                            leadingContent = {
                                Icon(
                                    painter = painterResource(id = iconRes),
                                    contentDescription = null,
                                )
                            },
                            colors = MySideListItemColor(),
                            headlineContent = {},
                        )
                    }
                }
            },
            drawerState = DrawerState(DrawerValue.Closed),
            content = {
                NavHost(
                    navController = mainNavController,
                    startDestination = "HomePage",
                    modifier = Modifier
                        .background(Color.Black)
                        .fillMaxHeight()
                        .fillMaxWidth()
                ) {
                    // 主页、历史、设置设为顶级页面
                    composable("HomePage") {
                        HomeScreen(mainNavController)
                    }
                    composable("HistoryPage") {
                        MediaHistoryScreen(mainNavController)
                    }
                    composable("SettingsPage") {
                        SettingsScreen(mainNavController)
                    }

                    // 其他页面保持原有逻辑
                    composable("LocalFileTypeScreen") { backStackEntry ->
                        LocalFileTypeScreen(mainNavController)
                    }
                    composable("LocalFileScreen/{path}") { backStackEntry ->
                        val encodedPath = backStackEntry.arguments?.getString("path")
                        if (encodedPath != null) {
                            val path = URLDecoder.decode(encodedPath, "UTF-8")
                            Log.d("encodedPath", path)
                            LocalFileScreen(path, mainNavController)
                        }
                    }
                    composable("VideoPlayer/{sourceUri}/{dataSourceType}/{fileName}/{connectionName}") { backStackEntry ->
                        val sourceUri = backStackEntry.arguments?.getString("sourceUri")
                        val dataSourceType = backStackEntry.arguments?.getString("dataSourceType")
                        val fileName = backStackEntry.arguments?.getString("fileName")
                        val connectionName = backStackEntry.arguments?.getString("connectionName") ?: "未知"
                        if (sourceUri != null && dataSourceType != null && fileName != null) {
                            Log.d("sourceUri", sourceUri)
                            Log.d("dataSourceType", dataSourceType)
                            VideoPlayerScreen(
                                URLDecoder.decode(sourceUri, "UTF-8"),
                                dataSourceType,
                                URLDecoder.decode(fileName, "UTF-8"),
                                URLDecoder.decode(connectionName, "UTF-8")
                            )
                        }
                    }
                    composable("AudioPlayer/{sourceUri}/{dataSourceType}/{fileName}/{connectionName}/{currentIndex}") { backStackEntry ->
                        val sourceUri = backStackEntry.arguments?.getString("sourceUri")
                        val dataSourceType = backStackEntry.arguments?.getString("dataSourceType")
                        val fileName = backStackEntry.arguments?.getString("fileName")
                        val currentIndex = backStackEntry.arguments?.getString("currentIndex") ?: "1"
                        val connectionName = backStackEntry.arguments?.getString("connectionName") ?: "未知"
                        val extraList = MzDkPlayerApplication.getStringList("audio_playlist")
                        if (sourceUri != null && dataSourceType != null) {
                            Log.d("sourceUri", sourceUri)
                            Log.d("dataSourceType", dataSourceType)
                            AudioPlayerScreen(
                                URLDecoder.decode(sourceUri, "UTF-8"),
                                dataSourceType,
                                URLDecoder.decode(fileName, "UTF-8") ?: "未知文件名",
                                extraList,
                                currentIndex = currentIndex,
                                URLDecoder.decode(connectionName, "UTF-8")
                            )
                        }
                    }
                    composable("SMBFileListScreen/{path}/{connectionName}") { backStackEntry ->
                        val encodedPath = backStackEntry.arguments?.getString("path")
                        val connectionName = backStackEntry.arguments?.getString("connectionName") ?: "未知"
                        if (encodedPath != null) {
                            val path = URLDecoder.decode(encodedPath, "UTF-8")
                            Log.d("encodedPath", path)
                            SMBFileListScreen(path, mainNavController, connectionName)
                        }
                    }
                    composable("WebDavFileListScreen/{path}/{username}/{pw}/{connectionName}") { backStackEntry ->
                        val encodedPath = backStackEntry.arguments?.getString("path")
                        val username = backStackEntry.arguments?.getString("username")
                        val pw = backStackEntry.arguments?.getString("pw")
                        val connectionName = backStackEntry.arguments?.getString("connectionName") ?: "未知"
                        if (encodedPath != null && username != null && pw != null) {
                            val path = URLDecoder.decode(encodedPath, "UTF-8")
                            Log.d("encodedPath", path)
                            WebDavFileListScreen(
                                path,
                                mainNavController,
                                WebDavConnection("1", connectionName, path, username, pw)
                            )
                        }
                    }
                    composable("FTPFileListScreen/{encodedIp}/{encodedUsername}/{encodedPassword}/{port}/{encodedShareName}/{connectionName}") { backStackEntry ->
                        val encodedIp = backStackEntry.arguments?.getString("encodedIp")
                        val encodedUsername = backStackEntry.arguments?.getString("encodedUsername")
                        val encodedPassword = backStackEntry.arguments?.getString("encodedPassword")
                        val encodedShareName = backStackEntry.arguments?.getString("encodedShareName")
                        val connectionName = backStackEntry.arguments?.getString("connectionName") ?: "未知"
                        if (encodedIp != null) {
                            Log.d(
                                "encodedPath",
                                "${URLDecoder.decode(encodedUsername, "UTF-8")}${URLDecoder.decode(encodedPassword, "UTF-8")}${URLDecoder.decode(encodedShareName, "UTF-8")}"
                            )
                            FTPFileListScreen(
                                URLDecoder.decode(encodedShareName, "UTF-8"),
                                mainNavController,
                                FTPConnection(
                                    "1",
                                    connectionName,
                                    ip = encodedIp,
                                    21,
                                    URLDecoder.decode(encodedUsername, "UTF-8"),
                                    URLDecoder.decode(encodedPassword, "UTF-8"),
                                    shareName = URLDecoder.decode(encodedShareName, "UTF-8"),
                                )
                            )
                        }
                    }
                    composable("NFSFileListScreen/{encodedIp}/{encodedShareName}/{newSubPath}/{connectionName}") { backStackEntry ->
                        val encodedIp = backStackEntry.arguments?.getString("encodedIp")
                        val encodedShareName = backStackEntry.arguments?.getString("encodedShareName")
                        val newSubPath = backStackEntry.arguments?.getString("newSubPath")
                        val connectionName = backStackEntry.arguments?.getString("connectionName") ?: "未知"
                        if (encodedIp != null) {
                            Log.d("encodedPath", "${URLDecoder.decode(newSubPath, "UTF-8")}")
                            NFSFileListScreen(
                                URLDecoder.decode(newSubPath, "UTF-8"),
                                mainNavController,
                                NFSConnection(
                                    "1",
                                    connectionName,
                                    URLDecoder.decode(encodedIp, "UTF-8"),
                                    URLDecoder.decode(encodedShareName, "UTF-8"),
                                )
                            )
                        }
                    }
                    composable("HTTPLinkFileListScreen/{connectionName}/{newSubPath}") { backStackEntry ->
                        val newSubPath = backStackEntry.arguments?.getString("newSubPath")
                        val connectionName = backStackEntry.arguments?.getString("connectionName") ?: "未知"
                        if (newSubPath != null) {
                            Log.d("encodedPath", "${URLDecoder.decode(newSubPath, "UTF-8")}")
                            HTTPLinkFileListScreen(URLDecoder.decode(newSubPath, "UTF-8"), mainNavController, connectionName)
                        }
                    }
                    composable("SMBListScreen") {
                        SMBConListScreen(mainNavController)
                    }
                    composable("SMBConScreen") {
                        SMBConScreen()
                    }
                    composable("WebDavConScreen") {
                        WebDavConScreen()
                    }
                    composable("WebDavListScreen") {
                        WebDavConListScreen(mainNavController)
                    }
                    composable("FTPConScreen") {
                        FTPConScreen()
                    }
                    composable("FTPConListScreen") {
                        FTPConListScreen(mainNavController)
                    }
                    composable("NFSConScreen") {
                        NFSConScreen()
                    }
                    composable("NFSConListScreen") {
                        NFSConListScreen(mainNavController)
                    }
                    composable("HTTPLinkConScreen") {
                        HTTPLinkConScreen()
                    }
                    composable("HTTPLinkConListScreen") {
                        HTTPLinkConListScreen(mainNavController)
                    }
                }
            }
        )
    } else {
        // 不显示侧边栏，只显示内容
        NavHost(
            navController = mainNavController,
            startDestination = "HomePage",
            modifier = Modifier
                .background(Color.Black)
                .fillMaxSize()
        ) {
            // 主页、历史、设置设为顶级页面
            composable("HomePage") {
                HomeScreen(mainNavController)
            }
            composable("HistoryPage") {
                MediaHistoryScreen(mainNavController)
            }
            composable("SettingsPage") {
                SettingsScreen(mainNavController)
            }

            // 其他页面保持原有逻辑
            composable("LocalFileTypeScreen") { backStackEntry ->
                LocalFileTypeScreen(mainNavController)
            }
            composable("LocalFileScreen/{path}") { backStackEntry ->
                val encodedPath = backStackEntry.arguments?.getString("path")
                if (encodedPath != null) {
                    val path = URLDecoder.decode(encodedPath, "UTF-8")
                    Log.d("encodedPath", path)
                    LocalFileScreen(path, mainNavController)
                }
            }
            composable("VideoPlayer/{sourceUri}/{dataSourceType}/{fileName}/{connectionName}") { backStackEntry ->
                val sourceUri = backStackEntry.arguments?.getString("sourceUri")
                val dataSourceType = backStackEntry.arguments?.getString("dataSourceType")
                val fileName = backStackEntry.arguments?.getString("fileName")
                val connectionName = backStackEntry.arguments?.getString("connectionName") ?: "未知"
                if (sourceUri != null && dataSourceType != null && fileName != null) {
                    Log.d("sourceUri", sourceUri)
                    Log.d("dataSourceType", dataSourceType)
                    VideoPlayerScreen(
                        URLDecoder.decode(sourceUri, "UTF-8"),
                        dataSourceType,
                        URLDecoder.decode(fileName, "UTF-8"),
                        URLDecoder.decode(connectionName, "UTF-8")
                    )
                }
            }
            composable("AudioPlayer/{sourceUri}/{dataSourceType}/{fileName}/{connectionName}/{currentIndex}") { backStackEntry ->
                val sourceUri = backStackEntry.arguments?.getString("sourceUri")
                val dataSourceType = backStackEntry.arguments?.getString("dataSourceType")
                val fileName = backStackEntry.arguments?.getString("fileName")
                val currentIndex = backStackEntry.arguments?.getString("currentIndex") ?: "1"
                val connectionName = backStackEntry.arguments?.getString("connectionName") ?: "未知"
                val extraList = MzDkPlayerApplication.getStringList("audio_playlist")
                if (sourceUri != null && dataSourceType != null) {
                    Log.d("sourceUri", sourceUri)
                    Log.d("dataSourceType", dataSourceType)
                    AudioPlayerScreen(
                        URLDecoder.decode(sourceUri, "UTF-8"),
                        dataSourceType,
                        URLDecoder.decode(fileName, "UTF-8") ?: "未知文件名",
                        extraList,
                        currentIndex = currentIndex,
                        URLDecoder.decode(connectionName, "UTF-8")
                    )
                }
            }
            composable("SMBFileListScreen/{path}/{connectionName}") { backStackEntry ->
                val encodedPath = backStackEntry.arguments?.getString("path")
                val connectionName = backStackEntry.arguments?.getString("connectionName") ?: "未知"
                if (encodedPath != null) {
                    val path = URLDecoder.decode(encodedPath, "UTF-8")
                    Log.d("encodedPath", path)
                    SMBFileListScreen(path, mainNavController, connectionName)
                }
            }
            composable("WebDavFileListScreen/{path}/{username}/{pw}/{connectionName}") { backStackEntry ->
                val encodedPath = backStackEntry.arguments?.getString("path")
                val username = backStackEntry.arguments?.getString("username")
                val pw = backStackEntry.arguments?.getString("pw")
                val connectionName = backStackEntry.arguments?.getString("connectionName") ?: "未知"
                if (encodedPath != null && username != null && pw != null) {
                    val path = URLDecoder.decode(encodedPath, "UTF-8")
                    Log.d("encodedPath", path)
                    WebDavFileListScreen(
                        path,
                        mainNavController,
                        WebDavConnection("1", connectionName, path, username, pw)
                    )
                }
            }
            composable("FTPFileListScreen/{encodedIp}/{encodedUsername}/{encodedPassword}/{port}/{encodedShareName}/{connectionName}") { backStackEntry ->
                val encodedIp = backStackEntry.arguments?.getString("encodedIp")
                val encodedUsername = backStackEntry.arguments?.getString("encodedUsername")
                val encodedPassword = backStackEntry.arguments?.getString("encodedPassword")
                val encodedShareName = backStackEntry.arguments?.getString("encodedShareName")
                val connectionName = backStackEntry.arguments?.getString("connectionName") ?: "未知"
                if (encodedIp != null) {
                    Log.d(
                        "encodedPath",
                        "${URLDecoder.decode(encodedUsername, "UTF-8")}${URLDecoder.decode(encodedPassword, "UTF-8")}${URLDecoder.decode(encodedShareName, "UTF-8")}"
                    )
                    FTPFileListScreen(
                        URLDecoder.decode(encodedShareName, "UTF-8"),
                        mainNavController,
                        FTPConnection(
                            "1",
                            connectionName,
                            ip = encodedIp,
                            21,
                            URLDecoder.decode(encodedUsername, "UTF-8"),
                            URLDecoder.decode(encodedPassword, "UTF-8"),
                            shareName = URLDecoder.decode(encodedShareName, "UTF-8"),
                        )
                    )
                }
            }
            composable("NFSFileListScreen/{encodedIp}/{encodedShareName}/{newSubPath}/{connectionName}") { backStackEntry ->
                val encodedIp = backStackEntry.arguments?.getString("encodedIp")
                val encodedShareName = backStackEntry.arguments?.getString("encodedShareName")
                val newSubPath = backStackEntry.arguments?.getString("newSubPath")
                val connectionName = backStackEntry.arguments?.getString("connectionName") ?: "未知"
                if (encodedIp != null) {
                    Log.d("encodedPath", "${URLDecoder.decode(newSubPath, "UTF-8")}")
                    NFSFileListScreen(
                        URLDecoder.decode(newSubPath, "UTF-8"),
                        mainNavController,
                        NFSConnection(
                            "1",
                            connectionName,
                            URLDecoder.decode(encodedIp, "UTF-8"),
                            URLDecoder.decode(encodedShareName, "UTF-8"),
                        )
                    )
                }
            }
            composable("HTTPLinkFileListScreen/{connectionName}/{newSubPath}") { backStackEntry ->
                val newSubPath = backStackEntry.arguments?.getString("newSubPath")
                val connectionName = backStackEntry.arguments?.getString("connectionName") ?: "未知"
                if (newSubPath != null) {
                    Log.d("encodedPath", "${URLDecoder.decode(newSubPath, "UTF-8")}")
                    HTTPLinkFileListScreen(URLDecoder.decode(newSubPath, "UTF-8"), mainNavController, connectionName)
                }
            }
            composable("SMBListScreen") {
                SMBConListScreen(mainNavController)
            }
            composable("SMBConScreen") {
                SMBConScreen()
            }
            composable("WebDavConScreen") {
                WebDavConScreen()
            }
            composable("WebDavListScreen") {
                WebDavConListScreen(mainNavController)
            }
            composable("FTPConScreen") {
                FTPConScreen()
            }
            composable("FTPConListScreen") {
                FTPConListScreen(mainNavController)
            }
            composable("NFSConScreen") {
                NFSConScreen()
            }
            composable("NFSConListScreen") {
                NFSConListScreen(mainNavController)
            }
            composable("HTTPLinkConScreen") {
                HTTPLinkConScreen()
            }
            composable("HTTPLinkConListScreen") {
                HTTPLinkConListScreen(mainNavController)
            }
        }
    }
}



