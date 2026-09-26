package org.mz.mzdkplayer.ui.screen.common

import org.mz.mzdkplayer.tool.MzToastManager
import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.tv.material3.*
import com.google.accompanist.permissions.*
import org.mz.mzdkplayer.R
import org.mz.mzdkplayer.ui.theme.myListItemCoverColor

@OptIn(ExperimentalPermissionsApi::class, ExperimentalTvMaterial3Api::class)
@Composable
fun FilePermissionScreen() {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        Text(
            text = stringResource(R.string.perm_section_storage),
            style = MaterialTheme.typography.titleMedium,
            color = Color.White.copy(alpha = 0.8f),
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // 1. All Files Access (Recommended for Android 11+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            PermissionItem(
                title = stringResource(R.string.perm_title_all_files),
                subtitle = stringResource(R.string.perm_desc_all_files),
                isGranted = Environment.isExternalStorageManager(),
                onClick = { requestManageStoragePermission(context) }
            )
        }

        // 2. Granular Media Permissions (Android 13+) or Legacy Storage
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            PermissionRequestItem(
                permission = Manifest.permission.READ_MEDIA_VIDEO,
                title = stringResource(R.string.perm_title_media_video)
            )
            PermissionRequestItem(
                permission = Manifest.permission.READ_MEDIA_AUDIO,
                title = stringResource(R.string.perm_title_media_audio)
            )
            PermissionRequestItem(
                permission = Manifest.permission.READ_MEDIA_IMAGES,
                title = stringResource(R.string.perm_title_media_images)
            )
        } else {
            PermissionRequestItem(
                permission = Manifest.permission.READ_EXTERNAL_STORAGE,
                title = stringResource(R.string.perm_title_media_video)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.perm_section_other),
            style = MaterialTheme.typography.titleMedium,
            color = Color.White.copy(alpha = 0.8f),
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // 3. Notification Permission (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            PermissionRequestItem(
                permission = Manifest.permission.POST_NOTIFICATIONS,
                title = stringResource(R.string.perm_title_notifications)
            )
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PermissionRequestItem(permission: String, title: String) {
    val permissionState = rememberPermissionState(permission)
    val context = LocalContext.current

    PermissionItem(
        title = title,
        isGranted = permissionState.status.isGranted,
        onClick = {
            if (!permissionState.status.isGranted) {
                permissionState.launchPermissionRequest()
            } else {
                openAppSettings(context)
            }
        }
    )
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun PermissionItem(
    title: String,
    subtitle: String? = null,
    isGranted: Boolean,
    onClick: () -> Unit
) {
    ListItem(
        selected = false,
        onClick = onClick,
        headlineContent = { Text(title) },
        supportingContent = if (subtitle != null) { { Text(subtitle) } } else null,
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (isGranted) stringResource(R.string.perm_status_granted) else stringResource(R.string.perm_status_denied),
                    color = if (isGranted) Color.Green else Color.Red,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    painter = painterResource(if (isGranted) R.drawable.check24dp else R.drawable.close24dp),
                    contentDescription = null,
                    tint = if (isGranted) Color.Green else Color.Red
                )
            }
        },
        colors = myListItemCoverColor(),
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
    )
}

private fun openAppSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.fromParts("package", context.packageName, null)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(intent)
}

@RequiresApi(Build.VERSION_CODES.R)
private fun requestManageStoragePermission(context: Context) {
    val intents = listOf(
        Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
            .setData("package:${context.packageName}".toUri()),
        Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
            .setData("package:${context.packageName}".toUri()),
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            .setData("package:${context.packageName}".toUri())
    )

    for (intent in intents) {
        try {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            return
        } catch (e: Exception) {
            continue
        }
    }
    MzToastManager.show(context.getString(R.string.ui_label_manually_enable_permission_in_settings))
}
