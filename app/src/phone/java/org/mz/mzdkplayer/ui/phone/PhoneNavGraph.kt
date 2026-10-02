package org.mz.mzdkplayer.ui.phone

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import org.mz.mzdkplayer.data.model.PhoneThemeMode
import org.mz.mzdkplayer.data.repository.AudioPlaylistRepository
import org.mz.mzdkplayer.tool.Tools.fromBase64
import org.mz.mzdkplayer.tool.logic.PhoneMediaLogic
import org.mz.mzdkplayer.ui.phone.model.PhoneFileProtocol
import org.mz.mzdkplayer.ui.phone.model.PhoneSettingCategory
import org.mz.mzdkplayer.ui.phone.screen.PhoneAboutSettingsPage
import org.mz.mzdkplayer.ui.phone.screen.PhoneAudioPlayerScreen
import org.mz.mzdkplayer.ui.phone.screen.PhoneAudioSettingsPage
import org.mz.mzdkplayer.ui.phone.screen.PhoneDetailScreen
import org.mz.mzdkplayer.ui.phone.screen.PhoneFilesScreen
import org.mz.mzdkplayer.ui.phone.screen.PhoneFtpBrowserScreen
import org.mz.mzdkplayer.ui.phone.screen.PhoneHomeScreen
import org.mz.mzdkplayer.ui.phone.screen.PhoneHttpBrowserScreen
import org.mz.mzdkplayer.ui.phone.screen.PhoneImageViewerScreen
import org.mz.mzdkplayer.ui.phone.screen.PhoneInterfaceSettingsPage
import org.mz.mzdkplayer.ui.phone.screen.PhoneLibrarySettingsPage
import org.mz.mzdkplayer.ui.phone.screen.PhoneLocalBrowserScreen
import org.mz.mzdkplayer.ui.phone.screen.PhoneMatchScreen
import org.mz.mzdkplayer.ui.phone.screen.PhoneNfsBrowserScreen
import org.mz.mzdkplayer.ui.phone.screen.PhonePlaybackSettingsPage
import org.mz.mzdkplayer.ui.phone.screen.PhonePlayerScreen
import org.mz.mzdkplayer.ui.phone.screen.PhoneSettingsScreen
import org.mz.mzdkplayer.ui.phone.screen.PhoneSmbBrowserScreen
import org.mz.mzdkplayer.ui.phone.screen.PhoneSubtitleSettingsPage
import org.mz.mzdkplayer.ui.phone.screen.PhoneToolsSettingsPage
import org.mz.mzdkplayer.ui.phone.screen.PhoneWebDavBrowserScreen
import org.mz.mzdkplayer.viewmodel.SettingsUiState

/**
 * 手机端的导航图（原 `PhoneApp.kt` 里 300 多行的 `NavHost`，只挪位置）。
 *
 * 只做接线：路由参数在这里解码（Base64 / `PhoneRoutes.decodeArg`），ViewModel 与回调
 * 来自 [viewModels] 与调用方。页面的业务实现一概在各页面自己的文件里。
 */
