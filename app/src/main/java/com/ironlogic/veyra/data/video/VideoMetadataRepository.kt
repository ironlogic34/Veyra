package com.ironlogic.veyra.data.video

import android.content.Context
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

/**
 * Enriches a caller-selected batch of videos with container metadata.
 *
 * Callers should pass only visible, selected, or otherwise relevant items.
 * A small concurrency limit avoids opening many media providers at once.
 */
class VideoMetadataRepository(context: Context) {
    private val dataSource = VideoMetadataDataSource(context)

    suspend fun enrich(
        videos: List<VideoItem>,
        maxConcurrentLoads: Int = DEFAULT_MAX_CONCURRENT_LOADS
    ): List<VideoItem> {
        require(maxConcurrentLoads > 0) { "maxConcurrentLoads must be positive." }
        val semaphore = Semaphore(maxConcurrentLoads)

        return coroutineScope {
            videos.map { video ->
                async {
                    if (!video.needsMetadata()) {
                        video
                    } else {
                        semaphore.withPermit {
                            dataSource.load(video.contentUri)?.let(video::withMetadata) ?: video
                        }
                    }
                }
            }.awaitAll()
        }
    }

    private fun VideoItem.needsMetadata(): Boolean =
        durationMs <= 0L ||
            width == null ||
            height == null ||
            rotationDegrees == null ||
            bitrateBitsPerSecond == null ||
            mimeType == null

    private companion object {
        const val DEFAULT_MAX_CONCURRENT_LOADS = 3
    }
}
