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

/** Adds extracted values without replacing valid information already indexed by Android. */
internal fun VideoItem.withMetadata(metadata: VideoMetadata): VideoItem = copy(
    durationMs = durationMs.takeIf { it > 0L } ?: metadata.durationMs ?: durationMs,
    width = width ?: metadata.width,
    height = height ?: metadata.height,
    rotationDegrees = rotationDegrees ?: metadata.rotationDegrees,
    bitrateBitsPerSecond = bitrateBitsPerSecond ?: metadata.bitrateBitsPerSecond,
    mimeType = mimeType ?: metadata.mimeType
)
