package com.ironlogic.veyra.data.video

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VideoLibraryRefreshGateTest {
    @Test
    fun `first request starts a scan`() {
        val gate = VideoLibraryRefreshGate()

        assertTrue(gate.request())
        assertFalse(gate.finish())
        assertTrue(gate.request())
    }

    @Test
    fun `requests during a scan coalesce into one follow-up scan`() {
        val gate = VideoLibraryRefreshGate()

        assertTrue(gate.request())
        assertFalse(gate.request())
        assertFalse(gate.request())
        assertTrue(gate.finish())
        assertFalse(gate.finish())
        assertTrue(gate.request())
    }

    @Test
    fun `cancel releases the gate and discards pending work`() {
        val gate = VideoLibraryRefreshGate()

        assertTrue(gate.request())
        assertFalse(gate.request())
        gate.cancel()

        assertTrue(gate.request())
    }
}
