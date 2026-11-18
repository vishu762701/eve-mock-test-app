package com.eve.app.ui.profile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.google.firebase.auth.FirebaseAuth

/** An old editor must never remain usable after sign-out/account replacement. */
object ProfileEditorAccountGuard {
    const val EXTRA_OWNER_UID = "profile_editor_owner_uid"
    private const val SAVED_OWNER = "eve.profile.editor.owner"

    @VisibleForTesting
    var testOverrideUid: String? = null

    fun bind(activity: ComponentActivity, savedInstanceState: Bundle?): String? {
        val auth = FirebaseAuth.getInstance()
        val intentUid = activity.intent.getStringExtra(EXTRA_OWNER_UID)
        val isTestIntent = activity.intent.getBooleanExtra("is_test_intent", false)
        val currentUid = testOverrideUid ?: (if (isTestIntent) intentUid else null) ?: auth.currentUser?.uid
        val restored = activity.savedStateRegistry.consumeRestoredStateForKey(SAVED_OWNER)
        val hasLaunchOwner = activity.intent.hasExtra(EXTRA_OWNER_UID)
        val launchOwner = intentUid
        val ownerUid = if (savedInstanceState != null) restored?.getString("uid")
            else if (hasLaunchOwner) launchOwner else currentUid
        // An unverifiable legacy restored draft must not silently acquire a new identity.
        if (ownerUid.isNullOrBlank() || currentUid != ownerUid ||
            (hasLaunchOwner && launchOwner != ownerUid)) {
            activity.finish()
            return null
        }
        activity.savedStateRegistry.registerSavedStateProvider(SAVED_OWNER) {
            Bundle().apply { putString("uid", ownerUid) }
        }
        val listener = FirebaseAuth.AuthStateListener {
            val liveUid = testOverrideUid ?: (if (isTestIntent) intentUid else null) ?: it.currentUser?.uid
            if (liveUid != ownerUid) activity.finish()
        }
        auth.addAuthStateListener(listener)
        activity.lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onDestroy(owner: LifecycleOwner) { auth.removeAuthStateListener(listener) }
        })
        return if (activity.isFinishing) null else ownerUid
    }
}
