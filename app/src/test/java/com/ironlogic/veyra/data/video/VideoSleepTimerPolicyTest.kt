package com.ironlogic.veyra.data.video

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VideoSleepTimerPolicyTest {
    @Test
    fun `provides the supported sleep timer durations`() {
        assertEquals(900_000L, VideoSleepTimerPolicy.durationMs(15))
        assertEquals(1_800_000L, VideoSleepTimerPolicy.durationMs(30))
        assertEquals(2_700_000L, VideoSleepTimerPolicy.durationMs(45))
        assertEquals(3_600_000L, VideoSleepTimerPolicy.durationMs(60))
    }

    @Test
    fun `rejects durations that are not offered by the player`() {
        assertNull(VideoSleepTimerPolicy.durationMs(0))
        assertNull(VideoSleepTimerPolicy.durationMs(10))
        assertNull(VideoSleepTimerPolicy.durationMs(90))
    }

    @Test
    fun `remaining time never becomes negative`() {
        assertEquals(5_000L, VideoSleepTimerPolicy.remainingMs(15_000L, 10_000L))
        assertEquals(0L, VideoSleepTimerPolicy.remainingMs(10_000L, 15_000L))
    }
}
