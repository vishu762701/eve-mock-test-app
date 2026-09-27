package com.eve.app.util

import com.eve.app.data.model.Question
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.util.zip.ZipInputStream

object QuestionImportHelper {

    data class RowError(
        val rowNumber: Int,
        val snippet: String,
        val reason: String
    )

    data class ImportResult(
        val validQuestions: List<Question>,
        val errors: List<RowError>,
        val totalRowsProcessed: Int
    )

    /**
     * Parses an input stream of CSV or Excel (.xlsx) file and validates each row.
     */
    fun parseQuestions(
        inputStream: InputStream,
        fileName: String,
        examId: String = ""
    ): ImportResult {
        return if (fileName.endsWith(".xlsx", ignoreCase = true)) {
            parseXlsx(inputStream, examId)
        } else {
            parseCsv(inputStream, examId)
        }
    }

    /**
     * Parses RFC 4180 compliant CSV stream.
     */
    fun parseCsv(inputStream: InputStream, examId: String = ""): ImportResult {
        val reader = BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8))
        val rows = parseCsvRows(reader)
        return processParsedRows(rows, examId)
    }

    /**
     * Parses XLSX using lightweight ZipInputStream + XmlPullParser (zero external heavy dependencies).
     */
    fun parseXlsx(inputStream: InputStream, examId: String = ""): ImportResult {
        val sharedStrings = mutableListOf<String>()
        val sheetRows = mutableListOf<List<String>>()

        try {
            val zis = ZipInputStream(inputStream)
            var entry = zis.nextEntry
            var sheetBytes: ByteArray? = null

            while (entry != null) {
                if (entry.name == "xl/sharedStrings.xml") {
                    parseSharedStrings(zis, sharedStrings)
                } else if (entry.name == "xl/worksheets/sheet1.xml") {
                    sheetBytes = zis.readBytes()
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }

            if (sheetBytes != null) {
                val sheetStream = sheetBytes.inputStream()
                parseSheetXml(sheetStream, sharedStrings, sheetRows)
            }
        } catch (e: Exception) {
            return ImportResult(
                validQuestions = emptyList(),
                errors = listOf(RowError(1, "", "Failed to read Excel (.xlsx) format: ${e.message}")),
                totalRowsProcessed = 0
            )
        }

        return processParsedRows(sheetRows, examId)
    }

    private fun parseSharedStrings(stream: InputStream, outStrings: MutableList<String>) {
        val factory = XmlPullParserFactory.newInstance()
        factory.isNamespaceAware = false
        val parser = factory.newPullParser()
        parser.setInput(stream, "UTF-8")

        var eventType = parser.eventType
        var currentText = StringBuilder()
        var insideT = false

        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    if (parser.name.equals("t", ignoreCase = true)) {
                        insideT = true
                        currentText.clear()
                    }
                }
                XmlPullParser.TEXT -> {
                    if (insideT) {
                        currentText.append(parser.text)
                    }
                }
                XmlPullParser.END_TAG -> {
                    if (parser.name.equals("t", ignoreCase = true)) {
                        insideT = false
                        outStrings.add(currentText.toString())
                    }
                }
            }
            eventType = parser.next()
        }
    }

    private fun parseSheetXml(
        stream: InputStream,
        sharedStrings: List<String>,
        outRows: MutableList<List<String>>
    ) {
        val factory = XmlPullParserFactory.newInstance()
        factory.isNamespaceAware = false
        val parser = factory.newPullParser()
        parser.setInput(stream, "UTF-8")

        var eventType = parser.eventType
        var currentRow = mutableListOf<String>()
        var cellType = ""
        var cellValue = StringBuilder()
        var insideV = false

        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    when (parser.name.lowercase()) {
                        "row" -> {
                            currentRow = mutableListOf()
                        }
                        "c" -> {
                            cellType = parser.getAttributeValue(null, "t").orEmpty()
                            cellValue.clear()
                        }
                        "v" -> {
                            insideV = true
                        }
                    }
                }
                XmlPullParser.TEXT -> {
                    if (insideV) {
                        cellValue.append(parser.text)
                    }
                }
                XmlPullParser.END_TAG -> {
                    when (parser.name.lowercase()) {
                        "v" -> {
                            insideV = false
                        }
                        "c" -> {
                            val raw = cellValue.toString().trim()
                            val resolved = if (cellType == "s") {
                                val idx = raw.toIntOrNull()
                                if (idx != null && idx in sharedStrings.indices) {
                                    sharedStrings[idx]
                                } else {
                                    raw
                                }
                            } else {
                                raw
                            }
                            currentRow.add(resolved)
                        }
                        "row" -> {
                            if (currentRow.any { it.isNotBlank() }) {
                                outRows.add(currentRow)
                            }
                        }
                    }
                }
            }
            eventType = parser.next()
        }
    }

    private fun parseCsvRows(reader: BufferedReader): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        var inQuotes = false
        val currentCell = StringBuilder()
        val currentRow = mutableListOf<String>()

        var line: String? = reader.readLine()
        while (line != null) {
            val chars = line.toCharArray()
            var i = 0
            while (i < chars.size) {
                val c = chars[i]
                if (c == '"') {
                    if (inQuotes && i + 1 < chars.size && chars[i + 1] == '"') {
                        currentCell.append('"')
                        i++ // Skip escaped quote
                    } else {
                        inQuotes = !inQuotes
                    }
                } else if (c == ',' && !inQuotes) {
                    currentRow.add(currentCell.toString().trim())
                    currentCell.clear()
                } else {
                    currentCell.append(c)
                }
                i++
            }

            if (!inQuotes) {
                currentRow.add(currentCell.toString().trim())
                currentCell.clear()
                if (currentRow.any { it.isNotBlank() }) {
                    rows.add(currentRow.toList())
                }
                currentRow.clear()
            } else {
                currentCell.append("\n")
            }

            line = reader.readLine()
        }

        if (currentCell.isNotEmpty() || currentRow.isNotEmpty()) {
            currentRow.add(currentCell.toString().trim())
            if (currentRow.any { it.isNotBlank() }) {
                rows.add(currentRow.toList())
            }
        }

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
                val clean = title.trim().lowercase().replace("_", "").replace(" ", "")
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
        val firstCell = row.firstOrNull()?.trim()?.lowercase()?.replace(" ", "")?.replace("_", "").orEmpty()
        return firstCell == "question" ||
                firstCell == "questiontext" ||
                firstCell == "q" ||
                row.any { it.trim().equals("OptionA", ignoreCase = true) || it.trim().equals("Option A", ignoreCase = true) } ||
                row.any { it.trim().equals("CorrectOption", ignoreCase = true) || it.trim().equals("Correct Option", ignoreCase = true) }
    }

    private fun normalizeCorrectOption(raw: String): String? {
        val upper = raw.trim().uppercase()
        return when (upper) {
            "A", "1", "OPTION A", "OPTIONA" -> "A"
            "B", "2", "OPTION B", "OPTIONB" -> "B"
            "C", "3", "OPTION C", "OPTIONC" -> "C"
            "D", "4", "OPTION D", "OPTIOND" -> "D"
            else -> null
        }
    }
}
