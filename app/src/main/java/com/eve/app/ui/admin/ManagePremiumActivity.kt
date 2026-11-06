package com.eve.app.ui.admin

import com.eve.app.designsystem.TelegramNativeAlertDialog
import android.content.DialogInterface

import com.eve.app.ui.common.EveBaseActivity

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.eve.app.data.model.*
import com.eve.app.data.remote.ApiClient
import com.eve.app.databinding.ActivityManagePremiumBinding
import com.eve.app.util.AppBulletin
import com.eve.app.util.AdminIdentityGuard
import com.eve.app.util.AdminPermissionSnapshot
import com.eve.app.data.repository.AdminRepository
import com.google.firebase.auth.FirebaseAuth
import androidx.lifecycle.Lifecycle
import kotlinx.coroutines.CancellationException
import com.eve.app.designsystem.TelegramSwitch
import com.google.android.material.tabs.TabLayout
import kotlinx.coroutines.launch
import com.eve.app.data.remote.toUserFriendlyMessage

class ManagePremiumActivity : EveBaseActivity() {

    private lateinit var binding: ActivityManagePremiumBinding
    private val adminRepo = AdminRepository()
    private var authorizedUid = ""
    private var authorizedEmail = ""
    private var adminReady = false
    private var mutationPending = false
    private var mutationEpoch = 0L
    private var readGeneration = 0L
    private var transactionsGeneration = 0L
    private var usersGeneration = 0L
    private var stoppedOnce = false
    private var reauthorizeOnResume = false
    private var activeDialog: TelegramNativeAlertDialog? = null

    private fun ownerCurrent(): Boolean {
        val live = FirebaseAuth.getInstance().currentUser
        return adminReady && !isFinishing && !isDestroyed &&
            AdminIdentityGuard.allowed(true, authorizedUid, authorizedEmail, live?.uid, live?.email)
    }

