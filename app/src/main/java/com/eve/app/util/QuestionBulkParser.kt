package com.eve.app.util

import com.eve.app.data.model.DailyQuestion
import com.eve.app.data.model.Question

data class BulkRowError(val rowNumber: Int, val reason: String)

data class ExamBulkParseResult(
    val questions: List<Question>,
    val errors: List<BulkRowError>
)

data class DailyBulkParseResult(
    val questions: List<DailyQuestion>,
    val errors: List<BulkRowError>
)

/**
 * Phase 21: spreadsheet headers ko Question / DailyQuestion me map karta hai.
 * Header names case-insensitive, extra spaces ignore.
 */
object QuestionBulkParser {

    fun parseExamQuestions(sheet: SpreadsheetReader.Sheet, examId: String): ExamBulkParseResult {
        val idx = HeaderIndex(sheet.headers)
        if (idx.question < 0 || idx.a < 0 || idx.b < 0 || idx.c < 0 || idx.d < 0 || idx.correct < 0) {
            throw IllegalArgumentException(
                "CSV/Excel me yeh columns zaroori hain: questionText, optionA, optionB, optionC, optionD, correctAnswer"
            )
        }
        val ok = mutableListOf<Question>()
        val errors = mutableListOf<BulkRowError>()
        sheet.rows.forEachIndexed { i, row ->
            val line = i + 2 // header = row 1
            try {
                val qText = row.cell(idx.question)
                val a = row.cell(idx.a)
                val b = row.cell(idx.b)
                val c = row.cell(idx.c)
                val d = row.cell(idx.d)
                val correct = normalizeCorrect(row.cell(idx.correct))
                if (qText.isBlank() || a.isBlank() || b.isBlank() || c.isBlank() || d.isBlank()) {
                    errors += BulkRowError(line, "Question / options khali hain")
                    return@forEachIndexed
                }
                if (correct !in setOf("A", "B", "C", "D")) {
                    errors += BulkRowError(line, "correctAnswer A/B/C/D hona chahiye (mila: ${row.cell(idx.correct)})")
                    return@forEachIndexed
                }
                val isPyq = parseBool(row.cell(idx.isPyq))
                val year = row.cell(idx.pyqYear).toIntOrNull() ?: 0
                if (isPyq && year !in 1990..2100) {
                    errors += BulkRowError(line, "PYQ row me valid year chahiye (e.g. 2024)")
                    return@forEachIndexed
                }
                ok += Question(
                    examId = examId,
                    questionText = qText,
                    optionA = a,
                    optionB = b,
                    optionC = c,
                    optionD = d,
                    correctAnswer = correct,
                    explanation = row.cell(idx.explanation),
                    topic = row.cell(idx.topic),
                    isPyq = isPyq,
                    pyqYear = if (isPyq) year else 0,
                    pyqPaper = if (isPyq) row.cell(idx.pyqPaper) else "",
                    questionTextHi = row.cell(idx.questionHi),
                    optionAHi = row.cell(idx.aHi),
                    optionBHi = row.cell(idx.bHi),
                    optionCHi = row.cell(idx.cHi),
                    optionDHi = row.cell(idx.dHi),
                    explanationHi = row.cell(idx.explanationHi)
                )
            } catch (e: Exception) {
                errors += BulkRowError(line, e.message ?: "Row parse fail")
            }
        }
        return ExamBulkParseResult(ok, errors)
    }

