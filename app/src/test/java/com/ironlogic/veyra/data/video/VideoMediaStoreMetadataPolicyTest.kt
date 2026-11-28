package com.ironlogic.veyra.data.video

import org.junit.Assert.assertEquals
import org.junit.Test

class VideoMediaStoreMetadataPolicyTest {
    @Test
    fun `uses the shared fallback for a missing display name`() {
        assertEquals("Untitled video", VideoMediaStoreMetadataPolicy.displayName(null))
    }

    @Test
    fun `uses the shared fallback for a blank display name`() {
        assertEquals("Untitled video", VideoMediaStoreMetadataPolicy.displayName("  "))
    }

    @Test
    fun `preserves a valid display name`() {
        assertEquals("Film.mp4", VideoMediaStoreMetadataPolicy.displayName("Film.mp4"))
    }

    @Test
    fun `normalizes negative metadata values`() {
        assertEquals(0L, VideoMediaStoreMetadataPolicy.nonNegative(-1L))
        assertEquals(42L, VideoMediaStoreMetadataPolicy.nonNegative(42L))
    }
}
