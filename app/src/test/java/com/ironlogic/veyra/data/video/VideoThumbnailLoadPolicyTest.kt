package com.ironlogic.veyra.data.video

import java.io.IOException
import kotlinx.coroutines.CancellationException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertThrows
import org.junit.Test

class VideoThumbnailLoadPolicyTest {
    @Test
    fun `optional extraction failures remain non-fatal`() {
        val result = VideoThumbnailLoadPolicy.runCatchingWithoutCancellation<Int> {
            throw IOException("thumbnail provider unavailable")
        }

        assertTrue(result.isFailure)
    }

    @Test
    fun `successful extraction values are preserved`() {
        assertEquals(
            320,
            VideoThumbnailLoadPolicy.runCatchingWithoutCancellation { 320 }.getOrNull(),
        )
    }

    @Test
    fun `coroutine cancellation is rethrown`() {
        assertThrows(CancellationException::class.java) {
            VideoThumbnailLoadPolicy.runCatchingWithoutCancellation<Int> {
                throw CancellationException("thumbnail load cancelled")
            }
        }
    }
}
