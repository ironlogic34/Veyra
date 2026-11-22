package com.ironlogic.veyra.data.video

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.util.LruCache
import android.util.Size
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File

/** Extracts thumbnails locally and avoids decoding the same video repeatedly. */
class VideoThumbnailRepository(
    private val contentResolver: ContentResolver,
    cacheDirectory: File,
) {
    private val diskCache = VideoThumbnailDiskCache(cacheDirectory)
    private val memoryCache = object : LruCache<String, Bitmap>(MAX_ENTRIES) {}

    suspend fun load(video: VideoItem): Bitmap? {
        val cacheKey = VideoThumbnailCacheKey.forVideo(video)
        memoryCache.get(cacheKey)?.let { return it }

        val bitmap = withContext(Dispatchers.IO) {
            val cachedBytes = diskCache.read(cacheKey)
            if (cachedBytes != null) {
                val cachedBitmap = runCatching {
                    BitmapFactory.decodeByteArray(cachedBytes, 0, cachedBytes.size)
                }.getOrNull()
                if (cachedBitmap != null) return@withContext cachedBitmap
                diskCache.remove(cacheKey)
            }

            val uri = Uri.parse(video.contentUri)
            val extracted = runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    contentResolver.loadThumbnail(uri, Size(320, 180), null)
                } else {
                    extractWithRetriever(uri)
                }
            }.getOrNull() ?: return@withContext null

            val encoded = runCatching {
                ByteArrayOutputStream().use { output ->
                    if (extracted.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, output)) {
                        output.toByteArray()
                    } else {
                        null
                    }
                }
            }.getOrNull()
            if (encoded != null) diskCache.write(cacheKey, encoded)
            extracted
        }

        if (bitmap != null) memoryCache.put(cacheKey, bitmap)
        return bitmap
    }

    fun clear() {
        memoryCache.evictAll()
        diskCache.clear()
    }

    private fun extractWithRetriever(uri: Uri): Bitmap? {
        val retriever = MediaMetadataRetriever()
        return try {
            contentResolver.openFileDescriptor(uri, "r")?.use { descriptor ->
                retriever.setDataSource(descriptor.fileDescriptor)
                retriever.getFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
            }
        } finally {
            runCatching { retriever.release() }
        }
    }

    private companion object {
        const val MAX_ENTRIES = 32
        const val JPEG_QUALITY = 86
    }
}
