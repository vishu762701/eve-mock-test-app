package com.eve.app.data.model

data class AdminUser(
    val id: String = "",
    val email: String = "",
    val displayName: String = "Student",
    val dob: String = "",
    val category: String = "General",
    val createdAt: Long = 0L,
    val lastActive: Long = 0L,
    val disabled: Boolean = false
)
