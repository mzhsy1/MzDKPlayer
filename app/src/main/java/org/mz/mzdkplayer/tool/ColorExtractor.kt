// ColorExtractor.kt
package org.mz.mzdkplayer.tool

import android.graphics.Bitmap
import androidx.palette.graphics.Palette
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object ColorExtractor {
    suspend fun extractColorsFromBitmap(bitmap: Bitmap?): List<Color> {
        if (bitmap == null) {
            return getDefaultColors()
        }

        return withContext(Dispatchers.IO) {
            try {
                val palette = Palette.from(bitmap).generate()
                val colors = mutableListOf<Color>()

                // 从调色板中提取主要颜色
                palette.vibrantSwatch?.let { swatch ->
                    colors.add(Color(swatch.rgb))
                }
                palette.lightVibrantSwatch?.let { swatch ->
                    colors.add(Color(swatch.rgb))
                }
                palette.darkVibrantSwatch?.let { swatch ->
                    colors.add(Color(swatch.rgb))
                }
                palette.mutedSwatch?.let { swatch ->
                    colors.add(Color(swatch.rgb))
                }
                palette.lightMutedSwatch?.let { swatch ->
                    colors.add(Color(swatch.rgb))
                }
                palette.darkMutedSwatch?.let { swatch ->
                    colors.add(Color(swatch.rgb))
                }

                // 如果提取的颜色不足，使用默认颜色补充
                if (colors.size < 3) {
                    colors.addAll(getDefaultColors().take(3 - colors.size))
                }

                colors.take(3) // 只取前3个颜色用于渐变
            } catch (e: Exception) {
                getDefaultColors()
            }
        }
    }

    private fun getDefaultColors(): List<Color> {
        return listOf(
            Color(0xFF81ECEC), // 薄荷蓝
            Color(0xFF74B9FF), // 天空蓝
            Color(0xFFA29BFE)  // 淡紫色
        )
    }
}