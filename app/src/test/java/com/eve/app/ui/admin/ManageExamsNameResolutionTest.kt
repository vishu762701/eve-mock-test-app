package com.eve.app.ui.admin

import com.eve.app.data.model.Exam
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ManageExamsNameResolutionTest {

    @Test
    fun `scenario 1 - new main exam mode with stale sub-exam resolves typed main exam name and isSavingSubExam is false`() {
        val staleSubExam = Exam(id = "sub_stale", examName = "Stale Sub Exam", parentExamId = "main_old")
        val selectedMain = Exam(id = "main_old", examName = "Old Main Exam")

        val isSavingSub = ManageExamsActivity.computeIsSavingSubExam(
            isNewSubExamMode = false,
            isNewMainExamMode = true,
            selectedSubExam = staleSubExam
        )
        assertFalse("isSavingSubExam must be false in new main exam mode even if selectedSubExam is non-null", isSavingSub)

        val resolvedName = ManageExamsActivity.resolveExamName(
            isNewSubExamMode = false,
            newSubExamTypedText = "",
            isNewMainExamMode = true,
            newMainExamTypedText = "  UPSC Civil Services  ",
            selectedSubExam = staleSubExam,
            selectedMainExam = selectedMain,
            currentExamFieldText = ""
        )
        assertEquals("UPSC Civil Services", resolvedName)
    }

    @Test
    fun `scenario 2 - new sub exam mode resolves typed sub-exam name and isSavingSubExam is true`() {
        val parentExam = Exam(id = "main_gate", examName = "GATE")

        val isSavingSub = ManageExamsActivity.computeIsSavingSubExam(
            isNewSubExamMode = true,
            isNewMainExamMode = false,
            selectedSubExam = null
        )
        assertTrue("isSavingSubExam must be true in new sub-exam mode", isSavingSub)

        val resolvedName = ManageExamsActivity.resolveExamName(
            isNewSubExamMode = true,
            newSubExamTypedText = "Computer Science & IT",
            isNewMainExamMode = false,
            newMainExamTypedText = "",
            selectedSubExam = null,
            selectedMainExam = parentExam,
            currentExamFieldText = parentExam.examName
        )
        assertEquals("Computer Science & IT", resolvedName)
    }

    @Test
    fun `scenario 3 - existing main exam resolves main exam name and isSavingSubExam is false`() {
        val mainExam = Exam(id = "main_ssc", examName = "SSC CGL")

        val isSavingSub = ManageExamsActivity.computeIsSavingSubExam(
            isNewSubExamMode = false,
            isNewMainExamMode = false,
            selectedSubExam = null
        )
        assertFalse("isSavingSubExam must be false for existing main exam", isSavingSub)

        val resolvedName = ManageExamsActivity.resolveExamName(
            isNewSubExamMode = false,
            newSubExamTypedText = "",
            isNewMainExamMode = false,
            newMainExamTypedText = "",
            selectedSubExam = null,
            selectedMainExam = mainExam,
            currentExamFieldText = mainExam.examName
        )
        assertEquals("SSC CGL", resolvedName)
    }

    @Test
    fun `scenario 4 - existing sub exam resolves sub exam name and isSavingSubExam is true`() {
        val mainExam = Exam(id = "main_ssc", examName = "SSC CGL")
        val subExam = Exam(id = "sub_math", examName = "Quantitative Aptitude", parentExamId = mainExam.id)

        val isSavingSub = ManageExamsActivity.computeIsSavingSubExam(
            isNewSubExamMode = false,
            isNewMainExamMode = false,
            selectedSubExam = subExam
        )
        assertTrue("isSavingSubExam must be true when an existing sub-exam is selected", isSavingSub)

        val resolvedName = ManageExamsActivity.resolveExamName(
            isNewSubExamMode = false,
            newSubExamTypedText = "",
            isNewMainExamMode = false,
            newMainExamTypedText = "",
            selectedSubExam = subExam,
            selectedMainExam = mainExam,
            currentExamFieldText = mainExam.examName
        )
        assertEquals("Quantitative Aptitude", resolvedName)
    }

    @Test
    fun `test negative marking parsing helper`() {
        assertEquals("0" to 0.0, ManageExamsActivity.parseNegativeMarking("0"))
        assertEquals("0.25" to 0.25, ManageExamsActivity.parseNegativeMarking("0.25"))
        assertEquals("1/3" to (1.0 / 3.0), ManageExamsActivity.parseNegativeMarking("1/3"))
        assertEquals("1/4" to 0.25, ManageExamsActivity.parseNegativeMarking("1/4"))
        assertEquals(null, ManageExamsActivity.parseNegativeMarking("abc"))
        assertEquals(null, ManageExamsActivity.parseNegativeMarking(""))
    }
}
