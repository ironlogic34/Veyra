package com.ironlogic.veyra.data.video

enum class VideoLockGesture {
    TAP,
    LONG_PRESS,
}

object VideoPlaybackLockPolicy {
    fun shouldUnlock(gesture: VideoLockGesture): Boolean =
        gesture == VideoLockGesture.LONG_PRESS
}
