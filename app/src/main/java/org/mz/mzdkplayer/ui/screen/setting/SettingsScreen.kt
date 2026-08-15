package org.mz.mzdkplayer.ui.screen.setting

import android.R.attr.versionCode
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape

import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.FilterChip
import androidx.tv.material3.FilterChipDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.ListItem
import androidx.tv.material3.ListItemDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.SurfaceDefaults
import androidx.tv.material3.Switch
import androidx.tv.material3.Text
import org.mz.mzdkplayer.R
import org.mz.mzdkplayer.di.RepositoryProvider
import org.mz.mzdkplayer.data.repository.SettingsRepository
import org.mz.mzdkplayer.tool.viewModelWithFactory
import org.mz.mzdkplayer.ui.screen.common.DeleteConfirmDialog
import org.mz.mzdkplayer.ui.screen.common.FilePermissionScreen
import org.mz.mzdkplayer.ui.screen.common.MyIconButton
import org.mz.mzdkplayer.ui.screen.vm.AudioViewModel
import org.mz.mzdkplayer.ui.screen.vm.MovieViewModel
import org.mz.mzdkplayer.ui.screen.vm.SettingsUiState
import org.mz.mzdkplayer.ui.screen.vm.SettingsViewModel
import org.mz.mzdkplayer.ui.theme.myListItemCoverColor
import org.mz.mzdkplayer.ui.theme.mySideFilterChipColor
import org.mz.mzdkplayer.ui.videoplayer.components.NumberControl
import androidx.core.net.toUri

// 定义左侧菜单分类
enum class SettingCategory(@param:StringRes val titleRes: Int, val iconRes: Int? = null) {
    General(R.string.cat_general),
    Playback(R.string.cat_playback),
    Audio(R.string.cat_audio),
    Subtitle(R.string.cat_subtitle),
    Source(R.string.cat_source),
    Tools(R.string.cat_tools),
    About(R.string.cat_about)
}

@Composable
fun SettingsScreen(
    mainNavController: NavHostController,
    settingsVM: SettingsViewModel = viewModel(),
    audioViewModel: AudioViewModel
) {
    // 获取 ViewModel

    val movieVM: MovieViewModel = viewModelWithFactory { RepositoryProvider.createMovieViewModel() }
    val state by settingsVM.uiState.collectAsState()
    val context = LocalContext.current
// 👇 1. 为lie创建 FocusRequester
    val listFocusRequester = remember { FocusRequester() }
    // 当前选中的分类，默认为第一个
    var selectedCategory by remember { mutableStateOf(SettingCategory.General) }

    // 焦点计数器 (用于隐藏的计数)
    var focusClickCount by remember { mutableIntStateOf(0) }
    // 跟踪上次点击时间，用于防止长按或焦点保持
    var lastClickTime by remember { mutableLongStateOf(0L) }

    // 重置焦点的协程
    LaunchedEffect(focusClickCount) {
        if (focusClickCount > 0) {
            val currentTime = System.currentTimeMillis()
            // 如果两次点击间隔超过 1000ms，则重置计数器
            if (currentTime - lastClickTime > 1000L) {
                focusClickCount = 0
            }
            lastClickTime = currentTime
        }
    }
    // 👇 关键：页面加载后，主动把焦点丢给按钮
    LaunchedEffect(Unit) {
        listFocusRequester.requestFocus()
    }
    // 使用 Row 实现左右两栏布局
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 24.dp, bottom = 24.dp)
    ) {
        // --- 左侧：导航栏 (占宽 30%) ---
        LazyColumn(
            modifier = Modifier
                .weight(0.3f)
                .fillMaxHeight()
                .selectableGroup() // 优化无障碍和焦点
                .padding(end = 12.dp).focusRequester(listFocusRequester),
            contentPadding = PaddingValues(horizontal = 16.dp)
        )
        {
            item {
                Text(
                    text = stringResource(R.string.settings_title),
                    style = MaterialTheme.typography.displaySmall,
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 24.dp, start = 12.dp)
                )
            }
            items(SettingCategory.entries.toTypedArray()) { category ->
                CategoryItem(
                    category = category,
                    isSelected = category == selectedCategory,
                    onClick = { selectedCategory = category }
                )
            }
        }

        // --- 中间分割线 (可选) ---
        Box(
            modifier = Modifier
                .width(1.dp)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.border.copy(alpha = 0.5f))
        )

        // --- 右侧：内容区域 (占宽 70%) ---
        // 使用 Box 容纳内容，根据 selectedCategory 切换显示不同的 Composable
        Box(
            modifier = Modifier
                .weight(0.7f)
                .fillMaxHeight()
                .padding(horizontal = 32.dp)
        ) {
            // 这里使用 LazyColumn 保证右侧内容过多时也可以在内部滚动
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 50.dp)
            ) {
                item {
                    // 右侧顶部的标题提示
                    Text(
                        text = stringResource(selectedCategory.titleRes),
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White,
                        modifier = Modifier.padding(bottom = 24.dp)
                    )
                }

                // 根据分类加载具体内容
                when (selectedCategory) {
                    SettingCategory.General -> item {
                        GeneralSection(state, settingsVM, context = context)
                    }

                    SettingCategory.Playback -> item {
                        PlaybackSection(state, settingsVM)
                    }

                    SettingCategory.Audio -> item {
                        AudioSection(state, settingsVM)
                    }

                    SettingCategory.Subtitle -> item {
                        SubtitleSection(state, settingsVM)
                    }

                    SettingCategory.Source -> item {
                        SourceSection(state, settingsVM)
                    }

                    SettingCategory.Tools -> item {
                        ToolsSection(movieVM, audioViewModel)
                    }

                    SettingCategory.About -> item {
                        AboutSection(context, mainNavController)
                    }
                }
            }
        }
    }
}

