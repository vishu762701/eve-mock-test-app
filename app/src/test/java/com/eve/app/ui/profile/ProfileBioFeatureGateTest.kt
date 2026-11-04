package com.eve.app.ui.profile

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ProfileBioFeatureGateTest {
    @Test fun profileDoesNotExposeBioOrItsEditor() {
        val layout = File("src/main/res/layout/activity_profile.xml").readText()
        val profile = File("src/main/java/com/eve/app/ui/profile/ProfileActivity.kt").readText()
        for (id in listOf("@+id/cellBio", "@+id/bioDivider", "@+id/tvBioValue", "@+id/tvBioLabel")) {
            assertFalse(layout.contains(id))
        }
        assertFalse(profile.contains("editBioLauncher"))
        assertFalse(profile.contains("binding.cellBio"))
        assertFalse(profile.contains("currentBio"))
        assertTrue(layout.contains("@+id/cellUsername"))
        assertTrue(layout.contains("@+id/cellBirthday"))
    }
    @Test fun removedBioPlaceholderHasNoCompetingRuntimeController() {
        // The hidden Add bio implementation was orphaned; do not recreate a
        // dormant second Profile UI owner while server bio compatibility remains.
        assertFalse(File("src/main/java/com/eve/app/ui/profile/ProfileBioCapability.kt").exists())
    }

    @Test fun existingBackendBioContractIsPreserved() {
        val service = File("src/main/java/com/eve/app/data/remote/EveApiService.kt").readText()
        assertTrue(service.contains("val bio: String? = null"))
    }
}
