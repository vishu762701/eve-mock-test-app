package com.eve.app.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class DebugCrashReporterPrivacyTest {
    @Test
    fun crashTracesArePrivateByDefaultAndCopyIsExplicit() {
        val sourceFile = File("src/main/java/com/eve/app/util/DebugCrashReporter.kt")
            .takeIf { it.isFile }
            ?: File("app/src/main/java/com/eve/app/util/DebugCrashReporter.kt")
        assertTrue("Reporter source must exist", sourceFile.isFile)

        val code = sourceFile.readText()
        assertTrue("Crash trace must use private app filesDir",
            code.contains("File(context.filesDir, CRASH_FILE_NAME)"))
        assertTrue("User-initiated copy must be retained",
            code.contains("ClipboardManager") && code.contains("Copy Trace"))
        assertFalse("Do not auto-export exception traces to Downloads",
            code.contains("MediaStore.Downloads") || code.contains("DIRECTORY_DOWNLOADS"))
        assertFalse("Do not auto-export exception traces to shared external files",
            code.contains("getExternalFilesDir") || code.contains("/sdcard/Download"))
        assertFalse("Do not log the entire exception payload automatically",
            code.contains("FATAL CRASH DETECTED:\\n\$crashPayload"))
        assertTrue("Debug trace viewer must use source-isolated native dialog",
            code.contains("TelegramNativeAlertDialog.Builder(activity)"))
        assertTrue("Only explicit Copy Trace button may write to clipboard",
            code.contains("dialog.getActionButton(DialogInterface.BUTTON_POSITIVE)?.setOnClickListener") &&
                code.contains("clipboard?.setPrimaryClip(clip)"))
        assertTrue("Crash dialog must preserve dismiss callback",
            code.contains(".setOnDismissListener {") && code.contains("onDismiss()"))
    }
}
