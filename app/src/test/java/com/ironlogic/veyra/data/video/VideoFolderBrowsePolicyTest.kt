package com.ironlogic.veyra.data.video

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VideoFolderBrowsePolicyTest {
    @Test
    fun `shows only videos belonging to the opened folder or its descendants`() {
        val first = video("first", setOf("tree-a", "dir-a"))
        val second = video("second", setOf("tree-b", "dir-b"))
        val nested = video("nested", setOf("tree-a", "dir-a", "dir-a-child"))

        assertEquals(
            listOf(first, nested),
            VideoFolderBrowsePolicy.videosInFolder(listOf(first, second, nested), "dir-a"),
        )
        assertEquals(
            listOf(first, nested),
            VideoFolderBrowsePolicy.videosInFolder(listOf(first, second, nested), "tree-a"),
        )
    }

    @Test
    fun `returns an empty list for a blank or unknown folder`() {
        val videos = listOf(video("first", setOf("tree-a")))

        assertEquals(emptyList<VideoItem>(), VideoFolderBrowsePolicy.videosInFolder(videos, ""))
        assertEquals(emptyList<VideoItem>(), VideoFolderBrowsePolicy.videosInFolder(videos, "tree-b"))
    }

    @Test
    fun `lists only direct child folders from the active selected root`() {
        val child = VideoFolderDirectory("dir-a", "tree-a", "tree-a", "A")
        val nested = VideoFolderDirectory("dir-a-child", "dir-a", "tree-a", "A child")
        val otherRoot = VideoFolderDirectory("dir-b", "tree-b", "tree-b", "B")

        assertEquals(
            listOf(child),
            VideoFolderBrowsePolicy.childFolders(listOf(child, nested, otherRoot), "tree-a", "tree-a"),
        )
        assertEquals(
            listOf(nested),
            VideoFolderBrowsePolicy.childFolders(listOf(child, nested, otherRoot), "tree-a", "dir-a"),
        )
    }

    @Test
    fun `returns the parent of a nested folder but not of the selected root`() {
        val child = VideoFolderDirectory("dir-a", "tree-a", "tree-a", "A")

        assertEquals(
            "tree-a",
            VideoFolderBrowsePolicy.parentFolderUri(listOf(child), "tree-a", "dir-a"),
        )
        assertNull(VideoFolderBrowsePolicy.parentFolderUri(listOf(child), "tree-a", "tree-a"))
    }

    private fun video(id: String, directories: Set<String>) = VideoItem(
        id = "content://video/$id",
        contentUri = "content://video/$id",
        displayName = "$id.mp4",
        durationMs = 60_000L,
        sizeBytes = 1_024L,
        addedEpochSeconds = 1L,
        sourceFolderUris = directories.filter { it.startsWith("tree-") }.toSet(),
        sourceDirectoryUris = directories,
    )
}
