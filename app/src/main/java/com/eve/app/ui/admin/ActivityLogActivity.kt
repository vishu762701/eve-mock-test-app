package com.eve.app.ui.admin

import android.content.Context
import android.os.Bundle
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import com.eve.app.designsystem.TelegramNativeAlertDialog
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.eve.app.R
import com.eve.app.data.model.AdminAuditLog
import com.eve.app.data.repository.AdminRepository
import com.eve.app.data.repository.AuditLogRepository
import com.eve.app.data.repository.AuditLogTimeRange
import com.eve.app.databinding.ActivityActivityLogBinding
import com.eve.app.theme.TelegramThemeEngine
import com.eve.app.ui.common.EveBaseActivity
import com.eve.app.util.AdminAccessGate
import com.eve.app.util.AppBulletin
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.auth.FirebaseAuth
import androidx.lifecycle.Lifecycle
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ActivityLogActivity : EveBaseActivity() {

    private lateinit var binding: ActivityActivityLogBinding
    private val adminRepo = AdminRepository()
    private val auditRepo = AuditLogRepository()
    private lateinit var adapter: AuditLogAdapter

    private var currentTimeRange = AuditLogTimeRange.ALL_TIME
    private var selectedAdminEmail: String? = null
    private var selectedActionType: String? = null
    private var currentSearchQuery: String? = null

    private val loadedLogs = mutableListOf<AdminAuditLog>()
    private var isLoading = false
    private var hasMore = true
    private var searchJob: Job? = null
    private var auditJob: Job? = null
    private var auditEpoch = 0L
    private var activeUid: String? = null
    private var activeEmail: String? = null
    private var accessReady = false
    private var reauthorizeOnResume = false
    private var viewsPrepared = false
    private var filterDialog: BottomSheetDialog? = null
    private var filterAdminJob: Job? = null
    private var filterTypeJob: Job? = null
    private var logDialog: TelegramNativeAlertDialog? = null

    private fun isCurrentOwner(
        uid: String? = activeUid, email: String? = activeEmail
    ): Boolean {
        val user = FirebaseAuth.getInstance().currentUser
        return accessReady && !reauthorizeOnResume && !isFinishing && !isDestroyed &&
            !uid.isNullOrBlank() && !email.isNullOrBlank() &&
            activeUid == uid && activeEmail.equals(email, ignoreCase = true) &&
            user?.uid == uid && user?.email.equals(email, ignoreCase = true)
    }

    private fun cancelAuditRead() {
        auditEpoch++
        auditJob?.cancel()
        auditJob = null
        isLoading = false
        binding.progressBar.visibility = View.GONE
    }

    private fun requireAuditAccess() {
        AdminAccessGate.requireAdmin(this, binding.root, requireFreshServer = true,
            requiredRight = "view_analytics_logs") {
            if (reauthorizeOnResume || isFinishing || isDestroyed) {
                return@requireAdmin
            }
            val user = FirebaseAuth.getInstance().currentUser
            if (user == null || (activeUid != null && (user.uid != activeUid || !user.email.equals(activeEmail, ignoreCase = true)))) {
                binding.root.visibility = View.INVISIBLE
                finish()
                return@requireAdmin
            }
            activeUid = user.uid
            activeEmail = user.email
            accessReady = true
            binding.progressBar.visibility = View.GONE
            binding.rvAuditLogs.visibility = View.VISIBLE
            if (!viewsPrepared) {
                setupViews()
                viewsPrepared = true
            }
            reloadLogs()
        }
    }

    override fun onResume() {
        super.onResume()
        if (reauthorizeOnResume && ::binding.isInitialized) {
            reauthorizeOnResume = false
            val live = FirebaseAuth.getInstance().currentUser
            if (live == null || live.uid != activeUid || !live.email.equals(activeEmail, ignoreCase = true)) {
                finish()
                return
            }
            requireAuditAccess()
        }
    }

    override fun onStop() {
        reauthorizeOnResume = true
        accessReady = false
        searchJob?.cancel()
        cancelAuditRead()
        filterAdminJob?.cancel()
        filterTypeJob?.cancel()
        filterDialog?.dismiss()
        filterDialog = null
        logDialog?.dismiss()
        logDialog = null
        super.onStop()
    }

    private val fullDateTimeFormat = SimpleDateFormat("dd MMM yyyy, hh:mm:ss a", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null || user.email.isNullOrBlank()) {
            finish()
            return
        }
        activeUid = user.uid
        activeEmail = user.email
        binding = ActivityActivityLogBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.actionBar.setTitle("Recent Actions")
        binding.actionBar.setSubtitle("Audit log trail")
        binding.actionBar.onBackPressed = { finish() }
        binding.actionBar.bindSystemBack(this)
        binding.progressBar.visibility = View.VISIBLE
        binding.rvAuditLogs.visibility = View.GONE

        requireAuditAccess()
    }

    private fun setupViews() {
        // 1. ActionBar
        binding.actionBar.setTitle("Recent Actions")
        binding.actionBar.setSubtitle("Audit log trail")
        binding.actionBar.onBackPressed = { finish() }
        binding.actionBar.bindSystemBack(this)
        binding.actionBar.showFilterAction { if (isCurrentOwner()) showFilterSheet() }

        binding.actionBar.onSearchTextChanged = { query ->
            searchJob?.cancel()
            if (isCurrentOwner()) {
                // Invalidate immediately, before debounce, so a previous response
                // can never be published beneath newly typed filter text.
                cancelAuditRead()
                loadedLogs.clear()
                adapter.submitLogs(emptyList())
                binding.emptyContainer.visibility = View.GONE
                binding.rvAuditLogs.visibility = View.GONE
                binding.progressBar.visibility = View.VISIBLE
                searchJob = lifecycleScope.launch {
                    delay(300L)
                    if (!isCurrentOwner()) return@launch
                    currentSearchQuery = query.takeIf { it.isNotBlank() }
                    reloadLogs()
                }
            }
        }

        binding.actionBar.onSearchCollapsed = {
            searchJob?.cancel()
            currentSearchQuery = null
            reloadLogs()
        }

        // 2. Segmented Range Tabs
        binding.tabsRange.setTabs(listOf("Today", "Last 7 days", "All time"), defaultIndex = 2)
        binding.tabsRange.onTabSelectedListener = { index ->
            currentTimeRange = when (index) {
                0 -> AuditLogTimeRange.TODAY
                1 -> AuditLogTimeRange.LAST_7_DAYS
                else -> AuditLogTimeRange.ALL_TIME
            }
            reloadLogs()
        }

        // 3. RecyclerView & Adapter
        adapter = AuditLogAdapter { log ->
            if (isCurrentOwner() && loadedLogs.any { it.id == log.id && it.id.isNotBlank() }) {
                showLogDetailDialog(log)
            }
        }
        val layoutManager = LinearLayoutManager(this)
        binding.rvAuditLogs.layoutManager = layoutManager
        binding.rvAuditLogs.adapter = adapter

        binding.rvAuditLogs.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (dy > 0 && isCurrentOwner() && !isLoading && hasMore) {
                    val totalItemCount = layoutManager.itemCount
                    val lastVisibleItem = layoutManager.findLastVisibleItemPosition()
                    if (totalItemCount > 0 && lastVisibleItem >= totalItemCount - 4) {
                        loadNextPage()
                    }
                }
            }
        })
    }

    private fun reloadLogs() {
        if (!isCurrentOwner()) return
        cancelAuditRead()
        loadedLogs.clear()
        if (::adapter.isInitialized) adapter.submitLogs(emptyList())
        logDialog?.dismiss()
        logDialog = null
        hasMore = true
        binding.emptyContainer.visibility = View.GONE
        binding.rvAuditLogs.visibility = View.VISIBLE
        updateFilterBadge()
        loadPage(before = null)
    }

    private fun loadNextPage() {
        val oldestTimestamp = loadedLogs.lastOrNull()?.timestamp ?: return
        loadPage(before = oldestTimestamp)
    }

    private fun loadPage(before: Long?) {
        if (isLoading || !isCurrentOwner()) return
        val uid = activeUid ?: return
        val email = activeEmail ?: return
        val request = AuditReadWindow(
            uid, email, ++auditEpoch, currentTimeRange, selectedAdminEmail,
            selectedActionType, currentSearchQuery, before
        )
        fun stillCurrent() = isCurrentOwner(uid, email) &&
            request.matches(activeUid, activeEmail, auditEpoch, currentTimeRange,
                selectedAdminEmail, selectedActionType, currentSearchQuery)

        isLoading = true
        if (before == null) {
            binding.progressBar.visibility = View.VISIBLE
            binding.emptyContainer.visibility = View.GONE
        }

        auditJob = lifecycleScope.launch {
            try {
                val newLogs = auditRepo.getLogs(
                    timeRange = request.range,
                    before = request.before,
                    adminEmail = request.adminEmail,
                    actionType = request.actionType,
                    searchQuery = request.searchQuery,
                    limit = 50
                )
                if (!stillCurrent()) return@launch
                hasMore = newLogs.size >= 50
                if (before == null) loadedLogs.clear()
                loadedLogs.addAll(newLogs)
                adapter.submitLogs(loadedLogs)
                binding.rvAuditLogs.visibility = View.VISIBLE
                binding.tvEmptyTitle.text = "No Recent Actions"
                binding.tvEmptySubtitle.text = "No activity logs match the selected filters."
                binding.lottieEmpty.visibility = View.VISIBLE
                binding.emptyContainer.visibility = if (loadedLogs.isEmpty()) View.VISIBLE else View.GONE
            } catch (cancel: CancellationException) {
                throw cancel
            } catch (e: Exception) {
                if (!stillCurrent()) return@launch
                hasMore = false
                if (before == null) {
                    if (loadedLogs.isNotEmpty()) {
                        binding.rvAuditLogs.visibility = View.VISIBLE
                        binding.emptyContainer.visibility = View.GONE
                    } else {
                        binding.rvAuditLogs.visibility = View.GONE
                        binding.emptyContainer.visibility = View.VISIBLE
                        binding.tvEmptyTitle.text = "Audit history unavailable"
                        binding.tvEmptySubtitle.text = "The server did not confirm this audit read. Change filters to retry."
                        binding.lottieEmpty.visibility = View.GONE
                    }
                }
                AppBulletin.showError(this@ActivityLogActivity, "Failed to load recent actions: ${e.message}")
            } finally {
                if (stillCurrent()) {
                    isLoading = false
                    binding.progressBar.visibility = View.GONE
                    auditJob = null
                }
            }
        }
    }

    private fun updateFilterBadge() {
        val filters = mutableListOf<String>()
        selectedAdminEmail?.let { filters.add("Admin: $it") }
        selectedActionType?.let { filters.add("Type: $it") }

        if (filters.isNotEmpty()) {
            binding.tvActiveFilters.visibility = View.VISIBLE
            binding.tvActiveFilters.text = "Filtered by: " + filters.joinToString(" • ")
        } else {
            binding.tvActiveFilters.visibility = View.GONE
        }
    }

    private fun showFilterSheet() {
        if (!isCurrentOwner()) return
        val ownerUid = activeUid ?: return
        val ownerEmail = activeEmail ?: return
        filterDialog?.dismiss()
        val dialog = BottomSheetDialog(this, R.style.Theme_Eve_BottomSheetDialog)
        filterDialog = dialog
        fun validFilter(): Boolean =
            isCurrentOwner(ownerUid, ownerEmail) && filterDialog === dialog && dialog.isShowing
        dialog.setOnDismissListener {
            if (filterDialog === dialog) filterDialog = null
            filterAdminJob?.cancel()
            filterTypeJob?.cancel()
        }
        val palette = TelegramThemeEngine.resolveCurrentPalette(this)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20f), dp(10f), dp(20f), dp(24f))
            setBackgroundColor(palette.surface)
        }

        val pillHandle = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(dp(36f), dp(4f)).apply {
                gravity = android.view.Gravity.CENTER_HORIZONTAL
                bottomMargin = dp(14f)
            }
            setBackgroundResource(R.drawable.design_pill_surface)
        }
        root.addView(pillHandle)

        val titleTv = TextView(this).apply {
            text = "Filter Recent Actions"
            textSize = 20f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(palette.textPrimary)
        }
        root.addView(titleTv)

        // Admin Filter Section
        val adminSectionTv = TextView(this).apply {
            text = "ADMIN"
            textSize = 13f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(palette.accent)
            setPadding(0, dp(16f), 0, dp(8f))
        }
        root.addView(adminSectionTv)

        val btnAllAdmins = com.eve.app.ui.admin.tg.TextCell(this).apply {
            setShowArrow(false)
            val isSelected = (selectedAdminEmail == null)
            setTextAndValue("All Admins", if (isSelected) "✓" else "", false)
            if (isSelected) setValueColor(palette.accent)
            setOnClickListener {
                if (!validFilter()) return@setOnClickListener
                selectedAdminEmail = null
                dialog.dismiss()
                reloadLogs()
            }
        }
        root.addView(btnAllAdmins)

        filterAdminJob = lifecycleScope.launch {
            try {
                val admins = adminRepo.getDynamicAdmins()
                if (!validFilter()) return@launch
                for (admin in admins) {
                    val isSelected = selectedAdminEmail.equals(admin.email, ignoreCase = true)
                    val cell = com.eve.app.ui.admin.tg.TextCell(this@ActivityLogActivity).apply {
                        setShowArrow(false)
                        setTextAndValue(admin.email, if (isSelected) "✓" else "", false)
                        if (isSelected) setValueColor(palette.accent)
                        setOnClickListener {
                            if (!validFilter()) return@setOnClickListener
                            selectedAdminEmail = admin.email
                            dialog.dismiss()
                            reloadLogs()
                        }
                    }
                    root.addView(cell)
                }
            } catch (cancel: CancellationException) {
                throw cancel
            } catch (e: Exception) {
                if (validFilter()) AppBulletin.showError(this@ActivityLogActivity, "Admin filter unavailable: ${e.message}")
            }
        }

        // Action Type Section
        val typeSectionTv = TextView(this).apply {
            text = "ACTION TYPE"
            textSize = 13f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(palette.accent)
            setPadding(0, dp(16f), 0, dp(8f))
        }
        root.addView(typeSectionTv)

        val btnAllTypes = com.eve.app.ui.admin.tg.TextCell(this).apply {
            setShowArrow(false)
            val isSelected = (selectedActionType == null)
            setTextAndValue("All Actions", if (isSelected) "✓" else "", false)
            if (isSelected) setValueColor(palette.accent)
            setOnClickListener {
                if (!validFilter()) return@setOnClickListener
                selectedActionType = null
                dialog.dismiss()
                reloadLogs()
            }
        }
        root.addView(btnAllTypes)

        filterTypeJob = lifecycleScope.launch {
            try {
                val types = auditRepo.getActionTypes()
                if (!validFilter()) return@launch
                for (type in types) {
                    val isSelected = selectedActionType.equals(type, ignoreCase = true)
                    val cell = com.eve.app.ui.admin.tg.TextCell(this@ActivityLogActivity).apply {
                        setShowArrow(false)
                        setTextAndValue(type, if (isSelected) "✓" else "", false)
                        if (isSelected) setValueColor(palette.accent)
                        setOnClickListener {
                            if (!validFilter()) return@setOnClickListener
                            selectedActionType = type
                            dialog.dismiss()
                            reloadLogs()
                        }
                    }
                    root.addView(cell)
                }
            } catch (cancel: CancellationException) {
                throw cancel
            } catch (e: Exception) {
                if (validFilter()) AppBulletin.showError(this@ActivityLogActivity, "Action type filter unavailable: ${e.message}")
            }
        }

        dialog.setContentView(root)
        com.eve.app.ui.home.TelegramBottomSheetSourceBackdrop.install(dialog, this)
        dialog.show()
    }

    private fun showLogDetailDialog(log: AdminAuditLog) {
        if (!isCurrentOwner() || loadedLogs.none { it.id == log.id && it.id.isNotBlank() }) return
        val palette = TelegramThemeEngine.resolveCurrentPalette(this)

        val msg = StringBuilder()
        msg.append("Action: ").append(log.actionType).append("\n\n")
        msg.append("Description: ").append(log.description).append("\n\n")
        msg.append("Admin: ").append(log.adminEmail.ifBlank { "Unknown" }).append("\n")
        if (!log.actorUid.isNullOrBlank()) {
            msg.append("Actor UID: ").append(log.actorUid).append("\n")
        }
        if (!log.targetType.isNullOrBlank() || !log.targetId.isNullOrBlank()) {
            msg.append("Target: ").append(log.targetType ?: "N/A").append(" -> ").append(log.targetId ?: "N/A").append("\n")
        }
        msg.append("Source: ").append(log.source).append("\n")
        if (log.timestamp > 0L) {
            msg.append("Time: ").append(fullDateTimeFormat.format(Date(log.timestamp))).append("\n")
        }

        if (log.metadata != null && log.metadata.isNotEmpty()) {
            msg.append("\nMetadata:\n")
            for ((k, v) in log.metadata) {
                msg.append("• ").append(k).append(": ").append(v).append("\n")
            }
        }

        logDialog?.dismiss()
        val dialog = TelegramNativeAlertDialog.Builder(this)
            .setTitle(log.actionBadge)
            .setMessage(msg.toString().trimEnd())
            .setPositiveButton("Close", null)
            .show()
        logDialog = dialog
        dialog.setOnDismissListener {
            if (logDialog === dialog) logDialog = null
        }
    }

    private fun dp(v: Float): Int = (v * resources.displayMetrics.density).toInt()

    override fun onDestroy() {
        searchJob?.cancel()
        searchJob = null
        cancelAuditRead()
        filterAdminJob?.cancel()
        filterAdminJob = null
        filterTypeJob?.cancel()
        filterTypeJob = null
        filterDialog?.dismiss()
        filterDialog = null
        logDialog?.dismiss()
        logDialog = null
        loadedLogs.clear()
        super.onDestroy()
    }
}
