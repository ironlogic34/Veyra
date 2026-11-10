package com.ironlogic.veyra.playback

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch

/**
 * Bridges an existing playback session to an owner's lifecycle.
 *
 * The supplied scope must outlive the lifecycle owner (for example, a
 * ViewModel scope), so final persistence is not cancelled during destruction.
 * Playback is deliberately not paused on backgrounding: Veyra can support
 * background audio/video policy independently from progress persistence.
 */
class VideoPlaybackSessionLifecycle(
    private val lifecycle: Lifecycle,
    private val session: VideoPlaybackSession,
    private val ownerScope: CoroutineScope,
    private val shouldReleaseOnDestroy: () -> Boolean
) : DefaultLifecycleObserver, AutoCloseable {

    private var saveJob: Job? = null
    private var releaseJob: Job? = null
    private var closed = false

    init {
        lifecycle.addObserver(this)
    }

    override fun onPause(owner: LifecycleOwner) {
        if (closed) return
        val previousSave = saveJob
        saveJob = ownerScope.launch {
            previousSave?.join()
            session.saveProgress()
        }
    }

    override fun onDestroy(owner: LifecycleOwner) {
        lifecycle.removeObserver(this)
        if (closed) return
        closed = true

        if (shouldReleaseOnDestroy()) {
            val pendingSave = saveJob
            releaseJob = ownerScope.launch {
                pendingSave?.join()
                session.release()
            }
        }
    }

    /** Waits for a requested final release, if this owner initiated one. */
    suspend fun awaitRelease() {
        releaseJob?.join()
    }

    override fun close() {
        lifecycle.removeObserver(this)
    }
}
