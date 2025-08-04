package org.mz.mzdkplayer.ui.theme
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.Text
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import androidx.core.net.toUri

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun FilePermissionScreen() {
    val context = LocalContext.current

    // 对于 Android 10 及以下版本，请求读写权限
    val readPermissionState = rememberPermissionState(
        android.Manifest.permission.READ_EXTERNAL_STORAGE
    )
    val writePermissionState = rememberPermissionState(
        android.Manifest.permission.WRITE_EXTERNAL_STORAGE
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11+ 需要 MANAGE_EXTERNAL_STORAGE 权限
            if (Environment.isExternalStorageManager()) {
                Text("已获得所有文件访问权限", color = Color.White)
                // 这里可以放置你的应用内容
            } else {
                Text("需要所有文件访问权限")
                Button(onClick = {
                    try {
                        // 首先尝试标准的 Intent
                        val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                        intent.data = "package:${context.packageName}".toUri()
                        context.startActivity(intent)
                    } catch (e: ActivityNotFoundException) {
                        try {
                            // 如果失败，尝试备用 Intent
                            val intent = Intent()
                            intent.action = Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION
                            intent.data = "package:${context.packageName}".toUri()
                            context.startActivity(intent)
                        } catch (e: ActivityNotFoundException) {
                            // 如果还是失败，打开应用详情设置页面
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                            intent.data = "package:${context.packageName}".toUri()
                            context.startActivity(intent)

                            // 可以显示一个 Snackbar 提示用户手动开启权限
                            Toast.makeText(
                                context,
                                "请在权限设置中启用'所有文件访问'权限",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }) {
                    Text("请求所有文件访问权限")
                }
            }
        } else {
            // Android 10 及以下版本处理
            if (readPermissionState.status.isGranted && writePermissionState.status.isGranted) {
                Text("已获得存储权限", color = Color.White)
                // 这里可以放置你的应用内容
            } else {
                val textToShow = if (readPermissionState.status.shouldShowRationale ||
                    writePermissionState.status.shouldShowRationale
                ) {
                    "应用需要存储权限来访问文件。请授予权限。"
                } else {
                    "需要存储权限来访问文件"
                }

                Text(textToShow)
                Button(onClick = {
                    readPermissionState.launchPermissionRequest()
                    writePermissionState.launchPermissionRequest()
                }) {
                    Text("请求存储权限")
                }
            }
        }
    }

    // 自动请求权限（可选）
    LaunchedEffect(Unit) {

            if (!readPermissionState.status.isGranted || !writePermissionState.status.isGranted) {
                readPermissionState.launchPermissionRequest()
                writePermissionState.launchPermissionRequest()
            }

    }
}