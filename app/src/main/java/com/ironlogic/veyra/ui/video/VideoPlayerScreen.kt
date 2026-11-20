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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.ironlogic.veyra.data.video.VideoItem
import com.ironlogic.veyra.data.video.VideoPlaybackProgressStore
import kotlinx.coroutines.delay

@Composable
fun VideoPlayerScreen(
    video: VideoItem,
    progressStore: VideoPlaybackProgressStore,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val player = remember(video.contentUri) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(video.contentUri))
            seekTo(progressStore.read(video)?.positionMs ?: 0L)
            prepare()
            playWhenReady = true
        }
    }

    LaunchedEffect(player, video.contentUri) {
        while (true) {
            delay(1_000)
            if (player.playbackState == Player.STATE_ENDED) {
                progressStore.clear(video)
                break
            }
            val durationMs = player.duration.takeIf { it > 0L } ?: video.durationMs
            progressStore.save(video, player.currentPosition, durationMs)
        }
    }

    DisposableEffect(player) {
        onDispose {
            if (player.playbackState == Player.STATE_ENDED) {
                progressStore.clear(video)
            } else {
                val durationMs = player.duration.takeIf { it > 0L } ?: video.durationMs
                progressStore.save(video, player.currentPosition, durationMs)
            }
            player.release()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        AndroidView(
            factory = { PlayerView(it).apply { this.player = player } },
            modifier = Modifier.fillMaxSize(),
        )
        Button(
            onClick = {
                val durationMs = player.duration.takeIf { it > 0L } ?: video.durationMs
                progressStore.save(video, player.currentPosition, durationMs)
                onBack()
            },
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp),
        ) {
            Text("Back")
        }
    }
}
