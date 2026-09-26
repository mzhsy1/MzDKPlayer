package org.mz.mzdkplayer.ui.phone

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.annotation.StringRes
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ShortNavigationBar
import androidx.compose.material3.ShortNavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import org.mz.mzdkplayer.R
import org.mz.mzdkplayer.data.model.PhoneThemeMode
import org.mz.mzdkplayer.data.repository.AudioPlaylistRepository
import org.mz.mzdkplayer.data.repository.SettingsRepository
import org.mz.mzdkplayer.di.RepositoryProvider
import org.mz.mzdkplayer.tool.PhoneMediaLogic
import org.mz.mzdkplayer.tool.PhoneThemeLogic
import org.mz.mzdkplayer.tool.Tools.fromBase64
import org.mz.mzdkplayer.tool.viewModelWithFactory
import org.mz.mzdkplayer.ui.phone.screen.PhoneAudioPlayerScreen
import org.mz.mzdkplayer.ui.phone.screen.PhoneDetailScreen
import org.mz.mzdkplayer.ui.phone.screen.PhoneFileProtocol
import org.mz.mzdkplayer.ui.phone.screen.PhoneFilesScreen
import org.mz.mzdkplayer.ui.phone.screen.PhoneFtpBrowserScreen
import org.mz.mzdkplayer.ui.phone.screen.PhoneHomeScreen
import org.mz.mzdkplayer.ui.phone.screen.PhoneHttpBrowserScreen
import org.mz.mzdkplayer.ui.phone.screen.PhoneImageViewerScreen
import org.mz.mzdkplayer.ui.phone.screen.PhoneLocalBrowserScreen
import org.mz.mzdkplayer.ui.phone.screen.PhoneMatchScreen
import org.mz.mzdkplayer.ui.phone.screen.PhoneNfsBrowserScreen
import org.mz.mzdkplayer.ui.phone.screen.PhonePlayerScreen
import org.mz.mzdkplayer.ui.phone.screen.PhoneSettingsScreen
import org.mz.mzdkplayer.ui.phone.screen.PhoneSmbBrowserScreen
import org.mz.mzdkplayer.ui.phone.screen.PhoneWebDavBrowserScreen
import org.mz.mzdkplayer.viewmodel.AudioViewModel
import org.mz.mzdkplayer.viewmodel.FTPConViewModel
import org.mz.mzdkplayer.viewmodel.FTPListViewModel
import org.mz.mzdkplayer.viewmodel.HTTPLinkConViewModel
import org.mz.mzdkplayer.viewmodel.HTTPLinkListViewModel
import org.mz.mzdkplayer.viewmodel.MediaHistoryViewModel
import org.mz.mzdkplayer.viewmodel.MediaLibraryViewModel
import org.mz.mzdkplayer.viewmodel.MediaMetaViewModel
import org.mz.mzdkplayer.viewmodel.MovieViewModel
import org.mz.mzdkplayer.viewmodel.NFSConViewModel
import org.mz.mzdkplayer.viewmodel.NFSListViewModel
import org.mz.mzdkplayer.viewmodel.SMBConViewModel
import org.mz.mzdkplayer.viewmodel.SMBListViewModel
import org.mz.mzdkplayer.viewmodel.SettingsUiState
import org.mz.mzdkplayer.viewmodel.SettingsViewModel
import org.mz.mzdkplayer.viewmodel.WebDavConViewModel
import org.mz.mzdkplayer.viewmodel.WebDavListViewModel

/**
 * 手机端根组件：主题 + 底部导航 + 页面路由。
 *
 * 业务逻辑全部复用现有代码（各协议 `XxxListViewModel` / `XxxConViewModel` / `MovieViewModel` /
 * `MediaLibraryViewModel` / `MzExoPlayer` / `SettingsRepository`），
 * 这里只负责手机端的 Material 3 Expressive 外壳。
 */
