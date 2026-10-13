package org.mz.mzdkplayer.ui

import org.mz.mzdkplayer.data.repository.AudioPlaylistRepository
import org.mz.mzdkplayer.ui.screen.setting.SolarSystem
import android.net.Uri
import android.util.Log
import androidx.annotation.OptIn
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import kotlinx.coroutines.delay
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.util.UnstableApi
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.navigation.navOptions
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import org.mz.mzdkplayer.MzDkPlayerApplication
import org.mz.mzdkplayer.R
import org.mz.mzdkplayer.data.model.FTPConnection
import org.mz.mzdkplayer.data.model.NFSConnection
import org.mz.mzdkplayer.data.model.WebDavConnection
import org.mz.mzdkplayer.di.RepositoryProvider
import org.mz.mzdkplayer.tool.logic.PhonePlayerLogic
import org.mz.mzdkplayer.tool.Tools.fromBase64
import org.mz.mzdkplayer.tool.Tools.toBase64
import org.mz.mzdkplayer.tool.Tools.toSafeInt
import org.mz.mzdkplayer.di.viewModelWithFactory
import org.mz.mzdkplayer.ui.audioplayer.AudioPlayerScreen

import org.mz.mzdkplayer.ui.picviewer.PicViewerScreen
import org.mz.mzdkplayer.ui.screen.common.EditTMDBInfoScreen
import org.mz.mzdkplayer.ui.screen.common.MzToast
import org.mz.mzdkplayer.common.MzToastManager
import org.mz.mzdkplayer.ui.screen.common.rememberMzToastState
import org.mz.mzdkplayer.ui.screen.movie.MovieDetailsScreen
import org.mz.mzdkplayer.ui.screen.history.MediaHistoryScreen
import org.mz.mzdkplayer.ui.screen.ftp.FTPConListScreen
import org.mz.mzdkplayer.ui.screen.ftp.FTPConScreen
import org.mz.mzdkplayer.ui.screen.ftp.FTPFileListScreen
import org.mz.mzdkplayer.ui.screen.filehome.FileHomeScreen
import org.mz.mzdkplayer.ui.screen.httplink.HTTPLinkFileListScreen
import org.mz.mzdkplayer.ui.screen.httplink.HTTPLinkConListScreen

import org.mz.mzdkplayer.ui.screen.localfile.FontPickerScreen
import org.mz.mzdkplayer.ui.screen.localfile.LocalFileListScreen
import org.mz.mzdkplayer.ui.screen.localfile.LocalFileTypeScreen
import org.mz.mzdkplayer.ui.screen.httplink.HTTPLinkConScreen
import org.mz.mzdkplayer.ui.screen.library.HomeScreen
import org.mz.mzdkplayer.ui.screen.library.MediaLibraryScreen
import org.mz.mzdkplayer.ui.screen.library.MediaLibraryTab
import org.mz.mzdkplayer.ui.screen.nfs.NFSConListScreen
import org.mz.mzdkplayer.ui.screen.nfs.NFSConScreen
import org.mz.mzdkplayer.ui.screen.nfs.NFSFileListScreen
import org.mz.mzdkplayer.ui.screen.search.SearchScreen
import org.mz.mzdkplayer.ui.screen.setting.BlackHoleSimulationScreen
import org.mz.mzdkplayer.ui.screen.smbfile.SMBConListScreen
import org.mz.mzdkplayer.ui.screen.smbfile.SMBConScreen
import org.mz.mzdkplayer.ui.screen.smbfile.SMBFileListScreen
import org.mz.mzdkplayer.ui.screen.webdavfile.WebDavConListScreen

import org.mz.mzdkplayer.ui.screen.webdavfile.WebDavConScreen
import org.mz.mzdkplayer.ui.screen.webdavfile.WebDavFileListScreen
import org.mz.mzdkplayer.ui.screen.setting.SettingsScreen
import org.mz.mzdkplayer.ui.screen.tv.TVSeriesDetailsScreen
import org.mz.mzdkplayer.viewmodel.AudioViewModel
import org.mz.mzdkplayer.viewmodel.FTPListViewModel
import org.mz.mzdkplayer.viewmodel.HTTPLinkListViewModel

