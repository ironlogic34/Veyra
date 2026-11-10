package com.ironlogic.veyra.data.video

import org.junit.Assert.assertEquals
import org.junit.Test

class VideoDisplayFormattersTest {
    @Test
    fun formatsDurationAsMinutesAndSeconds() {
        assertEquals("2:05", VideoDisplayFormatters.formatDuration(125_000L))
    }

    @Test
    fun formatsLongDurationWithHours() {
        assertEquals("1:02:03", VideoDisplayFormatters.formatDuration(3_723_000L))
    }

    @Test
    fun treatsMissingDurationAsUnknown() {
        assertEquals("Unknown", VideoDisplayFormatters.formatDuration(0L))
        assertEquals("Unknown", VideoDisplayFormatters.formatDuration(-1L))
    }

    @Test
    fun formatsResolutionAndAccountsForQuarterTurnRotation() {
        assertEquals("1920 × 1080", VideoDisplayFormatters.formatResolution(1920, 1080))
        assertEquals("1080 × 1920", VideoDisplayFormatters.formatResolution(1920, 1080, 90))
        assertEquals("1080 × 1920", VideoDisplayFormatters.formatResolution(1920, 1080, 450))
    }

    @Test
    fun treatsIncompleteOrInvalidResolutionAsUnknown() {
        assertEquals("Unknown", VideoDisplayFormatters.formatResolution(null, 1080))
        assertEquals("Unknown", VideoDisplayFormatters.formatResolution(1920, null))
        assertEquals("Unknown", VideoDisplayFormatters.formatResolution(0, 1080))
    }

    @Test
    fun formatsFileSizeUsingBinaryUnits() {
        assertEquals("512 B", VideoDisplayFormatters.formatFileSize(512L))
        assertEquals("1.0 KiB", VideoDisplayFormatters.formatFileSize(1_024L))
        assertEquals("1.5 MiB", VideoDisplayFormatters.formatFileSize(1_572_864L))
    }

    @Test
    fun treatsMissingOrInvalidFileSizeAsUnknown() {
        assertEquals("Unknown", VideoDisplayFormatters.formatFileSize(0L))
        assertEquals("Unknown", VideoDisplayFormatters.formatFileSize(-1L))
    }
}
