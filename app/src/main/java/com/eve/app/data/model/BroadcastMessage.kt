package com.eve.app.data.model

data class BroadcastMessage(
    val id: String = "",
    val title: String = "",
    val message: String = "",
    val sentAt: Long = 0L,
    val sentBy: String = "",
    val type: String = "general"
)
