package com.ironlogic.veyra.data.video

/**
 * Playback progress associated with a stable content-URI identity.
 *
 * Persistence is deliberately kept outside this model so storage can be selected
 * without coupling playback rules to a database implementation.
 */
data class VideoPlaybackRecord(
    val mediaUri: String,
    val positionMs: Long,
    val durationMs: Long?,
    val lastPlayedAtEpochMs: Long,
    val isCompleted: Boolean = false
) {
    init {
        require(mediaUri.isNotBlank()) { "mediaUri must not be blank." }
        require(positionMs >= 0L) { "positionMs cannot be negative." }
        require(durationMs == null || durationMs >= 0L) {
            "durationMs cannot be negative."
        }
        require(lastPlayedAtEpochMs >= 0L) {
            "lastPlayedAtEpochMs cannot be negative."
        }
    }
}

/** Pure rules for deciding whether a previous position can be resumed. */
internal object VideoResumePolicy {
    /**
     * Returns a valid resume position, or null when playback should start at zero.
     * A completed item, mismatched media identity, zero position, or position at
     * or beyond a known duration is not resumable.
     */
    fun resumePosition(record: VideoPlaybackRecord?, mediaUri: String): Long? {
        if (record == null || mediaUri.isBlank()) return null
        if (record.mediaUri != mediaUri || record.isCompleted) return null
        if (record.positionMs <= 0L) return null

        val duration = record.durationMs
        if (duration != null && duration > 0L && record.positionMs >= duration) {
            return null
        }

        return record.positionMs
    }

    fun progress(
        mediaUri: String,
        positionMs: Long,
        durationMs: Long?,
        nowEpochMs: Long
    ): VideoPlaybackRecord = VideoPlaybackRecord(
        mediaUri = mediaUri,
        positionMs = positionMs.coerceAtLeast(0L),
        durationMs = durationMs?.takeIf { it > 0L },
        lastPlayedAtEpochMs = nowEpochMs,
        isCompleted = false
    )

    fun completed(
        mediaUri: String,
        durationMs: Long?,
        nowEpochMs: Long
    ): VideoPlaybackRecord = VideoPlaybackRecord(
        mediaUri = mediaUri,
        positionMs = durationMs?.coerceAtLeast(0L) ?: 0L,
        durationMs = durationMs?.takeIf { it > 0L },
        lastPlayedAtEpochMs = nowEpochMs,
        isCompleted = true
    )
}
