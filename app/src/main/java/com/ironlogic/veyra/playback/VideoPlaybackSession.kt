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
import com.ironlogic.veyra.data.video.VideoResumePolicy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

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
    private var queue: List<Uri> = emptyList()
    private var completionMode: VideoCompletionMode = VideoCompletionMode.STOP
    private var completionHandled = false
    private var released = false
    private var pendingProgressSave: Job? = null

    private val listener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            if (!isPlaying) persistCurrentProgress()
        }

        override fun onMediaItemTransition(mediaItem: androidx.media3.common.MediaItem?, reason: Int) {
            completionHandled = false
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
        val record = if (resume) history.find(mediaUri.toString()) else null
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
        val record = if (resume) history.find(selectedUri.toString()) else null
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
        pendingProgressSave?.join()
        currentRecord()?.let { history.save(it) }
    }

    /** Saves the final position before releasing both player and database. */
    suspend fun release() {
        if (released) return
        pendingProgressSave?.join()
        currentRecord()?.let { history.save(it) }
        released = true
        engine.removeListener(listener)
        engine.close()
        history.close()
        scope.cancel()
    }

    private suspend fun saveCurrentBeforeSwitch() {
        pendingProgressSave?.join()
        currentRecord()?.let { history.save(it) }
    }

    private fun persistCurrentProgress() {
        if (released) return
        val record = currentRecord() ?: return
        if (record.positionMs <= 0L) return
        val previousSave = pendingProgressSave
        pendingProgressSave = scope.launch {
            previousSave?.join()
            history.save(record)
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
        completionHandled = true
        val index = engine.currentMediaItemIndex()
        val currentUri = queue.getOrNull(index)?.toString() ?: return
        scope.launch {
            val snapshot = engine.snapshot()
            history.save(
                VideoResumePolicy.completed(currentUri, snapshot.durationMs, nowEpochMs())
            )
            when (val action = VideoCompletionPolicy.decide(
                completionMode,
                queue.map(Uri::toString),
                index
            )) {
                VideoCompletionAction.Stop -> engine.pause()
                is VideoCompletionAction.Replay -> {
                    engine.seekTo(0L)
                    engine.play()
                }
                is VideoCompletionAction.PlayNext -> {
                    val expectedNext = queue.getOrNull(index + 1)?.toString()
                    if (expectedNext == action.mediaUri) {
                        engine.seekToNextMediaItem()
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
