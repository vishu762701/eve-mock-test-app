package com.eve.app.ui.profile

/**
 * The server exclusively decides "online" using authenticated expiring leases.
 * Null means the old Worker is installed; do not invent a presence state.
 */
internal object ProfilePresenceStatus {
    fun label(isOnline: Boolean?, eveId: String): String = when (isOnline) {
        true -> "online"
        false -> "offline"
        null -> if (eveId != "--") "Eve ID: $eveId" else "Student"
    }
}