    private fun canPublish(): Boolean =
        ownerCurrent() && lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)

    private fun requireMutationCurrent(epoch: Long) {
        if (epoch != mutationEpoch || !canPublish()) throw CancellationException("Premium owner or lifecycle changed")
    }

    private suspend fun requirePremiumRight(epoch: Long) {
        requireMutationCurrent(epoch)
        val allowed = AdminPermissionSnapshot.load(
            authorizedEmail, "manage_premium",
            membership = { adminRepo.isAdminFresh(authorizedUid, authorizedEmail) },
            permissions = { adminRepo.getAdminMe() }
        )
        requireMutationCurrent(epoch)
        if (allowed == null) throw SecurityException("Premium management permission is unavailable")
    }

    private fun mutatePremium(successMessage: String, action: suspend (Long) -> Unit) {
        if (!canPublish() || mutationPending) return
        val epoch = mutationEpoch
        mutationPending = true
        ++readGeneration
        binding.progressAdmin.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                requirePremiumRight(epoch)
                requireMutationCurrent(epoch)
                action(epoch)
                requireMutationCurrent(epoch)
                if (canPublish()) {
                    AppBulletin.showSuccess(this@ManagePremiumActivity, successMessage)
                    loadUsers()
                }
            } catch (cancel: CancellationException) {
                throw cancel
            } catch (error: Exception) {
                if (epoch == mutationEpoch && canPublish()) {
                    AppBulletin.showError(this@ManagePremiumActivity, error.toUserFriendlyMessage())
                }
            } finally {
                mutationPending = false
                if (epoch == mutationEpoch && canPublish()) binding.progressAdmin.visibility = View.GONE
            }
        }
    }

    override fun onStop() {
        activeDialog?.dismiss()
        activeDialog = null
        stoppedOnce = true
        reauthorizeOnResume = true
        ++mutationEpoch
        binding.progressAdmin.visibility = View.GONE
        ++readGeneration
        ++transactionsGeneration
        ++usersGeneration
        super.onStop()
    }

    override fun onStart() {
        super.onStart()
        if (adminReady && !ownerCurrent()) {
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        if (reauthorizeOnResume && ::binding.isInitialized) {
            reauthorizeOnResume = false
            val live = FirebaseAuth.getInstance().currentUser
            if (live == null || live.uid != authorizedUid || live.email != authorizedEmail) {
                finish()
                return
            }
            com.eve.app.util.AdminAccessGate.requireAdmin(
                this,
                binding.root,
                requireFreshServer = true,
                requiredRight = "manage_premium"
            ) {
                if (!ownerCurrent()) {
                    finish()
                    return@requireAdmin
                }
                authorizedUid = live.uid
                authorizedEmail = live.email.orEmpty()
                if (stoppedOnce) {
                    stoppedOnce = false
                    loadAllData()
                }
            }
        }
    }

    private val transactionAdapter = AdminTransactionAdapter()
    private lateinit var userAdapter: AdminPremiumUserAdapter

    private var currentConfig: AdminPremiumConfigDto? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityManagePremiumBinding.inflate(layoutInflater)
        setContentView(binding.root)

        com.eve.app.util.AdminAccessGate.requireAdmin(this, binding.root, requireFreshServer = true, requiredRight = "manage_premium") {
        val initiating = FirebaseAuth.getInstance().currentUser ?: return@requireAdmin
        authorizedUid = initiating.uid
        authorizedEmail = initiating.email.orEmpty()
        adminReady = true
        if (!ownerCurrent()) return@requireAdmin

        setupToolbar()
        setupTabs()
        setupRecyclerViews()
        setupSettingsActions()

        loadAllData()
        }
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
        if (!ownerCurrent()) return
        loadConfig()
        loadTransactions()
        loadUsers()
    }

    private fun loadConfig() {
        if (!ownerCurrent() || mutationPending) return
        val request = ++readGeneration
        binding.progressAdmin.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                val response = ApiClient.api.getAdminPremiumConfig()
                if (request != readGeneration || !canPublish()) return@launch
                val config = response.data
                if (!response.success || !AdminPremiumResponseContracts.configValid(config)) {
                    throw IllegalStateException(response.error ?: "Premium settings were not confirmed")
                }
                val confirmed = requireNotNull(config)
                currentConfig = confirmed
                binding.switchEnablePremium.isChecked = confirmed.isEnabled
                binding.etPlanName.setText(confirmed.planName)
                binding.etPriceInr.setText(confirmed.priceInr.toString())
                binding.switchLifetime.isChecked = confirmed.isLifetime
                binding.etDurationDays.setText(confirmed.durationDays.toString())
                binding.tilDurationDays.visibility = if (confirmed.isLifetime) View.GONE else View.VISIBLE
                binding.etDescription.setText(confirmed.description)
                binding.etBenefits.setText(confirmed.benefits.joinToString("\n"))
                binding.switchQrEnabled.isChecked = confirmed.qrEnabled
                binding.switchUpiEnabled.isChecked = confirmed.upiEnabled
                binding.etSessionExpiry.setText(confirmed.sessionExpiryMinutes.toString())
                binding.etMerchantVpa.setText(confirmed.merchantVpa)
            } catch (cancel: CancellationException) {
                throw cancel
            } catch (error: Exception) {
                if (request == readGeneration && canPublish()) {
                    currentConfig = null
                    AppBulletin.showError(this@ManagePremiumActivity, "Failed to load premium config: ${error.toUserFriendlyMessage()}")
                }
            } finally {
                if (request == readGeneration && canPublish()) {
                    binding.progressAdmin.visibility = View.GONE
                }
            }
        }
    }

    private fun saveConfig() {
        if (!canPublish() || mutationPending) return
        val original = currentConfig
        if (original == null) {
            AppBulletin.showError(this, "Load current premium settings before saving.")
            return
        }
        val planName = binding.etPlanName.text?.toString()?.trim().orEmpty()
        val priceInr = binding.etPriceInr.text?.toString()?.trim()?.toIntOrNull()
        val isLifetime = binding.switchLifetime.isChecked
        val durationDays = if (isLifetime) original.durationDays
            else binding.etDurationDays.text?.toString()?.trim()?.toIntOrNull()
        val expiry = binding.etSessionExpiry.text?.toString()?.trim()?.toIntOrNull()
        val merchantVpa = binding.etMerchantVpa.text?.toString()?.trim().orEmpty()
        val description = binding.etDescription.text?.toString()?.trim().orEmpty()
        // Worker replaces empty description with its default: reject rather than report
        // a mismatch after the mutation has already been committed.
        if (planName.isBlank() || planName.length > 100 || description.isBlank() ||
            priceInr == null || priceInr !in 1..1000000 ||
            durationDays == null || durationDays !in 1..3650 ||
            expiry == null || expiry !in 1..1440 || merchantVpa.isBlank()) {
            AppBulletin.showError(this, "Enter valid premium plan, price, duration, payment details and session expiry.")
            return
        }
        val updated = original.copy(
            isEnabled = binding.switchEnablePremium.isChecked,
            planName = planName,
            priceInr = priceInr,
            isLifetime = isLifetime,
            durationDays = durationDays,
            description = description,
            benefits = binding.etBenefits.text?.toString().orEmpty().lines().map { it.trim() }.filter { it.isNotEmpty() },
            qrEnabled = binding.switchQrEnabled.isChecked,
            upiEnabled = binding.switchUpiEnabled.isChecked,
            sessionExpiryMinutes = expiry,
            merchantVpa = merchantVpa
        )
        if (!AdminPremiumResponseContracts.configValid(updated)) {
            AppBulletin.showError(this, "Premium settings are invalid.")
            return
        }
        mutatePremium("Premium settings confirmed by the server.") { epoch ->
            val response = ApiClient.api.updateAdminPremiumConfig(updated)
            requireMutationCurrent(epoch)
            if (!response.success) {
                throw IllegalStateException(response.error ?: "Premium update was rejected")
            }
            // Older Worker versions acknowledge config PUT without a payload.
            // Read the authoritative config back; never publish optimistic success.
            val readback = ApiClient.api.getAdminPremiumConfig()
            if (!readback.success || !AdminPremiumResponseContracts.configMatches(updated, readback.data)) {
                throw IllegalStateException("Premium config was not confirmed. Reload before retrying.")
            }
            requireMutationCurrent(epoch)
            currentConfig = readback.data
        }
    }

    private fun loadTransactions() {
        if (!ownerCurrent()) return
        val request = ++transactionsGeneration
        lifecycleScope.launch {
            try {
                val response = ApiClient.api.getAdminTransactions()
                if (request != transactionsGeneration || !canPublish()) return@launch
                val items = response.data
                if (!response.success || items == null) {
                    throw IllegalStateException(response.error ?: "Transactions unavailable")
                }
                transactionAdapter.submitList(items)
                binding.tvEmptyTransactions.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
            } catch (cancel: CancellationException) {
                throw cancel
            } catch (error: Exception) {
                if (request == transactionsGeneration && canPublish()) {
                    AppBulletin.showError(this@ManagePremiumActivity, "Transactions: ${error.toUserFriendlyMessage()}")
                }
            }
        }
    }

    private fun loadUsers() {
        if (!ownerCurrent()) return
        val request = ++usersGeneration
        lifecycleScope.launch {
            try {
                val response = ApiClient.api.getAdminPremiumUsers()
                if (request != usersGeneration || !canPublish()) return@launch
                val items = response.data
                if (!response.success || items == null) {
                    throw IllegalStateException(response.error ?: "Premium users unavailable")
                }
                userAdapter.submitList(items)
                binding.tvEmptyUsers.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
            } catch (cancel: CancellationException) {
                throw cancel
            } catch (error: Exception) {
                if (request == usersGeneration && canPublish()) {
                    AppBulletin.showError(this@ManagePremiumActivity, "Premium users: ${error.toUserFriendlyMessage()}")
                }
            }
        }
    }

    private fun showManualGrantDialog() {
        if (!canPublish() || mutationPending) return
        val dialogView = LayoutInflater.from(this).inflate(com.eve.app.R.layout.dialog_manual_grant_premium, null)
        val etUser = dialogView.findViewById<EditText>(com.eve.app.R.id.etGrantUser)
        val etDays = dialogView.findViewById<EditText>(com.eve.app.R.id.etGrantDays)
        val switchLifetime = dialogView.findViewById<TelegramSwitch>(com.eve.app.R.id.switchGrantLifetime)

        switchLifetime.setOnCheckedChangeListener { _, isChecked ->
            etDays.visibility = if (isChecked) View.GONE else View.VISIBLE
        }
        val dialog = TelegramNativeAlertDialog.Builder(this)
            .setTitle("Grant Premium Manually")
            .setView(dialogView)
            .setPositiveButton("Grant") { _, _ ->
                if (!canPublish() || mutationPending) return@setPositiveButton
                val target = etUser.text?.toString()?.trim().orEmpty()
                val isLife = switchLifetime.isChecked
                val days = if (isLife) 30 else etDays.text?.toString()?.trim()?.toIntOrNull()
                if (target.isBlank() || days == null || days !in 1..3650) {
                    AppBulletin.showError(this, "Provide a valid user and duration (1 to 3650 days).")
                    return@setPositiveButton
                }
                if (isLife) {
                    val lifeDialog = TelegramNativeAlertDialog.Builder(this@ManagePremiumActivity)
                        .setTitle("Confirm Lifetime Grant")
                        .setMessage("Grant permanent Lifetime Premium to '$target'? This entitlement will never expire.")
                        .setPositiveButton("Grant Lifetime") { _, _ ->
                            if (canPublish() && !mutationPending) executeManualGrant(target, 30, true, true)
                        }
                        .setNegativeButton("Cancel", null)
                        .show()
                    activeDialog = lifeDialog
                    lifeDialog.setOnDismissListener { if (activeDialog === lifeDialog) activeDialog = null }
                } else {
                    executeManualGrant(target, days, false, false)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
        activeDialog = dialog
        dialog.setOnDismissListener { if (activeDialog === dialog) activeDialog = null }
    }

    private fun executeManualGrant(target: String, durationDays: Int, isLifetime: Boolean, confirmLifetime: Boolean) {
        if (!canPublish() || mutationPending || target.isBlank() || durationDays !in 1..3650 ||
            (isLifetime && !confirmLifetime)) return
        mutatePremium("Premium granted to $target") {
            val response = ApiClient.api.grantPremium(
                AdminGrantRequest(
                    userIdOrEmail = target,
                    durationDays = durationDays,
                    isLifetime = isLifetime,
                    planName = "Admin Manual Access",
                    confirmLifetime = confirmLifetime
                )
            )
            if (!AdminPremiumResponseContracts.granted(response, isLifetime)) {
                throw IllegalStateException(response.error ?: "Premium grant was not acknowledged")
            }
        }
    }

    private fun extendUserPremium(user: AdminPremiumUserDto) {
        if (!canPublish() || mutationPending || user.userId.isBlank()) return
        mutatePremium("Premium extended by 30 days") {
            val response = ApiClient.api.extendPremium(AdminExtendRequest(user.userId, 30))
            if (!AdminPremiumResponseContracts.extended(response, user.userId)) {
                throw IllegalStateException(response.error ?: "Premium extension was not acknowledged")
            }
        }
    }

    private fun confirmRevokeUserPremium(user: AdminPremiumUserDto) {
        if (!canPublish() || mutationPending || user.userId.isBlank()) return
        val dialog = TelegramNativeAlertDialog.Builder(this)
            .setTitle("Revoke Premium?")
            .setMessage("Are you sure you want to revoke Premium access for ${user.email.ifBlank { user.userId }}? They will revert to the standard 3-attempt restriction.")
            .setPositiveButton("Revoke") { _, _ ->
                if (!canPublish() || mutationPending) return@setPositiveButton
                mutatePremium("Premium revoked") {
                    val response = ApiClient.api.revokePremium(AdminRevokeRequest(user.userId))
                    if (!AdminPremiumResponseContracts.revoked(response, user.userId)) {
                        throw IllegalStateException(response.error ?: "Premium revocation was not acknowledged")
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .makeRed(DialogInterface.BUTTON_POSITIVE).show()
        activeDialog = dialog
        dialog.setOnDismissListener { if (activeDialog === dialog) activeDialog = null }
    }

    override fun onDestroy() {
        activeDialog?.dismiss()
        activeDialog = null
        super.onDestroy()
    }
}
