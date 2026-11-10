package com.ironlogic.veyra.data.video

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VideoPlaybackRecordTest {
    @Test
    fun storesStableMediaIdentityAndProgress() {
        val record = VideoPlaybackRecord(
            mediaUri = "content://media/external/video/7",
            positionMs = 15_000L,
            durationMs = 90_000L,
            lastPlayedAtEpochMs = 1_700_000_000_000L
        )

        assertEquals("content://media/external/video/7", record.mediaUri)
        assertEquals(15_000L, record.positionMs)
        assertEquals(90_000L, record.durationMs)
        assertFalse(record.isCompleted)
    }

    @Test
    fun completedStateIsExplicit() {
        val record = VideoPlaybackRecord(
            mediaUri = "content://media/external/video/8",
            positionMs = 60_000L,
            durationMs = 60_000L,
            lastPlayedAtEpochMs = 1_700_000_000_000L,
            isCompleted = true
        )

        assertTrue(record.isCompleted)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsBlankMediaIdentity() {
        VideoPlaybackRecord("", 0L, null, 0L)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsNegativePosition() {
        VideoPlaybackRecord("content://media/1", -1L, null, 0L)
    }
}
