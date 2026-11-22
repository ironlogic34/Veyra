package com.ironlogic.veyra.data.video

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VideoMemoryMarkCodecTest {
    @Test
    fun `round trips local marks with optional text and unicode`() {
        val marks = listOf(
            VideoMemoryMark(
                id = "mark-1",
                contentUri = "content://provider/video|one",
                displayName = "A scene | रात.mp4",
                positionMs = 125_000L,
                note = "The quiet part\nwith a separator |",
                mood = "Beautiful",
                createdAtEpochMs = 1_800_000_000_000L,
            ),
            VideoMemoryMark(
                id = "mark-2",
                contentUri = "content://provider/video-two",
                displayName = "Second.mp4",
                positionMs = 0L,
                note = "",
                mood = null,
                createdAtEpochMs = 1_800_000_000_001L,
            ),
        )

        assertEquals(marks, VideoMemoryMarkCodec.decode(VideoMemoryMarkCodec.encode(marks)))
    }

    @Test
    fun `ignores malformed rows without losing valid marks`() {
        val valid = VideoMemoryMark(
            id = "mark-1",
            contentUri = "content://provider/video",
            displayName = "Film.mp4",
            positionMs = 5_000L,
            note = "",
            mood = null,
            createdAtEpochMs = 1_800_000_000_000L,
        )
        val encoded = VideoMemoryMarkCodec.encode(listOf(valid)) + "\nnot-a-valid-row"

        assertEquals(listOf(valid), VideoMemoryMarkCodec.decode(encoded))
    }

    @Test
    fun `empty storage decodes as an empty list`() {
        assertTrue(VideoMemoryMarkCodec.decode(null).isEmpty())
        assertTrue(VideoMemoryMarkCodec.decode("").isEmpty())
    }

    @Test
    fun `rejects negative timestamps and empty identities`() {
        val invalid = VideoMemoryMark(
            id = "",
            contentUri = "content://provider/video",
            displayName = "Film.mp4",
            positionMs = -1L,
            note = "",
            mood = null,
            createdAtEpochMs = -1L,
        )
        assertTrue(VideoMemoryMarkCodec.decode(VideoMemoryMarkCodec.encode(listOf(invalid))).isEmpty())
    }
}
