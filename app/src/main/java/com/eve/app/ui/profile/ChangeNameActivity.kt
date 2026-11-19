package com.eve.app.ui.profile

import android.app.Activity
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import com.eve.app.data.remote.ApiClient
import com.eve.app.data.remote.ProfileRequestOwner
import com.eve.app.ui.common.TelegramHostActivity
import com.eve.app.util.AppBulletin
import com.eve.app.util.TelegramShake
import com.eve.app.util.VibrationHelper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import org.telegram.ui.ChangeNameActivity as TelegramChangeNameActivity

/**
 * Host Activity for Telegram ChangeNameActivity port.
 * Extends TelegramHostActivity (INavigationLayout) and implements TelegramProfileDelegates.ChangeNameDelegate.
 */
class ChangeNameActivity : TelegramHostActivity<TelegramChangeNameActivity>(), TelegramProfileDelegates.ChangeNameDelegate {
    private var editorOwnerUid: String? = null
    private var saveInFlight = false

    override fun onBeforeCreateFragment(savedInstanceState: Bundle?): Boolean {
        editorOwnerUid = ProfileEditorAccountGuard.bind(this, savedInstanceState) ?: return false
        return true
    }

    override fun createTelegramFragment(savedInstanceState: Bundle?): TelegramChangeNameActivity {
        return TelegramChangeNameActivity(null).apply {
            setEveDelegate(this@ChangeNameActivity)
        }
    }

    // --- ChangeNameDelegate implementation (Eve save flow & account guard) ---
    override fun getInitialFirstName(): String {
        val user = FirebaseAuth.getInstance().currentUser
        val fullName = intent.getStringExtra("current_name") ?: user?.displayName ?: ""
        val parts = fullName.trim().split(" ", limit = 2)
        return parts.getOrNull(0) ?: ""
    }

    override fun getInitialLastName(): String {
        val user = FirebaseAuth.getInstance().currentUser
        val fullName = intent.getStringExtra("current_name") ?: user?.displayName ?: ""
        val parts = fullName.trim().split(" ", limit = 2)
        return parts.getOrNull(1) ?: ""
    }

    override fun saveName(first: String, last: String) {
        val ownerUid = editorOwnerUid ?: return
        if (FirebaseAuth.getInstance().currentUser?.uid != ownerUid) { finish(); return }
        if (saveInFlight) return

        val name = "$first $last".trim().replace(Regex("\\s+"), " ")
        if (name.isBlank() || name.length > 100) {
            TelegramShake.shakeViewSpring(fragment.fragmentView, 10f)
            VibrationHelper.vibrateAppError(this)
            AppBulletin.showError(this, "Enter a display name of 1–100 characters")
            return
        }

        val confirmedName = (intent.getStringExtra("current_name")
            ?: FirebaseAuth.getInstance().currentUser?.displayName ?: "")
            .trim().replace(Regex("\\s+"), " ")
        if (name == confirmedName) {
            finish()
            return
        }

        val user = FirebaseAuth.getInstance().currentUser ?: return
        saveInFlight = true
        lifecycleScope.launch {
            try {
                val response = ApiClient.apiService.updateProfile(mapOf("displayName" to name), ProfileRequestOwner(ownerUid))
                if (response.success) {
                    val confirmed = ApiClient.apiService.getProfile(ProfileRequestOwner(ownerUid))
                    if (confirmed.success && confirmed.data?.displayName == name &&
                        FirebaseAuth.getInstance().currentUser?.uid == user.uid && confirmed.data.id == ownerUid) {
                        try {
                            user.updateProfile(UserProfileChangeRequest.Builder().setDisplayName(name).build()).await()
                        } catch (_: Exception) {
                        }
                        setResult(Activity.RESULT_OK)
                        finish()
                    } else {
                        AppBulletin.showError(this@ChangeNameActivity,
                            "Display name update could not be verified. Refresh your profile before retrying.")
                    }
                } else {
                    AppBulletin.showError(this@ChangeNameActivity,
                        response.error ?: "Failed to update display name")
                }
            } catch (cancel: kotlinx.coroutines.CancellationException) {
                throw cancel
            } catch (e: Exception) {
                AppBulletin.showError(this@ChangeNameActivity,
                    e.localizedMessage ?: "Failed to update display name")
            } finally {
                saveInFlight = false
            }
        }
    }
}
