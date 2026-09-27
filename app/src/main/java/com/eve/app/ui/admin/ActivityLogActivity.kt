package com.eve.app.ui.admin

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.eve.app.R
import com.eve.app.data.repository.AdminRepository
import com.eve.app.data.repository.AuditLogRepository
import com.eve.app.data.repository.AuditLogTimeRange
import com.eve.app.databinding.ActivityActivityLogBinding
import com.eve.app.util.AppBulletin
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class ActivityLogActivity : AppCompatActivity() {

    private lateinit var binding: ActivityActivityLogBinding
    private val adminRepo = AdminRepository()
    private val auditRepo = AuditLogRepository()
    private val adapter = AuditLogAdapter()
    private var currentFilter = AuditLogTimeRange.ALL_TIME

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setBackgroundDrawableResource(R.color.eve_bg)
        binding = ActivityActivityLogBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val email = FirebaseAuth.getInstance().currentUser?.email
        lifecycleScope.launch {
            if (!adminRepo.isAdmin(email)) {
                finish()
                return@launch
            }
            setupUi()
            loadLogs()
        }
    }

    private fun setupUi() {
        binding.btnBack.setOnClickListener { finish() }
        binding.btnRefresh.setOnClickListener { loadLogs() }

        binding.rvAuditLogs.layoutManager = LinearLayoutManager(this)
        binding.rvAuditLogs.adapter = adapter

        binding.chipGroupAuditFilter.setOnCheckedStateChangeListener { _, checkedIds ->
            currentFilter = when {
                checkedIds.contains(R.id.chipToday) -> AuditLogTimeRange.TODAY
                checkedIds.contains(R.id.chipLast7Days) -> AuditLogTimeRange.LAST_7_DAYS
                else -> AuditLogTimeRange.ALL_TIME
            }
            loadLogs()
        }
    }

    private fun loadLogs() {
        binding.progressBar.visibility = View.VISIBLE
        binding.tvEmpty.visibility = View.GONE
        lifecycleScope.launch {
            try {
                val logs = auditRepo.getLogs(currentFilter)
                binding.progressBar.visibility = View.GONE
                adapter.submit(logs)
                binding.tvEmpty.visibility = if (logs.isEmpty()) View.VISIBLE else View.GONE
            } catch (e: Exception) {
                binding.progressBar.visibility = View.GONE
                AppBulletin.showError(this@ActivityLogActivity, "Failed to load audit logs: ${e.message}")
            }
        }
    }
}
