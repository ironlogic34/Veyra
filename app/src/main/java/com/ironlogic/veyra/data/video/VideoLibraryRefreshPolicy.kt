package com.ironlogic.veyra.data.video

object VideoLibraryRefreshPolicy {
    /**
     * Uses the latest MediaStore snapshot when available. If that scan fails, retains the
     * previous MediaStore snapshot; SAF videos are tracked and refreshed separately.
     * A permission change invalidates the old snapshot until a scan with the new access completes.
     */
    fun mediaStoreVideos(
        previousVideos: List<VideoItem>,
        scannedVideos: List<VideoItem>?,
        accessChangedDuringScan: Boolean,
    ): List<VideoItem> = when {
        accessChangedDuringScan -> emptyList()
        scannedVideos != null -> scannedVideos
        else -> previousVideos
    }
}
