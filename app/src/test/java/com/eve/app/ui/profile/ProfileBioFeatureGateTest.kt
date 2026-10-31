package com.eve.app.ui.profile

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ProfileBioFeatureGateTest {
    @Test fun bioIsServerCapabilityGatedAndMustConfirmAuthenticatedGetAfterWrite() {
        val service = File("src/main/java/com/eve/app/data/remote/EveApiService.kt").readText()
        val profile = File("src/main/java/com/eve/app/ui/profile/ProfileActivity.kt").readText()
        val editor = File("src/main/java/com/eve/app/ui/profile/ChangeBioActivity.kt").readText()
        val manifest = File("src/main/AndroidManifest.xml").readText()
        assertTrue(service.contains("val bio: String? = null"))
        assertTrue(profile.contains("currentBio = profile.bio"))
        assertTrue(profile.contains("ProfileBioCapability.apply(binding.cellBio, binding.tvBioValue, profile.bio)"))
        val gate = File("src/main/java/com/eve/app/ui/profile/ProfileBioCapability.kt").readText()
        assertTrue(gate.contains("cell.isEnabled = authenticatedBio != null"))
        assertTrue(gate.contains("authenticatedBio == null ->"))
        assertTrue(gate.contains("authenticatedBio.isBlank() ->"))
        assertTrue(profile.contains("val bio = currentBio ?: return@setOnClickListener"))
        assertTrue(editor.contains("val original = intent.getStringExtra(\"current_bio\")"))
        assertTrue(editor.contains("if (original == null)"))
        assertTrue(editor.contains("ApiClient.apiService.updateProfile(mapOf(\"bio\" to bio))"))
        assertTrue(editor.contains("val readback = ApiClient.apiService.getProfile()"))
        assertTrue(editor.contains("ProfileSaveConfirmation.matches("))
        assertTrue(editor.contains("userId, FirebaseAuth.getInstance().currentUser?.uid"))
        assertTrue(editor.contains("readback.success, bio, readback.data?.bio"))
        assertTrue(editor.contains("setResult(Activity.RESULT_OK)"))
        assertTrue(manifest.contains("android:name=\".ui.profile.ChangeBioActivity\""))
    }

    @Test fun bioEditorIsDistinctFromDeletedAboutNavigationAndUsesTypographyRoles() {
        val profile = File("src/main/res/layout/activity_profile.xml").readText()
        val editor = File("src/main/res/layout/activity_change_bio.xml").readText()
        assertTrue(profile.contains("android:id=\"@+id/cellBio\""))
        assertTrue(profile.contains("android:id=\"@+id/tvBioValue\""))
        assertTrue(profile.contains("android:id=\"@+id/tvBioLabel\""))
        assertFalse(profile.contains("android:id=\"@+id/cellAbout\""))
        assertTrue(editor.contains("android:id=\"@+id/etBio\""))
        assertTrue(editor.contains("android:maxLength=\"160\""))
        assertTrue(editor.contains("android:inputType=\"textCapSentences|textMultiLine\""))
        assertTrue(editor.contains("style=\"@style/TextAppearance.Eve.Telegram.EditInput\""))
        assertFalse(editor.contains("android:textSize="))
        assertFalse(profile.contains("android:textSize="))
    }
}
