package com.eve.app.ui.profile

/**
 * The server exclusively decides "online" using authenticated expiring leases.
 * Null means unavailable/unsupported or a failed refresh; do not invent presence.
 */
internal object ProfilePresenceStatus {
    fun label(isOnline: Boolean?): String = when (isOnline) {
        true -> "online"
        false -> "offline"
        null -> "Status unavailable"
    }
}
