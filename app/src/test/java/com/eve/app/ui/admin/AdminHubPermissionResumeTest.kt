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
}
