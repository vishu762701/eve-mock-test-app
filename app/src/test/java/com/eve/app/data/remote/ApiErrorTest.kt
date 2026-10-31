package com.eve.app.data.remote

import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException
import java.net.SocketTimeoutException
import java.util.concurrent.TimeoutException

class ApiErrorTest {

    private fun createHttpException(code: Int, jsonBody: String): HttpException {
        val mediaType = "application/json".toMediaTypeOrNull()
        val responseBody = jsonBody.toResponseBody(mediaType)
        val response = Response.error<Any>(code, responseBody)
        return HttpException(response)
    }

    @Test
    fun rateLimitAndValidationKeepMachineCodesAndClearMessages() {
        val limited = createHttpException(429, """{"error":"Please wait before retrying.","code":"RATE_LIMITED","requestId":"request-1"}""").toApiError()
        assertTrue(limited is ApiError.RateLimited)
        assertEquals("RATE_LIMITED", limited.serverCode)
        assertEquals("request-1", limited.requestId)
        assertEquals("Please wait before retrying.", limited.userMessage(false))
        val invalid = createHttpException(422, """{"error":"Invalid exam duration.","code":"VALIDATION_FAILED"}""").toApiError()
        assertTrue(invalid is ApiError.BadRequest)
        assertEquals("VALIDATION_FAILED", invalid.serverCode)
        assertEquals("Invalid exam duration.", invalid.userMessage(false))
    }

    @Test
    fun genericServiceOutageIsNeverMisrepresentedAsAnAiFailure() {
        val message = "Request protection is temporarily unavailable. Please retry."
        assertEquals(message, sanitizeErrorMessage(message))
        val unavailable = createHttpException(503, """{"error":"Database unavailable","code":"SERVICE_UNAVAILABLE"}""").toApiError()
        assertEquals("The service is temporarily unavailable. Please retry shortly.", unavailable.userMessage(false))
    }

    @Test
    fun retryAfterAndResponseMetadataRemainAvailableToTheClient() {
        val raw = okhttp3.Response.Builder()
            .request(okhttp3.Request.Builder().url("https://example.test").build())
            .protocol(okhttp3.Protocol.HTTP_1_1).code(429).message("Too Many Requests")
            .header("Retry-After", "12").build()
        val response = Response.error<Any>("{}".toResponseBody("application/json".toMediaTypeOrNull()), raw)
        val error = HttpException(response).toApiError() as ApiError.RateLimited
        assertEquals(12L, error.retryAfterSeconds)
        val envelope = com.google.gson.Gson().fromJson(
            """{"success":false,"error":"Invalid field.","code":"INVALID_FIELD_TYPE","requestId":"request-2"}""",
            ApiResponse::class.java
        )
        assertEquals("INVALID_FIELD_TYPE", envelope.errorCode)
        assertEquals("request-2", envelope.requestId)
    }

    @Test
    fun test401SessionExpiredMapping() {
        val exception = createHttpException(401, """{"error": "Token has expired"}""")
        val apiError = exception.toApiError()

        assertTrue(apiError is ApiError.SessionExpired)
        val sessionExpired = apiError as ApiError.SessionExpired
        assertEquals(401, sessionExpired.code)
        assertEquals("Token has expired", sessionExpired.serverMessage)

        val debugMsg = sessionExpired.userMessage(isDebug = true)
        assertTrue(debugMsg.contains("[HTTP 401]"))
        assertTrue(debugMsg.contains("Token has expired"))

        val releaseMsg = sessionExpired.userMessage(isDebug = false)
        assertEquals("Session expired. Please sign in again.", releaseMsg)
    }

    @Test
    fun test403ForbiddenMapping() {
        val exception = createHttpException(403, """{"message": "Admin access required"}""")
        val apiError = exception.toApiError()

        assertTrue(apiError is ApiError.Forbidden)
        val forbidden = apiError as ApiError.Forbidden
        assertEquals(403, forbidden.code)
        assertEquals("Admin access required", forbidden.serverMessage)

        val debugMsg = forbidden.userMessage(isDebug = true)
        assertTrue(debugMsg.contains("[HTTP 403]"))
        assertTrue(debugMsg.contains("Admin access required"))

        val releaseMsg = forbidden.userMessage(isDebug = false)
        assertEquals("Admin access required", releaseMsg)
    }

