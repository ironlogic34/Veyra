package com.ironlogic.veyra.data.video

import org.junit.Assert.assertEquals
import org.junit.Test

class VideoSeekPolicyTest {
    @Test
    fun clampsSliderPositionToKnownDuration() {
        assertEquals(0L, VideoSeekPolicy.clampSliderPosition(-20f, 90_000L))
        assertEquals(90_000L, VideoSeekPolicy.clampSliderPosition(100_000f, 90_000L))
    }

    @Test
    fun convertsPreviewPositionToWholeMilliseconds() {
        assertEquals(12_345L, VideoSeekPolicy.clampSliderPosition(12_345.9f, 90_000L))
    }

    @Test
    fun rejectsNonFiniteSliderValuesAndNegativeDuration() {
        assertEquals(0L, VideoSeekPolicy.clampSliderPosition(Float.NaN, 90_000L))
        assertEquals(0L, VideoSeekPolicy.clampSliderPosition(Float.POSITIVE_INFINITY, 90_000L))
        assertEquals(0L, VideoSeekPolicy.clampSliderPosition(10f, -1L))
    }
}
