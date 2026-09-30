package com.eve.app.ui.admin

import com.eve.app.ui.common.EveBaseActivity

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.eve.app.data.model.*
import com.eve.app.data.remote.ApiClient
import com.eve.app.data.repository.AuditLogRepository
import com.eve.app.databinding.ActivityManagePremiumBinding
import com.eve.app.util.AppBulletin
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.tabs.TabLayout
import kotlinx.coroutines.launch

class ManagePremiumActivity : EveBaseActivity() {

    private lateinit var binding: ActivityManagePremiumBinding
    private val auditRepo = AuditLogRepository()

    private val transactionAdapter = AdminTransactionAdapter()
    private lateinit var userAdapter: AdminPremiumUserAdapter

    private var currentConfig: AdminPremiumConfigDto? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityManagePremiumBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupTabs()
        setupRecyclerViews()
        setupSettingsActions()

        loadAllData()
    }

    private fun setupToolbar() {
        binding.btnBack.setOnClickListener { finish() }
        binding.btnRefresh.setOnClickListener { loadAllData() }
    }

    private fun setupTabs() {
        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> {
                        binding.containerSettings.visibility = View.VISIBLE
                        binding.containerTransactions.visibility = View.GONE
                        binding.containerUsers.visibility = View.GONE
                    }
                    1 -> {
                        binding.containerSettings.visibility = View.GONE
                        binding.containerTransactions.visibility = View.VISIBLE
                        binding.containerUsers.visibility = View.GONE
                        loadTransactions()
                    }
                    2 -> {
                        binding.containerSettings.visibility = View.GONE
                        binding.containerTransactions.visibility = View.GONE
                        binding.containerUsers.visibility = View.VISIBLE
                        loadUsers()
                    }
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        binding.switchLifetime.setOnCheckedChangeListener { _, isChecked ->
            binding.tilDurationDays.visibility = if (isChecked) View.GONE else View.VISIBLE
        }
    }

    private fun setupRecyclerViews() {
        binding.rvTransactions.layoutManager = LinearLayoutManager(this)
        binding.rvTransactions.adapter = transactionAdapter

        userAdapter = AdminPremiumUserAdapter(
            onExtend = { user -> extendUserPremium(user) },
            onRevoke = { user -> confirmRevokeUserPremium(user) }
        )
        binding.rvPremiumUsers.layoutManager = LinearLayoutManager(this)
        binding.rvPremiumUsers.adapter = userAdapter

        binding.btnGrantManual.setOnClickListener {
            showManualGrantDialog()
        }
    }

    private fun setupSettingsActions() {
        binding.btnSaveSettings.setOnClickListener {
            saveConfig()
        }
    }

    private fun loadAllData() {
        loadConfig()
        loadTransactions()
        loadUsers()
    }

    private fun loadConfig() {
        binding.progressAdmin.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                val res = ApiClient.api.getAdminPremiumConfig()
                binding.progressAdmin.visibility = View.GONE
                if (res.success && res.data != null) {
                    val config = res.data
                    currentConfig = config

                    binding.switchEnablePremium.isChecked = config.isEnabled
                    binding.etPlanName.setText(config.planName)
                    binding.etPriceInr.setText(config.priceInr.toString())
                    binding.switchLifetime.isChecked = config.isLifetime
                    binding.etDurationDays.setText(config.durationDays.toString())
                    binding.tilDurationDays.visibility = if (config.isLifetime) View.GONE else View.VISIBLE
                    binding.etDescription.setText(config.description)
                    val benefitsText = if (config.benefits.isNotEmpty()) {
                        config.benefits.joinToString("\n")
                    } else {
                        listOf(
                            "Access to all eligible tests",
                            "Unlimited eligible reattempts",
                            "No normal 3-attempt restriction while Premium is active"
                        ).joinToString("\n")
                    }
                    binding.etBenefits.setText(benefitsText)
                    binding.switchQrEnabled.isChecked = config.qrEnabled
                    binding.switchUpiEnabled.isChecked = config.upiEnabled
                    binding.etSessionExpiry.setText(config.sessionExpiryMinutes.toString())
                    binding.etMerchantVpa.setText(config.merchantVpa)
                }
            } catch (e: Exception) {
                binding.progressAdmin.visibility = View.GONE
                AppBulletin.showError(this@ManagePremiumActivity, "Failed to load config: ${e.message}")
            }
        }
    }

    private fun saveConfig() {
        val planName = binding.etPlanName.text.toString().trim().ifBlank { "Premium Pro" }
        val priceInr = binding.etPriceInr.text.toString().trim().toIntOrNull() ?: 99
        val isLifetime = binding.switchLifetime.isChecked
        val durationDays = binding.etDurationDays.text.toString().trim().toIntOrNull() ?: 30
        val description = binding.etDescription.text.toString().trim()
        val benefits = binding.etBenefits.text.toString()
            .lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
        val isEnabled = binding.switchEnablePremium.isChecked
        val qrEnabled = binding.switchQrEnabled.isChecked
        val upiEnabled = binding.switchUpiEnabled.isChecked
        val sessionExpiry = binding.etSessionExpiry.text.toString().trim().toIntOrNull() ?: 10
        val merchantVpa = binding.etMerchantVpa.text.toString().trim().ifBlank { "evemocktest@upi" }

        val updated = (currentConfig ?: AdminPremiumConfigDto()).copy(
            isEnabled = isEnabled,
            planName = planName,
            priceInr = priceInr,
            isLifetime = isLifetime,
            durationDays = durationDays,
            description = description,
            benefits = benefits,
            qrEnabled = qrEnabled,
            upiEnabled = upiEnabled,
            sessionExpiryMinutes = sessionExpiry,
            merchantVpa = merchantVpa
        )

        binding.progressAdmin.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                val res = ApiClient.api.updateAdminPremiumConfig(updated)
                binding.progressAdmin.visibility = View.GONE
                if (res.success) {
                    auditRepo.recordLog(
                        actionType = "PREMIUM_CONFIG_UPDATED",
                        description = "Updated Premium plan '$planName' (Price: ₹$priceInr, Duration: $durationDays days, Lifetime: $isLifetime)"
                    )
                    AppBulletin.showSuccess(this@ManagePremiumActivity, "Settings saved successfully!")
                    currentConfig = updated
                } else {
                    AppBulletin.showError(this@ManagePremiumActivity, res.error ?: "Save failed")
                }
            } catch (e: Exception) {
                binding.progressAdmin.visibility = View.GONE
                AppBulletin.showError(this@ManagePremiumActivity, "Error saving: ${e.message}")
            }
        }
    }

    private fun loadTransactions() {
        lifecycleScope.launch {
            try {
                val res = ApiClient.api.getAdminTransactions()
                if (res.success && res.data != null) {
                    val list = res.data
                    transactionAdapter.submitList(list)
                    binding.tvEmptyTransactions.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
                }
            } catch (_: Exception) {}
        }
    }

    private fun loadUsers() {
        lifecycleScope.launch {
            try {
                val res = ApiClient.api.getAdminPremiumUsers()
                if (res.success && res.data != null) {
                    val list = res.data
                    userAdapter.submitList(list)
                    binding.tvEmptyUsers.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
                }
            } catch (_: Exception) {}
        }
    }

    private fun showManualGrantDialog() {
        val dialogView = LayoutInflater.from(this).inflate(com.eve.app.R.layout.dialog_manual_grant_premium, null)
        val etUser = dialogView.findViewById<EditText>(com.eve.app.R.id.etGrantUser)
        val etDays = dialogView.findViewById<EditText>(com.eve.app.R.id.etGrantDays)
        val switchLifetime = dialogView.findViewById<MaterialSwitch>(com.eve.app.R.id.switchGrantLifetime)

        switchLifetime.setOnCheckedChangeListener { _, isChecked ->
            etDays.visibility = if (isChecked) View.GONE else View.VISIBLE
        }

        MaterialAlertDialogBuilder(this)
            .setTitle("Grant Premium Manually")
            .setView(dialogView)
            .setPositiveButton("Grant") { _, _ ->
                val target = etUser.text.toString().trim()
                val isLife = switchLifetime.isChecked
                val days = etDays.text.toString().trim().toIntOrNull() ?: 30

                if (target.isBlank()) {
                    AppBulletin.showError(this, "User ID or Email is required")
                    return@setPositiveButton
                }

                lifecycleScope.launch {
                    try {
                        val res = ApiClient.api.grantPremium(
                            AdminGrantRequest(
                                userIdOrEmail = target,
                                durationDays = days,
                                isLifetime = isLife,
                                planName = "Admin Manual Access"
                            )
                        )
                        if (res.success) {
                            auditRepo.recordLog(
                                actionType = "PREMIUM_MANUALLY_GRANTED",
                                description = "Manually granted Premium to '$target' (Lifetime: $isLife, Days: $days)"
                            )
                            AppBulletin.showSuccess(this@ManagePremiumActivity, "Premium granted to $target")
                            loadUsers()
                        } else {
                            AppBulletin.showError(this@ManagePremiumActivity, res.error ?: "Grant failed")
                        }
                    } catch (e: Exception) {
                        AppBulletin.showError(this@ManagePremiumActivity, "Error: ${e.message}")
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun extendUserPremium(user: AdminPremiumUserDto) {
        lifecycleScope.launch {
            try {
                val res = ApiClient.api.extendPremium(AdminExtendRequest(user.userId, 30))
                if (res.success) {
                    auditRepo.recordLog(
                        actionType = "PREMIUM_EXTENDED",
                        description = "Extended Premium by 30 days for user '${user.email.ifBlank { user.userId }}'"
                    )
                    AppBulletin.showSuccess(this@ManagePremiumActivity, "Extended by 30 days")
                    loadUsers()
                } else {
                    AppBulletin.showError(this@ManagePremiumActivity, res.error ?: "Extension failed")
                }
            } catch (e: Exception) {
                AppBulletin.showError(this@ManagePremiumActivity, "Error: ${e.message}")
            }
        }
    }

    private fun confirmRevokeUserPremium(user: AdminPremiumUserDto) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Revoke Premium?")
            .setMessage("Are you sure you want to revoke Premium access for ${user.email.ifBlank { user.userId }}? They will revert to the standard 3-attempt restriction.")
            .setPositiveButton("Revoke") { _, _ ->
                lifecycleScope.launch {
                    try {
                        val res = ApiClient.api.revokePremium(AdminRevokeRequest(user.userId))
                        if (res.success) {
                            auditRepo.recordLog(
                                actionType = "PREMIUM_REVOKED",
                                description = "Revoked Premium for user '${user.email.ifBlank { user.userId }}'"
                            )
                            AppBulletin.showSuccess(this@ManagePremiumActivity, "Premium revoked")
                            loadUsers()
                        } else {
                            AppBulletin.showError(this@ManagePremiumActivity, res.error ?: "Revocation failed")
                        }
                    } catch (e: Exception) {
                        AppBulletin.showError(this@ManagePremiumActivity, "Error: ${e.message}")
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