// --- 左侧菜单项组件 ---
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun CategoryItem(
    category: SettingCategory,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    ListItem(
        selected = isSelected,
        onClick = onClick,
        headlineContent = { Text(stringResource(category.titleRes)) },
        colors = myListItemCoverColor(),
        trailingContent = {
            if (isSelected) {
                Icon(painterResource(R.drawable.chevronright24dp), contentDescription = null)
            }
        },
        modifier = Modifier.padding(vertical = 4.dp)
    )
}

// --- 以下为拆分后的右侧具体设置内容块 ---

@Composable
fun GeneralSection(state: SettingsUiState, settingsVM: SettingsViewModel,context: Context) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SwitchSettingItem(
            title = stringResource(R.string.setting_hide_details),
            subtitle = stringResource(R.string.setting_hide_details_sub),
            checked = state.hideDetails,
            onCheckedChange = { settingsVM.toggleHideDetails(it) }
        )

        SwitchSettingItem(
            title = stringResource(R.string.setting_hide_net_speed),
            checked = state.hideNetworkSpeed,
            onCheckedChange = { settingsVM.toggleHideNetWorkSpeed(it) }
        )
        ActionSettingItem(
            title = stringResource(R.string.setting_app_lang),
            value = formatAppLang(state.appLang),
            onClick = {
                val next = when(state.appLang){
                    "" -> "zh"
                    "zh" -> "en"
                    "en" -> "ja"
                    else -> ""
                }
                settingsVM.setAppLanguage(context,next)
            }
        )
    }
}

