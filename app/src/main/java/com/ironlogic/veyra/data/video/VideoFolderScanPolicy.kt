package com.ironlogic.veyra.data.video

data class VideoFolderScanSnapshot(
    val videos: List<VideoItem>,
    val directories: List<VideoFolderDirectory>,
)

enum class VideoFolderScanOutcome {
    COMPLETE,
    INCOMPLETE,
    ACCESS_LOST,
}

/** Maintains a per-root snapshot so one failed SAF scan cannot publish partial results. */
object VideoFolderScanPolicy {
    fun retainSelectedRoots(
        existingVideos: List<VideoItem>,
        existingDirectories: List<VideoFolderDirectory>,
        selectedRootUris: List<String>,
    ): VideoFolderScanSnapshot {
        val selectedRoots = selectedRootUris.toSet()
        val directories = existingDirectories.filter { it.rootUri in selectedRoots }
        val validDirectoryUris = selectedRoots + directories.map { it.uri }
        val videos = existingVideos.mapNotNull { item ->
            val retainedRoots = item.sourceFolderUris intersect selectedRoots
            if (retainedRoots.isEmpty()) {
                null
            } else {
                item.copy(
                    sourceFolderUris = retainedRoots,
                    sourceDirectoryUris = item.sourceDirectoryUris intersect validDirectoryUris,
                )
            }
        }
        return VideoFolderScanSnapshot(videos, directories)
    }

    fun commitRoot(
        existingVideos: List<VideoItem>,
        existingDirectories: List<VideoFolderDirectory>,
        rootVideos: List<VideoItem>,
        rootDirectories: List<VideoFolderDirectory>,
        rootUri: String,
        outcome: VideoFolderScanOutcome,
    ): VideoFolderScanSnapshot {
        if (outcome == VideoFolderScanOutcome.INCOMPLETE) {
            return VideoFolderScanSnapshot(existingVideos, existingDirectories)
        }

        val rootDirectoryUris = existingDirectories
            .filter { it.rootUri == rootUri }
            .map { it.uri }
            .toSet() + rootUri
        val retainedVideos = existingVideos.mapNotNull { item ->
            val retainedRoots = item.sourceFolderUris - rootUri
            if (retainedRoots.isEmpty()) {
                null
            } else {
                item.copy(
                    sourceFolderUris = retainedRoots,
                    sourceDirectoryUris = item.sourceDirectoryUris - rootDirectoryUris,
                )
            }
        }
        val retainedDirectories = existingDirectories.filterNot { it.rootUri == rootUri }

        if (outcome == VideoFolderScanOutcome.ACCESS_LOST) {
            return VideoFolderScanSnapshot(retainedVideos, retainedDirectories)
        }

        return VideoFolderScanSnapshot(
            videos = VideoLibraryMergePolicy.merge(rootVideos, retainedVideos),
            directories = (retainedDirectories + rootDirectories)
                .distinctBy { it.rootUri to it.uri },
        )
    }
}
