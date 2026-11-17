package com.ironlogic.veyra.data.video

import android.content.Context

/**
 * User-controlled defaults for local video playback.
 *
 * Defaults preserve the behaviour that existed before these preferences were
 * exposed: resume saved positions and continue to the next queued video.
 */
data class VideoPlaybackSettings(
    val resumePlayback: Boolean = true,
    val completionMode: VideoCompletionMode = VideoCompletionMode.PLAY_NEXT
)

/** Invalid or missing stored values fall back to the established V1 default. */
internal fun parseVideoCompletionMode(value: String?): VideoCompletionMode =
    value?.let { stored ->
        runCatching { VideoCompletionMode.valueOf(stored) }.getOrNull()
    } ?: VideoCompletionMode.PLAY_NEXT

/** Small SharedPreferences-backed store; no new persistence dependency is required. */
class VideoPlaybackSettingsStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE
    )

    fun load(): VideoPlaybackSettings = VideoPlaybackSettings(
        resumePlayback = preferences.getBoolean(KEY_RESUME_PLAYBACK, DEFAULT_RESUME_PLAYBACK),
        completionMode = parseVideoCompletionMode(
            preferences.getString(KEY_COMPLETION_MODE, null)
        )
    )

    fun save(settings: VideoPlaybackSettings) {
        preferences.edit()
            .putBoolean(KEY_RESUME_PLAYBACK, settings.resumePlayback)
            .putString(KEY_COMPLETION_MODE, settings.completionMode.name)
            .apply()
    }

    private companion object {
        const val PREFERENCES_NAME = "veyra_video_playback_settings"
        const val KEY_RESUME_PLAYBACK = "resume_playback"
        const val KEY_COMPLETION_MODE = "completion_mode"
        const val DEFAULT_RESUME_PLAYBACK = true
    }
}
