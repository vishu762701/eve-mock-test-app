package com.eve.app.data.model

/** Banner image and optional CTA shown inside the single Home banner surface. */
data class HomeBanner(
    val id: String = "",
    val imageUrl: String = "",
    val storagePath: String? = null,
    val order: Int = 0,
    val uploadedAt: Long = 0L,
    val uploadedBy: String = "",
    val active: Boolean = true,
    val linkUrl: String = "",
    val linkLabel: String = ""
)
