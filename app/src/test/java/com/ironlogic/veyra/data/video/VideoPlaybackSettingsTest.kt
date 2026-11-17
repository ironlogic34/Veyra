package com.ironlogic.veyra.data.video

import org.junit.Assert.assertEquals
import org.junit.Test

class VideoPlaybackSettingsTest {
    @Test
    fun parsesEverySupportedCompletionMode() {
        VideoCompletionMode.entries.forEach { mode ->
            assertEquals(mode, parseVideoCompletionMode(mode.name))
        }
    }

    @Test
    fun missingOrInvalidCompletionModeUsesPlayNextDefault() {
        assertEquals(
            VideoCompletionMode.PLAY_NEXT,
            parseVideoCompletionMode(null)
        )
        assertEquals(
            VideoCompletionMode.PLAY_NEXT,
            parseVideoCompletionMode("unknown")
        )
        assertEquals(
            VideoCompletionMode.PLAY_NEXT,
            parseVideoCompletionMode("play_next")
        )
    }

    @Test
    fun playbackSettingsKeepTheExistingDefaults() {
        assertEquals(
            VideoPlaybackSettings(
                resumePlayback = true,
                completionMode = VideoCompletionMode.PLAY_NEXT
            ),
            VideoPlaybackSettings()
        )
    }
}
