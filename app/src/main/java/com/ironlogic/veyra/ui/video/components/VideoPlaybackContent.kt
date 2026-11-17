package com.ironlogic.veyra.ui.video.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import com.ironlogic.veyra.data.video.VideoItem
import com.ironlogic.veyra.playback.VideoPlaybackSession
import kotlinx.coroutines.delay

/**
 * A neutral playback host content block. Session creation, lifecycle ownership,
 * navigation, and the final phone/tablet arrangement remain with the caller.
 */
@Composable
fun VideoPlaybackContent(
    session: VideoPlaybackSession,
    modifier: Modifier = Modifier,
    relatedVideos: List<VideoItem> = emptyList(),
    onRelatedVideoClick: (VideoItem) -> Unit = {}
) {
    val player = session.engine.player
    var snapshot by remember(player) {
        mutableStateOf(session.engine.snapshot())
    }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onEvents(player: Player, events: Player.Events) {
                snapshot = session.engine.snapshot()
            }
        }
        player.addListener(listener)
        onDispose { player.removeListener(listener) }
    }

    LaunchedEffect(player) {
        while (true) {
            snapshot = session.engine.snapshot()
            delay(500L)
        }
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        VideoPlayerSurface(
            player = player,
            modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f)
        )
        VideoTransportControls(
            isPlaying = snapshot.isPlaying,
            positionMs = snapshot.currentPositionMs,
            durationMs = snapshot.durationMs,
            onPlayPause = { session.engine.togglePlayback() },
            onSeek = { session.engine.seekTo(it) },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
        )
        if (relatedVideos.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Related videos", style = MaterialTheme.typography.titleSmall)
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        items = relatedVideos,
                        key = { it.contentUri.toString() }
                    ) { video ->
                        TextButton(
                            onClick = { onRelatedVideoClick(video) },
                            modifier = Modifier.widthIn(max = 220.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(video.displayName, maxLines = 1)
                                Text(
                                    video.relativePath ?: "Device library",
                                    style = MaterialTheme.typography.labelSmall,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
