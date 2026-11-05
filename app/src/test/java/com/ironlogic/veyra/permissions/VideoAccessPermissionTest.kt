package com.ironlogic.veyra.permissions

import org.junit.Assert.assertEquals
import org.junit.Test

class VideoAccessPermissionTest {
    @Test fun android14FullPermissionWinsOverSelectedAccess() {
        assertEquals(VideoAccessLevel.FULL_LIBRARY, VideoAccessPermission.resolve(34, true, true, false))
    }

    @Test fun android14SupportsSelectedVideosOnly() {
        assertEquals(VideoAccessLevel.SELECTED_VIDEOS, VideoAccessPermission.resolve(34, false, true, false))
    }

    @Test fun android13UsesVideoPermission() {
        assertEquals(VideoAccessLevel.FULL_LIBRARY, VideoAccessPermission.resolve(33, true, false, false))
    }

    @Test fun android12AndEarlierUseLegacyPermission() {
        assertEquals(VideoAccessLevel.FULL_LIBRARY, VideoAccessPermission.resolve(32, false, false, true))
    }

    @Test fun missingPermissionDoesNotGrantLibraryAccess() {
        assertEquals(VideoAccessLevel.NONE, VideoAccessPermission.resolve(35, false, false, false))
    }
}
