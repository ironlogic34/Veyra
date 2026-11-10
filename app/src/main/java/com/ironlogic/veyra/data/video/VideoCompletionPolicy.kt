package com.ironlogic.veyra.data.video

enum class VideoCompletionMode {
    STOP,
    REPEAT,
    PLAY_NEXT
}

sealed interface VideoCompletionAction {
    data object Stop : VideoCompletionAction
    data class Replay(val mediaUri: String) : VideoCompletionAction
    data class PlayNext(val mediaUri: String) : VideoCompletionAction
}

/**
 * Resolves end-of-video behavior without depending on a player or UI.
 * Queue entries use content URIs so filenames are never treated as identity.
 */
internal object VideoCompletionPolicy {
    fun decide(
        mode: VideoCompletionMode,
        queue: List<String>,
        currentIndex: Int
    ): VideoCompletionAction {
        if (currentIndex !in queue.indices) return VideoCompletionAction.Stop

        val current = queue[currentIndex]
        if (current.isBlank()) return VideoCompletionAction.Stop

        return when (mode) {
            VideoCompletionMode.STOP -> VideoCompletionAction.Stop
            VideoCompletionMode.REPEAT -> VideoCompletionAction.Replay(current)
            VideoCompletionMode.PLAY_NEXT -> {
                val next = queue.getOrNull(currentIndex + 1)
                    ?.takeIf(String::isNotBlank)
                if (next == null) VideoCompletionAction.Stop
                else VideoCompletionAction.PlayNext(next)
            }
        }
    }
}
