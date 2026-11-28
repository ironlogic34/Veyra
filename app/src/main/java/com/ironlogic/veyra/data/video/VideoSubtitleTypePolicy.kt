package com.ironlogic.veyra.data.video

object VideoSubtitleTypePolicy {
    fun mimeTypeFor(displayName: String): String? = when (
        displayName.substringAfterLast('.', "").lowercase()
    ) {
        "srt" -> "application/x-subrip"
        "vtt" -> "text/vtt"
        else -> null
    }
}
