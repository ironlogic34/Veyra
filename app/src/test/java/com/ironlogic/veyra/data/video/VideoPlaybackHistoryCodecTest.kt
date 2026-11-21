package com.ironlogic.veyra.data.video

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VideoPlaybackHistoryCodecTest {
    @Test
    fun `round trips names and URIs containing separators and unicode`() {
        val entries = listOf(
            VideoHistoryEntry(
                contentUri = "content://provider/tree/a%2Fb/document/c%7Cd",
                displayName = "Film | रात.mp4",
                positionMs = 12_500L,
                durationMs = 90_000L,
                lastPlayedEpochMs = 1_800_000_000_000L,
            ),
        )

        assertEquals(entries, VideoPlaybackHistoryCodec.decode(VideoPlaybackHistoryCodec.encode(entries)))
    }

    @Test
    fun `ignores malformed rows without losing valid history entries`() {
        val valid = VideoHistoryEntry(
            contentUri = "content://media/1",
            displayName = "Film.mp4",
            positionMs = 5_000L,
            durationMs = 60_000L,
            lastPlayedEpochMs = 1_800_000_000_000L,
        )
        val encoded = VideoPlaybackHistoryCodec.encode(listOf(valid)) + "\nnot-a-valid-row"

        assertEquals(listOf(valid), VideoPlaybackHistoryCodec.decode(encoded))
    }

    @Test
    fun `empty history decodes as an empty list`() {
        assertTrue(VideoPlaybackHistoryCodec.decode(null).isEmpty())
        assertTrue(VideoPlaybackHistoryCodec.decode("").isEmpty())
    }
}
