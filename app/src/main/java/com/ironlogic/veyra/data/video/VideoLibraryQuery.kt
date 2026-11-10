package com.ironlogic.veyra.data.video

import java.util.Locale

enum class VideoSortOrder {
    NEWEST_FIRST,
    OLDEST_FIRST,
    NAME_A_TO_Z,
    NAME_Z_TO_A,
    DURATION_SHORTEST_FIRST,
    DURATION_LONGEST_FIRST,
    SIZE_SMALLEST_FIRST,
    SIZE_LARGEST_FIRST
}

/**
 * Stateless search and sorting for a video collection.
 *
 * Selectors keep this logic independent from Android and straightforward to test.
 * Search is case-insensitive and checks both the display name and relative folder path.
 */
internal object VideoLibraryQuery {
    fun <T> apply(
        items: List<T>,
        searchQuery: String = "",
        sortOrder: VideoSortOrder = VideoSortOrder.NEWEST_FIRST,
        allowedMimeTypes: Set<String>? = null,
        displayName: (T) -> String,
        relativePath: (T) -> String?,
        durationMs: (T) -> Long,
        sizeBytes: (T) -> Long,
        modifiedSeconds: (T) -> Long,
        mimeType: (T) -> String?
    ): List<T> {
        val query = searchQuery.trim().lowercase(Locale.ROOT)
        val mimeFilter = allowedMimeTypes
            ?.mapTo(mutableSetOf()) { it.lowercase(Locale.ROOT) }

        val filtered = items.filter { item ->
            val matchesQuery = query.isEmpty() ||
                displayName(item).lowercase(Locale.ROOT).contains(query) ||
                relativePath(item)?.lowercase(Locale.ROOT)?.contains(query) == true

            val itemMimeType = mimeType(item)?.lowercase(Locale.ROOT)
            val matchesMimeType = mimeFilter == null ||
                itemMimeType in mimeFilter

            matchesQuery && matchesMimeType
        }

        val nameAscending = compareBy<T> {
            displayName(it).lowercase(Locale.ROOT)
        }
        val newestFirst = compareByDescending<T> { modifiedSeconds(it) }
            .then(nameAscending)

        val comparator = when (sortOrder) {
            VideoSortOrder.NEWEST_FIRST -> newestFirst
            VideoSortOrder.OLDEST_FIRST ->
                compareBy<T> { modifiedSeconds(it) }.then(nameAscending)
            VideoSortOrder.NAME_A_TO_Z -> nameAscending
                .thenByDescending { modifiedSeconds(it) }
            VideoSortOrder.NAME_Z_TO_A ->
                compareByDescending<T> { displayName(it).lowercase(Locale.ROOT) }
                    .thenByDescending { modifiedSeconds(it) }
            VideoSortOrder.DURATION_SHORTEST_FIRST ->
                compareBy<T> { durationMs(it) }.then(newestFirst)
            VideoSortOrder.DURATION_LONGEST_FIRST ->
                compareByDescending<T> { durationMs(it) }.then(newestFirst)
            VideoSortOrder.SIZE_SMALLEST_FIRST ->
                compareBy<T> { sizeBytes(it) }.then(newestFirst)
            VideoSortOrder.SIZE_LARGEST_FIRST ->
                compareByDescending<T> { sizeBytes(it) }.then(newestFirst)
        }

        return filtered.sortedWith(comparator)
    }
}
