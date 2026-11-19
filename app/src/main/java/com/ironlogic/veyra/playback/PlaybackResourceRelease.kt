package com.ironlogic.veyra.playback

/**
 * Releases playback resources even when the final progress write or player
 * release fails. The history store is always the last resource closed.
 */
internal suspend fun releasePlaybackResources(
    saveFinalProgress: suspend () -> Unit,
    releasePlayer: () -> Unit,
    closeHistory: () -> Unit
) {
    try {
        saveFinalProgress()
    } finally {
        try {
            releasePlayer()
        } finally {
            closeHistory()
        }
    }
}
