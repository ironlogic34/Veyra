package com.ironlogic.veyra.data.video

import org.junit.Assert.assertEquals
import org.junit.Test

class VideoLibrarySettingsTest {
    @Test
    fun parsesEverySupportedSortOrder() {
        VideoSortOrder.entries.forEach { order ->
            assertEquals(order, parseVideoSortOrder(order.name))
        }
    }

    @Test
    fun missingOrInvalidSortOrderUsesNewestFirst() {
        assertEquals(VideoSortOrder.NEWEST_FIRST, parseVideoSortOrder(null))
        assertEquals(VideoSortOrder.NEWEST_FIRST, parseVideoSortOrder("unknown"))
        assertEquals(VideoSortOrder.NEWEST_FIRST, parseVideoSortOrder("name_a_to_z"))
    }

    @Test
    fun librarySettingsKeepNewestFirstAsTheDefault() {
        assertEquals(
            VideoLibrarySettings(sortOrder = VideoSortOrder.NEWEST_FIRST),
            VideoLibrarySettings()
        )
    }
}
