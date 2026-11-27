package com.ironlogic.veyra.data.video

/** Normalizes playback history values before they are persisted. */
object VideoPlaybackHistoryPolicy {
    fun normalizePositionMs(positionMs: Long, durationMs: Long): Long {
        val nonNegativePosition = positionMs.coerceAtLeast(0L)
        val nonNegativeDuration = durationMs.coerceAtLeast(0L)
        return if (nonNegativeDuration > 0L) {
            nonNegativePosition.coerceAtMost(nonNegativeDuration)
        } else {
            nonNegativePosition
        }
    }
}
