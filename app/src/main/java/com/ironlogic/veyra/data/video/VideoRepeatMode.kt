package com.ironlogic.veyra.data.video

enum class VideoRepeatMode(
    val label: String,
    val storageValue: String,
) {
    OFF("Off", "off"),
    ONE("One", "one");

    fun next(): VideoRepeatMode = when (this) {
        OFF -> ONE
        ONE -> OFF
    }

    companion object {
        fun fromStorage(value: String?): VideoRepeatMode =
            values().firstOrNull { it.storageValue == value } ?: OFF
    }
}
