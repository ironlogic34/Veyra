package com.ironlogic.veyra.data.video

import org.junit.Assert.assertEquals
import org.junit.Test

class VideoQueuePolicyTest {
    @Test
    fun `new item is inserted immediately after the current video`() {
        val current = video("current")
        val later = video("later")
        val selected = video("selected")

        assertEquals(
            listOf(current, selected, later),
            VideoQueuePolicy.playNext(listOf(current, later), current.contentUri, selected),
        )
    }

    @Test
    fun `existing earlier item moves after the current video`() {
        val earlier = video("earlier")
        val current = video("current")
        val later = video("later")

        assertEquals(
            listOf(current, earlier, later),
            VideoQueuePolicy.playNext(
                listOf(earlier, current, later),
                current.contentUri,
                earlier,
            ),
        )
    }

    @Test
    fun `existing later item moves directly after the current video`() {
        val current = video("current")
        val middle = video("middle")
        val selected = video("selected")
        val last = video("last")

        assertEquals(
            listOf(current, selected, middle, last),
            VideoQueuePolicy.playNext(
                listOf(current, middle, selected, last),
                current.contentUri,
                selected,
            ),
        )
    }

    @Test
    fun `item already next keeps the same order`() {
        val current = video("current")
        val next = video("next")
        val later = video("later")

        assertEquals(
            listOf(current, next, later),
            VideoQueuePolicy.playNext(listOf(current, next, later), current.contentUri, next),
        )
    }

    @Test
    fun `current video cannot be queued after itself`() {
        val current = video("current")
        val later = video("later")
        val queue = listOf(current, later)

        assertEquals(queue, VideoQueuePolicy.playNext(queue, current.contentUri, current))
    }

    @Test
    fun `unknown current video leaves the queue unchanged`() {
        val first = video("first")
        val queue = listOf(first)

        assertEquals(queue, VideoQueuePolicy.playNext(queue, "missing", video("selected")))
    }

    private fun video(id: String) = VideoItem(
        id = "content://video/$id",
        contentUri = "content://video/$id",
        displayName = "$id.mp4",
        durationMs = 60_000L,
        sizeBytes = 1_024L,
        addedEpochSeconds = 1L,
    )
}
