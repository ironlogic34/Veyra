package com.ironlogic.veyra.data.video

import org.junit.Assert.assertEquals
import org.junit.Test

class VideoPlaybackProgressStoreTest {

    @Test
    fun `saved progress reconstructs a playable video item`() {
        val progress = VideoProgress(
            contentUri = "content://media/external/video/media/42",
            displayName = "Remembered film.mp4",
            positionMs = 15_000,
            durationMs = 90_000,
        )

        val video = progress.asVideoItem()

        assertEquals(progress.contentUri, video.id)
        assertEquals(progress.contentUri, video.contentUri)
        assertEquals(progress.displayName, video.displayName)
        assertEquals(progress.durationMs, video.durationMs)
    }
    @Test
    fun `progress record keeps the media identity and position`() {
        val progress = VideoProgress(
            contentUri = "content://media/external/video/media/7",
            displayName = "Travel film.mp4",
            positionMs = 12_500,
            durationMs = 120_000,
        )

        assertEquals("content://media/external/video/media/7", progress.contentUri)
        assertEquals("Travel film.mp4", progress.displayName)
        assertEquals(12_500, progress.positionMs)
        assertEquals(120_000, progress.durationMs)
    }
}
