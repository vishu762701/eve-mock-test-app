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

    private val fullDateTimeFormat = SimpleDateFormat("dd MMM yyyy, hh:mm:ss a", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityActivityLogBinding.inflate(layoutInflater)
        setContentView(binding.root)

        AdminAccessGate.requireAdmin(this, binding.root, requireFreshServer = true) {
            setupViews()
            reloadLogs()
        }
    }

    private fun setupViews() {
        // 1. ActionBar
        binding.actionBar.setTitle("Recent Actions")
        binding.actionBar.setSubtitle("Audit log trail")
        binding.actionBar.onBackPressed = { finish() }
        binding.actionBar.bindSystemBack(this)
        binding.actionBar.showFilterAction { showFilterSheet() }

        binding.actionBar.onSearchTextChanged = { query ->
            searchJob?.cancel()
            searchJob = lifecycleScope.launch {
                delay(300L)
                currentSearchQuery = query.takeIf { it.isNotBlank() }
                reloadLogs()
            }
        }

        binding.actionBar.onSearchCollapsed = {
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
            showLogDetailDialog(log)
        }
        val layoutManager = LinearLayoutManager(this)
        binding.rvAuditLogs.layoutManager = layoutManager
        binding.rvAuditLogs.adapter = adapter

        binding.rvAuditLogs.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (dy > 0 && !isLoading && hasMore) {
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
        loadedLogs.clear()
        hasMore = true
        updateFilterBadge()
        loadPage(before = null)
    }

    private fun loadNextPage() {
        val oldestTimestamp = loadedLogs.lastOrNull()?.timestamp ?: return
        loadPage(before = oldestTimestamp)
    }

    private fun loadPage(before: Long?) {
        if (isLoading) return
        isLoading = true

        if (before == null) {
            binding.progressBar.visibility = View.VISIBLE
            binding.emptyContainer.visibility = View.GONE
        }

        lifecycleScope.launch {
            try {
                val newLogs = auditRepo.getLogs(
                    timeRange = currentTimeRange,
                    before = before,
                    adminEmail = selectedAdminEmail,
                    actionType = selectedActionType,
                    searchQuery = currentSearchQuery,
                    limit = 50
                )
                binding.progressBar.visibility = View.GONE
                isLoading = false

                if (newLogs.size < 50) {
                    hasMore = false
                }

                if (before == null) {
                    loadedLogs.clear()
                }
                loadedLogs.addAll(newLogs)
                adapter.submitLogs(loadedLogs)

                binding.emptyContainer.visibility = if (loadedLogs.isEmpty()) View.VISIBLE else View.GONE
            } catch (e: Exception) {
                binding.progressBar.visibility = View.GONE
                isLoading = false
                AppBulletin.showError(this@ActivityLogActivity, "Failed to load recent actions: ${e.message}")
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
        val dialog = BottomSheetDialog(this)
        val palette = TelegramThemeEngine.resolveCurrentPalette(this)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20f), dp(16f), dp(20f), dp(24f))
            setBackgroundColor(palette.surface)
        }

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
            setTextAndValue("All Admins", if (selectedAdminEmail == null) "✓" else "", false)
            setOnClickListener {
                selectedAdminEmail = null
                dialog.dismiss()
                reloadLogs()
            }
        }
        root.addView(btnAllAdmins)

        lifecycleScope.launch {
            try {
                val admins = adminRepo.getDynamicAdmins()
                for (admin in admins) {
                    val isSelected = selectedAdminEmail.equals(admin.email, ignoreCase = true)
                    val cell = com.eve.app.ui.admin.tg.TextCell(this@ActivityLogActivity).apply {
                        setTextAndValue(admin.email, if (isSelected) "✓" else "", false)
                        setOnClickListener {
                            selectedAdminEmail = admin.email
                            dialog.dismiss()
                            reloadLogs()
                        }
                    }
                    root.addView(cell)
                }
            } catch (_: Exception) {}
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
            setTextAndValue("All Actions", if (selectedActionType == null) "✓" else "", false)
            setOnClickListener {
                selectedActionType = null
                dialog.dismiss()
                reloadLogs()
            }
        }
        root.addView(btnAllTypes)

        lifecycleScope.launch {
            try {
                val types = auditRepo.getActionTypes()
                for (type in types) {
                    val isSelected = selectedActionType.equals(type, ignoreCase = true)
                    val cell = com.eve.app.ui.admin.tg.TextCell(this@ActivityLogActivity).apply {
                        setTextAndValue(type, if (isSelected) "✓" else "", false)
                        setOnClickListener {
                            selectedActionType = type
                            dialog.dismiss()
                            reloadLogs()
                        }
                    }
                    root.addView(cell)
                }
            } catch (_: Exception) {}
        }

        dialog.setContentView(root)
        dialog.show()
    }

    private fun showLogDetailDialog(log: AdminAuditLog) {
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

        TelegramNativeAlertDialog.Builder(this)
            .setTitle(log.actionBadge)
            .setMessage(msg.toString().trimEnd())
            .setPositiveButton("Close", null)
            .show()
    }

    private fun dp(v: Float): Int = (v * resources.displayMetrics.density).toInt()
}
