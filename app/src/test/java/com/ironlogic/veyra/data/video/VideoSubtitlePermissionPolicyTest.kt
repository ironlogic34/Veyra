package com.ironlogic.veyra.data.video

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VideoSubtitlePermissionPolicyTest {
    @Test
    fun `releases the previous grant when a different subtitle is selected`() {
        assertEquals(
            "content://provider/old-subtitle",
            VideoSubtitlePermissionPolicy.uriToRelease(
                "content://provider/old-subtitle",
                "content://provider/new-subtitle",
            ),
        )
    }

    @Test
    fun `keeps the grant when the same subtitle remains selected`() {
        assertNull(
            VideoSubtitlePermissionPolicy.uriToRelease(
                "content://provider/subtitle",
                "content://provider/subtitle",
            ),
        )
    }

    @Test
    fun `has nothing to release when no previous subtitle exists`() {
        assertNull(VideoSubtitlePermissionPolicy.uriToRelease(null, "content://provider/subtitle"))
        assertNull(VideoSubtitlePermissionPolicy.uriToRelease("", null))
    }
}
