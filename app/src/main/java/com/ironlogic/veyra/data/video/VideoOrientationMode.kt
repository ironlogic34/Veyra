package com.ironlogic.veyra.data.video

enum class VideoOrientationMode(
    val label: String,
    val storageValue: String,
) {
    SYSTEM("System default", "system"),
    PORTRAIT("Portrait", "portrait"),
    LANDSCAPE("Landscape", "landscape");

    companion object {
        fun fromStorage(value: String?): VideoOrientationMode =
            values().firstOrNull { it.storageValue == value } ?: SYSTEM
    }
}
