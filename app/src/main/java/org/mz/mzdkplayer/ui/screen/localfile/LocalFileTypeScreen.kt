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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.tv.material3.Card
import androidx.tv.material3.Text
import org.mz.mzdkplayer.ui.theme.myCardBorderStyle
import org.mz.mzdkplayer.ui.theme.myCardColor
import org.mz.mzdkplayer.ui.theme.myFileTypeCardScaleStyle
import java.net.URLEncoder


@SuppressLint("SdCardPath")
@Composable

fun LocalFileTypeScreen(mainNavController: NavController) {
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
    val filesName = remember {
        mutableStateListOf<String>(
            "内部存储",
            "U盘及其他外接存储",
            "设备挂载文件夹",
            "根目录 需要root权限"
        )
    }

    LazyColumn(modifier = Modifier.padding(16.dp).fillMaxSize()) {
        item {
            Text(text = "请选择文件存储路径",fontSize = 24.sp, color = Color.White)
        }
        itemsIndexed(filesPaths) { index, conn ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                onClick = {
                    mainNavController.navigate(
                        "LocalFileScreen/${URLEncoder.encode(filesPaths[index], "UTF-8")}"
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
                    Text("目录路径: ${filesPaths[index]}")
                }
            }
        }

    }
    Log.d("filesPaths", filesPaths.toString())
}