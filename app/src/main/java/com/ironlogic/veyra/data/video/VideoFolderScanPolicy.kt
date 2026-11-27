package com.ironlogic.veyra.data.video

data class VideoFolderScanSnapshot(
    val videos: List<VideoItem>,
    val directories: List<VideoFolderDirectory>,
)

/** Commits a selected-folder scan only after that root finishes without losing access. */
object VideoFolderScanPolicy {
    fun commitRoot(
        existingVideos: List<VideoItem>,
        existingDirectories: List<VideoFolderDirectory>,
        rootVideos: List<VideoItem>,
        rootDirectories: List<VideoFolderDirectory>,
        rootAccessible: Boolean,
    ): VideoFolderScanSnapshot {
        if (!rootAccessible) {
            return VideoFolderScanSnapshot(existingVideos, existingDirectories)
        }

        return VideoFolderScanSnapshot(
            videos = VideoLibraryMergePolicy.merge(existingVideos, rootVideos),
            directories = (existingDirectories + rootDirectories)
                .distinctBy { it.rootUri to it.uri },
        )
    }
}
