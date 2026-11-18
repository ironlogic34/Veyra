package com.ironlogic.veyra.data.video

/** Creates a progress record when a seek leaves one queued media item for another. */
internal object VideoPlaybackTransitionPolicy {
    fun progressForItemChange(
        oldMediaUri: String?,
        newMediaUri: String?,
        oldPositionMs: Long,
        oldDurationMs: Long?,
        nowEpochMs: Long
    ): VideoPlaybackRecord? {
        if (oldMediaUri.isNullOrBlank() || oldMediaUri == newMediaUri || oldPositionMs <= 0L) {
            return null
        }

        return VideoResumePolicy.progress(
            mediaUri = oldMediaUri,
            positionMs = oldPositionMs,
            durationMs = oldDurationMs,
            nowEpochMs = nowEpochMs
        )
    }
}
