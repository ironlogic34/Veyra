package com.ironlogic.veyra.data.video

import android.net.Uri

/**
 * A locally indexed video item.
 *
 * The content URI is the playback/access handle. A filename or filesystem path
 * is not treated as a stable media identity. Optional technical fields are
 * populated by on-demand metadata extraction.
 */
data class VideoItem(
    val contentUri: Uri,
    val displayName: String,
    val durationMs: Long,
    val sizeBytes: Long,
    val dateModifiedSeconds: Long,
    val mimeType: String?,
    val relativePath: String?,
    val width: Int? = null,
    val height: Int? = null,
    val rotationDegrees: Int? = null,
    val bitrateBitsPerSecond: Long? = null
)
