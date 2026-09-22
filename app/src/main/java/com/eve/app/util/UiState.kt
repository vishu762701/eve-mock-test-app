package com.eve.app.util

sealed interface UiState<out T> {
    object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

/** Sirf hardcoded list check karta hai - fast, no network chahiye */
fun isHardcodedAdmin(email: String?): Boolean =
    email != null && Constants.ADMIN_EMAILS.any { it.equals(email, ignoreCase = true) }
