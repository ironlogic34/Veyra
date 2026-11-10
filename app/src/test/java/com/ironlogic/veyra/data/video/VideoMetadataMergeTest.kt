package com.ironlogic.veyra.data.video

import android.net.Uri
import org.junit.Assert.assertEquals
import org.junit.Test

class VideoMetadataMergeTest {
    private fun video(
        durationMs: Long = 0L,
        mimeType: String? = null,
        width: Int? = null,
        height: Int? = null
    ) = VideoItem(
        contentUri = Uri.parse("content://videos/1"),
        displayName = "clip.mp4",
        durationMs = durationMs,
        sizeBytes = 1024L,
        dateModifiedSeconds = 10L,
        mimeType = mimeType,
        relativePath = null,
        width = width,
        height = height
    )

    @Test
    fun fillsMissingFieldsFromExtractedMetadata() {
        val result = video().withMetadata(
            VideoMetadata(
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
        val result = video(
            durationMs = 60000L,
            mimeType = "video/webm",
            width = 1280,
            height = 720
        ).withMetadata(
            VideoMetadata(
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
