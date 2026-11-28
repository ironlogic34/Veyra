package com.ironlogic.veyra.data.video

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VideoMediaStoreVolumePolicyTest {
    @Test
    fun `pre Android 10 uses the legacy external collection`() {
        assertFalse(VideoMediaStoreVolumePolicy.usesAggregateExternalVolume(28))
    }

    @Test
    fun `Android 10 and newer use the aggregate external collection`() {
        assertTrue(VideoMediaStoreVolumePolicy.usesAggregateExternalVolume(29))
        assertTrue(VideoMediaStoreVolumePolicy.usesAggregateExternalVolume(36))
    }
}