@Composable
fun PlaybackSection(state: SettingsUiState, settingsVM: SettingsViewModel) {

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ActionSettingItem(
            title = stringResource(R.string.setting_audio_lang),
            value = formatLang(state.audioLang),
            onClick = {
                val next = when (state.audioLang) {
                    "" -> "zh"; "zh" -> "en"; else -> ""
                }
                settingsVM.setAudioLanguage(next)
            }
        )
        // ==================== 新增：播放器内核选择 ====================
        ActionSettingItem(
            title = stringResource(R.string.setting_default_player),
            value = if (state.defaultPlayer == "vlc") stringResource(R.string.setting_default_player_vlc) else stringResource(R.string.setting_default_player_exo),
            onClick = {
                val next = if (state.defaultPlayer == "exo") "vlc" else "exo"
                settingsVM.setDefaultPlayer(next)
            }
        )
        ActionSettingItem(
            title = stringResource(R.string.setting_iso_playback_mode),
            value = formatIsoPlaybackMode(state.isoPlaybackMode),
            onClick = {
                val next = (state.isoPlaybackMode + 1) % 2
                settingsVM.setIsoPlaybackMode(next)
            }
        )
        ActionSettingItem(
            title = stringResource(R.string.setting_sub_lang),
            value = formatLang(state.subLang),
            onClick = {
                val next = when (state.subLang) {
                    "" -> "zh"; "zh" -> "en"; else -> ""
                }
                settingsVM.setSubLanguage(next)
            }
        )
        SwitchSettingItem(
            title = stringResource(R.string.setting_tunneling),
            subtitle = stringResource(R.string.setting_tunneling_sub),
            checked = state.enableTunneling,
            onCheckedChange = { settingsVM.toggleTunneling(it) }
        )
        SwitchSettingItem(
            title = stringResource(R.string.setting_lock_video_ratio),
            checked = state.lockVideoRatio,
            onCheckedChange = { settingsVM.toggleLockVideoRatio(it) }
        )
        ActionSettingItem(
            title = stringResource(R.string.setting_dpad_up_action),
            value = formatDpadAction(state.dpadUpAction),
            onClick = {
                settingsVM.setDpadUpAction(nextDpadAction(state.dpadUpAction))
            }
        )
        ActionSettingItem(
            title = stringResource(R.string.setting_dpad_down_action),
            value = formatDpadAction(state.dpadDownAction),
            onClick = {
                settingsVM.setDpadDownAction(nextDpadAction(state.dpadDownAction))
            }
        )
    }
}

@Composable
fun AudioSection(state: SettingsUiState, settingsVM: SettingsViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SwitchSettingItem(
            title =stringResource(R.string.setting_passthrough),
            subtitle = stringResource(R.string.setting_passthrough_sub),
            checked = state.enablePassthrough,
            onCheckedChange = { settingsVM.togglePassthrough(it) }
        )
        // 新增音频解码模式切换选项
        ActionSettingItem(
            title = stringResource(R.string.setting_exo_audio_decode_mode),
            value = formatAudioDecodeMode(state.exoAudioDecodeMode),
            onClick = {
                // 循环切换逻辑: 1(硬优先) -> 2(软优先) -> 0(纯硬解) -> 1(硬优先)
                val next = when (state.exoAudioDecodeMode) {
                    1 -> 2
                    2 -> 0
                    else -> 1
                }
                settingsVM.setExoAudioDecodeMode(next)
            }
        )

    }
}

