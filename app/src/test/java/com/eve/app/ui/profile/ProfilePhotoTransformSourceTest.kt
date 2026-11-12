package com.eve.app.ui.profile

import com.eve.app.util.ExifTransform
import com.eve.app.util.ProfilePhotoManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure JVM contract for the Telegram-derived photo transform. Robolectric's
 * native runtime is unavailable on this aarch64 host, so the mapping/sample
 * checks live in a runner that does not require an Android sandbox.
 */
class ProfilePhotoTransformSourceTest {

    @Test fun exifOrientationMatchesTelegramAndIsBakedIntoTheSavedAvatar() {
        val expected = mapOf(
            1 to ExifTransform.Orientation(0, 0),
            2 to ExifTransform.Orientation(0, 1),
            3 to ExifTransform.Orientation(180, 0),
            4 to ExifTransform.Orientation(0, 2),
            5 to ExifTransform.Orientation(270, 2),
            6 to ExifTransform.Orientation(90, 0),
            7 to ExifTransform.Orientation(270, 1),
            8 to ExifTransform.Orientation(270, 0),
            9 to ExifTransform.Orientation(0, 0)
        )
        for ((exif, orientation) in expected) {
            assertEquals("EXIF $exif", orientation, ExifTransform.fromExifValue(exif))
        }

        val manager = java.io.File("src/main/java/com/eve/app/util/ProfilePhotoManager.kt").readText()
        assertTrue(manager.contains("ExifTransform.read(bytes)"))
        assertTrue(manager.contains("ExifTransform.apply(original, orientation)"))
        assertTrue(manager.contains("fun decodeForCrop("))
        assertTrue(manager.contains("fun saveCroppedPhoto("))
        assertTrue(manager.contains("!square || !bounded"))
        // Telegram ImageUpdater.java:854-855 exports 800x800 at quality 80.
        assertTrue(manager.contains("800"))
        assertTrue(manager.contains("CompressFormat.JPEG, 80"))

        // Sampled decode keeps at least 800px on the shorter side; the source
        // image is then center-cropped and scaled to the 800px export.
        assertEquals(1, ProfilePhotoManager.sampleSizeFor(800, 800))
        assertEquals(2, ProfilePhotoManager.sampleSizeFor(1600, 1600))
        assertEquals(2, ProfilePhotoManager.sampleSizeFor(3000, 3000))
        assertEquals(8, ProfilePhotoManager.sampleSizeFor(12000, 8000))
        assertEquals(1, ProfilePhotoManager.sampleSizeFor(4000, 1000))
        assertEquals(1, ProfilePhotoManager.sampleSizeFor(0, 0))
        // A failed/interrupted save must not truncate the previous verified avatar.
        assertTrue(manager.contains("destination.name + \".tmp\""))
        assertTrue(manager.contains("temp.renameTo(destination)"))
    }
}
