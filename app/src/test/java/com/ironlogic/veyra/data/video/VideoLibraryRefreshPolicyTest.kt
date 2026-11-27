package com.ironlogic.veyra.data.video

import org.junit.Assert.assertEquals
import org.junit.Test

class VideoLibraryRefreshPolicyTest {
    @Test
    fun `successful scan replaces the cached MediaStore snapshot`() {
        val previous = listOf(video("old"))
        val scanned = listOf(video("new"))

        assertEquals(
            scanned,
            VideoLibraryRefreshPolicy.mediaStoreVideos(previous, scanned, accessChangedDuringScan = false),
        )
    }

    @Test
    fun `successful empty scan clears old MediaStore items`() {
        assertEquals(
            emptyList<VideoItem>(),
            VideoLibraryRefreshPolicy.mediaStoreVideos(
                previousVideos = listOf(video("old")),
                scannedVideos = emptyList(),
                accessChangedDuringScan = false,
            ),
        )
    }

    @Test
    fun `transient scan failure preserves the previous MediaStore snapshot`() {
        val previous = listOf(video("first"), video("second"))

        assertEquals(
            previous,
            VideoLibraryRefreshPolicy.mediaStoreVideos(
                previousVideos = previous,
                scannedVideos = null,
                accessChangedDuringScan = false,
            ),
        )
    }

    @Test
    fun `permission changes invalidate the previous MediaStore snapshot after scan failure`() {
        assertEquals(
            emptyList<VideoItem>(),
            VideoLibraryRefreshPolicy.mediaStoreVideos(
                previousVideos = listOf(video("old")),
                scannedVideos = null,
                accessChangedDuringScan = true,
            ),
        )
    }

    @Test
    fun `permission changes discard a scan made with the previous access state`() {
        assertEquals(
            emptyList<VideoItem>(),
            VideoLibraryRefreshPolicy.mediaStoreVideos(
                previousVideos = emptyList(),
                scannedVideos = listOf(video("stale")),
                accessChangedDuringScan = true,
            ),
        )
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
