package com.ironlogic.veyra.data.video

object VideoFolderBrowsePolicy {
    fun videosInFolder(videos: List<VideoItem>, folderUri: String): List<VideoItem> {
        if (folderUri.isBlank()) return emptyList()
        return videos.filter { folderUri in it.sourceFolderUris }
    }
}
