package com.ironlogic.veyra.data.video

object VideoResumePolicy {
    const val MINIMUM_RESUME_POSITION_MS = 1_000L
    const val MINIMUM_COMPLETION_WINDOW_MS = 1_000L
    const val MAXIMUM_COMPLETION_WINDOW_MS = 5_000L

    fun shouldResume(positionMs: Long, durationMs: Long): Boolean {
        if (positionMs < MINIMUM_RESUME_POSITION_MS) return false
        if (durationMs <= 0L) return true

        val completionWindowMs = (durationMs / 20L).coerceIn(
            MINIMUM_COMPLETION_WINDOW_MS,
            MAXIMUM_COMPLETION_WINDOW_MS,
        )
        val completionBoundaryMs = (durationMs - completionWindowMs).coerceAtLeast(0L)
        return positionMs < completionBoundaryMs
    }
}
