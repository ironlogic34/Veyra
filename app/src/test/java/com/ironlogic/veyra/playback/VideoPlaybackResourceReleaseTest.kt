package com.ironlogic.veyra.playback

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test

class VideoPlaybackResourceReleaseTest {
    @Test
    fun savesProgressBeforeReleasingPlayerAndHistory() = runBlocking {
        val events = mutableListOf<String>()

        releasePlaybackResources(
            saveFinalProgress = { events += "save" },
            releasePlayer = { events += "player" },
            closeHistory = { events += "history" }
        )

        assertEquals(listOf("save", "player", "history"), events)
    }

    @Test
    fun closesPlayerAndHistoryWhenFinalProgressSaveFails() = runBlocking {
        val events = mutableListOf<String>()

        try {
            releasePlaybackResources(
                saveFinalProgress = {
                    events += "save"
                    throw IllegalStateException("history write failed")
                },
                releasePlayer = { events += "player" },
                closeHistory = { events += "history" }
            )
            fail("The final progress failure should be propagated.")
        } catch (expected: IllegalStateException) {
            assertEquals("history write failed", expected.message)
        }

        assertEquals(listOf("save", "player", "history"), events)
    }

    @Test
    fun closesHistoryWhenPlayerReleaseFails() = runBlocking {
        val events = mutableListOf<String>()

        try {
            releasePlaybackResources(
                saveFinalProgress = { events += "save" },
                releasePlayer = {
                    events += "player"
                    throw IllegalStateException("player release failed")
                },
                closeHistory = { events += "history" }
            )
            fail("The player release failure should be propagated.")
        } catch (expected: IllegalStateException) {
            assertEquals("player release failed", expected.message)
        }

        assertEquals(listOf("save", "player", "history"), events)
    }
}