@Composable
fun SubtitleSection(state: SettingsUiState, settingsVM: SettingsViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // 字体大小 - 数字调节
        NumberControl(
            value = state.subFontSize.toInt(),
            onValueChange = { settingsVM.setSubFontSize(it.toFloat()) },
            maxValue = 100,
            minValue = 16,
            label = stringResource(R.string.setting_font_size)
        )
        // 字体颜色
        ActionSettingItem(
            title = stringResource(R.string.setting_font_color),
            value = if (state.subColor == 0xFFFFFFFF) stringResource(R.string.color_white) else stringResource(R.string.color_yellow),
            onClick = {
                val next = if (state.subColor == 0xFFFFFFFF) 0xFFFFFF00 else 0xFFFFFFFF
                settingsVM.setSubColor(next)
            }
        )
        // 背景颜色
        ActionSettingItem(
            title = stringResource(R.string.setting_bg_color),
            value = parseBgColorName(state.subBgColor),
            onClick = {
                val next = when (state.subBgColor) {
                    0x80000000 -> 0x80FFFFFF
                    0x80FFFFFF -> 0x80FFFF00
                    0x80FFFF00 -> 0x00000000
                    else -> 0x80000000
                }
                settingsVM.setSubBgColor(next)
            }
        )
        // 距离底部 - 数字调节
        NumberControl(
            value = state.subBottomPadding.toInt(),
            onValueChange = { settingsVM.setSubBottomPadding(it.toFloat()) },
            maxValue = 200,
            minValue = -100,
            label = stringResource(R.string.setting_bottom_padding)
        )
        SwitchSettingItem(
            title = stringResource(R.string.setting_force_pgs_center),
            subtitle = stringResource(R.string.setting_force_pgs_center_sub),
            checked = state.forcePgsCenter,
            onCheckedChange = { settingsVM.togglePgsCenter(it) }
        )
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun SourceSection(state: SettingsUiState, settingsVM: SettingsViewModel) {
    var showTmdbConfig by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            val modifier = Modifier.weight(1f)
            DataSourceSwitch("SMB", state.smb, modifier) { settingsVM.toggleSource("SMB", it) }
            DataSourceSwitch("WebDav", state.webdav, modifier) {
                settingsVM.toggleSource(
                    "WebDav",
                    it
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            val modifier = Modifier.weight(1f)
            DataSourceSwitch("FTP", state.ftp, modifier) { settingsVM.toggleSource("FTP", it) }
            DataSourceSwitch("NFS", state.nfs, modifier) { settingsVM.toggleSource("NFS", it) }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            val modifier = Modifier.weight(1f)
            DataSourceSwitch("Local", state.local, modifier) {
                settingsVM.toggleSource(
                    "Local",
                    it
                )
            }
            DataSourceSwitch("HTTP", state.http, modifier) { settingsVM.toggleSource("HTTP", it) }
        }
        Spacer(modifier = Modifier.height(16.dp))
        ActionSettingItem(
            title = stringResource(R.string.setting_tmdb_api_mirror),
            value = if (state.tmdbBaseUrl == SettingsRepository.DEFAULT_TMDB_URL) "Official" else state.tmdbBaseUrl,
            onClick = { showTmdbConfig = true }
        )
        Spacer(modifier = Modifier.height(8.dp))
        ActionSettingItem(
            title = stringResource(R.string.setting_tmdb_search_lang),
            value = formatTmdbLang(state.tmdbSearchLang),
            onClick = {
                val next = when (state.tmdbSearchLang) {
                    "" -> "zh-CN"
                    "zh-CN" -> "zh-TW"
                    "zh-TW" -> "en-US"
                    "en-US" -> "ja-JP"
                    "ja-JP" -> "ko-KR"
                    else -> ""
                }
                settingsVM.setTmdbSearchLang(next)
            }
        )
        Spacer(modifier = Modifier.height(8.dp))
        ActionSettingItem(
            title = stringResource(R.string.setting_tmdb_result_lang),
            value = formatTmdbLang(state.tmdbResultLang),
            onClick = {
                val next = when (state.tmdbResultLang) {
                    "" -> "zh-CN"
                    "zh-CN" -> "zh-TW"
                    "zh-TW" -> "en-US"
                    "en-US" -> "ja-JP"
                    "ja-JP" -> "ko-KR"
                    else -> ""
                }
                settingsVM.setTmdbResultLang(next)
            }
        )
        Spacer(modifier = Modifier.height(8.dp))
        SwitchSettingItem(
            title = stringResource(R.string.setting_prioritize_nfo),
            subtitle = stringResource(R.string.setting_prioritize_nfo_sub),
            checked = state.prioritizeLocalNfo,
            onCheckedChange = { settingsVM.togglePrioritizeLocalNfo(it) }
        )
        ActionSettingItem(
            title = stringResource(R.string.setting_recursive_scan_level),
            value = formatRecursiveScanLevel(state.recursiveScanLevel),
            onClick = {
                val next = (state.recursiveScanLevel + 1) % 6
                settingsVM.setRecursiveScanLevel(next)
            }
        )
    }

    if (showTmdbConfig) {
        TMDBConfigDialog(
            settingsVM = settingsVM,
            onDismiss = { showTmdbConfig = false }
        )
    }
}

@Composable
fun ToolsSection(movieVM: MovieViewModel, audioViewModel: AudioViewModel) {
    // 1. 定义两个状态，用来控制影视库和音乐库清理弹窗的显示
    var showClearMovieDialog by remember { mutableStateOf(false) }
    var showClearAudioDialog by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // --- 权限管理板块 ---
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = stringResource(R.string.cat_tools),
                style = MaterialTheme.typography.titleLarge,
                color = Color.White
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = Color.White.copy(alpha = 0.05f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(8.dp)
            ) {
                FilePermissionScreen()
            }
        }

        // --- 数据库管理板块 ---
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = stringResource(R.string.tool_section_database),
                style = MaterialTheme.typography.titleLarge,
                color = Color.White
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = Color.White.copy(alpha = 0.05f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    MyIconButton(
                        text = stringResource(R.string.btn_clear_movie_db),
                        icon = R.drawable.delete24dp,
                        modifier = Modifier.weight(1f),
                        onClick = { showClearMovieDialog = true }
                    )
                    MyIconButton(
                        text = stringResource(R.string.btn_clear_audio_db),
                        icon = R.drawable.delete24dp,
                        modifier = Modifier.weight(1f),
                        onClick = { showClearAudioDialog = true }
                    )
                }
            }
        }

        // --- 性能测试工具板块 ---
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = stringResource(R.string.tool_section_performance),
                style = MaterialTheme.typography.titleLarge,
                color = Color.White
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = Color.White.copy(alpha = 0.05f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(8.dp)
            ) {
                PerformanceTestScreen()
            }
        }
    }

    // 4. 在界面底部挂载弹窗组件（当状态为 true 时显示）
    if (showClearMovieDialog) {
        DeleteConfirmDialog(
            title = stringResource(R.string.btn_clear_movie_db), // 可以直接复用按钮的文案当标题
            message = stringResource(R.string.msg_clear_movie_db_confirm),
            onConfirm = {
                // 用户点击确认后，真正执行清理逻辑
                movieVM.clearMediaLibrary()
            },
            onDismiss = {
                // 关闭弹窗
                showClearMovieDialog = false
            }
        )
    }

    if (showClearAudioDialog) {
        DeleteConfirmDialog(
            title = stringResource(R.string.btn_clear_audio_db),
            message = stringResource(R.string.msg_clear_audio_db_confirm),
            onConfirm = {
                audioViewModel.clearLibrary()
            },
            onDismiss = {
                showClearAudioDialog = false
            }
        )
    }
}


