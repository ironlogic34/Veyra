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
    private val metadataCache = VideoMetadataCache()

    private val singleItemSemaphore = Semaphore(DEFAULT_MAX_CONCURRENT_LOADS)

    suspend fun enrichOne(video: VideoItem): VideoItem {
        if (!video.needsMetadata()) return video
        return loadMetadata(video, singleItemSemaphore)?.let(video::withMetadata) ?: video
    }

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
                        loadMetadata(video, semaphore)?.let(video::withMetadata) ?: video
                    }
                }
            }.awaitAll()
        }
    }

    /** Drops both successful and failed extraction results when the library is refreshed. */
    fun clearCache() {
        metadataCache.clear()
    }

    private suspend fun loadMetadata(
        video: VideoItem,
        semaphore: Semaphore
    ): VideoMetadata? {
        val key = VideoMetadataCacheKey(
            contentUri = video.contentUri.toString(),
            dateModifiedSeconds = video.dateModifiedSeconds,
            sizeBytes = video.sizeBytes
        )
        metadataCache.get(key)?.let { return it.metadata }

        return semaphore.withPermit {
            metadataCache.get(key)?.let { return@withPermit it.metadata }
            dataSource.load(video.contentUri).also { metadataCache.put(key, it) }
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
