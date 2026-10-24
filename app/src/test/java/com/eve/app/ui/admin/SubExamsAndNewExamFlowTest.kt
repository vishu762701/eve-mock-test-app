package com.eve.app.ui.admin

import com.eve.app.data.model.Exam
import com.eve.app.data.model.GeneratedQuestion
import com.eve.app.data.model.GeneratedTest
import com.eve.app.ui.home.ExamTestsActivity
import com.eve.app.ui.home.ExamTestsListItem
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

    @Test
    fun `test main exam without children shows direct tests only`() {
        val test = GeneratedTest(id = "t1", examId = "main_1", examName = "Main", testNumber = "Test 1")
        val testItem = ExamTestsListItem.TestItem(test, "Test 1", "20 questions", isCompleted = false)
        val list = ExamTestsActivity.buildExamTestsList(listOf(testItem), emptyList())

        assertEquals(1, list.size)
        assertTrue(list[0] is ExamTestsListItem.TestItem)
        assertEquals("Test 1", (list[0] as ExamTestsListItem.TestItem).title)
    }

    @Test
    fun `test main exam with children and direct tests shows BOTH in deterministic order`() {
        val test1 = GeneratedTest(id = "t1", examId = "main_1", examName = "Main", testNumber = "Test 1")
        val test2 = GeneratedTest(id = "t2", examId = "main_1", examName = "Main", testNumber = "Test 2")
        val testItem1 = ExamTestsListItem.TestItem(test1, "Test 1", "20 questions", isCompleted = false)
        val testItem2 = ExamTestsListItem.TestItem(test2, "Test 2", "20 questions", isCompleted = false)

        val child1 = Exam(id = "sub_1", examName = "English", parentExamId = "main_1")
        val child2 = Exam(id = "sub_2", examName = "Hindi", parentExamId = "main_1")
        val subExamItem1 = ExamTestsListItem.SubExamItem(child1)
        val subExamItem2 = ExamTestsListItem.SubExamItem(child2)

        val list = ExamTestsActivity.buildExamTestsList(
            listOf(testItem1, testItem2),
            listOf(subExamItem1, subExamItem2)
        )

        // Structure: Header "Tests", Test 1, Test 2, Header "Sub-Exams", Sub 1, Sub 2
        assertEquals(6, list.size)
        assertTrue(list[0] is ExamTestsListItem.HeaderItem)
        assertEquals("Tests", (list[0] as ExamTestsListItem.HeaderItem).title)
        assertTrue(list[1] is ExamTestsListItem.TestItem)
        assertEquals("Test 1", (list[1] as ExamTestsListItem.TestItem).title)
        assertTrue(list[2] is ExamTestsListItem.TestItem)
        assertEquals("Test 2", (list[2] as ExamTestsListItem.TestItem).title)
        assertTrue(list[3] is ExamTestsListItem.HeaderItem)
        assertEquals("Sub-Exams", (list[3] as ExamTestsListItem.HeaderItem).title)
        assertTrue(list[4] is ExamTestsListItem.SubExamItem)
        assertEquals("English", (list[4] as ExamTestsListItem.SubExamItem).exam.examName)
        assertTrue(list[5] is ExamTestsListItem.SubExamItem)
        assertEquals("Hindi", (list[5] as ExamTestsListItem.SubExamItem).exam.examName)
    }

    @Test
    fun `test main exam with children but no direct tests shows children`() {
        val child1 = Exam(id = "sub_1", examName = "English", parentExamId = "main_1")
        val subExamItem1 = ExamTestsListItem.SubExamItem(child1)
        val list = ExamTestsActivity.buildExamTestsList(emptyList(), listOf(subExamItem1))

        assertEquals(1, list.size)
        assertTrue(list[0] is ExamTestsListItem.SubExamItem)
        assertEquals("English", (list[0] as ExamTestsListItem.SubExamItem).exam.examName)
    }

    @Test
    fun `test sub-exam shows only its own tests`() {
        val subExamTest = GeneratedTest(id = "st1", examId = "sub_hindi", examName = "Hindi", testNumber = "Test 1")
        val testItem = ExamTestsListItem.TestItem(subExamTest, "Test 1", "20 questions", isCompleted = false)
        val list = ExamTestsActivity.buildExamTestsList(listOf(testItem), emptyList())

        assertEquals(1, list.size)
        assertTrue(list[0] is ExamTestsListItem.TestItem)
        assertEquals("sub_hindi", (list[0] as ExamTestsListItem.TestItem).test.examId)
    }

    @Test
    fun `test generated test ownership and scope uses exam ID not exam name`() {
        val mainExam = Exam(id = "exam_rssb_3rd", examName = "RSSB 3rd Grade")
        val subExam = Exam(id = "exam_rssb_hindi", examName = "RSSB 3rd Grade", parentExamId = mainExam.id)

        val parentTest = GeneratedTest(id = "t_p", examId = mainExam.id, examName = mainExam.examName)
        val childTest = GeneratedTest(id = "t_c", examId = subExam.id, examName = subExam.examName)

        assertEquals("exam_rssb_3rd", parentTest.examId)
        assertEquals("exam_rssb_hindi", childTest.examId)
        assertFalse("Parent and child tests must not share exam ID even if names are identical", parentTest.examId == childTest.examId)
    }

    @Test
    fun `test renamed exam retains its generated tests because ownership is by ID`() {
        val test = GeneratedTest(id = "test_1", examId = "main_cet", examName = "Old CET Name")
        val updatedExam = Exam(id = "main_cet", examName = "New CET Name")

        assertEquals(test.examId, updatedExam.id)
    }
}
