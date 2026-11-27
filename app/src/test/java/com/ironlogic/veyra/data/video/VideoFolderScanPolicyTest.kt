package com.ironlogic.veyra.data.video

import org.junit.Assert.assertEquals
import org.junit.Test

class VideoFolderScanPolicyTest {
    @Test
    fun `discards partial videos and directories when root access is lost`() {
        val existing = video("content://tree-a/complete.mp4", setOf("tree-a"))
        val partial = video("content://tree-b/partial.mp4", setOf("tree-b"))
        val existingDirectory = directory("tree-a", "tree-a", "tree-a")
        val partialDirectory = directory("tree-b/child", "tree-b", "tree-b")

        val result = VideoFolderScanPolicy.commitRoot(
            existingVideos = listOf(existing),
            existingDirectories = listOf(existingDirectory),
            rootVideos = listOf(partial),
            rootDirectories = listOf(partialDirectory),
            rootAccessible = false,
        )

        assertEquals(listOf(existing), result.videos)
        assertEquals(listOf(existingDirectory), result.directories)
    }

    @Test
    fun `commits a completed root and preserves duplicate video memberships`() {
        val existing = video("content://shared/video.mp4", setOf("tree-a"))
        val folderCopy = video("content://shared/video.mp4", setOf("tree-b"))
        val child = directory("tree-b/child", "tree-b", "tree-b")

        val result = VideoFolderScanPolicy.commitRoot(
            existingVideos = listOf(existing),
            existingDirectories = emptyList(),
            rootVideos = listOf(folderCopy),
            rootDirectories = listOf(child),
            rootAccessible = true,
        )

        assertEquals(1, result.videos.size)
        assertEquals(setOf("tree-a", "tree-b"), result.videos.single().sourceFolderUris)
        assertEquals(listOf(child), result.directories)
    }

    private fun video(uri: String, roots: Set<String>) = VideoItem(
        id = uri,
        contentUri = uri,
        displayName = "Video.mp4",
        durationMs = 1_000L,
        sizeBytes = 1L,
        addedEpochSeconds = 1L,
        sourceFolderUris = roots,
        sourceDirectoryUris = roots,
    )

    private fun directory(uri: String, parent: String, root: String) = VideoFolderDirectory(
        uri = uri,
        parentUri = parent,
        rootUri = root,
        name = "Folder",
    )
}
