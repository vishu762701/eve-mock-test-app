package com.eve.app.ui.admin.uistudio

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.eve.app.R
import com.eve.app.data.model.uistudio.*
import com.eve.app.data.repository.UiStudioRepository
import com.eve.app.databinding.ActivityUiStudioBinding
import com.eve.app.ui.common.EveBaseActivity
import com.eve.app.uistudio.UiStudioRegistry
import com.eve.app.util.AppBulletin
import com.eve.app.util.UiStudioEngine
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import kotlinx.coroutines.launch

class UiStudioActivity : EveBaseActivity() {

    private lateinit var binding: ActivityUiStudioBinding
    private val repo = UiStudioRepository.getInstance()

    private var activeConfig: UiStudioConfig = repo.getDefaultTemplate()
    private var isUpdatingFields = false

    private val supportedScreens = UiStudioRegistry.SUPPORTED_SCREENS

    private var currentScreenKey = "home"
    private var currentCompKey = "hero_banner"

    // Maps component ID to its corresponding View in the canvas
    private val canvasViewMap = mutableMapOf<String, View>()
    private var currentlySelectedView: View? = null

    // Undo / Redo stacks
    private val undoStack = ArrayDeque<UiStudioConfig>()
    private val redoStack = ArrayDeque<UiStudioConfig>()
    private val maxHistorySize = 30
    private var lastUndoPushTime: Long = 0L

    // Screen transition presets
    private val transitionOptions = listOf(
        "contextual" to "Contextual (Default)",
        "fade" to "Fade",
        "fade_scale" to "Fade + Scale",
        "scale" to "Scale",
        "none" to "None (Instant)"
    )

    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUiStudioBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupScreenSpinner()
        setupScreenTransitionSpinner()
        setupActionSpinners()
        setupColorPresetButtons()
        setupFieldListeners()
        setupTreeActionButtons()
        setupAdvancedMode()
        setupBottomActions()
        updateUndoRedoButtonState()

