package com.eve.app.ui.profile

import androidx.activity.ComponentActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.google.firebase.auth.FirebaseAuth

/** An old editor must never remain usable after sign-out/account replacement. */
internal object ProfileEditorAccountGuard {
    fun bind(activity: ComponentActivity, ownerUid: String) {
        val auth = FirebaseAuth.getInstance()
        val listener = FirebaseAuth.AuthStateListener {
            if (it.currentUser?.uid != ownerUid) activity.finish()
        }
        auth.addAuthStateListener(listener)
        activity.lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onDestroy(owner: LifecycleOwner) { auth.removeAuthStateListener(listener) }
        })
    }
}
