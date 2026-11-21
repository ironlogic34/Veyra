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

    private companion object {
        const val PREFERENCES = "veyra_video_playback_settings"
        const val KEY_RESIZE_MODE = "resize_mode"
        const val KEY_ORIENTATION_MODE = "orientation_mode"
    }
}
