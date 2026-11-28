package com.ironlogic.veyra.data.video

object VideoFolderMediaTypePolicy {
    private val supportedExtensions = setOf(
        "3gp", "avi", "flv", "m4v", "mkv", "mov", "mp4", "mpeg", "mpg", "ts", "webm",
    )

    fun isVideo(mimeType: String?, fileName: String?): Boolean {
        if (mimeType?.substringBefore(';')?.startsWith("video/", ignoreCase = true) == true) {
            return true
        }
        val extension = fileName
            ?.substringAfterLast('.', missingDelimiterValue = "")
            ?.lowercase()
        return extension in supportedExtensions
    }
}
