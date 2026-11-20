package com.ironlogic.veyra.data.video

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VideoResumePolicyTest {
    @Test
    fun `does not resume from the first second`() {
        assertFalse(VideoResumePolicy.shouldResume(999L, 60_000L))
    }

    @Test
    fun `resumes once meaningful progress has been made`() {
        assertTrue(VideoResumePolicy.shouldResume(1_000L, 60_000L))
    }

    @Test
    fun `treats the final five percent as completed for long videos`() {
        assertFalse(VideoResumePolicy.shouldResume(57_000L, 60_000L))
        assertTrue(VideoResumePolicy.shouldResume(56_999L, 60_000L))
    }

    @Test
    fun `uses a one second completion window for short videos`() {
        assertFalse(VideoResumePolicy.shouldResume(9_000L, 10_000L))
        assertTrue(VideoResumePolicy.shouldResume(8_999L, 10_000L))
    }

    @Test
    fun `allows resume when duration is not available`() {
        assertTrue(VideoResumePolicy.shouldResume(5_000L, 0L))
    }

    @Test
    fun `does not resume from a negative position`() {
        assertFalse(VideoResumePolicy.shouldResume(-1L, 60_000L))
    }
}
