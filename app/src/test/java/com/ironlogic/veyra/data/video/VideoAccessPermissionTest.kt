package com.ironlogic.veyra.data.video

import android.Manifest
import android.os.Build
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class VideoAccessPermissionTest {
    @Test
    fun `android 12 and earlier request legacy storage access`() {
        assertArrayEquals(
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
            VideoAccessPermission.requestedPermissions(Build.VERSION_CODES.S),
        )
    }

    @Test
    fun `android 13 requests video access only`() {
        assertArrayEquals(
            arrayOf(Manifest.permission.READ_MEDIA_VIDEO),
            VideoAccessPermission.requestedPermissions(Build.VERSION_CODES.TIRAMISU),
        )
    }

    @Test
    fun `android 14 requests both full and selected video access`() {
        assertArrayEquals(
            arrayOf(
                Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED,
            ),
            VideoAccessPermission.requestedPermissions(Build.VERSION_CODES.UPSIDE_DOWN_CAKE),
        )
    }

    @Test
    fun `android 14 recognizes selected-only access`() {
        assertEquals(
            VideoAccessState.SELECTED,
            VideoAccessPermission.resolve(
                Build.VERSION_CODES.UPSIDE_DOWN_CAKE,
                setOf(Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED),
            ),
        )
    }

    @Test
    fun `full access takes precedence when both permissions are granted`() {
        assertEquals(
            VideoAccessState.FULL,
            VideoAccessPermission.resolve(
                Build.VERSION_CODES.UPSIDE_DOWN_CAKE,
                setOf(
                    Manifest.permission.READ_MEDIA_VIDEO,
                    Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED,
                ),
            ),
        )
    }

    @Test
    fun `selected access is not treated as granted before android 14`() {
        assertEquals(
            VideoAccessState.NONE,
            VideoAccessPermission.resolve(
                Build.VERSION_CODES.TIRAMISU,
                setOf(Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED),
            ),
        )
    }

    @Test
    fun `missing permission returns no access`() {
        assertEquals(
            VideoAccessState.NONE,
            VideoAccessPermission.resolve(Build.VERSION_CODES.UPSIDE_DOWN_CAKE, emptySet()),
        )
    }
}
