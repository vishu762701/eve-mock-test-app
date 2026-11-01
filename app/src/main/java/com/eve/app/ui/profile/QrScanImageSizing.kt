package com.eve.app.ui.profile

/**
 * Bounds-only, power-of-two QR gallery sampling. Keep scanner allocations finite
 * before opening untrusted image streams for an actual bitmap decode.
 * EVE CameraX/ZXing adapter: not an imported Telegram scanner implementation.
 */
internal object QrScanImageSizing {
    const val MAX_SIDE = 2048
    const val MAX_PIXELS = 4_000_000L
    const val MAX_SOURCE_PIXELS = 240_000_000L

    /** Returns 0 for invalid/oversize dimensions; 1 for images needing no sampling. */
    fun sampleSize(width: Int, height: Int): Int {
        if (width <= 0 || height <= 0 ||
            width.toLong() * height > MAX_SOURCE_PIXELS) return 0
        var sample = 1
        while (width.toLong() / sample > MAX_SIDE ||
            height.toLong() / sample > MAX_SIDE ||
            (width.toLong() / sample) * (height.toLong() / sample) > MAX_PIXELS) {
            sample *= 2
        }
        return sample
    }
}
