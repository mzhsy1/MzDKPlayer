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
import org.mz.mzdkplayer.logic.model.WebDavConnection


import org.mz.mzdkplayer.ui.screen.HomeScreen
import org.mz.mzdkplayer.ui.screen.localfile.LocalFileScreen
import org.mz.mzdkplayer.ui.screen.localfile.LocalFileTypeScreen
import org.mz.mzdkplayer.ui.screen.smbfile.SMBConListScreen
import org.mz.mzdkplayer.ui.screen.smbfile.SMBConScreen
import org.mz.mzdkplayer.ui.screen.smbfile.SMBFileListScreen

import org.mz.mzdkplayer.ui.screen.webdavfile.WebDavConScreen
import org.mz.mzdkplayer.ui.screen.webdavfile.WebDavFileListScreen
import org.mz.mzdkplayer.ui.screen.webdavfile.WebDavListScreen
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
                LocalFileScreen(path,mainNavController)

            }
        }
        composable("VideoPlayer/{smburi}") { backStackEntry ->
            //val bvid = backStackEntry.arguments?.getString("smburi")
            //页面路由对应的页面组件
            val smburi = backStackEntry.arguments?.getString("smburi")

            // 检查参数是否不为空，并渲染屏幕
            if (smburi != null) {
                Log.d("smburi", smburi)
                VideoPlayerScreen(URLDecoder.decode(smburi, "UTF-8"))
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
            if (encodedPath != null&&username!=null&&pw!=null) {

                val path = URLDecoder.decode(encodedPath, "UTF-8")
                Log.d("encodedPath", path)
                WebDavFileListScreen(path, mainNavController, WebDavConnection("1","webdav",path,username,pw,))
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
            WebDavListScreen(mainNavController)
        }
    }

}