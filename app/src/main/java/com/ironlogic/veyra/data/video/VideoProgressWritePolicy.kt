package com.ironlogic.veyra.data.video

import kotlin.math.abs

object VideoProgressWritePolicy {
    const val MINIMUM_POSITION_DELTA_MS = 5_000L

    fun shouldPersist(currentPositionMs: Long, lastSavedPositionMs: Long): Boolean {
        if (currentPositionMs < 0L || lastSavedPositionMs < 0L) return false
        return abs(currentPositionMs - lastSavedPositionMs) >= MINIMUM_POSITION_DELTA_MS
    }
}
