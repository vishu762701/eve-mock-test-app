package com.eve.app.ui.admin

import android.os.Bundle
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import com.eve.app.R
import com.eve.app.data.repository.AdminRepository
import com.eve.app.databinding.ActivityTypographyStudioBinding
import com.eve.app.databinding.ItemTypographyPreviewRowBinding
import com.eve.app.designsystem.DesignType
import com.eve.app.ui.common.EveBaseActivity
import com.eve.app.util.FontManager
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

/**
 * Dedicated Admin Typography Studio with live preview across the 11 Apple iOS 26
 * canonical typography styles and authentic Inter font family weights.
 *
 * Selecting a font changes the local preview immediately.
 */
class TypographyStudioActivity : EveBaseActivity() {

    lateinit var binding: ActivityTypographyStudioBinding
    private val adminRepo = AdminRepository()

    var selectedFont: FontManager.FontOption = FontManager.FontOption.INTER_REGULAR
        private set

    data class StyleRow(
        val role: DesignType.Role,
        val binding: ItemTypographyPreviewRowBinding,
        val name: String,
        val specs: String
    )

    private lateinit var styleRows: List<StyleRow>

    companion object {
        const val DEFAULT_ENGLISH_TEXT = "The quick brown fox jumps over the lazy dog"
        const val DEFAULT_HINDI_TEXT = "संघ लोक सेवा आयोग — सिविल सेवा परीक्षा"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTypographyStudioBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }

        val currentUser = FirebaseAuth.getInstance().currentUser
        val email = currentUser?.email
        if (currentUser != null && email != null) {
            lifecycleScope.launch {
                if (!adminRepo.isAdmin(email)) {
                    finish()
                    return@launch
                }
            }
        }

        initStyleRows()
        initControls()
        updatePreviewImmediately()
    }

    private fun initStyleRows() {
        styleRows = listOf(
            StyleRow(DesignType.Role.LARGE_TITLE, binding.previewLargeTitle, "Large Title", "34sp / 41sp"),
            StyleRow(DesignType.Role.TITLE_1, binding.previewTitle1, "Title 1", "28sp / 34sp"),
            StyleRow(DesignType.Role.TITLE_2, binding.previewTitle2, "Title 2", "22sp / 28sp"),
            StyleRow(DesignType.Role.TITLE_3, binding.previewTitle3, "Title 3", "20sp / 25sp"),
            StyleRow(DesignType.Role.HEADLINE, binding.previewHeadline, "Headline", "17sp / 22sp"),
            StyleRow(DesignType.Role.BODY, binding.previewBody, "Body", "17sp / 22sp"),
            StyleRow(DesignType.Role.CALLOUT, binding.previewCallout, "Callout", "16sp / 21sp"),
            StyleRow(DesignType.Role.SUBHEADLINE, binding.previewSubheadline, "Subheadline", "15sp / 20sp"),
            StyleRow(DesignType.Role.FOOTNOTE, binding.previewFootnote, "Footnote", "13sp / 18sp"),
            StyleRow(DesignType.Role.CAPTION_1, binding.previewCaption1, "Caption 1", "12sp / 16sp"),
            StyleRow(DesignType.Role.CAPTION_2, binding.previewCaption2, "Caption 2", "11sp / 13sp")
        )
    }

    private fun initControls() {
        binding.chipGroupFonts.setOnCheckedStateChangeListener { _, checkedIds ->
            val option = when (checkedIds.firstOrNull()) {
                R.id.chipInterMedium -> FontManager.FontOption.INTER_MEDIUM
                R.id.chipInterSemibold -> FontManager.FontOption.INTER_SEMIBOLD
                R.id.chipInterBold -> FontManager.FontOption.INTER_BOLD
                R.id.chipSystemSans -> FontManager.FontOption.SYSTEM_SANS
                R.id.chipSystemSerif -> FontManager.FontOption.SYSTEM_SERIF
                R.id.chipSystemMono -> FontManager.FontOption.SYSTEM_MONOSPACE
                else -> FontManager.FontOption.INTER_REGULAR
            }
            selectFont(option)
        }

        binding.switchEmphasized.setOnCheckedChangeListener { _, _ ->
            updatePreviewImmediately()
        }

        binding.etSampleText.doAfterTextChanged {
            updatePreviewImmediately()
        }

        binding.btnPresetEnglish.setOnClickListener {
            binding.etSampleText.setText(DEFAULT_ENGLISH_TEXT)
        }

        binding.btnPresetHindi.setOnClickListener {
            binding.etSampleText.setText(DEFAULT_HINDI_TEXT)
        }

        binding.btnResetText.setOnClickListener {
            binding.etSampleText.setText(DEFAULT_ENGLISH_TEXT)
            binding.switchEmphasized.isChecked = false
            binding.chipInterRegular.isChecked = true
            selectFont(FontManager.FontOption.INTER_REGULAR)
        }
    }

    /**
     * Selecting a font updates the active font info and immediately reflects on the local preview.
     */
    fun selectFont(option: FontManager.FontOption) {
        selectedFont = option
        binding.tvActiveFontInfo.text = "Selected: ${option.displayName} (${option.weightLabel})"
        updatePreviewImmediately()
    }

    /**
     * Updates all 11 canonical styles and component previews synchronously.
     */
    fun updatePreviewImmediately() {
        val tf = FontManager.getTypefaceForOption(this, selectedFont)
        val sampleText = binding.etSampleText.text?.toString()?.takeIf { it.isNotBlank() } ?: DEFAULT_ENGLISH_TEXT
        val isEmphasized = binding.switchEmphasized.isChecked

        for (row in styleRows) {
            row.binding.tvStyleName.text = row.name
            row.binding.tvStyleSpecs.text = row.specs
            row.binding.tvPreviewSample.text = sampleText
            DesignType.apply(row.binding.tvPreviewSample, row.role, isEmphasized)
            row.binding.tvPreviewSample.typeface = tf
        }

        binding.previewPrimaryButton.typeface = tf
        binding.previewSecondaryButton.typeface = tf
        binding.previewCardHeading.typeface = tf
        binding.previewCardBody.typeface = tf
    }
}
