package com.ironlogic.veyra.data.video

import android.content.Context

class VideoPlaybackProgressStore(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)

    fun read(): VideoProgress? {
        val uri = preferences.getString(KEY_URI, null) ?: return null
        return VideoProgress(
            contentUri = uri,
            displayName = preferences.getString(KEY_NAME, "Untitled video") ?: "Untitled video",
            positionMs = preferences.getLong(KEY_POSITION, 0L),
        )
    }

    fun save(video: VideoItem, positionMs: Long) {
        preferences.edit()
            .putString(KEY_URI, video.contentUri)
            .putString(KEY_NAME, video.displayName)
            .putLong(KEY_POSITION, positionMs.coerceAtLeast(0L))
            .apply()
    }

    fun clear() {
        preferences.edit().clear().apply()
    }

    private companion object {
        const val PREFERENCES = "veyra_playback_progress"
        const val KEY_URI = "content_uri"
        const val KEY_NAME = "display_name"
        const val KEY_POSITION = "position_ms"
    }
}

data class VideoProgress(
    val contentUri: String,
    val displayName: String,
    val positionMs: Long,
)
