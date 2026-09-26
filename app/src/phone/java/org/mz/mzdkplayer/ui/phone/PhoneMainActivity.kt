package org.mz.mzdkplayer.ui.phone

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge

/**
 * 手机端入口 Activity。
 *
 * 与电视端 `MainActivity` 是两个独立入口：
 * - 电视端保留 `LEANBACK_LAUNCHER`（只有带遥控器的大屏 Launcher 才会列出它）；
 * - 手机端用普通的 `LAUNCHER`，所以手机桌面上只会出现这一个图标。
 *
 * 这里**刻意不做** `MainActivity` 那套「按 960dp 目标宽度重算 densityDpi」的电视适配 ——
 * 手机端必须用系统原始密度，否则字号与点击区域整体失真。
 */
class PhoneMainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PhoneApp()
        }
    }
}
