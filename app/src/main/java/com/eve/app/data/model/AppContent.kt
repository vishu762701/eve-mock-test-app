package com.eve.app.data.model

data class AppContent(
    val title: String = "",
    val body: String = "",
    val updatedAt: Long = 0L,
    val updatedBy: String = "",
    // Structured fields for Contact Us
    val supportEmail: String = "",
    val phone: String = "",
    val website: String = "",
    val address: String = ""
)
