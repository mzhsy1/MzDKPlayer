package org.mz.mzdkplayer.ui.screen.localfile

import android.os.Environment
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.ListItem
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import org.mz.mzdkplayer.R
import org.mz.mzdkplayer.tool.Tools.toBase64
import org.mz.mzdkplayer.ui.screen.common.showToast
import org.mz.mzdkplayer.ui.screen.vm.SettingsViewModel
import org.mz.mzdkplayer.ui.theme.myListItemCoverColor
import java.io.File
import java.util.Locale

private val FONT_EXTENSIONS = setOf("ttf", "otf", "ttc", "otc")

private fun File.isFontFile(): Boolean =
    isFile && extension.lowercase(Locale.getDefault()) in FONT_EXTENSIONS

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun FontPickerScreen(
    path: String?,
    navController: NavHostController,
    settingsVM: SettingsViewModel
) {
    val context = LocalContext.current

    if (path == null) {
        // 根目录：列出可用存储
        val roots = remember { buildStorageRoots() }
        val primary = remember { Environment.getExternalStorageDirectory().absolutePath }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
        ) {
            Text(
                text = stringResource(R.string.font_picker_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(roots) { root ->
                    val label = if (root.absolutePath == primary) {
                        stringResource(R.string.storage_internal)
                    } else {
                        root.absolutePath
                    }
                    ListItem(
                        selected = false,
                        onClick = {
                            navController.navigate("FontPickerScreen/${root.absolutePath.toBase64()}")
                        },
                        headlineContent = {
                            Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        },
                        leadingContent = {
                            Icon(
                                painter = painterResource(R.drawable.localfile),
                                contentDescription = null
                            )
                        },
                        colors = myListItemCoverColor(),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
        return
    }

    val dir = File(path)
    val entries = remember(path) {
        dir.listFiles()
            ?.filter { (it.isDirectory && it.canRead()) || it.isFontFile() }
            ?.sortedWith(
                compareByDescending<File> { it.isDirectory }
                    .thenBy { it.name.lowercase(Locale.getDefault()) }
            )
            ?: emptyList()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Text(
            text = stringResource(R.string.font_picker_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Text(
            text = path,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.6f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            item {
                ListItem(
                    selected = false,
                    onClick = { navController.popBackStack() },
                    headlineContent = { Text(stringResource(R.string.font_picker_up)) },
                    leadingContent = {
                        Icon(
                            painter = painterResource(R.drawable.baseline_arrow_back_24),
                            contentDescription = null
                        )
                    },
                    colors = myListItemCoverColor(),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (entries.isEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.font_picker_no_font_found),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }

            items(entries) { file ->
                if (file.isDirectory) {
                    ListItem(
                        selected = false,
                        onClick = {
                            navController.navigate("FontPickerScreen/${file.absolutePath.toBase64()}")
                        },
                        headlineContent = {
                            Text(file.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        },
                        leadingContent = {
                            Icon(
                                painter = painterResource(R.drawable.baseline_folder_24),
                                contentDescription = null
                            )
                        },
                        colors = myListItemCoverColor(),
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    ListItem(
                        selected = false,
                        onClick = {
                            settingsVM.setSubFontPath(file.absolutePath)
                            showToast(
                                context,
                                context.getString(R.string.font_selected_toast, file.name)
                            )
                            navController.popBackStack()
                        },
                        headlineContent = {
                            Text(file.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        },
                        leadingContent = {
                            Icon(
                                painter = painterResource(R.drawable.baseline_insert_drive_file_24),
                                contentDescription = null
                            )
                        },
                        colors = myListItemCoverColor(),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

private fun buildStorageRoots(): List<File> {
    val roots = mutableListOf<File>()
    val primary = Environment.getExternalStorageDirectory()
    if (primary.exists()) {
        roots.add(primary)
    }
    // 其他可移动存储（USB / 外置存储卡）
    val storage = File("/storage")
    storage.listFiles()?.forEach { f ->
        if (f.isDirectory && f.canRead() &&
            f.name != "emulated" && f.name != "self" &&
            f.absolutePath != primary.absolutePath
        ) {
            roots.add(f)
        }
    }
    return roots.distinctBy { it.absolutePath }
}
