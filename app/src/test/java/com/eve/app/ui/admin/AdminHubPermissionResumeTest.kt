package com.eve.app.ui.admin

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Protected Hub refreshes the current Worker's rights without duplicating UI wiring. */
class AdminHubPermissionResumeTest {
    private val source = File("src/main/java/com/eve/app/ui/admin/AdminActivity.kt").readText()

    @Test fun returningHubRevalidatesUsingExistingGateBeforeShowingRows() {
        val stop = source.substringAfter("override fun onStop()").substringBefore("override fun onResume()")
        assertTrue(stop.contains("adminRightsRefreshOnResume = true"))
        assertTrue("Initial in-flight gate must also be refreshed after STOPPED",
            source.contains("if (adminRightsRefreshOnResume &&") &&
                source.contains("refreshAdminRightsAfterStop()"))
        val resume = source.substringAfter("override fun onResume()").substringBefore("override fun onDestroy()")
        assertTrue(resume.contains("refreshAdminRightsAfterStop()"))
        assertTrue(resume.contains("if (!adminUiReady || !adminRightsRefreshOnResume || isFinishing || isDestroyed) return"))
        assertTrue(resume.contains("AdminAccessGate.requireAdmin(this, binding.root, requireFreshServer = true)"))
        val gate = resume.indexOf("AdminAccessGate.requireAdmin(")
        val rights = resume.indexOf("applyAdminPermissions(permissions)")
        val data = resume.indexOf("loadData(permissions, refreshMaintenance = maintenanceDialog == null)")
        assertTrue(gate >= 0 && rights > gate && data > rights)
        assertFalse("Repeated resume must not re-register click listeners", resume.contains("setupHubSections()"))
        assertFalse("Repeated resume must not duplicate collectors", resume.contains("setupObservers()"))
    }

    @Test fun adminSearchRecomputesImmediatelyFromCurrentPermissions() {
        val resume = source.substringAfter("override fun onResume()").substringBefore("override fun onDestroy()")
        assertTrue(resume.contains("binding.telegramActionBar.isSearchExpanded"))
        assertTrue(resume.contains("performSearch(binding.telegramActionBar.searchEditText.text?.toString().orEmpty())"))
        assertTrue(source.contains("searchItems.clear()"))
    }

    @Test fun ownerPromotionRestoresPreviouslyHiddenRows() {
        val apply = source.substringAfter("private fun applyAdminPermissions(")
            .substringBefore("private fun performSearch(")
        assertTrue(apply.contains("fun has(right: String): Boolean = adminMe.isOwner || adminMe.rights.contains(right)"))
        assertTrue(apply.contains("binding.rowUsers.visibility = if (has(\"manage_users\"))"))
        assertTrue(apply.contains("binding.rowExams.visibility = if (has(\"manage_exams\"))"))
        assertTrue(apply.contains("binding.rowPolls.visibility = if (has(\"moderate_feedback\"))"))
        assertTrue(apply.contains("setupSearchIndex(adminMe)"))
        assertTrue(apply.contains("binding.rowVersionStatus.isEnabled = canConfigure"))
        assertFalse("Role promotion must retain the existing version-settings listener",
            apply.contains("rowVersionStatus.setOnClickListener(null)"))
        assertFalse("Owner must not return before updating previously hidden rows",
            apply.contains("if (adminMe.isOwner) {\n            setupSearchIndex(adminMe)"))
    }

    @Test fun testInstantShellOnCreateRendersActionBarAndProgressBeforeGateResolution() {
        val create = source.substringAfter("override fun onCreate(").substringBefore("override fun onSaveInstanceState(")
        val actionBar = create.indexOf("setupActionBar()")
        val progressVisible = create.indexOf("binding.progressBarAdmin.visibility = View.VISIBLE")
        val hubGone = create.indexOf("binding.scrollAdminHub.visibility = View.GONE")
        val gate = create.indexOf("AdminAccessGate.requireAdmin(")
        val progressGone = create.indexOf("binding.progressBarAdmin.visibility = View.GONE")
        val hubVisible = create.indexOf("binding.scrollAdminHub.visibility = View.VISIBLE")

        assertTrue("setupActionBar must run before requireAdmin for instant shell render",
            actionBar in 0..<gate)
        assertTrue("progressBarAdmin must be visible before requireAdmin",
            progressVisible in 0..<gate)
        assertTrue("scrollAdminHub must be hidden before authorization",
            hubGone in 0..<gate)
        assertTrue("progressBarAdmin must be hidden after authorization",
            progressGone > gate)
        assertTrue("scrollAdminHub must be shown after authorization",
            hubVisible > gate)

        val gateSrc = File("src/main/java/com/eve/app/util/AdminAccessGate.kt").readText()
        assertTrue("AdminAccessGate preserves root container visibility to eliminate blank screens",
            gateSrc.contains("isRootContainer(activity, protectedRoot)"))
    }
}
