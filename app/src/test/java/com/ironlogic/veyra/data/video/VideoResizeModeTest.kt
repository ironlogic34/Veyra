package com.ironlogic.veyra.data.video

import org.junit.Assert.assertEquals
import org.junit.Test

class VideoResizeModeTest {
    @Test
    fun `fit and crop cycle without a stretching mode`() {
        assertEquals(VideoResizeMode.CROP, VideoResizeMode.FIT.next())
        assertEquals(VideoResizeMode.FIT, VideoResizeMode.CROP.next())
    }

    @Test
    fun `unknown stored values fall back to fit`() {
        assertEquals(VideoResizeMode.FIT, VideoResizeMode.fromStorage(null))
        assertEquals(VideoResizeMode.FIT, VideoResizeMode.fromStorage("stretch"))
    }

    @Test
    fun `stored values restore their matching mode`() {
        assertEquals(VideoResizeMode.CROP, VideoResizeMode.fromStorage("crop"))
    }
}
