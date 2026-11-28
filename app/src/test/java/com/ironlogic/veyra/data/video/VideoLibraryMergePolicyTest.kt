package com.ironlogic.veyra.data.video

import org.junit.Assert.assertEquals
import org.junit.Test

class VideoLibraryMergePolicyTest {
    @Test
    fun `merges sources without duplicating a URI`() {
        val mediaStore = video("content://media/1", "MediaStore copy", 10L)
            .copy(
                sourceFolderUris = setOf("tree-a"),
                sourceDirectoryUris = setOf("tree-a", "dir-a"),
                relativePath = "Movies/Action",
            )
        val folderCopy = video("content://media/1", "Folder copy", 20L)
            .copy(
                sourceFolderUris = setOf("tree-b"),
                sourceDirectoryUris = setOf("tree-b", "dir-b"),
            )
        val folderOnly = video("content://documents/2", "Folder video", 30L)

        val result = VideoLibraryMergePolicy.merge(listOf(mediaStore), listOf(folderCopy, folderOnly))

        assertEquals(listOf("content://documents/2", "content://media/1"), result.map { it.contentUri })
        assertEquals("MediaStore copy", result.last().displayName)
        assertEquals(setOf("tree-a", "tree-b"), result.last().sourceFolderUris)
        assertEquals(setOf("tree-a", "dir-a", "tree-b", "dir-b"), result.last().sourceDirectoryUris)
        assertEquals("Movies/Action", result.last().relativePath)
    }

    @Test
    fun `uses folder metadata when MediaStore has no relative path`() {
        val folderVideo = video("content://media/1", "Folder copy", 20L)
            .copy(relativePath = "Selected movies")
        val result = VideoLibraryMergePolicy.merge(
            mediaStoreVideos = listOf(video("content://media/1", "MediaStore copy", 10L)),
            folderVideos = listOf(folderVideo),
        )

        assertEquals("Selected movies", result.single().relativePath)
    }

    private fun video(uri: String, name: String, added: Long) = VideoItem(
        id = uri,
        contentUri = uri,
        displayName = name,
        durationMs = 1_000L,
        sizeBytes = 1L,
        addedEpochSeconds = added,
    )
}
