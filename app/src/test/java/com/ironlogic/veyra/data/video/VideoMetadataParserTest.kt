package com.ironlogic.veyra.data.video

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VideoMetadataParserTest {
    @Test
    fun parsesValidNonNegativeLongValues() {
        assertEquals(125000L, VideoMetadataParser.parseLong("125000"))
    }

    @Test
    fun rejectsMissingMalformedAndNegativeLongValues() {
        assertNull(VideoMetadataParser.parseLong(null))
        assertNull(VideoMetadataParser.parseLong("unknown"))
        assertNull(VideoMetadataParser.parseLong("-1"))
    }

    @Test
    fun parsesValidNonNegativeIntegerValues() {
        assertEquals(1920, VideoMetadataParser.parseInt("1920"))
    }

    @Test
    fun rejectsMissingMalformedAndNegativeIntegerValues() {
        assertNull(VideoMetadataParser.parseInt(null))
        assertNull(VideoMetadataParser.parseInt("not-a-number"))
        assertNull(VideoMetadataParser.parseInt("-1080"))
    }
}
