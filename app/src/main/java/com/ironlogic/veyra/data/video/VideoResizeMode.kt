package com.ironlogic.veyra.data.video

enum class VideoResizeMode(
    val label: String,
    val storageValue: String,
) {
    FIT("Fit", "fit"),
    CROP("Crop", "crop");

    fun next(): VideoResizeMode = when (this) {
        FIT -> CROP
        CROP -> FIT
    }

    companion object {
        fun fromStorage(value: String?): VideoResizeMode =
            values().firstOrNull { it.storageValue == value } ?: FIT
    }
}
