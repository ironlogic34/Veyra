package com.ironlogic.veyra.data.video

object VideoSubtitlePermissionPolicy {
    fun uriToRelease(previousUri: String?, nextUri: String?): String? =
        previousUri?.takeIf { it.isNotBlank() && it != nextUri }
}
