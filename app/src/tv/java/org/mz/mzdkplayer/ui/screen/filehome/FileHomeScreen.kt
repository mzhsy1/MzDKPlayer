package org.mz.mzdkplayer.ui.screen.filehome

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.tv.material3.Border
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Glow
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import org.mz.mzdkplayer.R
import org.mz.mzdkplayer.ui.screen.common.ConListAccent

// 与主页卡片、侧边栏选中态同一套配色：聚焦时整块翻成暖白底，文字与图标跟着变深色
private val OnLightTitle = Color(0xFF16151A)
private val OnLightDesc = Color(0xFF6E655B)
private val OnDarkTitle = Color(0xFFF4F1EA)
private val OnDarkDesc = Color(0xFF918D9C)

/** 文件浏览入口的存储项：本地存储单独一张宽卡，其余协议进三列网格 */
private data class StorageEntry(
    val name: String,
    @StringRes val descriptionRes: Int,
    @DrawableRes val iconRes: Int,
    val accent: Color,
    val route: String
)

/**
 * 电视端「文件浏览」入口页。
 *
 * 原来是一列两行的裸 ListItem，六个来源长相完全一样，只能靠文字分辨。
 * 现在按「本地存储（宽卡）+ 网络存储（三列协议卡）」分区，每种协议沿用连接列表页的强调色，
 * 图标、标题、说明各占一层，焦点所在的那张卡翻成暖白底并加协议色描边。
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun FileHomeScreen(mainNavController: NavHostController) {
    val localFocusRequester = remember { FocusRequester() }
    val windowInfo = LocalWindowInfo.current

    // 导航进来时窗口可能还没拿到焦点，直接 requestFocus 会静默失败（焦点留在侧边栏），
    // 所以等窗口焦点事件再请求一次。
    LaunchedEffect(Unit) {
        snapshotFlow { windowInfo.isWindowFocused }.first { it }
        delay(80)
        runCatching { localFocusRequester.requestFocus() }
    }

    val protocols = remember {
        listOf(
            StorageEntry(
                name = "SMB",
                descriptionRes = R.string.ui_label_file_browse_smb_desc,
                iconRes = R.drawable.smb,
                accent = ConListAccent.smb,
                route = "SMBListScreen"
            ),
            StorageEntry(
                name = "WebDAV",
                descriptionRes = R.string.ui_label_file_browse_webdav_desc,
                iconRes = R.drawable.svgwebdavsvg,
                accent = ConListAccent.webDav,
                route = "WebDavListScreen"
            ),
            StorageEntry(
                name = "FTP",
                descriptionRes = R.string.ui_label_file_browse_ftp_desc,
                iconRes = R.drawable.ftp,
                accent = ConListAccent.ftp,
                route = "FTPConListScreen"
            ),
            StorageEntry(
                name = "NFS",
                descriptionRes = R.string.ui_label_file_browse_nfs_desc,
                iconRes = R.drawable.svgnfs,
                accent = ConListAccent.nfs,
                route = "NFSConListScreen"
            ),
            StorageEntry(
                name = "HTTP",
                descriptionRes = R.string.ui_label_file_browse_http_desc,
                iconRes = R.drawable.svghttp,
                accent = ConListAccent.http,
                route = "HTTPLinkConListScreen"
            )
        )
    }

    // 背景跟 app 根容器保持一致（纯黑）。内容区在悬浮侧边栏右侧，给这一页单独画渐变
    // 会在 x=72dp 处露出一条竖着的色差，看起来像侧边栏和页面不是同一个背景
    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        Column(modifier = Modifier.fillMaxSize()) {
            FileBrowseHeader()

            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                // 一屏装下「本地 + 5 个协议」，1080p 上不用滚动就能看全
                contentPadding = PaddingValues(start = 32.dp, end = 32.dp, bottom = 16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    LocalStorageCard(
                        onClick = { mainNavController.navigate("LocalFileTypeScreen") },
                        modifier = Modifier.focusRequester(localFocusRequester)
                    )
                }

                item(span = { GridItemSpan(maxLineSpan) }) {
                    NetworkSectionHeader()
                }

                items(protocols) { entry ->
                    ProtocolCard(
                        entry = entry,
                        onClick = { mainNavController.navigate(entry.route) }
                    )
                }
            }
        }
    }
}

/** 页头：图标 + 标题 + 一句说明，右侧提示遥控器操作 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun FileBrowseHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 32.dp, end = 32.dp, top = 22.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(13.dp))
                .background(ConListAccent.local.copy(alpha = 0.14f))
                .border(
                    width = 1.dp,
                    color = ConListAccent.local.copy(alpha = 0.34f),
                    shape = RoundedCornerShape(13.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = R.drawable.localfile),
                contentDescription = null,
                tint = ConListAccent.local,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.ui_label_file_browsing),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = stringResource(R.string.ui_label_file_browse_subtitle),
                fontSize = 13.sp,
                color = OnDarkDesc,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = stringResource(R.string.ui_label_file_browse_hint),
            style = MaterialTheme.typography.labelMedium,
            color = Color(0xFF7C7885)
        )
    }
}

/** 本地存储：横跨整行的主入口 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun LocalStorageCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val accent = ConListAccent.local

    FileStorageCard(
        onClick = onClick,
        interactionSource = interactionSource,
        accent = accent,
        modifier = modifier.height(88.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 22.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StorageIconTile(
                iconRes = R.drawable.localfile,
                accent = accent,
                isFocused = isFocused,
                size = 52.dp,
                corner = 16.dp,
                iconSize = 26.dp
            )
            Spacer(modifier = Modifier.width(18.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.ui_label_local_files),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isFocused) OnLightTitle else OnDarkTitle,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.ui_label_file_browse_local_desc),
                    fontSize = 13.sp,
                    color = if (isFocused) OnLightDesc else OnDarkDesc,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(
                        color = accent.copy(alpha = if (isFocused) 0.22f else 0.14f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.arrowright24dp),
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

/** 分区标题：左侧一根协议色竖条，右侧按键提示 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun NetworkSectionHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(width = 4.dp, height = 16.dp)
                .background(color = Color(0xFF8C8A96), shape = RoundedCornerShape(2.dp))
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = stringResource(R.string.ui_label_network_storage),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = Color.White
        )
    }
}

/** 单个协议入口卡 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun ProtocolCard(
    entry: StorageEntry,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    FileStorageCard(
        onClick = onClick,
        interactionSource = interactionSource,
        accent = entry.accent,
        modifier = Modifier.height(112.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp, vertical = 12.dp)
        ) {
            StorageIconTile(
                iconRes = entry.iconRes,
                accent = entry.accent,
                isFocused = isFocused,
                size = 38.dp,
                corner = 12.dp,
                iconSize = 20.dp
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = entry.name,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = if (isFocused) OnLightTitle else OnDarkTitle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = stringResource(entry.descriptionRes),
                fontSize = 12.sp,
                color = if (isFocused) OnLightDesc else OnDarkDesc,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/** 卡片外壳：统一圆角、暖白聚焦底色、协议色聚焦描边与光晕 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun FileStorageCard(
    onClick: () -> Unit,
    interactionSource: MutableInteractionSource,
    accent: Color,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val shape = RoundedCornerShape(18.dp)
    Card(
        onClick = onClick,
        interactionSource = interactionSource,
        shape = CardDefaults.shape(shape = shape),
        colors = CardDefaults.colors(
            containerColor = Color(0xFF1A1A21),
            contentColor = OnDarkTitle,
            focusedContainerColor = Color(0xFFFFF6E5),
            focusedContentColor = OnLightTitle
        ),
        scale = CardDefaults.scale(focusedScale = 1.03f, pressedScale = 0.99f),
        border = CardDefaults.border(
            border = Border(
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.06f)),
                shape = shape
            ),
            focusedBorder = Border(
                border = BorderStroke(2.dp, accent),
                shape = shape
            )
        ),
        glow = CardDefaults.glow(
            glow = Glow.None,
            focusedGlow = Glow(
                elevation = 18.dp,
                elevationColor = accent.copy(alpha = 0.28f)
            ),
            pressedGlow = Glow.None
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        content()
    }
}

/** 协议图标底：一格强调色圆角方块 */
@Composable
private fun StorageIconTile(
    @DrawableRes iconRes: Int,
    accent: Color,
    isFocused: Boolean,
    size: androidx.compose.ui.unit.Dp,
    corner: androidx.compose.ui.unit.Dp,
    iconSize: androidx.compose.ui.unit.Dp
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(corner))
            .background(accent.copy(alpha = if (isFocused) 0.24f else 0.16f))
            .border(
                width = 1.dp,
                color = accent.copy(alpha = 0.42f),
                shape = RoundedCornerShape(corner)
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = null,
            tint = accent,
            modifier = Modifier.size(iconSize)
        )
    }
}
