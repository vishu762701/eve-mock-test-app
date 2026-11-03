package com.eve.app.ui.admin

import org.junit.Assert.*
import org.junit.Test
import java.io.File

class AdminRightsNetworkAuthorityTest {
    private fun source(name: String): String =
        File("src/main/java/com/eve/app/ui/admin/${name}.kt").readText()

    @Test
    fun dismissCannotClaimSuccessBeforeServerConfirmation() {
        val edit = source("AdminRightsEditActivity")
        val list = source("AdminsAndRightsActivity")
        // Dismiss is now wrapped by the existing fresh-right, identity-bound mutation owner.
        assertTrue(edit.contains("viewModel.removeAdmin(targetEmail, callback)"))
        assertTrue(edit.contains("runAuthorizedMutation("))
        assertTrue(edit.contains("requireFreshRightsPermission()"))
        assertTrue(edit.contains("if (epoch == actionEpoch && canAct())"))
        assertTrue(edit.contains("result.onSuccess { success() }"))
        assertTrue(edit.contains(".onFailure { AppBulletin.showError(this@AdminRightsEditActivity"))
        assertTrue(edit.contains("AdminIdentityGuard.allowed("))
        assertTrue(edit.contains("AdminPermissionSnapshot.load("))
        assertTrue(list.contains("viewModel.removeAdmin(email) { result ->"))
        assertTrue(list.contains("}.onFailure {"))
        assertTrue(list.contains("AppBulletin.showError(this, it.toUserFriendlyMessage())"))
        val vm = source("AdminViewModel")
        assertTrue(vm.contains("onResult(Result.success(Unit))"))
        assertTrue(vm.contains("onResult(Result.failure(e))"))
    }

    @Test
    fun adminDirectoryRefreshMustBePostAuthorization() {
        val list = source("AdminsAndRightsActivity")
        assertTrue(list.contains("authorized = true"))
        assertTrue(list.contains("if (authorized) viewModel.loadAdmins()"))
    }

    @Test
    fun adminHeaderMustNotInferOwnerFromClientEmailAllowlist() {
        val hub = source("AdminActivity")
        assertTrue(hub.contains("applyAdminPermissions(permissions)"))
        assertTrue(hub.contains("adminMe.isOwner"))
        assertFalse(hub.contains("email == \"own.keni@gmail.com\""))
        assertFalse(hub.contains("email == \"admin@eve.app\""))
    }
}
