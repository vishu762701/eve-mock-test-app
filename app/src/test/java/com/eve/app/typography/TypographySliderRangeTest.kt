package com.eve.app.typography

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TypographySliderRangeTest {
    @Test fun usesExistingResolverBounds() {
        assertEquals(8f, TypographyResolver.MIN_TEXT_SIZE_SP, 0f)
        assertEquals(60f, TypographyResolver.MAX_TEXT_SIZE_SP, 0f)
        assertTrue(TypographyResolver.MIN_TEXT_SIZE_SP < TypographyResolver.MAX_TEXT_SIZE_SP)
    }

    @Test fun fractionalSizeIsNotForcedToIntegerWhenSynchronizingSlider() {
        listOf(10.125f, 13.75f, 17.5f, 32.25f).forEach { size ->
            assertEquals(size, size.coerceIn(
                TypographyResolver.MIN_TEXT_SIZE_SP, TypographyResolver.MAX_TEXT_SIZE_SP
            ), 0f)
        }
    }
}
