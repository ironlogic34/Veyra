package com.ironlogic.veyra.data.video

import org.junit.Assert.assertEquals
import org.junit.Test

class VideoPlaybackHistoryPolicyTest {
    @Test
    fun `clamps a position beyond a known duration`() {
        assertEquals(
            60_000L,
            VideoPlaybackHistoryPolicy.normalizePositionMs(60_500L, 60_000L),
        )
    }

    @Test
    fun `keeps a valid position within a known duration`() {
        assertEquals(
            45_000L,
            VideoPlaybackHistoryPolicy.normalizePositionMs(45_000L, 60_000L),
        )
    }

    @Test
    fun `normalizes negative positions and durations`() {
        assertEquals(0L, VideoPlaybackHistoryPolicy.normalizePositionMs(-1L, 60_000L))
        assertEquals(5_000L, VideoPlaybackHistoryPolicy.normalizePositionMs(5_000L, -1L))
    }

    @Test
    fun `keeps positive progress when duration is unknown`() {
        assertEquals(
            5_000L,
            VideoPlaybackHistoryPolicy.normalizePositionMs(5_000L, 0L),
        )
    }
}