@Composable
fun PhoneApp() {
    // 主题设置只在进入时读一次（SharedPreferences 是同步的），改完立刻写回；
    // Activity 重建时会重新读，所以不需要额外的 ViewModel。
    var themeMode by remember {
        mutableStateOf(PhoneThemeLogic.themeModeFromStorage(SettingsRepository.phoneThemeMode))
    }
    var dynamicColor by remember { mutableStateOf(SettingsRepository.phoneDynamicColor) }

    val darkTheme = PhoneThemeLogic.resolveDarkTheme(themeMode, isSystemInDarkTheme())

    PhoneTheme(darkTheme = darkTheme, dynamicColor = dynamicColor) {
        PhoneSystemBars(darkTheme = darkTheme)
        PhoneRoot(
            themeMode = themeMode,
            onThemeModeChange = { mode ->
                themeMode = mode
                SettingsRepository.phoneThemeMode = mode.name
            },
            dynamicColor = dynamicColor,
            onDynamicColorChange = { enabled ->
                dynamicColor = enabled
                SettingsRepository.phoneDynamicColor = enabled
            },
        )
    }
}

/**
 * 主题由 Compose 驱动（不调 `AppCompatDelegate.setDefaultNightMode`，避免整个 Activity 重建），
 * 所以状态栏/导航栏的图标明暗要手动跟着走。
 */
@Composable
private fun PhoneSystemBars(darkTheme: Boolean) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = view.context.findActivity()?.window ?: return@SideEffect
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }
}

/** `view.context` 可能是被包装过的 ContextThemeWrapper，一层层剥到 Activity 再取 window */
private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

/** 底部导航的标签页 */
private enum class PhoneTab(
    val route: String,
    @StringRes val labelRes: Int,
    val icon: ImageVector,
) {
    HOME(PhoneRoutes.HOME, R.string.ui_label_home, Icons.Filled.Home),
    FILES(PhoneRoutes.FILES, R.string.phone_nav_files, PhoneIcons.Folder),
    SETTINGS(PhoneRoutes.SETTINGS, R.string.ui_label_settings, Icons.Filled.Settings),
}

