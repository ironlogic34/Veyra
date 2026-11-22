package com.ironlogic.veyra.data.video

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class VideoThumbnailCacheKeyTest {
    @Test
    fun `same video metadata produces the same cache key`() {
        val video = video(sizeBytes = 100L, added = 10L)

        assertEquals(
            VideoThumbnailCacheKey.forVideo(video),
            VideoThumbnailCacheKey.forVideo(video.copy()),
        )
    }

    @Test
    fun `file size and added time invalidate stale thumbnail keys`() {
        val original = video(sizeBytes = 100L, added = 10L)

        assertNotEquals(
            VideoThumbnailCacheKey.forVideo(original),
            VideoThumbnailCacheKey.forVideo(original.copy(sizeBytes = 200L)),
        )
        assertNotEquals(
            VideoThumbnailCacheKey.forVideo(original),
            VideoThumbnailCacheKey.forVideo(original.copy(addedEpochSeconds = 11L)),
        )
    }

    private fun video(sizeBytes: Long, added: Long) = VideoItem(
        id = "content://video/1",
        contentUri = "content://video/1",
        displayName = "Video",
        durationMs = 60_000L,
        sizeBytes = sizeBytes,
        addedEpochSeconds = added,
    )
}
