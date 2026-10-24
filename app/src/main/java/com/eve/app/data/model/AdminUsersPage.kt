package com.eve.app.data.model

/** Server-supplied metadata; never infer this from a truncated user array. */
data class AdminUsersPagination(
    val page: Int = 1,
    val pageSize: Int = 50,
    val total: Int = 0,
    val totalPages: Int = 0,
    val hasNext: Boolean = false,
    val hasPrevious: Boolean = false
)

data class AdminUsersPage(
    val users: List<AdminUser>,
    val pagination: AdminUsersPagination
)
