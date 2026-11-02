package com.eve.app.ui.profile

import android.app.Application
import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import com.eve.app.R
import com.eve.app.ui.common.EveBaseActivity
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.Config

/** Real editor Activity lifecycle/view-state; only Firebase SDK identity boundary is replaced. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk=[35],application=Application::class,shadows=[ProfileEditorAuthFixture.ShadowAuth::class])
class ProfileEditorRestorationTest {
    private data class Editor(val type: Class<out EveBaseActivity>, val field: Int, val extra: String, val initial: String)
    private val editors = listOf(
        Editor(ChangeNameActivity::class.java,R.id.etName,"current_name","Owner"),
        Editor(ChangePhoneActivity::class.java,R.id.etPhone,"current_phone","+91 1234567890"),
        Editor(ChangeUsernameActivity::class.java,R.id.etUsername,"current_username","ownername"),
        Editor(ChangeBioActivity::class.java,R.id.etBio,"current_bio","Retained compatibility data")
    )
    @Before fun before() { ProfileEditorAuthFixture.reset(); ProfileEditorAuthFixture.setOwner("owner-a") }
    @After fun after() { ProfileEditorAuthFixture.reset() }
    private fun open(editor: Editor, state: Bundle? = null): ActivityController<out EveBaseActivity> {
        val intent = Intent(RuntimeEnvironment.getApplication(),editor.type)
            .putExtra(editor.extra,editor.initial)
            .putExtra("profile_editor_owner_uid","owner-a")
        val controller = Robolectric.buildActivity(editor.type,intent)
        controller.get().setTheme(R.style.Theme_Eve_Profile)
        if (state == null) controller.setup() else controller.setup(state)
        controller.visible()
        return controller
    }
    private fun close(controller: ActivityController<out EveBaseActivity>) { controller.pause().stop().destroy() }
    private fun assertRestoredOwnerRejected(editor: Editor) {
        val first = open(editor)
        assertFalse(first.get().isFinishing)
        first.get().findViewById<EditText>(editor.field).setText("Private draft")
        val state = Bundle(); first.saveInstanceState(state); close(first)
        ProfileEditorAuthFixture.setOwner("owner-b")
        val restored = open(editor,state)
        try {
            assertTrue("${editor.type.simpleName} must reject old-account state",restored.get().isFinishing)
            assertNull("Old private fields must not be inflated",restored.get().findViewById<EditText>(editor.field))
        } finally { close(restored) }
        assertTrue(ProfileEditorAuthFixture.listeners.isEmpty())
    }
    @Test fun nameDraftCannotAcquireAnotherAccount() = assertRestoredOwnerRejected(editors[0])
    @Test fun phoneDraftCannotAcquireAnotherAccount() = assertRestoredOwnerRejected(editors[1])
    @Test fun usernameDraftCannotAcquireAnotherAccount() = assertRestoredOwnerRejected(editors[2])
    @Test fun retainedBioDraftCannotAcquireAnotherAccount() = assertRestoredOwnerRejected(editors[3])
    @Test fun sameAccountRecreationPreservesTheActualNativeDraft() {
        for (editor in editors) {
            val first = open(editor)
            val draft = if (editor.type == ChangeUsernameActivity::class.java) "ownername" else "Unsubmitted draft"
            first.get().findViewById<EditText>(editor.field).setText(draft)
            val state=Bundle(); first.saveInstanceState(state); close(first)
            val restored=open(editor,state)
            try {
                assertFalse(restored.get().isFinishing)
                assertEquals(draft,restored.get().findViewById<EditText>(editor.field).text.toString())
            } finally { close(restored) }
            assertTrue(ProfileEditorAuthFixture.listeners.isEmpty())
        }
    }
    private fun assertDelayedLaunchRejected(editor: Editor) {
        ProfileEditorAuthFixture.setOwner("owner-b")
        val controller=open(editor)
        try {
            assertTrue(editor.type.simpleName,controller.get().isFinishing)
            assertNull(controller.get().findViewById<EditText>(editor.field))
        } finally { close(controller) }
    }
    @Test fun delayedNameLaunchRejectsChangedAccount() = assertDelayedLaunchRejected(editors[0])
    @Test fun delayedPhoneLaunchRejectsChangedAccount() = assertDelayedLaunchRejected(editors[1])
    @Test fun delayedUsernameLaunchRejectsChangedAccount() = assertDelayedLaunchRejected(editors[2])
    @Test fun delayedRetainedBioLaunchRejectsChangedAccount() = assertDelayedLaunchRejected(editors[3])
    @Test fun unverifiableLegacyRestorationCannotAcquireCurrentAccount() {
        for (editor in editors) {
            val controller=open(editor,Bundle())
            try { assertTrue(controller.get().isFinishing) } finally { close(controller) }
        }
    }
    @Test fun liveAccountReplacementStillFinishesAndRemovesListeners() {
        for(editor in editors) {
            ProfileEditorAuthFixture.setOwner("owner-a")
            val controller=open(editor)
            try {
                assertFalse(controller.get().isFinishing)
                ProfileEditorAuthFixture.setOwner(null)
                assertTrue(controller.get().isFinishing)
            } finally { close(controller) }
            assertTrue(ProfileEditorAuthFixture.listeners.isEmpty())
        }
    }
}
