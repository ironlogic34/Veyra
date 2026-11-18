package com.ironlogic.veyra.data.video

import org.junit.Assert.assertEquals
import org.junit.Test

class VideoContinueWatchingTest {
    private data class Entry(val uri: String, val name: String)

    private fun record(
        uri: String,
        positionMs: Long,
        lastPlayedAt: Long,
        durationMs: Long? = 100L,
        isCompleted: Boolean = false
    ) = VideoPlaybackRecord(
        mediaUri = uri,
        positionMs = positionMs,
        durationMs = durationMs,
        lastPlayedAtEpochMs = lastPlayedAt,
        isCompleted = isCompleted
    )

    @Test
    fun selectsRecentResumableItemsStillInTheAccessibleLibrary() {
        val records = listOf(
            record("new", 50L, 300L),
            record("completed", 90L, 250L, isCompleted = true),
            record("missing", 20L, 200L),
            record("old", 10L, 100L),
            record("at-end", 100L, 50L)
        )
        val candidates = listOf(
            Entry("old", "Old video"),
            Entry("completed", "Completed video"),
            Entry("new", "New video"),
            Entry("at-end", "At end")
        )

        val selected = VideoContinueWatching.select(
            records = records,
            candidates = candidates,
            mediaUri = Entry::uri
        )

        assertEquals(listOf("new", "old"), selected.map { it.item.uri })
        assertEquals(listOf(50L, 10L), selected.map { it.record.positionMs })
    }

    @Test
    fun respectsLimitAndAllowsAnEmptyResult() {
        val records = listOf(record("new", 50L, 300L), record("old", 10L, 100L))
        val candidates = listOf(Entry("new", "New"), Entry("old", "Old"))

        assertEquals(
            listOf("new"),
            VideoContinueWatching.select(records, candidates, limit = 1, mediaUri = Entry::uri)
                .map { it.item.uri }
        )
        assertEquals(
            emptyList<Entry>(),
            VideoContinueWatching.select(records, candidates, limit = 0, mediaUri = Entry::uri)
                .map { it.item }
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsNegativeLimit() {
        VideoContinueWatching.select(
            records = emptyList(),
            candidates = emptyList<Entry>(),
            limit = -1,
            mediaUri = Entry::uri
        )
    }
}
