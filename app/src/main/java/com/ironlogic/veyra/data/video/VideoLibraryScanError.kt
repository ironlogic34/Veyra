package com.ironlogic.veyra.data.video

object VideoLibraryScanError {
    fun messageFor(error: Exception): String = when (error) {
        is SecurityException -> "Video access changed. Check your video permissions and try again."
        else -> "Veyra couldn't load your local videos. Try scanning again."
    }
}
