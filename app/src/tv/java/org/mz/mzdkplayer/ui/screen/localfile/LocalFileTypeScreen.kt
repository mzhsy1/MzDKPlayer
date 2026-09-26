package org.mz.mzdkplayer.ui.screen.localfile

import android.annotation.SuppressLint
import android.os.Environment
import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.tv.material3.Card
import androidx.tv.material3.Text
import org.mz.mzdkplayer.R
import org.mz.mzdkplayer.ui.screen.common.FCLMainTitle
import org.mz.mzdkplayer.ui.theme.myCardBorderStyle
import org.mz.mzdkplayer.ui.theme.myCardColor
import org.mz.mzdkplayer.ui.theme.myFileTypeCardScaleStyle
import org.mz.mzdkplayer.tool.Tools.toBase64
import java.net.URLEncoder


@SuppressLint("SdCardPath")
@Composable
fun LocalFileTypeScreen(mainNavController: NavHostController) {
    val context = LocalContext.current
    val filesPaths = remember {
        mutableStateListOf<String>(
            Environment.getExternalStorageDirectory().absolutePath,
            "${
                if (Environment.getExternalStorageDirectory().parentFile != null) {
                    Environment.getExternalStorageDirectory().parentFile?.parent
                } else {
                    "/storage"
                }
            }",
            "/mnt",
            "/"
        )
    }
    // 这些 stringResource 会在语言切换时自动触发重组
    val labelInternal = stringResource(R.string.ui_label_internal_storage)
    val labelUsb = stringResource(R.string.ui_label_usb_and_external_storage)
    val labelMounted = stringResource(R.string.ui_label_device_mounted_folders)
    val labelRoot = stringResource(R.string.ui_label_root_directory_requires_root)

// 2. 将这些变量作为 remember 的 keys
// 只要其中任何一个字符串发生变动，remember 就会重新计算内部列表
    val filesName = remember(labelInternal, labelUsb, labelMounted, labelRoot) {
        mutableStateListOf(
            labelInternal,
            labelUsb,
            labelMounted,
            labelRoot
        )
    }
    LazyColumn(modifier = Modifier.padding().fillMaxSize()) {
        item {
            // 标题
            FCLMainTitle(mainNavController = mainNavController, stringResource(R.string.ui_label_local_files), "",true)
        }
        itemsIndexed(filesPaths) { index, conn ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                onClick = {
                    mainNavController.navigate(
                        "LocalFileListScreen/${filesPaths[index].toBase64()}"
                    )
                },
                colors = myCardColor(),
                border = myCardBorderStyle(),
                scale = myFileTypeCardScaleStyle(),

                ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        filesName[index], fontSize = 22.sp
                    )
                    Text(stringResource(R.string.ui_label_directory_path,filesPaths[index]))
                }
            }
        }

    }
    Log.d("filesPaths", filesPaths.toString())
}