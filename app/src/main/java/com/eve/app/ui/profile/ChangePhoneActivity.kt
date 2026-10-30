/*
 * Derived from Telegram Android profile phone flow.
 * GNU GPL v2 or later. See THIRD_PARTY_NOTICES.md.
 */

package com.eve.app.ui.profile

import android.app.Activity
import android.os.Bundle
import android.view.View
import androidx.lifecycle.lifecycleScope
import com.eve.app.data.remote.ApiClient
import com.eve.app.databinding.ActivityChangePhoneBinding
import com.eve.app.ui.common.EveBaseActivity
import com.eve.app.util.AppBulletin
import com.eve.app.util.EveNavigationHelper
import kotlinx.coroutines.launch

class ChangePhoneActivity : EveBaseActivity() {

    private lateinit var binding: ActivityChangePhoneBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChangePhoneBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val currentPhone = intent.getStringExtra("current_phone") ?: ""
        if (currentPhone.isNotEmpty()) {
            binding.etPhone.setText(currentPhone)
        } else {
            binding.etPhone.setText("+91 ")
        }
        binding.etPhone.setSelection(binding.etPhone.text.length)

        binding.btnBack.setOnClickListener {
            EveNavigationHelper.finishWithTransition(this)
        }

        binding.btnDone.setOnClickListener {
            savePhone()
        }
    }

    private fun savePhone() {
        val input = binding.etPhone.text.toString().trim()
        val digitsOnly = input.filter { it.isDigit() }

        if (input.isNotEmpty() && input != "+91" && input != "+91 ") {
            if (digitsOnly.length < 7 || digitsOnly.length > 15) {
                binding.tvStatus.text = "Please enter a valid phone number (7-15 digits)"
                binding.tvStatus.visibility = View.VISIBLE
                return
            }
        }
        binding.tvStatus.visibility = View.GONE

        binding.btnDone.isEnabled = false
        val phoneToSave = if (input == "+91" || input == "+91 ") "" else input

        lifecycleScope.launch {
            try {
                val res = ApiClient.apiService.updateProfile(mapOf("phone" to phoneToSave))
                if (res.success) {
                    setResult(Activity.RESULT_OK)
                    finish()
                } else {
                    binding.btnDone.isEnabled = true
                    AppBulletin.showError(this@ChangePhoneActivity, res.error ?: "Failed to update phone number")
                }
            } catch (e: Exception) {
                binding.btnDone.isEnabled = true
                AppBulletin.showError(this@ChangePhoneActivity, e.localizedMessage ?: "Failed to update phone number")
            }
        }
    }
}
