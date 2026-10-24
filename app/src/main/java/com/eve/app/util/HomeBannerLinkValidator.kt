package com.eve.app.util

import java.net.URI

object HomeBannerLinkValidator {
    const val MAX_URL_LENGTH = 2048
    const val MAX_LABEL_LENGTH = 48

    fun validate(urlValue: String, labelValue: String): String? {
        val url = urlValue.trim()
        val label = labelValue.trim()
        if (url.isEmpty()) return if (label.isEmpty()) null else "Clear the label when there is no link URL."
        if (url.length > MAX_URL_LENGTH) return "Link URL must be 2048 characters or fewer."
        if (label.isEmpty()) return "Enter a CTA label for this link."
        if (label.length > MAX_LABEL_LENGTH) return "CTA label must be 48 characters or fewer."
        if (!isValidUrl(url)) return "Enter a valid HTTP or HTTPS URL."
        return null
    }

    fun isValidUrl(value: String): Boolean {
        val url = value.trim()
        if (url.isEmpty() || url.length > MAX_URL_LENGTH || url.any { it.isWhitespace() || it.isISOControl() }) {
            return false
        }
        val parsed = try {
            URI(url)
        } catch (_: Exception) {
            return false
        }
        return parsed.scheme?.lowercase() in setOf("http", "https") &&
            !parsed.host.isNullOrBlank() &&
            parsed.rawUserInfo == null
    }
}
