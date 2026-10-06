package com.eve.app.ui.admin

import com.eve.app.ui.common.EveBaseActivity

import android.os.Bundle
import android.view.View
import com.eve.app.util.AppBulletin
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.eve.app.data.model.AppContent
import com.eve.app.data.repository.AppContentRepository
import com.eve.app.databinding.ActivityEditAboutBinding
import com.google.android.material.tabs.TabLayout
import android.widget.ArrayAdapter
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class EditAboutActivity : EveBaseActivity() {

    private lateinit var binding: ActivityEditAboutBinding
    private val repo = AppContentRepository()

    private var currentType: String = AppContentRepository.TYPE_PRIVACY
    private var initialContent: AppContent = AppContent()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditAboutBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val ctaActions = listOf("No button", "Open Practice", "Open PYQ", "Browse Exams")
        binding.spinnerHomeHeroCtaAction.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            ctaActions
        )
        currentType = intent.getStringExtra(EXTRA_INITIAL_TYPE)
            ?.takeIf { it == AppContentRepository.TYPE_HOME_HERO }
            ?: AppContentRepository.TYPE_PRIVACY
        binding.tabLayout.getTabAt(tabIndex(currentType))?.select()

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
                    3 -> AppContentRepository.TYPE_HOME_HERO
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
                                binding.tabLayout.getTabAt(tabIndex(currentType))?.select()
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
        val isHomeHero = type == AppContentRepository.TYPE_HOME_HERO
        binding.layoutHomeHeroFields.visibility = if (isHomeHero) View.VISIBLE else View.GONE
        binding.tilTitle.hint = if (isHomeHero) "Headline" else "Page Title"
        binding.tilBody.hint = if (isHomeHero) "Supporting text" else "Body Content"
        binding.etBody.minLines = if (isHomeHero) 5 else 12
        loadContent(type)
    }

    private fun loadContent(type: String) {
        binding.btnSave.isEnabled = false
        binding.progressBar.visibility = View.VISIBLE
        binding.scrollViewContent.visibility = View.INVISIBLE
        lifecycleScope.launch {
            try {
                val content = if (type == AppContentRepository.TYPE_HOME_HERO) {
                    repo.getAdminHomeHero()
                } else {
                    repo.getContent(type)
                }
                initialContent = content
                populateUi(content)
                binding.btnSave.isEnabled = true
                binding.progressBar.visibility = View.GONE
                binding.scrollViewContent.visibility = View.VISIBLE
            } catch (e: Exception) {
                initialContent = AppContent()
                populateUi(initialContent)
                binding.btnSave.isEnabled = false
                binding.progressBar.visibility = View.GONE
                binding.scrollViewContent.visibility = View.VISIBLE
                AppBulletin.showError(
                    this@EditAboutActivity,
                    "Couldn't load content: ${e.localizedMessage ?: "Unknown error"}"
                )
            }
        }
    }

    private fun populateUi(content: AppContent) {
        binding.etTitle.setText(content.title)
        binding.etBody.setText(content.body)
        binding.etSupportEmail.setText(content.supportEmail)
        binding.etPhone.setText(content.phone)
        binding.etWebsite.setText(content.website)
        binding.etAddress.setText(content.address)
        binding.switchHomeHeroEnabled.isChecked = content.enabled
        binding.etHomeHeroCtaLabel.setText(content.ctaLabel)
        binding.spinnerHomeHeroCtaAction.setSelection(
            when (content.ctaAction) {
                "open_practice" -> 1
                "open_pyq" -> 2
                "browse_exams" -> 3
                else -> 0
            }
        )
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
            address = binding.etAddress.text?.toString()?.trim().orEmpty(),
            enabled = binding.switchHomeHeroEnabled.isChecked,
            ctaLabel = binding.etHomeHeroCtaLabel.text?.toString()?.trim().orEmpty(),
            ctaAction = when (binding.spinnerHomeHeroCtaAction.selectedItemPosition) {
                1 -> "open_practice"
                2 -> "open_pyq"
                3 -> "browse_exams"
                else -> ""
            }
        )
    }

    private fun isDirty(): Boolean {
        val current = getCurrentUiContent()
        return current.title != initialContent.title ||
            current.body != initialContent.body ||
            current.supportEmail != initialContent.supportEmail ||
            current.phone != initialContent.phone ||
            current.website != initialContent.website ||
            current.address != initialContent.address ||
            (currentType == AppContentRepository.TYPE_HOME_HERO &&
                (current.enabled != initialContent.enabled ||
                    current.ctaLabel != initialContent.ctaLabel ||
                    current.ctaAction != initialContent.ctaAction))
    }

    private fun saveCurrent(onSuccess: (() -> Unit)? = null) {
        val current = getCurrentUiContent()
        val homeHero = currentType == AppContentRepository.TYPE_HOME_HERO
        val required = !homeHero || current.enabled
        if (required && current.title.isEmpty()) {
            binding.tilTitle.error = "Title cannot be empty"
            return
        } else {
            binding.tilTitle.error = null
        }

        if (required && current.body.isEmpty()) {
            binding.tilBody.error = "Body content cannot be empty"
            return
        } else {
            binding.tilBody.error = null
        }

        if (homeHero && current.title.length > 80) {
            binding.tilTitle.error = "Use at most 80 characters"
            return
        }
        if (homeHero && current.body.length > 600) {
            binding.tilBody.error = "Use at most 600 characters"
            return
        }
        binding.tilHomeHeroCtaLabel.error = null
        if (homeHero && current.ctaLabel.length > 32) {
            binding.tilHomeHeroCtaLabel.error = "Use at most 32 characters"
            return
        }
        if (homeHero && current.ctaLabel.isNotEmpty() != current.ctaAction.isNotEmpty()) {
            binding.tilHomeHeroCtaLabel.error = "Choose a button action for its label, or clear the label"
            return
        }

        binding.btnSave.isEnabled = false
        binding.progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                repo.saveContent(currentType, current)
                initialContent = current
                binding.btnSave.isEnabled = true
                binding.progressBar.visibility = View.GONE
                AppBulletin.showSuccess(this@EditAboutActivity, "Saved successfully!")
                onSuccess?.invoke()
            } catch (e: Exception) {
                binding.btnSave.isEnabled = true
                binding.progressBar.visibility = View.GONE
                AppBulletin.showError(
                    this@EditAboutActivity,
                    "Failed to save: ${e.localizedMessage ?: "Unknown error"}"
                )
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

    private fun tabIndex(type: String): Int = when (type) {
        AppContentRepository.TYPE_TERMS -> 1
        AppContentRepository.TYPE_CONTACT -> 2
        AppContentRepository.TYPE_HOME_HERO -> 3
        else -> 0
    }

    companion object {
        const val EXTRA_INITIAL_TYPE = "initial_type"
    }
}
