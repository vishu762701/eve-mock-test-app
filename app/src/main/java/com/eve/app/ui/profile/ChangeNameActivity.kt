/*
 * Derived from Telegram Android ChangeNameActivity.java, Copyright Nikolai Kudashov.
 * GNU GPL v2 or later. See THIRD_PARTY_NOTICES.md.
 */

package com.eve.app.ui.profile

import android.app.Activity
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import com.eve.app.data.remote.ApiClient
import com.eve.app.databinding.ActivityChangeNameBinding
import com.eve.app.ui.common.EveBaseActivity
import com.eve.app.util.AppBulletin
import com.eve.app.util.EveNavigationHelper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ChangeNameActivity : EveBaseActivity() {

    private lateinit var binding: ActivityChangeNameBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChangeNameBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val user = FirebaseAuth.getInstance().currentUser
        val initialName = intent.getStringExtra("current_name") ?: user?.displayName ?: ""
        binding.etName.setText(initialName)
        binding.etName.setSelection(binding.etName.text.length)

        binding.btnBack.setOnClickListener {
            EveNavigationHelper.finishWithTransition(this)
        }

        binding.btnDone.setOnClickListener {
            saveName()
        }
    }

    private fun saveName() {
        // EVE display names may contain many words; the canonical @username has separate rules.
        // Normalize whitespace to match the backend's bounded display_name column contract.
        val name = binding.etName.text.toString().trim().replace(Regex("\\s+"), " ")
        if (name.isBlank() || name.length > 100) {
            AppBulletin.showError(this, "Enter a display name of 1–100 characters")
            return
        }
        val user = FirebaseAuth.getInstance().currentUser ?: return
        binding.btnDone.isEnabled = false
        binding.etName.isEnabled = false
        lifecycleScope.launch {
            try {
                val response = ApiClient.apiService.updateProfile(mapOf("displayName" to name))
                if (response.success) {
                    val confirmed = ApiClient.apiService.getProfile()
                    if (confirmed.success && confirmed.data?.displayName == name &&
                        FirebaseAuth.getInstance().currentUser?.uid == user.uid) {
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
            } catch (e: Exception) {
                AppBulletin.showError(this@ChangeNameActivity,
                    e.localizedMessage ?: "Failed to update display name")
            } finally {
                binding.btnDone.isEnabled = true
                binding.etName.isEnabled = true
            }
        }
    }
}
