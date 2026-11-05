package com.ironlogic.veyra.data.video

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Loads small preview frames from content URIs, including SAF document URIs.
 * Thumbnails are held in a bounded in-memory cache and are not written to disk.
 */
class VideoThumbnailDataSource(
    private val context: Context,
    maxEntries: Int = 120
) {
    private val cache = LruCache<String, Bitmap>(maxEntries.coerceAtLeast(1))

    suspend fun load(
        uri: Uri,
        requestedWidth: Int = 320,
        requestedHeight: Int = 180
    ): Bitmap? = withContext(Dispatchers.IO) {
        val width = requestedWidth.coerceIn(1, 640)
        val height = requestedHeight.coerceIn(1, 640)
        val key = "${uri}|${width}x${height}"
        cache.get(key)?.let { return@withContext it }

        val retriever = MediaMetadataRetriever()
        val frame = try {
            retriever.setDataSource(context, uri)
            if (android.os.Build.VERSION.SDK_INT >= 27) {
                retriever.getScaledFrameAtTime(
                    1_000_000L,
                    MediaMetadataRetriever.OPTION_CLOSEST_SYNC,
                    width,
                    height
                )
            } else {
                retriever.getFrameAtTime(
                    1_000_000L,
                    MediaMetadataRetriever.OPTION_CLOSEST_SYNC
                )?.let { original ->
                    val scale = minOf(
                        width.toFloat() / original.width,
                        height.toFloat() / original.height
                    )
                    val scaled = Bitmap.createScaledBitmap(
                        original,
                        (original.width * scale).toInt().coerceAtLeast(1),
                        (original.height * scale).toInt().coerceAtLeast(1),
                        true
                    )
                    if (scaled !== original) original.recycle()
                    scaled
                }
            }
        } catch (_: Exception) {
            null
        } finally {
            try {
                retriever.release()
            } catch (_: RuntimeException) {
                // Release can fail for a provider that already invalidated its descriptor.
            }
        }

        frame?.let { cache.put(key, it) }
        frame
    }

    fun clearCache() {
        cache.evictAll()
    }
}
