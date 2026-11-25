package com.ironlogic.veyra.data.video

import org.junit.Assert.assertEquals
import org.junit.Test

class VideoFolderBrowsePolicyTest {
    @Test
    fun `shows only videos belonging to the opened folder`() {
        val first = video("first", setOf("tree-a"))
        val second = video("second", setOf("tree-b"))
        val shared = video("shared", setOf("tree-a", "tree-b"))

        assertEquals(
            listOf(first, shared),
            VideoFolderBrowsePolicy.videosInFolder(listOf(first, second, shared), "tree-a"),
        )
    }

    @Test
    fun `returns an empty list for a blank or unknown folder`() {
        val videos = listOf(video("first", setOf("tree-a")))

        assertEquals(emptyList<VideoItem>(), VideoFolderBrowsePolicy.videosInFolder(videos, ""))
        assertEquals(emptyList<VideoItem>(), VideoFolderBrowsePolicy.videosInFolder(videos, "tree-b"))
    }

    private fun video(id: String, folders: Set<String>) = VideoItem(
        id = "content://video/$id",
        contentUri = "content://video/$id",
        displayName = "$id.mp4",
        durationMs = 60_000L,
        sizeBytes = 1_024L,
        addedEpochSeconds = 1L,
        sourceFolderUris = folders,
    )
}
