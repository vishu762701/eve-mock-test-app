package com.eve.app.ui.admin

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/** Source contracts complement offline repository API fixtures without live D1 changes. */
class ManageUsersReadOwnershipTest {
    private val production = File("src/main/java/com/eve/app/ui/admin/ManageUsersActivity.kt").readText()

    @Test fun savedSearchAndPageAreNotAdoptedByAnotherAccount() {
        assertTrue(production.contains("savedInstanceState?.getString(\"users_owner_uid\")"))
        assertTrue(production.contains("restoredOwner == currentOwner"))
        assertTrue(production.contains("if (canRestore) savedInstanceState?.getString(\"users_query\")"))
        assertTrue(production.contains("outState.putString(\"users_owner_uid\", authorizedUid"))
    }

    @Test fun pagesAndErrorsAreBoundToCurrentAuthorizedRequest() {
        val load = production.substringAfter("private fun loadUsers()")
            .substringBefore("private fun renderPagination(")
        assertTrue(load.contains("val ownerAtRequest = authorizedUid"))
        val network = load.indexOf("adminRepo.getUsersPage(queryAtRequest, pageAtRequest)")
        val initialGuard = load.indexOf("if (!isCurrentOwner(ownerAtRequest)) return")
        val guard = load.indexOf("if (!isCurrentOwner(ownerAtRequest)", network)
        val submit = load.indexOf("adapter.submitList(response.users)")
        assertTrue("Request must be authorized before fetch and revalidated after await",
            initialGuard >= 0 && initialGuard < network &&
                network >= 0 && guard > network && submit > guard)
        assertTrue(load.substringAfter("catch (e: Exception)").contains("isCurrentOwner(ownerAtRequest)"))
        assertTrue(load.contains("pageAtRequest != currentPage || queryAtRequest != query"))
    }

    @Test fun detailDialogsAndStatusMutationsRejectStaleAuthorization() {
        val details = production.substringAfter("private fun showUserDetailsDialog(")
            .substringBefore("private fun confirmToggleUserStatus(")
        assertTrue(details.contains("detailsJob?.cancel()"))
        assertTrue(details.contains("requestPage != currentPage"))
        assertTrue(details.contains("requestQuery != query"))
        assertTrue(details.contains("isCurrentOwner(requestOwner)"))
        assertTrue(details.contains("catch (cancel: CancellationException)"))
        val status = production.substringAfter("private fun confirmToggleUserStatus(")
        assertTrue(status.contains("statusDialog?.isShowing == true"))
        assertTrue(status.contains("if (!isCurrentOwner(actionOwner)"))
        assertTrue(production.contains("auth") || production.contains("FirebaseAuth.getInstance().currentUser?.uid"))
        assertTrue(production.contains("detailsDialog?.dismiss()"))
    }
}
