package com.ironlogic.veyra.data.video

import org.junit.Assert.assertEquals
import org.junit.Test

class VideoRelatedItemsTest {
    private data class Entry(
        val uri: String,
        val name: String,
        val path: String?,
        val mime: String?,
        val modified: Long
    )

    private val current = Entry(
        uri = "content://media/current",
        name = "Current.mp4",
        path = "Movies/Action/",
        mime = "video/mp4",
        modified = 100
    )

    private val sameFolderAndMime = Entry(
        uri = "content://media/1",
        name = "A.mp4",
        path = "movies/action",
        mime = "VIDEO/MP4",
        modified = 10
    )
    private val sameFolder = Entry(
        uri = "content://media/2",
        name = "B.mkv",
        path = "Movies/Action/",
        mime = "video/x-matroska",
        modified = 20
    )
    private val sameMime = Entry(
        uri = "content://media/3",
        name = "C.mp4",
        path = "Clips/",
        mime = "video/mp4",
        modified = 30
    )
    private val fallback = Entry(
        uri = "content://media/4",
        name = "D.webm",
        path = null,
        mime = "video/webm",
        modified = 40
    )

    private fun select(
        candidates: List<Entry> = listOf(fallback, sameMime, sameFolder, sameFolderAndMime),
        limit: Int = 10
    ) = VideoRelatedItems.select(
        currentMediaUri = current.uri,
        currentRelativePath = current.path,
        currentMimeType = current.mime,
        candidates = candidates,
        limit = limit,
        mediaUri = Entry::uri,
        relativePath = Entry::path,
        mimeType = Entry::mime,
        modifiedSeconds = Entry::modified,
        displayName = Entry::name
    )

    @Test
    fun ranksSameFolderAndMimeBeforeSingleMatchesAndFallbacks() {
        assertEquals(
            listOf(sameFolderAndMime, sameFolder, sameMime, fallback),
            select()
        )
    }

    @Test
    fun excludesCurrentItemAndDeduplicatesRepeatedUris() {
        assertEquals(
            listOf(sameFolderAndMime),
            select(
                candidates = listOf(
                    current,
                    sameFolderAndMime,
                    sameFolderAndMime.copy(name = "Duplicate entry")
                )
            )
        )
    }

    @Test
    fun usesNewestThenNameAsDeterministicTieBreakers() {
        val older = sameMime.copy(uri = "uri-older", name = "Zebra.mp4", modified = 10)
        val newerZ = sameMime.copy(uri = "uri-newer-z", name = "zebra.mp4", modified = 30)
        val newerA = sameMime.copy(uri = "uri-newer-a", name = "Alpha.mp4", modified = 30)

        assertEquals(
            listOf(newerA, newerZ, older),
            select(candidates = listOf(older, newerZ, newerA))
        )
    }

    @Test
    fun respectsLimitAndAllowsZeroResults() {
        assertEquals(listOf(sameFolderAndMime), select(limit = 1))
        assertEquals(emptyList<Entry>(), select(limit = 0))
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsBlankCurrentMediaIdentity() {
        VideoRelatedItems.select(
            currentMediaUri = " ",
            currentRelativePath = null,
            currentMimeType = null,
            candidates = emptyList<Entry>(),
            mediaUri = Entry::uri,
            relativePath = Entry::path,
            mimeType = Entry::mime,
            modifiedSeconds = Entry::modified,
            displayName = Entry::name
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsNegativeLimit() {
        select(limit = -1)
    }
}