@Composable
internal fun PhoneNavGraph(
    navController: NavHostController,
    viewModels: PhoneViewModels,
    settingsState: SettingsUiState,
    themeMode: PhoneThemeMode,
    onThemeModeChange: (PhoneThemeMode) -> Unit,
    dynamicColor: Boolean,
    onDynamicColorChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
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

    NavHost(
        navController = navController,
        startDestination = PhoneRoutes.HOME,
        modifier = modifier,
    ) {
        composable(PhoneRoutes.HOME) {
            PhoneHomeScreen(
                libraryViewModel = viewModels.mediaLibrary,
                onOpenFiles = { navController.switchTab(PhoneRoutes.FILES) },
                onOpenSettings = { navController.switchTab(PhoneRoutes.SETTINGS) },
                showRecentlyWatched = settingsState.phoneHomeRecentlyWatched,
                showRecentlyAdded = settingsState.phoneHomeRecentlyAdded,
                showRecentlyVisited = settingsState.phoneHomeRecentlyVisited,
                onPlay = { sourceUri, dataSourceType, fileName, connectionName ->
                    navController.navigate(
                        PhoneRoutes.player(sourceUri, dataSourceType, fileName, connectionName)
                    )
                },
            )
        }

        composable(PhoneRoutes.FILES) {
            PhoneFilesScreen(
                smbListViewModel = viewModels.smbList,
                ftpListViewModel = viewModels.ftpList,
                nfsListViewModel = viewModels.nfsList,
                webDavListViewModel = viewModels.webDavList,
                httpLinkListViewModel = viewModels.httpList,
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
                onOpenCategory = { category ->
                    navController.navigate(PhoneRoutes.settingsSection(category.routeValue))
                },
            )
        }

        // 设置的二级分类页（第八阶段）：底部标签栏在这里收起，返回箭头回到设置首页。
        // 每个分类页自己从 SettingsViewModel 收状态，所以这里只接线、不传 state。
        composable(
            route = PhoneRoutes.SETTINGS_SECTION,
            arguments = listOf(navArgument("category") { type = NavType.StringType }),
        ) { entry ->
            val category = PhoneSettingCategory.fromRouteValue(
                entry.arguments?.getString("category")
            )
            val onBack: () -> Unit = { navController.popBackStack() }

            when (category) {
                PhoneSettingCategory.PLAYBACK -> PhonePlaybackSettingsPage(viewModels.settings, onBack)
                PhoneSettingCategory.AUDIO -> PhoneAudioSettingsPage(viewModels.settings, onBack)
                PhoneSettingCategory.SUBTITLE -> PhoneSubtitleSettingsPage(viewModels.settings, onBack)
                PhoneSettingCategory.INTERFACE -> PhoneInterfaceSettingsPage(viewModels.settings, onBack)
                PhoneSettingCategory.LIBRARY -> PhoneLibrarySettingsPage(viewModels.settings, onBack)

                PhoneSettingCategory.TOOLS -> PhoneToolsSettingsPage(
                    movieViewModel = viewModels.movie,
                    audioViewModel = viewModels.audio,
                    mediaHistoryViewModel = viewModels.mediaHistory,
                    onBack = onBack,
                )

                PhoneSettingCategory.ABOUT -> PhoneAboutSettingsPage(onBack)

                // 手改路由或旧版本留下的未知分类：直接退回，避免白屏
                null -> LaunchedEffect(Unit) { navController.popBackStack() }
            }
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
                movieViewModel = viewModels.movie,
                mediaMetaViewModel = viewModels.mediaMeta,
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
                smbListViewModel = viewModels.smbList,
                smbConViewModel = viewModels.smbCon,
                movieViewModel = viewModels.movie,
                mediaMetaViewModel = viewModels.mediaMeta,
                autoScrape = settingsState.autoScrapeEnabled(PhoneFileProtocol.SMB),
                onBack = { navController.popBackStack() },
                onOpenDirectory = { targetPath ->
                    navController.navigate(PhoneRoutes.smbBrowser(connectionId, targetPath))
                },
                onPlayVideo = { sourceUri, name, connName ->
                    navController.navigate(
                        PhoneRoutes.player(
                            sourceUri = sourceUri,
                            dataSourceType = PhoneFileProtocol.SMB.routeValue,
                            fileName = name,
                            connectionName = connName,
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
            val onPlayVideo: (String, String, String) -> Unit = { sourceUri, name, connName ->
                navController.navigate(
                    PhoneRoutes.player(sourceUri, protocolName, name, connName)
                )
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
                    movieViewModel = viewModels.movie,
                    mediaMetaViewModel = viewModels.mediaMeta,
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
                    ftpListViewModel = viewModels.ftpList,
                    ftpConViewModel = viewModels.ftpCon,
                    movieViewModel = viewModels.movie,
                    mediaMetaViewModel = viewModels.mediaMeta,
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
                    nfsListViewModel = viewModels.nfsList,
                    nfsConViewModel = viewModels.nfsCon,
                    movieViewModel = viewModels.movie,
                    mediaMetaViewModel = viewModels.mediaMeta,
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
                    webDavListViewModel = viewModels.webDavList,
                    webDavConViewModel = viewModels.webDavCon,
                    movieViewModel = viewModels.movie,
                    mediaMetaViewModel = viewModels.mediaMeta,
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
                    httpLinkListViewModel = viewModels.httpList,
                    httpLinkConViewModel = viewModels.httpCon,
                    movieViewModel = viewModels.movie,
                    mediaMetaViewModel = viewModels.mediaMeta,
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
                movieViewModel = viewModels.movie,
                mediaMetaViewModel = viewModels.mediaMeta,
                onBack = { navController.popBackStack() },
                onPlay = { sourceUri, type, fileName, connName ->
                    navController.navigate(PhoneRoutes.player(sourceUri, type, fileName, connName))
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
                navArgument("connectionName") { type = NavType.StringType },
            ),
        ) { entry ->
            PhonePlayerScreen(
                mediaUri = entry.arguments?.getString("sourceUri").orEmpty().fromBase64(),
                dataSourceType = entry.arguments?.getString("dataSourceType").orEmpty(),
                fileName = entry.arguments?.getString("title").orEmpty().fromBase64(),
                connectionName = PhoneRoutes.decodeArg(entry.arguments?.getString("connectionName")),
                mediaMetaViewModel = viewModels.mediaMeta,
                mediaHistoryViewModel = viewModels.mediaHistory,
                onBack = { navController.popBackStack() },
                // 播放下一个：换地址重进本页，并把旧的那个从返回栈里去掉 ——
                // 否则连看几集之后返回键要按很多次（电视端是同一处理）
                onPlayOther = { item ->
                    navController.navigate(
                        PhoneRoutes.player(
                            sourceUri = item.uri,
                            dataSourceType = item.dataSourceType,
                            fileName = item.fileName,
                            connectionName = item.connectionName,
                        )
                    ) {
                        popUpTo(PhoneRoutes.PLAYER) { inclusive = true }
                    }
                },
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
                audioViewModel = viewModels.audio,
                mediaHistoryViewModel = viewModels.mediaHistory,
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
