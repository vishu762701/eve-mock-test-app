package com.eve.app.ui.admin

import com.eve.app.ui.common.EveBaseActivity

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import android.widget.TextView
import android.widget.LinearLayout
import android.view.Gravity
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import androidx.core.content.ContextCompat
import com.eve.app.R
import com.eve.app.designsystem.DesignTokens
import com.eve.app.data.model.AdminUsersPage
import com.eve.app.data.model.AdminUsersPagination
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.CancellationException
import com.google.firebase.auth.FirebaseAuth
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.eve.app.data.model.AdminUser
import com.eve.app.data.repository.AdminRepository
import com.eve.app.databinding.ActivityManageUsersBinding
import com.eve.app.util.AppBulletin
import com.eve.app.util.AdminAccessGate
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.eve.app.data.model.AdminAuditLog
import com.eve.app.data.remote.toUserFriendlyMessage
import com.eve.app.data.repository.AuditLogRepository

class ManageUsersActivity : EveBaseActivity() {

    private lateinit var binding: ActivityManageUsersBinding
    private val adminRepo = AdminRepository()
    private val auditLogRepo = AuditLogRepository()
    private lateinit var adapter: UserAdminAdapter


    private val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
    private var authorizedUid: String? = null
    private var gateReady = false
    private var currentPage = 1
    private var query = ""
    private var restoredQuery = ""
    private var requestJob: Job? = null
    private var debounceJob: Job? = null
    private var actionPending = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        currentPage = (savedInstanceState?.getInt("users_page") ?: 1).coerceAtLeast(1)
        restoredQuery = savedInstanceState?.getString("users_query").orEmpty()
        binding = ActivityManageUsersBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.btnBack.setOnClickListener { finish() }
        AdminAccessGate.requireAdmin(this, binding.root, requireFreshServer = true) {
            authorizedUid = FirebaseAuth.getInstance().currentUser?.uid
            gateReady = true
            setupAdminControls()
            loadUsers()
        }
    }

    override fun onResume() {
        super.onResume()
        // A privilege may have been revoked while this Activity was paused.
        // Never show cached user rows without a fresh server decision.
        if (authorizedUid != null) {
            gateReady = false
            binding.root.visibility = View.INVISIBLE
            requestJob?.cancel()
            AdminAccessGate.requireAdmin(this, binding.root, requireFreshServer = true) {
                authorizedUid = FirebaseAuth.getInstance().currentUser?.uid
                gateReady = true
                loadUsers()
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("users_page", currentPage)
        outState.putString("users_query", binding.etSearchUser.text?.toString().orEmpty())
    }

    private fun setupAdminControls() {
        adapter = UserAdminAdapter(
            onViewDetails = { user -> showUserDetailsDialog(user) },
            onToggleStatus = { user, disable -> confirmToggleUserStatus(user, disable) }
        )
        binding.rvUsers.layoutManager = LinearLayoutManager(this)
        binding.rvUsers.adapter = adapter
        binding.btnRefresh.setOnClickListener {
            debounceJob?.cancel()
            loadUsers()
        }
        binding.btnRetryUsers.setOnClickListener {
            debounceJob?.cancel()
            loadUsers()
        }
        binding.btnPagePrevious.setOnClickListener { navigateTo(currentPage - 1) }
        binding.btnPageNext.setOnClickListener { navigateTo(currentPage + 1) }
        query = restoredQuery.trim()
        binding.etSearchUser.setText(restoredQuery)
        binding.etSearchUser.setSelection(binding.etSearchUser.length())
        binding.etSearchUser.doAfterTextChanged { text ->
            debounceJob?.cancel()
            val next = text?.toString()?.trim().orEmpty()
            if (next != query) {
                query = next
                currentPage = 1
                requestJob?.cancel()
                binding.rvUsers.visibility = View.GONE
                binding.layoutPagination.visibility = View.GONE
                binding.progressBar.visibility = View.VISIBLE
                debounceJob = lifecycleScope.launch {
                    delay(350)
                    loadUsers()
                }
            }
        }
    }

    private fun navigateTo(next: Int) {
        if (next < 1 || next == currentPage || requestJob?.isActive == true) return
        currentPage = next
        loadUsers()
    }

    private fun loadUsers() {
        if (!gateReady || authorizedUid == null) return
        requestJob?.cancel()
        binding.progressBar.visibility = View.VISIBLE
        binding.rvUsers.visibility = View.GONE
        binding.emptyGroup.visibility = View.GONE
        // Keep the previous page window attached while loading so the shared
        // mark can spring from the old page to the server-confirmed new page.
        binding.btnRefresh.isEnabled = false
        val pageAtRequest = currentPage
        val queryAtRequest = query
        requestJob = lifecycleScope.launch {
            try {
                val response: AdminUsersPage = adminRepo.getUsersPage(queryAtRequest, pageAtRequest)
                if (!gateReady || authorizedUid != FirebaseAuth.getInstance().currentUser?.uid ||
                    pageAtRequest != currentPage || queryAtRequest != query) return@launch
                val meta = response.pagination
                if (meta.totalPages > 0 && currentPage > meta.totalPages) {
                    currentPage = meta.totalPages
                    loadUsers()
                    return@launch
                }
                adapter.submitList(response.users)
                binding.progressBar.visibility = View.GONE
                binding.btnRefresh.isEnabled = true
                binding.btnRetryUsers.visibility = View.GONE
                if (response.users.isEmpty()) {
                    binding.emptyGroup.visibility = View.VISIBLE
                    binding.tvEmptyTitle.text = "No students found"
                    binding.tvEmptyMessage.text = if (meta.total == 0)
                        "No registered users match your search." else "No users on this page."
                } else binding.rvUsers.visibility = View.VISIBLE
                renderPagination(meta, response.users.size)
            } catch (cancel: CancellationException) {
                throw cancel
            } catch (e: Exception) {
                binding.progressBar.visibility = View.GONE
                binding.btnRefresh.isEnabled = true
                binding.emptyGroup.visibility = View.VISIBLE
                binding.tvEmptyTitle.text = "Couldn't load users"
                binding.tvEmptyMessage.text = e.toUserFriendlyMessage()
                binding.layoutPagination.visibility = View.GONE
                binding.btnRetryUsers.visibility = View.VISIBLE
            }
        }
    }

    private fun renderPagination(meta: AdminUsersPagination, count: Int) {
        val start = if (count == 0) 0L else (meta.page.toLong() - 1L) * meta.pageSize + 1
        val end = if (count == 0) 0L else start + count - 1
        binding.tvPageSummary.text = "Showing " + start + "–" + end + " of " + meta.total
        binding.layoutPagination.visibility = if (meta.total > 0) View.VISIBLE else View.GONE
        binding.layoutPageControls.visibility = if (meta.totalPages > 1) View.VISIBLE else View.GONE
        binding.btnPagePrevious.isEnabled = meta.hasPrevious
        binding.btnPageNext.isEnabled = meta.hasNext
        binding.btnPagePrevious.alpha = if (meta.hasPrevious) 1f else 0.4f
        binding.btnPageNext.alpha = if (meta.hasNext) 1f else 0.4f
        binding.layoutPageNumbers.bind(meta.page, meta.totalPages) { page ->
            navigateTo(page)
        }
        // Five accessible 48dp targets can exceed 320dp. Keep the current
        // mark in the narrow scroll viewport after the server confirms a page.
        binding.layoutPageNumbers.post {
            val viewport = binding.layoutPageNumbers.parent as? android.widget.HorizontalScrollView
                ?: return@post
            val pages = AdminPageWindow.pages(meta.page, meta.totalPages).toList()
            val index = pages.indexOf(meta.page)
            if (index < 0) return@post
            val rtl = binding.layoutPageNumbers.layoutDirection == View.LAYOUT_DIRECTION_RTL
            val logicalIndex = if (rtl) pages.lastIndex - index else index
            val step = (48f * resources.displayMetrics.density).toInt()
            val centered = ((logicalIndex + 0.5f) * step - viewport.width / 2f)
                .toInt().coerceAtLeast(0)
            if (com.eve.app.designsystem.UiArcMotion.isMotionEnabled(this)) {
                viewport.smoothScrollTo(centered, 0)
            } else viewport.scrollTo(centered, 0)
        }
    }

    private fun showUserDetailsDialog(user: AdminUser) {
        if (!gateReady || authorizedUid == null) return
        binding.progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                val attempts = adminRepo.getUserAttempts(user.id)
                if (!gateReady || authorizedUid != FirebaseAuth.getInstance().currentUser?.uid ||
                    !lifecycle.currentState.isAtLeast(androidx.lifecycle.Lifecycle.State.RESUMED)) return@launch
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
                AppBulletin.showError(this@ManageUsersActivity, "Failed to load attempts: ${e.toUserFriendlyMessage()}")
            }
        }
    }

    private fun confirmToggleUserStatus(user: AdminUser, disable: Boolean) {
        if (actionPending || !gateReady || authorizedUid == null) return
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
                if (actionPending || !gateReady ||
                    authorizedUid != FirebaseAuth.getInstance().currentUser?.uid) return@setPositiveButton
                actionPending = true
                binding.progressBar.visibility = View.VISIBLE
                lifecycleScope.launch {
                    try {
                        val success = adminRepo.toggleUserStatus(user.id, disable)
                        binding.progressBar.visibility = View.GONE
                        if (success) {
                            // The server mutation is authoritative. Refresh even if an
                            // independent audit-log write fails after the account changed.
                            loadUsers()
                            try {
                                auditLogRepo.recordLog(
                                    AdminAuditLog.ACTION_USER_STATUS_TOGGLED,
                                    if (disable) "Disabled/banned user: ${user.displayName} (${user.email})" else "Enabled user: ${user.displayName} (${user.email})"
                                )
                            } catch (cancel: CancellationException) {
                                throw cancel
                            } catch (auditError: Exception) {
                                android.util.Log.e("ManageUsers", "Account changed but audit log failed", auditError)
                                AppBulletin.showError(this@ManageUsersActivity, "Account updated; audit log unavailable")
                            }
                            val msg = if (disable) "Account disabled successfully" else "Account enabled successfully"
                            AppBulletin.showSuccess(this@ManageUsersActivity, msg)
                        } else {
                            AppBulletin.showError(this@ManageUsersActivity, "Failed to update account status")
                        }
                    } catch (cancel: CancellationException) {
                        throw cancel
                    } catch (e: Exception) {
                        binding.progressBar.visibility = View.GONE
                        AppBulletin.showError(this@ManageUsersActivity, "Error: ${e.toUserFriendlyMessage()}")
                    } finally {
                        actionPending = false
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
