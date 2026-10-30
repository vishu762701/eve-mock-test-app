package com.eve.app.ui.admin

import com.eve.app.ui.common.EveBaseActivity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.eve.app.R
import com.eve.app.data.repository.AdminRepository
import com.eve.app.data.repository.ApiUsageRepository
import com.eve.app.databinding.ActivityApiUsageBinding
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class ApiUsageActivity : EveBaseActivity() {

    private lateinit var binding: ActivityApiUsageBinding
    private val adminRepo = AdminRepository()
    private val usageRepo = ApiUsageRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityApiUsageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        com.eve.app.util.AdminAccessGate.requireAdmin(this, binding.root, requireFreshServer = true) {
            setupUi()
            loadUsage()
        }
    }

    private fun setupUi() {
        binding.btnBack.setOnClickListener { finish() }
        binding.btnRefresh.setOnClickListener { loadUsage() }
    }

    private fun loadUsage() {
        lifecycleScope.launch {
            val usage = usageRepo.getUsageForCurrentMonth()
            val countedNote = if (usage.countedSince.isNotBlank()) " • Since ${usage.countedSince.take(10)}" else ""
            binding.tvCurrentMonth.text = "📅 ${usage.monthDisplayName}$countedNote"
            binding.tvGeminiCallsCount.text = usage.geminiCalls.toString()
            binding.tvDocsCreatedCount.text = usage.documentsCreatedProxy.toString()
            binding.tvSubmissionsCount.text = usage.testSubmissionsProxy.toString()
        }
    }
}
