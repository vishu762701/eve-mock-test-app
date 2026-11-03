package com.eve.app.util

import android.view.View
import com.eve.app.R
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.eve.app.data.repository.AdminRepository
import com.eve.app.ui.common.EveBaseActivity
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * UI-only defense in depth for privileged administration workflows.
 * Backend rules remain authoritative for every read/write.
 */
object AdminAccessGate {
    fun requireAdmin(
        activity: EveBaseActivity,
        protectedRoot: View,
        requireFreshServer: Boolean = false,
        requiredRight: String? = null,
        restorationScope: String? = null,
        isRestoring: Boolean = false,
        onAuthorized: (com.eve.app.data.remote.AdminMeData) -> Unit
    ) {
        // Each protected root has one live gate. Revoking a prior gate also cancels
        // its suspended permissions request, unregisters its Firebase listener,
        // and invalidates any late authorization callback.
        (protectedRoot.getTag(R.id.eve_admin_gate_lifetime) as? AdminGateLifecycleLease)
            ?.invalidate()
        protectedRoot.visibility = View.INVISIBLE
        val user = FirebaseAuth.getInstance().currentUser
        val email = user?.email
        if (user == null || email.isNullOrBlank()) {
            activity.finish()
            return
        }

        // Optional draft ownership uses this existing gate, never a second auth/state manager.
        if (restorationScope != null) {
            val key = "eve.admin.gate.owner"
            val restored = activity.savedStateRegistry.consumeRestoredStateForKey(key)
            if (isRestoring && (restored?.getString("uid") != user.uid ||
                    restored.getString("email")?.equals(email, ignoreCase = true) != true ||
                    restored.getString("scope") != restorationScope)) {
                activity.finish()
                return
            }
            activity.savedStateRegistry.registerSavedStateProvider(key) {
                android.os.Bundle().apply {
                    putString("uid", user.uid); putString("email", email)
                    putString("scope", restorationScope)
                }
            }
        }

        // The verified screen remains bound to the identity that initiated its gate.
        val auth = FirebaseAuth.getInstance()
        lateinit var identityListener: FirebaseAuth.AuthStateListener
        var authorizationJob: Job? = null
        var cleanupObserver: DefaultLifecycleObserver? = null
        lateinit var lease: AdminGateLifecycleLease
        lease = AdminGateLifecycleLease {
            authorizationJob?.cancel()
            auth.removeAuthStateListener(identityListener)
            cleanupObserver?.let { activity.lifecycle.removeObserver(it) }
            if (protectedRoot.getTag(R.id.eve_admin_gate_lifetime) === lease) {
                protectedRoot.setTag(R.id.eve_admin_gate_lifetime, null)
            }
        }
        protectedRoot.setTag(R.id.eve_admin_gate_lifetime, lease)
        identityListener = FirebaseAuth.AuthStateListener { state ->
            if (!lease.isActive) return@AuthStateListener
            val live = state.currentUser
            if (live?.uid != user.uid || live?.email?.equals(email, ignoreCase = true) != true) {
                protectedRoot.visibility = View.INVISIBLE
                lease.invalidate()
                activity.finish()
            }
        }
        auth.addAuthStateListener(identityListener)
        // Firebase may deliver an immediate auth-change callback during
        // registration. That callback already invalidated and detached this
        // lease; do not attach a new observer or start an obsolete network read.
        if (!lease.isActive) return
        cleanupObserver = object : DefaultLifecycleObserver {
            override fun onDestroy(owner: LifecycleOwner) {
                lease.invalidate()
            }
        }
        activity.lifecycle.addObserver(requireNotNull(cleanupObserver))

        authorizationJob = activity.lifecycleScope.launch {
            val permissions = try {
                val repository = AdminRepository()
                AdminPermissionSnapshot.load(email, requiredRight,
                    membership = {
                        if (requireFreshServer) repository.isAdminFresh(user.uid, email)
                        else repository.isAdmin(email)
                    },
                    permissions = { repository.getAdminMe() })
            } catch (cancel: CancellationException) {
                throw cancel
            } catch (_: Exception) {
                null
            }
            if (!lease.isActive || protectedRoot.getTag(R.id.eve_admin_gate_lifetime) !== lease ||
                activity.isFinishing || activity.isDestroyed) return@launch
            val current = FirebaseAuth.getInstance().currentUser
            if (!AdminIdentityGuard.allowed(
                    permissions != null, user.uid, email, current?.uid, current?.email
                )) {
                activity.finish()
                return@launch
            }
            protectedRoot.visibility = View.VISIBLE
            onAuthorized(requireNotNull(permissions))
        }
    }
}

/** Small, idempotent cancellation token; never a second authorization authority. */
internal class AdminGateLifecycleLease(private val cleanup: () -> Unit) {
    var isActive: Boolean = true
        private set

    fun invalidate() {
        if (!isActive) return
        isActive = false
        cleanup()
    }
}
