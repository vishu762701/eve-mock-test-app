package com.eve.app.util

import android.view.View
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.eve.app.data.repository.AdminRepository
import com.eve.app.ui.common.EveBaseActivity
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CancellationException
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
        // INVISIBLE prevents focus/touch and content disclosure while the async
        // authorization check is unresolved. No privileged listeners/data loads yet.
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
        val identityListener = FirebaseAuth.AuthStateListener { state ->
            val live = state.currentUser
            if (live?.uid != user.uid || live?.email?.equals(email, ignoreCase = true) != true) {
                protectedRoot.visibility = View.INVISIBLE
                activity.finish()
            }
        }
        auth.addAuthStateListener(identityListener)
        activity.lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onDestroy(owner: LifecycleOwner) {
                auth.removeAuthStateListener(identityListener)
            }
        })

        activity.lifecycleScope.launch {
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
            if (activity.isFinishing || activity.isDestroyed) return@launch
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
