package com.eve.app.util

sealed interface UiState<out T> {
    object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

/** Fresh Worker presentation hint; privileged screens always verify with the Worker. */
fun hasFreshWorkerAdminHint(email: String?): Boolean =
    SessionManager.getCachedAdminStatus(email, allowStale = false) == true
