package com.eve.app.designsystem

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UploadStepStateTest {
    @Test fun neverShowsCompletionWithoutValidatedExamAndPdf() {
        val noExam = UploadStepState.from(false, false, true, true, true)
        assertEquals(0, noExam.progress)
        assertFalse(noExam.uploaded)
        val noPdf = UploadStepState.from(true, false, false, true, false)
        assertEquals(1, noPdf.progress)
        assertFalse(noPdf.uploaded)
    }
    @Test fun confirmedUploadIsOnlyFinalStage() {
        assertEquals(2, UploadStepState.from(true, true, false, false, false).progress)
        assertTrue(UploadStepState.from(true, true, true, false, false).uploading)
        assertEquals(3, UploadStepState.from(true, true, false, true, false).progress)
        assertTrue(UploadStepState.from(true, true, false, true, false).uploaded)
    }
    @Test fun errorAndRetryDoNotClaimSuccess() {
        val failed = UploadStepState.from(true, true, false, false, true)
        assertTrue(failed.failed)
        assertFalse(failed.uploaded)
        assertEquals(2, failed.progress)
        val retry = UploadStepState.from(true, true, true, false, false)
        assertTrue(retry.uploading)
        assertFalse(retry.failed)
        assertFalse(retry.uploaded)
    }
    @Test fun interruptedRequestResetsToTruthfulCurrentFormState() {
        val restored = UploadStepState.from(true, false, false, false, false)
        assertEquals(1, restored.progress)
        assertFalse(restored.uploading)
    }
}
