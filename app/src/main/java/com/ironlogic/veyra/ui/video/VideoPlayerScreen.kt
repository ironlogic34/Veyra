package com.ironlogic.veyra.ui.video

import android.app.Activity
import android.content.pm.ActivityInfo
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.ironlogic.veyra.data.video.VideoItem
import com.ironlogic.veyra.data.video.VideoPlaybackProgressStore
import com.ironlogic.veyra.data.video.VideoPlaybackHistoryStore
import com.ironlogic.veyra.data.video.VideoPlaybackSettingsStore
import com.ironlogic.veyra.data.video.VideoOrientationMode
import com.ironlogic.veyra.data.video.VideoResizeMode
import com.ironlogic.veyra.data.video.VideoProgressWritePolicy
import kotlinx.coroutines.delay

@Composable
fun VideoPlayerScreen(
    video: VideoItem,
    progressStore: VideoPlaybackProgressStore,
    historyStore: VideoPlaybackHistoryStore,
    settingsStore: VideoPlaybackSettingsStore,
    isTabletLandscape: Boolean,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    var resizeMode by remember(video.contentUri) {
        mutableStateOf(settingsStore.readResizeMode())
    }
    val orientationMode = remember(video.contentUri) {
        settingsStore.readOrientationMode()
    }
    val activity = context as? Activity
    val originalRequestedOrientation = remember(activity) { activity?.requestedOrientation }

    DisposableEffect(activity, orientationMode) {
        activity?.requestedOrientation = orientationMode.toRequestedOrientation()
        onDispose {
            if (activity != null && originalRequestedOrientation != null) {
                activity.requestedOrientation = originalRequestedOrientation
            }
        }
    }

    val exoPlayer = remember(video.contentUri) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(video.contentUri))
            seekTo(progressStore.read(video)?.positionMs ?: 0L)
            prepare()
            playWhenReady = true
        }
    }
    var controlsVisible by remember(video.contentUri) { mutableStateOf(true) }
    var isPlaying by remember(video.contentUri) { mutableStateOf(true) }
    var isSeeking by remember(video.contentUri) { mutableStateOf(false) }
    var currentPositionMs by remember(video.contentUri) { mutableStateOf(0L) }
    var durationMs by remember(video.contentUri) { mutableStateOf(video.durationMs) }
    var seekPreviewMs by remember(video.contentUri) { mutableStateOf(0L) }

    fun saveAndExit() {
        val duration = exoPlayer.duration.takeIf { it > 0L } ?: video.durationMs
        progressStore.save(video, exoPlayer.currentPosition, duration)
        historyStore.record(video, exoPlayer.currentPosition, duration)
        onBack()
    }

    BackHandler(onBack = ::saveAndExit)

    LaunchedEffect(exoPlayer, isSeeking) {
        while (true) {
            durationMs = exoPlayer.duration.takeIf { it > 0L } ?: video.durationMs
            isPlaying = exoPlayer.isPlaying
            if (!isSeeking) {
                currentPositionMs = exoPlayer.currentPosition.coerceAtLeast(0L)
                seekPreviewMs = currentPositionMs
            }
            delay(250)
        }
    }

    LaunchedEffect(controlsVisible, isPlaying, isSeeking, isTabletLandscape) {
        if (!isTabletLandscape && controlsVisible && isPlaying && !isSeeking) {
            delay(3_500)
            controlsVisible = false
        }
    }

    LaunchedEffect(exoPlayer, video.contentUri) {
        val initialDurationMs = exoPlayer.duration.takeIf { it > 0L } ?: video.durationMs
        historyStore.record(video, exoPlayer.currentPosition, initialDurationMs)
        var lastSavedPositionMs = exoPlayer.currentPosition
        while (true) {
            delay(1_000)
            if (exoPlayer.playbackState == Player.STATE_ENDED) {
                progressStore.clear(video)
                historyStore.record(video, exoPlayer.currentPosition, video.durationMs)
                break
            }

            val current = exoPlayer.currentPosition
            if (VideoProgressWritePolicy.shouldPersist(current, lastSavedPositionMs)) {
                val currentDurationMs = exoPlayer.duration.takeIf { it > 0L } ?: video.durationMs
                progressStore.save(video, current, currentDurationMs)
                lastSavedPositionMs = current
            }
        }
    }

    DisposableEffect(exoPlayer) {
        onDispose {
            if (exoPlayer.playbackState == Player.STATE_ENDED) {
                progressStore.clear(video)
            } else {
                val duration = exoPlayer.duration.takeIf { it > 0L } ?: video.durationMs
                progressStore.save(video, exoPlayer.currentPosition, duration)
                historyStore.record(video, exoPlayer.currentPosition, duration)
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
                    useController = isTabletLandscape
                    controllerAutoShow = true
                    this.resizeMode = resizeMode.toPlayerViewResizeMode()
                }
            },
            update = { view ->
                view.player = exoPlayer
                view.useController = isTabletLandscape
                view.resizeMode = resizeMode.toPlayerViewResizeMode()
            },
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (isTabletLandscape) Modifier else Modifier.pointerInput(Unit) {
                        detectTapGestures(onTap = { controlsVisible = true })
                    },
                ),
        )

        if (isTabletLandscape) {
            Button(
                onClick = ::saveAndExit,
                modifier = Modifier.align(Alignment.TopStart).padding(16.dp),
            ) {
                Text("Back")
            }
            Button(
                onClick = {
                    resizeMode = resizeMode.next()
                    settingsStore.writeResizeMode(resizeMode)
                },
                modifier = Modifier.align(Alignment.TopEnd).padding(16.dp),
            ) {
                Text("Aspect: ${resizeMode.label}")
            }
        } else if (controlsVisible) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.42f))
                    .padding(horizontal = 18.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    TextButton(onClick = ::saveAndExit) { Text("Back") }
                    Text(
                        video.displayName,
                        modifier = Modifier.weight(1f),
                        color = Color.White,
                        maxLines = 1,
                        fontWeight = FontWeight.SemiBold,
                    )
                    TextButton(
                        onClick = {
                            resizeMode = resizeMode.next()
                            settingsStore.writeResizeMode(resizeMode)
                        },
                    ) {
                        Text(resizeMode.label)
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(formatPlaybackTime(if (isSeeking) seekPreviewMs else currentPositionMs), color = Color.White, fontSize = 12.sp)
                        Text(formatPlaybackTime(durationMs), color = Color.White, fontSize = 12.sp)
                    }
                    Slider(
                        value = if (durationMs > 0L) {
                            ((if (isSeeking) seekPreviewMs else currentPositionMs).toFloat() / durationMs).coerceIn(0f, 1f)
                        } else {
                            0f
                        },
                        onValueChange = { fraction ->
                            isSeeking = true
                            seekPreviewMs = (fraction * durationMs).toLong().coerceIn(0L, durationMs.coerceAtLeast(0L))
                        },
                        onValueChangeFinished = {
                            exoPlayer.seekTo(seekPreviewMs)
                            currentPositionMs = seekPreviewMs
                            isSeeking = false
                        },
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TextButton(
                            onClick = {
                                exoPlayer.seekTo((exoPlayer.currentPosition - 10_000L).coerceAtLeast(0L))
                                controlsVisible = true
                            },
                        ) { Text("−10s") }
                        Spacer(modifier = Modifier.weight(1f))
                        TextButton(
                            onClick = {
                                if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
                                controlsVisible = true
                            },
                        ) {
                            Text(
                                if (isPlaying) "Pause" else "Play",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                            )
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        TextButton(
                            onClick = {
                                val target = (exoPlayer.currentPosition + 10_000L).coerceAtMost(durationMs)
                                exoPlayer.seekTo(target)
                                controlsVisible = true
                            },
                        ) { Text("+10s") }
                    }
                }
            }
        }
    }
}

private fun formatPlaybackTime(positionMs: Long): String {
    val totalSeconds = positionMs.coerceAtLeast(0L) / 1_000L
    val seconds = totalSeconds % 60L
    val minutes = (totalSeconds / 60L) % 60L
    val hours = totalSeconds / 3_600L
    return if (hours > 0L) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%d:%02d".format(totalSeconds / 60L, seconds)
    }
}

private fun VideoOrientationMode.toRequestedOrientation(): Int = when (this) {
    VideoOrientationMode.SYSTEM -> ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
    VideoOrientationMode.PORTRAIT -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
    VideoOrientationMode.LANDSCAPE -> ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
}

private fun VideoResizeMode.toPlayerViewResizeMode(): Int = when (this) {
    VideoResizeMode.FIT -> AspectRatioFrameLayout.RESIZE_MODE_FIT
    VideoResizeMode.CROP -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
}
