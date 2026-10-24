package com.eve.app.ui.test

import com.eve.app.util.DebugCrashReporter
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class TestStateSurvivalTest {

    private fun findProjectFile(relativePath: String): File {
        val candidates = listOf(
            File(relativePath),
            File("app/$relativePath"),
            File("../app/$relativePath")
        )
        return candidates.firstOrNull { it.exists() }
            ?: throw IllegalStateException("Cannot find file: $relativePath")
    }

    @Test
    fun testDebugCrashReporterExists() {
        assertNotNull(DebugCrashReporter)
        val file = findProjectFile("src/main/java/com/eve/app/util/DebugCrashReporter.kt")
        assertTrue("DebugCrashReporter.kt must exist", file.exists())
    }

    @Test
    fun testTestViewModelHasStatePreservationMethods() {
        val vmFile = findProjectFile("src/main/java/com/eve/app/ui/test/TestViewModel.kt")
        val content = vmFile.readText()
        assertTrue("TestViewModel must contain writeToBundle", content.contains("fun writeToBundle("))
        assertTrue("TestViewModel must contain restoreFromBundle", content.contains("fun restoreFromBundle("))
    }

    @Test
    fun testTestActivitySavesAndRestoresBundle() {
        val actFile = findProjectFile("src/main/java/com/eve/app/ui/test/TestActivity.kt")
        val content = actFile.readText()
        assertTrue("TestActivity must call viewModel.writeToBundle", content.contains("viewModel.writeToBundle(outState"))
        assertTrue("TestActivity must call viewModel.restoreFromBundle", content.contains("viewModel.restoreFromBundle(savedInstanceState,"))
    }
}