        loadDraftFromRepository()
    }

    override fun onPause() {
        super.onPause()
        // Save current working configuration to local cache to ensure 100% persistence
        repo.saveDraftToLocalCache(activeConfig)
    }

    private fun setupToolbar() {
        binding.btnBack.setOnClickListener { finish() }
        binding.btnUndo.setOnClickListener { performUndo() }
        binding.btnRedo.setOnClickListener { performRedo() }
        binding.btnVersionHistory.setOnClickListener { showVersionHistoryDialog() }
        binding.btnExportImport.setOnClickListener { showExportImportDialog() }
        binding.btnResetMenu.setOnClickListener { showResetOptionsDialog() }
    }

    private fun setupScreenSpinner() {
        val screenNames = supportedScreens.map { it.displayName }
        val screenAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, screenNames).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        binding.spScreenSelector.adapter = screenAdapter

        binding.spScreenSelector.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val newScreenKey = supportedScreens[position].id
                if (newScreenKey != currentScreenKey) {
                    currentScreenKey = newScreenKey
                    binding.tvActiveScreenBadge.text = supportedScreens[position].displayName
                    val screen = activeConfig.screens[currentScreenKey]
                    val firstCompKey = screen?.components?.keys?.firstOrNull() ?: ""
                    currentCompKey = firstCompKey
                    updateScreenTransitionSelection()
                    updateComponentDropdownAndTree()
                    renderRealScreenCanvas()
                    populateFieldsForCurrentComponent()
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun setupScreenTransitionSpinner() {
        val transitionLabels = transitionOptions.map { it.second }
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, transitionLabels).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        binding.spScreenTransition.adapter = adapter
        binding.spScreenTransition.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (!isUpdatingFields) {
                    val selectedTransition = transitionOptions[position].first
                    val screen = activeConfig.screens[currentScreenKey] ?: return
                    if (screen.transition != selectedTransition) {
                        pushUndoState()
                        val updatedScreen = screen.copy(transition = selectedTransition)
                        val updatedScreens = activeConfig.screens.toMutableMap().apply {
                            put(currentScreenKey, updatedScreen)
                        }
                        activeConfig = activeConfig.copy(screens = updatedScreens)
                        repo.saveDraftToLocalCache(activeConfig)
                    }
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun updateScreenTransitionSelection() {
        val screen = activeConfig.screens[currentScreenKey]
        val currentTransition = screen?.transition ?: "contextual"
        val index = transitionOptions.indexOfFirst { it.first.equals(currentTransition, ignoreCase = true) }
        val targetIdx = if (index >= 0) index else 0
        if (binding.spScreenTransition.selectedItemPosition != targetIdx) {
            binding.spScreenTransition.setSelection(targetIdx)
        }
    }

    private fun setupActionSpinners() {
        // Action Types
        val actionTypes = listOf("none" to "None (Passive)", "open_screen" to "Open App Screen", "go_back" to "Go Back", "open_url" to "Open Web URL")
        val actionAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, actionTypes.map { it.second }).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        binding.spActionType.adapter = actionAdapter
        binding.spActionType.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (!isUpdatingFields) {
                    val selectedType = actionTypes[position].first
                    updateCurrentComponentConfig { comp ->
                        comp.copy(actions = comp.actions.copy(actionType = selectedType))
                    }
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Animation Types
        val animTypes = listOf("none" to "None", "fade" to "Fade In", "scale" to "Scale In", "fade_scale" to "Fade + Scale", "slide" to "Slide Up", "pop" to "Spring Pop")
        val animAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, animTypes.map { it.second }).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        binding.spAnimationType.adapter = animAdapter
        binding.spAnimationType.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (!isUpdatingFields) {
                    val selectedType = animTypes[position].first
                    updateCurrentComponentConfig { comp ->
                        comp.copy(animation = comp.animation.copy(type = selectedType))
                    }
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Component dropdown listener
        binding.spComponentSelector.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val screen = activeConfig.screens[currentScreenKey] ?: return
                val keys = screen.components.keys.toList()
                if (position in keys.indices) {
                    val selectedKey = keys[position]
                    if (selectedKey != currentCompKey) {
                        selectComponent(selectedKey)
                    }
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun updateComponentDropdownAndTree() {
        val screen = activeConfig.screens[currentScreenKey] ?: return
        val filterQuery = binding.etSearchComponent.text?.toString()?.trim()?.lowercase() ?: ""
        
        val components = screen.components.filter { (key, comp) ->
            if (filterQuery.isBlank()) true
            else key.lowercase().contains(filterQuery) || comp.name.lowercase().contains(filterQuery) || comp.type.lowercase().contains(filterQuery)
        }

        val compLabels = components.map { (key, comp) ->
            val protTag = if (comp.isProtected) " [Protected]" else ""
            "${comp.name} ($key)$protTag"
        }

        val compAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, compLabels).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        binding.spComponentSelector.adapter = compAdapter

        val selectedIndex = components.keys.indexOf(currentCompKey)
        if (selectedIndex >= 0) {
            binding.spComponentSelector.setSelection(selectedIndex)
        }

        binding.tvTreeCount.text = "${screen.components.size} components"
    }

    private fun setupColorPresetButtons() {
        binding.btnColorDark.setOnClickListener { binding.etBackgroundColor.setText("#000000") }
        binding.btnColorWhite.setOnClickListener { binding.etBackgroundColor.setText("#FFFFFF") }
        binding.btnColorSlate.setOnClickListener { binding.etBackgroundColor.setText("#1E293B") }
        binding.btnColorBlue.setOnClickListener { binding.etBackgroundColor.setText("#007AFF") }
        binding.btnColorAmber.setOnClickListener { binding.etBackgroundColor.setText("#F59E0B") }
    }

    private fun setupTreeActionButtons() {
        // Search Filter
        binding.etSearchComponent.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                updateComponentDropdownAndTree()
            }
        })

        // Add Component
        binding.btnAddElement.setOnClickListener { showAddComponentDialog() }

        // Duplicate Component
        binding.btnDuplicateElement.setOnClickListener { duplicateCurrentComponent() }

        // Delete Component
        binding.btnDeleteElement.setOnClickListener { deleteCurrentComponent() }

        // Reorder Components
        binding.btnMoveUp.setOnClickListener { reorderComponent(isUp = true) }
        binding.btnMoveDown.setOnClickListener { reorderComponent(isUp = false) }

        // Preview Animation
        binding.btnPreviewAnimation.setOnClickListener {
            val comp = getCurrentComponentConfig()
            val view = canvasViewMap[comp.id]
            if (view != null) {
                UiStudioEngine.playEntranceAnimation(view, comp.animation.copy(enabled = true))
            }
        }
    }

    private fun loadDraftFromRepository() {
        lifecycleScope.launch {
            binding.tvStudioStatus.text = "Loading saved configuration..."
            val result = repo.getDraft()
            result.onSuccess { draft ->
                activeConfig = if (draft.screens.isNotEmpty()) draft else repo.getDefaultTemplate()
                binding.tvStudioStatus.text = if (activeConfig.version > 0) "Draft v${activeConfig.version}" else "Unpublished Draft"
                
                // Select first screen
                val screenKeys = activeConfig.screens.keys.toList()
                if (screenKeys.isNotEmpty()) {
                    currentScreenKey = screenKeys[0]
                    val screenIndex = supportedScreens.indexOfFirst { it.id == currentScreenKey }
                    if (screenIndex >= 0) binding.spScreenSelector.setSelection(screenIndex)
                    currentCompKey = activeConfig.screens[currentScreenKey]?.components?.keys?.firstOrNull() ?: ""
                }

                updateScreenTransitionSelection()
                updateComponentDropdownAndTree()
                renderRealScreenCanvas()
                populateFieldsForCurrentComponent()
                updateUndoRedoButtonState()
            }.onFailure {
                activeConfig = repo.loadCachedDraft() ?: repo.loadCachedConfig() ?: repo.getDefaultTemplate()
                binding.tvStudioStatus.text = "Working Offline Draft"
                updateScreenTransitionSelection()
                updateComponentDropdownAndTree()
                renderRealScreenCanvas()
                populateFieldsForCurrentComponent()
                updateUndoRedoButtonState()
            }
        }
    }

    private fun pushUndoState() {
        if (undoStack.size >= maxHistorySize) {
            undoStack.removeFirst()
        }
        undoStack.addLast(activeConfig)
        redoStack.clear()
        updateUndoRedoButtonState()
    }

    private fun updateUndoRedoButtonState() {
        val canUndo = undoStack.isNotEmpty()
        val canRedo = redoStack.isNotEmpty()
        binding.btnUndo.isEnabled = canUndo
        binding.btnUndo.alpha = if (canUndo) 1.0f else 0.4f
        binding.btnRedo.isEnabled = canRedo
        binding.btnRedo.alpha = if (canRedo) 1.0f else 0.4f
    }

    private fun performUndo() {
        if (undoStack.isEmpty()) return
        redoStack.addLast(activeConfig)
        activeConfig = undoStack.removeLast()
        repo.saveDraftToLocalCache(activeConfig)
        updateUndoRedoButtonState()
        refreshEntireStudioUi()
        AppBulletin.show(this, "Undo applied")
    }

    private fun performRedo() {
        if (redoStack.isEmpty()) return
        undoStack.addLast(activeConfig)
        activeConfig = redoStack.removeLast()
        repo.saveDraftToLocalCache(activeConfig)
        updateUndoRedoButtonState()
        refreshEntireStudioUi()
        AppBulletin.show(this, "Redo applied")
    }

    private fun recordFieldEditUndo() {
        val now = System.currentTimeMillis()
        if (now - lastUndoPushTime > 1500L) {
            pushUndoState()
            lastUndoPushTime = now
        }
    }

    private fun refreshEntireStudioUi() {
        if (!activeConfig.screens.containsKey(currentScreenKey)) {
            currentScreenKey = activeConfig.screens.keys.firstOrNull() ?: "home"
        }
        val screenIndex = supportedScreens.indexOfFirst { it.id == currentScreenKey }
        if (screenIndex >= 0 && binding.spScreenSelector.selectedItemPosition != screenIndex) {
            binding.spScreenSelector.setSelection(screenIndex)
        }
        val screen = activeConfig.screens[currentScreenKey]
        if (screen != null && !screen.components.containsKey(currentCompKey)) {
            currentCompKey = screen.components.keys.firstOrNull() ?: ""
        }
        updateScreenTransitionSelection()
        updateComponentDropdownAndTree()
        renderRealScreenCanvas()
        populateFieldsForCurrentComponent()
    }

    private fun setupAdvancedMode() {
        binding.btnToggleAdvanced.setOnClickListener {
            val isCurrentlyVisible = binding.layoutAdvancedContent.visibility == View.VISIBLE
            if (isCurrentlyVisible) {
                binding.layoutAdvancedContent.visibility = View.GONE
                binding.btnToggleAdvanced.text = "Show JSON"
            } else {
                binding.layoutAdvancedContent.visibility = View.VISIBLE
                binding.btnToggleAdvanced.text = "Hide JSON"
                refreshAdvancedRawJson()
            }
        }

        binding.btnApplyRawJson.setOnClickListener {
            applyAdvancedRawJson()
        }
    }

    private fun refreshAdvancedRawJson() {
        if (binding.layoutAdvancedContent.visibility == View.VISIBLE) {
            val comp = getCurrentComponentConfig()
            binding.etComponentRawJson.setText(gson.toJson(comp))
        }
    }

    private fun applyAdvancedRawJson() {
        val rawJson = binding.etComponentRawJson.text?.toString()?.trim()
        if (rawJson.isNullOrBlank()) {
            AppBulletin.showError(this, "JSON cannot be empty")
            return
        }

        try {
            val parsedComp = gson.fromJson(rawJson, ComponentConfig::class.java)
            if (parsedComp == null || parsedComp.id.isBlank()) {
                AppBulletin.showError(this, "Invalid Component JSON: id is required")
                return
            }

            pushUndoState()

            val screen = activeConfig.screens[currentScreenKey] ?: ScreenConfig(id = currentScreenKey)
            val updatedComponents = screen.components.toMutableMap().apply {
                put(parsedComp.id, parsedComp)
            }
            val updatedScreen = screen.copy(components = updatedComponents)
            val updatedScreens = activeConfig.screens.toMutableMap().apply {
                put(currentScreenKey, updatedScreen)
            }
            activeConfig = activeConfig.copy(screens = updatedScreens)
            currentCompKey = parsedComp.id
            repo.saveDraftToLocalCache(activeConfig)

            updateComponentDropdownAndTree()
            renderRealScreenCanvas()
            selectComponent(parsedComp.id)
            AppBulletin.showSuccess(this, "Component JSON applied successfully")
        } catch (e: Exception) {
            AppBulletin.showError(this, "JSON parse error: ${e.message}")
        }
    }

    private fun getCurrentComponentConfig(): ComponentConfig {
        val screen = activeConfig.screens[currentScreenKey] ?: ScreenConfig(id = currentScreenKey)
        return screen.components[currentCompKey] ?: ComponentConfig(id = currentCompKey)
    }

    private fun updateCurrentComponentConfig(mutator: (ComponentConfig) -> ComponentConfig) {
        recordFieldEditUndo()
        val currentComp = getCurrentComponentConfig()
        val updatedComp = mutator(currentComp)

        val screen = activeConfig.screens[currentScreenKey] ?: ScreenConfig(id = currentScreenKey)
        val updatedComponents = screen.components.toMutableMap().apply {
            put(currentCompKey, updatedComp)
        }
        val updatedScreen = screen.copy(components = updatedComponents)
        val updatedScreens = activeConfig.screens.toMutableMap().apply {
            put(currentScreenKey, updatedScreen)
        }
        activeConfig = activeConfig.copy(screens = updatedScreens)

        // Save immediately to local persistent cache
        repo.saveDraftToLocalCache(activeConfig)

        // Live update the view on the canvas
        val view = canvasViewMap[updatedComp.id]
        if (view != null) {
            UiStudioEngine.applyToView(view, updatedComp)
        }
    }

    /**
     * Taps or selects a component: highlights it visually on the canvas,
     * updates the component spinner, and populates the inspector fields.
     */
    private fun selectComponent(compKey: String) {
        currentCompKey = compKey
        val screen = activeConfig.screens[currentScreenKey] ?: return
        val comp = screen.components[compKey] ?: return

        // Update visual selection border on canvas
        currentlySelectedView?.foreground = null
        val targetView = canvasViewMap[compKey]
        if (targetView != null) {
            val highlightBorder = GradientDrawable().apply {
                setColor(Color.TRANSPARENT)
                setStroke(UiStudioEngine.dpToPx(this@UiStudioActivity, 2), Color.parseColor("#007AFF"))
                cornerRadius = UiStudioEngine.dpToPx(this@UiStudioActivity, comp.appearance.cornerRadius ?: 12).toFloat()
            }
            targetView.foreground = highlightBorder
            currentlySelectedView = targetView
        }

        binding.tvSelectedTag.text = "Selected: ${comp.name}"
        binding.tvProtectedBadge.text = if (comp.isProtected) "Protected Core" else "Editable Component"
        binding.tvProtectedBadge.setTextColor(if (comp.isProtected) ContextCompat.getColor(this, R.color.eve_system_yellow) else ContextCompat.getColor(this, R.color.eve_green))

        val keys = screen.components.keys.toList()
        val idx = keys.indexOf(compKey)
        if (idx >= 0 && idx != binding.spComponentSelector.selectedItemPosition) {
            binding.spComponentSelector.setSelection(idx)
        }

        populateFieldsForCurrentComponent()
        refreshAdvancedRawJson()
    }

    /**
     * Renders the REAL corresponding EVE screen structure dynamically on the visual canvas.
     */
    private fun renderRealScreenCanvas() {
        binding.canvasScreenContent.removeAllViews()
        canvasViewMap.clear()
        currentlySelectedView = null

        val screen = activeConfig.screens[currentScreenKey] ?: ScreenConfig(id = currentScreenKey)
        val density = resources.displayMetrics.density

        // Create and populate actual visual representations for the selected screen
        screen.components.forEach { (compKey, comp) ->
            val compCard = MaterialCardView(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(
                        UiStudioEngine.dpToPx(context, comp.layout.marginStart ?: 12),
                        UiStudioEngine.dpToPx(context, comp.layout.marginTop ?: 6),
                        UiStudioEngine.dpToPx(context, comp.layout.marginEnd ?: 12),
                        UiStudioEngine.dpToPx(context, comp.layout.marginBottom ?: 6)
                    )
                }
                radius = (comp.appearance.cornerRadius ?: 14) * density
                strokeWidth = (comp.appearance.strokeWidth ?: 1) * density.toInt()
                strokeColor = UiStudioEngine.parseColorSafe(comp.appearance.strokeColor) ?: Color.parseColor("#334155")
                setCardBackgroundColor(UiStudioEngine.parseColorSafe(comp.appearance.backgroundColor) ?: Color.parseColor("#1E293B"))
                elevation = (comp.appearance.elevation ?: 1) * density
                isClickable = true
                isFocusable = true
            }

            val innerLayout = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(
                    UiStudioEngine.dpToPx(context, comp.layout.paddingStart ?: 14),
                    UiStudioEngine.dpToPx(context, comp.layout.paddingTop ?: 14),
                    UiStudioEngine.dpToPx(context, comp.layout.paddingEnd ?: 14),
                    UiStudioEngine.dpToPx(context, comp.layout.paddingBottom ?: 14)
                )
            }

            val titleView = TextView(this).apply {
                text = comp.content.title?.takeIf { it.isNotBlank() } ?: comp.name
                setTextColor(UiStudioEngine.parseColorSafe(comp.typography.textColor) ?: Color.WHITE)
                textSize = (comp.typography.textSize ?: 15).toFloat()
                setTypeface(null, if (comp.typography.textStyle == "bold") Typeface.BOLD else Typeface.NORMAL)
            }
            innerLayout.addView(titleView)

            if (!comp.content.subtitle.isNullOrBlank() || comp.type in listOf("banner", "card")) {
                val subView = TextView(this).apply {
                    text = comp.content.subtitle ?: "Type: ${comp.type} • ID: $compKey"
                    setTextColor(Color.parseColor("#94A3B8"))
                    textSize = 12f
                    setPadding(0, (4 * density).toInt(), 0, 0)
                }
                innerLayout.addView(subView)
            }

            compCard.addView(innerLayout)

            // Tap-to-select visual handler
            compCard.setOnClickListener {
                selectComponent(compKey)
            }

            canvasViewMap[compKey] = compCard
            binding.canvasScreenContent.addView(compCard)
        }

        // Highlight initial selected component
        if (currentCompKey.isNotBlank()) {
            selectComponent(currentCompKey)
        }
    }

    private fun populateFieldsForCurrentComponent() {
        isUpdatingFields = true
        val comp = getCurrentComponentConfig()

        binding.tvInspectorHeader.text = "4. INSPECTOR: ${comp.name} [${comp.id}]"

        // Appearance
        binding.etBackgroundColor.setText(comp.appearance.backgroundColor ?: "")
        binding.etCornerRadius.setText(comp.appearance.cornerRadius?.toString() ?: "")
        binding.etStrokeWidth.setText(comp.appearance.strokeWidth?.toString() ?: "")
        binding.etStrokeColor.setText(comp.appearance.strokeColor ?: "")
        binding.etElevation.setText(comp.appearance.elevation?.toString() ?: "")
        binding.etOpacity.setText(comp.appearance.opacity?.toString() ?: "")

        val swatchColor = UiStudioEngine.parseColorSafe(comp.appearance.backgroundColor) ?: Color.DKGRAY
        binding.swatchBgColor.background = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(swatchColor)
        }

        // Material / Glass Controls (Visible Blur Slider)
        val blurVal = (comp.material.blurRadius ?: 0).coerceIn(0, 35)
        binding.sliderBlurRadius.value = blurVal.toFloat()
        binding.tvBlurLabel.text = "Blur: $blurVal px"

        val matOpacity = (comp.material.materialOpacity ?: 1.0f).coerceIn(0.0f, 1.0f)
        binding.sliderMaterialOpacity.value = matOpacity
        binding.tvMaterialOpacityLabel.text = "Material Opacity: ${(matOpacity * 100).toInt()}%"

        binding.etTintColor.setText(comp.material.tintColor ?: "")
        binding.etTintOpacity.setText(comp.material.tintOpacity?.toString() ?: "")

        // Layout
        binding.etLayoutWidth.setText(comp.layout.width ?: "")
        binding.etLayoutHeight.setText(comp.layout.height ?: "")
        binding.etMarginTop.setText(comp.layout.marginTop?.toString() ?: "")
        binding.etMarginBottom.setText(comp.layout.marginBottom?.toString() ?: "")
        binding.etMarginStart.setText(comp.layout.marginStart?.toString() ?: "")
        binding.etMarginEnd.setText(comp.layout.marginEnd?.toString() ?: "")

        binding.etPaddingTop.setText(comp.layout.paddingTop?.toString() ?: "")
        binding.etPaddingBottom.setText(comp.layout.paddingBottom?.toString() ?: "")
        binding.etPaddingStart.setText(comp.layout.paddingStart?.toString() ?: "")
        binding.etPaddingEnd.setText(comp.layout.paddingEnd?.toString() ?: "")

        // Typography
        binding.etTextColor.setText(comp.typography.textColor ?: "")
        binding.etTextSize.setText(comp.typography.textSize?.toString() ?: "")
        binding.etTextStyle.setText(comp.typography.textStyle ?: "")
        binding.etTextAlign.setText(comp.typography.textAlign ?: "")

        // Content & Visibility
        binding.switchVisible.isChecked = comp.visible
        binding.switchEnabled.isChecked = comp.enabled
        binding.etContentTitle.setText(comp.content.title ?: "")
        binding.etContentSubtitle.setText(comp.content.subtitle ?: "")
        binding.etContentHint.setText(comp.content.hint ?: "")

        // Actions & Routing
        val actionTypeIndex = when (comp.actions.actionType.lowercase()) {
            "open_screen" -> 1
            "go_back" -> 2
            "open_url" -> 3
            else -> 0
        }
        binding.spActionType.setSelection(actionTypeIndex)
        binding.etActionTarget.setText(comp.actions.actionTarget ?: "")

        // Animation
        binding.switchAnimationEnabled.isChecked = comp.animation.enabled
        val animTypeIndex = when (comp.animation.type.lowercase()) {
            "fade" -> 1
            "scale" -> 2
            "fade_scale" -> 3
            "slide" -> 4
            "pop" -> 5
            else -> 0
        }
        binding.spAnimationType.setSelection(animTypeIndex)
        binding.etAnimDuration.setText(comp.animation.durationMs.toString())
        binding.etAnimDelay.setText(comp.animation.delayMs.toString())

        isUpdatingFields = false
    }

    private fun setupFieldListeners() {
        fun addSimpleWatcher(editText: EditText, action: (String) -> Unit) {
            editText.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    if (!isUpdatingFields) {
                        action(s?.toString()?.trim() ?: "")
                    }
                }
            })
        }

        // Appearance
        addSimpleWatcher(binding.etBackgroundColor) { hex ->
            updateCurrentComponentConfig { comp ->
                comp.copy(appearance = comp.appearance.copy(backgroundColor = if (hex.isBlank()) null else hex))
            }
        }
        addSimpleWatcher(binding.etCornerRadius) { v ->
            updateCurrentComponentConfig { comp -> comp.copy(appearance = comp.appearance.copy(cornerRadius = v.toIntOrNull())) }
        }
        addSimpleWatcher(binding.etStrokeWidth) { v ->
            updateCurrentComponentConfig { comp -> comp.copy(appearance = comp.appearance.copy(strokeWidth = v.toIntOrNull())) }
        }
        addSimpleWatcher(binding.etStrokeColor) { hex ->
            updateCurrentComponentConfig { comp -> comp.copy(appearance = comp.appearance.copy(strokeColor = if (hex.isBlank()) null else hex)) }
        }
        addSimpleWatcher(binding.etElevation) { v ->
            updateCurrentComponentConfig { comp -> comp.copy(appearance = comp.appearance.copy(elevation = v.toIntOrNull())) }
        }
        addSimpleWatcher(binding.etOpacity) { v ->
            updateCurrentComponentConfig { comp -> comp.copy(appearance = comp.appearance.copy(opacity = v.toFloatOrNull())) }
        }

        // Material Blur Sliders
        binding.sliderBlurRadius.addOnChangeListener { _, value, fromUser ->
            if (fromUser && !isUpdatingFields) {
                val intVal = value.toInt()
                binding.tvBlurLabel.text = "Blur: $intVal px"
                updateCurrentComponentConfig { comp ->
                    comp.copy(material = comp.material.copy(blurRadius = intVal))
                }
            }
        }
        binding.sliderMaterialOpacity.addOnChangeListener { _, value, fromUser ->
            if (fromUser && !isUpdatingFields) {
                binding.tvMaterialOpacityLabel.text = "Material Opacity: ${(value * 100).toInt()}%"
                updateCurrentComponentConfig { comp ->
                    comp.copy(material = comp.material.copy(materialOpacity = value))
                }
            }
        }
        addSimpleWatcher(binding.etTintColor) { hex ->
            updateCurrentComponentConfig { comp -> comp.copy(material = comp.material.copy(tintColor = if (hex.isBlank()) null else hex)) }
        }
        addSimpleWatcher(binding.etTintOpacity) { v ->
            updateCurrentComponentConfig { comp -> comp.copy(material = comp.material.copy(tintOpacity = v.toFloatOrNull())) }
        }

        // Layout
        addSimpleWatcher(binding.etLayoutWidth) { w ->
            updateCurrentComponentConfig { comp -> comp.copy(layout = comp.layout.copy(width = if (w.isBlank()) null else w)) }
        }
        addSimpleWatcher(binding.etLayoutHeight) { h ->
            updateCurrentComponentConfig { comp -> comp.copy(layout = comp.layout.copy(height = if (h.isBlank()) null else h)) }
        }
        addSimpleWatcher(binding.etMarginTop) { v ->
            updateCurrentComponentConfig { comp -> comp.copy(layout = comp.layout.copy(marginTop = v.toIntOrNull())) }
        }
        addSimpleWatcher(binding.etMarginBottom) { v ->
            updateCurrentComponentConfig { comp -> comp.copy(layout = comp.layout.copy(marginBottom = v.toIntOrNull())) }
        }
        addSimpleWatcher(binding.etMarginStart) { v ->
            updateCurrentComponentConfig { comp -> comp.copy(layout = comp.layout.copy(marginStart = v.toIntOrNull())) }
        }
        addSimpleWatcher(binding.etMarginEnd) { v ->
            updateCurrentComponentConfig { comp -> comp.copy(layout = comp.layout.copy(marginEnd = v.toIntOrNull())) }
        }

        addSimpleWatcher(binding.etPaddingTop) { v ->
            updateCurrentComponentConfig { comp -> comp.copy(layout = comp.layout.copy(paddingTop = v.toIntOrNull())) }
        }
        addSimpleWatcher(binding.etPaddingBottom) { v ->
            updateCurrentComponentConfig { comp -> comp.copy(layout = comp.layout.copy(paddingBottom = v.toIntOrNull())) }
        }
        addSimpleWatcher(binding.etPaddingStart) { v ->
            updateCurrentComponentConfig { comp -> comp.copy(layout = comp.layout.copy(paddingStart = v.toIntOrNull())) }
        }
        addSimpleWatcher(binding.etPaddingEnd) { v ->
            updateCurrentComponentConfig { comp -> comp.copy(layout = comp.layout.copy(paddingEnd = v.toIntOrNull())) }
        }

        // Typography
        addSimpleWatcher(binding.etTextColor) { hex ->
            updateCurrentComponentConfig { comp -> comp.copy(typography = comp.typography.copy(textColor = if (hex.isBlank()) null else hex)) }
        }
        addSimpleWatcher(binding.etTextSize) { v ->
            updateCurrentComponentConfig { comp -> comp.copy(typography = comp.typography.copy(textSize = v.toIntOrNull())) }
        }
        addSimpleWatcher(binding.etTextStyle) { s ->
            updateCurrentComponentConfig { comp -> comp.copy(typography = comp.typography.copy(textStyle = if (s.isBlank()) null else s)) }
        }
        addSimpleWatcher(binding.etTextAlign) { a ->
            updateCurrentComponentConfig { comp -> comp.copy(typography = comp.typography.copy(textAlign = if (a.isBlank()) null else a)) }
        }

        // Content & Visibility
        binding.switchVisible.setOnCheckedChangeListener { _, isChecked ->
            if (!isUpdatingFields) {
                updateCurrentComponentConfig { comp -> comp.copy(visible = isChecked) }
            }
        }
        binding.switchEnabled.setOnCheckedChangeListener { _, isChecked ->
            if (!isUpdatingFields) {
                updateCurrentComponentConfig { comp -> comp.copy(enabled = isChecked) }
            }
        }
        addSimpleWatcher(binding.etContentTitle) { t ->
            updateCurrentComponentConfig { comp -> comp.copy(content = comp.content.copy(title = if (t.isBlank()) null else t)) }
        }
        addSimpleWatcher(binding.etContentSubtitle) { s ->
            updateCurrentComponentConfig { comp -> comp.copy(content = comp.content.copy(subtitle = if (s.isBlank()) null else s)) }
        }
        addSimpleWatcher(binding.etContentHint) { h ->
            updateCurrentComponentConfig { comp -> comp.copy(content = comp.content.copy(hint = if (h.isBlank()) null else h)) }
        }

        // Actions & Animation
        addSimpleWatcher(binding.etActionTarget) { t ->
            updateCurrentComponentConfig { comp -> comp.copy(actions = comp.actions.copy(actionTarget = if (t.isBlank()) null else t)) }
        }
        binding.switchAnimationEnabled.setOnCheckedChangeListener { _, isChecked ->
            if (!isUpdatingFields) {
                updateCurrentComponentConfig { comp -> comp.copy(animation = comp.animation.copy(enabled = isChecked)) }
            }
        }
        addSimpleWatcher(binding.etAnimDuration) { d ->
            updateCurrentComponentConfig { comp -> comp.copy(animation = comp.animation.copy(durationMs = d.toLongOrNull() ?: 300L)) }
        }
        addSimpleWatcher(binding.etAnimDelay) { d ->
            updateCurrentComponentConfig { comp -> comp.copy(animation = comp.animation.copy(delayMs = d.toLongOrNull() ?: 0L)) }
        }
    }

    private fun showAddComponentDialog() {
        val typeOptions = UiStudioRegistry.COMPONENT_TYPES.map { "${it.displayName} (${it.type})" }
        val dialogView = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 20, 40, 20)
        }

        val spType = Spinner(this).apply {
            adapter = ArrayAdapter(this@UiStudioActivity, android.R.layout.simple_spinner_dropdown_item, typeOptions)
        }
        val etName = EditText(this).apply {
            hint = "Display Name (e.g. Featured Banner)"
        }
        val etId = EditText(this).apply {
            hint = "Stable ID (lowercase letters & underscores)"
        }

        dialogView.addView(TextView(this).apply { text = "Select Component Primitive:" })
        dialogView.addView(spType)
        dialogView.addView(etName)
        dialogView.addView(etId)

        MaterialAlertDialogBuilder(this)
            .setTitle("Add UI Component")
            .setView(dialogView)
            .setPositiveButton("Add") { _, _ ->
                val selectedType = UiStudioRegistry.COMPONENT_TYPES[spType.selectedItemPosition].type
                val name = etName.text.toString().trim().ifBlank { "New Component" }
                val rawId = etId.text.toString().trim().lowercase().replace(" ", "_")
                val uniqueId = if (rawId.isNotBlank()) rawId else "${selectedType}_${System.currentTimeMillis() % 10000}"

                pushUndoState()
                val newComp = UiStudioRegistry.createDefaultComponent(uniqueId, selectedType, name)
                val screen = activeConfig.screens[currentScreenKey] ?: ScreenConfig(id = currentScreenKey)
                val updatedComponents = screen.components.toMutableMap().apply { put(uniqueId, newComp) }
                val updatedScreen = screen.copy(components = updatedComponents)
                val updatedScreens = activeConfig.screens.toMutableMap().apply { put(currentScreenKey, updatedScreen) }

                activeConfig = activeConfig.copy(screens = updatedScreens)
                repo.saveDraftToLocalCache(activeConfig)

                currentCompKey = uniqueId
                updateComponentDropdownAndTree()
                renderRealScreenCanvas()
                selectComponent(uniqueId)
                AppBulletin.showSuccess(this, "Added component '$name'")
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun duplicateCurrentComponent() {
        val screen = activeConfig.screens[currentScreenKey] ?: return
        val comp = screen.components[currentCompKey] ?: return

        pushUndoState()
        val newId = "${comp.id}_copy_${System.currentTimeMillis() % 1000}"
        val newComp = comp.copy(
            id = newId,
            name = "${comp.name} (Copy)",
            isProtected = false,
            order = comp.order + 1
        )

        val updatedComponents = screen.components.toMutableMap().apply { put(newId, newComp) }
        val updatedScreen = screen.copy(components = updatedComponents)
        val updatedScreens = activeConfig.screens.toMutableMap().apply { put(currentScreenKey, updatedScreen) }

        activeConfig = activeConfig.copy(screens = updatedScreens)
        repo.saveDraftToLocalCache(activeConfig)

        currentCompKey = newId
        updateComponentDropdownAndTree()
        renderRealScreenCanvas()
        selectComponent(newId)
        AppBulletin.showSuccess(this, "Duplicated component '${comp.name}'")
    }

    private fun deleteCurrentComponent() {
        val screen = activeConfig.screens[currentScreenKey] ?: return
        val comp = screen.components[currentCompKey] ?: return

        if (comp.isProtected) {
            MaterialAlertDialogBuilder(this)
                .setTitle("Protected Component")
                .setMessage("Component '${comp.name}' is required for application functionality and cannot be deleted. You may set its Visibility to 'Gone' instead.")
                .setPositiveButton("OK", null)
                .show()
            return
        }

        MaterialAlertDialogBuilder(this)
            .setTitle("Delete Component")
            .setMessage("Are you sure you want to delete '${comp.name}' from this screen?")
            .setPositiveButton("Delete") { _, _ ->
                pushUndoState()
                val updatedComponents = screen.components.toMutableMap().apply { remove(currentCompKey) }
                val updatedScreen = screen.copy(components = updatedComponents)
                val updatedScreens = activeConfig.screens.toMutableMap().apply { put(currentScreenKey, updatedScreen) }

                activeConfig = activeConfig.copy(screens = updatedScreens)
                repo.saveDraftToLocalCache(activeConfig)

                currentCompKey = updatedComponents.keys.firstOrNull() ?: ""
                updateComponentDropdownAndTree()
                renderRealScreenCanvas()
                if (currentCompKey.isNotBlank()) selectComponent(currentCompKey)
                AppBulletin.showSuccess(this, "Deleted component '${comp.name}'")
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun reorderComponent(isUp: Boolean) {
        val screen = activeConfig.screens[currentScreenKey] ?: return
        val keys = screen.components.keys.toList()
        val index = keys.indexOf(currentCompKey)
        if (index == -1) return

        val targetIndex = if (isUp) index - 1 else index + 1
        if (targetIndex !in keys.indices) return

        pushUndoState()
        val entries = screen.components.entries.toList().toMutableList()
        val item = entries.removeAt(index)
        entries.add(targetIndex, item)

        val reorderedMap = linkedMapOf<String, ComponentConfig>()
        entries.forEachIndexed { i, entry ->
            reorderedMap[entry.key] = entry.value.copy(order = i + 1)
        }

        val updatedScreen = screen.copy(components = reorderedMap)
        val updatedScreens = activeConfig.screens.toMutableMap().apply { put(currentScreenKey, updatedScreen) }

        activeConfig = activeConfig.copy(screens = updatedScreens)
        repo.saveDraftToLocalCache(activeConfig)

        updateComponentDropdownAndTree()
        renderRealScreenCanvas()
        selectComponent(currentCompKey)
    }

    private fun setupBottomActions() {
        binding.btnSaveDraft.setOnClickListener { saveDraft() }
        binding.btnValidate.setOnClickListener { validateCurrentDraft() }
        binding.btnPublish.setOnClickListener { promptPublish() }
    }

    private fun saveDraft() {
        val validation = repo.validateConfig(activeConfig)
        if (!validation.first) {
            AppBulletin.showError(this, "Validation issue: ${validation.second.firstOrNull()}")
            return
        }

        lifecycleScope.launch {
            binding.btnSaveDraft.isEnabled = false
            val result = repo.saveDraft(activeConfig)
            binding.btnSaveDraft.isEnabled = true
            result.onSuccess {
                binding.tvStudioStatus.text = "Draft Saved Successfully"
                AppBulletin.showSuccess(this@UiStudioActivity, "Draft saved and persisted successfully")
            }.onFailure { e ->
                AppBulletin.showError(this@UiStudioActivity, "Failed to save draft: ${e.localizedMessage}")
            }
        }
    }

    private fun validateCurrentDraft() {
        val validation = repo.validateConfig(activeConfig)
        if (validation.first) {
            AppBulletin.showSuccess(this, "Validation passed: 100% compliant schema")
        } else {
            MaterialAlertDialogBuilder(this)
                .setTitle("Validation Issues")
                .setMessage(validation.second.joinToString("\n• ", prefix = "• "))
                .setPositiveButton("OK", null)
                .show()
        }
    }

    private fun promptPublish() {
        val validation = repo.validateConfig(activeConfig)
        if (!validation.first) {
            AppBulletin.showError(this, "Cannot publish: Fix schema errors first")
            return
        }

        val input = EditText(this).apply {
            hint = "Changelog notes (e.g. Master App Builder redesign)"
            setPadding(40, 30, 40, 30)
        }

        MaterialAlertDialogBuilder(this)
            .setTitle("Publish Live Configuration")
            .setMessage("This will push your changes live to all student devices.")
            .setView(input)
            .setPositiveButton("Publish Live") { _, _ ->
                val notes = input.text.toString().trim()
                executePublish(notes)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun executePublish(notes: String) {
        lifecycleScope.launch {
            binding.btnPublish.isEnabled = false
            val result = repo.publish(notes, activeConfig)
            binding.btnPublish.isEnabled = true
            result.onSuccess { version ->
                activeConfig = activeConfig.copy(version = version, status = "published")
                binding.tvStudioStatus.text = "Published v$version"
                AppBulletin.showSuccess(this@UiStudioActivity, "Published v$version successfully to all devices!")
            }.onFailure { e ->
                AppBulletin.showError(this@UiStudioActivity, "Publish failed: ${e.localizedMessage}")
            }
        }
    }

    private fun showResetOptionsDialog() {
        val options = arrayOf(
            "Reset Selected Component to Native Default",
            "Reset Current Screen to Native Default",
            "Reset Working Draft to Published Config",
            "Emergency Factory Reset (All Screens)"
        )

        MaterialAlertDialogBuilder(this)
            .setTitle("Reset & Fallback Options")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> resetCurrentComponentToDefault()
                    1 -> resetCurrentScreenToDefault()
                    2 -> resetDraftToPublished()
                    3 -> emergencyFactoryReset()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun resetCurrentComponentToDefault() {
        val defaultTemplate = repo.getDefaultTemplate()
        val defaultComp = defaultTemplate.screens[currentScreenKey]?.components?.get(currentCompKey)
        if (defaultComp != null) {
            pushUndoState()
            updateCurrentComponentConfig { defaultComp }
            populateFieldsForCurrentComponent()
            AppBulletin.showSuccess(this, "Reset '$currentCompKey' to native default")
        } else {
            AppBulletin.showError(this, "No default template found for component '$currentCompKey'")
        }
    }

    private fun resetCurrentScreenToDefault() {
        val defaultTemplate = repo.getDefaultTemplate()
        val defaultScreen = defaultTemplate.screens[currentScreenKey]
        if (defaultScreen != null) {
            pushUndoState()
            val updatedScreens = activeConfig.screens.toMutableMap().apply { put(currentScreenKey, defaultScreen) }
            activeConfig = activeConfig.copy(screens = updatedScreens)
            repo.saveDraftToLocalCache(activeConfig)
            updateComponentDropdownAndTree()
            renderRealScreenCanvas()
            populateFieldsForCurrentComponent()
            AppBulletin.showSuccess(this, "Reset screen '$currentScreenKey' to native default")
        }
    }

    private fun resetDraftToPublished() {
        lifecycleScope.launch {
            val published = repo.loadCachedConfig()
            if (published != null && published.screens.isNotEmpty()) {
                pushUndoState()
                activeConfig = published
                repo.saveDraftToLocalCache(activeConfig)
                updateComponentDropdownAndTree()
                renderRealScreenCanvas()
                populateFieldsForCurrentComponent()
                AppBulletin.showSuccess(this@UiStudioActivity, "Restored published configuration")
            } else {
                AppBulletin.showError(this@UiStudioActivity, "No published configuration exists on device")
            }
        }
    }

    private fun emergencyFactoryReset() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Confirm Emergency Reset")
            .setMessage("This will reset all screens, layouts, and components back to factory defaults.")
            .setPositiveButton("Reset Everything") { _, _ ->
                pushUndoState()
                activeConfig = repo.getDefaultTemplate()
                repo.saveDraftToLocalCache(activeConfig)
                updateComponentDropdownAndTree()
                renderRealScreenCanvas()
                populateFieldsForCurrentComponent()
                AppBulletin.showSuccess(this, "Factory reset complete")
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showVersionHistoryDialog() {
        lifecycleScope.launch {
            val result = repo.getVersions()
            result.onSuccess { versions ->
                if (versions.isEmpty()) {
                    AppBulletin.show(this@UiStudioActivity, "No historical versions found yet")
                    return@onSuccess
                }

                val items = versions.map { v ->
                    "v${v.version} • ${v.notes ?: "Published update"} (${v.createdBy})"
                }.toTypedArray()

                MaterialAlertDialogBuilder(this@UiStudioActivity)
                    .setTitle("Version Rollback & History")
                    .setItems(items) { _, which ->
                        val selectedVer = versions[which].version
                        promptRollbackVersion(selectedVer)
                    }
                    .setNegativeButton("Close", null)
                    .show()
            }.onFailure { e ->
                AppBulletin.showError(this@UiStudioActivity, "Failed to load versions: ${e.localizedMessage}")
            }
        }
    }

    private fun promptRollbackVersion(versionId: Int) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Rollback to v$versionId")
            .setMessage("Do you want to restore v$versionId into your active working draft?")
            .setPositiveButton("Restore to Draft") { _, _ ->
                lifecycleScope.launch {
                    pushUndoState()
                    val result = repo.restoreVersion(versionId, target = "draft")
                    result.onSuccess {
                        AppBulletin.showSuccess(this@UiStudioActivity, "Restored v$versionId into working draft")
                        loadDraftFromRepository()
                    }.onFailure { e ->
                        AppBulletin.showError(this@UiStudioActivity, "Rollback failed: ${e.localizedMessage}")
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showExportImportDialog() {
        val options = arrayOf("Copy Configuration JSON to Clipboard", "Import Configuration from JSON")
        MaterialAlertDialogBuilder(this)
            .setTitle("JSON Data Interchange")
            .setItems(options) { _, which ->
                if (which == 0) {
                    val json = repo.exportToJson(activeConfig)
                    val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("EVE_UI_Studio_Config", json))
                    AppBulletin.showSuccess(this, "Configuration JSON copied to clipboard")
                } else {
                    promptImportJson()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun promptImportJson() {
        val input = EditText(this).apply {
            hint = "Paste UI Studio JSON schema here"
            minLines = 4
            setPadding(30, 20, 30, 20)
        }

        MaterialAlertDialogBuilder(this)
            .setTitle("Import Configuration")
            .setView(input)
            .setPositiveButton("Import") { _, _ ->
                val json = input.text.toString().trim()
                val result = repo.importFromJson(json)
                result.onSuccess { importedConfig ->
                    activeConfig = importedConfig
                    repo.saveDraftToLocalCache(activeConfig)
                    updateComponentDropdownAndTree()
                    renderRealScreenCanvas()
                    populateFieldsForCurrentComponent()
                    AppBulletin.showSuccess(this, "Configuration imported successfully")
                }.onFailure { e ->
                    AppBulletin.showError(this, "Import failed: ${e.localizedMessage}")
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
