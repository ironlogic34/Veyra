package com.ironlogic.veyra.permissions

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build

enum class VideoAccessLevel { FULL_LIBRARY, SELECTED_VIDEOS, NONE }

object VideoAccessPermission {
    fun requiredPermissions(sdkInt: Int = Build.VERSION.SDK_INT): Array<String> =
        when {
            sdkInt >= 34 -> arrayOf(
                Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
            )
            sdkInt >= 33 -> arrayOf(Manifest.permission.READ_MEDIA_VIDEO)
            else -> arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }

    fun evaluate(context: Context): VideoAccessLevel = resolve(
        sdkInt = Build.VERSION.SDK_INT,
        hasVideoPermission = hasPermission(context, Manifest.permission.READ_MEDIA_VIDEO),
        hasSelectedVideosPermission = Build.VERSION.SDK_INT >= 34 &&
            hasPermission(context, Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED),
        hasLegacyPermission = Build.VERSION.SDK_INT <= 32 &&
            hasPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE)
    )

    internal fun resolve(
        sdkInt: Int,
        hasVideoPermission: Boolean,
        hasSelectedVideosPermission: Boolean,
        hasLegacyPermission: Boolean
    ): VideoAccessLevel = when {
        sdkInt >= 34 && hasVideoPermission -> VideoAccessLevel.FULL_LIBRARY
        sdkInt >= 34 && hasSelectedVideosPermission -> VideoAccessLevel.SELECTED_VIDEOS
        sdkInt == 33 && hasVideoPermission -> VideoAccessLevel.FULL_LIBRARY
        sdkInt <= 32 && hasLegacyPermission -> VideoAccessLevel.FULL_LIBRARY
        else -> VideoAccessLevel.NONE
    }

    private fun hasPermission(context: Context, permission: String): Boolean =
        context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
}
