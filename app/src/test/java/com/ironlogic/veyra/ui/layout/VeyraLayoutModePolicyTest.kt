package com.ironlogic.veyra.ui.layout

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VeyraLayoutModePolicyTest {
    @Test
    fun `wide landscape windows use the tablet layout`() {
        assertTrue(VeyraLayoutModePolicy.isTabletLandscape(600, 400))
        assertTrue(VeyraLayoutModePolicy.isTabletLandscape(960, 600))
    }

    @Test
    fun `large portrait windows keep the phone layout`() {
        assertFalse(VeyraLayoutModePolicy.isTabletLandscape(800, 1_200))
    }

    @Test
    fun `narrow landscape windows keep the phone layout`() {
        assertFalse(VeyraLayoutModePolicy.isTabletLandscape(599, 400))
    }

    @Test
    fun `square windows keep the phone layout`() {
        assertFalse(VeyraLayoutModePolicy.isTabletLandscape(800, 800))
    }
}
