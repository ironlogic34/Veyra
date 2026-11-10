package com.ironlogic.veyra.ui.video.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.Player
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView

/**
 * Media3 rendering surface only. Playback ownership and controls stay outside
 * this component so phone and tablet screens can arrange them independently.
 */
@Composable
fun VideoPlayerSurface(
    player: Player,
    modifier: Modifier = Modifier,
    resizeMode: Int = AspectRatioFrameLayout.RESIZE_MODE_FIT
) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            PlayerView(context).apply {
                this.player = player
                useController = false
                this.resizeMode = resizeMode
                setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
            }
        },
        update = { view ->
            if (view.player !== player) {
                view.player = player
            }
            view.resizeMode = resizeMode
        },
        onRelease = { view ->
            // Detach the view without releasing the session-owned player.
            view.player = null
        }
    )
}