// --- 基础组件 (保持大致不变，略微调整以适应新布局) ---

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun SwitchSettingItem(
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    ListItem(
        selected = false,
        onClick = { onCheckedChange(!checked) },
        headlineContent = { Text(title) },
        colors = myListItemCoverColor(),
        supportingContent = if (subtitle != null) {
            { Text(subtitle) }
        } else null,
        trailingContent = {
            Switch(checked = checked, onCheckedChange = null)
        },
        modifier = Modifier.fillMaxWidth()
    )
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun ActionSettingItem(
    title: String,
    value: String,
    onClick: () -> Unit
) {
    ListItem(
        selected = false,
        onClick = onClick,
        headlineContent = { Text(title) },
        colors = myListItemCoverColor(),
        trailingContent = {
            Text(text = value, style = MaterialTheme.typography.bodyMedium)
        },
        modifier = Modifier.fillMaxWidth()
    )
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun DataSourceSwitch(
    name: String,
    checked: Boolean,
    modifier: Modifier = Modifier,
    onCheckedChange: (Boolean) -> Unit
) {
    FilterChip(
        selected = checked,
        onClick = { onCheckedChange(!checked) },
        colors = mySideFilterChipColor(),
        scale = FilterChipDefaults.scale(1f, 1.05f),
        modifier = modifier,
        leadingIcon = {
            if (checked) Icon(painterResource(R.drawable.check24dp), contentDescription = null)
        }
    ) {
        Text(name)
    }
}
val PackageInfo.versionCodeCompat: Long
    get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        longVersionCode // API 28+ 使用这个
    } else {
        @Suppress("DEPRECATION")
        android.R.attr.versionCode.toLong() // 旧版本使用这个，并压制警告
    }
@Composable
fun AboutSection(context: Context, navController: NavHostController) {
    val pkgInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        context.packageManager.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0))
    } else {
        context.packageManager.getPackageInfo(context.packageName, 0)
    }
    // 用于记录点击次数
    var clickCount by remember { mutableIntStateOf(0) }
    // 记录最后一次点击时间，超过 2 秒没点就重置计数
    var lastClickTime by remember { mutableLongStateOf(0L) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // --- 头部：Logo 和 版本信息 ---
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = 8.dp)
        ) {
            Surface(
                shape = ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(16.dp)),
                colors = ClickableSurfaceDefaults.colors(
                    containerColor = Color.White.copy(alpha = 0.05f),
                    focusedContainerColor = Color.White.copy(alpha = 0.05f),
                    pressedContainerColor = Color.White.copy(alpha = 0.05f)
                ),
                onClick = { /* 仅展示 */ }
            ) {
                Icon(
                    painter = painterResource(R.mipmap.ic_launcher),
                    contentDescription = "Logo",
                    modifier = Modifier
                        .size(100.dp)
                        .padding(12.dp),
                    tint = Color.Unspecified
                )
            }
            Spacer(Modifier.width(24.dp))
            Column {
                Text(
                    text = context.getString(R.string.app_name),
                    style = MaterialTheme.typography.displaySmall,
                    color = Color.White
                )
                Spacer(Modifier.height(8.dp))
                Surface(
                    shape = ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(8.dp)),
                    colors = ClickableSurfaceDefaults.colors(
                        containerColor = Color.Transparent,
                        focusedContainerColor = Color.White.copy(alpha = 0.1f),
                        pressedContainerColor = Color.White.copy(alpha = 0.2f)
                    ),
                    border = ClickableSurfaceDefaults.border(
                        focusedBorder = Border(BorderStroke(1.dp, Color.White.copy(alpha = 0.5f)))
                    ),
                    onClick = {
                        val currentTime = System.currentTimeMillis()
                        if (currentTime - lastClickTime < 1000) {
                            clickCount++
                        } else {
                            clickCount = 1
                        }
                        lastClickTime = currentTime

                        if (clickCount >= 5) {
                            clickCount = 0
                            if (System.currentTimeMillis() % 2 == 0L) {
                                navController.navigate("SolarSystemScreen")
                            } else {
                                navController.navigate("BlackHoleSimulationScreen")
                            }
                        }
                    }
                ) {
                    Text(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        text = stringResource(R.string.ui_label_version_prefix) + "${pkgInfo?.versionName} (${pkgInfo?.versionCodeCompat})",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                }
            }
        }

        // --- 内容：项目链接和版权 ---
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AboutItem(
                label = stringResource(R.string.ui_label_author),
                value = "@MZHSY",
            )

            AboutItem(
                label = stringResource(R.string.ui_label_official_website),
                value = "https://mzdkplayer.pages.dev/",
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, "https://mzdkplayer.pages.dev/".toUri())
                    context.startActivity(intent)
                }
            )

            AboutItem(
                label = stringResource(R.string.ui_label_github),
                value = "https://github.com/mzhsy1/MzDKPlayer",
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW,
                        "https://github.com/mzhsy1/MzDKPlayer".toUri())
                    context.startActivity(intent)
                }
            )

            AboutItem(
                label = stringResource(R.string.ui_label_gitee),
                value = "https://gitee.com/mzhsy/MzDKPlayer",
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW,
                        "https://gitee.com/mzhsy/MzDKPlayer".toUri())
                    context.startActivity(intent)
                }
            )
        }

        // --- 底部：版权和免责声明 ---
        Column(
            modifier = Modifier.padding(top = 16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = stringResource(R.string.ui_label_copyright),
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.4f)
            )
            Text(
                text = stringResource(R.string.ui_label_disclaimer),
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.3f)
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun AboutItem(
    label: String,
    value: String,
    icon: Int? = null,
    onClick: (() -> Unit)? = null
) {
    ListItem(
        selected = false,
        onClick = { onClick?.invoke() },
        enabled = onClick != null,
        headlineContent = {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.6f)
            )
        },
        supportingContent = {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White
            )
        },
        leadingContent = icon?.let {
            { Icon(painterResource(it), contentDescription = null, modifier = Modifier.size(24.dp)) }
        },
        shape = ListItemDefaults.shape(shape = RoundedCornerShape(12.dp)),
        colors = ListItemDefaults.colors(
            containerColor = Color.White.copy(alpha = 0.05f),
            focusedContainerColor = Color.White.copy(alpha = 0.15f),
            pressedContainerColor = Color.White.copy(alpha = 0.2f),
            contentColor = Color.White,
            focusedContentColor = Color.White
        ),
        modifier = Modifier.fillMaxWidth()
    )
}

