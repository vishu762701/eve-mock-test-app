package com.eve.app.ui.profile

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AvatarCropGeometryTest {

    private val inset = 14f

    @Test fun fitImagePreservesAspectRatioAndCentersInsideInset() {
        val landscape = AvatarCropGeometry.fitImage(2000, 1000, 1000, 800, inset)
        assertEquals(14f, landscape.left, 0.01f)
        assertEquals(972f, landscape.width, 0.01f)
        assertEquals(486f, landscape.height, 0.01f)
        assertEquals(157f, landscape.top, 0.01f)

        val portrait = AvatarCropGeometry.fitImage(1000, 2000, 1000, 800, inset)
        assertEquals(386f, portrait.width, 0.01f)
        assertEquals(772f, portrait.height, 0.01f)
        assertEquals(307f, portrait.left, 0.01f)
        assertEquals(14f, portrait.top, 0.01f)
    }

    @Test fun initialCropIsCenteredSquareUsingShorterSide() {
        val landscape = AvatarCropGeometry.initialSquare(
            AvatarCropGeometry.Box(0f, 0f, 1000f, 500f)
        )
        assertEquals(250f, landscape.left, 0.01f)
        assertEquals(0f, landscape.top, 0.01f)
        assertEquals(500f, landscape.width, 0.01f)
        assertEquals(500f, landscape.height, 0.01f)

        val portrait = AvatarCropGeometry.initialSquare(
            AvatarCropGeometry.Box(0f, 0f, 400f, 900f)
        )
        assertEquals(0f, portrait.left, 0.01f)
        assertEquals(250f, portrait.top, 0.01f)
        assertEquals(400f, portrait.width, 0.01f)
    }

    @Test fun moveClampsSelectionInsideTheFittedBitmap() {
        val image = AvatarCropGeometry.Box(10f, 20f, 610f, 420f)
        val crop = AvatarCropGeometry.Box(210f, 70f, 410f, 270f)

        val moved = AvatarCropGeometry.move(crop, image, -1000f, -1000f)
        assertEquals(10f, moved.left, 0.01f)
        assertEquals(20f, moved.top, 0.01f)
        assertEquals(200f, moved.width, 0.01f)

        val movedBack = AvatarCropGeometry.move(crop, image, 1000f, 1000f)
        assertEquals(410f, movedBack.left, 0.01f)
        assertEquals(220f, movedBack.top, 0.01f)
        assertEquals(200f, movedBack.width, 0.01f)
    }

    @Test fun cornerResizeKeepsOppositeCornerAndSquareMinimum() {
        val image = AvatarCropGeometry.Box(0f, 0f, 500f, 500f)
        val crop = AvatarCropGeometry.Box(100f, 100f, 300f, 300f)

        val grown = AvatarCropGeometry.resize(
            crop, image, AvatarCropGeometry.Corner.BOTTOM_RIGHT, 100f, 10f, 96f
        )
        assertEquals(100f, grown.left, 0.01f)
        assertEquals(100f, grown.top, 0.01f)
        assertEquals(400f, grown.right, 0.01f)
        assertEquals(400f, grown.bottom, 0.01f)

        val shrunk = AvatarCropGeometry.resize(
            crop, image, AvatarCropGeometry.Corner.TOP_LEFT, 1000f, 1000f, 96f
        )
        assertEquals(96f, shrunk.width, 0.01f)
        assertEquals(300f, shrunk.right, 0.01f)
        assertEquals(300f, shrunk.bottom, 0.01f)

        val clamped = AvatarCropGeometry.resize(
            crop, image, AvatarCropGeometry.Corner.TOP_LEFT, -1000f, -1000f, 96f
        )
        assertEquals(0f, clamped.left, 0.01f)
        assertEquals(0f, clamped.top, 0.01f)
        assertEquals(300f, clamped.right, 0.01f)
        assertEquals(300f, clamped.bottom, 0.01f)
    }

    @Test fun visibleCropMapsToSourcePixelsOnBothAxes() {
        val image = AvatarCropGeometry.Box(0f, 0f, 1000f, 500f)
        val crop = AvatarCropGeometry.Box(250f, 0f, 750f, 500f)
        val mapped = AvatarCropGeometry.mapToImage(crop, image, 2000, 1000)
        assertEquals(500, mapped.x)
        assertEquals(0, mapped.y)
        assertEquals(1000, mapped.width)
        assertEquals(1000, mapped.height)
    }

    @Test fun invalidInputsFailClosed() {
        assertTrue(!AvatarCropGeometry.fitImage(0, 0, 100, 100, inset).isValid)
        assertTrue(!AvatarCropGeometry.initialSquare(AvatarCropGeometry.Box(0f, 0f, 0f, 0f)).isValid)
        val mapped = AvatarCropGeometry.mapToImage(
            AvatarCropGeometry.Box(0f, 0f, 0f, 0f),
            AvatarCropGeometry.Box(0f, 0f, 100f, 100f),
            100,
            100
        )
        assertEquals(AvatarCropGeometry.IntBox(0, 0, 0, 0), mapped)
    }

    @Test fun cropHostRetainsSourceGeometryAndExportContracts() {
        val view = java.io.File(
            "src/main/java/com/eve/app/ui/profile/AvatarCropView.kt"
        ).readText()
        assertTrue(view.contains("0xFF333333"))
        assertTrue(view.contains("0xC8000000"))
        assertTrue(view.contains("0x3FFAFAFA"))
        assertTrue(view.contains("0xFFFFFFFF"))
        assertTrue(view.contains("20f * density")) // source 20px corner handles
        assertTrue(view.contains("AvatarCropGeometry.resize"))
        assertTrue(view.contains("Bitmap.CompressFormat.JPEG, 80") || view.contains("cropBitmap"))

        val activity = java.io.File(
            "src/main/java/com/eve/app/ui/profile/AvatarCropActivity.kt"
        ).readText()
        assertTrue(activity.contains("ProfilePhotoManager.decodeForCrop"))
        assertTrue(activity.contains("Bitmap.CompressFormat.JPEG, 80"))
        assertTrue(activity.contains("avatar_crop_"))
        assertTrue(activity.contains("EXTRA_OUTPUT_PATH"))
        assertTrue(activity.contains("temp.renameTo(output)"))

        val profile = java.io.File(
            "src/main/java/com/eve/app/ui/profile/ProfileActivity.kt"
        ).readText()
        assertTrue(profile.contains("AvatarCropActivity.intent(this, uri)"))
        assertTrue(profile.contains("ProfilePhotoManager.saveCroppedPhoto"))
        assertTrue(profile.contains("pendingPhotoCropped"))

        val layout = java.io.File(
            "src/main/res/layout/activity_avatar_crop.xml"
        ).readText()
        assertTrue(layout.contains("AvatarCropView"))
        assertTrue(layout.contains("@string/profile_crop_title"))

        val manifest = java.io.File("src/main/AndroidManifest.xml").readText()
        assertTrue(manifest.contains(".ui.profile.AvatarCropActivity"))
    }
}
