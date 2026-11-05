package com.ironlogic.veyra.data.video

import android.net.Uri

/**
 * A locally indexed video item.
 *
 * The content URI is the playback/access handle. A filename or filesystem path
 * is not treated as a stable media identity.
 */
data class VideoItem(
    val contentUri: Uri,
    val displayName: String,
    val durationMs: Long,
    val sizeBytes: Long,
    val dateModifiedSeconds: Long,
    val mimeType: String?,
    val relativePath: String?
)
