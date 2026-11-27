package com.ironlogic.veyra.data.video

/** Coalesces refresh requests so changes during a scan trigger one follow-up scan. */
class VideoLibraryRefreshGate {
    private var running = false
    private var pending = false

    @Synchronized
    fun request(): Boolean {
        if (running) {
            pending = true
            return false
        }
        running = true
        return true
    }

    /** Returns true when a queued request should be processed by the active worker. */
    @Synchronized
    fun finish(): Boolean {
        if (pending) {
            pending = false
            return true
        }
        running = false
        return false
    }

    @Synchronized
    fun cancel() {
        pending = false
        running = false
    }
}