@Composable
private fun PhoneRoot(
    themeMode: PhoneThemeMode,
    onThemeModeChange: (PhoneThemeMode) -> Unit,
    dynamicColor: Boolean,
    onDynamicColorChange: (Boolean) -> Unit,
) {
    val navController = rememberNavController()

    // 五个协议的两类 ViewModel 都挂在 Activity 作用域：
    // 在目录之间来回跳、切到设置再切回来，都不会重新建立连接。
    // List = 连接增删（SharedPreferences），Con = 连接与列目录。
    val smbListViewModel: SMBListViewModel = viewModel()
    val smbConViewModel: SMBConViewModel = viewModel()
    val ftpListViewModel: FTPListViewModel = viewModel()
    val ftpConViewModel: FTPConViewModel = viewModel()
    val nfsListViewModel: NFSListViewModel = viewModel()
    val nfsConViewModel: NFSConViewModel = viewModel()
    val webDavListViewModel: WebDavListViewModel = viewModel()
    val webDavConViewModel: WebDavConViewModel = viewModel()
    val httpLinkListViewModel: HTTPLinkListViewModel = viewModel()
    val httpLinkConViewModel: HTTPLinkConViewModel = viewModel()

    // 刮削相关的三个 ViewModel 也挂在 Activity 作用域：
    // 进目录 / 进匹配页 / 退回列表共用同一份刮削状态，不会来回抖动。
    val movieViewModel: MovieViewModel = viewModelWithFactory { RepositoryProvider.createMovieViewModel() }
    val mediaMetaViewModel: MediaMetaViewModel = viewModelWithFactory { RepositoryProvider.createMediaMetaViewModel() }
    val mediaLibraryViewModel: MediaLibraryViewModel =
        viewModelWithFactory { RepositoryProvider.createMediaLibraryViewModel() }
    // 各协议的自动刮削开关（电视端「设置 → 刮削与媒体库」里的那一组）
    val settingsViewModel: SettingsViewModel = viewModel()
    val settingsState by settingsViewModel.uiState.collectAsState()

    // 第五阶段：音频元数据（audio_cache）与播放历史（media_history）都挂在 Activity 作用域，
    // 换歌 / 退出播放页回来不会每次重建
    val audioViewModel: AudioViewModel =
        viewModelWithFactory { RepositoryProvider.createAudioViewModel() }
    val mediaHistoryViewModel: MediaHistoryViewModel =
        viewModelWithFactory { RepositoryProvider.createMediaHistoryViewModel() }

    // 点音频 / 图片时的统一出口：音频先把整张播放列表写进仓库再进播放页
    // （播放页从仓库读回来，路由里只带下标，见 `PhoneRoutes.AUDIO` 的注释）
    val openMedia: (PhoneMediaLogic.MediaOpen) -> Unit = { open ->
        when (open.kind) {
            PhoneMediaLogic.Kind.AUDIO -> {
                AudioPlaylistRepository.setPlaylist(open.audioItems)
                navController.navigate(
                    PhoneRoutes.audio(
                        currentIndex = open.currentIndex,
                        dataSourceType = open.dataSourceType,
                        connectionName = open.connectionName,
                    )
                )
            }

            PhoneMediaLogic.Kind.IMAGE -> navController.navigate(
                PhoneRoutes.image(
                    dataSourceType = open.dataSourceType,
                    uris = open.imageUris,
                    index = open.currentIndex,
                )
            )

            // 视频走原有的播放路由，不该出现在这里
            PhoneMediaLogic.Kind.VIDEO, PhoneMediaLogic.Kind.OTHER -> Unit
        }
    }

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showNavigationBar = PhoneTab.entries.any { it.route == currentRoute }

    Scaffold(
        // 底部栏自带导航栏内边距（ShortNavigationBarDefaults.windowInsets）；
        // 状态栏内边距交给每个页面自己的 TopAppBar，避免出现双重留白。
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (showNavigationBar) {
                ShortNavigationBar {
                    PhoneTab.entries.forEach { tab ->
                        ShortNavigationBarItem(
                            selected = currentRoute == tab.route,
                            onClick = { navController.switchTab(tab.route) },
                            icon = { Icon(tab.icon, contentDescription = null) },
                            label = { Text(stringResource(tab.labelRes)) },
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = PhoneRoutes.HOME,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            composable(PhoneRoutes.HOME) {
                PhoneHomeScreen(
                    libraryViewModel = mediaLibraryViewModel,
                    onOpenFiles = { navController.switchTab(PhoneRoutes.FILES) },
                    onOpenSettings = { navController.switchTab(PhoneRoutes.SETTINGS) },
                    onPlay = { sourceUri, dataSourceType, title ->
                        navController.navigate(PhoneRoutes.player(sourceUri, dataSourceType, title))
                    },
                )
            }

            composable(PhoneRoutes.FILES) {
                PhoneFilesScreen(
                    smbListViewModel = smbListViewModel,
                    ftpListViewModel = ftpListViewModel,
                    nfsListViewModel = nfsListViewModel,
                    webDavListViewModel = webDavListViewModel,
                    httpLinkListViewModel = httpLinkListViewModel,
                    onOpen = { protocol, connectionId, path ->
                        // SMB 用的是自带「本地网络权限」引导的页面，
                        // 其余五个协议（本机 / FTP / NFS / WebDAV / HTTP）走通用浏览页
                        val route = if (protocol == PhoneFileProtocol.SMB) {
                            PhoneRoutes.smbBrowser(connectionId, path)
                        } else {
                            PhoneRoutes.browser(protocol.routeValue, connectionId, path)
                        }
                        navController.navigate(route)
                    },
                )
            }

            composable(PhoneRoutes.SETTINGS) {
                PhoneSettingsScreen(
                    themeMode = themeMode,
                    onThemeModeChange = onThemeModeChange,
                    dynamicColor = dynamicColor,
                    onDynamicColorChange = onDynamicColorChange,
                    autoScrape = settingsState.phoneAutoScrape,
                    onAutoScrapeChange = settingsViewModel::togglePhoneAutoScrape,
                    scrapeSources = PhoneFileProtocol.entries.map {
                        it to settingsState.sourceEnabled(it)
                    },
                    onScrapeSourceChange = { protocol, enabled ->
                        settingsViewModel.toggleSource(protocol.scrapeKey, enabled)
                    },
                )
            }

            // 刮削到一半、或想给某个文件换一个匹配结果时打开；写库后回上一页，列表会重新读一次缓存
            composable(
                route = PhoneRoutes.MATCH,
                arguments = listOf(
                    navArgument("videoUri") { type = NavType.StringType },
                    navArgument("dataSourceType") { type = NavType.StringType },
                    navArgument("fileName") { type = NavType.StringType },
                    navArgument("connectionName") { type = NavType.StringType },
                ),
            ) { entry ->
                PhoneMatchScreen(
                    videoUri = entry.arguments?.getString("videoUri").orEmpty().fromBase64(),
                    dataSourceType = entry.arguments?.getString("dataSourceType").orEmpty(),
                    fileName = entry.arguments?.getString("fileName").orEmpty().fromBase64(),
                    connectionName = PhoneRoutes.decodeArg(entry.arguments?.getString("connectionName")),
                    movieViewModel = movieViewModel,
                    mediaMetaViewModel = mediaMetaViewModel,
                    onBack = { navController.popBackStack() },
                )
            }

            composable(
                route = PhoneRoutes.SMB_BROWSER,
                arguments = listOf(
                    navArgument("connectionId") { type = NavType.StringType },
                    navArgument("path") { type = NavType.StringType },
                ),
            ) { entry ->
                val connectionId = entry.arguments?.getString("connectionId").orEmpty().fromBase64()
                val path = entry.arguments?.getString("path").orEmpty().fromBase64()
                    .ifEmpty { SMB_ROOT_PATH }

                PhoneSmbBrowserScreen(
                    connectionId = connectionId,
                    path = path,
                    smbListViewModel = smbListViewModel,
                    smbConViewModel = smbConViewModel,
                    movieViewModel = movieViewModel,
                    mediaMetaViewModel = mediaMetaViewModel,
                    autoScrape = settingsState.autoScrapeEnabled(PhoneFileProtocol.SMB),
                    onBack = { navController.popBackStack() },
                    onOpenDirectory = { targetPath ->
                        navController.navigate(PhoneRoutes.smbBrowser(connectionId, targetPath))
                    },
                    onPlayVideo = { sourceUri, name ->
                        navController.navigate(
                            PhoneRoutes.player(
                                sourceUri = sourceUri,
                                dataSourceType = PhoneFileProtocol.SMB.routeValue,
                                title = name,
                            )
                        )
                    },
                    onOpenMedia = openMedia,
                    onOpenDetail = { sourceUri, name, connName ->
                        navController.navigate(
                            PhoneRoutes.detail(
                                videoUri = sourceUri,
                                dataSourceType = PhoneFileProtocol.SMB.routeValue,
                                fileName = name,
                                connectionName = connName,
                            )
                        )
                    },
                )
            }

            composable(
                route = PhoneRoutes.BROWSER,
                arguments = listOf(
                    navArgument("protocol") { type = NavType.StringType },
                    navArgument("connectionId") { type = NavType.StringType },
                    navArgument("path") { type = NavType.StringType },
                ),
            ) { entry ->
                val protocolName = entry.arguments?.getString("protocol").orEmpty()
                val protocol = PhoneFileProtocol.entries.firstOrNull { it.routeValue == protocolName }
                val connectionId = PhoneRoutes.decodeArg(entry.arguments?.getString("connectionId"))
                val path = PhoneRoutes.decodeArg(entry.arguments?.getString("path"))

                val onOpenDirectory: (String) -> Unit = { targetPath ->
                    navController.navigate(
                        PhoneRoutes.browser(protocolName, connectionId, targetPath)
                    )
                }
                val onPlayVideo: (String, String) -> Unit = { sourceUri, name ->
                    navController.navigate(PhoneRoutes.player(sourceUri, protocolName, name))
                }
                // 连接名由各协议页自己带上来（本机文件为空串）；详情页里再决定要不要去「重新匹配」
                val onOpenDetail: (String, String, String) -> Unit = { sourceUri, name, connName ->
                    navController.navigate(
                        PhoneRoutes.detail(
                            videoUri = sourceUri,
                            dataSourceType = protocolName,
                            fileName = name,
                            connectionName = connName,
                        )
                    )
                }
                val onBack: () -> Unit = { navController.popBackStack() }

                when (protocol) {
                    PhoneFileProtocol.LOCAL -> PhoneLocalBrowserScreen(
                        path = path,
                        movieViewModel = movieViewModel,
                        mediaMetaViewModel = mediaMetaViewModel,
                        autoScrape = settingsState.autoScrapeEnabled(PhoneFileProtocol.LOCAL),
                        onBack = onBack,
                        onOpenDirectory = onOpenDirectory,
                        onPlayVideo = onPlayVideo,
                        onOpenMedia = openMedia,
                        onOpenDetail = onOpenDetail,
                    )

                    PhoneFileProtocol.FTP -> PhoneFtpBrowserScreen(
                        connectionId = connectionId,
                        path = path,
                        ftpListViewModel = ftpListViewModel,
                        ftpConViewModel = ftpConViewModel,
                        movieViewModel = movieViewModel,
                        mediaMetaViewModel = mediaMetaViewModel,
                        autoScrape = settingsState.autoScrapeEnabled(PhoneFileProtocol.FTP),
                        onBack = onBack,
                        onOpenDirectory = onOpenDirectory,
                        onPlayVideo = onPlayVideo,
                        onOpenMedia = openMedia,
                        onOpenDetail = onOpenDetail,
                    )

                    PhoneFileProtocol.NFS -> PhoneNfsBrowserScreen(
                        connectionId = connectionId,
                        path = path,
                        nfsListViewModel = nfsListViewModel,
                        nfsConViewModel = nfsConViewModel,
                        movieViewModel = movieViewModel,
                        mediaMetaViewModel = mediaMetaViewModel,
                        autoScrape = settingsState.autoScrapeEnabled(PhoneFileProtocol.NFS),
                        onBack = onBack,
                        onOpenDirectory = onOpenDirectory,
                        onPlayVideo = onPlayVideo,
                        onOpenMedia = openMedia,
                        onOpenDetail = onOpenDetail,
                    )

                    PhoneFileProtocol.WEBDAV -> PhoneWebDavBrowserScreen(
                        connectionId = connectionId,
                        path = path,
                        webDavListViewModel = webDavListViewModel,
                        webDavConViewModel = webDavConViewModel,
                        movieViewModel = movieViewModel,
                        mediaMetaViewModel = mediaMetaViewModel,
                        autoScrape = settingsState.autoScrapeEnabled(PhoneFileProtocol.WEBDAV),
                        onBack = onBack,
                        onOpenDirectory = onOpenDirectory,
                        onPlayVideo = onPlayVideo,
                        onOpenMedia = openMedia,
                        onOpenDetail = onOpenDetail,
                    )

                    PhoneFileProtocol.HTTP -> PhoneHttpBrowserScreen(
                        connectionId = connectionId,
                        path = path,
                        httpLinkListViewModel = httpLinkListViewModel,
                        httpLinkConViewModel = httpLinkConViewModel,
                        movieViewModel = movieViewModel,
                        mediaMetaViewModel = mediaMetaViewModel,
                        autoScrape = settingsState.autoScrapeEnabled(PhoneFileProtocol.HTTP),
                        onBack = onBack,
                        onOpenDirectory = onOpenDirectory,
                        onPlayVideo = onPlayVideo,
                        onOpenMedia = openMedia,
                        onOpenDetail = onOpenDetail,
                    )

                    // SMB 有自己的一套页面（本地网络权限引导），协议标签里不会走到这里；
                    // 真被手改路由绕进来时直接退回，避免白屏
                    PhoneFileProtocol.SMB, null -> LaunchedEffect(Unit) {
                        navController.popBackStack()
                    }
                }
            }

            // 沉浸式影片详情：刮削结果全在这里看，播放 / 重新匹配也从这里出发
            composable(
                route = PhoneRoutes.DETAIL,
                arguments = listOf(
                    navArgument("videoUri") { type = NavType.StringType },
                    navArgument("dataSourceType") { type = NavType.StringType },
                    navArgument("fileName") { type = NavType.StringType },
                    navArgument("connectionName") { type = NavType.StringType },
                ),
            ) { entry ->
                val dataSourceType = entry.arguments?.getString("dataSourceType").orEmpty()

                PhoneDetailScreen(
                    videoUri = entry.arguments?.getString("videoUri").orEmpty().fromBase64(),
                    dataSourceType = dataSourceType,
                    fileName = entry.arguments?.getString("fileName").orEmpty().fromBase64(),
                    connectionName = PhoneRoutes.decodeArg(entry.arguments?.getString("connectionName")),
                    movieViewModel = movieViewModel,
                    mediaMetaViewModel = mediaMetaViewModel,
                    onBack = { navController.popBackStack() },
                    onPlay = { sourceUri, type, title ->
                        navController.navigate(PhoneRoutes.player(sourceUri, type, title))
                    },
                    onRematch = { sourceUri, name, connection ->
                        navController.navigate(
                            PhoneRoutes.match(
                                videoUri = sourceUri,
                                dataSourceType = dataSourceType,
                                fileName = name,
                                connectionName = connection,
                            )
                        )
                    },
                )
            }

            composable(
                route = PhoneRoutes.PLAYER,
                arguments = listOf(
                    navArgument("sourceUri") { type = NavType.StringType },
                    navArgument("dataSourceType") { type = NavType.StringType },
                    navArgument("title") { type = NavType.StringType },
                ),
            ) { entry ->
                PhonePlayerScreen(
                    mediaUri = entry.arguments?.getString("sourceUri").orEmpty().fromBase64(),
                    dataSourceType = entry.arguments?.getString("dataSourceType").orEmpty(),
                    title = entry.arguments?.getString("title").orEmpty().fromBase64(),
                    onBack = { navController.popBackStack() },
                )
            }

            // 音频播放：列表已经写进 `AudioPlaylistRepository`，这里只还原下标与来源
            composable(
                route = PhoneRoutes.AUDIO,
                arguments = listOf(
                    navArgument("currentIndex") { type = NavType.StringType },
                    navArgument("dataSourceType") { type = NavType.StringType },
                    navArgument("connectionName") { type = NavType.StringType },
                ),
            ) { entry ->
                PhoneAudioPlayerScreen(
                    startIndex = entry.arguments?.getString("currentIndex").orEmpty().toIntOrNull() ?: 0,
                    dataSourceType = entry.arguments?.getString("dataSourceType").orEmpty(),
                    connectionName = PhoneRoutes.decodeArg(entry.arguments?.getString("connectionName")),
                    audioViewModel = audioViewModel,
                    mediaHistoryViewModel = mediaHistoryViewModel,
                    onBack = { navController.popBackStack() },
                )
            }

            composable(
                route = PhoneRoutes.IMAGE,
                arguments = listOf(
                    navArgument("dataSourceType") { type = NavType.StringType },
                    navArgument("uris") { type = NavType.StringType },
                    navArgument("index") { type = NavType.StringType },
                ),
            ) { entry ->
                PhoneImageViewerScreen(
                    uris = PhoneMediaLogic
                        .splitArgs(entry.arguments?.getString("uris"))
                        .map { it.fromBase64() },
                    initialIndex = entry.arguments?.getString("index").orEmpty().toIntOrNull() ?: 0,
                    dataSourceType = entry.arguments?.getString("dataSourceType").orEmpty(),
                    onBack = { navController.popBackStack() },
                )
            }
        }
    }
}

/** SMB 共享根目录 */
internal const val SMB_ROOT_PATH = "/"

/** 该来源在设置里是否参与刮削（电视端「刮削与媒体库」那六个开关，手机端与它共用同一份存储） */
private fun SettingsUiState.sourceEnabled(protocol: PhoneFileProtocol): Boolean = when (protocol) {
    PhoneFileProtocol.LOCAL -> local
    PhoneFileProtocol.SMB -> smb
    PhoneFileProtocol.FTP -> ftp
    PhoneFileProtocol.NFS -> nfs
    PhoneFileProtocol.WEBDAV -> webdav
    PhoneFileProtocol.HTTP -> http
}

/**
 * 进目录时是否自动刮削 = 手机端总开关（默认关）**且** 该来源开关打开。
 *
 * 两个都满足才会联网，避免「我只想看文件名，进目录却开始下载海报」。
 */
private fun SettingsUiState.autoScrapeEnabled(protocol: PhoneFileProtocol): Boolean =
    phoneAutoScrape && sourceEnabled(protocol)

/** 与 `SettingsViewModel.toggleSource` 的入参对齐（那几个字符串是电视端的历史口径，不能改） */
private val PhoneFileProtocol.scrapeKey: String
    get() = when (this) {
        PhoneFileProtocol.LOCAL -> "Local"
        PhoneFileProtocol.SMB -> "SMB"
        PhoneFileProtocol.FTP -> "FTP"
        PhoneFileProtocol.NFS -> "NFS"
        PhoneFileProtocol.WEBDAV -> "WebDav"
        PhoneFileProtocol.HTTP -> "HTTP"
    }

/**
 * 切标签页：只保留一个标签页实例，来回切不会把页面栈越堆越深。
 * `saveState / restoreState` 让每个标签页各自记住滚动位置。
 */
private fun NavHostController.switchTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
