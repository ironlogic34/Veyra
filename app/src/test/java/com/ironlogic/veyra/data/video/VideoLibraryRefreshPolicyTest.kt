package com.ironlogic.veyra.data.video

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VideoLibraryRefreshPolicyTest {
    @Test
    fun `replaces cached videos after a successful local scan`() {
        assertTrue(VideoLibraryRefreshPolicy.shouldReplaceCachedVideos(true, false, false))
    }

    @Test
    fun `keeps folder results when local scan fails`() {
        assertTrue(VideoLibraryRefreshPolicy.shouldReplaceCachedVideos(false, true, false))
    }

    @Test
    fun `replaces stale cached results when access changes during a scan`() {
        assertTrue(VideoLibraryRefreshPolicy.shouldReplaceCachedVideos(false, false, true))
    }

    @Test
    fun `preserves the previous library after an unrelated transient scan failure`() {
        assertFalse(VideoLibraryRefreshPolicy.shouldReplaceCachedVideos(false, false, false))
    }
}
