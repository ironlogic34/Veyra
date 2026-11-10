package com.ironlogic.veyra.data.video

import org.junit.Assert.assertEquals
import org.junit.Test

class VideoCompletionPolicyTest {
    private val queue = listOf(
        "content://media/video/one",
        "content://media/video/two",
        "content://media/video/three"
    )

    @Test
    fun stopModeStopsAtCompletion() {
        assertEquals(
            VideoCompletionAction.Stop,
            VideoCompletionPolicy.decide(VideoCompletionMode.STOP, queue, 0)
        )
    }

    @Test
    fun repeatModeReplaysCurrentItem() {
        assertEquals(
            VideoCompletionAction.Replay(queue[1]),
            VideoCompletionPolicy.decide(VideoCompletionMode.REPEAT, queue, 1)
        )
    }

    @Test
    fun playNextAdvancesWithinQueue() {
        assertEquals(
            VideoCompletionAction.PlayNext(queue[1]),
            VideoCompletionPolicy.decide(VideoCompletionMode.PLAY_NEXT, queue, 0)
        )
    }

    @Test
    fun playNextStopsWhenCurrentItemIsLast() {
        assertEquals(
            VideoCompletionAction.Stop,
            VideoCompletionPolicy.decide(VideoCompletionMode.PLAY_NEXT, queue, 2)
        )
    }

    @Test
    fun invalidQueuePositionStopsSafely() {
        assertEquals(
            VideoCompletionAction.Stop,
            VideoCompletionPolicy.decide(VideoCompletionMode.REPEAT, queue, -1)
        )
        assertEquals(
            VideoCompletionAction.Stop,
            VideoCompletionPolicy.decide(VideoCompletionMode.PLAY_NEXT, queue, 3)
        )
        assertEquals(
            VideoCompletionAction.Stop,
            VideoCompletionPolicy.decide(VideoCompletionMode.REPEAT, emptyList(), 0)
        )
    }

    @Test
    fun blankQueueEntriesAreNotPlayed() {
        assertEquals(
            VideoCompletionAction.Stop,
            VideoCompletionPolicy.decide(
                VideoCompletionMode.PLAY_NEXT,
                listOf(queue[0], "  "),
                0
            )
        )
        assertEquals(
            VideoCompletionAction.Stop,
            VideoCompletionPolicy.decide(
                VideoCompletionMode.REPEAT,
                listOf(" "),
                0
            )
        )
    }
}
