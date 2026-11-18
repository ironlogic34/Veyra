package com.ironlogic.veyra.data.video

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VideoPlaybackTransitionPolicyTest {
    @Test
    fun recordsOutgoingItemWhenQueueSelectionChanges() {
        val record = VideoPlaybackTransitionPolicy.progressForItemChange(
            oldMediaUri = "content://media/video/one",
            newMediaUri = "content://media/video/two",
            oldPositionMs = 25_000L,
            oldDurationMs = 90_000L,
            nowEpochMs = 1_700L
        )

        assertEquals("content://media/video/one", record?.mediaUri)
        assertEquals(25_000L, record?.positionMs)
        assertEquals(90_000L, record?.durationMs)
        assertEquals(1_700L, record?.lastPlayedAtEpochMs)
        assertEquals(false, record?.isCompleted)
    }

    @Test
    fun ignoresSeeksWithinTheSameMediaItem() {
        assertNull(
            VideoPlaybackTransitionPolicy.progressForItemChange(
                oldMediaUri = "content://media/video/one",
                newMediaUri = "content://media/video/one",
                oldPositionMs = 25_000L,
                oldDurationMs = 90_000L,
                nowEpochMs = 1_700L
            )
        )
    }

    @Test
    fun ignoresMissingIdentityAndZeroPosition() {
        assertNull(
            VideoPlaybackTransitionPolicy.progressForItemChange(
                oldMediaUri = null,
                newMediaUri = "content://media/video/two",
                oldPositionMs = 25_000L,
                oldDurationMs = null,
                nowEpochMs = 1_700L
            )
        )
        assertNull(
            VideoPlaybackTransitionPolicy.progressForItemChange(
                oldMediaUri = "content://media/video/one",
                newMediaUri = null,
                oldPositionMs = 0L,
                oldDurationMs = 90_000L,
                nowEpochMs = 1_700L
            )
        )
    }
}
