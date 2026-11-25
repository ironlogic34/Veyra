package com.ironlogic.veyra.data.video

object VideoFolderBrowsePolicy {
    fun videosInFolder(videos: List<VideoItem>, folderUri: String): List<VideoItem> {
        if (folderUri.isBlank()) return emptyList()
        return videos.filter { folderUri in it.sourceDirectoryUris || folderUri in it.sourceFolderUris }
    }

    fun childFolders(
        directories: List<VideoFolderDirectory>,
        rootUri: String,
        parentUri: String,
    ): List<VideoFolderDirectory> = directories.filter {
        it.rootUri == rootUri && it.parentUri == parentUri
    }

    fun parentFolderUri(
        directories: List<VideoFolderDirectory>,
        rootUri: String,
        folderUri: String,
    ): String? = directories.firstOrNull {
        it.rootUri == rootUri && it.uri == folderUri
    }?.parentUri
}
