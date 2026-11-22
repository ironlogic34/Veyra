package com.ironlogic.veyra.data.video

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VideoPlaybackLockPolicyTest {
    @Test
    fun `ordinary taps do not unlock playback controls`() {
        assertFalse(VideoPlaybackLockPolicy.shouldUnlock(VideoLockGesture.TAP))
    }

    @Test
    fun `long press unlocks playback controls`() {
        assertTrue(VideoPlaybackLockPolicy.shouldUnlock(VideoLockGesture.LONG_PRESS))
    }
}