// --- Helper Functions ---
@Composable
fun formatLang(code: String): String = when (code) {
    "zh" -> stringResource(R.string.ui_label_chinese_language)
    "en" -> stringResource(R.string.lang_english)
    else -> stringResource(R.string.lang_auto)
}
@Composable
fun parseBgColorName(color: Long): String = when (color) {
    0x80000000 -> stringResource(R.string.color_black_50)
    0x80FFFFFF -> stringResource(R.string.color_white_50)
    0x80FFFF00 -> stringResource(R.string.color_yellow_50)
    0x00000000L -> stringResource(R.string.color_transparent)
    else -> stringResource(R.string.ui_label_custom)
}
@Composable
fun formatAppLang(code: String): String = when(code){
    "" -> stringResource(R.string.lang_auto_system)
    "zh" -> stringResource(R.string.ui_label_chinese_language)
    "en" -> stringResource(R.string.lang_english)
    "ja" -> stringResource(R.string.ui_label_japanese_language)
    else -> stringResource(R.string.lang_auto)
}
@Composable
fun formatAudioDecodeMode(mode: Int): String = when (mode) {
    0 -> stringResource(R.string.setting_audio_decode_pure_hw)
    1 -> stringResource(R.string.setting_audio_decode_hw_priority)
    2 -> stringResource(R.string.setting_audio_decode_sw_priority)
    else -> stringResource(R.string.ui_label_unknown)
}
@Composable
fun formatRecursiveScanLevel(level: Int): String = when (level) {
    0 -> stringResource(R.string.recursive_scan_level_0)
    1 -> stringResource(R.string.recursive_scan_level_1)
    2 -> stringResource(R.string.recursive_scan_level_2)
    3 -> stringResource(R.string.recursive_scan_level_3)
    4 -> stringResource(R.string.recursive_scan_level_4)
    5 -> stringResource(R.string.recursive_scan_level_5)
    else -> "Level $level"
}

