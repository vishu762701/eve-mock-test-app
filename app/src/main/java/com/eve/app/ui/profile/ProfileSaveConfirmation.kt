package com.eve.app.ui.profile

/** A readback can confirm a save only for the account that initiated it. */
internal object ProfileSaveConfirmation {
    fun matches(
        ownerUid: String?, currentUid: String?, success: Boolean,
        expected: String, actual: String?, ignoreCase: Boolean = false
    ): Boolean = !ownerUid.isNullOrBlank() && ownerUid == currentUid &&
        success && actual != null && expected.equals(actual, ignoreCase)
}
