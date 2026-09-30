package com.eve.app.util

import android.app.Activity
import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentValues
import android.content.Context
import android.content.DialogInterface
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import com.eve.app.BuildConfig
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter

/**
 * Debug-only uncaught exception reporter.
 * Saves crash stack traces to filesDir on unhandled exceptions and displays a copyable
 * dialog on the subsequent app launch.
 *
 * NOTE: Native crashes (e.g. SIGSEGV / Fatal signal 11) terminate the process at the OS/Bionic
 * level and are not caught by JVM uncaught exception handlers.
 */
object DebugCrashReporter {

    private const val CRASH_FILE_NAME = "debug_last_crash_trace.txt"
    private var hasCheckedCrash = false

    fun init(application: Application) {
        if (!BuildConfig.DEBUG) return

        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                val sw = StringWriter()
                val pw = PrintWriter(sw)
                throwable.printStackTrace(pw)
                pw.flush()
                val stackTrace = sw.toString()
                val crashPayload = "Time: ${System.currentTimeMillis()}\nThread: ${thread.name}\n\n$stackTrace"

                Log.e("CRITICAL_STARTUP", "FATAL CRASH DETECTED:\n$crashPayload")

                saveCrashPayload(application, crashPayload)
            } catch (t: Throwable) {
                t.printStackTrace()
            } finally {
                defaultHandler?.uncaughtException(thread, throwable)
            }
        }

        application.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
            override fun onActivityStarted(activity: Activity) {
                if (!hasCheckedCrash) {
                    checkAndShowDialog(activity)
                }
            }

            override fun onActivityResumed(activity: Activity) {
                if (!hasCheckedCrash) {
                    checkAndShowDialog(activity)
                }
            }

            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
            override fun onActivityPaused(activity: Activity) {}
            override fun onActivityStopped(activity: Activity) {}
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
            override fun onActivityDestroyed(activity: Activity) {}
        })
    }

    private fun saveCrashPayload(context: Context, crashPayload: String) {
        // 1. Private internal storage (always works)
        try {
            val crashFile = File(context.filesDir, CRASH_FILE_NAME)
            crashFile.writeText(crashPayload)
        } catch (_: Throwable) {}

        // 2. App-specific external files (accessible to Termux if storage granted, no permissions required)
        try {
            val extDir = context.getExternalFilesDir(null)
            if (extDir != null) {
                File(extDir, CRASH_FILE_NAME).writeText(crashPayload)
            }
        } catch (_: Throwable) {}

        // 3. Public Download storage via MediaStore (API 29+) or direct file write (API <= 28)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = context.contentResolver
                val selection = "${MediaStore.MediaColumns.DISPLAY_NAME} = ?"
                val selectionArgs = arrayOf(CRASH_FILE_NAME)
                try {
                    resolver.delete(
                        MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                        selection,
                        selectionArgs
                    )
                } catch (_: Throwable) {}

                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, CRASH_FILE_NAME)
                    put(MediaStore.MediaColumns.MIME_TYPE, "text/plain")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                uri?.let { destUri ->
                    resolver.openOutputStream(destUri)?.use { os ->
                        os.write(crashPayload.toByteArray())
                    }
                }
            } else {
                val downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (downloadDir != null && downloadDir.exists()) {
                    File(downloadDir, CRASH_FILE_NAME).writeText(crashPayload)
                }
            }
        } catch (_: Throwable) {}

        // 4. Legacy fallback
        try {
            File("/sdcard/Download", CRASH_FILE_NAME).writeText(crashPayload)
        } catch (_: Throwable) {}
    }

    fun hasCrash(context: Context): Boolean {
        if (!BuildConfig.DEBUG) return false
        val crashFile = File(context.filesDir, CRASH_FILE_NAME)
        return crashFile.exists() && crashFile.length() > 0
    }

    fun getCrashTrace(context: Context): String? {
        if (!BuildConfig.DEBUG) return null
        val crashFile = File(context.filesDir, CRASH_FILE_NAME)
        if (!crashFile.exists()) return null
        return try {
            crashFile.readText()
        } catch (_: Throwable) {
            null
        }
    }

    fun clearCrash(context: Context) {
        try {
            File(context.filesDir, CRASH_FILE_NAME).delete()
        } catch (_: Throwable) {}
    }

    fun showCrashDialog(activity: Activity, onDismiss: () -> Unit = {}) {
        if (!BuildConfig.DEBUG || activity.isFinishing || activity.isDestroyed) {
            onDismiss()
            return
        }

        val crashContent = getCrashTrace(activity)
        if (crashContent.isNullOrBlank()) {
            onDismiss()
            return
        }

        clearCrash(activity)
        hasCheckedCrash = true

        val textView = TextView(activity).apply {
            text = crashContent
            setPadding(40, 20, 40, 20)
            setTextIsSelectable(true)
            textSize = 12f
        }
        val scrollView = ScrollView(activity).apply {
            addView(textView)
        }

        val dialog = MaterialAlertDialogBuilder(activity)
            .setTitle("Crash Detected on Previous Launch")
            .setView(scrollView)
            .setPositiveButton("Copy Trace", null)
            .setNegativeButton("Dismiss") { d, _ ->
                d.dismiss()
            }
            .setOnDismissListener {
                onDismiss()
            }
            .create()

        dialog.setOnShowListener {
            dialog.getButton(DialogInterface.BUTTON_POSITIVE)?.setOnClickListener {
                val clipboard = activity.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                val clip = ClipData.newPlainText("Crash Trace", crashContent)
                clipboard?.setPrimaryClip(clip)
                Toast.makeText(activity, "Crash trace copied to clipboard", Toast.LENGTH_SHORT).show()
            }
        }

        dialog.show()
    }

    private fun checkAndShowDialog(activity: Activity) {
        if (hasCrash(activity)) {
            showCrashDialog(activity)
        }
    }
}
