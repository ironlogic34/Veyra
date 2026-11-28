package com.ironlogic.veyra.data.video

import org.junit.Assert.assertEquals
import org.junit.Test

class VideoOrientationModeTest {
    @Test
    fun `system default is used for missing or unknown settings`() {
        assertEquals(VideoOrientationMode.SYSTEM, VideoOrientationMode.fromStorage(null))
        assertEquals(VideoOrientationMode.SYSTEM, VideoOrientationMode.fromStorage("diagonal"))
    }

    @Test
    fun `stored orientation values restore the selected preference`() {
        assertEquals(VideoOrientationMode.PORTRAIT, VideoOrientationMode.fromStorage("portrait"))
        assertEquals(VideoOrientationMode.LANDSCAPE, VideoOrientationMode.fromStorage("landscape"))
    }
}
