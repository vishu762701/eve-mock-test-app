package com.eve.app.util

import android.app.Activity
import android.os.SystemClock
import android.os.Trace
import android.util.Log
import java.util.concurrent.ConcurrentLinkedDeque

/**
 * Low-overhead performance tracing for launch-to-first-frame and launch-to-interactive timings.
 * Supports COLD, WARM, and RETURN navigation transitions.
 * Emits Android system Trace sections and logs to Logcat.
 */
object EvePerformanceTracer {

    private const val TAG = "EvePerformance"
    private const val MAX_HISTORY = 50

    enum class LaunchType {
        COLD,
        WARM,
        RETURN
    }

    data class LaunchTiming(
        val activityName: String,
        val launchType: LaunchType,
        val launchToFirstFrameMs: Long,
        val launchToInteractiveMs: Long,
        val timestamp: Long
    )

    private data class ActiveLaunch(
        val activityName: String,
        val launchType: LaunchType,
        val startTimeMs: Long,
        var firstFrameMs: Long = -1L
    )

    @Volatile
    private var processLaunched = false

    private val activeLaunches = mutableMapOf<String, ActiveLaunch>()
    private val timingHistory = ConcurrentLinkedDeque<LaunchTiming>()

    @Synchronized
    fun recordLaunchStart(activity: Activity, isRestoring: Boolean): LaunchType {
        val name = activity.javaClass.simpleName
        val type = if (!processLaunched) {
            processLaunched = true
            LaunchType.COLD
        } else if (isRestoring) {
            LaunchType.WARM
        } else {
            LaunchType.WARM
        }

        val start = SystemClock.elapsedRealtime()
        activeLaunches[name] = ActiveLaunch(name, type, start)

        try {
            Trace.beginSection("EveLaunch:$name:${type.name}")
        } catch (_: Throwable) {}

        return type
    }

    @Synchronized
    fun recordReturnStart(activity: Activity): LaunchType {
        val name = activity.javaClass.simpleName
        val type = LaunchType.RETURN
        val start = SystemClock.elapsedRealtime()
        activeLaunches[name] = ActiveLaunch(name, type, start)

        try {
            Trace.beginSection("EveReturn:$name")
        } catch (_: Throwable) {}

        return type
    }

    @Synchronized
    fun recordFirstFrame(activity: Activity) {
        val name = activity.javaClass.simpleName
        val active = activeLaunches[name] ?: return
        val now = SystemClock.elapsedRealtime()
        val duration = (now - active.startTimeMs).coerceAtLeast(0L)
        active.firstFrameMs = duration

        try {
            Trace.beginSection("EveFirstFrame:$name:${duration}ms")
            Trace.endSection()
        } catch (_: Throwable) {}

        Log.i(TAG, "[$name] ${active.launchType} first-frame: ${duration}ms")
    }

    @Synchronized
    fun recordInteractive(activity: Activity) {
        val name = activity.javaClass.simpleName
        val active = activeLaunches.remove(name) ?: return
        val now = SystemClock.elapsedRealtime()
        val interactiveDuration = (now - active.startTimeMs).coerceAtLeast(0L)
        val firstFrameDuration = if (active.firstFrameMs >= 0L) active.firstFrameMs else interactiveDuration

        val record = LaunchTiming(
            activityName = name,
            launchType = active.launchType,
            launchToFirstFrameMs = firstFrameDuration,
            launchToInteractiveMs = interactiveDuration,
            timestamp = System.currentTimeMillis()
        )

        timingHistory.addLast(record)
        while (timingHistory.size > MAX_HISTORY) {
            timingHistory.pollFirst()
        }

        try {
            Trace.endSection() // Ends EveLaunch or EveReturn
        } catch (_: Throwable) {}

        Log.i(TAG, "[$name] ${active.launchType} interactive: ${interactiveDuration}ms (first-frame: ${firstFrameDuration}ms)")
    }

    fun getLatestTiming(): LaunchTiming? = timingHistory.peekLast()

    fun getTimingsFor(activityName: String): List<LaunchTiming> =
        timingHistory.filter { it.activityName == activityName }

    fun getAllTimings(): List<LaunchTiming> = timingHistory.toList()

    @Synchronized
    fun clearForTests() {
        timingHistory.clear()
        activeLaunches.clear()
    }

    @Synchronized
    fun resetProcessStateForTests() {
        clearForTests()
        processLaunched = false
    }
}
