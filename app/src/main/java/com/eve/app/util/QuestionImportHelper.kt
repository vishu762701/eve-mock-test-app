package com.eve.app.util

import com.eve.app.R
import com.eve.app.data.model.Question
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.util.zip.ZipInputStream

object QuestionImportHelper {
    // EVE safety budgets; these are not Telegram-source values. Worker batch maximum is 300.
    const val MAX_BATCH_QUESTIONS = 300
    const val MAX_INPUT_BYTES = 8 * 1024 * 1024
    const val MAX_EXPANDED_BYTES = 16 * 1024 * 1024
    const val MAX_ROWS = 10000
    const val MAX_COLUMNS = 64
    const val MAX_CELL_CHARS = 65536
    const val MAX_SHARED_STRINGS = 80000

    enum class Failure(val messageRes: Int, val description: String) {
        INPUT(R.string.question_import_limit_input, "Input exceeds the 8 MiB import limit"),
        EXPANDED(R.string.question_import_limit_expanded, "Workbook exceeds the 16 MiB expanded-data limit"),
        ROWS(R.string.question_import_limit_rows, "Document exceeds 10000 rows"),
        COLUMNS(R.string.question_import_limit_columns, "Document exceeds 64 columns"),
        CELL(R.string.question_import_limit_cell, "A cell exceeds 65536 characters"),
        SHARED(R.string.question_import_limit_shared, "Workbook exceeds 80000 shared strings"),
        BATCH(R.string.question_import_limit_batch, "Provide at most 300 questions; split the file into smaller batches"),
        MALFORMED(R.string.question_import_malformed, "Invalid or unsupported document format")
    }
    private class ImportFailure(val failure: Failure) : java.io.IOException(failure.description)
    data class RowError(val rowNumber: Int, val snippet: String, val reason: String)
    data class ImportResult(val validQuestions: List<Question>, val errors: List<RowError>,
        val totalRowsProcessed: Int, val failure: Failure? = null)

    private fun failed(failure: Failure) = ImportResult(emptyList(),
        listOf(RowError(1, "", failure.description)), 0, failure)
    private inline fun checkedParse(parse: () -> ImportResult): ImportResult = try { parse() }
        catch (cancel: kotlinx.coroutines.CancellationException) { throw cancel }
        catch (limit: ImportFailure) { failed(limit.failure) }
        catch (_: Exception) { failed(Failure.MALFORMED) }

    // The caller owns its input; ZIP close releases inflater state without closing that input.
    private class InputBudget(input: InputStream) : java.io.FilterInputStream(input) {
        private var count = 0L
        private val single = ByteArray(1)
        override fun read(): Int = if (read(single, 0, 1) < 0) -1 else single[0].toInt() and 255
        override fun read(b: ByteArray, off: Int, len: Int): Int {
            val read = super.read(b, off, minOf(len.toLong(), MAX_INPUT_BYTES - count + 1).toInt())
            if (read > 0) { count += read; if (count > MAX_INPUT_BYTES) throw ImportFailure(Failure.INPUT) }
            return read
        }
        override fun close() = Unit
    }
    private class ExpandedZip(input: InputStream) : ZipInputStream(input) {
        private var count = 0L
        override fun read(b: ByteArray, off: Int, len: Int): Int {
            val read = super.read(b, off, minOf(len.toLong(), MAX_EXPANDED_BYTES - count + 1).toInt())
            if (read > 0) { count += read; if (count > MAX_EXPANDED_BYTES) throw ImportFailure(Failure.EXPANDED) }
            return read
        }
    }

    fun parseQuestions(inputStream: InputStream, fileName: String, examId: String = ""): ImportResult =
        if (fileName.endsWith(".xlsx", ignoreCase = true)) parseXlsx(inputStream, examId)
        else parseCsv(inputStream, examId)

    fun parseCsv(inputStream: InputStream, examId: String = ""): ImportResult = checkedParse {
        processParsedRows(parseCsvRows(BufferedReader(InputStreamReader(InputBudget(inputStream), Charsets.UTF_8))), examId)
    }

