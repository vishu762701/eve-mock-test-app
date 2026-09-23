package com.eve.app.util

import android.content.ContentResolver
import android.net.Uri
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.BufferedInputStream
import java.io.ByteArrayInputStream
import java.nio.charset.Charset
import java.util.zip.ZipInputStream

/**
 * Phase 21: CSV aur .xlsx ko rows me convert karta hai. Apache POI nahi —
 * Android par POI heavy + javax.xml issues. .xlsx = zip + XML, built-in
 * ZipInputStream + XmlPullParser se padhte hain. Purana .xls support nahi
 * (Excel se "Save as CSV / xlsx" karna padega).
 */
object SpreadsheetReader {

    data class Sheet(
        val headers: List<String>,
        val rows: List<List<String>>
    )

    fun read(resolver: ContentResolver, uri: Uri, displayName: String = ""): Sheet {
        val name = displayName.lowercase()
        val bytes = resolver.openInputStream(uri)?.use { it.readBytes() }
            ?: throw IllegalStateException("Unable to open file")
        return when {
            name.endsWith(".xls") && !name.endsWith(".xlsx") ->
                throw IllegalArgumentException("Legacy .xls format is not supported. Please Save As CSV or .xlsx in Excel.")
            name.endsWith(".xlsx") || isZip(bytes) -> readXlsx(bytes)
            else -> readCsv(bytes)
        }
    }

    private fun isZip(bytes: ByteArray): Boolean =
        bytes.size >= 2 && bytes[0] == 0x50.toByte() && bytes[1] == 0x4B.toByte()

    fun readCsv(bytes: ByteArray): Sheet {
        val text = decodeText(bytes)
        val table = parseCsv(text)
        if (table.isEmpty()) throw IllegalArgumentException("CSV is empty")
        val headers = table.first().map { it.trim() }
        val rows = table.drop(1).filter { row -> row.any { it.isNotBlank() } }
        return Sheet(headers, rows)
    }

    private fun decodeText(bytes: ByteArray): String {
        if (bytes.size >= 3 && bytes[0] == 0xEF.toByte() && bytes[1] == 0xBB.toByte() && bytes[2] == 0xBF.toByte()) {
            return String(bytes, 3, bytes.size - 3, Charsets.UTF_8)
        }
        if (bytes.size >= 2 && bytes[0] == 0xFF.toByte() && bytes[1] == 0xFE.toByte()) {
            return String(bytes, Charset.forName("UTF-16LE"))
        }
        return String(bytes, Charsets.UTF_8)
    }

    fun parseCsv(text: String): List<List<String>> {
        val delimiter = detectDelimiter(text)
        val rows = mutableListOf<List<String>>()
        val row = mutableListOf<String>()
        val cell = StringBuilder()
        var i = 0
        var inQuotes = false
        while (i < text.length) {
            val ch = text[i]
            when {
                ch == '"' -> {
                    if (inQuotes && i + 1 < text.length && text[i + 1] == '"') {
                        cell.append('"')
                        i++
                    } else {
                        inQuotes = !inQuotes
                    }
                }
                ch == delimiter && !inQuotes -> {
                    row.add(cell.toString())
                    cell.clear()
                }
                (ch == '\n' || ch == '\r') && !inQuotes -> {
                    if (ch == '\r' && i + 1 < text.length && text[i + 1] == '\n') i++
                    row.add(cell.toString())
                    cell.clear()
                    if (row.any { it.isNotBlank() }) rows.add(row.toList())
                    row.clear()
                }
                else -> cell.append(ch)
            }
            i++
        }
        if (cell.isNotEmpty() || row.isNotEmpty()) {
            row.add(cell.toString())
            if (row.any { it.isNotBlank() }) rows.add(row)
        }
        return rows
    }

    private fun detectDelimiter(text: String): Char {
        val first = text.lineSequence().firstOrNull { it.isNotBlank() } ?: return ','
        val counts = listOf(',', ';', '\t').associateWith { d ->
            var n = 0
            var q = false
            for (ch in first) {
                if (ch == '"') q = !q
                else if (!q && ch == d) n++
            }
            n
        }
        return counts.maxByOrNull { it.value }?.takeIf { it.value > 0 }?.key ?: ','
    }

