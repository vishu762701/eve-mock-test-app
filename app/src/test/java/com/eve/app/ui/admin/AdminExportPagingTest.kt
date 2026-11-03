package com.eve.app.ui.admin

import com.eve.app.data.remote.ApiResponse
import com.eve.app.data.remote.ExportExamsResponseDto
import com.eve.app.data.remote.ExportPaginationDto
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class AdminExportPagingTest {
    private fun response(offset: Int, ids: List<String>, total: Int, more: Boolean) =
        ApiResponse(success = true, data = ExportExamsResponseDto(
            exams = ids.map { mapOf("id" to it) },
            pagination = ExportPaginationDto(offset, 50, total, 4, more)
        ))

    @Test fun completePagesReturnExactConfirmedContent() = runTest {
        val result = collectVerifiedAdminExamExport({ true }) { offset, size ->
            assertEquals(50, size)
            if (offset == 0) response(0, (1..50).map { "e" + it }, 51, true)
            else response(50, listOf("e51"), 51, false)
        }
        assertEquals(51, result.exams.size)
        assertEquals(4, result.totalQuestions)
    }

    @Test fun replacementAccountAfterNetworkResponseFailsClosed() = runTest {
        var current = true
        val delayed = CompletableDeferred<ApiResponse<ExportExamsResponseDto>>()
        val job = async {
            collectVerifiedAdminExamExport({ current }) { _, _ -> delayed.await() }
        }
        runCurrent()
        current = false
        delayed.complete(response(0, listOf("e1"), 1, false))
        advanceUntilIdle()
        assertTrue(job.isCancelled)
    }

    @Test fun impossibleEmptyContinuationIsError() = runTest {
        try {
            collectVerifiedAdminExamExport({ true }) { _, _ -> response(0, emptyList(), 2, true) }
            fail("Empty continuation cannot confirm export")
        } catch (_: IOException) { }
    }

    @Test fun crossPageDuplicateIsError() = runTest {
        try {
            collectVerifiedAdminExamExport({ true }) { offset, _ ->
                if (offset == 0) response(0, (1..50).map { "e" + it }, 51, true)
                else response(50, listOf("e1"), 51, false)
            }
            fail("Duplicate record invalidates backup")
        } catch (_: IOException) { }
    }

    @Test fun malformedSuccessCannotBecomeEmptyBackup() = runTest {
        try {
            collectVerifiedAdminExamExport({ true }) { _, _ ->
                ApiResponse<ExportExamsResponseDto>(success = true, data = null)
            }
            fail("Missing payload must fail")
        } catch (_: IOException) { }
    }
}
