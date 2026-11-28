package com.ironlogic.veyra.data.video

import android.content.Context

class VideoPlaybackSettingsStore(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)

    fun readResizeMode(): VideoResizeMode =
        VideoResizeMode.fromStorage(preferences.getString(KEY_RESIZE_MODE, null))

    fun writeResizeMode(mode: VideoResizeMode) {
        preferences.edit().putString(KEY_RESIZE_MODE, mode.storageValue).apply()
    }

    fun readOrientationMode(): VideoOrientationMode =
        VideoOrientationMode.fromStorage(preferences.getString(KEY_ORIENTATION_MODE, null))

    fun writeOrientationMode(mode: VideoOrientationMode) {
        preferences.edit().putString(KEY_ORIENTATION_MODE, mode.storageValue).apply()
    }

    fun readAutoResumeEnabled(): Boolean =
        preferences.getBoolean(KEY_AUTO_RESUME_ENABLED, true)

    fun writeAutoResumeEnabled(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_AUTO_RESUME_ENABLED, enabled).apply()
    }

    fun readRepeatMode(): VideoRepeatMode =
        VideoRepeatMode.fromStorage(preferences.getString(KEY_REPEAT_MODE, null))

    fun writeRepeatMode(mode: VideoRepeatMode) {
        preferences.edit().putString(KEY_REPEAT_MODE, mode.storageValue).apply()
    }

    private companion object {
        const val PREFERENCES = "veyra_video_playback_settings"
        const val KEY_RESIZE_MODE = "resize_mode"
        const val KEY_ORIENTATION_MODE = "orientation_mode"
        const val KEY_AUTO_RESUME_ENABLED = "auto_resume_enabled"
        const val KEY_REPEAT_MODE = "repeat_mode"
    }
}
