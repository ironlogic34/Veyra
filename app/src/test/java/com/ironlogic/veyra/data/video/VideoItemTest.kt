package com.ironlogic.veyra.data.video

import org.junit.Assert.assertEquals
import org.junit.Test

class VideoItemTest {
    @Test
    fun `video item keeps content uri as its stable playback identity`() {
        val item = VideoItem(
            id = "42",
            contentUri = "content://media/external/video/media/42",
            displayName = "A quiet evening.mp4",
            durationMs = 90_000,
            sizeBytes = 1_024,
            addedEpochSeconds = 1_700_000_000,
        )

        assertEquals("content://media/external/video/media/42", item.contentUri)
        assertEquals("A quiet evening.mp4", item.displayName)
    }
}
