package com.eve.app.data.model

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

data class AdminPremiumConfigDto(
    val id: String = "default",
    val isEnabled: Boolean = true,
    val planName: String = "Premium Pro",
    val priceInr: Int = 99,
    val currency: String = "INR",
    val durationDays: Int = 30,
    val isLifetime: Boolean = false,
    val description: String = "",
    val benefits: List<String> = emptyList(),
    val qrEnabled: Boolean = true,
    val upiEnabled: Boolean = true,
    val sessionExpiryMinutes: Int = 10,
    val merchantVpa: String = "evemocktest@upi",
    val merchantName: String = "Eve Mock Test",
    val webhookSecret: String = "eve_whsec_dev"
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
    val planName: String = "Admin Premium Access"
)

data class AdminExtendRequest(
    val userId: String,
    val additionalDays: Int = 30
)

data class AdminRevokeRequest(
    val userId: String
)
