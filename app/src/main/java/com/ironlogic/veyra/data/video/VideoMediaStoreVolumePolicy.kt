package com.ironlogic.veyra.data.video

object VideoMediaStoreVolumePolicy {
    fun usesAggregateExternalVolume(sdkInt: Int): Boolean = sdkInt >= 29
}
