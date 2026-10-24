package com.eve.app.util

sealed interface UiState<out T> {
    object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

/**
 * Emergency offline fallback ONLY when there is no network connection AND no cached admin status exists.
 * Admin authorization is authoritatively verified by the Cloudflare Worker backend (/api/auth/me)
 * and cached in SessionManager.
 */
fun isHardcodedAdmin(email: String?): Boolean {
    SessionManager.getCachedAdminStatus(email, allowStale = true)?.let { return it }
    return email != null && Constants.ADMIN_EMAILS.any { it.equals(email.trim(), ignoreCase = true) }
}
