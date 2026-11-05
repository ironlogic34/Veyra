package com.ironlogic.veyra.data.video

import org.junit.Assert.assertEquals
import org.junit.Test

class VideoLibraryMergerTest {
    private data class Entry(
        val uri: String,
        val name: String,
        val modified: Long
    )

    private fun merge(vararg sources: List<Entry>) = VideoLibraryMerger.merge(
        sources = sources.toList(),
        identity = Entry::uri,
        modifiedSeconds = Entry::modified,
        displayName = Entry::name
    )

    @Test
    fun duplicateUriKeepsFirstSourceEntry() {
        val mediaStore = Entry("content://media/1", "Original title.mp4", 100)
        val folderCopy = Entry("content://media/1", "Different title.mp4", 200)

        val result = merge(listOf(mediaStore), listOf(folderCopy))

        assertEquals(listOf(mediaStore), result)
    }

    @Test
    fun sortsNewestFirstThenName() {
        val older = Entry("uri-1", "Zebra.mp4", 10)
        val newestZ = Entry("uri-2", "zebra.mp4", 20)
        val newestA = Entry("uri-3", "Alpha.mp4", 20)

        val result = merge(listOf(older, newestZ, newestA))

        assertEquals(listOf(newestA, newestZ, older), result)
    }

    @Test
    fun keepsDifferentUrisEvenWhenNamesMatch() {
        val first = Entry("uri-1", "clip.mp4", 10)
        val second = Entry("uri-2", "clip.mp4", 10)

        val result = merge(listOf(first, second))

        assertEquals(2, result.size)
    }
}
