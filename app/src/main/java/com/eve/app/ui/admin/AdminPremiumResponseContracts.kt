package com.eve.app.ui.admin

import com.eve.app.data.model.AdminPremiumConfigDto
import com.eve.app.data.remote.ApiResponse

/** Pure Worker acknowledgment checks; not a second state or authorization owner. */
internal object AdminPremiumResponseContracts {
    fun configValid(config: AdminPremiumConfigDto?): Boolean {
        if (config == null) return false
        // Gson can assign null even to Kotlin's nominally non-null properties.
        val id: String? = config.id
        val plan: String? = config.planName
        val merchant: String? = config.merchantVpa
        val merchantName: String? = config.merchantName
        val entries: List<*>? = config.benefits
        return id == "default" && !plan.isNullOrBlank() && plan.length <= 100 &&
            config.priceInr in 1..1000000 &&
            config.durationDays in 1..3650 &&
            config.sessionExpiryMinutes in 1..1440 &&
            config.currency == "INR" &&
            !merchant.isNullOrBlank() && !merchantName.isNullOrBlank() &&
            entries != null && entries.all { it is String && it.isNotBlank() }
    }

    fun configMatches(expected: AdminPremiumConfigDto, received: AdminPremiumConfigDto?): Boolean =
        configValid(received) && received != null &&
            expected.isEnabled == received.isEnabled &&
            expected.planName == received.planName &&
            expected.priceInr == received.priceInr &&
            expected.currency == received.currency &&
            expected.durationDays == received.durationDays &&
            expected.isLifetime == received.isLifetime &&
            expected.description == received.description &&
            expected.benefits == received.benefits &&
            expected.qrEnabled == received.qrEnabled &&
            expected.upiEnabled == received.upiEnabled &&
            expected.sessionExpiryMinutes == received.sessionExpiryMinutes &&
            expected.merchantVpa == received.merchantVpa &&
            expected.merchantName == received.merchantName

    fun granted(response: ApiResponse<Any>, expectedLifetime: Boolean): Boolean {
        if (!response.success) return false
        val data = response.data as? Map<*, *> ?: return false
        val uid = data["user_id"] as? String ?: return false
        val status = data["status"] as? String
        val source = data["source"] as? String
        val lifetime = when (val value = data["is_lifetime"]) {
            is Boolean -> if (value) 1 else 0
            is Number -> when (value.toDouble()) { 0.0 -> 0; 1.0 -> 1; else -> return false }
            else -> return false
        }
        return uid.isNotBlank() && status == "ACTIVE" && source == "ADMIN_GRANTED" &&
            (lifetime == 0 || lifetime == 1) &&
            (!expectedLifetime || lifetime == 1)
    }

    fun extended(response: ApiResponse<Any>, expectedUid: String): Boolean {
        if (!response.success) return false
        val data = response.data as? Map<*, *> ?: return false
        val uid = data["userId"] as? String
        val expiry = data["expiresAt"] as? Number
        return expectedUid.isNotBlank() && uid == expectedUid &&
            expiry != null && expiry.toDouble().isFinite() && expiry.toDouble() > 0 &&
            expiry.toDouble() == kotlin.math.floor(expiry.toDouble())
    }

    fun revoked(response: ApiResponse<Any>, expectedUid: String): Boolean {
        if (!response.success) return false
        val data = response.data as? Map<*, *> ?: return false
        return expectedUid.isNotBlank() && data["userId"] == expectedUid &&
            data["status"] == "REVOKED"
    }
}
