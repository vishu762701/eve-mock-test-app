package com.eve.app.ui.admin

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.eve.app.R
import com.eve.app.data.repository.AdminRepository
import com.eve.app.data.repository.ApiUsageRepository
import com.eve.app.databinding.ActivityApiUsageBinding
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class ApiUsageActivity : AppCompatActivity() {

    private lateinit var binding: ActivityApiUsageBinding
    private val adminRepo = AdminRepository()
    private val usageRepo = ApiUsageRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setBackgroundDrawableResource(R.color.eve_bg)
        binding = ActivityApiUsageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val email = FirebaseAuth.getInstance().currentUser?.email
        lifecycleScope.launch {
            if (!adminRepo.isAdmin(email)) {
                finish()
                return@launch
            }
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
            binding.tvCurrentMonth.text = "📅 ${usage.monthDisplayName}"
            binding.tvGeminiCallsCount.text = usage.geminiCalls.toString()
            binding.tvDocsCreatedCount.text = usage.documentsCreatedProxy.toString()
            binding.tvSubmissionsCount.text = usage.testSubmissionsProxy.toString()
        }
    }
}
