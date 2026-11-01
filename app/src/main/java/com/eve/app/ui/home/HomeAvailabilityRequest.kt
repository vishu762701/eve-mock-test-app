package com.eve.app.ui.home

import com.eve.app.data.model.AppConfig
import kotlinx.coroutines.withTimeoutOrNull
import java.io.IOException

/** Public availability and privileged bypass have separate bounded deadlines. */
class HomeAvailabilityRequest(
    private val fetch: suspend () -> AppConfig,
    private val verifyAdmin: suspend () -> Boolean,
    private val configTimeoutMs: Long = 8_000L,
    private val adminTimeoutMs: Long = 5_000L
) {
    companion object {
        // Disk-cached OFF must not bypass a fresh cold-start verification.
        fun canReuseConfirmedOffOnThemeChange(themeRecreation: Boolean, confirmedOff: Boolean) =
            themeRecreation && confirmedOff
    }

    suspend fun config(): AppConfig = withTimeoutOrNull(configTimeoutMs) { fetch() }
        ?: throw IOException("Configuration request timed out")

    suspend fun adminBypass(config: AppConfig): Boolean {
        if (!config.maintenance_mode) return false
        return withTimeoutOrNull(adminTimeoutMs) { verifyAdmin() } ?: false
    }
}
