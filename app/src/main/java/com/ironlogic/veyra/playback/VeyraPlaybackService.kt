package com.ironlogic.veyra.playback

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.ironlogic.veyra.data.video.VideoItem
import com.ironlogic.veyra.data.video.VideoPlaybackHistoryStore
import com.ironlogic.veyra.data.video.VideoPlaybackProgressStore
import com.ironlogic.veyra.data.video.VideoProgressWritePolicy
import com.ironlogic.veyra.data.video.VideoResumePolicy
import com.ironlogic.veyra.data.video.VideoSleepTimerPolicy

/** Owns playback and keeps progress/history current while the UI is not visible. */
class VeyraPlaybackService : MediaSessionService() {
    private var mediaSession: MediaSession? = null
    private lateinit var progressStore: VideoPlaybackProgressStore
    private lateinit var historyStore: VideoPlaybackHistoryStore
    private lateinit var sleepTimerStore: VideoSleepTimerStore

    private val progressHandler = Handler(Looper.getMainLooper())
    private var trackedVideo: VideoItem? = null
    private var trackedPositionMs = 0L
    private var trackedDurationMs = 0L
    private var lastSavedPositionMs = 0L

    private val progressRunnable = object : Runnable {
        override fun run() {
            updateTrackedSnapshot()
            persistTrackedPlayback()
            if (sleepTimerStore.remainingMs() == 0L) {
                mediaSession?.player?.pause()
            }
            progressHandler.postDelayed(this, PROGRESS_INTERVAL_MS)
        }
    }

    override fun onCreate() {
        super.onCreate()
        progressStore = VideoPlaybackProgressStore(this)
        historyStore = VideoPlaybackHistoryStore(this)
        sleepTimerStore = VideoSleepTimerStore(this)

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
            trackedPositionMs = 0L
            trackedDurationMs = 0L
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
            if (trackedPositionMs >= VideoResumePolicy.MINIMUM_RESUME_POSITION_MS) {
                progressStore.save(video, trackedPositionMs, duration)
            }
            historyStore.record(video, trackedPositionMs, duration)
        }
        lastSavedPositionMs = trackedPositionMs
    }

    private companion object {
        const val PROGRESS_INTERVAL_MS = 1_000L
    }
}

/** Persists a sleep deadline so playback can stop even after the player screen is closed. */
class VideoSleepTimerStore(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)

    fun schedule(durationMs: Long) {
        if (durationMs <= 0L) {
            clear()
            return
        }
        preferences.edit()
            .putLong(KEY_ELAPSED_DEADLINE, SystemClock.elapsedRealtime() + durationMs)
            .putLong(KEY_WALL_CLOCK_DEADLINE, System.currentTimeMillis() + durationMs)
            .apply()
    }

    fun remainingMs(): Long? {
        if (!preferences.contains(KEY_ELAPSED_DEADLINE) || !preferences.contains(KEY_WALL_CLOCK_DEADLINE)) {
            clear()
            return null
        }
        val elapsedRemaining = VideoSleepTimerPolicy.remainingMs(
            preferences.getLong(KEY_ELAPSED_DEADLINE, 0L),
            SystemClock.elapsedRealtime(),
        )
        val wallClockRemaining = VideoSleepTimerPolicy.remainingMs(
            preferences.getLong(KEY_WALL_CLOCK_DEADLINE, 0L),
            System.currentTimeMillis(),
        )
        val remaining = minOf(elapsedRemaining, wallClockRemaining)
        if (remaining == 0L) clear()
        return remaining
    }

    fun clear() {
        preferences.edit()
            .remove(KEY_ELAPSED_DEADLINE)
            .remove(KEY_WALL_CLOCK_DEADLINE)
            .apply()
    }

    private companion object {
        const val PREFERENCES = "veyra_sleep_timer"
        const val KEY_ELAPSED_DEADLINE = "elapsed_deadline_ms"
        const val KEY_WALL_CLOCK_DEADLINE = "wall_clock_deadline_ms"
    }
}
