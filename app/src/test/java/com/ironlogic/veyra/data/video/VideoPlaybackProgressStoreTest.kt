package com.ironlogic.veyra.data.video

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VideoPlaybackProgressStoreTest {

    @Test
    fun `saved progress reconstructs a playable video item`() {
        val progress = VideoProgress(
            contentUri = "content://media/external/video/media/42",
            displayName = "Remembered film.mp4",
            positionMs = 15_000,
            durationMs = 90_000,
        )

        val video = progress.asVideoItem()

        assertEquals(progress.contentUri, video.id)
        assertEquals(progress.contentUri, video.contentUri)
        assertEquals(progress.displayName, video.displayName)
        assertEquals(progress.durationMs, video.durationMs)
    }

    @Test
    fun `progress record keeps the media identity and position`() {
        val progress = VideoProgress(
            contentUri = "content://media/external/video/media/7",
            displayName = "Travel film.mp4",
            positionMs = 12_500,
            durationMs = 120_000,
        )

        assertEquals("content://media/external/video/media/7", progress.contentUri)
        assertEquals("Travel film.mp4", progress.displayName)
        assertEquals(12_500, progress.positionMs)
        assertEquals(120_000, progress.durationMs)
    }

    @Test
    fun `progress codec round trips URI and names containing separators`() {
        val progress = VideoProgress(
            contentUri = "content://media/video?id=7&folder=Movies/Drama",
            displayName = "A film\twith\na title.mp4",
            positionMs = 12_500L,
            durationMs = 120_000L,
            lastUpdatedEpochMs = 1_800_000_000_000L,
        )

        assertEquals(progress, VideoPlaybackProgressCodec.decode(VideoPlaybackProgressCodec.encode(progress)))
    }

    @Test
    fun `progress codec rejects malformed and invalid records`() {
        assertNull(VideoPlaybackProgressCodec.decode(null))
        assertNull(VideoPlaybackProgressCodec.decode("not-a-progress-record"))
        assertNull(
            VideoPlaybackProgressCodec.decode(
                VideoPlaybackProgressCodec.encode(
                    VideoProgress(
                        contentUri = "",
                        displayName = "Invalid",
                        positionMs = 1_000L,
                        durationMs = 60_000L,
                    ),
                ),
            ),
        )
        assertNull(
            VideoPlaybackProgressCodec.decode(
                VideoPlaybackProgressCodec.encode(
                    VideoProgress(
                        contentUri = "content://video/1",
                        displayName = "Invalid",
                        positionMs = 70_000L,
                        durationMs = 60_000L,
                    ),
                ),
            ),
        )
    }

    @Test
    fun `most recent progress ignores completed entries`() {
        val older = VideoProgress(
            contentUri = "content://video/older",
            displayName = "Older.mp4",
            positionMs = 20_000L,
            durationMs = 100_000L,
            lastUpdatedEpochMs = 100L,
        )
        val newer = VideoProgress(
            contentUri = "content://video/newer",
            displayName = "Newer.mp4",
            positionMs = 30_000L,
            durationMs = 100_000L,
            lastUpdatedEpochMs = 200L,
        )
        val completed = VideoProgress(
            contentUri = "content://video/completed",
            displayName = "Completed.mp4",
            positionMs = 100_000L,
            durationMs = 100_000L,
            lastUpdatedEpochMs = 300L,
        )

        assertEquals(newer, VideoPlaybackProgressPolicy.mostRecent(listOf(older, newer, completed)))
    }

    @Test
    fun `most recent progress uses a stable URI tie breaker`() {
        val first = VideoProgress(
            contentUri = "content://video/a",
            displayName = "A.mp4",
            positionMs = 5_000L,
            durationMs = 100_000L,
            lastUpdatedEpochMs = 200L,
        )
        val second = VideoProgress(
            contentUri = "content://video/b",
            displayName = "B.mp4",
            positionMs = 5_000L,
            durationMs = 100_000L,
            lastUpdatedEpochMs = 200L,
        )

        assertEquals(second, VideoPlaybackProgressPolicy.mostRecent(listOf(first, second)))
    }
}
