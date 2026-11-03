package com.eve.app.data.repository

import com.eve.app.data.model.GeneratedTest
import com.eve.app.data.remote.ApiResponse
import com.eve.app.data.remote.EveApiService
import java.lang.reflect.Proxy
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

/** Real repository methods with a fake transport; no production Worker writes. */
class GeneratedTestMutationContractTest {
    private fun repository(handler: (String, Array<out Any?>) -> Any?): ExamRepository {
        val api = Proxy.newProxyInstance(
            EveApiService::class.java.classLoader,
            arrayOf(EveApiService::class.java)
        ) { _, method, args -> handler(method.name, args ?: emptyArray()) } as EveApiService
        return ExamRepository(api)
    }

    @Test fun statusRequiresExactPersistedTestAndNeverTrustsSuccessFlagAlone() = runBlocking {
        for (status in listOf("live", "paused")) {
            val calls = mutableListOf<String>()
            val repo = repository { name, args ->
                calls.add(name)
                when (name) {
                    "updateGeneratedTestStatus" -> {
                        assertEquals("test-1", args[0])
                        assertEquals(mapOf("status" to status), args[1])
                        ApiResponse<Unit>(true)
                    }
                    "getGeneratedTest" -> ApiResponse(true, GeneratedTest(id = "test-1", status = status))
                    else -> error("Unexpected call: $name")
                }
            }
            repo.updateGeneratedTestStatus("test-1", status)
            assertEquals(listOf("updateGeneratedTestStatus", "getGeneratedTest"), calls)
        }

        for (bad in listOf(
            null,
            GeneratedTest(id = "wrong", status = "live"),
            GeneratedTest(id = "test-1", status = "paused")
        )) {
            val repo = repository { name, _ ->
                when (name) {
                    "updateGeneratedTestStatus" -> ApiResponse<Unit>(true)
                    "getGeneratedTest" -> ApiResponse(true, bad)
                    else -> error(name)
                }
            }
            try {
                repo.updateGeneratedTestStatus("test-1", "live")
                fail("Status success requires matching persisted identity and state")
            } catch (expected: IllegalStateException) {
                assertTrue(expected.message.orEmpty().contains("not verified"))
            }
        }
    }

    @Test fun scheduleChecksServerAcknowledgmentAndAuthoritativeReadback() = runBlocking {
        val expected = 1_893_456_000_000L
        var reads = 0
        val ok = repository { name, args ->
            when (name) {
                "scheduleGeneratedTest" -> {
                    assertEquals("test-1", args[0])
                    assertEquals(mapOf("availableFrom" to expected), args[1])
                    ApiResponse(true, mapOf<String, Any>("id" to "test-1", "availableFrom" to expected.toDouble()))
                }
                "getGeneratedTest" -> {
                    reads++
                    ApiResponse(true, GeneratedTest(id = "test-1", availableFrom = expected))
                }
                else -> error(name)
            }
        }
        ok.scheduleGeneratedTest("test-1", expected)
        assertEquals(1, reads)

        for (ack in listOf(
            null,
            mapOf<String, Any>("id" to "wrong", "availableFrom" to expected.toDouble()),
            mapOf<String, Any>("id" to "test-1", "availableFrom" to 42.0),
            mapOf<String, Any>("id" to "test-1", "availableFrom" to 12.5),
            mapOf<String, Any>("id" to "test-1")
        )) {
            val repo = repository { name, _ ->
                when (name) {
                    "scheduleGeneratedTest" -> ApiResponse(true, ack)
                    "getGeneratedTest" -> error("Malformed acknowledgment must not trigger readback")
                    else -> error(name)
                }
            }
            try {
                repo.scheduleGeneratedTest("test-1", expected)
                fail("Malformed schedule acknowledgment must not show success")
            } catch (expectedError: IllegalStateException) {
                assertTrue(expectedError.message.orEmpty().contains("not acknowledged"))
            }
        }

        val mismatch = repository { name, _ ->
            when (name) {
                "scheduleGeneratedTest" -> ApiResponse(true,
                    mapOf<String, Any>("id" to "test-1", "availableFrom" to expected))
                "getGeneratedTest" -> ApiResponse(true,
                    GeneratedTest(id = "test-1", availableFrom = 0L))
                else -> error(name)
            }
        }
        try {
            mismatch.scheduleGeneratedTest("test-1", expected)
            fail("An unconfirmed schedule must not be reported as persisted")
        } catch (expectedError: IllegalStateException) {
            assertTrue(expectedError.message.orEmpty().contains("not verified"))
        }
    }

    @Test fun missingGeneratedListIsNotConfirmedEmpty() = runBlocking {
        val repo = repository { name, _ ->
            check(name == "getGeneratedTests")
            ApiResponse<List<GeneratedTest>>(true, data = null)
        }
        try {
            repo.getGeneratedTests()
            fail("Missing success data must not erase a displayed list")
        } catch (expected: IllegalStateException) {
            assertTrue(expected.message.orEmpty().contains("missing"))
        }
    }
}
