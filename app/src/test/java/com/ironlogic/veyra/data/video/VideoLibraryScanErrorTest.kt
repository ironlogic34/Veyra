package com.ironlogic.veyra.data.video

import org.junit.Assert.assertEquals
import org.junit.Test

class VideoLibraryScanErrorTest {
    @Test
    fun `security failures explain that video access may have changed`() {
        assertEquals(
            "Video access changed. Check your video permissions and try again.",
            VideoLibraryScanError.messageFor(SecurityException("permission revoked")),
        )
    }

    @Test
    fun `other scan failures provide a safe retry message`() {
        assertEquals(
            "Veyra couldn't load your local videos. Try scanning again.",
            VideoLibraryScanError.messageFor(IllegalStateException("provider unavailable")),
        )
    }
}
