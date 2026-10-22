package com.eve.app.util

import android.view.View
import androidx.lifecycle.lifecycleScope
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
        onAuthorized: () -> Unit
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

        activity.lifecycleScope.launch {
            val authorized = try {
                if (requireFreshServer) {
                    AdminRepository().isAdminFresh(user.uid, email)
                } else {
                    AdminRepository().isAdmin(email)
                }
            } catch (cancel: CancellationException) {
                throw cancel
            } catch (_: Exception) {
                false
            }
            if (activity.isFinishing || activity.isDestroyed) return@launch
            val current = FirebaseAuth.getInstance().currentUser
            if (!authorized || current?.uid != user.uid ||
                !current.email.equals(email, ignoreCase = true)) {
                activity.finish()
                return@launch
            }
            protectedRoot.visibility = View.VISIBLE
            onAuthorized()
        }
    }
}
