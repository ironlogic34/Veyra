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
    fun `file size added time and modified time invalidate stale thumbnail keys`() {
        val original = video(sizeBytes = 100L, added = 10L)

        assertNotEquals(
            VideoThumbnailCacheKey.forVideo(original),
            VideoThumbnailCacheKey.forVideo(original.copy(sizeBytes = 200L)),
        )
        assertNotEquals(
            VideoThumbnailCacheKey.forVideo(original),
            VideoThumbnailCacheKey.forVideo(original.copy(addedEpochSeconds = 11L)),
        )
        assertNotEquals(
            VideoThumbnailCacheKey.forVideo(original),
            VideoThumbnailCacheKey.forVideo(original.copy(modifiedEpochSeconds = 12L)),
        )
    }

    private fun video(sizeBytes: Long, added: Long, modified: Long = 0L) = VideoItem(
        id = "content://video/1",
        contentUri = "content://video/1",
        displayName = "Video",
        durationMs = 60_000L,
        sizeBytes = sizeBytes,
        addedEpochSeconds = added,
        modifiedEpochSeconds = modified,
    )
}
