package com.ironlogic.veyra.playback

import android.content.Context
import android.net.Uri
import androidx.annotation.MainThread
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer

/**
 * Owns the Media3 player used by Veyra's video playback feature.
 *
 * Create and control this class on the main thread. The owner is responsible
 * for calling [close] when the playback session is finished.
 */
@MainThread
class VideoPlaybackEngine(context: Context) : AutoCloseable {
    private val exoPlayer = ExoPlayer.Builder(context.applicationContext).build().apply {
        setAudioAttributes(androidx.media3.common.AudioAttributes.DEFAULT, true)
        setWakeMode(C.WAKE_MODE_LOCAL)
    }

    val player: Player
        get() = exoPlayer

    @MainThread
    fun prepare(
        mediaUri: Uri,
        startPositionMs: Long = 0L,
        playWhenReady: Boolean = true
    ) {
        require(startPositionMs >= 0L) { "Start position cannot be negative." }
        exoPlayer.setMediaItem(MediaItem.fromUri(mediaUri), startPositionMs)
        exoPlayer.prepare()
        exoPlayer.playWhenReady = playWhenReady
    }

    @MainThread
    fun prepareQueue(
        mediaUris: List<Uri>,
        startIndex: Int = 0,
        startPositionMs: Long = 0L,
        playWhenReady: Boolean = true,
        displayNames: List<String> = emptyList()
    ) {
        require(mediaUris.isNotEmpty()) { "Playback queue cannot be empty." }
        require(startIndex in mediaUris.indices) { "Start index is outside the playback queue." }
        require(startPositionMs >= 0L) { "Start position cannot be negative." }
        require(displayNames.isEmpty() || displayNames.size == mediaUris.size) {
            "Display names must match the playback queue size."
        }
        val items = mediaUris.mapIndexed { index, uri ->
            val title = displayNames.getOrNull(index)
            if (title.isNullOrBlank()) {
                MediaItem.fromUri(uri)
            } else {
                MediaItem.Builder()
                    .setUri(uri)
                    .setMediaMetadata(MediaMetadata.Builder().setTitle(title).build())
                    .build()
            }
        }
        exoPlayer.setMediaItems(items, startIndex, startPositionMs)
        exoPlayer.prepare()
        exoPlayer.playWhenReady = playWhenReady
    }

    @MainThread fun play() = exoPlayer.play()
    @MainThread fun pause() = exoPlayer.pause()
    @MainThread fun togglePlayback() {
        if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
    }
    @MainThread fun seekTo(positionMs: Long) = exoPlayer.seekTo(positionMs.coerceAtLeast(0L))
    @MainThread fun seekToNextMediaItem() = exoPlayer.seekToNextMediaItem()
    @MainThread fun currentMediaItemIndex(): Int = exoPlayer.currentMediaItemIndex
    @MainThread fun currentMediaUri(): Uri? =
        exoPlayer.currentMediaItem?.localConfiguration?.uri

    @MainThread
    fun setRepeatOne(enabled: Boolean) {
        exoPlayer.repeatMode = if (enabled) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
    }

    @MainThread fun addListener(listener: Player.Listener) = exoPlayer.addListener(listener)
    @MainThread fun removeListener(listener: Player.Listener) = exoPlayer.removeListener(listener)

    @MainThread
    fun snapshot(): VideoPlayerSnapshot {
        val duration = exoPlayer.duration
        return VideoPlayerSnapshot(
            isPlaying = exoPlayer.isPlaying,
            playbackState = exoPlayer.playbackState,
            currentPositionMs = exoPlayer.currentPosition.coerceAtLeast(0L),
            bufferedPositionMs = exoPlayer.bufferedPosition.coerceAtLeast(0L),
            durationMs = duration.takeUnless { it == C.TIME_UNSET }?.coerceAtLeast(0L),
            isEnded = exoPlayer.playbackState == Player.STATE_ENDED
        )
    }

    @MainThread
    override fun close() {
        exoPlayer.release()
    }
}

data class VideoPlayerSnapshot(
    val isPlaying: Boolean,
    val playbackState: Int,
    val currentPositionMs: Long,
    val bufferedPositionMs: Long,
    val durationMs: Long?,
    val isEnded: Boolean
)
