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
    fun `rejects negative and out of range history values without losing valid rows`() {
        val valid = VideoHistoryEntry(
            contentUri = "content://media/valid",
            displayName = "Valid.mp4",
            positionMs = 5_000L,
            durationMs = 60_000L,
            lastPlayedEpochMs = 1_800_000_000_000L,
        )
        val invalid = listOf(
            valid.copy(contentUri = "content://media/negative-position", positionMs = -1L),
            valid.copy(contentUri = "content://media/negative-duration", durationMs = -1L),
            valid.copy(contentUri = "content://media/negative-time", lastPlayedEpochMs = -1L),
            valid.copy(contentUri = "content://media/past-duration", positionMs = 60_001L),
        )

        assertEquals(
            listOf(valid),
            VideoPlaybackHistoryCodec.decode(VideoPlaybackHistoryCodec.encode(listOf(valid) + invalid)),
        )
    }

    @Test
    fun `allows progress when duration is not yet known`() {
        val entry = VideoHistoryEntry(
            contentUri = "content://media/unknown-duration",
            displayName = "Unknown duration.mp4",
            positionMs = 5_000L,
            durationMs = 0L,
            lastPlayedEpochMs = 1_800_000_000_000L,
        )

        assertEquals(listOf(entry), VideoPlaybackHistoryCodec.decode(VideoPlaybackHistoryCodec.encode(listOf(entry))))
    }

    @Test
    fun `empty history decodes as an empty list`() {
        assertTrue(VideoPlaybackHistoryCodec.decode(null).isEmpty())
        assertTrue(VideoPlaybackHistoryCodec.decode("").isEmpty())
    }
}
