package com.eve.app.ui.profile

import android.os.SystemClock
import com.eve.app.data.remote.ApiClient
import com.eve.app.data.remote.ProfileRequestOwner
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import java.io.IOException

/** Process-local, identity-scoped confirmed data; Firebase changes clear it immediately. */
internal object ProfileSession {
    val store = ConfirmedProfileStore(
        CoroutineScope(SupervisorJob() + Dispatchers.IO),
        { FirebaseAuth.getInstance().currentUser?.uid },
        { SystemClock.elapsedRealtime() }
    ) { uid ->
        val response = ApiClient.apiService.getProfile(ProfileRequestOwner(uid))
        response.data?.takeIf { response.success && it.id == uid }
            ?: throw IOException(response.error ?: "Profile unavailable")
    }

    init {
        FirebaseAuth.getInstance().addAuthStateListener { store.setOwner(it.currentUser?.uid) }
    }
}