    fun parseDailyQuestions(sheet: SpreadsheetReader.Sheet, fallbackDate: String): DailyBulkParseResult {
        val idx = HeaderIndex(sheet.headers)
        if (idx.question < 0 || idx.a < 0 || idx.b < 0 || idx.c < 0 || idx.d < 0 || idx.correct < 0) {
            throw IllegalArgumentException(
                "CSV/Excel me yeh columns zaroori hain: questionText, optionA, optionB, optionC, optionD, correctAnswer"
            )
        }
        val ok = mutableListOf<DailyQuestion>()
        val errors = mutableListOf<BulkRowError>()
        sheet.rows.forEachIndexed { i, row ->
            val line = i + 2
            try {
                val dateRaw = row.cell(idx.date).ifBlank { fallbackDate }
                val date = normalizeDate(dateRaw)
                if (date == null) {
                    errors += BulkRowError(line, "Date yyyy-MM-dd me daalo (mila: $dateRaw)")
                    return@forEachIndexed
                }
                val qText = row.cell(idx.question)
                val a = row.cell(idx.a)
                val b = row.cell(idx.b)
                val c = row.cell(idx.c)
                val d = row.cell(idx.d)
                val correct = normalizeCorrect(row.cell(idx.correct))
                if (qText.isBlank() || a.isBlank() || b.isBlank() || c.isBlank() || d.isBlank()) {
                    errors += BulkRowError(line, "Question / options khali hain")
                    return@forEachIndexed
                }
                if (correct !in setOf("A", "B", "C", "D")) {
                    errors += BulkRowError(line, "correctAnswer A/B/C/D hona chahiye")
                    return@forEachIndexed
                }
                ok += DailyQuestion(
                    date = date,
                    questionText = qText,
                    optionA = a,
                    optionB = b,
                    optionC = c,
                    optionD = d,
                    correctAnswer = correct,
                    explanation = row.cell(idx.explanation),
                    topic = row.cell(idx.topic),
                    questionTextHi = row.cell(idx.questionHi),
                    optionAHi = row.cell(idx.aHi),
                    optionBHi = row.cell(idx.bHi),
                    optionCHi = row.cell(idx.cHi),
                    optionDHi = row.cell(idx.dHi),
                    explanationHi = row.cell(idx.explanationHi)
                )
            } catch (e: Exception) {
                errors += BulkRowError(line, e.message ?: "Row parse fail")
            }
        }
        return DailyBulkParseResult(ok, errors)
    }

    private fun List<String>.cell(index: Int): String =
        if (index < 0 || index >= size) "" else this[index].trim()

    private fun normalizeCorrect(raw: String): String {
        val v = raw.trim().uppercase()
        return when (v) {
            "A", "B", "C", "D" -> v
            "1", "OPTION A", "OPT A", "ANS A" -> "A"
            "2", "OPTION B", "OPT B", "ANS B" -> "B"
            "3", "OPTION C", "OPT C", "ANS C" -> "C"
            "4", "OPTION D", "OPT D", "ANS D" -> "D"
            else -> v.take(1)
        }
    }

    private fun parseBool(raw: String): Boolean {
        val v = raw.trim().lowercase()
        return v in setOf("1", "true", "yes", "y", "pyq", "haan", "ha")
    }

    private fun normalizeDate(raw: String): String? {
        val v = raw.trim()
        if (v.matches(Regex("\\d{4}-\\d{2}-\\d{2}"))) return v
        val slashed = Regex("(\\d{1,2})[/-](\\d{1,2})[/-](\\d{4})").matchEntire(v)
        if (slashed != null) {
            val d = slashed.groupValues[1].padStart(2, '0')
            val m = slashed.groupValues[2].padStart(2, '0')
            val y = slashed.groupValues[3]
            // Prefer yyyy-MM-dd; Indian files often dd/MM/yyyy
            return "$y-$m-$d"
        }
        return null
    }

    private class HeaderIndex(headers: List<String>) {
        private val map = headers.mapIndexed { i, h -> normalize(h) to i }.toMap()
        val question = find("questiontext", "question", "q", "ques")
        val a = find("optiona", "a", "opta")
        val b = find("optionb", "b", "optb")
        val c = find("optionc", "c", "optc")
        val d = find("optiond", "d", "optd")
        val correct = find("correctanswer", "correct", "answer", "ans")
        val explanation = find("explanation", "explain", "reason")
        val topic = find("topic", "subject")
        val isPyq = find("ispyq", "pyq")
        val pyqYear = find("pyqyear", "year")
        val pyqPaper = find("pyqpaper", "paper", "shift")
        val questionHi = find("questiontexthi", "questionhi", "qhi")
        val aHi = find("optionahi", "ahi")
        val bHi = find("optionbhi", "bhi")
        val cHi = find("optionchi", "chi")
        val dHi = find("optiondhi", "dhi")
        val explanationHi = find("explanationhi", "explainhi")
        val date = find("date", "quizdate", "day")

        private fun find(vararg keys: String): Int = keys.firstNotNullOfOrNull { map[it] } ?: -1

        private fun normalize(h: String): String =
            h.lowercase().replace(Regex("[^a-z0-9]"), "")
    }
}
