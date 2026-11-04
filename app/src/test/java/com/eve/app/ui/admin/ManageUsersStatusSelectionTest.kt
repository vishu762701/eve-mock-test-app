package com.eve.app.ui.admin

import com.eve.app.data.model.AdminUser
import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Production status confirmation must remain bound to one UID and visible row snapshot. */
class ManageUsersStatusSelectionTest {
    private val first = AdminUser(id = "a", email = "a@example.test", disabled = false)
    private val second = AdminUser(id = "b", email = "b@example.test", disabled = true)
    private val selected = UserStatusSelection("admin-A", "a", "a@example.test", false, 1, "a", 7L)

    @Test fun selectionRejectsChangedUserOwnerPageQueryOrEpoch() {
        assertTrue(selected.matches("admin-A", 1, "a", 7L, listOf(first, second)))
        assertFalse(selected.matches("admin-B", 1, "a", 7L, listOf(first)))
        assertFalse(selected.matches("admin-A", 2, "a", 7L, listOf(first)))
        assertFalse(selected.matches("admin-A", 1, "b", 7L, listOf(first)))
        assertFalse(selected.matches("admin-A", 1, "a", 8L, listOf(first)))
        assertFalse(selected.matches("admin-A", 1, "a", 7L, listOf(second)))
        assertFalse(selected.matches("admin-A", 1, "a", 7L,
            listOf(first.copy(email = "changed@example.test"))))
        assertFalse(selected.matches("admin-A", 1, "a", 7L,
            listOf(first.copy(disabled = true))))
    }

    @Test fun productionDismissesStaleDialogAndChecksSelectionBeforeMutationAndReceipt() {
        val source = File("src/main/java/com/eve/app/ui/admin/ManageUsersActivity.kt").readText()
        val reload = source.substringAfter("private fun loadUsers()").substringBefore("private fun renderPagination(")
        val status = source.substringAfter("private fun confirmToggleUserStatus(")
        assertTrue(reload.contains("statusDialog?.dismiss()"))
        assertTrue(status.contains("selection.matches(authorizedUid, currentPage, query, detailsEpoch, adapter.currentList)"))
        val positive = status.indexOf("requestJob?.isActive == true) return@setPositiveButton")
        val mutate = status.indexOf("adminRepo.toggleUserStatus(selection.userId, disable)")
        val verify = status.indexOf("if (!selectionCurrent()", mutate)
        assertTrue("Selection must be checked before and after Worker mutation",
            positive >= 0 && mutate > positive && verify > mutate)
        assertTrue(status.contains("if (selectionCurrent() &&"))
    }
}
