package com.eve.app.data.model

data class AppConfig(
    val minimum_supported_version_code: Int = 1,
    val maintenance_mode: Boolean = false,
    val maintenance_message: String = "Eve Mock Test is currently undergoing scheduled maintenance. Please check back shortly.",
    val default_publish_mode: String = "paused"
)
