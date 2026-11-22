package com.ironlogic.veyra.data.video

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VideoSubtitleTypePolicyTest {
    @Test
    fun `recognizes SubRip subtitles regardless of extension case`() {
        assertEquals(
            "application/x-subrip",
            VideoSubtitleTypePolicy.mimeTypeFor("episode.SRT"),
        )
    }

    @Test
    fun `recognizes WebVTT subtitles`() {
        assertEquals(
            "text/vtt",
            VideoSubtitleTypePolicy.mimeTypeFor("captions.vtt"),
        )
    }

    @Test
    fun `rejects unsupported subtitle formats and files without extensions`() {
        assertNull(VideoSubtitleTypePolicy.mimeTypeFor("captions.ass"))
        assertNull(VideoSubtitleTypePolicy.mimeTypeFor("captions"))
        assertNull(VideoSubtitleTypePolicy.mimeTypeFor("notes.txt"))
    }
}
