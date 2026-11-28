package com.ironlogic.veyra.data.video

import android.content.Context
import java.util.Locale

enum class VideoSortOrder(val label: String) {
    NEWEST_FIRST("Newest"),
    OLDEST_FIRST("Oldest"),
    NAME_A_TO_Z("Name A–Z"),
    NAME_Z_TO_A("Name Z–A"),
    DURATION_SHORTEST_FIRST("Shortest"),
    DURATION_LONGEST_FIRST("Longest"),
    SIZE_SMALLEST_FIRST("Smallest"),
    SIZE_LARGEST_FIRST("Largest"),
}

internal fun parseVideoSortOrder(value: String?): VideoSortOrder =
    value?.let { stored ->
        runCatching { VideoSortOrder.valueOf(stored) }.getOrNull()
    } ?: VideoSortOrder.NEWEST_FIRST

/** Search and ordering for the local library, independent of Compose and Android APIs. */
internal object VideoLibraryQuery {
    fun apply(
        items: List<VideoItem>,
        searchQuery: String = "",
        sortOrder: VideoSortOrder = VideoSortOrder.NEWEST_FIRST,
    ): List<VideoItem> {
        val query = searchQuery.trim().lowercase(Locale.ROOT)
        val filtered = items.filter { item ->
            query.isEmpty() || item.displayName.lowercase(Locale.ROOT).contains(query)
        }

        val nameAscending = compareBy<VideoItem> { it.displayName.lowercase(Locale.ROOT) }
            .thenBy { it.contentUri }
        val newestFirst = compareByDescending<VideoItem> { it.addedEpochSeconds }
            .then(nameAscending)

        val comparator = when (sortOrder) {
            VideoSortOrder.NEWEST_FIRST -> newestFirst
            VideoSortOrder.OLDEST_FIRST ->
                compareBy<VideoItem> { it.addedEpochSeconds }.then(nameAscending)
            VideoSortOrder.NAME_A_TO_Z -> nameAscending.thenByDescending { it.addedEpochSeconds }
            VideoSortOrder.NAME_Z_TO_A ->
                compareByDescending<VideoItem> { it.displayName.lowercase(Locale.ROOT) }
                    .thenByDescending { it.addedEpochSeconds }
                    .thenBy { it.contentUri }
            VideoSortOrder.DURATION_SHORTEST_FIRST ->
                compareBy<VideoItem> { it.durationMs }.then(newestFirst)
            VideoSortOrder.DURATION_LONGEST_FIRST ->
                compareByDescending<VideoItem> { it.durationMs }.then(newestFirst)
            VideoSortOrder.SIZE_SMALLEST_FIRST ->
                compareBy<VideoItem> { it.sizeBytes }.then(newestFirst)
            VideoSortOrder.SIZE_LARGEST_FIRST ->
                compareByDescending<VideoItem> { it.sizeBytes }.then(newestFirst)
        }

        return filtered.sortedWith(comparator)
    }
}

/** Persists the user's local-library sort preference without adding a database dependency. */
class VideoLibrarySettingsStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE,
    )

    fun readSortOrder(): VideoSortOrder =
        parseVideoSortOrder(preferences.getString(KEY_SORT_ORDER, null))

    fun writeSortOrder(sortOrder: VideoSortOrder) {
        preferences.edit().putString(KEY_SORT_ORDER, sortOrder.name).apply()
    }

    private companion object {
        const val PREFERENCES_NAME = "veyra_video_library_settings"
        const val KEY_SORT_ORDER = "sort_order"
    }
}
