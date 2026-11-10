package com.ironlogic.veyra.data.video

import org.junit.Assert.assertEquals
import org.junit.Test

class VideoLibraryQueryTest {
    private data class Entry(
        val name: String,
        val path: String?,
        val duration: Long,
        val size: Long,
        val modified: Long,
        val mime: String?
    )

    private val alpha = Entry("Alpha.mp4", "Movies/", 60_000, 100, 10, "video/mp4")
    private val beta = Entry("beta.mkv", "Series/Drama/", 120_000, 300, 30, "video/x-matroska")
    private val gamma = Entry("Gamma.mp4", "Clips/", 30_000, 200, 20, "video/mp4")

    private fun query(
        items: List<Entry> = listOf(alpha, beta, gamma),
        search: String = "",
        order: VideoSortOrder = VideoSortOrder.NEWEST_FIRST,
        mimeTypes: Set<String>? = null
    ) = VideoLibraryQuery.apply(
        items = items,
        searchQuery = search,
        sortOrder = order,
        allowedMimeTypes = mimeTypes,
        displayName = Entry::name,
        relativePath = Entry::path,
        durationMs = Entry::duration,
        sizeBytes = Entry::size,
        modifiedSeconds = Entry::modified,
        mimeType = Entry::mime
    )

    @Test
    fun defaultsToNewestFirstWithNameTieBreak() {
        assertEquals(listOf(beta, gamma, alpha), query())
    }

    @Test
    fun trimsAndIgnoresSearchCase() {
        assertEquals(listOf(beta), query(search = "  BETA.MKV "))
    }

    @Test
    fun searchesRelativeFolderPath() {
        assertEquals(listOf(beta), query(search = "drama"))
    }

    @Test
    fun emptySearchReturnsAllItems() {
        assertEquals(3, query(search = "  ").size)
    }

    @Test
    fun filtersByAllowedMimeTypesCaseInsensitively() {
        assertEquals(
            listOf(gamma, alpha),
            query(mimeTypes = setOf("VIDEO/MP4"))
        )
    }

    @Test
    fun supportsOldestFirstAndNameDescending() {
        assertEquals(
            listOf(alpha, gamma, beta),
            query(order = VideoSortOrder.OLDEST_FIRST)
        )
        assertEquals(
            listOf(gamma, beta, alpha),
            query(order = VideoSortOrder.NAME_Z_TO_A)
        )
    }

    @Test
    fun sortsByDurationAndSizeInBothDirections() {
        assertEquals(
            listOf(gamma, alpha, beta),
            query(order = VideoSortOrder.DURATION_SHORTEST_FIRST)
        )
        assertEquals(
            listOf(beta, alpha, gamma),
            query(order = VideoSortOrder.DURATION_LONGEST_FIRST)
        )
        assertEquals(
            listOf(alpha, gamma, beta),
            query(order = VideoSortOrder.SIZE_SMALLEST_FIRST)
        )
        assertEquals(
            listOf(beta, gamma, alpha),
            query(order = VideoSortOrder.SIZE_LARGEST_FIRST)
        )
    }

    @Test
    fun doesNotMutateTheInputList() {
        val input = listOf(alpha, beta, gamma)
        query(items = input, order = VideoSortOrder.NAME_A_TO_Z)
        assertEquals(listOf(alpha, beta, gamma), input)
    }
}
