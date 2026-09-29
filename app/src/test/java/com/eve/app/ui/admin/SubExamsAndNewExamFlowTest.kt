package com.eve.app.ui.admin

import com.eve.app.data.model.Exam
import com.eve.app.data.model.GeneratedQuestion
import com.eve.app.data.model.GeneratedTest
import com.eve.app.ui.home.ExamTestsActivity
import com.eve.app.util.AttemptKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SubExamsAndNewExamFlowTest {

    @Test
    fun `test new exam mode creates clean empty form`() {
        val newExam = Exam(
            id = "",
            examName = "",
            timeLimitMinutes = 0,
            testNumber = "",
            questionCount = 0,
            negativeMarkingText = "",
            negativeMarkingValue = 0.0,
            parentExamId = "",
            autoGenerationEnabled = false,
            autoGenTime = "00:00",
            syllabusUrl = "",
            syllabusFileName = "",
            generationPrompt = ""
        )

        assertEquals("", newExam.id)
        assertEquals("", newExam.examName)
        assertEquals(0, newExam.timeLimitMinutes)
        assertEquals("", newExam.testNumber)
        assertEquals(0, newExam.questionCount)
        assertEquals("", newExam.negativeMarkingText)
        assertEquals(0.0, newExam.negativeMarkingValue, 0.001)
        assertEquals("", newExam.parentExamId)
        assertTrue(newExam.isMainExam)
        assertFalse(newExam.autoGenerationEnabled)
        assertEquals("00:00", newExam.autoGenTime)
        assertEquals("", newExam.syllabusUrl)
        assertEquals("", newExam.generationPrompt)
    }

    @Test
    fun `test sub-exam and main exam distinction`() {
        val mainExam = Exam(
            id = "main_rssb_3rd",
            examName = "RSSB 3rd Grade",
            parentExamId = ""
        )
        val subExam1 = Exam(
            id = "sub_hindi",
            examName = "Hindi",
            parentExamId = "main_rssb_3rd",
            timeLimitMinutes = 30,
            negativeMarkingText = "1/3",
            negativeMarkingValue = 1.0 / 3.0
        )
        val subExam2 = Exam(
            id = "sub_maths",
            examName = "Maths",
            parentExamId = "main_rssb_3rd",
            timeLimitMinutes = 45,
            negativeMarkingText = "0.25",
            negativeMarkingValue = 0.25
        )
        val standaloneExam = Exam(
            id = "main_cet",
            examName = "Rajasthan CET",
            parentExamId = ""
        )

        val allExams = listOf(mainExam, subExam1, subExam2, standaloneExam)

        // Rule 1: Home shows ONLY main exams
        val homeExams = allExams.filter { it.isMainExam }
        assertEquals(2, homeExams.size)
        assertEquals(listOf("RSSB 3rd Grade", "Rajasthan CET"), homeExams.map { it.examName })

        // Rule 2: Tap main exam that HAS sub-exams -> list of its sub-exams
        val rssbSubExams = allExams.filter { it.parentExamId == mainExam.id }
        assertEquals(2, rssbSubExams.size)
        assertEquals(listOf("Hindi", "Maths"), rssbSubExams.map { it.examName })

        // Rule 3: Tap main exam that has NO sub-exams -> empty sub-exams list (routes directly to tests)
        val cetSubExams = allExams.filter { it.parentExamId == standaloneExam.id }
        assertTrue(cetSubExams.isEmpty())

        assertTrue(mainExam.isMainExam)
        assertFalse(subExam1.isMainExam)
        assertFalse(subExam2.isMainExam)
        assertTrue(standaloneExam.isMainExam)
    }

    @Test
    fun `test AttemptKey parsing and formatting`() {
        val testKey = AttemptKey.forTest("exam_123", "test_456")
        assertEquals("exam_123__test_456", testKey)
        assertTrue(AttemptKey.isTestKey(testKey))
        assertEquals("exam_123", AttemptKey.sourceExamId(testKey))
        assertEquals("test_456", AttemptKey.generatedTestId(testKey))

        val legacyKey = "exam_123"
        assertFalse(AttemptKey.isTestKey(legacyKey))
        assertEquals("exam_123", AttemptKey.sourceExamId(legacyKey))
        assertNull(AttemptKey.generatedTestId(legacyKey))
    }

    @Test
    fun `test test number integer extraction and sorting`() {
        assertEquals(1, ExamTestsActivity.extractEndingInteger("Test 1"))
        assertEquals(12, ExamTestsActivity.extractEndingInteger("Test 12"))
        assertEquals(3, ExamTestsActivity.extractEndingInteger("Special Exam Test 3"))
        assertNull(ExamTestsActivity.extractEndingInteger("Test Without Number"))

        val testList = listOf(
            GeneratedTest(id = "1", examId = "e", examName = "E", testNumber = "Test 10", generatedAt = 100),
            GeneratedTest(id = "2", examId = "e", examName = "E", testNumber = "Test 2", generatedAt = 200),
            GeneratedTest(id = "3", examId = "e", examName = "E", testNumber = "Test 1", generatedAt = 300),
            GeneratedTest(id = "4", examId = "e", examName = "E", testNumber = "Diagnostic Test", generatedAt = 50)
        )

        val sorted = testList.sortedWith(
            compareBy<GeneratedTest> {
                ExamTestsActivity.extractEndingInteger(it.testNumber) ?: Int.MAX_VALUE
            }.thenBy { it.generatedAt }
        )

        assertEquals(listOf("Test 1", "Test 2", "Test 10", "Diagnostic Test"), sorted.map { it.testNumber })
    }

    @Test
    fun `test each generated test is independent with own lock and result identification`() {
        val test1 = GeneratedTest(
            id = "gen_test_001",
            examId = "sub_hindi",
            examName = "Hindi",
            testNumber = "Test 1",
            title = "Hindi - Test 1",
            status = "live",
            questionCount = 20,
            questions = listOf(
                GeneratedQuestion(questionText = "Q1", optionA = "A", optionB = "B", optionC = "C", optionD = "D", correctAnswer = "A")
            )
        )
        val test2 = GeneratedTest(
            id = "gen_test_002",
            examId = "sub_hindi",
            examName = "Hindi",
            testNumber = "Test 2",
            title = "Hindi - Test 2",
            status = "live",
            questionCount = 20,
            questions = listOf(
                GeneratedQuestion(questionText = "Q1", optionA = "A", optionB = "B", optionC = "C", optionD = "D", correctAnswer = "B")
            )
        )

        val key1 = AttemptKey.forTest("sub_hindi", test1.id)
        val key2 = AttemptKey.forTest("sub_hindi", test2.id)

        val userLocks = mutableSetOf(key1)
        assertTrue(userLocks.contains(key1))
        assertFalse(userLocks.contains(key2))

        assertEquals("sub_hindi__gen_test_001", key1)
        assertEquals("sub_hindi__gen_test_002", key2)
    }
}
