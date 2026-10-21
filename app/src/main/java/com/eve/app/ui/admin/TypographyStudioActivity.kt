package com.eve.app.ui.admin

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Typeface
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import com.eve.app.R
import com.eve.app.data.repository.AdminRepository
import com.eve.app.databinding.ActivityTypographyStudioBinding
import com.eve.app.databinding.ItemTypographyPreviewRowBinding
import com.eve.app.designsystem.DesignType
import com.eve.app.typography.StyleRule
import com.eve.app.typography.TypographyPreset
import com.eve.app.typography.TypographyRegistry
import com.eve.app.typography.TypographyRepository
import java.math.BigDecimal
import java.math.RoundingMode
import com.eve.app.typography.TypographyResolver
import com.eve.app.typography.TypographyStudioViewModel
import com.eve.app.ui.common.EveBaseActivity
import com.eve.app.util.FontManager
import com.google.android.material.tabs.TabLayout
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Dedicated Admin Typography Studio with live preview across the 11 Apple iOS 26
 * canonical typography styles and authentic font library (Inter, Poppins, Roboto, Noto Sans).
 *
 * 5 Studio Sections:
 * 1. Font Library
 * 2. Global Styles
 * 3. Screen Editor
 * 4. Live Preview
 * 5. Versions & Restore
 */
class TypographyStudioActivity : EveBaseActivity() {

    lateinit var binding: ActivityTypographyStudioBinding
    private val adminRepo = AdminRepository()
    private val viewModel by lazy { TypographyStudioViewModel(TypographyRepository.getInstance(this)) }

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

