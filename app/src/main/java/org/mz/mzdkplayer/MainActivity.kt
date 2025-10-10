package org.mz.mzdkplayer

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.util.UnstableApi
import androidx.tv.material3.ExperimentalTvMaterial3Api
import org.mz.mzdkplayer.ui.MzDKPlayerAPP

@UnstableApi
class MainActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "MainActivity"
        private const val TARGET_DENSITY_DPI = 320 // 目标 DPI 值
        private const val BASE_DENSITY = 160       // Android mdpi 基准密度
    }

    // 不再使用 lazy 初始化，避免在 Context 完全可用前访问资源
    private var needsDensityChangeCalculated = false
    private var needsDensityChangeResult = false

    /**
     * 在 attachBaseContext 中安全地计算是否需要更改 DPI。
     * @param context 用于获取 display metrics 的有效上下文
     */
    private fun calculateNeedsDensityChange(context: Context): Boolean {
        if (needsDensityChangeCalculated) {
            return needsDensityChangeResult
        }

        val currentDpi = context.resources?.displayMetrics?.densityDpi ?: -1
        val isValidDpi = currentDpi > 0
        val isStandardMultiple = isValidDpi && (currentDpi % BASE_DENSITY == 0)
        val shouldChange = isValidDpi && !isStandardMultiple

        Log.i(
            TAG,
            "Current DPI: $currentDpi. Valid? $isValidDpi. Is standard multiple of $BASE_DENSITY? $isStandardMultiple. Needs change? $shouldChange"
        )

        needsDensityChangeResult = shouldChange
        needsDensityChangeCalculated = true
        return shouldChange
    }

    @OptIn(ExperimentalTvMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MzDKPlayerAPP()
        }
    }

    override fun attachBaseContext(newBase: Context?) {
        var contextToUse = newBase

        if (newBase != null) {
            try {
                val shouldApplyCustomDpi = calculateNeedsDensityChange(newBase)
                if (shouldApplyCustomDpi) {
                    val newConfig = Configuration(newBase.resources.configuration)
                    newConfig.densityDpi = TARGET_DENSITY_DPI
                    Log.i(TAG, "Applying custom densityDpi: $TARGET_DENSITY_DPI")
                    contextToUse = newBase.createConfigurationContext(newConfig)
                } else {
                    Log.d(TAG, "Keeping original densityDpi as it's a standard multiple of $BASE_DENSITY or invalid.")
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



