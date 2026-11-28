package com.ironlogic.veyra.data.video

/** Keeps incomplete or malformed MediaStore metadata safe for library presentation. */
object VideoMediaStoreMetadataPolicy {
    fun displayName(value: String?): String =
        value?.takeIf { it.isNotBlank() } ?: "Untitled video"

    fun nonNegative(value: Long): Long = value.coerceAtLeast(0L)
}
