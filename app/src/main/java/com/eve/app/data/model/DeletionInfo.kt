package com.eve.app.data.model

data class DeletionInfo(
    val attempts: Long = 0,
    val sessions: Long = 0,
    val subExams: Long = 0,
    val generationJobs: Long = 0,
    val canDelete: Boolean = false,
    val code: String? = null,
    val message: String? = null,
    val safeAction: String? = null
)
