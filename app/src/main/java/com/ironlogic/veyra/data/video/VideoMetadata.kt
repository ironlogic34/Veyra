package com.ironlogic.veyra.data.video

/** Technical metadata read from a video's container. Values may be absent when a provider does not expose them. */
data class VideoMetadata(
    val durationMs: Long?,
    val width: Int?,
    val height: Int?,
    val rotationDegrees: Int?,
    val bitrateBitsPerSecond: Long?,
    val mimeType: String?
)

/** Keeps Android's string-based metadata extraction separate from URI/provider access. */
internal object VideoMetadataParser {
    fun parseLong(value: String?): Long? = value?.toLongOrNull()?.takeIf { it >= 0L }

    fun parseInt(value: String?): Int? = value?.toIntOrNull()?.takeIf { it >= 0 }
}

/** Merges extracted metadata while preserving every valid indexed value. */
internal fun mergeVideoMetadata(
    indexedDurationMs: Long,
    indexedWidth: Int?,
    indexedHeight: Int?,
    indexedRotationDegrees: Int?,
    indexedBitrateBitsPerSecond: Long?,
    indexedMimeType: String?,
    extracted: VideoMetadata
): VideoMetadata = VideoMetadata(
    durationMs = indexedDurationMs.takeIf { it > 0L } ?: extracted.durationMs ?: indexedDurationMs,
    width = indexedWidth ?: extracted.width,
    height = indexedHeight ?: extracted.height,
    rotationDegrees = indexedRotationDegrees ?: extracted.rotationDegrees,
    bitrateBitsPerSecond = indexedBitrateBitsPerSecond ?: extracted.bitrateBitsPerSecond,
    mimeType = indexedMimeType ?: extracted.mimeType
)

internal fun VideoItem.withMetadata(metadata: VideoMetadata): VideoItem {
    val merged = mergeVideoMetadata(
        indexedDurationMs = durationMs,
        indexedWidth = width,
        indexedHeight = height,
        indexedRotationDegrees = rotationDegrees,
        indexedBitrateBitsPerSecond = bitrateBitsPerSecond,
        indexedMimeType = mimeType,
        extracted = metadata
    )
    return copy(
        durationMs = merged.durationMs ?: durationMs,
        width = merged.width,
        height = merged.height,
        rotationDegrees = merged.rotationDegrees,
        bitrateBitsPerSecond = merged.bitrateBitsPerSecond,
        mimeType = merged.mimeType
    )
}
