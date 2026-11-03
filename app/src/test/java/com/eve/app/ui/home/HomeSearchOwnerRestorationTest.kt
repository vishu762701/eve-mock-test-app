package com.eve.app.ui.home

import android.os.Bundle
import android.os.Looper
import android.view.View
import android.widget.EditText
import com.eve.app.EveApplication
import com.eve.app.R
import com.eve.app.ui.profile.ProfileEditorAuthFixture
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/** Actual authenticated MainActivity, click listener and Android hierarchy restoration. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk=[35],application=EveApplication::class,
 shadows=[ProfileEditorAuthFixture.ShadowAuth::class,HomeStartupBoundaryFixture.ApplicationBoundary::class,
 HomeStartupBoundaryFixture.ApiBoundary::class,HomeStartupBoundaryFixture.PushBoundary::class,
 HomeStartupBoundaryFixture.SchedulerBoundary::class],
 instrumentedPackages=["com.eve.app.data.remote","com.eve.app.util","com.eve.app.EveApplication"])
class HomeSearchOwnerRestorationTest {
    @Before fun before(){ProfileEditorAuthFixture.reset();ProfileEditorAuthFixture.setOwner("account-a")}
    @After fun after(){ProfileEditorAuthFixture.reset()}
    private fun idle()=shadowOf(Looper.getMainLooper()).idle()
    private fun open(state:Bundle?=null):org.robolectric.android.controller.ActivityController<MainActivity>{
        val c=Robolectric.buildActivity(MainActivity::class.java);c.get().setTheme(R.style.Theme_Eve)
        if(state==null)c.setup()else c.setup(state)
        c.visible();idle();return c
    }
    private fun close(c:org.robolectric.android.controller.ActivityController<MainActivity>){c.pause().stop().destroy();idle()}
    private fun snapshot(cursor:Boolean=false):Bundle {
        val c=open()
        try {
            c.get().findViewById<View>(R.id.btnSearch).performClick();idle()
            c.get().findViewById<EditText>(R.id.etSearch).setText("Private synthetic query")
            if(cursor)c.get().findViewById<EditText>(R.id.etSearch).setSelection(2,6)
            val state=Bundle();c.saveInstanceState(state);return state
        } finally {close(c)}
    }
    @Test fun anotherAccountCannotRestoreNativeSearchOrQuery(){
        val state=snapshot();ProfileEditorAuthFixture.setOwner("account-b")
        val c=open(state)
        try {
            assertTrue(c.get().findViewById<EditText>(R.id.etSearch).text.isEmpty())
            assertEquals(View.GONE,c.get().findViewById<View>(R.id.searchTopBar).visibility)
        } finally {close(c)}
    }
    @Test fun sameAccountRecreationPreservesNativeQueryAndSearch(){
        val state=snapshot();val c=open(state)
        try {
            assertEquals("Private synthetic query",c.get().findViewById<EditText>(R.id.etSearch).text.toString())
            assertEquals(View.VISIBLE,c.get().findViewById<View>(R.id.searchTopBar).visibility)
        } finally {close(c)}
    }
    @Test fun foreignNativeHierarchyCannotRepopulateClosedSearch(){
        val state=snapshot();state.putBoolean("home_search_active",false)
        state.remove("home_search_query");ProfileEditorAuthFixture.setOwner("account-b")
        val c=open(state)
        try {assertTrue(c.get().findViewById<EditText>(R.id.etSearch).text.isEmpty())}
        finally {close(c)}
    }

    @Test fun untaggedLegacyHierarchyCannotRestorePrivateSearch(){
        val state=snapshot();state.remove("home_search_owner_uid")
        val c=open(state)
        try {
            assertTrue(c.get().findViewById<EditText>(R.id.etSearch).text.isEmpty())
            assertEquals(View.GONE,c.get().findViewById<View>(R.id.searchTopBar).visibility)
        } finally {close(c)}
    }
    @Test fun sameOwnerKeepsNativeCursorSelection(){
        val c=open(snapshot(cursor=true))
        try {
            val field=c.get().findViewById<EditText>(R.id.etSearch)
            assertEquals(2,field.selectionStart);assertEquals(6,field.selectionEnd)
        } finally {close(c)}
    }
    @Test fun oldActivityCannotStampNewOwnersIdentityOnSavedQuery(){
        val c=open();val state=Bundle()
        try {
            c.get().findViewById<View>(R.id.btnSearch).performClick();idle()
            c.get().findViewById<EditText>(R.id.etSearch).setText("Old owner query")
            ProfileEditorAuthFixture.setOwner("account-b")
            c.saveInstanceState(state)
            assertEquals("account-a",state.getString("home_search_owner_uid"))
            assertFalse(state.getBoolean("home_search_active"));assertFalse(state.containsKey("home_search_query"))
        } finally {close(c)}
        val next=open(state)
        try {assertTrue(next.get().findViewById<EditText>(R.id.etSearch).text.isEmpty())}
        finally {close(next)}
    }
    @Test fun repeatedRealOpenCloseAndBackKeepOneCurrentSearchSurface(){
        val c=open()
        try {
            repeat(3){index ->
                c.get().findViewById<View>(R.id.btnSearch).performClick();idle()
                c.get().findViewById<EditText>(R.id.etSearch).setText("Query $index")
                if(index%2==0)c.get().findViewById<View>(R.id.btnSearchBack).performClick()
                else c.get().onBackPressedDispatcher.onBackPressed()
                shadowOf(Looper.getMainLooper()).idleFor(java.time.Duration.ofMillis(300))
                val field=c.get().findViewById<EditText>(R.id.etSearch)
                assertTrue(field.text.isEmpty());assertFalse(field.hasFocus())
                assertEquals(View.GONE,c.get().findViewById<View>(R.id.searchTopBar).visibility)
            }
        } finally {close(c)}
    }
    @Test @Config(qualifiers="hi-rIN-ldrtl-night")
    fun sameOwnerHindiNightLargeFontRestoresRealQuery(){
        org.robolectric.RuntimeEnvironment.setFontScale(2f)
        val c=open(snapshot())
        try {
            assertEquals(2f,c.get().resources.configuration.fontScale)
            assertEquals("Private synthetic query",c.get().findViewById<EditText>(R.id.etSearch).text.toString())
            assertEquals(View.VISIBLE,c.get().findViewById<View>(R.id.searchTopBar).visibility)
        } finally {close(c)}
    }

}
