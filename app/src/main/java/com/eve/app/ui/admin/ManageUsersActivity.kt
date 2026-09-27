package com.eve.app.ui.admin

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.eve.app.data.model.AdminUser
import com.eve.app.data.repository.AdminRepository
import com.eve.app.databinding.ActivityManageUsersBinding
import com.eve.app.util.AppBulletin
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.eve.app.data.model.AdminAuditLog
import com.eve.app.data.repository.AuditLogRepository

class ManageUsersActivity : AppCompatActivity() {

    private lateinit var binding: ActivityManageUsersBinding
    private val adminRepo = AdminRepository()
    private val auditLogRepo = AuditLogRepository()
    private lateinit var adapter: UserAdminAdapter

    private var allUsers: List<AdminUser> = emptyList()
    private val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityManageUsersBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }
        binding.btnRefresh.setOnClickListener { loadUsers() }

        adapter = UserAdminAdapter(
            onViewDetails = { user -> showUserDetailsDialog(user) },
            onToggleStatus = { user, disable -> confirmToggleUserStatus(user, disable) }
        )

        binding.rvUsers.layoutManager = LinearLayoutManager(this)
        binding.rvUsers.adapter = adapter

        binding.etSearchUser.doAfterTextChanged { text ->
            filterUsers(text?.toString().orEmpty())
        }

        loadUsers()
    }

    private fun loadUsers() {
        binding.progressBar.visibility = View.VISIBLE
        binding.emptyGroup.visibility = View.GONE

        lifecycleScope.launch {
            try {
                val users = adminRepo.getUsers()
                allUsers = users
                binding.progressBar.visibility = View.GONE
                filterUsers(binding.etSearchUser.text?.toString().orEmpty())
            } catch (e: Exception) {
                binding.progressBar.visibility = View.GONE
                AppBulletin.showError(this@ManageUsersActivity, "Failed to load users: ${e.message}")
            }
        }
    }

    private fun filterUsers(query: String) {
        val q = query.trim().lowercase()
        val filtered = if (q.isBlank()) {
            allUsers
        } else {
            allUsers.filter {
                it.displayName.lowercase().contains(q) ||
                        it.email.lowercase().contains(q)
            }
        }

        adapter.submitList(filtered)
        if (filtered.isEmpty()) {
            binding.emptyGroup.visibility = View.VISIBLE
            binding.rvUsers.visibility = View.GONE
        } else {
            binding.emptyGroup.visibility = View.GONE
            binding.rvUsers.visibility = View.VISIBLE
        }
    }

    private fun showUserDetailsDialog(user: AdminUser) {
        binding.progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                val attempts = adminRepo.getUserAttempts(user.id)
                binding.progressBar.visibility = View.GONE

                val attemptsSummary = if (attempts.isEmpty()) {
                    "No test attempts yet."
                } else {
                    val total = attempts.size
                    val avgScore = attempts.map { it.score }.average()
                    val recent = attempts.take(6).joinToString("\n") { att ->
                        val dateStr = if (att.timestamp > 0) dateFormat.format(Date(att.timestamp)) else ""
                        "• ${att.examName}: ${att.score}/${att.total} (${att.correct} correct)  $dateStr"
                    }
                    "Total Tests Attempted: $total\nAverage Score: ${String.format(Locale.US, "%.1f", avgScore)}\n\nRecent Attempts:\n$recent"
                }

                val joined = if (user.createdAt > 0) dateFormat.format(Date(user.createdAt)) else "Unknown"
                val active = if (user.lastActive > 0) dateFormat.format(Date(user.lastActive)) else "Never"

                val message = """
                    Email: ${user.email.ifBlank { "N/A" }}
                    Category: ${user.category}
                    Joined: $joined
                    Last Active: $active
                    Status: ${if (user.disabled) "Banned / Disabled" else "Active"}

                    ══════════════════════
                    $attemptsSummary
                """.trimIndent()

                MaterialAlertDialogBuilder(this@ManageUsersActivity)
                    .setTitle(user.displayName.ifBlank { "Student Details" })
                    .setMessage(message)
                    .setPositiveButton("Close", null)
                    .show()
            } catch (e: Exception) {
                binding.progressBar.visibility = View.GONE
                AppBulletin.showError(this@ManageUsersActivity, "Failed to load attempts: ${e.message}")
            }
        }
    }

    private fun confirmToggleUserStatus(user: AdminUser, disable: Boolean) {
        val actionTitle = if (disable) "Disable / Ban User Account?" else "Enable User Account?"
        val actionWarning = if (disable) {
            "Are you sure? This user will be blocked from accessing and submitting mock tests in the app.\n\nUser: ${user.displayName} (${user.email})"
        } else {
            "Enable and restore full access for ${user.displayName} (${user.email})?"
        }

        MaterialAlertDialogBuilder(this)
            .setTitle(actionTitle)
            .setMessage(actionWarning)
            .setPositiveButton(if (disable) "Disable Account" else "Enable Account") { _, _ ->
                binding.progressBar.visibility = View.VISIBLE
                lifecycleScope.launch {
                    try {
                        val success = adminRepo.toggleUserStatus(user.id, disable)
                        binding.progressBar.visibility = View.GONE
                        if (success) {
                            auditLogRepo.recordLog(
                                AdminAuditLog.ACTION_USER_STATUS_TOGGLED,
                                if (disable) "Disabled/banned user: ${user.displayName} (${user.email})" else "Enabled user: ${user.displayName} (${user.email})"
                            )
                            val msg = if (disable) "Account disabled successfully" else "Account enabled successfully"
                            AppBulletin.showSuccess(this@ManageUsersActivity, msg)
                            allUsers = allUsers.map {
                                if (it.id == user.id) it.copy(disabled = disable) else it
                            }
                            filterUsers(binding.etSearchUser.text?.toString().orEmpty())
                        } else {
                            AppBulletin.showError(this@ManageUsersActivity, "Failed to update account status")
                        }
                    } catch (e: Exception) {
                        binding.progressBar.visibility = View.GONE
                        AppBulletin.showError(this@ManageUsersActivity, "Error: ${e.message}")
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
