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
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import org.mz.mzdkplayer.R
import org.mz.mzdkplayer.data.model.PhoneThemeMode
import org.mz.mzdkplayer.data.repository.SettingsRepository
import org.mz.mzdkplayer.tool.logic.PhoneThemeLogic

/**
 * 手机端根组件：主题 + 系统栏 + 底部导航 + 导航图。
 *
 * 这里只留「外壳」：
 * - ViewModel 装配在 [PhoneViewModels]；
 * - 路由接线（含参数解码）在 [PhoneNavGraph]；
 * - 「哪个来源参与刮削」的口径在 `PhoneScrapeSources.kt`。
 *
 * 业务逻辑全部复用现有代码（各协议 VM / `MovieViewModel` / `MediaLibraryViewModel` /
 * `MzExoPlayer` / `SettingsRepository`），本文件不碰业务。
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

/** 外壳：底部标签栏 + 导航图（标签页之外的页面不显示底部栏） */
@Composable
private fun PhoneRoot(
    themeMode: PhoneThemeMode,
    onThemeModeChange: (PhoneThemeMode) -> Unit,
    dynamicColor: Boolean,
    onDynamicColorChange: (Boolean) -> Unit,
) {
    val navController = rememberNavController()
    val viewModels = rememberPhoneViewModels()
    val settingsState by viewModels.settings.uiState.collectAsState()

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
        PhoneNavGraph(
            navController = navController,
            viewModels = viewModels,
            settingsState = settingsState,
            themeMode = themeMode,
            onThemeModeChange = onThemeModeChange,
            dynamicColor = dynamicColor,
            onDynamicColorChange = onDynamicColorChange,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        )
    }
}

/** SMB 共享根目录 */
internal const val SMB_ROOT_PATH = "/"

/**
 * 切标签页：只保留一个标签页实例，来回切不会把页面栈越堆越深。
 * `saveState / restoreState` 让每个标签页各自记住滚动位置。
 */
internal fun NavHostController.switchTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
