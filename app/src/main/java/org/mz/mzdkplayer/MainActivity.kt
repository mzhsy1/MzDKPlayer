package org.mz.mzdkplayer

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.tv.material3.DrawerState
import androidx.tv.material3.DrawerValue
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.ListItem
import androidx.tv.material3.ListItemDefaults
import androidx.tv.material3.NavigationDrawer

import org.mz.mzdkplayer.ui.MzDKPlayerAPP
import org.mz.mzdkplayer.ui.screen.FileScreen
import org.mz.mzdkplayer.ui.screen.HomeScreen

import org.mz.mzdkplayer.ui.screen.SMBListScreen
import java.net.URLDecoder
import kotlin.properties.Delegates


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


