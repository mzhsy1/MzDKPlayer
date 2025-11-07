package org.mz.mzdkplayer

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.media3.common.util.UnstableApi
import androidx.tv.material3.ExperimentalTvMaterial3Api
import org.mz.mzdkplayer.ui.MzDKPlayerAPP

@UnstableApi
class MainActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "MainActivity"
        private const val TARGET_WIDTH_DP = 960 // 目标宽度，单位dp
        private const val MIN_DENSITY_DPI = 120 // 最小密度DPI
        private const val MAX_DENSITY_DPI = 640 // 最大密度DPI
    }

    // 不再使用 lazy 初始化，避免在 Context 完全可用前访问资源
    private var needsDensityChangeCalculated = false
    private var needsDensityChangeResult = false
    private var calculatedTargetDensityDpi = -1

    /**
     * 计算目标密度DPI
     * @param context 用于获取屏幕尺寸的上下文
     * @return 计算出的目标DPI值
     */
    private fun calculateTargetDensityDpi(context: Context): Int {
        val displayMetrics = context.resources.displayMetrics
        val screenWidthDp = displayMetrics.widthPixels / displayMetrics.density

        // 计算需要的DPI：实际像素宽度 / 目标dp宽度
        val targetDpi = (displayMetrics.widthPixels * 160.0 / TARGET_WIDTH_DP).toInt()

        // 限制在合理范围内
        val clampedDpi = targetDpi.coerceIn(MIN_DENSITY_DPI, MAX_DENSITY_DPI)

        Log.i(TAG, "Screen width: ${displayMetrics.widthPixels}px, Screen width in dp: ${screenWidthDp}, " +
                "Calculated target DPI: $targetDpi, Clamped to: $clampedDpi")

        return clampedDpi
    }

    /**
     * 在 attachBaseContext 中安全地计算是否需要更改 DPI。
     * @param context 用于获取 display metrics 的有效上下文
     */
    private fun calculateNeedsDensityChange(context: Context): Boolean {
        if (needsDensityChangeCalculated) {
            return needsDensityChangeResult
        }

        val currentDpi = context.resources?.displayMetrics?.densityDpi ?: -1
        if (currentDpi <= 0) {
            Log.w(TAG, "Invalid current DPI: $currentDpi")
            needsDensityChangeResult = false
            needsDensityChangeCalculated = true
            return false
        }

        calculatedTargetDensityDpi = calculateTargetDensityDpi(context)
        val shouldChange = calculatedTargetDensityDpi != currentDpi

        Log.i(
            TAG,
            "Current DPI: $currentDpi. Target DPI: $calculatedTargetDensityDpi. Needs change? $shouldChange"
        )

        needsDensityChangeResult = shouldChange
        needsDensityChangeCalculated = true
        return shouldChange
    }

    @OptIn(ExperimentalTvMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var showSplash by remember { mutableStateOf(true) }
            if (showSplash) {
                LaunchScreen(onFinish = { showSplash = false })
            } else {
                MzDKPlayerAPP()
            }

        }
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
                } else if (!shouldApplyCustomDpi) {
                    Log.d(TAG, "No density change needed - current DPI matches target or target DPI is invalid")
                } else {
                    Log.w(TAG, "Density change needed but target DPI is invalid: $calculatedTargetDensityDpi")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error calculating or applying density change", e)
                // 出错情况下保留原始context不变
            }
        } else {
            Log.w(TAG, "attachBaseContext called with null newBase")
        }

        super.attachBaseContext(contextToUse)
    }
}



