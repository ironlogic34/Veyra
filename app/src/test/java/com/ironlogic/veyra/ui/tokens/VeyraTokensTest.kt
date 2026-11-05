package com.ironlogic.veyra.ui.tokens

import org.junit.Assert.assertEquals
import org.junit.Test

class VeyraTokensTest {
    @Test
    fun spacingScaleIsStrictlyIncreasing() {
        val spacing = listOf(
            VeyraSpacing.xxs,
            VeyraSpacing.xs,
            VeyraSpacing.sm,
            VeyraSpacing.md,
            VeyraSpacing.lg,
            VeyraSpacing.xl,
            VeyraSpacing.xxl,
            VeyraSpacing.xxxl,
            VeyraSpacing.huge
        )

        assertEquals(true, spacing.zipWithNext().all { (a, b) -> a < b })
    }

    @Test
    fun motionDurationsAreOrdered() {
        assertEquals(true, VeyraMotion.Quick < VeyraMotion.Standard)
        assertEquals(true, VeyraMotion.Standard < VeyraMotion.Emphasized)
    }
}
