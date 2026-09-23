package com.eve.app.util

import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.eve.app.data.repository.AdminRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

object SecurityHelper {

    private val adminRepo = AdminRepository()

    /**
     * Applies screenshot/recording protection (FLAG_SECURE) to sensitive screens for regular users.
     * To prevent window leakage before asynchronous admin verification finishes, FLAG_SECURE
     * is applied immediately by default. If the current authenticated user is an admin,
     * FLAG_SECURE is removed.
     */
    fun applyScreenProtection(activity: AppCompatActivity) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        val email = currentUser?.email

        // Fast path for hardcoded admin
        if (isHardcodedAdmin(email)) {
            activity.window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
            return
        }

        // Apply protection immediately for non-admin or unverified states
        activity.window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)

        // Verify dynamic admin status asynchronously
        if (currentUser != null && email != null) {
            activity.lifecycleScope.launch {
                val isAdmin = adminRepo.isAdmin(email)
                if (!activity.isFinishing && !activity.isDestroyed) {
                    if (isAdmin) {
                        activity.window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                    } else {
                        activity.window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
                    }
                }
            }
        }
    }
}
