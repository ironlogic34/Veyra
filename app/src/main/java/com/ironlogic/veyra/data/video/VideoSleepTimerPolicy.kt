package com.ironlogic.veyra.data.video

object VideoSleepTimerPolicy {
    val PRESET_MINUTES = listOf(15, 30, 45, 60)

    fun durationMs(minutes: Int): Long? =
        minutes.takeIf { it in PRESET_MINUTES }?.times(60_000L)

    fun remainingMs(deadlineElapsedMs: Long, nowElapsedMs: Long): Long =
        (deadlineElapsedMs - nowElapsedMs).coerceAtLeast(0L)
}
