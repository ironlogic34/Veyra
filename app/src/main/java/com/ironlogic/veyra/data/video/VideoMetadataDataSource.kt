package com.ironlogic.veyra.data.video

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Reads technical metadata directly from a content URI.
 *
 * This is useful for SAF providers that do not expose duration in their document
 * cursor. A failed or unsupported provider returns null rather than breaking a
 * library scan.
 */
class VideoMetadataDataSource(private val context: Context) {
    suspend fun load(uri: Uri): VideoMetadata? = withContext(Dispatchers.IO) {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, uri)
            VideoMetadata(
                durationMs = VideoMetadataParser.parseLong(
                    retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ),
                width = VideoMetadataParser.parseInt(
                    retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
                ),
                height = VideoMetadataParser.parseInt(
                    retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
                ),
                rotationDegrees = VideoMetadataParser.parseInt(
                    retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)
                ),
                bitrateBitsPerSecond = VideoMetadataParser.parseLong(
                    retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)
                ),
                mimeType = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE)
            )
        } catch (_: Exception) {
            null
        } finally {
            try {
                retriever.release()
            } catch (_: RuntimeException) {
                // The provider may invalidate its descriptor during extraction.
            }
        }
    }
}
