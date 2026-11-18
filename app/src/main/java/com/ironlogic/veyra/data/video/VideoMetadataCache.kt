package com.ironlogic.veyra.data.video

/** Stable identity for metadata that may change when a content URI is updated in place. */
internal data class VideoMetadataCacheKey(
    val contentUri: String,
    val dateModifiedSeconds: Long,
    val sizeBytes: Long
)

/** A cached null is meaningful: the provider was checked but could not supply metadata. */
internal data class VideoMetadataCacheEntry(
    val metadata: VideoMetadata?
)

/** Small access-ordered cache shared by visible cards and batch enrichment. */
internal class VideoMetadataCache(
    private val maxEntries: Int = DEFAULT_MAX_ENTRIES
) {
    init {
        require(maxEntries > 0) { "maxEntries must be positive." }
    }

    private val entries = object : LinkedHashMap<VideoMetadataCacheKey, VideoMetadataCacheEntry>(
        maxEntries + 1,
        0.75f,
        true
    ) {
        override fun removeEldestEntry(
            eldest: MutableMap.MutableEntry<VideoMetadataCacheKey, VideoMetadataCacheEntry>?
        ): Boolean = size > maxEntries
    }

    @Synchronized
    fun get(key: VideoMetadataCacheKey): VideoMetadataCacheEntry? = entries[key]

    @Synchronized
    fun put(key: VideoMetadataCacheKey, metadata: VideoMetadata?) {
        entries[key] = VideoMetadataCacheEntry(metadata)
    }

    @Synchronized
    fun clear() {
        entries.clear()
    }

    private companion object {
        const val DEFAULT_MAX_ENTRIES = 256
    }
}
