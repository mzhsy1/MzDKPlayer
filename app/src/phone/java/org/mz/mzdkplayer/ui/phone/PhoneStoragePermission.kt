package org.mz.mzdkplayer.ui.phone

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.core.content.ContextCompat

/**
 * 手机端「浏览本机文件」需要的存储权限（第三阶段引入）。
 *
 * 与电视端 `FilePermissionScreen` 的口径一致，但把判定收成一个对象：判定散在屏里的话
 * 「授权完到底算不算已授权」会随 Android 版本分叉。
 *
 * - Android 11（API 30）起要 `MANAGE_EXTERNAL_STORAGE`（`Environment.isExternalStorageManager`），
 *   这是唯一能列出任意目录（含 `Android/data` 之外的普通目录）的方式；
 * - Android 13（API 33）起如果只拿到 `READ_MEDIA_VIDEO/AUDIO/IMAGES`，也能看到媒体文件
 *   （`java.io.File.listFiles` 会过滤掉没权限的条目），所以视为「够用」；
 * - 再早的系统用 `READ_EXTERNAL_STORAGE`。
 *
 * 权限名已在 `app/src/main/AndroidManifest.xml` 声明（两个 flavor 共用）。
 */
internal object PhoneStoragePermission {

    /** 「所有文件访问」跳到应用自身的授权页 */
    fun allFilesAccessIntent(context: Context): Intent =
        Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
            .setData(Uri.parse("package:${context.packageName}"))

    /**
     * 旧系统（Android 11 之前）用的运行时权限；
     * Android 13 起改成按媒体类型申请，这里只申请视频（手机端播放页目前只播视频）。
     */
    val runtimePermission: String =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_VIDEO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

    /**
     * 是否需要「所有文件访问」这个特殊授权页面：
     * Android 11 起才有这个概念，低版本走运行时权限。
     */
    val needsAllFilesAccess: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R

    /** 当前是否已经能读本机文件 */
    fun isGranted(context: Context): Boolean = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> 
            Environment.isExternalStorageManager() || hasMediaPermission(context)

        else -> hasMediaPermission(context)
    }

    private fun hasMediaPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, runtimePermission) ==
                PackageManager.PERMISSION_GRANTED
}
