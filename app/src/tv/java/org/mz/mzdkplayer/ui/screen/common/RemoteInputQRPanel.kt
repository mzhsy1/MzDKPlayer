package org.mz.mzdkplayer.ui.screen.common


import org.mz.mzdkplayer.tool.MzToastManager
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape

import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import org.mz.mzdkplayer.R
import org.mz.mzdkplayer.tool.RemoteConfig
import org.mz.mzdkplayer.tool.RemoteInputServer
import org.mz.mzdkplayer.tool.Tools

/**
 * 通用组件：显示二维码并启动本地 HTTP 服务器接收手机输入
 * @param modifier 布局修饰符
 * @param onReceiveConfig 当收到手机端发来的配置时的回调
 */
@Composable
fun RemoteInputQRPanel(
    modifier: Modifier = Modifier,
    onReceiveConfig: (RemoteConfig) -> Unit
) {
    val context = LocalContext.current
    var qrCodeBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    var serverUrl by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    // 管理 Server 生命周期
    DisposableEffect(Unit) {
        val localIp = Tools.getLocalIpAddress()
        var serverInstance: RemoteInputServer? = null

        if (localIp != null) {
            // 尝试启动服务器 (使用之前定义的工具方法)
            val result = Tools.startServerOnAvailablePort(9382) { config ->
                // 回调给父组件
                onReceiveConfig(config)
                // 可选：弹个吐司提示用户
                MzToastManager.show(context.getString(R.string.ui_label_mobile_config_received))
            }

            if (result != null) {
                serverInstance = result.first
                val port = result.second
                val url = "http://$localIp:$port"
                serverUrl = url
                // 生成二维码
                qrCodeBitmap = Tools.generateQRCode(url)
            } else {
                errorMsg = context.getString(R.string.ui_label_port_occupied_cannot_start_service)
            }
        } else {
            errorMsg = context.getString(R.string.ui_label_no_wifi_or_cannot_get_ip)
        }

        onDispose {
            serverInstance?.stop()
        }
    }

    // UI 布局
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (qrCodeBitmap != null) {
            Text(
                text = stringResource(R.string.ui_label_quick_input_via_mobile_qr_scan),
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // 二维码容器 (白色背景，圆角)
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White)
                    .padding(12.dp)
            ) {
                Image(
                    bitmap = qrCodeBitmap!!,
                    contentDescription = "Scan QR to input",
                    modifier = Modifier.size(240.dp)
                )
            }

            Text(
                text = stringResource(R.string.ui_label_or_access_via_browser,serverUrl),
                color = Color.LightGray,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 16.dp)
            )

            Text(
                text = stringResource(R.string.ui_label_mobile_and_tv_same_lan),
                color = Color.Gray,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(top = 4.dp)
            )

        } else {
            // 错误或加载状态
            Text(
                text = errorMsg ?: stringResource(R.string.ui_label_initializing_service),
                color = if (errorMsg != null) Color.Red else Color.Gray,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}