import org.mz.mzdkplayer.viewmodel.MediaHistoryViewModel
import org.mz.mzdkplayer.viewmodel.MediaLibraryViewModel
import org.mz.mzdkplayer.viewmodel.NFSListViewModel
import org.mz.mzdkplayer.viewmodel.SMBListViewModel
import org.mz.mzdkplayer.viewmodel.SettingsViewModel
import org.mz.mzdkplayer.viewmodel.WebDavListViewModel

import org.mz.mzdkplayer.ui.videoplayer.VideoPlayerScreen
import java.net.URLDecoder
import java.net.URLEncoder

@OptIn(UnstableApi::class, ExperimentalTvMaterial3Api::class)
@Composable
fun MzDKPlayerAPP(
    externalVideoUri: Uri?,
    onExternalVideoConsumed: () -> Unit = {}
) {
    val coroutineScope = rememberCoroutineScope()
    LaunchedEffect(Unit) {
        MzToastManager.init(coroutineScope)
    }


    // 侧边栏收敛成 5 项：电影 / 剧集 / 音乐 合并进「媒体库」的分页标签，搜索移到主页搜索框，
    // 播放历史保留独立入口（追剧用户每天都要用）
    val items =
        listOf(
            stringResource(id = R.string.ui_label_home) to painterResource(id = R.drawable.baseline_home_24),
            stringResource(id = R.string.ui_label_media_library) to painterResource(id = R.drawable.moviefileicon),
            stringResource(id = R.string.ui_label_history) to painterResource(id = R.drawable.history24dp),
            stringResource(id = R.string.ui_label_file_browsing) to painterResource(id = R.drawable.baseline_folder_24),
            stringResource(id = R.string.ui_label_settings) to painterResource(id = R.drawable.baseline_settings_24),
        )
    // 与 items 一一对应的内层路由：既用于「当前路由 → 选中项」，也用于点击跳转
    val itemRoutes = listOf(
        "HomePage",
        "MediaLibraryPage",
        "HistoryPage",
        "FileHomePage",
        "SettingsPage",
    )
    var backPressedTime by remember { mutableLongStateOf(0L) }
    val backPressThreshold = 2000L // 2秒内再次按返回键才退出
    val mainNavController = rememberNavController()
    val context = LocalContext.current

    // 👇 关键：如果 externalVideoUri 存在，直接播放，不显示主页
    LaunchedEffect(externalVideoUri) {
        if (externalVideoUri != null) {
            val uriString = externalVideoUri.toString()
            Log.i("externalVideoUri",uriString)
            val extVideo = context.getString(R.string.ui_label_external_video).toBase64()
            mainNavController.navigate(
                "VideoPlayer/${uriString.toBase64()}/HTTP/$extVideo/$extVideo"
            )
            onExternalVideoConsumed()
        }
    }

    val smbListViewModel: SMBListViewModel = viewModel()
    val webDavListViewModel: WebDavListViewModel = viewModel()
    val ftpListViewModel: FTPListViewModel = viewModel()
    val httpLinkListViewModel: HTTPLinkListViewModel = viewModel()
    val nfsListViewModel: NFSListViewModel = viewModel()
    //val webDavListViewModel: WebDavListViewModel = viewModel()

    // 使用工厂初始化 ViewModel
    val libraryViewModel: MediaLibraryViewModel = viewModelWithFactory {
        RepositoryProvider.createMediaLibraryViewModel()
    }
    val audioViewModel: AudioViewModel = viewModelWithFactory {
        RepositoryProvider.createAudioViewModel()
    }

    val mediaHistoryViewModel: MediaHistoryViewModel = viewModelWithFactory {RepositoryProvider.createMediaHistoryViewModel()  }
    val settingsVM: SettingsViewModel = viewModel()
    val settingsState by settingsVM.uiState.collectAsState()
    NavHost(
        navController = mainNavController,
        startDestination = "MainPage",
        modifier = Modifier.background(Color.Black)
    )
    {

        composable("MainPage") {
            val homeNavController = rememberNavController()
            // 当前路由 → 侧边栏选中项。不再用一个独立的 selectedIndex 状态跟着点击走：
            // 那样从主页快捷入口跳进媒体库时，侧边栏会停在上一次点的那一项上。
            // 带参数的主页面（MediaLibraryPage?tab=xxx）在 NavDestination.route 里拿到的是
            // 带占位符的模式串，所以按「路由 + ?」前缀匹配。
            val currentHomeRoute = homeNavController.currentBackStackEntryAsState()
                .value?.destination?.route.orEmpty()
            // -1 = 当前页不在侧边栏里（比如搜索页），这时谁都不高亮
            val selectedIndex = itemRoutes
                .indexOfFirst { currentHomeRoute == it || currentHomeRoute.startsWith("$it?") }
            val contentFocusRequest = remember { FocusRequester() }

            // 悬浮侧边栏的容器：内容整屏铺开，胶囊按钮列浮在它上面（Box 里先内容、后侧边栏）
            Box(modifier = Modifier.fillMaxSize()) {
                    NavHost(
                        navController = homeNavController,
                        startDestination = "HomePage",
                        modifier = Modifier
                            .background(Color.Black)
                            .fillMaxSize()
                            // 左侧让出悬浮侧边栏的宽度：背景仍是全屏黑，看不出内容被让位，
                            // 只是列表第一项不会被按钮压住
                            .padding(start = FloatingSideBarReservedWidth)
                            .focusRequester(contentFocusRequest)
                    ) {
                        composable("HomePage") {
                            HomeScreen(libraryViewModel, mainNavController, homeNavController, settingsVM)
                        }
                        //声明名为MainPage的页面路由
                        composable("FileHomePage") {
                            //页面路由对应的页面组件
                            FileHomeScreen(mainNavController)
                        }
                        composable("SettingsPage") {
                            //页面路由对应的页面组件
                            SettingsScreen(mainNavController,settingsVM,audioViewModel)
                        }
                        composable("HistoryPage") {
                            //页面路由对应的页面组件
                            MediaHistoryScreen(mainNavController,mediaHistoryViewModel)
                        }
                        // 媒体库：电影 / 剧集 / 音乐 三个库收在一个页面里，用顶部标签切换。
                        // tab 是可选参数，侧边栏进来时用默认值（电影），主页快捷入口带 tab 进来
                        composable(
                            route = "MediaLibraryPage?tab={tab}",
                            arguments = listOf(navArgument("tab") {
                                type = NavType.StringType
                                defaultValue = MediaLibraryTab.DEFAULT.key
                            })
                        ) { backStackEntry ->
                            MediaLibraryScreen(
                                libraryViewModel = libraryViewModel,
                                audioViewModel = audioViewModel,
                                navController = mainNavController,
                                homeNavController = homeNavController,
                                settingsViewModel = settingsVM,
                                initialTab = backStackEntry.arguments?.getString("tab")
                                    ?: MediaLibraryTab.DEFAULT.key
                            )
                        }
                        composable("SearchPage") {
                            //页面路由对应的页面组件
                            SearchScreen(mainNavController)
                        }
                        // 主页搜索框带着关键词进来：词是 base64（与其它路由参数同一套编码口径），
                        // 单独一条路由而不是给 SearchPage 加可选参数 —— 空词时路由段会变成空串，匹配不上
                        composable("SearchPage/{query}") { backStackEntry ->
                            val encodedQuery = backStackEntry.arguments?.getString("query").orEmpty()
                            SearchScreen(
                                navController = mainNavController,
                                initialQuery = encodedQuery.fromBase64()
                            )
                        }

                    }

                    // 悬浮侧边栏：左侧居中一列胶囊按钮，点哪一项切哪一页
                    FloatingSideBar(
                        items = items,
                        selectedIndex = selectedIndex,
                        onSelect = { index ->
                            val target = itemRoutes[index]
                            homeNavController.navigate(
                                target,
                                navOptions = navOptions {
                                    launchSingleTop = true
                                    // 首页是内层导航的起点：点首页就把自己换掉，点其它页则把它留在栈底，
                                    // 于是内层栈深度恒为 1~2，返回键的落点始终可预期
                                    popUpTo("HomePage") {
                                        inclusive = target == "HomePage"
                                    }
                                }
                            )
                        },
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .padding(start = FloatingSideBarEdgePadding)
                    )
                }

            // 首次启动：等窗口真正拿到焦点后，再把焦点交给内容区。
            // 原来固定 delay(150)：冷启动 / 低端电视上这个时间点窗口还没拿到焦点，requestFocus 静默失败，
            // 焦点就留在侧边栏上 —— 表现为「一进应用焦点先落在侧边栏」（模拟器上必现）。
            // 改成等窗口焦点事件
            val windowFocused = LocalWindowInfo.current.isWindowFocused
            LaunchedEffect(windowFocused) {
                if (!windowFocused) return@LaunchedEffect
                delay(150)
                runCatching { contentFocusRequest.requestFocus() }
            }
        }
        composable("LocalFileTypeScreen") { backStackEntry ->
            LocalFileTypeScreen(mainNavController)
        }
        composable("LocalFileListScreen/{path}") { backStackEntry ->
            val encodedPath = backStackEntry.arguments?.getString("path")
            if (encodedPath != null) {
                val path = encodedPath.fromBase64()
                Log.d("encodedPath", path)
                LocalFileListScreen(path, mainNavController,settingsVM)

            }
        }
        composable("FontPickerScreen") {
            FontPickerScreen(null, mainNavController, settingsVM)
        }
        composable("FontPickerScreen/{path}") { backStackEntry ->
            val encodedPath = backStackEntry.arguments?.getString("path")
            val path = encodedPath?.fromBase64()
            FontPickerScreen(path, mainNavController, settingsVM)
        }
        composable("VideoPlayer/{sourceUri}/{dataSourceType}/{fileName}/{connectionName}") { backStackEntry ->

            //页面路由对应的页面组件
            val sourceUri = backStackEntry.arguments?.getString("sourceUri")
            val dataSourceType = backStackEntry.arguments?.getString("dataSourceType")
            val fileName = backStackEntry.arguments?.getString("fileName")
            val connectionNameEncoded = backStackEntry.arguments?.getString("connectionName")
            val connectionName = connectionNameEncoded?.fromBase64() ?: context.getString(R.string.ui_label_unknown)

            // ============ 强制 VLC 判断逻辑 ============

            // 检查参数是否不为空，并渲染屏幕
            if (sourceUri != null && dataSourceType != null && fileName != null) {
                //Log.d("sourceUri", sourceUri)
                //Log.d("dataSourceType", dataSourceType)
                val decodedUri = sourceUri.fromBase64()
                // 传输流与蓝光原盘强制走 VLC：判定口径与手机端共用 `PhonePlayerLogic`
                val forceVlcByExtension = PhonePlayerLogic.forceVlcByExtension(decodedUri)
                Log.d("VideoPlayer", "✅ Decoded MRL（传给 VLC）: $decodedUri")   // ← 关键！
                val shouldUseVlc = forceVlcByExtension || (settingsState.defaultPlayer == "vlc")
                VideoPlayerScreen(
                    decodedUri,
                    dataSourceType,
                    fileName.fromBase64(),
                    connectionName,
                    mediaHistoryViewModel,
                    shouldUseVlc,
                    settingsVM,
                    mainNavController
                )
            }
        }
        composable("AudioPlayer/{sourceUri}/{dataSourceType}/{fileName}/{connectionName}/{currentIndex}") { backStackEntry ->

            //页面路由对应的页面组件
            val sourceUri = backStackEntry.arguments?.getString("sourceUri")
            val dataSourceType = backStackEntry.arguments?.getString("dataSourceType")
            val fileName = backStackEntry.arguments?.getString("fileName")
            val currentIndex = backStackEntry.arguments?.getString("currentIndex") ?: "1"
            val connectionNameEncoded = backStackEntry.arguments?.getString("connectionName")
            val connectionName = connectionNameEncoded?.fromBase64() ?: context.getString(R.string.ui_label_unknown)
            // 从 Repository 获取播放列表
            val extraList by AudioPlaylistRepository.playlist.collectAsState()
            // 检查参数是否不为空，并渲染屏幕
            if (sourceUri != null && dataSourceType != null) {
                Log.d("sourceUri", sourceUri)
                Log.d("dataSourceType", dataSourceType)
                AudioPlayerScreen(
                    sourceUri.fromBase64(),
                    dataSourceType,
                    fileName?.fromBase64() ?: context.getString(R.string.ui_label_unknown_filename),
                    extraList,
                    currentIndex = currentIndex,
                    connectionName,
                    mediaHistoryViewModel,
                    audioViewModel = audioViewModel

                )
            }
        }
        composable("PicViewer/{sourceUri}/{dataSourceType}/{fileName}/{connectionName}") { backStackEntry ->

            //页面路由对应的页面组件
            val sourceUri = backStackEntry.arguments?.getString("sourceUri")
            val dataSourceType = backStackEntry.arguments?.getString("dataSourceType")
            val fileName = backStackEntry.arguments?.getString("fileName")

            val connectionNameEncoded = backStackEntry.arguments?.getString("connectionName")
            val connectionName = connectionNameEncoded?.fromBase64() ?: context.getString(R.string.ui_label_unknown)

            // 检查参数是否不为空，并渲染屏幕
            if (sourceUri != null && dataSourceType != null) {
                Log.d("sourceUri", sourceUri)
                Log.d("dataSourceType", dataSourceType)
                PicViewerScreen(
                    sourceUri.fromBase64(),
                    dataSourceType,
                    fileName?.fromBase64() ?: context.getString(R.string.ui_label_unknown_filename),
                    connectionName,
                )
            }
        }
        composable("EditTMDBInfoScreen/{mediaUri}") { backStackEntry ->
            val mediaUri = backStackEntry.arguments?.getString("mediaUri")
            if (mediaUri != null) {
                EditTMDBInfoScreen(mediaUri.fromBase64(), navController = mainNavController)
            }
        }

        composable("SMBFileListScreen/{path}/{connectionName}") { backStackEntry ->
            val encodedPath = backStackEntry.arguments?.getString("path")
            val connectionName = backStackEntry.arguments?.getString("connectionName")

            if (encodedPath != null) {
                // 🟢 路径和连接名使用 Base64 解码
                val path = encodedPath.fromBase64()
                val decodedConnectionName = connectionName?.fromBase64() ?: stringResource(R.string.ui_label_unknown)

                SMBFileListScreen(path, mainNavController, decodedConnectionName, settingsVM)
            }
        }
        composable("WebDavFileListScreen/{path}/{username}/{pw}/{connectionName}") { backStackEntry ->
            val encodedPath = backStackEntry.arguments?.getString("path")
            val username = backStackEntry.arguments?.getString("username")
            val pw = backStackEntry.arguments?.getString("pw")
            val connectionNameEncoded = backStackEntry.arguments?.getString("connectionName")
            val decodedConnectionName = connectionNameEncoded?.fromBase64() ?: context.getString(R.string.ui_label_unknown)
            if (encodedPath != null && username != null && pw != null) {

                val path = encodedPath.fromBase64()
                val decodedUsername = username.fromBase64()
                val decodedPw = pw.fromBase64()

                Log.d("encodedPath", path)
                WebDavFileListScreen(
                    path,
                    mainNavController,
                    WebDavConnection("1", decodedConnectionName, path, decodedUsername, decodedPw),
                    settingsVM
                )
            }
        }
        composable("FTPFileListScreen/{encodedIp}/{encodedUsername}/{encodedPassword}/{port}/{encodedShareName}/{connectionName}") { backStackEntry ->
            val encodedIp = backStackEntry.arguments?.getString("encodedIp")
            val encodedUsername = backStackEntry.arguments?.getString("encodedUsername")
            val encodedPassword = backStackEntry.arguments?.getString("encodedPassword")
            val encodedShareName = backStackEntry.arguments?.getString("encodedShareName")
            val connectionNameEncoded = backStackEntry.arguments?.getString("connectionName")
            val decodedConnectionName = connectionNameEncoded?.fromBase64() ?: context.getString(R.string.ui_label_unknown)
            val port = backStackEntry.arguments?.getString("port") ?: "21"
            //URLEncoder.encode(conn.shareName, "UTF-8")
            if (encodedIp != null) {

                val decodedIp = encodedIp.fromBase64()
                val decodedUsername = encodedUsername?.fromBase64() ?: ""
                val decodedPassword = encodedPassword?.fromBase64() ?: ""
                val decodedShareName = encodedShareName?.fromBase64() ?: ""

                Log.d(
                    "encodedPath",
                    "$decodedUsername$decodedPassword$decodedShareName"
                )
                FTPFileListScreen(
                    decodedShareName,
                    mainNavController,
                    FTPConnection(
                        "1",
                        decodedConnectionName,
                        ip = decodedIp,
                        port.toSafeInt(21),
                        decodedUsername,
                        decodedPassword,
                        shareName = decodedShareName,
                    ),
                    settingsVM
                )
            }
        }
        composable("NFSFileListScreen/{encodedIp}/{encodedShareName}/{newSubPath}/{connectionName}") { backStackEntry ->
            val encodedIp = backStackEntry.arguments?.getString("encodedIp")
            val encodedShareName = backStackEntry.arguments?.getString("encodedShareName")
            val newSubPath = backStackEntry.arguments?.getString("newSubPath")
            val connectionNameEncoded = backStackEntry.arguments?.getString("connectionName")
            val decodedConnectionName = connectionNameEncoded?.fromBase64() ?: context.getString(R.string.ui_label_unknown)
            //URLEncoder.encode(conn.shareName, "UTF-8")
            if (encodedIp != null) {

                val decodedIp = encodedIp.fromBase64()
                val decodedShareName = encodedShareName?.fromBase64() ?: ""
                val decodedSubPath = newSubPath?.fromBase64() ?: ""

                Log.d("encodedPath", decodedSubPath)
                NFSFileListScreen(
                    decodedSubPath,
                    mainNavController,
                    NFSConnection(
                        "1",
                        decodedConnectionName,
                        decodedIp,
                        decodedShareName,
                        ),
                    settingsVM
                )
            }
        }
        composable("HTTPLinkFileListScreen/{connectionName}/{newSubPath}") { backStackEntry ->
            val newSubPath = backStackEntry.arguments?.getString("newSubPath")
            val connectionNameEncoded = backStackEntry.arguments?.getString("connectionName")
            val decodedConnectionName = connectionNameEncoded?.fromBase64() ?: context.getString(R.string.ui_label_unknown)
            //URLEncoder.encode(conn.shareName, "UTF-8")
            if (newSubPath != null) {
                val decodedSubPath = newSubPath.fromBase64()
                Log.d("encodedPath", decodedSubPath)
                HTTPLinkFileListScreen(
                    decodedSubPath,
                    mainNavController,
                    decodedConnectionName,
                    settingsVM
                )
            }
        }
        composable("MovieDetails/{videoUri}/{dataSourceType}/{fileName}/{connectionName}/{movieId}") { backStackEntry ->
            val videoUri = backStackEntry.arguments?.getString("videoUri") ?: ""
            val dataSourceType = backStackEntry.arguments?.getString("dataSourceType") ?: ""
            val fileName = backStackEntry.arguments?.getString("fileName") ?: ""
            val connectionName = backStackEntry.arguments?.getString("connectionName") ?: ""
            val movieId = backStackEntry.arguments?.getString("movieId")?.toIntOrNull() ?: 0

            MovieDetailsScreen(
                videoUri = videoUri,
                dataSourceType = dataSourceType,
                fileName = fileName,
                connectionName = connectionName,
                movieId = movieId,
                navController = mainNavController
            )
        }
        composable("TVSeriesDetails/{videoUri}/{dataSourceType}/{fileName}/{connectionName}/{seriesId}/{seasonNumber}/{episodeNumber}") { backStackEntry ->
            val videoUri = backStackEntry.arguments?.getString("videoUri") ?: ""
            val dataSourceType = backStackEntry.arguments?.getString("dataSourceType") ?: ""
            val fileName = backStackEntry.arguments?.getString("fileName") ?: ""
            val connectionName = backStackEntry.arguments?.getString("connectionName") ?: ""
            // TV 剧集特有参数
            val seriesId = backStackEntry.arguments?.getString("seriesId")?.toIntOrNull() ?: 0
            val seasonNumber = backStackEntry.arguments?.getString("seasonNumber")?.toIntOrNull() ?: 0
            val episodeNumber = backStackEntry.arguments?.getString("episodeNumber")?.toIntOrNull() ?: 0

            TVSeriesDetailsScreen(
                videoUri = videoUri,
                dataSourceType = dataSourceType,
                fileName = fileName,
                connectionName = connectionName,
                seriesId = seriesId,
                currentSeason = seasonNumber,   // 传递季号
                currentEpisode = episodeNumber, // 传递集号
                navController = mainNavController
            )
        }
        composable("SMBListScreen") {
            SMBConListScreen(mainNavController, smbListViewModel)
        }
        composable(
            route = "SMBConScreen?connId={connId}",
            arguments = listOf(navArgument("connId") {
                type = NavType.StringType
                defaultValue = ""
            })
        ) { backStackEntry ->
            SMBConScreen(
                mainNavController = mainNavController,
                connectionId = backStackEntry.arguments?.getString("connId")?.ifBlank { null },
                smbListViewModel = smbListViewModel
            )
        }
        composable(
            route = "WebDavConScreen?connId={connId}",
            arguments = listOf(navArgument("connId") {
                type = NavType.StringType
                defaultValue = ""
            })
        ) { backStackEntry ->
            WebDavConScreen(
                mainNavController = mainNavController,
                connectionId = backStackEntry.arguments?.getString("connId")?.ifBlank { null },
                webDavListViewModel = webDavListViewModel
            )
        }
        composable("WebDavListScreen") {
            WebDavConListScreen(mainNavController,webDavListViewModel)
        }
        composable(
            route = "FTPConScreen?connId={connId}",
            arguments = listOf(navArgument("connId") {
                type = NavType.StringType
                defaultValue = ""
            })
        ) { backStackEntry ->
            FTPConScreen(
                mainNavController = mainNavController,
                connectionId = backStackEntry.arguments?.getString("connId")?.ifBlank { null },
                ftpListViewModel = ftpListViewModel
            )
        }
        composable("FTPConListScreen") {
            FTPConListScreen(mainNavController,ftpListViewModel)
        }
        composable(
            route = "NFSConScreen?connId={connId}",
            arguments = listOf(navArgument("connId") {
                type = NavType.StringType
                defaultValue = ""
            })
        ) { backStackEntry ->
            NFSConScreen(
                mainNavController = mainNavController,
                connectionId = backStackEntry.arguments?.getString("connId")?.ifBlank { null },
                nfsListViewModel = nfsListViewModel
            )
        }
        composable("NFSConListScreen") {
            NFSConListScreen(mainNavController,nfsListViewModel)
        }
        composable(
            route = "HTTPLinkConScreen?connId={connId}",
            arguments = listOf(navArgument("connId") {
                type = NavType.StringType
                defaultValue = ""
            })
        ) { backStackEntry ->
            HTTPLinkConScreen(
                mainNavController = mainNavController,
                connectionId = backStackEntry.arguments?.getString("connId")?.ifBlank { null },
                httpLinkListViewModel = httpLinkListViewModel
            )
        }
        composable("HTTPLinkConListScreen") {
            HTTPLinkConListScreen(mainNavController,httpLinkListViewModel)
        }
        composable("SolarSystemScreen") {
            SolarSystem()
        }
        composable("BlackHoleSimulationScreen") {
            BlackHoleSimulationScreen(modifier = Modifier.fillMaxSize())
        }

    }
    MzToast(state = MzToastManager.state)

}

