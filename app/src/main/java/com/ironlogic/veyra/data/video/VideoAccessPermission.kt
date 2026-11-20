package com.ironlogic.veyra.data.video

import android.Manifest
import android.os.Build

enum class VideoAccessState {
    NONE,
    SELECTED,
    FULL,
}

/** Models Android's full-library and user-selected video access permissions. */
object VideoAccessPermission {
    fun requestedPermissions(sdkInt: Int): Array<String> = when {
        sdkInt >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> arrayOf(
            Manifest.permission.READ_MEDIA_VIDEO,
            Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED,
        )
        sdkInt >= Build.VERSION_CODES.TIRAMISU -> arrayOf(
            Manifest.permission.READ_MEDIA_VIDEO,
        )
        else -> arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
        )
    }

    fun resolve(sdkInt: Int, grantedPermissions: Set<String>): VideoAccessState = when {
        sdkInt >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE &&
            Manifest.permission.READ_MEDIA_VIDEO in grantedPermissions -> VideoAccessState.FULL

        sdkInt >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE &&
            Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED in grantedPermissions -> VideoAccessState.SELECTED

        sdkInt >= Build.VERSION_CODES.TIRAMISU &&
            Manifest.permission.READ_MEDIA_VIDEO in grantedPermissions -> VideoAccessState.FULL

        sdkInt < Build.VERSION_CODES.TIRAMISU &&
            Manifest.permission.READ_EXTERNAL_STORAGE in grantedPermissions -> VideoAccessState.FULL

        else -> VideoAccessState.NONE
    }
}
