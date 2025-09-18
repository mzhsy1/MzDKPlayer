package org.mz.mzdkplayer

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.util.DisplayMetrics
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.util.UnstableApi
import androidx.tv.material3.ExperimentalTvMaterial3Api

import org.mz.mzdkplayer.ui.MzDKPlayerAPP


@UnstableApi
class MainActivity : AppCompatActivity() {
    private val dm: DisplayMetrics = MzDkPlayerApplication.context.resources.displayMetrics
    private val newDensity: Float = dm.widthPixels / 960.0.toFloat();

    private val newDensityDpi: Int = (newDensity * DisplayMetrics.DENSITY_DEFAULT).toInt();
    private val isChangeDensityDpi: Boolean = dm.densityDpi % DisplayMetrics.DENSITY_DEFAULT == 0


    @OptIn(ExperimentalTvMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        if (!isChangeDensityDpi) {
            val newConfig = Configuration();
            newConfig.densityDpi = newDensityDpi
            applicationContext.createConfigurationContext(newConfig)
        }
        super.onCreate(savedInstanceState)

        setContent {
            MzDKPlayerAPP()
        }

        }
    override fun attachBaseContext(newBase: Context?) {
        val newConfig = Configuration()
        if (!isChangeDensityDpi) {
            newConfig.densityDpi = newDensityDpi

        }
        super.attachBaseContext(newBase?.createConfigurationContext(newConfig))
    }

    override fun onStop() {
        if (!isChangeDensityDpi) {
            val newConfig = Configuration();
            newConfig.densityDpi = dm.densityDpi
            // Log.d("attachBaseContext", dm.densityDpi.toString())
            baseContext.createConfigurationContext(newConfig)
        }
        super.onStop()
    }

    override fun onResume() {
        if (!isChangeDensityDpi) {
            val newConfig = Configuration()
            newConfig.densityDpi = newDensityDpi
            // Log.d("attachBaseContext", newDensityDpi.toString())
            baseContext.createConfigurationContext(newConfig)
        }
        super.onResume()
    }

    override fun onDestroy() {
        if (!isChangeDensityDpi) {
            val newConfig = Configuration()
            newConfig.densityDpi = dm.densityDpi
            // Log.d("attachBaseContext", newDensityDpi.toString())
            baseContext.createConfigurationContext(newConfig)
        }

        super.onDestroy()
    }

}


