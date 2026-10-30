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
import com.eve.app.databinding.ActivityAdminsAndRightsBinding
import com.eve.app.designsystem.TelegramNativeAlertDialog
import com.eve.app.ui.common.EveBaseActivity
import com.eve.app.util.AdminAccessGate
import com.eve.app.util.AppBulletin
import kotlinx.coroutines.launch

class AdminsAndRightsActivity : EveBaseActivity() {

    private lateinit var binding: ActivityAdminsAndRightsBinding
    private val viewModel: AdminViewModel by viewModels()

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

        AdminAccessGate.requireAdmin(this, binding.root, requireFreshServer = true) {
            setupUi()
            setupObservers()
            viewModel.loadAdmins()
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadAdmins()
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
        TelegramNativeAlertDialog.Builder(this)
            .setTitle("Remove Admin?")
            .setMessage("Remove $email? This will revoke admin privileges and cannot be undone.")
            .setPositiveButton("Remove") { _, _ ->
                viewModel.removeAdmin(email)
                AppBulletin.showSuccess(this, "Admin removed")
            }
            .setNegativeButton("Cancel", null)
            .makeRed(DialogInterface.BUTTON_POSITIVE).show()
    }
}
