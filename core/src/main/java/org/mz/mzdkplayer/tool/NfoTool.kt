package org.mz.mzdkplayer.tool

import android.util.Log
import org.mz.mzdkplayer.data.local.MediaCacheEntity
import org.mz.mzdkplayer.data.model.Genre
import java.io.InputStream
import javax.xml.parsers.DocumentBuilderFactory

object NfoTool {
    private const val TAG = "NfoTool"

    fun parseNfo(
        inputStream: InputStream,
        videoUri: String,
        dataSourceType: String,
        fileName: String,
        connectionName: String
    ): MediaCacheEntity? {
        return try {
            val dbFactory = DocumentBuilderFactory.newInstance()
            val dBuilder = dbFactory.newDocumentBuilder()
            val doc = dBuilder.parse(inputStream)
            doc.documentElement.normalize()

            val rootNode = doc.documentElement.nodeName
            val isMovie = rootNode == "movie"

            val title = doc.getElementsByTagName("title").item(0)?.textContent ?: fileName
            val overview = doc.getElementsByTagName("plot").item(0)?.textContent ?: ""
            val releaseDate = doc.getElementsByTagName("aired").item(0)?.textContent
                ?: doc.getElementsByTagName("premiered").item(0)?.textContent
                ?: doc.getElementsByTagName("year").item(0)?.textContent

            val voteAverage = doc.getElementsByTagName("rating").item(0)?.textContent?.toDoubleOrNull() ?: 0.0

            val posterPath = doc.getElementsByTagName("poster").item(0)?.textContent
                ?: doc.getElementsByTagName("thumb").item(0)?.textContent // Kodi uses <thumb aspect="poster"> but we simplify

            val backdropPath = doc.getElementsByTagName("fanart").item(0)?.let { fanartNode ->
                // Fanart usually has a <thumb> child
                val thumbs = (fanartNode as? org.w3c.dom.Element)?.getElementsByTagName("thumb")
                if (thumbs != null && thumbs.length > 0) {
                    thumbs.item(0).textContent
                } else null
            }

            val genres = mutableListOf<Genre>()
            val genreNodes = doc.getElementsByTagName("genre")
            for (i in 0 until genreNodes.length) {
                val name = genreNodes.item(i).textContent
                if (name.isNotBlank()) {
                    genres.add(Genre(id = i, name = name))
                }
            }

            val tmdbId = doc.getElementsByTagName("tmdbid").item(0)?.textContent?.toIntOrNull()
                ?: doc.getElementsByTagName("id").item(0)?.textContent?.toIntOrNull()
                ?: 0

            MediaCacheEntity(
                videoUri = videoUri,
                dataSourceType = dataSourceType,
                fileName = fileName,
                connectionName = connectionName,
                tmdbId = tmdbId,
                mediaType = if (isMovie) "movie" else "tv",
                title = title,
                overview = overview,
                posterPath = posterPath,
                backdropPath = backdropPath,
                releaseDate = releaseDate,
                voteAverage = voteAverage,
                isDetailsLoaded = true,
                genres = genres,
                groupKey = if (isMovie) "movie_${videoUri}" else "tv_${tmdbId}"
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing NFO for $fileName", e)
            null
        }
    }
}
