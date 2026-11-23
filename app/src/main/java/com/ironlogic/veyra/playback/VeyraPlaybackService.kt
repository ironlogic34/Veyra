package com.ironlogic.veyra.playback

import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.ironlogic.veyra.data.video.VideoItem
import com.ironlogic.veyra.data.video.VideoPlaybackHistoryStore
import com.ironlogic.veyra.data.video.VideoPlaybackProgressStore
import com.ironlogic.veyra.data.video.VideoProgressWritePolicy

/** Owns playback and keeps progress/history current while the UI is not visible. */
class VeyraPlaybackService : MediaSessionService() {
    private var mediaSession: MediaSession? = null
    private lateinit var progressStore: VideoPlaybackProgressStore
    private lateinit var historyStore: VideoPlaybackHistoryStore

    private val progressHandler = Handler(Looper.getMainLooper())
    private var trackedVideo: VideoItem? = null
    private var trackedPositionMs = 0L
    private var trackedDurationMs = 0L
    private var lastSavedPositionMs = 0L

    private val progressRunnable = object : Runnable {
        override fun run() {
            updateTrackedSnapshot()
            persistTrackedPlayback()
            progressHandler.postDelayed(this, PROGRESS_INTERVAL_MS)
        }
    }

    override fun onCreate() {
        super.onCreate()
        progressStore = VideoPlaybackProgressStore(this)
        historyStore = VideoPlaybackHistoryStore(this)

        val player = ExoPlayer.Builder(this).build()
        player.addListener(
            object : Player.Listener {
                override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                    persistTrackedPlayback(force = true)
                    trackedVideo = null
                    lastSavedPositionMs = 0L
                    updateTrackedSnapshot()
                }

                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    if (!isPlaying) {
                        updateTrackedSnapshot()
                        persistTrackedPlayback(force = true)
                    }
                }

                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_READY || playbackState == Player.STATE_ENDED) {
                        updateTrackedSnapshot()
                        persistTrackedPlayback(force = true)
                    }
                }
            },
        )
        mediaSession = MediaSession.Builder(this, player).build()
        progressHandler.post(progressRunnable)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? =
        mediaSession

    override fun onDestroy() {
        updateTrackedSnapshot()
        persistTrackedPlayback(force = true)
        progressHandler.removeCallbacks(progressRunnable)
        mediaSession?.let { session ->
            session.player.release()
            session.release()
        }
        mediaSession = null
        super.onDestroy()
    }

    private fun updateTrackedSnapshot() {
        val player = mediaSession?.player ?: return
        val mediaItem = player.currentMediaItem ?: return
        val contentUri = mediaItem.mediaId.takeIf { it.isNotBlank() }
            ?: mediaItem.localConfiguration?.uri?.toString()
            ?: return

        if (trackedVideo?.contentUri != contentUri) {
            persistTrackedPlayback(force = true)
            val displayName = mediaItem.mediaMetadata.title
                ?.toString()
                ?.takeIf { it.isNotBlank() }
                ?: Uri.parse(contentUri).lastPathSegment
                ?: "Untitled video"
            trackedVideo = VideoItem(
                id = contentUri,
                contentUri = contentUri,
                displayName = displayName,
                durationMs = 0L,
                sizeBytes = 0L,
                addedEpochSeconds = 0L,
            )
            lastSavedPositionMs = 0L
        }

        trackedPositionMs = player.currentPosition.coerceAtLeast(0L)
        trackedDurationMs = player.duration.takeIf { it > 0L } ?: trackedDurationMs
    }

    private fun persistTrackedPlayback(force: Boolean = false) {
        val video = trackedVideo ?: return
        if (!force && !VideoProgressWritePolicy.shouldPersist(trackedPositionMs, lastSavedPositionMs)) {
            return
        }

        val player = mediaSession?.player
        val duration = trackedDurationMs.coerceAtLeast(0L)
        if (player?.playbackState == Player.STATE_ENDED) {
            progressStore.clear(video)
            historyStore.record(video, duration, duration)
            trackedPositionMs = duration
        } else {
            progressStore.save(video, trackedPositionMs, duration)
            historyStore.record(video, trackedPositionMs, duration)
        }
        lastSavedPositionMs = trackedPositionMs
    }

    private companion object {
        const val PROGRESS_INTERVAL_MS = 1_000L
    }
}
