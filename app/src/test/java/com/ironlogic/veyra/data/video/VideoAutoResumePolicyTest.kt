package com.ironlogic.veyra.data.video

import org.junit.Assert.assertEquals
import org.junit.Test

class VideoAutoResumePolicyTest {
    @Test
    fun `uses saved position when automatic resume is enabled`() {
        assertEquals(12_000L, VideoAutoResumePolicy.startPositionMs(true, null, 12_000L))
    }

    @Test
    fun `starts at the beginning when automatic resume is disabled`() {
        assertEquals(0L, VideoAutoResumePolicy.startPositionMs(false, null, 12_000L))
    }

    @Test
    fun `manual Continue Watching position overrides the setting`() {
        assertEquals(12_000L, VideoAutoResumePolicy.startPositionMs(false, 12_000L, 30_000L))
    }

    @Test
    fun `missing and negative positions safely start at zero`() {
        assertEquals(0L, VideoAutoResumePolicy.startPositionMs(true, null, null))
        assertEquals(0L, VideoAutoResumePolicy.startPositionMs(true, null, -5L))
        assertEquals(0L, VideoAutoResumePolicy.startPositionMs(true, -5L, 10_000L))
    }
}
