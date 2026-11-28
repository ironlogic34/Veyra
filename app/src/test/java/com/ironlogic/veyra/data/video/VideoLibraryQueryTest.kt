package com.ironlogic.veyra.data.video

import org.junit.Assert.assertEquals
import org.junit.Test

class VideoLibraryQueryTest {
    @Test
    fun `search trims input and ignores letter case`() {
        val trailer = video("trailer", "Space Trailer.mp4", added = 2L)
        val episode = video("episode", "space episode.mp4", added = 3L)
        val other = video("other", "Nature.mp4", added = 4L)

        assertEquals(
            listOf(episode, trailer),
            VideoLibraryQuery.apply(
                items = listOf(trailer, other, episode),
                searchQuery = "  SPACE ",
            ),
        )
    }

    @Test
    fun `search matches folder paths case insensitively`() {
        val actionVideo = video("action", "Episode 1.mp4", added = 2L)
            .copy(relativePath = "Movies\\Action\\Season 1\\")
        val natureVideo = video("nature", "Episode 2.mp4", added = 3L)
            .copy(relativePath = "Movies/Nature")

        assertEquals(
            listOf(actionVideo),
            VideoLibraryQuery.apply(
                items = listOf(natureVideo, actionVideo),
                searchQuery = "  action/season 1 ",
            ),
        )
    }

    @Test
    fun `newest and oldest sorting use library added time`() {
        val older = video("older", "Older.mp4", added = 1L)
        val newer = video("newer", "Newer.mp4", added = 9L)

        assertEquals(
            listOf(newer, older),
            VideoLibraryQuery.apply(listOf(older, newer), sortOrder = VideoSortOrder.NEWEST_FIRST),
        )
        assertEquals(
            listOf(older, newer),
            VideoLibraryQuery.apply(listOf(newer, older), sortOrder = VideoSortOrder.OLDEST_FIRST),
        )
    }

    @Test
    fun `name sorting is case insensitive and deterministic`() {
        val beta = video("beta", "beta.mp4", added = 1L)
        val alphaNew = video("alpha-new", "Alpha.mp4", added = 9L)
        val alphaOld = video("alpha-old", "alpha.mp4", added = 2L)

        assertEquals(
            listOf(alphaNew, alphaOld, beta),
            VideoLibraryQuery.apply(
                listOf(beta, alphaOld, alphaNew),
                sortOrder = VideoSortOrder.NAME_A_TO_Z,
            ),
        )
        assertEquals(
            listOf(beta, alphaNew, alphaOld),
            VideoLibraryQuery.apply(
                listOf(alphaOld, beta, alphaNew),
                sortOrder = VideoSortOrder.NAME_Z_TO_A,
            ),
        )
    }

    @Test
    fun `duration and size sorting use the requested direction`() {
        val shortSmall = video("short", "Short.mp4", added = 1L, duration = 10L, size = 100L)
        val longLarge = video("long", "Long.mp4", added = 2L, duration = 90L, size = 900L)
        val medium = video("medium", "Medium.mp4", added = 3L, duration = 50L, size = 500L)
        val items = listOf(medium, longLarge, shortSmall)

        assertEquals(
            listOf(shortSmall, medium, longLarge),
            VideoLibraryQuery.apply(items, sortOrder = VideoSortOrder.DURATION_SHORTEST_FIRST),
        )
        assertEquals(
            listOf(longLarge, medium, shortSmall),
            VideoLibraryQuery.apply(items, sortOrder = VideoSortOrder.DURATION_LONGEST_FIRST),
        )
        assertEquals(
            listOf(shortSmall, medium, longLarge),
            VideoLibraryQuery.apply(items, sortOrder = VideoSortOrder.SIZE_SMALLEST_FIRST),
        )
        assertEquals(
            listOf(longLarge, medium, shortSmall),
            VideoLibraryQuery.apply(items, sortOrder = VideoSortOrder.SIZE_LARGEST_FIRST),
        )
    }

    @Test
    fun `empty search results stay empty`() {
        assertEquals(
            emptyList<VideoItem>(),
            VideoLibraryQuery.apply(
                items = listOf(video("one", "One.mp4", added = 1L)),
                searchQuery = "missing",
            ),
        )
    }

    @Test
    fun `missing and invalid stored sort orders use newest first`() {
        assertEquals(VideoSortOrder.NEWEST_FIRST, parseVideoSortOrder(null))
        assertEquals(VideoSortOrder.NEWEST_FIRST, parseVideoSortOrder("unknown"))
        assertEquals(VideoSortOrder.NAME_A_TO_Z, parseVideoSortOrder("NAME_A_TO_Z"))
    }

    private fun video(
        id: String,
        name: String,
        added: Long,
        duration: Long = 60_000L,
        size: Long = 1_024L,
    ) = VideoItem(
        id = "content://video/$id",
        contentUri = "content://video/$id",
        displayName = name,
        durationMs = duration,
        sizeBytes = size,
        addedEpochSeconds = added,
    )
}
