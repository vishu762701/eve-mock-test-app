package com.eve.app.util

import android.app.Activity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito

class EvePerformanceTracerTest {

    private class MockActivity : Activity()
    private class SecondActivity : Activity()

    @Before
    fun setUp() {
        EvePerformanceTracer.resetProcessStateForTests()
    }

    @Test
    fun testColdWarmAndReturnLaunchClassification() {
        val activity1 = MockActivity()
        val coldType = EvePerformanceTracer.recordLaunchStart(activity1, isRestoring = false)
        assertEquals(EvePerformanceTracer.LaunchType.COLD, coldType)

        EvePerformanceTracer.recordFirstFrame(activity1)
        EvePerformanceTracer.recordInteractive(activity1)

        val timingCold = EvePerformanceTracer.getLatestTiming()
        assertNotNull(timingCold)
        assertEquals("MockActivity", timingCold?.activityName)
        assertEquals(EvePerformanceTracer.LaunchType.COLD, timingCold?.launchType)
        assertTrue(timingCold!!.launchToFirstFrameMs >= 0)
        assertTrue(timingCold.launchToInteractiveMs >= timingCold.launchToFirstFrameMs)

        // Second activity launched in same process -> WARM
        val activity2 = SecondActivity()
        val warmType = EvePerformanceTracer.recordLaunchStart(activity2, isRestoring = false)
        assertEquals(EvePerformanceTracer.LaunchType.WARM, warmType)

        EvePerformanceTracer.recordFirstFrame(activity2)
        EvePerformanceTracer.recordInteractive(activity2)

        val timingWarm = EvePerformanceTracer.getLatestTiming()
        assertNotNull(timingWarm)
        assertEquals("SecondActivity", timingWarm?.activityName)
        assertEquals(EvePerformanceTracer.LaunchType.WARM, timingWarm?.launchType)

        // Return navigation back to MockActivity -> RETURN
        val returnType = EvePerformanceTracer.recordReturnStart(activity1)
        assertEquals(EvePerformanceTracer.LaunchType.RETURN, returnType)

        EvePerformanceTracer.recordFirstFrame(activity1)
        EvePerformanceTracer.recordInteractive(activity1)

        val timingReturn = EvePerformanceTracer.getLatestTiming()
        assertNotNull(timingReturn)
        assertEquals("MockActivity", timingReturn?.activityName)
        assertEquals(EvePerformanceTracer.LaunchType.RETURN, timingReturn?.launchType)
    }

    @Test
    fun testHistoryRetrievalAndFiltering() {
        val act = MockActivity()
        EvePerformanceTracer.recordLaunchStart(act, isRestoring = false)
        EvePerformanceTracer.recordFirstFrame(act)
        EvePerformanceTracer.recordInteractive(act)

        val act2 = SecondActivity()
        EvePerformanceTracer.recordLaunchStart(act2, isRestoring = true)
        EvePerformanceTracer.recordFirstFrame(act2)
        EvePerformanceTracer.recordInteractive(act2)

        val all = EvePerformanceTracer.getAllTimings()
        assertEquals(2, all.size)

        val mockList = EvePerformanceTracer.getTimingsFor("MockActivity")
        assertEquals(1, mockList.size)
        assertEquals("MockActivity", mockList[0].activityName)

        val secondList = EvePerformanceTracer.getTimingsFor("SecondActivity")
        assertEquals(1, secondList.size)
        assertEquals("SecondActivity", secondList[0].activityName)
    }
}
