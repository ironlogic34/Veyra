package com.ironlogic.veyra.data.video

/** Pure queue ordering rules shared by the player and its tests. */
object VideoQueuePolicy {
    /**
     * Places [selected] immediately after the currently playing video.
     *
     * An item already in the queue is moved rather than duplicated. The current item
     * is never moved, and an unknown current item leaves the queue unchanged.
     */
    fun playNext(
        queue: List<VideoItem>,
        currentContentUri: String,
        selected: VideoItem,
    ): List<VideoItem> {
        if (currentContentUri.isBlank() || selected.contentUri == currentContentUri) {
            return queue
        }

        val withoutSelected = queue.filterNot { it.contentUri == selected.contentUri }
        val currentIndex = withoutSelected.indexOfFirst { it.contentUri == currentContentUri }
        if (currentIndex < 0) return queue

        return withoutSelected.toMutableList().apply {
            add(currentIndex + 1, selected)
        }
    }
}
