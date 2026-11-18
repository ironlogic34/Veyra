package com.ironlogic.veyra.data.video

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class VideoMetadataCacheTest {
    private fun key(id: String) = VideoMetadataCacheKey(
        contentUri = "content://media/video/$id",
        dateModifiedSeconds = 100L,
        sizeBytes = 2_000L
    )

    @Test
    fun cachesPartialMetadataAndFailedExtractionResults() {
        val cache = VideoMetadataCache(maxEntries = 2)
        val partial = VideoMetadata(
            durationMs = 90_000L,
            width = 1920,
            height = 1080,
            rotationDegrees = null,
            bitrateBitsPerSecond = null,
            mimeType = "video/mp4"
        )

        cache.put(key("partial"), partial)
        cache.put(key("failed"), null)

        assertEquals(partial, cache.get(key("partial"))?.metadata)
        assertNotNull(cache.get(key("failed")))
        assertNull(cache.get(key("failed"))?.metadata)
        assertNull(cache.get(key("missing")))
    }

    @Test
    fun evictsLeastRecentlyUsedEntry() {
        val cache = VideoMetadataCache(maxEntries = 2)
        val first = key("first")
        val second = key("second")
        val third = key("third")

        cache.put(first, null)
        cache.put(second, null)
        cache.get(first)
        cache.put(third, null)

        assertNotNull(cache.get(first))
        assertNull(cache.get(second))
        assertNotNull(cache.get(third))
    }

    @Test
    fun modifiedTimeAndSizeArePartOfCacheIdentity() {
        val original = key("same")
        val changedTime = original.copy(dateModifiedSeconds = 101L)
        val changedSize = original.copy(sizeBytes = 2_001L)
        val cache = VideoMetadataCache()

        cache.put(original, null)

        assertNotNull(cache.get(original))
        assertNull(cache.get(changedTime))
        assertNull(cache.get(changedSize))
    }

    @Test
    fun clearRemovesPositiveAndNegativeEntries() {
        val cache = VideoMetadataCache(maxEntries = 2)
        cache.put(key("metadata"), VideoMetadata(90_000L, 1280, 720, 0, null, "video/mp4"))
        cache.put(key("failure"), null)

        cache.clear()

        assertNull(cache.get(key("metadata")))
        assertNull(cache.get(key("failure")))
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsZeroCapacity() {
        VideoMetadataCache(maxEntries = 0)
    }
}
