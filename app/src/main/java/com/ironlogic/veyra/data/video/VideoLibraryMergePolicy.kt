package com.ironlogic.veyra.data.video

object VideoLibraryMergePolicy {
    /** Prefer MediaStore metadata for duplicate URIs while preserving every selected-folder membership. */
    fun merge(
        mediaStoreVideos: List<VideoItem>,
        folderVideos: List<VideoItem>,
    ): List<VideoItem> {
        val merged = linkedMapOf<String, VideoItem>()
        (mediaStoreVideos + folderVideos).forEach { item ->
            val existing = merged[item.contentUri]
            merged[item.contentUri] = if (existing == null) {
                item
            } else {
                existing.copy(
                    sourceFolderUris = existing.sourceFolderUris + item.sourceFolderUris,
                )
            }
        }
        return merged.values.sortedByDescending(VideoItem::addedEpochSeconds)
    }
}
