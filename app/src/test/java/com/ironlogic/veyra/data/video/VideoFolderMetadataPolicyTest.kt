package com.ironlogic.veyra.data.video

import java.io.IOException
import kotlinx.coroutines.CancellationException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class VideoFolderMetadataPolicyTest {
    @Test
    fun `unavailable optional metadata falls back to zero duration`() {
        assertEquals(
            0L,
            VideoFolderMetadataPolicy.readDuration { throw IOException("metadata unavailable") },
        )
    }

    @Test
    fun `negative metadata duration is normalized to zero`() {
        assertEquals(0L, VideoFolderMetadataPolicy.readDuration { -1L })
    }

    @Test
    fun `permission failures propagate so the root scan can mark access lost`() {
        assertThrows(SecurityException::class.java) {
            VideoFolderMetadataPolicy.readDuration { throw SecurityException("permission revoked") }
        }
    }

    @Test
    fun `cancellation propagates instead of becoming a missing duration`() {
        assertThrows(CancellationException::class.java) {
            VideoFolderMetadataPolicy.readDuration { throw CancellationException("scan cancelled") }
        }
    }
}
