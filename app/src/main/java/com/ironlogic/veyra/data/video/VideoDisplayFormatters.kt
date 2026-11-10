package com.ironlogic.veyra.data.video

import java.util.Locale

/** Formats video metadata for display without depending on Android UI classes. */
internal object VideoDisplayFormatters {
    fun formatDuration(durationMs: Long): String {
        if (durationMs <= 0L) return "Unknown"

        val totalSeconds = durationMs / 1_000L
        val seconds = totalSeconds % 60L
        val minutes = (totalSeconds / 60L) % 60L
        val hours = totalSeconds / 3_600L

        return if (hours > 0L) {
            String.format(Locale.ROOT, "%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.ROOT, "%d:%02d", totalSeconds / 60L, seconds)
        }
    }

    fun formatResolution(width: Int?, height: Int?, rotationDegrees: Int? = null): String {
        if (width == null || height == null || width <= 0 || height <= 0) return "Unknown"

        val normalizedRotation = ((rotationDegrees ?: 0) % 360 + 360) % 360
        val isQuarterTurn = normalizedRotation == 90 || normalizedRotation == 270
        val displayWidth = if (isQuarterTurn) height else width
        val displayHeight = if (isQuarterTurn) width else height
        return "${displayWidth} × ${displayHeight}"
    }

    fun formatFileSize(sizeBytes: Long): String {
        if (sizeBytes <= 0L) return "Unknown"

        val units = arrayOf("B", "KiB", "MiB", "GiB", "TiB")
        var value = sizeBytes.toDouble()
        var unitIndex = 0
        while (value >= 1_024.0 && unitIndex < units.lastIndex) {
            value /= 1_024.0
            unitIndex++
        }

        return if (unitIndex == 0) {
            "$sizeBytes B"
        } else {
            String.format(Locale.ROOT, "%.1f %s", value, units[unitIndex])
        }
    }
}