@Composable
fun formatTmdbLang(code: String): String = when (code) {
    "" -> stringResource(R.string.lang_auto_system)
    "zh-CN" -> "简体中文"
    "zh-TW" -> "繁體中文"
    "en-US" -> "English"
    "ja-JP" -> "日本語"
    "ko-KR" -> "한국어"
    else -> code
}

@Composable
fun formatIsoPlaybackMode(mode: Int): String = when (mode) {
    0 -> stringResource(R.string.iso_playback_mode_default)
    1 -> stringResource(R.string.iso_playback_mode_main_movie)
    else -> stringResource(R.string.ui_label_unknown)
}

// 遥控器上下键可选功能列表
private val DPAD_ACTION_ORDER = listOf("A", "S", "D", "V", "SPEED", "R")

private fun nextDpadAction(current: String): String {
    val idx = DPAD_ACTION_ORDER.indexOf(current)
    val next = if (idx < 0) 0 else (idx + 1) % DPAD_ACTION_ORDER.size
    return DPAD_ACTION_ORDER[next]
}

@Composable
fun formatDpadAction(code: String): String = when (code) {
    "A" -> stringResource(R.string.ui_label_audio_track)
    "S" -> stringResource(R.string.ui_label_subtitle_select)
    "D" -> stringResource(R.string.ui_label_danmaku_settings)
    "V" -> stringResource(R.string.ui_label_video_track)
    "SPEED" -> stringResource(R.string.ui_label_speed)
    "R" -> stringResource(R.string.ui_label_aspect_ratio)
    else -> stringResource(R.string.ui_label_unknown)
}
