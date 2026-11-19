package com.ironlogic.veyra.data.video

/** Converts a slider preview into a valid player position when the gesture commits. */
internal object VideoSeekPolicy {
    fun clampSliderPosition(value: Float, durationMs: Long): Long {
        if (!value.isFinite()) return 0L
        return value.toLong().coerceIn(0L, durationMs.coerceAtLeast(0L))
    }
}
