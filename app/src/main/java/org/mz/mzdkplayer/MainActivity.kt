package org.mz.mzdkplayer

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.util.DisplayMetrics
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.util.Log
import androidx.media3.common.util.UnstableApi
import androidx.tv.material3.ExperimentalTvMaterial3Api

import org.mz.mzdkplayer.ui.MzDKPlayerAPP


@UnstableApi
class MainActivity : AppCompatActivity() {
    //    private val dm: DisplayMetrics = MzDkPlayerApplication.context.resources.displayMetrics
//    private val targetWidthInDp: Float = 960.0f
//    private val newDensity: Float = if (dm.widthPixels > 0) dm.widthPixels / targetWidthInDp else 1.0f
//    private val newDensityDpi: Int = (newDensity * DisplayMetrics.DENSITY_DEFAULT).toInt()
//
//    // 修复逻辑判断：如果当前 densityDpi 已经是目标值或计算出的值，则不需要修改
//    // 原逻辑 isChangeDensityDpi 可能导致即使需要修改也不修改，或不需要修改时反而修改。
//    // 这里简化为只要计算出的新值与当前不同，就认为需要修改。
//    // 你可能需要根据你的具体需求调整这个判断逻辑。
//    private val needsDensityChange: Boolean = dm.densityDpi != newDensityDpi && dm.widthPixels > 0
    companion object {
        private const val TAG = "MainActivity"
        private const val TARGET_DENSITY_DPI = 320 // 你想要设置的 DPI 值
    }

    // 标记是否需要修改 DPI。这里简化为总是尝试修改，除非已经是目标值。
    // 你可以根据需要添加更复杂的判断逻辑。
    private val needsDensityChange: Boolean by lazy {
        val currentDpi = MzDkPlayerApplication.context.resources.displayMetrics.densityDpi
        val shouldChange = currentDpi != TARGET_DENSITY_DPI
        Log.i(
            TAG,
            "Current DPI: $currentDpi, Target DPI: $TARGET_DENSITY_DPI, Needs Change: $shouldChange"
        )
        shouldChange
    }

    @OptIn(ExperimentalTvMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        setContent {
            MzDKPlayerAPP()
        }

    }

    override fun attachBaseContext(newBase: Context?) {
        var contextToUse = newBase // 默认使用原始 Context

        if (newBase != null && needsDensityChange) {
            // 创建新的 Configuration 并设置目标 densityDpi
            val newConfig = Configuration(newBase.resources.configuration) // Copy current config
            newConfig.densityDpi = TARGET_DENSITY_DPI
            Log.i(TAG, "Creating new context with densityDpi: $TARGET_DENSITY_DPI")
            // 使用新的 Configuration 创建新的 Context
            contextToUse = newBase.createConfigurationContext(newConfig)
        } else {
            if (newBase == null) {
                Log.w(TAG, "attachBaseContext called with null newBase")
            } else {
                Log.d(TAG, "Not applying new densityDpi, already set or not needed.")
            }
        }

        // 调用父类方法，传入可能被修改过的 Context
        super.attachBaseContext(contextToUse)
    }

    override fun onStop() {
//        if (!isChangeDensityDpi) {
//            val newConfig = Configuration();
//            newConfig.densityDpi = dm.densityDpi
//            // Log.d("attachBaseContext", dm.densityDpi.toString())
//            baseContext.createConfigurationContext(newConfig)
//        }
        super.onStop()
    }

    override fun onPause() {
//        if (!isChangeDensityDpi) {
//            val newConfig = Configuration()
//            newConfig.densityDpi = newDensityDpi
//            // Log.d("attachBaseContext", newDensityDpi.toString())
//            baseContext.createConfigurationContext(newConfig)
//        }
        super.onPause()
    }

    override fun onResume() {
//        if (!isChangeDensityDpi) {
//            val newConfig = Configuration()
//            newConfig.densityDpi = newDensityDpi
//            // Log.d("attachBaseContext", newDensityDpi.toString())
//            baseContext.createConfigurationContext(newConfig)
//        }
        super.onResume()
    }

    override fun onDestroy() {
//        if (!isChangeDensityDpi) {
//            val newConfig = Configuration()
//            newConfig.densityDpi = dm.densityDpi
//            // Log.d("attachBaseContext", newDensityDpi.toString())
//            baseContext.createConfigurationContext(newConfig)
//        }

        super.onDestroy()
    }

}