        initTabs()
        initStyleRows()
        initControls()
        initFontLibrarySection()
        initGlobalStylesSection()
        initScreenEditorSection()
        initVersionsSection()
        observeViewModel()
        updatePreviewImmediately()
    }

    private fun initTabs() {
        binding.tabLayoutStudio.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                showTabSection(tab?.position ?: 0)
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) = Unit
            override fun onTabReselected(tab: TabLayout.Tab?) = Unit
        })
    }

    private fun showTabSection(position: Int) {
        binding.sectionFontLibrary.visibility = if (position == 0) View.VISIBLE else View.GONE
        binding.sectionGlobalStyles.visibility = if (position == 1) View.VISIBLE else View.GONE
        binding.sectionScreenEditor.visibility = if (position == 2) View.VISIBLE else View.GONE
        binding.sectionLivePreview.visibility = if (position == 3) View.VISIBLE else View.GONE
        binding.sectionVersions.visibility = if (position == 4) View.VISIBLE else View.GONE
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

        // Viewport width simulation
        binding.chipGroupViewport.setOnCheckedStateChangeListener { _, checkedIds ->
            val width = when (checkedIds.firstOrNull()) {
                R.id.chipViewport320 -> 320
                R.id.chipViewport412 -> 412
                else -> 360
            }
            viewModel.setPreviewWidth(width)
        }

        // Theme simulation
        binding.switchPreviewTheme.setOnCheckedChangeListener { _, _ ->
            viewModel.toggleDarkMode()
        }

        // Before / After Comparison
        binding.switchBeforeAfter.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                applyConfigToPreview(viewModel.publishedConfig.value)
            } else {
                applyConfigToPreview(viewModel.draftConfig.value)
            }
        }
    }

    private fun initFontLibrarySection() {
        // Set authentic font samples
        try {
            val inter400 = FontManager.getTypeface(this, FontManager.FontFamily.INTER, FontManager.FontWeight.REGULAR)
            val inter500 = FontManager.getTypeface(this, FontManager.FontFamily.INTER, FontManager.FontWeight.MEDIUM)
            val inter600 = FontManager.getTypeface(this, FontManager.FontFamily.INTER, FontManager.FontWeight.SEMIBOLD)
            val inter700 = FontManager.getTypeface(this, FontManager.FontFamily.INTER, FontManager.FontWeight.BOLD)
            binding.tvSampleInterRegular.typeface = inter400
            binding.tvSampleInterMedium.typeface = inter500
            binding.tvSampleInterSemibold.typeface = inter600
            binding.tvSampleInterBold.typeface = inter700
            binding.tvSampleInterHindi.typeface = inter400

            val pop400 = FontManager.getTypeface(this, FontManager.FontFamily.POPPINS, FontManager.FontWeight.REGULAR)
            val pop500 = FontManager.getTypeface(this, FontManager.FontFamily.POPPINS, FontManager.FontWeight.MEDIUM)
            val pop600 = FontManager.getTypeface(this, FontManager.FontFamily.POPPINS, FontManager.FontWeight.SEMIBOLD)
            val pop700 = FontManager.getTypeface(this, FontManager.FontFamily.POPPINS, FontManager.FontWeight.BOLD)
            binding.tvSamplePoppinsRegular.typeface = pop400
            binding.tvSamplePoppinsMedium.typeface = pop500
            binding.tvSamplePoppinsSemibold.typeface = pop600
            binding.tvSamplePoppinsBold.typeface = pop700
            binding.tvSamplePoppinsHindi.typeface = pop400

            val rob400 = FontManager.getTypeface(this, FontManager.FontFamily.ROBOTO, FontManager.FontWeight.REGULAR)
            val rob500 = FontManager.getTypeface(this, FontManager.FontFamily.ROBOTO, FontManager.FontWeight.MEDIUM)
            val rob600 = FontManager.getTypeface(this, FontManager.FontFamily.ROBOTO, FontManager.FontWeight.SEMIBOLD)
            val rob700 = FontManager.getTypeface(this, FontManager.FontFamily.ROBOTO, FontManager.FontWeight.BOLD)
            binding.tvSampleRobotoRegular.typeface = rob400
            binding.tvSampleRobotoMedium.typeface = rob500
            binding.tvSampleRobotoSemibold.typeface = rob600
            binding.tvSampleRobotoBold.typeface = rob700
            binding.tvSampleRobotoHindi.typeface = rob400

            val noto400 = FontManager.getTypeface(this, FontManager.FontFamily.NOTO_SANS, FontManager.FontWeight.REGULAR)
            val noto500 = FontManager.getTypeface(this, FontManager.FontFamily.NOTO_SANS, FontManager.FontWeight.MEDIUM)
            val noto600 = FontManager.getTypeface(this, FontManager.FontFamily.NOTO_SANS, FontManager.FontWeight.SEMIBOLD)
            val noto700 = FontManager.getTypeface(this, FontManager.FontFamily.NOTO_SANS, FontManager.FontWeight.BOLD)
            binding.tvSampleNotoRegular.typeface = noto400
            binding.tvSampleNotoMedium.typeface = noto500
            binding.tvSampleNotoSemibold.typeface = noto600
            binding.tvSampleNotoBold.typeface = noto700
            binding.tvSampleNotoHindi.typeface = noto400
        } catch (_: Throwable) {}

        binding.btnSelectInter.setOnClickListener {
            viewModel.selectFontFamily(FontManager.FontFamily.INTER)
            selectFont(FontManager.FontOption.INTER_REGULAR)
            Toast.makeText(this, "Selected Inter for active draft", Toast.LENGTH_SHORT).show()
        }
        binding.btnSelectPoppins.setOnClickListener {
            viewModel.selectFontFamily(FontManager.FontFamily.POPPINS)
            selectFont(FontManager.FontOption.SYSTEM_SANS)
            Toast.makeText(this, "Selected Poppins for active draft", Toast.LENGTH_SHORT).show()
        }
        binding.btnSelectRoboto.setOnClickListener {
            viewModel.selectFontFamily(FontManager.FontFamily.ROBOTO)
            selectFont(FontManager.FontOption.SYSTEM_SANS)
            Toast.makeText(this, "Selected Roboto for active draft", Toast.LENGTH_SHORT).show()
        }
        binding.btnSelectNoto.setOnClickListener {
            viewModel.selectFontFamily(FontManager.FontFamily.NOTO_SANS)
            selectFont(FontManager.FontOption.SYSTEM_SANS)
            Toast.makeText(this, "Selected Noto Sans for active draft", Toast.LENGTH_SHORT).show()
        }
    }

    private fun initGlobalStylesSection() {
        binding.btnPresetIosBalanced.setOnClickListener {
            viewModel.applyPreset(TypographyPreset.IOS26_BALANCED)
            Toast.makeText(this, "Applied iOS 26 Balanced preset", Toast.LENGTH_SHORT).show()
        }
        binding.btnPresetIosHig.setOnClickListener {
            viewModel.applyPreset(TypographyPreset.IOS26_HIG_REFERENCE)
            Toast.makeText(this, "Applied iOS 26 HIG Reference preset", Toast.LENGTH_SHORT).show()
        }
        binding.btnPresetPoppinsClean.setOnClickListener {
            viewModel.applyPreset(TypographyPreset.POPPINS_CLEAN)
            Toast.makeText(this, "Applied Poppins Clean preset", Toast.LENGTH_SHORT).show()
        }
        binding.btnPresetRobotoNeutral.setOnClickListener {
            viewModel.applyPreset(TypographyPreset.ROBOTO_NEUTRAL)
            Toast.makeText(this, "Applied Roboto Neutral preset", Toast.LENGTH_SHORT).show()
        }
        binding.btnPresetNotoReadable.setOnClickListener {
            viewModel.applyPreset(TypographyPreset.NOTO_READABLE)
            Toast.makeText(this, "Applied Noto Readable preset", Toast.LENGTH_SHORT).show()
        }

        val canonicalRoles = listOf(
            "large_title" to "Large Title (34sp)",
            "title_1" to "Title 1 (28sp)",
            "title_2" to "Title 2 (22sp)",
            "title_3" to "Title 3 (20sp)",
            "headline" to "Headline (17sp · 600)",
            "body" to "Body (17sp · 400)",
            "callout" to "Callout (16sp)",
            "subheadline" to "Subheadline (15sp)",
            "footnote" to "Footnote (13sp)",
            "caption_1" to "Caption 1 (12sp)",
            "caption_2" to "Caption 2 (11sp)"
        )
        val roleNames = canonicalRoles.map { it.second }
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, roleNames).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        binding.spGlobalRole.adapter = adapter

        // UIArc Slider adaptation: no draft mutation until the existing Save button.
        binding.sliderRoleSize.apply {
            valueTo = TypographyResolver.MAX_TEXT_SIZE_SP
            value = 17f
            valueFrom = TypographyResolver.MIN_TEXT_SIZE_SP
            stepSize = 0f // The typography resolver supports fractional sp without a fixed increment.
            addOnChangeListener { _, size, fromUser ->
                if (!fromUser) return@addOnChangeListener
                val formatted = BigDecimal(size.toString())
                    .setScale(1, RoundingMode.HALF_UP)
                    .stripTrailingZeros().toPlainString()
                if (binding.etRoleSize.text?.toString() != formatted) {
                    binding.etRoleSize.setText(formatted)
                }
            }
        }
        binding.etRoleSize.doAfterTextChanged { text ->
            val size = text?.toString()?.toFloatOrNull() ?: return@doAfterTextChanged
            if (!size.isFinite()) return@doAfterTextChanged
            val clamped = size.coerceIn(
                TypographyResolver.MIN_TEXT_SIZE_SP, TypographyResolver.MAX_TEXT_SIZE_SP
            )
            if (binding.sliderRoleSize.value != clamped) binding.sliderRoleSize.value = clamped
            if (size == clamped) binding.tvRolePreviewSample.setTextSize(TypedValue.COMPLEX_UNIT_SP, size)
        }

        binding.spGlobalRole.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val roleKey = canonicalRoles.getOrNull(position)?.first ?: "headline"
                populateRoleFields(roleKey)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }

        binding.btnSaveRole.setOnClickListener {
            val selectedPos = binding.spGlobalRole.selectedItemPosition
            val roleKey = canonicalRoles.getOrNull(selectedPos)?.first ?: "headline"
            val size = binding.etRoleSize.text?.toString()?.toFloatOrNull() ?: 17f
            val line = binding.etRoleLineHeight.text?.toString()?.toFloatOrNull() ?: 22f
            val weight = when (binding.chipGroupRoleWeight.checkedChipId) {
                R.id.chipWeight400 -> 400
                R.id.chipWeight500 -> 500
                R.id.chipWeight700 -> 700
                else -> 600
            }
            binding.tilRoleSize.error = null
            binding.tilRoleLineHeight.error = null
            if (!size.isFinite() || size < TypographyResolver.MIN_TEXT_SIZE_SP ||
                size > TypographyResolver.MAX_TEXT_SIZE_SP) {
                binding.tilRoleSize.error = "Font size must be 8–60 sp"
                return@setOnClickListener
            }
            if (!line.isFinite() || line < size) {
                binding.tilRoleLineHeight.error = "Line height cannot be smaller than font size"
                return@setOnClickListener
            }
            val rule = StyleRule(textSizeSp = size, lineHeightSp = line, weight = weight)
            viewModel.updateGlobalRole(roleKey, rule)
            Toast.makeText(this, "Updated role $roleKey ($size/$line · $weight)", Toast.LENGTH_SHORT).show()
        }

        binding.btnResetCurrentRole.setOnClickListener {
            val selectedPos = binding.spGlobalRole.selectedItemPosition
            val roleKey = canonicalRoles.getOrNull(selectedPos)?.first ?: "headline"
            val defaultPreset = TypographyPreset.IOS26_BALANCED
            val defaultRule = defaultPreset.rules[roleKey] ?: StyleRule(textSizeSp = 17f, lineHeightSp = 22f, weight = 400)
            viewModel.updateGlobalRole(roleKey, defaultRule)
            populateRoleFields(roleKey)
            Toast.makeText(this, "Reset role $roleKey to default", Toast.LENGTH_SHORT).show()
        }
    }

    private fun populateRoleFields(roleKey: String) {
        val rule = viewModel.draftConfig.value.globalStyles[roleKey]
            ?: TypographyPreset.IOS26_BALANCED.rules[roleKey]
            ?: StyleRule(textSizeSp = 17f, lineHeightSp = 22f, weight = 400)

        val size = rule.textSizeSp ?: 17f
        binding.etRoleSize.setText(size.toString().removeSuffix(".0"))
        binding.etRoleLineHeight.setText((rule.lineHeightSp ?: 22f).toString().removeSuffix(".0"))
        binding.sliderRoleSize.value = size.coerceIn(
            TypographyResolver.MIN_TEXT_SIZE_SP, TypographyResolver.MAX_TEXT_SIZE_SP
        )
        when (rule.weight) {
            400 -> binding.chipWeight400.isChecked = true
            500 -> binding.chipWeight500.isChecked = true
            700 -> binding.chipWeight700.isChecked = true
            else -> binding.chipWeight600.isChecked = true
        }
        binding.tvRolePreviewSample.textSize = rule.textSizeSp ?: 17f
    }

    private fun initScreenEditorSection() {
        val screenKeys = TypographyRegistry.SCREENS.map { it.key }
        val screenNames = TypographyRegistry.SCREENS.map { it.displayName }
        val screenAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, screenNames).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        binding.spScreen.adapter = screenAdapter

        fun updateElementForm(screenIdx: Int, elemIdx: Int) {
            val screen = TypographyRegistry.SCREENS.getOrNull(screenIdx) ?: return
            val elem = screen.elements.getOrNull(elemIdx) ?: return
            val currentConfig = viewModel.draftConfig.value
            val override = currentConfig.screenOverrides[screen.key]?.elementOverrides?.get(elem.key)
                ?: currentConfig.elementOverrides[elem.key]

            if (override != null) {
                binding.etElementSize.setText(override.textSizeSp?.toString() ?: "")
                binding.etElementLineHeight.setText(override.lineHeightSp?.toString() ?: "")
                when (override.weight) {
                    400 -> binding.chipElemWeight400.isChecked = true
                    500 -> binding.chipElemWeight500.isChecked = true
                    600 -> binding.chipElemWeight600.isChecked = true
                    700 -> binding.chipElemWeight700.isChecked = true
                    else -> binding.chipElemWeightInherit.isChecked = true
                }
                val famPos = when (override.fontFamily?.lowercase()) {
                    "inter" -> 1
                    "poppins" -> 2
                    "roboto" -> 3
                    "noto_sans" -> 4
                    else -> 0
                }
                binding.spElementFontFamily.setSelection(famPos)
            } else {
                binding.etElementSize.setText("")
                binding.etElementLineHeight.setText("")
                binding.chipElemWeightInherit.isChecked = true
                binding.spElementFontFamily.setSelection(0)
            }
        }

        fun updateElementsForScreen(screenIndex: Int) {
            val screen = TypographyRegistry.SCREENS.getOrNull(screenIndex) ?: return
            val elemNames = screen.elements.map { "${it.displayName} (${it.defaultRole})" }
            val elemAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, elemNames).apply {
                setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            }
            binding.spElement.adapter = elemAdapter
            updateElementForm(screenIndex, 0)
        }

        updateElementsForScreen(0)

        binding.spScreen.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                updateElementsForScreen(position)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }

        binding.spElement.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val screenIdx = binding.spScreen.selectedItemPosition
                updateElementForm(screenIdx, position)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }

        val familyOptions = listOf("Inherit Default", "Inter", "Poppins", "Roboto", "Noto Sans")
        val familyAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, familyOptions).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        binding.spElementFontFamily.adapter = familyAdapter

        binding.btnSaveElementOverride.setOnClickListener {
            val screenIdx = binding.spScreen.selectedItemPosition
            val screen = TypographyRegistry.SCREENS.getOrNull(screenIdx) ?: return@setOnClickListener
            val elemIdx = binding.spElement.selectedItemPosition
            val elem = screen.elements.getOrNull(elemIdx) ?: return@setOnClickListener

            val size = binding.etElementSize.text?.toString()?.toFloatOrNull()
            val line = binding.etElementLineHeight.text?.toString()?.toFloatOrNull()
            val weight = when (binding.chipGroupElementWeight.checkedChipId) {
                R.id.chipElemWeight400 -> 400
                R.id.chipElemWeight500 -> 500
                R.id.chipElemWeight600 -> 600
                R.id.chipElemWeight700 -> 700
                else -> null
            }
            val family = when (binding.spElementFontFamily.selectedItemPosition) {
                1 -> "inter"
                2 -> "poppins"
                3 -> "roboto"
                4 -> "noto_sans"
                else -> null
            }

            val overrideRule = StyleRule(
                fontFamily = family,
                textSizeSp = size,
                lineHeightSp = line,
                weight = weight
            )
            viewModel.updateElementOverride(screen.key, elem.key, overrideRule)
            Toast.makeText(this, "Saved override for ${elem.displayName}", Toast.LENGTH_SHORT).show()
        }

        binding.btnResetElementOverride.setOnClickListener {
            val screenIdx = binding.spScreen.selectedItemPosition
            val screen = TypographyRegistry.SCREENS.getOrNull(screenIdx) ?: return@setOnClickListener
            val elemIdx = binding.spElement.selectedItemPosition
            val elem = screen.elements.getOrNull(elemIdx) ?: return@setOnClickListener

            viewModel.resetElementOverride(screen.key, elem.key)
            binding.etElementSize.setText("")
            binding.etElementLineHeight.setText("")
            binding.chipElemWeightInherit.isChecked = true
            binding.spElementFontFamily.setSelection(0)
            Toast.makeText(this, "Reset ${elem.displayName} to inherited values", Toast.LENGTH_SHORT).show()
        }
    }

    private fun initVersionsSection() {
        binding.btnSaveDraft.setOnClickListener {
            viewModel.saveDraft { success ->
                Toast.makeText(this, if (success) "Draft saved" else "Draft save failed", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnPublishDraft.setOnClickListener {
            val notes = binding.etPublishNotes.text?.toString()
            viewModel.publish(notes) { success, msg ->
                Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
            }
        }

        binding.btnDiscardDraft.setOnClickListener {
            viewModel.discardDraft()
            Toast.makeText(this, "Draft discarded, active published config restored", Toast.LENGTH_SHORT).show()
        }

        val versions = listOf("Version 1 (Factory Baseline)")
        val versionAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, versions).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        binding.spVersionHistory.adapter = versionAdapter

        binding.btnRollbackVersion.setOnClickListener {
            val selectedItem = binding.spVersionHistory.selectedItem?.toString()
            val version = selectedItem?.split(" ")?.firstOrNull()?.replace("v", "")?.toIntOrNull() ?: 1
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Confirm Rollback")
                .setMessage("Are you sure you want to rollback to typography snapshot v$version?")
                .setPositiveButton("Rollback") { _, _ ->
                    viewModel.rollback(version) { success, msg ->
                        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        binding.btnFactoryReset.setOnClickListener {
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Confirm Factory Reset")
                .setMessage("Are you sure you want to reset typography to factory defaults? All published customizations will be restored to iOS 26 Balanced.")
                .setPositiveButton("Reset") { _, _ ->
                    viewModel.resetToFactoryDefaults { success, msg ->
                        Toast.makeText(this, msg ?: (if (success) "Reset to factory defaults" else "Reset failed"), Toast.LENGTH_SHORT).show()
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        binding.btnExportJson.setOnClickListener {
            val json = viewModel.exportJson()
            binding.etImportJson.setText(json)
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            clipboard?.setPrimaryClip(ClipData.newPlainText("Eve Typography Config", json))
            Toast.makeText(this, "Exported JSON copied to clipboard", Toast.LENGTH_SHORT).show()
        }

        binding.btnImportJson.setOnClickListener {
            val json = binding.etImportJson.text?.toString() ?: ""
            val success = viewModel.importJson(json)
            if (success) {
                Toast.makeText(this, "Imported valid configuration", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Import rejected: invalid format or constraints", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.draftConfig.collectLatest { config ->
                binding.tvVersionStatus.text = "Status: ${config.status.uppercase()} · Version ${config.version}"
                binding.tvVersionMeta.text = "Preset: ${config.presetId} · Font: ${config.fontFamily} · Overrides: ${config.screenOverrides.size}"
                applyConfigToPreview(config)
            }
        }

        lifecycleScope.launch {
            viewModel.validationIssues.collectLatest { issues ->
                val errors = issues.filter { it.level == TypographyResolver.ValidationIssue.Level.ERROR }
                if (errors.isNotEmpty()) {
                    binding.tvValidationStatus.text = "Validation: ${errors.size} error(s) — ${errors.first().message}"
                } else {
                    binding.tvValidationStatus.text = "Validation: All semantic styles pass accessibility & safety checks"
                }
            }
        }
    }

    private fun applyConfigToPreview(config: com.eve.app.typography.TypographyConfig) {
        val family = FontManager.FontFamily.fromId(config.fontFamily)
        val tfRegular = FontManager.getTypeface(this, family, FontManager.FontWeight.REGULAR)
        val tfSemibold = FontManager.getTypeface(this, family, FontManager.FontWeight.SEMIBOLD)

        binding.previewPrimaryButton.typeface = tfSemibold
        binding.previewSecondaryButton.typeface = tfSemibold
        binding.previewCardHeading.typeface = tfSemibold
        binding.previewCardBody.typeface = tfRegular

        val sampleText = binding.etSampleText.text?.toString()?.takeIf { it.isNotBlank() } ?: DEFAULT_ENGLISH_TEXT
        for (row in styleRows) {
            row.binding.tvPreviewSample.text = sampleText
            val roleRule = config.globalStyles[row.role.name.lowercase()]
            if (roleRule != null) {
                row.binding.tvPreviewSample.textSize = roleRule.textSizeSp ?: 17f
                val weight = FontManager.FontWeight.fromInt(roleRule.weight)
                row.binding.tvPreviewSample.typeface = FontManager.getTypeface(this, family, weight)
            } else {
                row.binding.tvPreviewSample.typeface = tfRegular
            }
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
