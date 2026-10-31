package com.eve.app.ui.profile

import android.view.View
import android.widget.TextView

/**
 * One authoritative state transition for Bio capability. Null means the
 * production Worker has not returned an authenticated Bio field; it must never
 * be mistaken for the valid empty Bio.
 */
internal object ProfileBioCapability {
    fun apply(cell: View, value: TextView, authenticatedBio: String?) {
        // An XML default is not a substitute for explicit controller state.
        cell.isEnabled = authenticatedBio != null
        value.text = when {
            authenticatedBio == null -> "Bio unavailable until server update"
            authenticatedBio.isBlank() -> "Add bio"
            else -> authenticatedBio
        }
    }
}
