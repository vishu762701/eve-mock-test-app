package com.eve.app.ui.profile

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Actual bounds algorithm tests, not a string-only scanner contract. */
class QrScanImageSizingTest {
    @Test fun smallQrImagesKeepFullResolution() {
        assertEquals(1, QrScanImageSizing.sampleSize(320, 320))
        assertEquals(1, QrScanImageSizing.sampleSize(1080, 1920))
    }

    @Test fun highResolutionGalleryImagesGetBoundedSampling() {
        assertEquals(2, QrScanImageSizing.sampleSize(4000, 3000))
        assertEquals(4, QrScanImageSizing.sampleSize(8000, 6000))
        assertEquals(8, QrScanImageSizing.sampleSize(16384, 12000))
    }

    @Test fun invalidOrExcessiveInputsFailClosedWithoutOverflow() {
        assertEquals(0, QrScanImageSizing.sampleSize(0, 100))
        assertEquals(0, QrScanImageSizing.sampleSize(-1, 100))
        assertEquals(0, QrScanImageSizing.sampleSize(100, 0))
        assertEquals(0, QrScanImageSizing.sampleSize(Int.MAX_VALUE, Int.MAX_VALUE))
        assertEquals(0, QrScanImageSizing.sampleSize(30000, 30000))
    }

    @Test fun selectedSampleAlwaysBoundsDecodedDimensions() {
        for (width in listOf(1, 64, 1024, 2048, 2049, 4096, 8192, 15000)) {
            for (height in listOf(1, 512, 1025, 2048, 4096, 12000)) {
                val sample = QrScanImageSizing.sampleSize(width, height)
                if (sample == 0) continue
                assertEquals(0, sample and (sample - 1))
                assertTrue(width.toLong() / sample <= QrScanImageSizing.MAX_SIDE)
                assertTrue(height.toLong() / sample <= QrScanImageSizing.MAX_SIDE)
                assertTrue(width.toLong() / sample * (height.toLong() / sample) <= QrScanImageSizing.MAX_PIXELS)
            }
        }
    }
}
