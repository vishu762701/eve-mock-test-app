package com.eve.app.ui.profile

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ProfileAdminStatusContractTest {
    @Test fun adminRoleIsRenderedOnceAndOnlineIsAuthenticatedNotFaked() {
        val activity = File("src/main/java/com/eve/app/ui/profile/ProfileActivity.kt").readText()
        val layout = File("src/main/res/layout/activity_profile.xml").readText()
        assertTrue(activity.contains("binding.chipAdmin.visibility = View.VISIBLE"))
        assertTrue(activity.contains("binding.tvStatus.text = ProfilePresenceStatus.label(profile.isOnline, eveId)"))
        val status = File("src/main/java/com/eve/app/ui/profile/ProfilePresenceStatus.kt").readText()
        assertTrue(status.contains("true -> \"online\""))
        assertTrue(status.contains("false -> \"offline\""))
        assertTrue(status.contains("null -> if (eveId != \"--\")"))
        assertFalse(activity.contains("binding.tvStatus.text = \"Admin\""))
        assertFalse(activity.contains("binding.tvStatus.text = \"online\""))
        assertFalse(activity.contains("binding.cellEditAbout"))
        assertFalse(layout.contains("cellEditAbout"))
        assertFalse(layout.contains("Edit About Content (Admin)"))
    }

    @Test fun adminContentEditorRemainsAvailableOutsideProfile() {
        val admin = File("src/main/java/com/eve/app/ui/admin/AdminActivity.kt").readText()
        assertTrue(admin.contains("binding.rowEditAbout.setOnClickListener"))
        assertTrue(admin.contains("startActivity(Intent(this, EditAboutActivity::class.java))"))
    }
}
