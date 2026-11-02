package com.eve.app.ui.profile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.google.firebase.auth.FirebaseAuth

/** An old editor must never remain usable after sign-out/account replacement. */
internal object ProfileEditorAccountGuard {
    const val EXTRA_OWNER_UID = "profile_editor_owner_uid"
    private const val SAVED_OWNER = "eve.profile.editor.owner"

    fun bind(activity: ComponentActivity, savedInstanceState: Bundle?): String? {
        val auth = FirebaseAuth.getInstance()
        val currentUid = auth.currentUser?.uid
        val restored = activity.savedStateRegistry.consumeRestoredStateForKey(SAVED_OWNER)
        val hasLaunchOwner = activity.intent.hasExtra(EXTRA_OWNER_UID)
        val launchOwner = activity.intent.getStringExtra(EXTRA_OWNER_UID)
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
            if (it.currentUser?.uid != ownerUid) activity.finish()
        }
        auth.addAuthStateListener(listener)
        activity.lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onDestroy(owner: LifecycleOwner) { auth.removeAuthStateListener(listener) }
        })
        return if (activity.isFinishing) null else ownerUid
    }
}
