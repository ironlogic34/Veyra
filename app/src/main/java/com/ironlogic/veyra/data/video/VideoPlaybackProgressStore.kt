package com.ironlogic.veyra.data.video

import android.content.Context

class VideoPlaybackProgressStore(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)

    fun read(): VideoProgress? {
        val uri = preferences.getString(KEY_URI, null) ?: return null
        val positionMs = preferences.getLong(KEY_POSITION, 0L)
        val durationMs = preferences.getLong(KEY_DURATION, 0L)
        if (!VideoResumePolicy.shouldResume(positionMs, durationMs)) {
            clear()
            return null
        }

        return VideoProgress(
            contentUri = uri,
            displayName = preferences.getString(KEY_NAME, "Untitled video") ?: "Untitled video",
            positionMs = positionMs,
            durationMs = durationMs,
        )
    }

    fun read(video: VideoItem): VideoProgress? =
        read()?.takeIf { it.contentUri == video.contentUri }

    fun save(video: VideoItem, positionMs: Long, durationMs: Long = video.durationMs) {
        if (!VideoResumePolicy.shouldResume(positionMs, durationMs)) {
            clear(video)
            return
        }

        preferences.edit()
            .putString(KEY_URI, video.contentUri)
            .putString(KEY_NAME, video.displayName)
            .putLong(KEY_POSITION, positionMs)
            .putLong(KEY_DURATION, durationMs.coerceAtLeast(0L))
            .apply()
    }

    fun clear(video: VideoItem) {
        if (preferences.getString(KEY_URI, null) == video.contentUri) clear()
    }

    fun clear() {
        preferences.edit().clear().apply()
    }

    private companion object {
        const val PREFERENCES = "veyra_playback_progress"
        const val KEY_URI = "content_uri"
        const val KEY_NAME = "display_name"
        const val KEY_POSITION = "position_ms"
        const val KEY_DURATION = "duration_ms"
    }
}

data class VideoProgress(
    val contentUri: String,
    val displayName: String,
    val positionMs: Long,
    val durationMs: Long = 0L,
)
