package com.eve.app.ui.profile

import android.app.Application
import android.text.SpannedString
import com.eve.app.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class ProfilePhotoInputContractTest {

    @Test fun sourceMenuAlwaysOffersCameraThenGalleryAndDeleteOnlyWithAvatar() {
        val withoutAvatar = ProfilePhotoMenuModel.items(hasAvatar = false)
        assertEquals(
            listOf(ProfilePhotoMenuModel.Action.CAMERA, ProfilePhotoMenuModel.Action.GALLERY),
            withoutAvatar.map { it.action }
        )
        assertEquals(R.drawable.ic_camera, withoutAvatar[0].iconRes)
        assertEquals(R.drawable.msg_photos, withoutAvatar[1].iconRes)
        assertTrue(withoutAvatar.none { it.destructive })

        val withAvatar = ProfilePhotoMenuModel.items(hasAvatar = true)
        assertEquals(
            listOf(
                ProfilePhotoMenuModel.Action.CAMERA,
                ProfilePhotoMenuModel.Action.GALLERY,
                ProfilePhotoMenuModel.Action.REMOVE
            ),
            withAvatar.map { it.action }
        )
        assertEquals(R.drawable.ic_delete, withAvatar[2].iconRes)
        assertTrue(withAvatar[2].destructive)
    }

    @Test fun photoMenuRetainsTelegramBottomSheetItemGeometry() {
        val source = java.io.File("src/main/java/com/eve/app/ui/profile/ProfilePhotoMenu.kt").readText()
        assertTrue(source.contains("dp(56f)")) // icon column
        assertTrue(source.contains("dp(48f)")) // title and row height
        assertTrue(source.contains("setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)"))
        assertTrue(source.contains("setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)"))
        assertTrue(source.contains("profile_remove_photo"))
    }

    @Test fun cameraActionUsesTempFileProviderAndReusesPreviewPipeline() {
        val source = java.io.File("src/main/java/com/eve/app/ui/profile/ProfileActivity.kt").readText()
        assertTrue(source.contains("ActivityResultContracts.TakePicture()"))
        assertTrue(source.contains("ActivityResultContracts.RequestPermission()"))
        assertTrue(source.contains("ActivityResultContracts.StartActivityForResult()"))
        assertTrue(source.contains("FileProvider.getUriForFile"))
        assertTrue(source.contains("launchAvatarCrop(cameraPhotoUri!!)"))
        assertTrue(source.contains("AvatarCropActivity.intent(this, uri)"))
        assertTrue(source.contains("STATE_CAMERA_URI"))
        assertTrue(source.contains("STATE_CAMERA_FILE"))
        assertTrue(source.contains("isChangingConfigurations"))
        val remove = source.substringAfter("private fun removeProfilePhoto()")
            .substringBefore("private fun clearCameraTemp()")
        assertTrue(remove.contains("ProfilePhotoManager.removePhoto"))
        assertTrue(remove.indexOf("hasCustomPhoto") < remove.indexOf("removePhoto"))
    }

    @Test fun bioEditorPortsCodepointLimitAndNewlineDoneBehavior() {
        val filter = CodepointsLengthInputFilter(5)
        // Supplementary characters count once even though they are two UTF-16 units.
        assertNull(filter.filter("😀", 0, 2, SpannedString("abcd"), 4, 4))
        assertEquals("", filter.filter("x", 0, 1, SpannedString("abcde"), 5, 5))
        // Rejecting at a surrogate boundary must not split the emoji.
        assertEquals("", filter.filter("😀😀", 0, 4, SpannedString("abcd"), 4, 4))
        // A partial insertion keeps only the codepoints that fit.
        assertEquals("d", filter.filter("de", 0, 2, SpannedString("abcd"), 4, 4).toString())

        // EVE's Worker counts UTF-16 units, so the combined filter must stop at
        // 160 units even though 160 code points would allow twice as many emoji.
        val combined = BioLengthInputFilter()
        val eightyEmoji = "😀".repeat(80)
        assertEquals(0, BioLengthInputFilter.remaining(eightyEmoji))
        val extra = "😀"
        assertEquals("", combined.filter(extra, 0, extra.length, SpannedString(eightyEmoji), 160, 160))
        assertEquals(2, BioLengthInputFilter.remaining("😀".repeat(79)))

        val editor = java.io.File("src/main/java/com/eve/app/ui/profile/ChangeBioActivity.kt").readText()
        assertTrue(editor.contains("source.indexOf('\\n') == source.length - 1"))
        assertTrue(editor.contains("TelegramShake.shakeView(binding.tvCount)"))
        assertTrue(editor.contains("VibrationHelper.vibrateOneShot(this, 200)"))
        assertTrue(editor.contains("EditorInfo.IME_ACTION_DONE"))
        assertTrue(editor.contains("binding.etBio.isEnabled = false"))

        // ChangeBioActivity.java:104,135: max four lines and 36dp minimum,
        // without the old three-line minimum.
        val layout = java.io.File("src/main/res/layout/activity_change_bio.xml").readText()
        assertTrue(layout.contains("android:minHeight=\"36dp\""))
        assertTrue(layout.contains("android:maxLines=\"4\""))
        assertFalse(layout.contains("android:minLines"))
    }
}
