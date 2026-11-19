package com.ironlogic.veyra.data.video

import org.junit.Assert.assertNotEquals
import org.junit.Test

class VideoThumbnailCacheKeyTest {
    private val original = VideoThumbnailCacheKey(
        contentUri = "content://media/video/42",
        dateModifiedSeconds = 1_700_000_000L,
        sizeBytes = 4_000_000L,
        width = 320,
        height = 180
    )

    @Test
    fun fileVersionChangesInvalidateCacheIdentity() {
        assertNotEquals(original, original.copy(dateModifiedSeconds = 1_700_000_001L))
        assertNotEquals(original, original.copy(sizeBytes = 4_000_001L))
    }

    @Test
    fun requestedDimensionsArePartOfCacheIdentity() {
        assertNotEquals(original, original.copy(width = 640))
        assertNotEquals(original, original.copy(height = 360))
    }

    @Test
    fun contentUriRemainsThePrimaryIdentity() {
        assertNotEquals(original, original.copy(contentUri = "content://media/video/43"))
    }
}
