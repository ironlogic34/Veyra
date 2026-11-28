package com.ironlogic.veyra.ui.layout

/**
 * Chooses the dedicated tablet shell only for wide landscape windows.
 * Large portrait windows continue to use the phone experience.
 */
object VeyraLayoutModePolicy {
    const val TABLET_MIN_WIDTH_DP = 600

    fun isTabletLandscape(screenWidthDp: Int, screenHeightDp: Int): Boolean =
        screenWidthDp >= TABLET_MIN_WIDTH_DP && screenWidthDp > screenHeightDp
}
