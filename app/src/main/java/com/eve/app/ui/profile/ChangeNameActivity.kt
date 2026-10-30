/*
 * Derived from Telegram Android ChangeNameActivity.java, Copyright Nikolai Kudashov.
 * GNU GPL v2 or later. See THIRD_PARTY_NOTICES.md.
 */

package com.eve.app.ui.profile

import android.app.Activity
import android.os.Bundle
import android.widget.Toast
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
        val name = binding.etName.text.toString().trim()
        if (name.isEmpty()) {
            Toast.makeText(this, "Name cannot be empty", Toast.LENGTH_SHORT).show()
            return
        }

        binding.btnDone.isEnabled = false
        val user = FirebaseAuth.getInstance().currentUser

        lifecycleScope.launch {
            try {
                val res = ApiClient.apiService.updateProfile(mapOf("displayName" to name))
                if (res.success) {
                    try {
                        user?.updateProfile(UserProfileChangeRequest.Builder().setDisplayName(name).build())?.await()
                    } catch (_: Exception) {}
                    setResult(Activity.RESULT_OK)
                    finish()
                } else {
                    binding.btnDone.isEnabled = true
                    AppBulletin.showError(this@ChangeNameActivity, res.error ?: "Failed to update name")
                }
            } catch (e: Exception) {
                binding.btnDone.isEnabled = true
                AppBulletin.showError(this@ChangeNameActivity, e.localizedMessage ?: "Failed to update name")
            }
        }
    }
}
