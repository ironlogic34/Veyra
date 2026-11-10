package com.ironlogic.veyra.data.video

import org.junit.Assert.assertEquals
import org.junit.Test

class VideoMetadataMergeTest {
    @Test
    fun fillsMissingFieldsFromExtractedMetadata() {
        val result = mergeVideoMetadata(
            indexedDurationMs = 0L,
            indexedWidth = null,
            indexedHeight = null,
            indexedRotationDegrees = null,
            indexedBitrateBitsPerSecond = null,
            indexedMimeType = null,
            extracted = VideoMetadata(
                durationMs = 120000L,
                width = 1920,
                height = 1080,
                rotationDegrees = 0,
                bitrateBitsPerSecond = 8000000L,
                mimeType = "video/mp4"
            )
        )

        assertEquals(120000L, result.durationMs)
        assertEquals(1920, result.width)
        assertEquals(1080, result.height)
        assertEquals(0, result.rotationDegrees)
        assertEquals(8000000L, result.bitrateBitsPerSecond)
        assertEquals("video/mp4", result.mimeType)
    }

    @Test
    fun preservesExistingIndexedValues() {
        val result = mergeVideoMetadata(
            indexedDurationMs = 60000L,
            indexedWidth = 1280,
            indexedHeight = 720,
            indexedRotationDegrees = null,
            indexedBitrateBitsPerSecond = null,
            indexedMimeType = "video/webm",
            extracted = VideoMetadata(
                durationMs = 120000L,
                width = 1920,
                height = 1080,
                rotationDegrees = 90,
                bitrateBitsPerSecond = 4000000L,
                mimeType = "video/mp4"
            )
        )

        assertEquals(60000L, result.durationMs)
        assertEquals("video/webm", result.mimeType)
        assertEquals(1280, result.width)
        assertEquals(720, result.height)
        assertEquals(90, result.rotationDegrees)
        assertEquals(4000000L, result.bitrateBitsPerSecond)
    }
}
