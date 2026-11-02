package com.eve.app.ui.admin

import android.app.Application
import android.os.Bundle
import android.os.Looper
import android.view.View
import com.eve.app.R
import com.eve.app.data.remote.AdminMeData
import com.eve.app.ui.profile.ProfileEditorAuthFixture
import org.junit.Assert.*
import org.junit.Before
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.android.controller.ActivityController

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[35],application=Application::class,
 shadows=[ProfileEditorAuthFixture.ShadowAuth::class,EditExamBoundaryFixture.AdminBoundary::class,
 QuestionReviewBoundaryFixture.AnalyticsBoundary::class],instrumentedPackages=["com.eve.app.data.repository"])
class AnalyticsFilterActivityTest {
    @Before fun before() {
        ProfileEditorAuthFixture.reset(); ProfileEditorAuthFixture.setOwner("owner")
        EditExamBoundaryFixture.reset()
        EditExamBoundaryFixture.permissions=AdminMeData(email="owner@example.test",rights=listOf("view_analytics_logs"))
        QuestionReviewBoundaryFixture.exams=emptyList<Any>(); QuestionReviewBoundaryFixture.analytics=emptyList<Any>()
        QuestionReviewBoundaryFixture.analyticsDays.clear()
    }
    @After fun after() { ProfileEditorAuthFixture.reset() }
    private fun idle()=shadowOf(Looper.getMainLooper()).idle()
    private fun open(state:Bundle?=null):ActivityController<AdminAnalyticsActivity> {
        val c=Robolectric.buildActivity(AdminAnalyticsActivity::class.java)
        c.get().setTheme(R.style.Theme_Eve)
        if (state == null) c.setup() else c.setup(state)
        c.visible(); idle(); return c
    }
    private fun close(c:ActivityController<AdminAnalyticsActivity>) { c.pause().stop().destroy(); idle() }
    @Test fun actualRangeCallbacksSupportRepeatedSwitchingAndSelectedNoOp() {
        val c=open()
        try {
            c.get().findViewById<View>(R.id.chipLast7Days).performClick(); idle()
            val count=QuestionReviewBoundaryFixture.analyticsDays.size
            c.get().findViewById<View>(R.id.chipLast7Days).performClick(); idle()
            assertEquals(count,QuestionReviewBoundaryFixture.analyticsDays.size)
            c.get().findViewById<View>(R.id.chipLast30Days).performClick(); idle()
            c.get().findViewById<View>(R.id.chipAllTime).performClick(); idle()
            assertEquals(listOf(0,7,30,0),QuestionReviewBoundaryFixture.analyticsDays)
        } finally { close(c) }
    }
    @Test fun recreatedRangeOwnsFirstReadInsteadOfDefaultAllTime() {
        val old=open()
        val state=Bundle()
        try { old.get().findViewById<View>(R.id.chipLast7Days).performClick(); idle(); old.saveInstanceState(state) }
        finally { close(old) }
        QuestionReviewBoundaryFixture.analyticsDays.clear()
        val c=open(state)
        try {
            assertEquals(listOf(7),QuestionReviewBoundaryFixture.analyticsDays)
            assertEquals(R.id.chipLast7Days,c.get().findViewById<com.eve.app.designsystem.UiArcSegmentedControl>(R.id.chipGroupTimeRange).checkedChipId)
        } finally { close(c) }
    }
}
