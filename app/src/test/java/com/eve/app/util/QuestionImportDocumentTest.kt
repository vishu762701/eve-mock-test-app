package com.eve.app.util

import android.app.Application
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Real existing parser and real ZIP/XML APIs, with synthetic document content only. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk=[35],application=Application::class)
class QuestionImportDocumentTest {
    private fun xlsx(sheet:String,shared:String=""):ByteArray {
        val output=ByteArrayOutputStream()
        ZipOutputStream(output).use { zip ->
            for((name,xml) in listOf("xl/sharedStrings.xml" to "<sst xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">$shared</sst>",
                "xl/worksheets/sheet1.xml" to "<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\"><sheetData>$sheet</sheetData></worksheet>")) {
                zip.putNextEntry(ZipEntry(name)); zip.write(xml.toByteArray()); zip.closeEntry()
            }
        }
        return output.toByteArray()
    }
    private fun shared(values:List<String>)=values.joinToString("") {"<si><t>$it</t></si>"}
    private fun cell(ref:String,value:Int)="<c r=\"$ref\" t=\"s\"><v>$value</v></c>"
    @Test fun sparseWorksheetCellCannotBecomeThePreviousColumnsValue() {
        val headers=listOf("Question","OptionA","OptionB","OptionC","OptionD","CorrectOption","Explanation","Topic")
        val row1="<row r=\"1\">"+headers.indices.joinToString("") {cell("${'A'+it}1",it)}+"</row>"
        val row2="<row r=\"2\">"+(0..5).joinToString("") {cell("${'A'+it}2",8+it)}+cell("H2",14)+"</row>"
        val result=QuestionImportHelper.parseQuestions(xlsx(row1+row2,shared(headers+listOf("Question text","A","B","C","D","A","Math"))).inputStream(),"questions.xlsx","exam")
        assertEquals(1,result.validQuestions.size)
        assertEquals("",result.validQuestions.single().explanation)
        assertEquals("Math",result.validQuestions.single().topic)
    }
    @Test fun inlineStringsPreserveQuestionAndCorrectAnswer() {
        val values=listOf("Inline question","A","B","C","D","B")
        val row="<row>"+values.mapIndexed {i,v -> "<c r=\"${'A'+i}1\" t=\"inlineStr\"><is><t>$v</t></is></c>"}.joinToString("")+"</row>"
        val result=QuestionImportHelper.parseXlsx(xlsx(row).inputStream(),"exam")
        assertEquals(1,result.validQuestions.size)
        assertEquals("Inline question",result.validQuestions.single().questionText)
        assertEquals("B",result.validQuestions.single().correctAnswer)
    }
    @Test fun richSharedRunsAreOneStringRatherThanShiftingEveryIndex() {
        val rich="<si><r><t>Rich </t></r><r><t>question</t></r></si>"+shared(listOf("A","B","C","D","B"))
        val row="<row>"+(0..5).joinToString("") {cell("${'A'+it}1",it)}+"</row>"
        val result=QuestionImportHelper.parseXlsx(xlsx(row,rich).inputStream(),"exam")
        assertEquals(1,result.validQuestions.size)
        assertEquals("Rich question",result.validQuestions.single().questionText)
        assertEquals("A",result.validQuestions.single().optionA)
        assertEquals("B",result.validQuestions.single().correctAnswer)
    }
    @Test fun oversizedValidBatchCannotBeOfferedOrSilentlyTruncated() {
        val csv="Question,OptionA,OptionB,OptionC,OptionD,CorrectOption\n"+(1..301).joinToString("\n") {"Q$it,A,B,C,D,A"}
        val result=QuestionImportHelper.parseQuestions(csv.byteInputStream(),"questions.csv","exam")
        assertTrue("Worker rejects >300; no partial batch may be offered",result.validQuestions.isEmpty())
        assertTrue(result.errors.isNotEmpty())
    }
    @Test fun cancelledZipReadPropagatesInsteadOfBecomingAFormatError() {
        val stream=object:java.io.InputStream() {override fun read():Int=throw kotlinx.coroutines.CancellationException("Stopped")}
        val error=runCatching {QuestionImportHelper.parseXlsx(stream,"exam")}.exceptionOrNull()
        assertTrue(error is kotlinx.coroutines.CancellationException)
    }
    private fun document(entries:List<Pair<String,ByteArray>>):ByteArray {
        val output=ByteArrayOutputStream()
        ZipOutputStream(output).use { zip -> for((name,bytes) in entries) { zip.putNextEntry(ZipEntry(name)); zip.write(bytes); zip.closeEntry() } }
        return output.toByteArray()
    }
    @Test fun rawBudgetStopsConsumptionAndRetainsCallerStreamOwnership() {
        var read=0; var closes=0
        val source=object:java.io.InputStream() {
            override fun read():Int {read++; return ' '.code}
            override fun read(b:ByteArray,off:Int,len:Int):Int { java.util.Arrays.fill(b,off,off+len,' '.code.toByte()); read+=len; return len }
            override fun close() {closes++}
        }
        val result=QuestionImportHelper.parseCsv(source,"exam")
        assertEquals(QuestionImportHelper.Failure.INPUT,result.failure)
        assertTrue(result.validQuestions.isEmpty()); assertEquals(QuestionImportHelper.MAX_INPUT_BYTES+1,read)
        assertEquals(0,closes); source.close(); assertEquals(1,closes)
    }
    @Test fun unusedZipEntryDrainingIsIncludedInExpandedBudget() {
        val output=ByteArrayOutputStream()
        ZipOutputStream(output).use { zip ->
            zip.putNextEntry(ZipEntry("ignored-large.bin"))
            val block=ByteArray(8192)
            repeat(QuestionImportHelper.MAX_EXPANDED_BYTES/block.size+1) {zip.write(block)}
            zip.closeEntry()
        }
        val result=QuestionImportHelper.parseXlsx(output.toByteArray().inputStream(),"exam")
        assertEquals(QuestionImportHelper.Failure.EXPANDED,result.failure)
        assertTrue(result.validQuestions.isEmpty())
    }
    @Test fun cellsColumnsAndRowsCannotGrowWithoutABound() {
        val cases=listOf(
            "x".repeat(QuestionImportHelper.MAX_CELL_CHARS+1) to QuestionImportHelper.Failure.CELL,
            List(QuestionImportHelper.MAX_COLUMNS+1){"x"}.joinToString(",") to QuestionImportHelper.Failure.COLUMNS,
            List(QuestionImportHelper.MAX_ROWS+1){"invalid"}.joinToString("\n") to QuestionImportHelper.Failure.ROWS)
        for((csv,failure) in cases) {
            val result=QuestionImportHelper.parseCsv(csv.byteInputStream(),"exam")
            assertEquals(failure,result.failure); assertTrue(result.validQuestions.isEmpty())
        }
        val wide="<row><c r=\"BM1\" t=\"inlineStr\"><is><t>Q</t></is></c></row>"
        assertEquals(QuestionImportHelper.Failure.COLUMNS,QuestionImportHelper.parseXlsx(xlsx(wide).inputStream()).failure)
    }
    @Test fun sharedStringTableCannotGrowWithoutABound() {
        val strings="<si><t>x</t></si>".repeat(QuestionImportHelper.MAX_SHARED_STRINGS+1)
        assertEquals(QuestionImportHelper.Failure.SHARED,QuestionImportHelper.parseXlsx(xlsx("",strings).inputStream()).failure)
    }
    @Test fun dtdCannotSupplyQuestionContentFromAnotherFile() {
        val secret=java.io.File.createTempFile("import-entity-fixture",".txt")
        try {
            secret.writeText("SYNTHETIC_ENTITY_CONTENT")
            val xml="""<?xml version="1.0"?><!DOCTYPE worksheet [<!ENTITY external SYSTEM "${secret.toURI()}">]><worksheet><sheetData><row><c t="inlineStr"><is><t>&external;</t></is></c></row></sheetData></worksheet>"""
            val result=QuestionImportHelper.parseXlsx(document(listOf("xl/worksheets/sheet1.xml" to xml.toByteArray())).inputStream())
            assertEquals(QuestionImportHelper.Failure.MALFORMED,result.failure)
            assertTrue(result.validQuestions.isEmpty()); assertFalse(result.errors.toString().contains("SYNTHETIC_ENTITY_CONTENT"))
        } finally { secret.delete() }
    }
    @Test fun duplicateCellReferencesAreRejectedRatherThanOverwritingAnswers() {
        val row="<row>"+cell("A1",0)+cell("A1",1)+"</row>"
        assertEquals(QuestionImportHelper.Failure.MALFORMED,QuestionImportHelper.parseXlsx(xlsx(row,shared(listOf("First","Second"))).inputStream()).failure)
    }
    @Test fun multilineEscapedCsvStillPreservesContentAndNumericAnswer() {
        val csv="\"Line one,\nline two \"\"quote\"\"\",A,B,C,D,2"
        val result=QuestionImportHelper.parseCsv(csv.byteInputStream(),"exam")
        assertNull(result.failure); assertEquals(1,result.validQuestions.size)
        assertEquals("Line one,\nline two \"quote\"",result.validQuestions.single().questionText)
        assertEquals("B",result.validQuestions.single().correctAnswer)
    }
    @Test fun exactlyThreeHundredQuestionsRemainSupported() {
        val csv=(1..300).joinToString("\n") {"Q$it,A,B,C,D,A"}
        val result=QuestionImportHelper.parseCsv(csv.byteInputStream(),"exam")
        assertNull(result.failure); assertEquals(300,result.validQuestions.size); assertTrue(result.errors.isEmpty())
    }
    @Test fun prefixedInlineXmlKeepsHindiCdataAndPredefinedEntities() {
        val values=listOf("<![CDATA[क्या & कैसे?]]>","A &amp; B","B","C","D","2")
        val cells=values.mapIndexed {i,v->"<x:c r=\"${'A'+i}1\" t=\"inlineStr\"><x:is><x:t>$v</x:t></x:is></x:c>"}.joinToString("")
        val xml="<x:worksheet xmlns:x=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\"><x:sheetData><x:row>$cells</x:row></x:sheetData></x:worksheet>"
        val result=QuestionImportHelper.parseXlsx(document(listOf("xl/worksheets/sheet1.xml" to xml.toByteArray())).inputStream(),"exam-hi")
        assertNull(result.failure)
        assertEquals("क्या & कैसे?",result.validQuestions.single().questionText)
        assertEquals("A & B",result.validQuestions.single().optionA)
        assertEquals("B",result.validQuestions.single().correctAnswer); assertEquals("exam-hi",result.validQuestions.single().examId)
    }
    @Test fun unclosedCsvQuoteCannotOfferAPartiallyParsedFile() {
        val csv="Good,A,B,C,D,A\n\"Unclosed,A,B,C,D,A"
        val result=QuestionImportHelper.parseCsv(csv.byteInputStream(),"exam")
        assertEquals(QuestionImportHelper.Failure.MALFORMED,result.failure); assertTrue(result.validQuestions.isEmpty())
    }
    @Test fun closingZipReleasesDecoderWithoutClosingCallerInput() {
        var closes=0
        val bytes=xlsx("<row/> ")
        val source=object:ByteArrayInputStream(bytes) {override fun close() {closes++; super.close()}}
        QuestionImportHelper.parseXlsx(source,"exam")
        assertEquals(0,closes); source.close(); assertEquals(1,closes)
    }

}
