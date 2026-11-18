package com.ironlogic.veyra.playback

import android.content.Context
import android.net.Uri
import androidx.annotation.MainThread
import androidx.media3.common.Player
import com.ironlogic.veyra.data.video.VideoCompletionAction
import com.ironlogic.veyra.data.video.VideoCompletionMode
import com.ironlogic.veyra.data.video.VideoCompletionPolicy
import com.ironlogic.veyra.data.video.VideoPlaybackHistoryStore
import com.ironlogic.veyra.data.video.VideoPlaybackRecord
import com.ironlogic.veyra.data.video.VideoPlaybackTransitionPolicy
import com.ironlogic.veyra.data.video.VideoResumePolicy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex

/**
 * Coordinates a player session with persisted progress and completion rules.
 *
 * Public methods are main-thread owned. Call [open] before playback, call
 * [saveProgress] when the host pauses, and call [release] when the session ends.
 */
@MainThread
class VideoPlaybackSession(
    context: Context,
    private val history: VideoPlaybackHistoryStore = VideoPlaybackHistoryStore(context),
    private val nowEpochMs: () -> Long = System::currentTimeMillis
) {
    val engine = VideoPlaybackEngine(context)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val historyMutex = Mutex()
    private var queue: List<Uri> = emptyList()
    private var completionMode: VideoCompletionMode = VideoCompletionMode.STOP
    private var completionHandled = false
    private var released = false
    private var pendingProgressSave: Job? = null
    private var pendingCompletion: Job? = null
    private var lastObservedMediaUri: String? = null
    private var lastObservedDurationMs: Long? = null

    private val listener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            if (!isPlaying) persistCurrentProgress()
        }

        override fun onMediaItemTransition(mediaItem: androidx.media3.common.MediaItem?, reason: Int) {
            completionHandled = false
        }

        override fun onPositionDiscontinuity(
            oldPosition: Player.PositionInfo,
            newPosition: Player.PositionInfo,
            reason: Int
        ) {
            if (released || completionHandled || reason != Player.DISCONTINUITY_REASON_SEEK) return
            if (oldPosition.mediaItemIndex == newPosition.mediaItemIndex) return

            val oldUri = queue.getOrNull(oldPosition.mediaItemIndex)?.toString()
            val newUri = queue.getOrNull(newPosition.mediaItemIndex)?.toString()
            val oldDuration = lastObservedDurationMs.takeIf { lastObservedMediaUri == oldUri }
            VideoPlaybackTransitionPolicy.progressForItemChange(
                oldMediaUri = oldUri,
                newMediaUri = newUri,
                oldPositionMs = oldPosition.positionMs,
                oldDurationMs = oldDuration,
                nowEpochMs = nowEpochMs()
            )?.let(::persistProgressRecord)
        }

        override fun onEvents(player: Player, events: Player.Events) {
            lastObservedMediaUri = engine.currentMediaUri()?.toString()
            lastObservedDurationMs = engine.snapshot().durationMs
        }

        override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
            if (!playWhenReady && reason == Player.PLAY_WHEN_READY_CHANGE_REASON_END_OF_MEDIA_ITEM) {
                handleCompletion()
            }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_ENDED) handleCompletion()
        }
    }

    init {
        engine.addListener(listener)
    }

    suspend fun open(
        mediaUri: Uri,
        resume: Boolean = true,
        completionMode: VideoCompletionMode = VideoCompletionMode.STOP
    ) {
        checkActive()
        saveCurrentBeforeSwitch()
        queue = listOf(mediaUri)
        this.completionMode = completionMode
        completionHandled = false
        val record = if (resume) withHistoryLock {
            checkActive()
            history.find(mediaUri.toString())
        } else null
        val position = VideoResumePolicy.resumePosition(record, mediaUri.toString()) ?: 0L
        engine.setRepeatOne(false)
        engine.prepare(mediaUri, position, playWhenReady = true)
    }

    suspend fun openQueue(
        mediaUris: List<Uri>,
        startIndex: Int = 0,
        resume: Boolean = true,
        completionMode: VideoCompletionMode = VideoCompletionMode.PLAY_NEXT,
        displayNames: List<String> = emptyList()
    ) {
        checkActive()
        require(mediaUris.isNotEmpty()) { "Playback queue cannot be empty." }
        require(startIndex in mediaUris.indices) { "Start index is outside the playback queue." }
        saveCurrentBeforeSwitch()
        queue = mediaUris.toList()
        this.completionMode = completionMode
        completionHandled = false
        val selectedUri = queue[startIndex]
        val record = if (resume) withHistoryLock {
            checkActive()
            history.find(selectedUri.toString())
        } else null
        val position = VideoResumePolicy.resumePosition(record, selectedUri.toString()) ?: 0L
        engine.setRepeatOne(false)
        engine.prepareQueue(
            queue,
            startIndex,
            position,
            playWhenReady = true,
            displayNames = displayNames
        )
    }

    fun setCompletionMode(mode: VideoCompletionMode) {
        completionMode = mode
        engine.setRepeatOne(false)
    }

    suspend fun saveProgress() {
        checkActive()
        awaitPendingWrites()
        withHistoryLock {
            checkActive()
            if (!completionHandled) {
                currentRecord()?.takeIf { it.positionMs > 0L }?.let { history.save(it) }
            }
        }
    }

    suspend fun recentHistory(limit: Int = 50): List<VideoPlaybackRecord> {
        checkActive()
        awaitPendingWrites()
        return withHistoryLock {
            checkActive()
            history.recent(limit)
        }
    }

    /** Saves pending completion/progress writes before releasing player and history storage. */
    suspend fun release() {
        if (released) return
        released = true
        awaitPendingWrites()
        withHistoryLock {
            if (!completionHandled) {
                currentRecord()?.takeIf { it.positionMs > 0L }?.let { history.save(it) }
            }
            engine.removeListener(listener)
            engine.close()
            history.close()
        }
        scope.cancel()
    }

    private suspend fun saveCurrentBeforeSwitch() {
        awaitPendingWrites()
        withHistoryLock {
            checkActive()
            if (!completionHandled) {
                currentRecord()?.takeIf { it.positionMs > 0L }?.let { history.save(it) }
            }
        }
    }

    private suspend fun awaitPendingWrites() {
        pendingProgressSave?.join()
        pendingCompletion?.join()
    }

    private suspend fun <T> withHistoryLock(block: suspend () -> T): T {
        historyMutex.lock()
        return try {
            block()
        } finally {
            historyMutex.unlock()
        }
    }

    private fun persistCurrentProgress() {
        if (released || completionHandled) return
        val record = currentRecord() ?: return
        persistProgressRecord(record)
    }

    private fun persistProgressRecord(record: VideoPlaybackRecord) {
        if (released || record.positionMs <= 0L) return
        val previousSave = pendingProgressSave
        pendingProgressSave = scope.launch {
            previousSave?.join()
            withHistoryLock { history.save(record) }
        }
    }

    private fun currentRecord(): VideoPlaybackRecord? {
        val uri = engine.currentMediaUri()?.toString() ?: return null
        val snapshot = engine.snapshot()
        if (snapshot.isEnded) return null
        return VideoResumePolicy.progress(
            mediaUri = uri,
            positionMs = snapshot.currentPositionMs,
            durationMs = snapshot.durationMs,
            nowEpochMs = nowEpochMs()
        )
    }

    private fun handleCompletion() {
        if (released || completionHandled) return
        val index = engine.currentMediaItemIndex()
        val completionQueue = queue.toList()
        val currentUri = completionQueue.getOrNull(index)?.toString() ?: return
        val mode = completionMode
        val durationMs = engine.snapshot().durationMs
        completionHandled = true

        val previousCompletion = pendingCompletion
        val progressSave = pendingProgressSave
        pendingCompletion = scope.launch {
            previousCompletion?.join()
            progressSave?.join()
            withHistoryLock {
                history.save(
                    VideoResumePolicy.completed(currentUri, durationMs, nowEpochMs())
                )
            }
            if (released) return@launch

            when (val action = VideoCompletionPolicy.decide(
                mode,
                completionQueue.map(Uri::toString),
                index
            )) {
                VideoCompletionAction.Stop -> engine.pause()
                is VideoCompletionAction.Replay -> {
                    engine.seekTo(0L)
                    completionHandled = false
                    engine.play()
                }
                is VideoCompletionAction.PlayNext -> {
                    val expectedNext = completionQueue.getOrNull(index + 1)?.toString()
                    if (expectedNext == action.mediaUri) {
                        engine.seekToNextMediaItem()
                        engine.play()
                    } else {
                        engine.pause()
                    }
                }
            }
        }
    }

    private fun checkActive() {
        check(!released) { "Playback session has already been released." }
    }
}
