package com.eve.app.data.remote

import com.eve.app.BuildConfig
import com.google.gson.JsonParser
import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException
import java.util.concurrent.TimeoutException

sealed class ApiError : Exception() {
    abstract fun userMessage(isDebug: Boolean = BuildConfig.DEBUG): String

    data class SessionExpired(val code: Int = 401, val serverMessage: String? = null) : ApiError() {
        override fun userMessage(isDebug: Boolean): String {
            return if (isDebug) {
                "[HTTP $code] Session Expired: ${serverMessage ?: "Token expired or missing"}"
            } else {
                "Session expired. Please sign in again."
            }
        }
    }

    data class Forbidden(val code: Int = 403, val serverMessage: String? = null) : ApiError() {
        override fun userMessage(isDebug: Boolean): String {
            return if (isDebug) {
                "[HTTP $code] Forbidden: ${serverMessage ?: "Access denied"}"
            } else {
                serverMessage?.takeIf { it.isNotBlank() } ?: "Access denied. Admin privileges required."
            }
        }
    }

    data class NotFound(val code: Int = 404, val serverMessage: String? = null) : ApiError() {
        override fun userMessage(isDebug: Boolean): String {
            return if (isDebug) {
                "[HTTP $code] Not Found: ${serverMessage ?: "Resource not found"}"
            } else {
                serverMessage?.takeIf { it.isNotBlank() } ?: "Requested resource not found on server."
            }
        }
    }

    data class Conflict(val code: Int = 409, val serverMessage: String? = null) : ApiError() {
        override fun userMessage(isDebug: Boolean): String {
            return if (isDebug) {
                "[HTTP $code] Conflict: ${serverMessage ?: "Resource conflict"}"
            } else {
                serverMessage?.takeIf { it.isNotBlank() } ?: "A conflict occurred. This item or name already exists."
            }
        }
    }

    data class Server(val code: Int, val serverMessage: String? = null) : ApiError() {
        override fun userMessage(isDebug: Boolean): String {
            return if (isDebug) {
                "[HTTP $code] Server Error: ${serverMessage ?: "Internal error"}"
            } else {
                "Internal server error. Please try again later."
            }
        }
    }

    data class Network(val causeThrowable: Throwable? = null) : ApiError() {
        override fun userMessage(isDebug: Boolean): String {
            return if (isDebug) {
                "[Network Error] ${causeThrowable?.localizedMessage ?: "Connection failure"}"
            } else {
                "Unable to connect to server. Please check your internet connection."
            }
        }
    }

    data class Timeout(val causeThrowable: Throwable? = null) : ApiError() {
        override fun userMessage(isDebug: Boolean): String {
            return if (isDebug) {
                "[Timeout Error] Request timed out: ${causeThrowable?.localizedMessage ?: ""}".trim()
            } else {
                "Connection timed out. Please try again."
            }
        }
    }

    data class Unknown(val causeThrowable: Throwable? = null, val customMessage: String? = null) : ApiError() {
        override fun userMessage(isDebug: Boolean): String {
            val msg = customMessage ?: causeThrowable?.localizedMessage
            return if (isDebug) {
                "[Unknown Error] ${msg ?: "An unexpected error occurred"}"
            } else {
                msg?.takeIf { it.isNotBlank() } ?: "An unexpected error occurred. Please try again."
            }
        }
    }

    companion object {
        fun from(throwable: Throwable): ApiError {
            if (throwable is ApiError) return throwable
            return when (throwable) {
                is HttpException -> {
                    val code = throwable.code()
                    val errorBodyString = try {
                        throwable.response()?.errorBody()?.string()
                    } catch (_: Exception) {
                        null
                    }

                    var serverMsg: String? = null
                    if (!errorBodyString.isNullOrBlank()) {
                        try {
                            val element = JsonParser.parseString(errorBodyString)
                            if (element.isJsonObject) {
                                val json = element.asJsonObject
                                serverMsg = when {
                                    json.has("error") && !json.get("error").isJsonNull -> json.get("error").asString
                                    json.has("message") && !json.get("message").isJsonNull -> json.get("message").asString
                                    else -> null
                                }
                            }
                        } catch (_: Exception) {
                            serverMsg = null
                        }
                        if (serverMsg.isNullOrBlank()) {
                            serverMsg = errorBodyString.take(200)
                        }
                    }

                    when (code) {
                        401 -> SessionExpired(code, serverMsg)
                        403 -> Forbidden(code, serverMsg)
                        404 -> NotFound(code, serverMsg)
                        409 -> Conflict(code, serverMsg)
                        in 500..599 -> Server(code, serverMsg)
                        else -> Server(code, serverMsg)
                    }
                }
                is SocketTimeoutException, is TimeoutException -> Timeout(throwable)
                is IOException -> Network(throwable)
                else -> Unknown(throwable, throwable.message)
            }
        }
    }
}

fun Throwable.toApiError(): ApiError = ApiError.from(this)

fun Throwable.toUserFriendlyMessage(isDebug: Boolean = BuildConfig.DEBUG): String {
    return toApiError().userMessage(isDebug)
}
