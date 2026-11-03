package com.eve.app.ui.admin

import com.eve.app.data.remote.AdminMeData
import com.eve.app.data.remote.ApiResponse
import com.eve.app.data.remote.EveApiService
import com.eve.app.data.remote.SyllabusUploadResponse
import com.eve.app.data.repository.ExamRepository
import com.google.gson.Gson
import java.lang.reflect.Proxy
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

/** Production auth-await helper and ExamRepository; only API transport is faked. */
class SyllabusMutationOwnerContractTest {
    private val email = "authorized@example.invalid"
    private val permission = AdminMeData(email = email, rights = listOf("manage_syllabus"))

    @Test fun freshPermissionIsRequiredAndValidGrantWorks() = runTest {
        requireCurrentSyllabusRight(email, { true }, { true }, { permission })
        var consulted = false
        val denied = runCatching {
            requireCurrentSyllabusRight(email, { true }, { true }, {
                consulted = true
                AdminMeData(email = email, rights = listOf("manage_questions"))
            })
        }
        assertTrue(consulted)
        assertTrue(denied.exceptionOrNull() is SecurityException)
    }

    @Test fun accountSwitchDuringSuspendedPermissionReadNeverAuthorizesMutation() = runTest {
        var current = true
        val delayed = CompletableDeferred<AdminMeData>()
        val attempt = async {
            runCatching {
                requireCurrentSyllabusRight(email, { current }, { true }, { delayed.await() })
            }
        }
        runCurrent()
        current = false
        delayed.complete(permission)
        advanceUntilIdle()
        assertTrue(attempt.await().exceptionOrNull() is kotlinx.coroutines.CancellationException)
    }

    @Test fun staleOwnerNeverStartsPermissionRequest() = runTest {
        var touchedServer = false
        val result = runCatching {
            requireCurrentSyllabusRight(email, { false }, {
                touchedServer = true; true
            }, {
                touchedServer = true; permission
            })
        }
        assertTrue(result.exceptionOrNull() is kotlinx.coroutines.CancellationException)
        assertFalse(touchedServer)
    }

    private fun repo(response: ApiResponse<SyllabusUploadResponse>): ExamRepository {
        val api = Proxy.newProxyInstance(
            EveApiService::class.java.classLoader,
            arrayOf(EveApiService::class.java)
        ) { _, method, args ->
            check(method.name == "uploadSyllabus") { "Unexpected request: ${method.name}" }
            assertEquals("exam-id", args?.get(0))
            assertEquals("syllabus.pdf", args?.get(1))
            response
        } as EveApiService
        return ExamRepository(api)
    }

    @Test fun missingMalformedAndDeniedUploadAckCannotReportSuccess() = runTest {
        val missingUrl = Gson().fromJson(
            """{"syllabusFileName":"syllabus.pdf"}""", SyllabusUploadResponse::class.java
        )
        val cases = listOf(
            ApiResponse<SyllabusUploadResponse>(success = true, data = null),
            ApiResponse(success = true, data = SyllabusUploadResponse("", "syllabus.pdf")),
            ApiResponse(success = true, data = missingUrl),
            ApiResponse(success = false, data = SyllabusUploadResponse(
                "https://example.invalid/syllabus.pdf", "syllabus.pdf"
            ), error = "Access denied")
        )
        for (response in cases) {
            val result = runCatching {
                repo(response).uploadSyllabusPdf("exam-id", "syllabus.pdf", "%PDF-1.7".toByteArray())
            }
            assertTrue("Unconfirmed Worker response must fail", result.isFailure)
        }
    }

    @Test fun confirmedSyllabusUrlPreservedFromWorker() = runTest {
        val expected = "https://example.invalid/syllabus.pdf"
        val ack = ApiResponse(success = true,
            data = SyllabusUploadResponse(expected, "syllabus.pdf"))
        assertEquals(expected,
            repo(ack).uploadSyllabusPdf("exam-id", "syllabus.pdf", "%PDF-1.7".toByteArray()))
    }
}
