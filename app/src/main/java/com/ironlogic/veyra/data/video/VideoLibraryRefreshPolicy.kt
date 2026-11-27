package com.ironlogic.veyra.data.video

object VideoLibraryRefreshPolicy {
    fun shouldReplaceCachedVideos(
        localScanSucceeded: Boolean,
        hasFolderVideos: Boolean,
        accessChangedDuringScan: Boolean,
    ): Boolean = localScanSucceeded || hasFolderVideos || accessChangedDuringScan
}
