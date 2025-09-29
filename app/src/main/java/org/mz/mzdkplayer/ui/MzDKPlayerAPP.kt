package org.mz.mzdkplayer.ui

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.tv.material3.DrawerState
import androidx.tv.material3.DrawerValue
import androidx.tv.material3.Icon
import androidx.tv.material3.ListItem
import androidx.tv.material3.ListItemDefaults
import androidx.tv.material3.NavigationDrawer
import org.mz.mzdkplayer.R
import org.mz.mzdkplayer.logic.model.FTPConnection
import org.mz.mzdkplayer.logic.model.NFSConnection
import org.mz.mzdkplayer.logic.model.WebDavConnection
import org.mz.mzdkplayer.ui.audioplayer.AudioPlayerScreen


import org.mz.mzdkplayer.ui.screen.HomeScreen
import org.mz.mzdkplayer.ui.screen.ftp.FTPConListScreen
import org.mz.mzdkplayer.ui.screen.ftp.FTPConScreen
import org.mz.mzdkplayer.ui.screen.ftp.FTPFileListScreen
import org.mz.mzdkplayer.ui.screen.http.HTTPLinkFileListScreen
import org.mz.mzdkplayer.ui.screen.httplink.HTTPLinkConListScreen

import org.mz.mzdkplayer.ui.screen.localfile.LocalFileScreen
import org.mz.mzdkplayer.ui.screen.localfile.LocalFileTypeScreen
import org.mz.mzdkplayer.ui.screen.nfs.HTTPLinkConScreen
import org.mz.mzdkplayer.ui.screen.nfs.NFSConListScreen
import org.mz.mzdkplayer.ui.screen.nfs.NFSConScreen
import org.mz.mzdkplayer.ui.screen.nfs.NFSFileListScreen
import org.mz.mzdkplayer.ui.screen.smbfile.SMBConListScreen
import org.mz.mzdkplayer.ui.screen.smbfile.SMBConScreen
import org.mz.mzdkplayer.ui.screen.smbfile.SMBFileListScreen
import org.mz.mzdkplayer.ui.screen.webdavfile.WebDavConListScreen

import org.mz.mzdkplayer.ui.screen.webdavfile.WebDavConScreen
import org.mz.mzdkplayer.ui.screen.webdavfile.WebDavFileListScreen

import org.mz.mzdkplayer.ui.videoplayer.VideoPlayerScreen
import java.net.URLDecoder

