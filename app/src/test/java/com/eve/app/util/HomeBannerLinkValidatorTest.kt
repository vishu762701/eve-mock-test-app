package com.eve.app.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeBannerLinkValidatorTest {

    @Test
    fun optionalLinkAllowsOnlyTheEmptyPairOrAUsableLabel() {
        assertNull(HomeBannerLinkValidator.validate("", ""))
        assertNull(
            HomeBannerLinkValidator.validate(
                "https://example.com/practice",
                "Start Practicing"
            )
        )
        assertNotNull(HomeBannerLinkValidator.validate("https://example.com", ""))
        assertNotNull(HomeBannerLinkValidator.validate("", "Start Practicing"))
    }

    @Test
    fun onlySafeHttpLinksAndLabelsWithinLimitAreAccepted() {
        assertTrue(HomeBannerLinkValidator.isValidUrl("https://example.com/path?q=1"))
        assertTrue(HomeBannerLinkValidator.isValidUrl("http://example.com"))
        assertFalse(HomeBannerLinkValidator.isValidUrl("javascript:alert(1)"))
        assertFalse(HomeBannerLinkValidator.isValidUrl("https:///missing-host"))
        assertFalse(HomeBannerLinkValidator.isValidUrl("https://user:pass@example.com"))
        assertNotNull(HomeBannerLinkValidator.validate("https://example.com", "x".repeat(49)))
    }
}
