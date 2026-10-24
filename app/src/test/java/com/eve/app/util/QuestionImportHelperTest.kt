package com.eve.app.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream

class QuestionImportHelperTest {

    @Test
    fun testValidCsvImport() {
        val csv = """
            Question,OptionA,OptionB,OptionC,OptionD,CorrectOption,Explanation,Topic
            What is 2+2?,1,2,3,4,D,Basic addition,Math
            What is the capital of India?,Mumbai,New Delhi,Kolkata,Chennai,B,Capital city,GK
        """.trimIndent()

        val stream = ByteArrayInputStream(csv.toByteArray(Charsets.UTF_8))
        val result = QuestionImportHelper.parseQuestions(stream, "sample.csv", "exam-1")

        assertEquals(2, result.validQuestions.size)
        assertEquals(0, result.errors.size)
        assertEquals(2, result.totalRowsProcessed)

        val q1 = result.validQuestions[0]
        assertEquals("What is 2+2?", q1.questionText)
        assertEquals("D", q1.correctAnswer)
        assertEquals("Math", q1.topic)

        val q2 = result.validQuestions[1]
        assertEquals("What is the capital of India?", q2.questionText)
        assertEquals("B", q2.correctAnswer)
    }

    @Test
    fun testErrorReportingOnInvalidCorrectOptionAndMissingFields() {
        val csv = """
            Question,OptionA,OptionB,OptionC,OptionD,CorrectOption,Explanation
            Valid question?,A,B,C,D,A,Good
            Invalid option question?,A,B,C,D,X,Bad answer option
            Missing option question?,A,B,C,,B,Option D is empty
            ,A,B,C,D,C,Missing question
        """.trimIndent()

        val stream = ByteArrayInputStream(csv.toByteArray(Charsets.UTF_8))
        val result = QuestionImportHelper.parseQuestions(stream, "sample.csv", "exam-1")

        assertEquals(1, result.validQuestions.size)
        assertEquals(3, result.errors.size)
        assertEquals(4, result.totalRowsProcessed)

        // Check error row numbers and reasons
        assertEquals(3, result.errors[0].rowNumber)
        assertTrue(result.errors[0].reason.contains("Invalid CorrectOption 'X'"))

        assertEquals(4, result.errors[1].rowNumber)
        assertTrue(result.errors[1].reason.contains("Missing option(s): D"))

        assertEquals(5, result.errors[2].rowNumber)
        assertTrue(result.errors[2].reason.contains("Question text is empty"))
    }

    @Test
    fun testCsvWithQuotedCommas() {
        val csv = """
            "Question, with comma","Option, A","Option, B","Option, C","Option, D",A,"Explanation, detailed",Math
        """.trimIndent()

        val stream = ByteArrayInputStream(csv.toByteArray(Charsets.UTF_8))
        val result = QuestionImportHelper.parseQuestions(stream, "sample.csv", "exam-1")

        assertEquals(1, result.validQuestions.size)
        assertEquals(0, result.errors.size)
        assertEquals("Question, with comma", result.validQuestions[0].questionText)
        assertEquals("Option, A", result.validQuestions[0].optionA)
    }

    @Test
    fun testNumericCorrectOptionNormalisation() {
        val csv = """
            Question,OptionA,OptionB,OptionC,OptionD,CorrectOption
            Q1,A,B,C,D,1
            Q2,A,B,C,D,2
            Q3,A,B,C,D,3
            Q4,A,B,C,D,4
        """.trimIndent()

        val stream = ByteArrayInputStream(csv.toByteArray(Charsets.UTF_8))
        val result = QuestionImportHelper.parseQuestions(stream, "sample.csv", "exam-1")

        assertEquals(4, result.validQuestions.size)
        assertEquals("A", result.validQuestions[0].correctAnswer)
        assertEquals("B", result.validQuestions[1].correctAnswer)
        assertEquals("C", result.validQuestions[2].correctAnswer)
        assertEquals("D", result.validQuestions[3].correctAnswer)
    }
}
