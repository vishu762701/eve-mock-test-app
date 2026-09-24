package com.eve.app.ui.admin

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.eve.app.data.model.AppContent
import com.eve.app.data.repository.AppContentRepository
import com.eve.app.databinding.ActivityEditAboutBinding
import com.google.android.material.tabs.TabLayout
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class EditAboutActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditAboutBinding
    private val repo = AppContentRepository()

    private var currentType: String = AppContentRepository.TYPE_PRIVACY
    private var initialContent: AppContent = AppContent()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditAboutBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { handleBack() }
        binding.btnSave.setOnClickListener { saveCurrent() }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                handleBack()
            }
        })

        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                val newType = when (tab?.position) {
                    0 -> AppContentRepository.TYPE_PRIVACY
                    1 -> AppContentRepository.TYPE_TERMS
                    2 -> AppContentRepository.TYPE_CONTACT
                    else -> AppContentRepository.TYPE_PRIVACY
                }
                if (newType != currentType) {
                    if (isDirty()) {
                        AlertDialog.Builder(this@EditAboutActivity)
                            .setTitle("Unsaved Changes")
                            .setMessage("You have unsaved changes. Do you want to save before switching?")
                            .setPositiveButton("Save") { _, _ ->
                                saveCurrent { switchTab(newType) }
                            }
                            .setNegativeButton("Discard") { _, _ ->
                                switchTab(newType)
                            }
                            .setNeutralButton("Cancel") { _, _ ->
                                // Re-select current tab
                                val oldIdx = when (currentType) {
                                    AppContentRepository.TYPE_PRIVACY -> 0
                                    AppContentRepository.TYPE_TERMS -> 1
                                    AppContentRepository.TYPE_CONTACT -> 2
                                    else -> 0
                                }
                                binding.tabLayout.getTabAt(oldIdx)?.select()
                            }
                            .show()
                    } else {
                        switchTab(newType)
                    }
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        loadContent(currentType)
    }

    private fun switchTab(type: String) {
        currentType = type
        binding.layoutContactFields.visibility =
            if (type == AppContentRepository.TYPE_CONTACT) View.VISIBLE else View.GONE
        loadContent(type)
    }

    private fun loadContent(type: String) {
        binding.progressBar.visibility = View.VISIBLE
        binding.scrollViewContent.visibility = View.INVISIBLE
        lifecycleScope.launch {
            val content = repo.getContent(type)
            initialContent = content
            populateUi(content)
            binding.progressBar.visibility = View.GONE
            binding.scrollViewContent.visibility = View.VISIBLE
        }
    }

    private fun populateUi(content: AppContent) {
        binding.etTitle.setText(content.title)
        binding.etBody.setText(content.body)
        binding.etSupportEmail.setText(content.supportEmail)
        binding.etPhone.setText(content.phone)
        binding.etWebsite.setText(content.website)
        binding.etAddress.setText(content.address)
    }

    private fun getCurrentUiContent(): AppContent {
        val userEmail = FirebaseAuth.getInstance().currentUser?.email ?: "admin"
        return AppContent(
            title = binding.etTitle.text?.toString()?.trim().orEmpty(),
            body = binding.etBody.text?.toString()?.trim().orEmpty(),
            updatedAt = System.currentTimeMillis(),
            updatedBy = userEmail,
            supportEmail = binding.etSupportEmail.text?.toString()?.trim().orEmpty(),
            phone = binding.etPhone.text?.toString()?.trim().orEmpty(),
            website = binding.etWebsite.text?.toString()?.trim().orEmpty(),
            address = binding.etAddress.text?.toString()?.trim().orEmpty()
        )
    }

    private fun isDirty(): Boolean {
        val current = getCurrentUiContent()
        return current.title != initialContent.title ||
            current.body != initialContent.body ||
            current.supportEmail != initialContent.supportEmail ||
            current.phone != initialContent.phone ||
            current.website != initialContent.website ||
            current.address != initialContent.address
    }

    private fun saveCurrent(onSuccess: (() -> Unit)? = null) {
        val current = getCurrentUiContent()
        if (current.title.isEmpty()) {
            binding.tilTitle.error = "Title cannot be empty"
            return
        } else {
            binding.tilTitle.error = null
        }

        if (current.body.isEmpty()) {
            binding.tilBody.error = "Body content cannot be empty"
            return
        } else {
            binding.tilBody.error = null
        }

        binding.btnSave.isEnabled = false
        binding.progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                repo.saveContent(currentType, current)
                initialContent = current
                binding.btnSave.isEnabled = true
                binding.progressBar.visibility = View.GONE
                Toast.makeText(this@EditAboutActivity, "Saved successfully!", Toast.LENGTH_SHORT).show()
                onSuccess?.invoke()
            } catch (e: Exception) {
                binding.btnSave.isEnabled = true
                binding.progressBar.visibility = View.GONE
                Toast.makeText(
                    this@EditAboutActivity,
                    "Failed to save: ${e.localizedMessage ?: "Unknown error"}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun handleBack() {
        if (isDirty()) {
            AlertDialog.Builder(this)
                .setTitle("Unsaved Changes")
                .setMessage("You have unsaved changes. Do you want to save before leaving?")
                .setPositiveButton("Save") { _, _ ->
                    saveCurrent { finish() }
                }
                .setNegativeButton("Discard") { _, _ ->
                    finish()
                }
                .setNeutralButton("Cancel", null)
                .show()
        } else {
            finish()
        }
    }
}
