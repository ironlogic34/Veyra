package com.ironlogic.veyra.data.video

import android.content.Context

/** Persistent preferences for the local video-library tab. */
data class VideoLibrarySettings(
    val sortOrder: VideoSortOrder = VideoSortOrder.NEWEST_FIRST
)

/** Invalid or missing values keep the established newest-first default. */
internal fun parseVideoSortOrder(value: String?): VideoSortOrder =
    value?.let { stored ->
        runCatching { VideoSortOrder.valueOf(stored) }.getOrNull()
    } ?: VideoSortOrder.NEWEST_FIRST

class VideoLibrarySettingsStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE
    )

    fun load(): VideoLibrarySettings = VideoLibrarySettings(
        sortOrder = parseVideoSortOrder(preferences.getString(KEY_SORT_ORDER, null))
    )

    fun save(settings: VideoLibrarySettings) {
        preferences.edit()
            .putString(KEY_SORT_ORDER, settings.sortOrder.name)
            .apply()
    }

    private companion object {
        const val PREFERENCES_NAME = "veyra_video_library_settings"
        const val KEY_SORT_ORDER = "sort_order"
    }
}
