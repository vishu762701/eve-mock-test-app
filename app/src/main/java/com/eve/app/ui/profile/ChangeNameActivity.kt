/*
 * Derived from Telegram Android ChangeNameActivity.java, Copyright Nikolai Kudashov.
 * GNU GPL v2 or later. See THIRD_PARTY_NOTICES.md.
 */

package com.eve.app.ui.profile

import android.app.Activity
import android.os.Bundle
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.core.view.doOnLayout
import androidx.lifecycle.lifecycleScope
import com.eve.app.data.remote.ApiClient
import com.eve.app.data.remote.ProfileRequestOwner
import com.eve.app.databinding.ActivityChangeNameBinding
import com.eve.app.ui.common.EveBaseActivity
import com.eve.app.util.AppBulletin
import com.eve.app.util.EveNavigationHelper
import com.eve.app.util.TelegramShake
import com.eve.app.util.VibrationHelper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ChangeNameActivity : EveBaseActivity() {
    private var editorOwnerUid: String? = null

    private lateinit var binding: ActivityChangeNameBinding
    private var saveInFlight = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        editorOwnerUid = ProfileEditorAccountGuard.bind(this, savedInstanceState) ?: return
        binding = ActivityChangeNameBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val user = FirebaseAuth.getInstance().currentUser
        val initialName = intent.getStringExtra("current_name") ?: user?.displayName ?: ""
        binding.etName.setText(initialName)
        binding.etName.setSelection(binding.etName.text.length)

        // ChangeNameActivity.java:118-126,190-196: the name field requests
        // focus and shows the keyboard when the editor opens.
        binding.etName.imeOptions = EditorInfo.IME_ACTION_DONE
        binding.etName.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                saveName()
                true
            } else {
                false
            }
        }
        if (savedInstanceState == null) {
            binding.etName.doOnLayout {
                if (isFinishing || isDestroyed) return@doOnLayout
                binding.etName.requestFocus()
                binding.etName.setSelection(binding.etName.text.length)
                (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager)
                    .showSoftInput(binding.etName, InputMethodManager.SHOW_IMPLICIT)
            }
        }

        binding.btnBack.setOnClickListener {
            EveNavigationHelper.finishWithTransition(this)
        }

        binding.btnDone.setOnClickListener {
            saveName()
        }
    }

    private fun saveName() {
        val ownerUid = editorOwnerUid ?: return
        if (FirebaseAuth.getInstance().currentUser?.uid != ownerUid) { finish(); return }
        if (saveInFlight) return
        // EVE display names may contain many words; the canonical @username has separate rules.
        // Normalize whitespace to match the backend's bounded display_name column contract.
        val name = binding.etName.text.toString().trim().replace(Regex("\\s+"), " ")
        if (name.isBlank() || name.length > 100) {
            // ChangeNameActivity.java:140-149 gives its field a red line state on
            // invalid edits; EVE exposes that feedback through the shared source
            // shake/error haptic because this single-field host has no error line.
            TelegramShake.shakeViewSpring(binding.etName, 10f)
            VibrationHelper.vibrateAppError(this)
            AppBulletin.showError(this, "Enter a display name of 1–100 characters")
            return
        }
        // ChangeNameActivity.java:67-72,178-180: returns without a network write
        // when the confirmed name is unchanged, and finishes the editor.
        val confirmedName = (intent.getStringExtra("current_name")
            ?: FirebaseAuth.getInstance().currentUser?.displayName ?: "")
            .trim().replace(Regex("\\s+"), " ")
        if (name == confirmedName) {
            finish()
            return
        }
        val user = FirebaseAuth.getInstance().currentUser ?: return
        saveInFlight = true
        binding.btnDone.isEnabled = false
        binding.etName.isEnabled = false
        lifecycleScope.launch {
            try {
                val response = ApiClient.apiService.updateProfile(mapOf("displayName" to name), ProfileRequestOwner(ownerUid))
                if (response.success) {
                    val confirmed = ApiClient.apiService.getProfile(ProfileRequestOwner(ownerUid))
                    if (confirmed.success && confirmed.data?.displayName == name &&
                        FirebaseAuth.getInstance().currentUser?.uid == user.uid && confirmed.data.id == ownerUid) {
                        // Firebase identity displayName is a secondary cache; EVE backend is authoritative.
                        try {
                            user.updateProfile(UserProfileChangeRequest.Builder().setDisplayName(name).build()).await()
                        } catch (_: Exception) {
                            // Keep the successful server value; profile reload reads the EVE API.
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
                binding.btnDone.isEnabled = true
                binding.etName.isEnabled = true
            }
        }
    }
}
