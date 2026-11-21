package com.ironlogic.veyra.data.video

import org.junit.Assert.assertEquals
import org.junit.Test

class VideoLibraryMergePolicyTest {
    @Test
    fun `merges sources without duplicating a URI`() {
        val mediaStore = video("content://media/1", "MediaStore copy", 10L)
        val folderCopy = video("content://media/1", "Folder copy", 20L)
        val folderOnly = video("content://documents/2", "Folder video", 30L)

        val result = VideoLibraryMergePolicy.merge(listOf(mediaStore), listOf(folderCopy, folderOnly))

        assertEquals(listOf("content://documents/2", "content://media/1"), result.map { it.contentUri })
        assertEquals("MediaStore copy", result.last().displayName)
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
