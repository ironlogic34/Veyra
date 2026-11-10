package com.ironlogic.veyra.data.video

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VideoResumePolicyTest {
    private val record = VideoPlaybackRecord(
        mediaUri = "content://media/external/video/42",
        positionMs = 45_000L,
        durationMs = 120_000L,
        lastPlayedAtEpochMs = 1_000L
    )

    @Test
    fun resumesMatchingIncompleteVideo() {
        assertEquals(
            45_000L,
            VideoResumePolicy.resumePosition(record, record.mediaUri)
        )
    }

    @Test
    fun rejectsMissingOrMismatchedHistory() {
        assertNull(VideoResumePolicy.resumePosition(null, record.mediaUri))
        assertNull(VideoResumePolicy.resumePosition(record, "content://media/other"))
    }

    @Test
    fun rejectsCompletedOrInvalidEndPositions() {
        assertNull(
            VideoResumePolicy.resumePosition(record.copy(isCompleted = true), record.mediaUri)
        )
        assertNull(
            VideoResumePolicy.resumePosition(
                record.copy(positionMs = 120_000L),
                record.mediaUri
            )
        )
        assertNull(
            VideoResumePolicy.resumePosition(
                record.copy(positionMs = 130_000L),
                record.mediaUri
            )
        )
    }

    @Test
    fun unknownDurationStillAllowsPositiveResumePosition() {
        assertEquals(
            45_000L,
            VideoResumePolicy.resumePosition(
                record.copy(durationMs = null),
                record.mediaUri
            )
        )
    }

    @Test
    fun progressNormalizesInvalidOptionalDurationAndNegativePosition() {
        val result = VideoResumePolicy.progress(
            mediaUri = record.mediaUri,
            positionMs = -10L,
            durationMs = 0L,
            nowEpochMs = 2_000L
        )

        assertEquals(0L, result.positionMs)
        assertNull(result.durationMs)
        assertEquals(2_000L, result.lastPlayedAtEpochMs)
        assertEquals(false, result.isCompleted)
    }

    @Test
    fun completionCreatesNonResumableRecord() {
        val result = VideoResumePolicy.completed(
            mediaUri = record.mediaUri,
            durationMs = 120_000L,
            nowEpochMs = 3_000L
        )

        assertEquals(true, result.isCompleted)
        assertNull(VideoResumePolicy.resumePosition(result, record.mediaUri))
    }
}
