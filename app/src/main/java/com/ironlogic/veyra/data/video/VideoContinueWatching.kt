package com.ironlogic.veyra.data.video

/** A library item paired with its most recent resumable playback record. */
internal data class VideoContinueWatchingEntry<T>(
    val item: T,
    val record: VideoPlaybackRecord
)

/**
 * Resolves recent playback records against the currently accessible library.
 * Input record order is preserved, so callers can pass the history store's
 * newest-first result without sorting or relying on filenames.
 */
internal object VideoContinueWatching {
    fun <T : Any> select(
        records: List<VideoPlaybackRecord>,
        candidates: List<T>,
        limit: Int = DEFAULT_LIMIT,
        mediaUri: (T) -> String
    ): List<VideoContinueWatchingEntry<T>> {
        require(limit >= 0) { "limit cannot be negative." }
        if (limit == 0 || records.isEmpty() || candidates.isEmpty()) return emptyList()

        val candidatesByUri = candidates.associateBy(mediaUri)
        return records.asSequence()
            .mapNotNull { record ->
                val item = candidatesByUri[record.mediaUri] ?: return@mapNotNull null
                if (VideoResumePolicy.resumePosition(record, record.mediaUri) == null) {
                    null
                } else {
                    VideoContinueWatchingEntry(item, record)
                }
            }
            .take(limit)
            .toList()
    }

    private const val DEFAULT_LIMIT = 10
}
