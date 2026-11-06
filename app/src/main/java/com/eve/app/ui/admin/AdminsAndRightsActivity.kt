package com.eve.app.ui.admin

import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.eve.app.data.remote.DynamicAdminItem
import com.eve.app.data.remote.toUserFriendlyMessage
import com.eve.app.databinding.ActivityAdminsAndRightsBinding
import com.eve.app.designsystem.TelegramNativeAlertDialog
import com.eve.app.ui.common.EveBaseActivity
import com.eve.app.util.AdminAccessGate
import com.eve.app.util.AppBulletin
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class AdminsAndRightsActivity : EveBaseActivity() {

    private lateinit var binding: ActivityAdminsAndRightsBinding
    private val viewModel: AdminViewModel by viewModels()
    private var authorized: Boolean = false
    private var authorizedUid: String = ""
    private var authorizedEmail: String = ""
    private var reauthorizeOnResume: Boolean = false
    private var removeAdminDialog: TelegramNativeAlertDialog? = null

    private val adminEmailAdapter = AdminEmailAdapter(
        onItemClick = { admin ->
            startActivity(AdminRightsEditActivity.createIntent(this, admin, canEdit = true))
        },
        onRemove = { admin -> confirmRemoveAdmin(admin) }
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminsAndRightsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.telegramActionBar.setTitle("Admins and Rights")
        binding.telegramActionBar.onBackPressed = { finish() }

        AdminAccessGate.requireAdmin(this, binding.root, requireFreshServer = true, requiredRight = "manage_admins_rights") {
            val user = FirebaseAuth.getInstance().currentUser ?: return@requireAdmin
            authorizedUid = user.uid
            authorizedEmail = user.email.orEmpty()
            authorized = true
            setupUi()
            setupObservers()
            viewModel.loadAdmins()
        }
    }

    override fun onStop() {
        removeAdminDialog?.dismiss()
        removeAdminDialog = null
        reauthorizeOnResume = true
        super.onStop()
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
            AdminAccessGate.requireAdmin(
                this,
                binding.root,
                requireFreshServer = true,
                requiredRight = "manage_admins_rights"
            ) {
                if (!authorized) {
                    finish()
                    return@requireAdmin
                }
                authorizedUid = live.uid
                authorizedEmail = live.email.orEmpty()
                if (authorized) viewModel.loadAdmins()
            }
        }
    }

    private fun setupUi() {
        binding.rvAdmins.layoutManager = LinearLayoutManager(this)
        binding.rvAdmins.adapter = adminEmailAdapter

        binding.btnAddAdmin.setOnClickListener { addAdmin() }
    }

    private fun setupObservers() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.admins.collect { adminList ->
                    if (adminList.isEmpty()) {
                        binding.tvNoAdmins.visibility = View.VISIBLE
                        adminEmailAdapter.submit(emptyList())
                    } else {
                        binding.tvNoAdmins.visibility = View.GONE
                        adminEmailAdapter.submit(adminList)
                    }
                }
            }
        }
    }

    private fun addAdmin() {
        val email = binding.etAdminEmail.text.toString().trim()
        if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            AppBulletin.showError(this, "Please enter a valid email address")
            return
        }
        viewModel.addAdmin(email) {
            binding.etAdminEmail.text?.clear()
            AppBulletin.showSuccess(this, "Admin added successfully! Tap the admin row to configure permissions.")
        }
    }

    private fun confirmRemoveAdmin(admin: DynamicAdminItem) {
        val email = admin.email
        removeAdminDialog?.dismiss()
        val dialog = TelegramNativeAlertDialog.Builder(this)
            .setTitle("Remove Admin?")
            .setMessage("Remove $email? This will revoke admin privileges and cannot be undone.")
            .setPositiveButton("Remove") { _, _ ->
                viewModel.removeAdmin(email) { result ->
                    result.onSuccess {
                        AppBulletin.showSuccess(this, "Admin removed")
                    }.onFailure {
                        AppBulletin.showError(this, it.toUserFriendlyMessage())
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .makeRed(DialogInterface.BUTTON_POSITIVE).show()
        removeAdminDialog = dialog
        dialog.setOnDismissListener { if (removeAdminDialog === dialog) removeAdminDialog = null }
    }

    override fun onDestroy() {
        removeAdminDialog?.dismiss()
        removeAdminDialog = null
        super.onDestroy()
    }
}
