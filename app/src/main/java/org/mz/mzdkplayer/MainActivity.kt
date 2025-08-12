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


class MainActivity : AppCompatActivity() {
    private lateinit var context: Context


    @OptIn(ExperimentalTvMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        context = this
        val dm: DisplayMetrics = context.resources.displayMetrics
        Log.d("dm",dm.densityDpi.toString())
        val newDensity: Float = dm.widthPixels / 960.0.toFloat();

        val newDensityDpi: Int = (newDensity * DisplayMetrics.DENSITY_DEFAULT).toInt();
        val isChangeDensityDpi: Boolean = dm.densityDpi % DisplayMetrics.DENSITY_DEFAULT == 0
        if (!isChangeDensityDpi) {
            val newConfig = Configuration();
            newConfig.densityDpi = newDensityDpi
            applicationContext.createConfigurationContext(newConfig)
        } else {
            Log.d("isChangeDensityDpi", isChangeDensityDpi.toString())
        }
        setContent {
            MzDKPlayerAPP()
        }

        }

}