    @Test
    fun test404NotFoundMapping() {
        val exception = createHttpException(404, """{"error": "Test paper not found"}""")
        val apiError = exception.toApiError()

        assertTrue(apiError is ApiError.NotFound)
        val notFound = apiError as ApiError.NotFound
        assertEquals(404, notFound.code)
        assertEquals("Test paper not found", notFound.serverMessage)

        val debugMsg = notFound.userMessage(isDebug = true)
        assertTrue(debugMsg.contains("[HTTP 404]"))
        assertTrue(debugMsg.contains("Test paper not found"))

        val releaseMsg = notFound.userMessage(isDebug = false)
        assertEquals("Test paper not found", releaseMsg)
    }

    @Test
    fun test409ConflictMapping() {
        val exception = createHttpException(409, """{"error": "An exam with this name already exists"}""")
        val apiError = exception.toApiError()

        assertTrue(apiError is ApiError.Conflict)
        val conflict = apiError as ApiError.Conflict
        assertEquals(409, conflict.code)
        assertEquals("An exam with this name already exists", conflict.serverMessage)

        val debugMsg = conflict.userMessage(isDebug = true)
        assertTrue(debugMsg.contains("[HTTP 409]"))
        assertTrue(debugMsg.contains("An exam with this name already exists"))

        val releaseMsg = conflict.userMessage(isDebug = false)
        assertEquals("An exam with this name already exists", releaseMsg)
    }

    @Test
    fun test500ServerMapping() {
        val exception = createHttpException(500, """{"error": "D1 database connection failed"}""")
        val apiError = exception.toApiError()

        assertTrue(apiError is ApiError.Server)
        val server = apiError as ApiError.Server
        assertEquals(500, server.code)
        assertEquals("D1 database connection failed", server.serverMessage)

        val debugMsg = server.userMessage(isDebug = true)
        assertTrue(debugMsg.contains("[HTTP 500]"))
        assertTrue(debugMsg.contains("D1 database connection failed"))

        val releaseMsg = server.userMessage(isDebug = false)
        assertEquals("Internal server error. Please try again later.", releaseMsg)
    }

    @Test
    fun testIOExceptionNetworkMapping() {
        val exception = IOException("Failed to connect to /api/exams")
        val apiError = exception.toApiError()

        assertTrue(apiError is ApiError.Network)
        val network = apiError as ApiError.Network
        assertEquals(exception, network.causeThrowable)

        val debugMsg = network.userMessage(isDebug = true)
        assertTrue(debugMsg.contains("[Network Error]"))
        assertTrue(debugMsg.contains("Failed to connect"))

        val releaseMsg = network.userMessage(isDebug = false)
        assertEquals("Unable to connect to server. Please check your internet connection.", releaseMsg)
    }

    @Test
    fun testTimeoutMapping() {
        val socketTimeout = SocketTimeoutException("Read timed out")
        val apiErrorSocket = socketTimeout.toApiError()

        assertTrue(apiErrorSocket is ApiError.Timeout)
        val debugMsgSocket = apiErrorSocket.userMessage(isDebug = true)
        assertTrue(debugMsgSocket.contains("[Timeout Error]"))

        val releaseMsgSocket = apiErrorSocket.userMessage(isDebug = false)
        assertEquals("Connection timed out. Please try again.", releaseMsgSocket)

        val timeoutEx = TimeoutException("Future timeout")
        val apiErrorTimeout = timeoutEx.toApiError()
        assertTrue(apiErrorTimeout is ApiError.Timeout)
    }

    @Test
    fun testGeminiRawProviderErrorSanitization() {
        val rawGoogle401 = """{"error": "Gemini API error (401) on model gemini-3.5-flash-lite: {\"error\": {\"code\": 401, \"message\": \"The bound service account is deleted or disabled. The service account bound to the API key must be active.\", \"status\": \"ACCOUNT_STATE_INVALID\"}}"}"""
        val exception = createHttpException(500, rawGoogle401)
        val apiError = exception.toApiError()

        val msgDebug = apiError.userMessage(isDebug = true)
        val msgRelease = apiError.userMessage(isDebug = false)

        assertTrue(msgDebug.contains("AI generation is temporarily unavailable"))
        assertTrue(msgRelease.contains("AI generation is temporarily unavailable"))
        org.junit.Assert.assertFalse(msgDebug.contains("service account"))
        org.junit.Assert.assertFalse(msgDebug.contains("ACCOUNT_STATE_INVALID"))
    }
}
