package com.ironlogic.veyra.data.video

object VideoAutoResumePolicy {
    fun startPositionMs(
        autoResumeEnabled: Boolean,
        manualPositionMs: Long?,
        savedPositionMs: Long?,
    ): Long = (manualPositionMs ?: if (autoResumeEnabled) savedPositionMs else null)
        ?.coerceAtLeast(0L)
        ?: 0L
}