    fun parseXlsx(inputStream: InputStream, examId: String = ""): ImportResult = checkedParse {
        val strings = mutableListOf<String>()
        val rows = mutableListOf<List<String>>()
        var sheet: ByteArray? = null
        ExpandedZip(InputBudget(inputStream)).use { zip ->
            var entry = zip.nextEntry
            var foundStrings = false
            while (entry != null) {
                when (entry.name) {
                    "xl/sharedStrings.xml" -> {
                        if (foundStrings) throw ImportFailure(Failure.MALFORMED)
                        foundStrings = true
                        parseSharedStrings(zip, strings)
                    }
                    "xl/worksheets/sheet1.xml" -> {
                        if (sheet != null) throw ImportFailure(Failure.MALFORMED)
                        sheet = zip.readBytes()
                    }
                }
                // ExpandedZip counts closeEntry draining too, including unused ZIP entries.
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
        val bytes = sheet ?: throw ImportFailure(Failure.MALFORMED)
        parseSheetXml(bytes.inputStream(), strings, rows)
        processParsedRows(rows, examId)
    }

    private fun parser(stream: InputStream): XmlPullParser = XmlPullParserFactory.newInstance().apply {
        isNamespaceAware = true
    }.newPullParser().apply {
        setFeature(XmlPullParser.FEATURE_PROCESS_DOCDECL, false)
        setInput(stream, "UTF-8")
    }
    private fun nextEvent(parser: XmlPullParser): Int {
        val event = parser.nextToken()
        if (event == XmlPullParser.DOCDECL || event == XmlPullParser.ENTITY_REF && parser.text == null)
            throw ImportFailure(Failure.MALFORMED)
        return event
    }
    private fun appendCell(out: StringBuilder, value: String) {
        if (out.length.toLong() + value.length > MAX_CELL_CHARS) throw ImportFailure(Failure.CELL)
        out.append(value)
    }
    private fun isText(event: Int) = event == XmlPullParser.TEXT || event == XmlPullParser.CDSECT || event == XmlPullParser.ENTITY_REF

    private fun parseSharedStrings(stream: InputStream, out: MutableList<String>) {
        val parser = parser(stream)
        val text = StringBuilder()
        var insideItem = false
        var insideText = false
        var phonetic = 0
        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> when (parser.name) {
                    "si" -> { insideItem = true; text.clear() }
                    "rPh" -> if (insideItem) phonetic++
                    "t" -> insideText = insideItem && phonetic == 0
                }
                XmlPullParser.END_TAG -> when (parser.name) {
                    "t" -> insideText = false
                    "rPh" -> if (insideItem) phonetic--
                    "si" -> {
                        if (out.size == MAX_SHARED_STRINGS) throw ImportFailure(Failure.SHARED)
                        out.add(text.toString()); insideItem = false
                    }
                }
                else -> if (isText(event) && insideText) appendCell(text, parser.text.orEmpty())
            }
            event = nextEvent(parser)
        }
    }
    private fun column(reference: String?, fallback: Int): Int {
        if (reference.isNullOrEmpty()) return fallback
        val letters = reference.takeWhile { it in 'A'..'Z' || it in 'a'..'z' }
        val digits = reference.drop(letters.length)
        if (letters.isEmpty() || digits.isEmpty() || digits.first() == '0' || digits.any { it !in '0'..'9' })
            throw ImportFailure(Failure.MALFORMED)
        var value = 0
        for (c in letters.uppercase(java.util.Locale.ROOT)) {
            value = value * 26 + (c - 'A' + 1)
            if (value > MAX_COLUMNS) throw ImportFailure(Failure.COLUMNS)
        }
        return value - 1
    }
    private fun parseSheetXml(stream: InputStream, strings: List<String>, out: MutableList<List<String>>) {
        val parser = parser(stream)
        var row = mutableMapOf<Int, String>()
        var inRow = false
        var index = 0
        var nextColumn = 0
        var type = ""
        var insideCell = false
        var insideValue = false
        var insideText = false
        var phonetic = 0
        val value = StringBuilder()
        val inline = StringBuilder()
        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> when (parser.name) {
                    "row" -> {
                        if (out.size == MAX_ROWS) throw ImportFailure(Failure.ROWS)
                        row = mutableMapOf(); inRow = true; nextColumn = 0
                    }
                    "c" -> {
                        if (!inRow) throw ImportFailure(Failure.MALFORMED)
                        index = column(parser.getAttributeValue(null, "r"), nextColumn)
                        if (index >= MAX_COLUMNS) throw ImportFailure(Failure.COLUMNS)
                        if (row.containsKey(index)) throw ImportFailure(Failure.MALFORMED)
                        type = parser.getAttributeValue(null, "t").orEmpty()
                        value.clear(); inline.clear(); insideCell = true
                    }
                    "v" -> insideValue = insideCell
                    "rPh" -> if (insideCell) phonetic++
                    "t" -> insideText = insideCell && type == "inlineStr" && phonetic == 0
                }
                XmlPullParser.END_TAG -> when (parser.name) {
                    "v" -> insideValue = false
                    "t" -> insideText = false
                    "rPh" -> if (insideCell) phonetic--
                    "c" -> {
                        val raw = value.toString().trim()
                        row[index] = when (type) {
                            "s" -> strings.getOrNull(raw.toIntOrNull() ?: -1) ?: throw ImportFailure(Failure.MALFORMED)
                            "inlineStr" -> inline.toString()
                            "b" -> when (raw) { "1" -> "TRUE"; "0" -> "FALSE"; else -> throw ImportFailure(Failure.MALFORMED) }
                            "e" -> ""
                            else -> raw
                        }
                        nextColumn = index + 1; insideCell = false
                    }
                    "row" -> {
                        out.add(List((row.keys.maxOrNull() ?: -1) + 1) { row[it].orEmpty() }); inRow = false
                    }
                }
                else -> if (isText(event)) {
                    if (insideValue) appendCell(value, parser.text.orEmpty())
                    if (insideText) appendCell(inline, parser.text.orEmpty())
                }
            }
            event = nextEvent(parser)
        }
    }

    private fun parseCsvRows(reader: BufferedReader): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        var inQuotes = false
        val cell = StringBuilder()
        val row = mutableListOf<String>()
        fun endCell() {
            if (row.size == MAX_COLUMNS) throw ImportFailure(Failure.COLUMNS)
            row.add(cell.toString().trim()); cell.clear()
        }
        fun endRow() {
            if (row.any { it.isNotBlank() }) {
                if (rows.size == MAX_ROWS) throw ImportFailure(Failure.ROWS)
                rows.add(row.toList())
            }
            row.clear()
        }
        var line = reader.readLine()
        while (line != null) {
            var i = 0
            while (i < line.length) {
                val c = line[i]
                if (c == '"') {
                    if (inQuotes && i + 1 < line.length && line[i + 1] == '"') { appendCell(cell, "\""); i++ }
                    else inQuotes = !inQuotes
                } else if (c == ',' && !inQuotes) endCell()
                else {
                    if (cell.length == MAX_CELL_CHARS) throw ImportFailure(Failure.CELL)
                    cell.append(c)
                }
                i++
            }
            if (!inQuotes) { endCell(); endRow() } else appendCell(cell, "\n")
            line = reader.readLine()
        }
        if (inQuotes) throw ImportFailure(Failure.MALFORMED)
        if (cell.isNotEmpty() || row.isNotEmpty()) { endCell(); endRow() }
        return rows
    }

    private fun processParsedRows(
        rows: List<List<String>>,
        examId: String
    ): ImportResult {
        if (rows.isEmpty()) {
            return ImportResult(emptyList(), listOf(RowError(1, "", "File is completely empty")), 0)
        }

        val firstRow = rows.first()
        val isHeader = isHeaderRow(firstRow)

        var qCol = 0
        var aCol = 1
        var bCol = 2
        var cCol = 3
        var dCol = 4
        var ansCol = 5
        var expCol = 6
        var topicCol = 7

        val dataRows: List<List<String>>
        val startingRowNumber: Int

        if (isHeader) {
            dataRows = rows.drop(1)
            startingRowNumber = 2

            firstRow.forEachIndexed { index, title ->
                val clean = title.trim().lowercase(java.util.Locale.ROOT).replace("_", "").replace(" ", "")
                when (clean) {
                    "question", "questiontext", "q" -> qCol = index
                    "optiona", "a" -> aCol = index
                    "optionb", "b" -> bCol = index
                    "optionc", "c" -> cCol = index
                    "optiond", "d" -> dCol = index
                    "correctoption", "correctanswer", "correct", "answer", "ans" -> ansCol = index
                    "explanation", "exp", "solution" -> expCol = index
                    "topic", "subject" -> topicCol = index
                }
            }
        } else {
            dataRows = rows
            startingRowNumber = 1
        }

        val validList = mutableListOf<Question>()
        val errorList = mutableListOf<RowError>()

        dataRows.forEachIndexed { index, cols ->
            val rowNum = startingRowNumber + index
            val snippet = cols.joinToString(" | ").take(60)

            if (cols.all { it.isBlank() }) {
                return@forEachIndexed
            }

            val questionText = cols.getOrNull(qCol)?.trim().orEmpty()
            val optA = cols.getOrNull(aCol)?.trim().orEmpty()
            val optB = cols.getOrNull(bCol)?.trim().orEmpty()
            val optC = cols.getOrNull(cCol)?.trim().orEmpty()
            val optD = cols.getOrNull(dCol)?.trim().orEmpty()
            val rawAns = cols.getOrNull(ansCol)?.trim().orEmpty()
            val explanation = cols.getOrNull(expCol)?.trim().orEmpty()
            val topic = cols.getOrNull(topicCol)?.trim().orEmpty()

            // Validations
            if (questionText.isBlank()) {
                errorList.add(RowError(rowNum, snippet, "Question text is empty"))
                return@forEachIndexed
            }

            if (optA.isBlank() || optB.isBlank() || optC.isBlank() || optD.isBlank()) {
                val missingOpts = mutableListOf<String>()
                if (optA.isBlank()) missingOpts.add("A")
                if (optB.isBlank()) missingOpts.add("B")
                if (optC.isBlank()) missingOpts.add("C")
                if (optD.isBlank()) missingOpts.add("D")
                errorList.add(
                    RowError(
                        rowNum,
                        snippet,
                        "Missing option(s): ${missingOpts.joinToString(", ")}"
                    )
                )
                return@forEachIndexed
            }

            val normalizedAns = normalizeCorrectOption(rawAns)
            if (normalizedAns == null) {
                errorList.add(
                    RowError(
                        rowNum,
                        snippet,
                        "Invalid CorrectOption '$rawAns'. Must be A, B, C, or D (or 1, 2, 3, 4)."
                    )
                )
                return@forEachIndexed
            }

            if (validList.size == MAX_BATCH_QUESTIONS) throw ImportFailure(Failure.BATCH)
            validList.add(
                Question(
                    id = "",
                    examId = examId,
                    questionText = questionText,
                    optionA = optA,
                    optionB = optB,
                    optionC = optC,
                    optionD = optD,
                    correctAnswer = normalizedAns,
                    explanation = explanation,
                    topic = topic
                )
            )
        }

        return ImportResult(
            validQuestions = validList,
            errors = errorList,
            totalRowsProcessed = dataRows.size
        )
    }

    private fun isHeaderRow(row: List<String>): Boolean {
        val firstCell = row.firstOrNull()?.trim()?.lowercase(java.util.Locale.ROOT)?.replace(" ", "")?.replace("_", "").orEmpty()
        return firstCell == "question" ||
                firstCell == "questiontext" ||
                firstCell == "q" ||
                row.any { it.trim().equals("OptionA", ignoreCase = true) || it.trim().equals("Option A", ignoreCase = true) } ||
                row.any { it.trim().equals("CorrectOption", ignoreCase = true) || it.trim().equals("Correct Option", ignoreCase = true) }
    }

    private fun normalizeCorrectOption(raw: String): String? {
        val upper = raw.trim().uppercase(java.util.Locale.ROOT)
        return when (upper) {
            "A", "1", "OPTION A", "OPTIONA" -> "A"
            "B", "2", "OPTION B", "OPTIONB" -> "B"
            "C", "3", "OPTION C", "OPTIONC" -> "C"
            "D", "4", "OPTION D", "OPTIOND" -> "D"
            else -> null
        }
    }
}
