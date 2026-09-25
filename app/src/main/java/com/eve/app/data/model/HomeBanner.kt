package com.eve.app.data.model

/**
 * Task A: Model for promotional banners displayed on the student Home screen carousel
 * and managed in the Admin Dashboard.
 * Firestore collection: home_banners/{bannerId}
 */
data class HomeBanner(
    val id: String = "",
    val imageUrl: String = "",
    val storagePath: String? = null,
    val order: Int = 0,
    val uploadedAt: Long = 0L,
    val uploadedBy: String = "",
    val active: Boolean = true
)