// ===== 悬浮侧边栏（电视端）=====
// 尺寸集中在这里：圆钮直径、展开后的胶囊宽度、距屏幕左缘的留白。
// 内容区左侧让出的宽度 = 圆钮直径 + 左缘留白 + 一点呼吸空间
private val FloatingSideBarItemSize = 52.dp
private val FloatingSideBarItemExpandedWidth = 196.dp
private val FloatingSideBarEdgePadding = 14.dp
private val FloatingSideBarReservedWidth =
    FloatingSideBarItemSize + FloatingSideBarEdgePadding + 6.dp

/** 选中项底色：与媒体库分页标签、主页快捷入口用的是同一块暖白 */
private val FloatingSideBarSelectedColor = Color(0xFFFFF6E5)

/**
 * 电视端侧边栏：贴在屏幕左侧、浮在内容之上的一列胶囊按钮。
 *
 * 与原来的抽屉（`ModalNavigationDrawer`）的区别：
 * - 不再推挤内容，也没有自己的深色背景板 —— 内容整屏铺开，按钮直接浮在它上面；
 * - 没有「展开 / 收起」两个形态，平时就是一枚枚圆钮，焦点落在哪一枚、哪一枚横向拉长露出名字；
 * - 于是也就不存在「冷启动焦点落进侧边栏把抽屉带开」的问题，返回键无需再拦截。
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun FloatingSideBar(
    items: List<Pair<String, Painter>>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.selectableGroup(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.Start
    ) {
        items.forEachIndexed { index, item ->
            FloatingSideBarItem(
                text = item.first,
                icon = item.second,
                selected = selectedIndex == index,
                onClick = { onSelect(index) }
            )
        }
    }
}

/** 单枚胶囊按钮：收起时是圆钮（只有图标），拿到焦点时横向拉长并显示名字 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun FloatingSideBarItem(
    text: String,
    icon: Painter,
    selected: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()

    // 展开只由宽度动画驱动、不做文字的出入场动画：文字位置固定，由外层裁剪出宽度，
    // 看起来像是从左往右把名字「露」出来（左缘贴屏幕，胶囊只能朝右长），比淡入淡出更贴合
    // 一枚胶囊被拉长的物理感
    val width by animateDpAsState(
        targetValue = if (focused) FloatingSideBarItemExpandedWidth else FloatingSideBarItemSize,
        animationSpec = tween(durationMillis = 220),
        label = "floatingSideBarItemWidth"
    )
    val containerColor = when {
        selected -> FloatingSideBarSelectedColor
        focused -> Color(0xFF3C3C3C)
        else -> Color(0xE61C1C1C)
    }
    val contentColor =
        if (selected) Color.Black else Color.White.copy(alpha = if (focused) 1f else 0.75f)
    val shape = RoundedCornerShape(percent = 50)

    Surface(
        onClick = onClick,
        modifier = Modifier
            .width(width)
            .height(FloatingSideBarItemSize)
            // 投影是为了「浮」起来：黑底上按钮与内容之间靠这圈阴影和描边分层
            .shadow(elevation = 12.dp, shape = shape),
        interactionSource = interactionSource,
        shape = ClickableSurfaceDefaults.shape(shape = shape),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = containerColor,
            contentColor = contentColor,
            focusedContainerColor = containerColor,
            focusedContentColor = contentColor
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.04f),
        border = ClickableSurfaceDefaults.border(
            focusedBorder = Border(
                border = BorderStroke(
                    2.dp,
                    // 暖白底上再描一圈白看不见，换成压暗的描边
                    if (selected) Color.Black.copy(alpha = 0.55f) else Color.White.copy(alpha = 0.7f)
                ),
                inset = 0.dp
            )
        )
    ) {
        Row(
            // 收起时 52dp 里图标正好居中：左侧留白 =（52 - 24）/ 2
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = icon,
                contentDescription = text,
                modifier = Modifier.size(24.dp),
                tint = contentColor
            )
            if (focused) {
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = text,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Clip
                )
            }
        }
    }
}