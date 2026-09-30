package com.eve.app.data.remote

import org.json.JSONObject
import retrofit2.HttpException
import java.io.IOException

fun Throwable.toUserFriendlyMessage(): String {
    return when (this) {
        is HttpException -> {
            val code = code()
            val errorBodyString = try {
                response()?.errorBody()?.string()
            } catch (_: Exception) {
                null
            }

            var serverMsg: String? = null
            if (!errorBodyString.isNullOrBlank()) {
                try {
                    val json = JSONObject(errorBodyString)
                    serverMsg = json.optString("error").takeIf { it.isNotBlank() }
                        ?: json.optString("message").takeIf { it.isNotBlank() }
                } catch (_: Exception) {
                    serverMsg = errorBodyString.take(200)
                }
            }

            when (code) {
                401 -> serverMsg ?: "Authentication required. Please sign in again."
                403 -> serverMsg ?: "Access denied. Admin privileges required."
                404 -> serverMsg ?: "Requested resource not found on server."
                409 -> serverMsg ?: "A conflict occurred. This item or name already exists."
                500 -> serverMsg ?: "Internal server error. Please try again later."
                503 -> serverMsg ?: "Service temporarily unavailable. Please try again later."
                else -> serverMsg ?: "Server error ($code): ${message()}"
            }
        }
        is IOException -> {
            "Network error: Unable to connect to server. Please check your internet connection."
        }
        else -> {
            message?.takeIf { it.isNotBlank() } ?: "An unexpected error occurred."
        }
    }
}
