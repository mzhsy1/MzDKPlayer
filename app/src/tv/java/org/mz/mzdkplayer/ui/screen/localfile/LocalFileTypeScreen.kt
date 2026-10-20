package org.mz.mzdkplayer.ui.screen.localfile

import android.annotation.SuppressLint
import android.os.Environment
import androidx.annotation.DrawableRes
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import androidx.tv.material3.Text
import org.mz.mzdkplayer.R
import org.mz.mzdkplayer.tool.Tools.toBase64
import org.mz.mzdkplayer.ui.screen.common.ConListAccent
import org.mz.mzdkplayer.ui.screen.common.FCLMainTitle

// 聚焦时卡片翻成暖白底，文字与图标必须跟着变深色
private val OnLightTitle = Color(0xFF16151A)
private val OnLightPath = Color(0xFF6E655B)
private val OnDarkTitle = Color(0xFFF4F1EA)
private val OnDarkPath = Color(0xFF918D9C)

private data class StorageRoot(
    val name: String,
    val path: String,
    @DrawableRes val iconRes: Int,
    val accent: Color
)

/**
 * 本地文件入口：列出可浏览的存储根目录。
 *
 * 原来每行只有「名称 + 目录路径: xxx」两行裸文字；现在每个根目录一张卡，
 * 按存储类型配图标与强调色，焦点所在卡片翻成暖白底并加描边。
 */
@SuppressLint("SdCardPath")
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun LocalFileTypeScreen(mainNavController: NavHostController) {
    val labelInternal = stringResource(R.string.ui_label_internal_storage)
    val labelUsb = stringResource(R.string.ui_label_usb_and_external_storage)
    val labelMounted = stringResource(R.string.ui_label_device_mounted_folders)
    val labelRoot = stringResource(R.string.ui_label_root_directory_requires_root)

    val roots = remember(labelInternal, labelUsb, labelMounted, labelRoot) {
        val externalDir = Environment.getExternalStorageDirectory()
        val storageParent = externalDir.parentFile?.parentFile?.absolutePath ?: "/storage"
        listOf(
            StorageRoot(
                name = labelInternal,
                path = externalDir.absolutePath,
                iconRes = R.drawable.svglocal,
                accent = ConListAccent.local
            ),
            StorageRoot(
                name = labelUsb,
                path = storageParent,
                iconRes = R.drawable.storage24dp,
                accent = ConListAccent.ftp
            ),
            StorageRoot(
                name = labelMounted,
                path = "/mnt",
                iconRes = R.drawable.foldermanaged24dp,
                accent = ConListAccent.smb
            ),
            StorageRoot(
                name = labelRoot,
                path = "/",
                iconRes = R.drawable.baseline_folder_24,
                accent = ConListAccent.webDav
            )
        )
    }

    LazyColumn(
        // 与 app 根容器同色，避免在悬浮侧边栏右侧出现一条背景色差
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            FCLMainTitle(
                mainNavController = mainNavController,
                titleText = stringResource(R.string.ui_label_local_files),
                isLocalFile = true
            )
        }
        itemsIndexed(roots) { _, root ->
            StorageRootCard(
                root = root,
                onClick = {
                    mainNavController.navigate(
                        "LocalFileListScreen/${root.path.toBase64()}"
                    )
                },
                modifier = Modifier.padding(horizontal = 24.dp)
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun StorageRootCard(
    root: StorageRoot,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val accent = root.accent
    val shape = RoundedCornerShape(16.dp)

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
        scale = CardDefaults.scale(focusedScale = 1.02f, pressedScale = 0.99f),
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
                elevation = 16.dp,
                elevationColor = accent.copy(alpha = 0.28f)
            ),
            pressedGlow = Glow.None
        ),
        modifier = modifier
            .fillMaxWidth()
            .height(88.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(15.dp))
                    .background(accent.copy(alpha = if (isFocused) 0.24f else 0.16f))
                    .border(
                        width = 1.dp,
                        color = accent.copy(alpha = 0.42f),
                        shape = RoundedCornerShape(15.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = root.iconRes),
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(26.dp)
                )
            }
            Spacer(modifier = Modifier.width(18.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = root.name,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isFocused) OnLightTitle else OnDarkTitle,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = root.path,
                    fontSize = 13.sp,
                    color = if (isFocused) OnLightPath else OnDarkPath,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Icon(
                painter = painterResource(id = R.drawable.arrowright24dp),
                contentDescription = null,
                tint = if (isFocused) OnLightPath else Color(0xFF6E6A78),
                modifier = Modifier.size(22.dp)
            )
        }
    }
}
