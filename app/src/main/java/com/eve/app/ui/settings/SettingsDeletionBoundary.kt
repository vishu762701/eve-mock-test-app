package com.eve.app.ui.settings

/**
 * FirebaseAuth normally signs out the deleted user itself. Null is expected after
 * success; a different, surviving user must never have their global login cleared.
 */
internal object SettingsDeletionBoundary {
    fun mayClearGlobalSession(deletedUid: String, activeUid: String?): Boolean =
        deletedUid.isNotBlank() && (activeUid == null || activeUid == deletedUid)
}
