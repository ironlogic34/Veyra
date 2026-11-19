package com.ironlogic.veyra.data.video

import android.content.ContentResolver
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.util.LruCache
import android.util.Size
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Extracts thumbnails locally and avoids decoding the same video repeatedly. */
class VideoThumbnailRepository(
    private val contentResolver: ContentResolver,
) {
    private val memoryCache = object : LruCache<String, Bitmap>(MAX_ENTRIES) {}

    suspend fun load(video: VideoItem): Bitmap? {
        memoryCache.get(video.contentUri)?.let { return it }

        val bitmap = withContext(Dispatchers.IO) {
            val uri = Uri.parse(video.contentUri)
            runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    contentResolver.loadThumbnail(uri, Size(320, 180), null)
                } else {
                    extractWithRetriever(uri)
                }
            }.getOrNull()
        }

        if (bitmap != null) memoryCache.put(video.contentUri, bitmap)
        return bitmap
    }

    fun clear() {
        memoryCache.evictAll()
    }

    private fun extractWithRetriever(uri: Uri): Bitmap? {
        val retriever = MediaMetadataRetriever()
        return try {
            contentResolver.openFileDescriptor(uri, "r")?.use { descriptor ->
                retriever.setDataSource(descriptor.fileDescriptor)
                retriever.getFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
            }
        } finally {
            retriever.release()
        }
    }

    private companion object {
        const val MAX_ENTRIES = 32
    }
}