@Composable
fun MzDKPlayerAPP() {


    var selectedIndex by remember { mutableIntStateOf(0) }
    val items =
        listOf(
            "主页" to painterResource(id = R.drawable.baseline_home_24),
            "我的" to painterResource(id = R.drawable.baseline_account_circle_24),
            "设置" to painterResource(id = R.drawable.baseline_settings_24),
        )

    val mainNavController = rememberNavController()
    val len = items.size

    NavHost(
        navController = mainNavController,
        startDestination = "MainPage",
        modifier = Modifier.background(Color.Black)
    ) {
        composable("MainPage") {
            val homeNavController = rememberNavController()
            NavigationDrawer(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(),
                drawerContent = {
                    Column(
                        Modifier
                            .background(Color(38, 38, 42, 255))
                            .fillMaxHeight()
                            .padding(6.dp)
                            .widthIn(50.dp, 50.dp)
                            .selectableGroup(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items.forEachIndexed { index, item ->
                            val (text, icon) = item
                            ListItem(
                                selected = selectedIndex == index,
                                modifier = if (index == len - 0 || index == len - 1) Modifier
                                    .widthIn(
                                        50.dp
                                    )
                                    .height(50.dp)
                                    .align(Alignment.End) else Modifier
                                    .widthIn(
                                        50.dp
                                    )
                                    .height(50.dp),
                                shape = ListItemDefaults.shape(RoundedCornerShape(50)),
                                onClick = {
                                    selectedIndex = index
                                    when (selectedIndex) {

                                    }
                                },
                                leadingContent = {
                                    Icon(
                                        painter = icon,
                                        contentDescription = null,
                                    )
                                    // Text(text, color = Color.White)
                                },
                                colors = ListItemDefaults.colors(
                                    containerColor = Color(38, 38, 42, 255),
                                    contentColor = Color(255, 255, 255),
                                    selectedContainerColor = Color(251, 114, 153, 220),
                                    selectedContentColor = Color(0, 0, 0),
                                    focusedSelectedContentColor = Color(0, 0, 0),
                                    focusedSelectedContainerColor = Color(255, 255, 255),
                                    focusedContainerColor = Color(255, 255, 255),
                                    focusedContentColor = Color(0, 0, 0)
                                ),
                                headlineContent = {},
                            )
                        }
                    }

                },
                drawerState = DrawerState(DrawerValue.Closed),
                content = {
                    NavHost(
                        navController = homeNavController,
                        startDestination = "HomePage",
                        modifier = Modifier
                            .background(Color.Black)
                            .fillMaxHeight()
                            .fillMaxWidth()
                    ) {
                        //声明名为MainPage的页面路由
                        composable("HomePage") {
                            //页面路由对应的页面组件
                            HomeScreen(mainNavController)
                        }

                    }
                })
        }
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
        composable("VideoPlayer/{sourceUri}/{dataSourceType}") { backStackEntry ->

            //页面路由对应的页面组件
            val sourceUri = backStackEntry.arguments?.getString("sourceUri")
            val dataSourceType = backStackEntry.arguments?.getString("dataSourceType")
            // 检查参数是否不为空，并渲染屏幕
            if (sourceUri != null && dataSourceType != null) {
                Log.d("sourceUri", sourceUri)
                Log.d("dataSourceType", dataSourceType)
                VideoPlayerScreen(URLDecoder.decode(sourceUri, "UTF-8"), dataSourceType)
            }
        }
        composable("AudioPlayer/{sourceUri}/{dataSourceType}") { backStackEntry ->

            //页面路由对应的页面组件
            val sourceUri = backStackEntry.arguments?.getString("sourceUri")
            val dataSourceType = backStackEntry.arguments?.getString("dataSourceType")
            // 检查参数是否不为空，并渲染屏幕
            if (sourceUri != null && dataSourceType != null) {
                Log.d("sourceUri", sourceUri)
                Log.d("dataSourceType", dataSourceType)
                AudioPlayerScreen(URLDecoder.decode(sourceUri, "UTF-8"), dataSourceType)
            }
        }
        composable("SMBFileListScreen/{path}") { backStackEntry ->
            val encodedPath = backStackEntry.arguments?.getString("path")
            if (encodedPath != null) {

                val path = URLDecoder.decode(encodedPath, "UTF-8")
                Log.d("encodedPath", path)
                SMBFileListScreen(path, mainNavController)
            }
        }
        composable("WebDavFileListScreen/{path}/{username}/{pw}") { backStackEntry ->
            val encodedPath = backStackEntry.arguments?.getString("path")
            val username = backStackEntry.arguments?.getString("username")
            val pw = backStackEntry.arguments?.getString("pw")
            if (encodedPath != null && username != null && pw != null) {

                val path = URLDecoder.decode(encodedPath, "UTF-8")
                Log.d("encodedPath", path)
                WebDavFileListScreen(
                    path,
                    mainNavController,
                    WebDavConnection("1", "webdav", path, username, pw)
                )
            }
        }
        composable("FTPFileListScreen/{encodedIp}/{encodedUsername}/{encodedPassword}/{port}/{encodedShareName}") { backStackEntry ->
            val encodedIp = backStackEntry.arguments?.getString("encodedIp")
            val encodedUsername = backStackEntry.arguments?.getString("encodedUsername")
            val encodedPassword = backStackEntry.arguments?.getString("encodedPassword")
            val encodedShareName = backStackEntry.arguments?.getString("encodedShareName")
            //URLEncoder.encode(conn.shareName, "UTF-8")
            if (encodedIp != null) {

                val path = URLDecoder.decode(URLDecoder.decode(encodedIp, "UTF-8"), "UTF-8")
                Log.d("encodedPath", "${URLDecoder.decode(encodedUsername, "UTF-8")}${URLDecoder.decode(encodedPassword, "UTF-8")}${URLDecoder.decode(encodedShareName, "UTF-8")}",)
                FTPFileListScreen(
                    URLDecoder.decode(encodedShareName, "UTF-8"),
                    mainNavController,
                    FTPConnection(
                        "1",
                        "ftp",
                        ip=encodedIp,
                        21,
                        URLDecoder.decode(encodedUsername, "UTF-8"),
                        URLDecoder.decode(encodedPassword, "UTF-8"),
                        shareName = URLDecoder.decode(encodedShareName, "UTF-8"),
                    )
                )
            }
        }
        composable("NFSFileListScreen/{encodedIp}/{encodedShareName}/{newSubPath}") { backStackEntry ->
            val encodedIp = backStackEntry.arguments?.getString("encodedIp")
            val encodedShareName = backStackEntry.arguments?.getString("encodedShareName")
            val newSubPath = backStackEntry.arguments?.getString("newSubPath")
            //URLEncoder.encode(conn.shareName, "UTF-8")
            if (encodedIp != null) {

                //val path = URLDecoder.decode(URLDecoder.decode(encodedIp, "UTF-8"), "UTF-8")
                Log.d("encodedPath", "${URLDecoder.decode(newSubPath, "UTF-8")}",)
                NFSFileListScreen(
                    URLDecoder.decode(newSubPath, "UTF-8"),
                    mainNavController,
                    NFSConnection(
                        "1",
                        "ftp",

                        URLDecoder.decode(encodedIp, "UTF-8"),
                        URLDecoder.decode(encodedShareName, "UTF-8"),
                    )
                )
            }
        }
        composable("HTTPLinkFileListScreen/{encodedIp}/{newSubPath}") { backStackEntry ->
            val encodedIp = backStackEntry.arguments?.getString("encodedIp")
            val encodedShareName = backStackEntry.arguments?.getString("encodedShareName")
            val newSubPath = backStackEntry.arguments?.getString("newSubPath")
            //URLEncoder.encode(conn.shareName, "UTF-8")
            if (encodedIp != null) {

                //val path = URLDecoder.decode(URLDecoder.decode(encodedIp, "UTF-8"), "UTF-8")
                Log.d("encodedPath", "${URLDecoder.decode(newSubPath, "UTF-8")}",)
                HTTPLinkFileListScreen(
                    URLDecoder.decode(encodedIp, "UTF-8"),
                    URLDecoder.decode(newSubPath, "UTF-8"),
                    mainNavController
                )
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