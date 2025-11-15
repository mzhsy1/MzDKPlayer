package org.mz.mzdkplayer

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.media3.common.util.UnstableApi
import org.mz.mzdkplayer.ui.MzDKPlayerAPP

@UnstableApi
class MainActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "MainActivity"
        private const val TARGET_WIDTH_DP = 960
        private const val MIN_DENSITY_DPI = 120
        private const val MAX_DENSITY_DPI = 640
    }

    // 用于 Compose 界面接收外部视频 URI
    private var externalVideoUri: Uri? = null

    // DPI 相关（保留你原有逻辑）
    private var needsDensityChangeCalculated = false
    private var needsDensityChangeResult = false
    private var calculatedTargetDensityDpi = -1

    /**
     * 计算目标密度DPI
     */
    private fun calculateTargetDensityDpi(context: Context): Int {
        val displayMetrics = context.resources.displayMetrics
        val screenWidthDp = displayMetrics.widthPixels / displayMetrics.density
        val targetDpi = (displayMetrics.widthPixels * 160.0 / TARGET_WIDTH_DP).toInt()
        val clampedDpi = targetDpi.coerceIn(MIN_DENSITY_DPI, MAX_DENSITY_DPI)

        Log.i(TAG, "Screen width: ${displayMetrics.widthPixels}px, " +
                "Screen width in dp: ${screenWidthDp}, " +
                "Calculated target DPI: $targetDpi, Clamped to: $clampedDpi")

        return clampedDpi
    }

    private fun calculateNeedsDensityChange(context: Context): Boolean {
        if (needsDensityChangeCalculated) return needsDensityChangeResult

        val currentDpi = context.resources.displayMetrics.densityDpi
        if (currentDpi <= 0) {
            Log.w(TAG, "Invalid current DPI: $currentDpi")
            needsDensityChangeResult = false
            needsDensityChangeCalculated = true
            return false
        }

        calculatedTargetDensityDpi = calculateTargetDensityDpi(context)
        val shouldChange = calculatedTargetDensityDpi != currentDpi

        Log.i(TAG, "Current DPI: $currentDpi. Target DPI: $calculatedTargetDensityDpi. Needs change? $shouldChange")

        needsDensityChangeResult = shouldChange
        needsDensityChangeCalculated = true
        return shouldChange
    }

    override fun attachBaseContext(newBase: Context?) {
        var contextToUse = newBase

        if (newBase != null) {
            try {
                val shouldApplyCustomDpi = calculateNeedsDensityChange(newBase)
                if (shouldApplyCustomDpi && calculatedTargetDensityDpi > 0) {
                    val newConfig = Configuration(newBase.resources.configuration)
                    newConfig.densityDpi = calculatedTargetDensityDpi
                    Log.i(TAG, "Applying custom densityDpi: $calculatedTargetDensityDpi")
                    contextToUse = newBase.createConfigurationContext(newConfig)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error applying density change", e)
            }
        }

        super.attachBaseContext(contextToUse)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleIntent(intent)

        setContent {
            var showSplash by remember { mutableStateOf(true) }
            if (showSplash) {
                LaunchScreen(onFinish = { showSplash = false })
            } else {
                MzDKPlayerAPP(externalVideoUri = externalVideoUri)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
        // 注意：由于 Compose 状态在 onCreate 中初始化，
        // 如果需要实时响应 onNewIntent，建议将 externalVideoUri 提升到 ViewModel 或使用 rememberUpdatedState。
        // 对于简单场景（如只启动一次播放），当前方式已足够。
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_VIEW) {
            val mimeType = intent.type
            val uri = intent.data

            // 只处理 video/* 类型（来自云盘等）
            if (mimeType?.startsWith("video/") == true && uri != null) {
                externalVideoUri = uri
                Log.i(TAG, "Received external video: $uri (MIME: $mimeType)")
            } else {
                Log.w(TAG, "Ignored non-video VIEW intent: type=$mimeType, uri=$uri")
            }
        }
    }
}