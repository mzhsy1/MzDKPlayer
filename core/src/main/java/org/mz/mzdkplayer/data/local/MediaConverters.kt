package org.mz.mzdkplayer.data.local



import androidx.room3.ColumnTypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.mz.mzdkplayer.data.model.Genre

class MediaConverters {
    private val gson = Gson()

    @ColumnTypeConverter
    fun fromGenreList(value: List<Genre>?): String {
        return gson.toJson(value)
    }

    @ColumnTypeConverter
    fun toGenreList(value: String?): List<Genre> {
        if (value == null) return emptyList()
        val listType = object : TypeToken<List<Genre>>() {}.type
        return gson.fromJson(value, listType)
    }

    @ColumnTypeConverter
    fun fromStringList(value: List<String>?): String {
        return gson.toJson(value)
    }

    @ColumnTypeConverter
    fun toStringList(value: String?): List<String> {
        if (value == null) return emptyList()
        val listType = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(value, listType)
    }
}