package com.ironlogic.veyra.data.video

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VideoProgressWritePolicyTest {
    @Test
    fun `small forward movement does not trigger a write`() {
        assertFalse(VideoProgressWritePolicy.shouldPersist(4_999L, 0L))
    }

    @Test
    fun `threshold forward movement triggers a write`() {
        assertTrue(VideoProgressWritePolicy.shouldPersist(5_000L, 0L))
    }

    @Test
    fun `backward seek triggers a write`() {
        assertTrue(VideoProgressWritePolicy.shouldPersist(30_000L, 40_000L))
    }

    @Test
    fun `unchanged and invalid positions do not trigger a write`() {
        assertFalse(VideoProgressWritePolicy.shouldPersist(10_000L, 10_000L))
        assertFalse(VideoProgressWritePolicy.shouldPersist(-1L, 10_000L))
    }
}
