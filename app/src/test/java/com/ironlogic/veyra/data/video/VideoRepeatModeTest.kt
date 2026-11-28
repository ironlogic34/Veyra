package com.ironlogic.veyra.data.video

import org.junit.Assert.assertEquals
import org.junit.Test

class VideoRepeatModeTest {
    @Test
    fun `repeat mode toggles between off and one`() {
        assertEquals(VideoRepeatMode.ONE, VideoRepeatMode.OFF.next())
        assertEquals(VideoRepeatMode.OFF, VideoRepeatMode.ONE.next())
    }

    @Test
    fun `unknown stored values safely default to off`() {
        assertEquals(VideoRepeatMode.OFF, VideoRepeatMode.fromStorage(null))
        assertEquals(VideoRepeatMode.OFF, VideoRepeatMode.fromStorage("all"))
    }

    @Test
    fun `stored values restore the selected repeat mode`() {
        assertEquals(VideoRepeatMode.OFF, VideoRepeatMode.fromStorage("off"))
        assertEquals(VideoRepeatMode.ONE, VideoRepeatMode.fromStorage("one"))
    }
}
