package com.ironlogic.veyra.ui.video.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import com.ironlogic.veyra.playback.VideoPlaybackSession
import com.ironlogic.veyra.playback.VideoPlayerSnapshot
import kotlinx.coroutines.delay

/**
 * A neutral playback host content block. Session creation, lifecycle ownership,
 * navigation, and the final phone/tablet arrangement remain with the caller.
 */
@Composable
fun VideoPlaybackContent(
    session: VideoPlaybackSession,
    modifier: Modifier = Modifier
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
            modifier = Modifier.fillMaxWidth()
        )
        VideoTransportControls(
            isPlaying = snapshot.isPlaying,
            positionMs = snapshot.currentPositionMs,
            durationMs = snapshot.durationMs,
            onPlayPause = { session.engine.togglePlayback() },
            onSeek = { session.engine.seekTo(it) },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
        )
    }
}
