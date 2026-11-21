package com.ironlogic.veyra.ui.video

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.ironlogic.veyra.data.video.VideoItem
import com.ironlogic.veyra.data.video.VideoPlaybackProgressStore
import com.ironlogic.veyra.data.video.VideoPlaybackSettingsStore
import com.ironlogic.veyra.data.video.VideoResizeMode
import com.ironlogic.veyra.data.video.VideoProgressWritePolicy
import kotlinx.coroutines.delay

@Composable
fun VideoPlayerScreen(
    video: VideoItem,
    progressStore: VideoPlaybackProgressStore,
    settingsStore: VideoPlaybackSettingsStore,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    var resizeMode by remember(video.contentUri) {
        mutableStateOf(settingsStore.readResizeMode())
    }
    val exoPlayer = remember(video.contentUri) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(video.contentUri))
            seekTo(progressStore.read(video)?.positionMs ?: 0L)
            prepare()
            playWhenReady = true
        }
    }

    LaunchedEffect(exoPlayer, video.contentUri) {
        var lastSavedPositionMs = exoPlayer.currentPosition
        while (true) {
            delay(1_000)
            if (exoPlayer.playbackState == Player.STATE_ENDED) {
                progressStore.clear(video)
                break
            }

            val currentPositionMs = exoPlayer.currentPosition
            if (VideoProgressWritePolicy.shouldPersist(currentPositionMs, lastSavedPositionMs)) {
                val durationMs = exoPlayer.duration.takeIf { it > 0L } ?: video.durationMs
                progressStore.save(video, currentPositionMs, durationMs)
                lastSavedPositionMs = currentPositionMs
            }
        }
    }

    DisposableEffect(exoPlayer) {
        onDispose {
            if (exoPlayer.playbackState == Player.STATE_ENDED) {
                progressStore.clear(video)
            } else {
                val durationMs = exoPlayer.duration.takeIf { it > 0L } ?: video.durationMs
                progressStore.save(video, exoPlayer.currentPosition, durationMs)
            }
            exoPlayer.release()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        AndroidView(
            factory = {
                PlayerView(it).apply {
                    player = exoPlayer
                    this.resizeMode = resizeMode.toPlayerViewResizeMode()
                }
            },
            update = { view ->
                view.player = exoPlayer
                view.resizeMode = resizeMode.toPlayerViewResizeMode()
            },
            modifier = Modifier.fillMaxSize(),
        )
        Button(
            onClick = {
                val durationMs = exoPlayer.duration.takeIf { it > 0L } ?: video.durationMs
                progressStore.save(video, exoPlayer.currentPosition, durationMs)
                onBack()
            },
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp),
        ) {
            Text("Back")
        }
        Button(
            onClick = {
                resizeMode = resizeMode.next()
                settingsStore.writeResizeMode(resizeMode)
            },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp),
        ) {
            Text("Aspect: ${resizeMode.label}")
        }
    }
}

private fun VideoResizeMode.toPlayerViewResizeMode(): Int = when (this) {
    VideoResizeMode.FIT -> AspectRatioFrameLayout.RESIZE_MODE_FIT
    VideoResizeMode.CROP -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
}
