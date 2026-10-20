package com.eve.app.util

import android.app.Activity
import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
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
 * Saves crash stack traces only in private filesDir on unhandled exceptions and displays a copyable
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

                Log.e("CRITICAL_STARTUP", "Debug crash trace saved privately (${throwable.javaClass.simpleName})")

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

        // Never write exception traces into shared storage automatically.
        // The debug-only crash dialog provides an explicit Copy Trace action.

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
            com.eve.app.designsystem.DesignType.apply(this, com.eve.app.designsystem.DesignType.Role.SECONDARY)
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
