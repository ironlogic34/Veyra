package com.ironlogic.veyra.data.video

object VideoLibraryMergePolicy {
    /** Prefer the first source for duplicate URIs, then show newest files first. */
    fun merge(
        mediaStoreVideos: List<VideoItem>,
        folderVideos: List<VideoItem>,
    ): List<VideoItem> = (mediaStoreVideos + folderVideos)
        .distinctBy(VideoItem::contentUri)
        .sortedByDescending(VideoItem::addedEpochSeconds)
}
