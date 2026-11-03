package com.eve.app.data.model

import com.google.gson.annotations.SerializedName
import com.google.gson.annotations.JsonAdapter

data class PremiumPlanDto(
    val isEnabled: Boolean = true,
    val planId: String = "default",
    val planName: String = "Premium Pro",
    val priceInr: Int = 99,
    val currency: String = "INR",
    val durationDays: Int = 30,
    val isLifetime: Boolean = false,
    val description: String = "",
    val benefits: List<String> = emptyList(),
    val paymentMethods: List<String> = listOf("qr", "upi"),
    val qrEnabled: Boolean = true,
    val upiEnabled: Boolean = true,
    val sessionExpiryMinutes: Int = 10
)

data class PremiumStatusDto(
    val isPremium: Boolean = false,
    val status: String = "NONE",
    val planName: String = "",
    val activatedAt: Long = 0L,
    val expiresAt: Long? = null,
    val isLifetime: Boolean = false,
    val source: String = ""
)

data class CreateOrderRequest(
    val paymentMethod: String
)

data class CreateOrderResponse(
    val orderId: String,
    val planName: String,
    val amount: Int,
    val currency: String,
    val durationDays: Int,
    val isLifetime: Boolean,
    val paymentMethod: String,
    val createdAt: Long,
    val expiresAt: Long,
    val sessionExpiryMinutes: Int,
    val upiUri: String,
    val qrData: String
)

data class OrderStatusDto(
    val orderId: String,
    val status: String,
    val amount: Int,
    val currency: String,
    val paymentMethod: String,
    val createdAt: Long,
    val expiresAt: Long,
    val paidAt: Long,
    val isPremium: Boolean
)

@JsonAdapter(AdminPremiumConfigJsonAdapter::class)
data class AdminPremiumConfigDto(
    // Admin Worker GET retains legacy D1 snake_case; PUT uses the client camelCase contract.
    @field:SerializedName(value = "id") val id: String = "",
    @field:SerializedName(value = "isEnabled", alternate = ["is_enabled"]) val isEnabled: Boolean = true,
    @field:SerializedName(value = "planName", alternate = ["plan_name"]) val planName: String = "",
    @field:SerializedName(value = "priceInr", alternate = ["price_inr"]) val priceInr: Int = 0,
    @field:SerializedName(value = "currency") val currency: String = "",
    @field:SerializedName(value = "durationDays", alternate = ["duration_days"]) val durationDays: Int = 0,
    @field:SerializedName(value = "isLifetime", alternate = ["is_lifetime"]) val isLifetime: Boolean = false,
    @field:SerializedName(value = "description") val description: String = "",
    @field:SerializedName(value = "benefits") val benefits: List<String> = emptyList(),
    @field:SerializedName(value = "qrEnabled", alternate = ["qr_enabled"]) val qrEnabled: Boolean = true,
    @field:SerializedName(value = "upiEnabled", alternate = ["upi_enabled"]) val upiEnabled: Boolean = true,
    @field:SerializedName(value = "sessionExpiryMinutes", alternate = ["session_expiry_minutes"]) val sessionExpiryMinutes: Int = 0,
    @field:SerializedName(value = "merchantVpa", alternate = ["merchant_vpa"]) val merchantVpa: String = "",
    @field:SerializedName(value = "merchantName", alternate = ["merchant_name"]) val merchantName: String = "",
    // Legacy field: never supplied by the backend, never editable in this screen.
    val webhookSecret: String = ""
)

data class AdminTransactionDto(
    val orderId: String,
    val userId: String,
    val userEmail: String,
    val planId: String,
    val planName: String,
    val amount: Int,
    val currency: String,
    val durationDays: Int,
    val isLifetime: Boolean,
    val paymentMethod: String,
    val providerOrderId: String,
    val providerPaymentId: String,
    val status: String,
    val createdAt: Long,
    val expiresAt: Long,
    val paidAt: Long
)

data class AdminPremiumUserDto(
    val userId: String,
    val email: String,
    val displayName: String,
    val planId: String,
    val planName: String,
    val paymentOrderId: String,
    val providerPaymentId: String,
    val activatedAt: Long,
    val expiresAt: Long?,
    val isLifetime: Boolean,
    val status: String,
    val source: String,
    val createdAt: Long,
    val updatedAt: Long
)

data class AdminGrantRequest(
    val userIdOrEmail: String,
    val durationDays: Int = 30,
    val isLifetime: Boolean = false,
    val planName: String = "Admin Premium Access",
    val confirmLifetime: Boolean = false
)

data class AdminExtendRequest(
    val userId: String,
    val additionalDays: Int = 30
)

data class AdminRevokeRequest(
    val userId: String
)
