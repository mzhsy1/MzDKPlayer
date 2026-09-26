package org.mz.mzdkplayer.ui.audioplayer.components

import android.graphics.Bitmap
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.exoplayer.ExoPlayer
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.SurfaceDefaults
import androidx.tv.material3.Text
import org.mz.mzdkplayer.R
import org.mz.mzdkplayer.data.model.AudioInfo
import org.mz.mzdkplayer.tool.parseLrc
import org.mz.mzdkplayer.viewmodel.AudioPlayerViewModel
import java.util.Locale
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun AudioPlayerMainContent(
    audioInfo: AudioInfo?,
    isAudioInfoLoading: Boolean,
    currentFileName: String,
    coverBitmap: Bitmap?,
    isPlaying: Boolean,
    currentPositionProvider: () -> Long,
    exoPlayer: ExoPlayer,
    audioPlayerState: AudioPlayerState,
    audioPlayerViewModel: AudioPlayerViewModel,
    safeDurationMs: Long
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // [左侧栏]
        AudioInfoColumn(
            modifier = Modifier.weight(0.45f).fillMaxHeight(),
            audioInfo = audioInfo,
            isAudioInfoLoading = isAudioInfoLoading,
            currentFileName = currentFileName,
            coverBitmap = coverBitmap,
            isPlaying = isPlaying,
            currentPositionProvider = currentPositionProvider,
            exoPlayer = exoPlayer,
            audioPlayerState = audioPlayerState,
            audioPlayerViewModel = audioPlayerViewModel,
            safeDurationMs = safeDurationMs
        )

        Spacer(modifier = Modifier.width(20.dp))

        // [右侧栏]
        AudioLyricsColumn(
            modifier = Modifier.weight(0.6f).padding(end = 20.dp).fillMaxHeight(),
            audioInfo = audioInfo,
            currentPositionProvider = { currentPositionProvider().milliseconds }
        )
    }
}

@Composable
fun AudioInfoColumn(
    modifier: Modifier = Modifier,
    audioInfo: AudioInfo?,
    isAudioInfoLoading: Boolean,
    currentFileName: String,
    coverBitmap: Bitmap?,
    isPlaying: Boolean,
    currentPositionProvider: () -> Long,
    exoPlayer: ExoPlayer,
    audioPlayerState: AudioPlayerState,
    audioPlayerViewModel: AudioPlayerViewModel,
    safeDurationMs: Long
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.Start
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .aspectRatio(1f)
                .padding(bottom = 0.dp),
            contentAlignment = Alignment.Center
        ) {
            AlbumCoverDisplay(coverBitmap, isPlaying)
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = if (isAudioInfoLoading) currentFileName else audioInfo?.title ?: currentFileName,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(8.dp))

            val loadingText = stringResource(R.string.ui_label_loading)
            val unknownSingerText = stringResource(R.string.ui_label_unknown_singer)
            Text(
                text = buildString {
                    append(if (isAudioInfoLoading) loadingText else audioInfo?.artist ?: unknownSingerText)
                    if (!audioInfo?.album.isNullOrEmpty()) {
                        append(" — ${audioInfo?.album}")
                    }
                },
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = Color.White.copy(alpha = 0.7f)
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(8.dp))

            AudioInfoBadge(audioInfo)
            Spacer(modifier = Modifier.height(12.dp))

            AudioPlayerControls(
                isPlaying = isPlaying,
                currentPositionProvider = currentPositionProvider,
                exoPlayer = exoPlayer,
                state = audioPlayerState,
                audioPlayerViewModel = audioPlayerViewModel,
                contentDuration = safeDurationMs.milliseconds
            )
        }
    }
}

@Composable
fun AudioLyricsColumn(
    modifier: Modifier = Modifier,
    audioInfo: AudioInfo?,
    currentPositionProvider: () -> Duration
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.TopStart
    ) {
        val parsedLyrics = remember(audioInfo?.lyrics) {
            if (audioInfo?.lyrics != null) {
                parseLrc(audioInfo!!.lyrics)
            } else {
                emptyList()
            }
        }

        ScrollableLyricsView(
            currentPositionProvider = currentPositionProvider,
            parsedLyrics = parsedLyrics,
            topMaskColor = Color.Black.copy(alpha = 0.6f),
            bottomMaskColor = Color.Black.copy(alpha = 0.9f)
        )
    }
}

@Composable
fun AudioInfoBadge(audioInfo: AudioInfo?) {
    val sampleRateValue = audioInfo?.sampleRate?.toIntOrNull()
    val infoText = "${audioInfo?.bitsPerSample ?: "--"} BIT · ${
        sampleRateValue?.let {
            Locale.getDefault().let { locale ->
                String.format(locale, "%.1f KHZ", it / 1000.0)
            }
        } ?: "--"
    } · ${audioInfo?.bit ?: "--"} KBPS"

    Surface(
        colors = SurfaceDefaults.colors(
            containerColor = Color.White.copy(alpha = 0.95f),
            contentColor = Color.Black
        ),
        shape = RoundedCornerShape(4.dp),
    ) {
        Text(
            text = infoText,
            color = Color.Black,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}
