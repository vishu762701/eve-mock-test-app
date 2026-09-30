package com.eve.app.util

import android.app.Activity
import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
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

                val crashFile = File(application.filesDir, CRASH_FILE_NAME)
                crashFile.writeText("Time: ${System.currentTimeMillis()}\nThread: ${thread.name}\n\n$stackTrace")
            } catch (t: Throwable) {
                t.printStackTrace()
            } finally {
                defaultHandler?.uncaughtException(thread, throwable)
            }
        }

        application.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
            override fun onActivityResumed(activity: Activity) {
                if (!hasCheckedCrash) {
                    hasCheckedCrash = true
                    checkAndShowDialog(activity)
                }
            }

            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
            override fun onActivityStarted(activity: Activity) {}
            override fun onActivityPaused(activity: Activity) {}
            override fun onActivityStopped(activity: Activity) {}
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
            override fun onActivityDestroyed(activity: Activity) {}
        })
    }

    private fun checkAndShowDialog(activity: Activity) {
        if (!BuildConfig.DEBUG || activity.isFinishing || activity.isDestroyed) return

        val crashFile = File(activity.filesDir, CRASH_FILE_NAME)
        if (!crashFile.exists()) return

        val crashContent = try {
            crashFile.readText()
        } catch (_: Throwable) {
            return
        } finally {
            crashFile.delete()
        }

        if (crashContent.isBlank()) return

        val textView = TextView(activity).apply {
            text = crashContent
            setPadding(40, 20, 40, 20)
            setTextIsSelectable(true)
            textSize = 12f
        }
        val scrollView = ScrollView(activity).apply {
            addView(textView)
        }

        MaterialAlertDialogBuilder(activity)
            .setTitle("Crash Detected on Previous Launch")
            .setView(scrollView)
            .setPositiveButton("Copy Trace") { _, _ ->
                val clipboard = activity.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                val clip = ClipData.newPlainText("Crash Trace", crashContent)
                clipboard?.setPrimaryClip(clip)
                Toast.makeText(activity, "Crash trace copied to clipboard", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Dismiss", null)
            .show()
    }
}