    private fun readXlsx(bytes: ByteArray): Sheet {
        val parts = linkedMapOf<String, ByteArray>()
        ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                if (!entry.isDirectory) {
                    parts[entry.name.replace('\\', '/')] = zip.readBytes()
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
        val stringsPath = parts.keys.firstOrNull { it.equals("xl/sharedStrings.xml", true) }
        val shared = stringsPath?.let { parseSharedStrings(parts[it]!!) } ?: emptyList()
        val sheetPath = parts.keys.firstOrNull {
            it.startsWith("xl/worksheets/sheet") && it.endsWith(".xml")
        } ?: throw IllegalArgumentException("No worksheet found in XLSX file")
        val table = parseSheet(parts[sheetPath]!!, shared)
        if (table.isEmpty()) throw IllegalArgumentException("Excel sheet is empty")
        val headers = table.first().map { it.trim() }
        val rows = table.drop(1).filter { row -> row.any { it.isNotBlank() } }
        return Sheet(headers, rows)
    }

    private fun parseSharedStrings(xml: ByteArray): List<String> {
        val parser = pull(xml)
        val out = mutableListOf<String>()
        val buf = StringBuilder()
        var inSi = false
        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> if (parser.name == "si") {
                    inSi = true
                    buf.clear()
                }
                XmlPullParser.TEXT -> if (inSi) buf.append(parser.text)
                XmlPullParser.END_TAG -> if (parser.name == "si") {
                    out.add(buf.toString())
                    inSi = false
                }
            }
            event = parser.next()
        }
        return out
    }

    private fun parseSheet(xml: ByteArray, shared: List<String>): List<List<String>> {
        val parser = pull(xml)
        val rows = mutableListOf<MutableMap<Int, String>>()
        var currentRow: MutableMap<Int, String>? = null
        var col = 0
        var cellType = ""
        var inValue = false
        var inInline = false
        val buf = StringBuilder()
        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> when (parser.name) {
                    "row" -> {
                        currentRow = mutableMapOf()
                        rows.add(currentRow!!)
                    }
                    "c" -> {
                        val ref = parser.getAttributeValue(null, "r") ?: ""
                        col = cellIndex(ref)
                        cellType = parser.getAttributeValue(null, "t") ?: ""
                        buf.clear()
                    }
                    "v" -> inValue = true
                    "t" -> if (cellType == "inlineStr") inInline = true
                    "is" -> { /* inline string wrapper */ }
                }
                XmlPullParser.TEXT -> if (inValue || inInline) buf.append(parser.text)
                XmlPullParser.END_TAG -> when (parser.name) {
                    "v" -> {
                        inValue = false
                        val raw = buf.toString()
                        val text = if (cellType == "s") shared.getOrNull(raw.toIntOrNull() ?: -1) ?: raw else raw
                        currentRow?.put(col, text)
                        buf.clear()
                    }
                    "t" -> if (inInline) {
                        inInline = false
                        currentRow?.put(col, buf.toString())
                        buf.clear()
                    }
                }
            }
            event = parser.next()
        }
        val width = rows.maxOfOrNull { row -> (row.keys.maxOrNull() ?: -1) + 1 } ?: 0
        return rows.map { row -> (0 until width).map { idx -> row[idx] ?: "" } }
            .filter { it.any { cell -> cell.isNotBlank() } }
    }

    private fun cellIndex(ref: String): Int {
        val letters = ref.takeWhile { it.isLetter() }.uppercase()
        if (letters.isEmpty()) return 0
        var n = 0
        for (ch in letters) n = n * 26 + (ch - 'A' + 1)
        return n - 1
    }

    private fun pull(xml: ByteArray): XmlPullParser {
        val factory = XmlPullParserFactory.newInstance()
        factory.isNamespaceAware = false
        return factory.newPullParser().apply {
            setInput(BufferedInputStream(ByteArrayInputStream(xml)), "UTF-8")
        }
    }
}
