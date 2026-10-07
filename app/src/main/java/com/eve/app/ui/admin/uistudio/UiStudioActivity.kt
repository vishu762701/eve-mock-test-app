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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class UiStudioActivity : EveBaseActivity() {

    private lateinit var binding: ActivityUiStudioBinding
    private val repo = UiStudioRepository.getInstance()

    private var activeConfig: UiStudioConfig = repo.getDefaultTemplate()
    private var isUpdatingFields = false
    private var hasUnsavedChanges = false

    private val supportedScreens = UiStudioRegistry.SUPPORTED_SCREENS

    private var currentScreenKey = "home"
    private var currentCompKey = "hero_banner"
    private var currentActiveTab = "design"
    private var currentViewMode = "split" // "split", "canvas", "inspector"
    private var currentDeviceWidthMode = "normal" // "compact", "normal", "large"

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

    // Canvas interaction & zoom state
    private var isInteractMode = false
    private var zoomIndex = 0
    private val zoomLevels = listOf(1.0f, 0.85f, 1.25f)
    private val zoomLabels = listOf("FIT", "85%", "125%")

    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUiStudioBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupViewModeButtons()
        setupCanvasControls()
        setupInspectorTabs()
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
        // Save current working configuration and session state to local cache for 100% persistence
        repo.saveDraftToLocalCache(activeConfig)
        repo.saveSessionState(
            UiStudioSessionState(
                selectedScreenKey = currentScreenKey,
                selectedComponentKey = currentCompKey,
                selectedTab = currentActiveTab,
                viewMode = currentViewMode,
                deviceWidthMode = currentDeviceWidthMode,
                timestamp = System.currentTimeMillis()
            )
        )
    }

    private fun setupToolbar() {
        binding.btnBack.setOnClickListener { finish() }
        binding.btnUndo.setOnClickListener { performUndo() }
        binding.btnRedo.setOnClickListener { performRedo() }
        binding.btnCopyStyle.setOnClickListener { performCopyStyle() }
        binding.btnPasteStyle.setOnClickListener { performPasteStyle() }
        binding.btnVersionHistory.setOnClickListener { showVersionHistoryDialog() }
        binding.btnExportImport.setOnClickListener { showExportImportDialog() }
        binding.btnResetMenu.setOnClickListener { showResetOptionsDialog() }
    }

    private fun setupViewModeButtons() {
        binding.btnModeSplit.setOnClickListener { setViewMode("split") }
        binding.btnModeCanvas.setOnClickListener { setViewMode("canvas") }
        binding.btnModeInspector.setOnClickListener { setViewMode("inspector") }
        binding.btnDeviceWidth.setOnClickListener { toggleDeviceWidth() }
    }

    private fun setupCanvasControls() {
        binding.btnInteractionMode.setOnClickListener {
            isInteractMode = !isInteractMode
            if (isInteractMode) {
                binding.btnInteractionMode.text = "INTERACT"
                binding.btnInteractionMode.setTextColor(ContextCompat.getColor(this, R.color.eve_green))
                AppBulletin.show(this, "Interact Mode: Tap items to interact")
            } else {
                binding.btnInteractionMode.text = "EDIT MODE"
                binding.btnInteractionMode.setTextColor(ContextCompat.getColor(this, R.color.eve_primary))
                AppBulletin.show(this, "Edit Mode: Tap items to inspect")
            }
        }

        binding.btnZoomFit.setOnClickListener {
            zoomIndex = 0
            val scale = zoomLevels[zoomIndex]
            binding.btnZoomFit.text = zoomLabels[zoomIndex]
            binding.canvasScreenContent.pivotX = 0f
            binding.canvasScreenContent.pivotY = 0f
            binding.canvasScreenContent.scaleX = scale
            binding.canvasScreenContent.scaleY = scale
        }

        binding.btnZoomIn.setOnClickListener {
            val cur = binding.canvasScreenContent.scaleX
            val next = (cur + 0.15f).coerceAtMost(2.0f)
            binding.canvasScreenContent.pivotX = 0f
            binding.canvasScreenContent.pivotY = 0f
            binding.canvasScreenContent.scaleX = next
            binding.canvasScreenContent.scaleY = next
            binding.btnZoomFit.text = "${(next * 100).toInt()}%"
        }

        binding.btnZoomOut.setOnClickListener {
            val cur = binding.canvasScreenContent.scaleX
            val next = (cur - 0.15f).coerceAtLeast(0.5f)
            binding.canvasScreenContent.pivotX = 0f
            binding.canvasScreenContent.pivotY = 0f
            binding.canvasScreenContent.scaleX = next
            binding.canvasScreenContent.scaleY = next
            binding.btnZoomFit.text = "${(next * 100).toInt()}%"
        }

        binding.btnFocus.setOnClickListener {
            if (currentViewMode == "canvas") {
                setViewMode("split")
                binding.btnFocus.text = "FOCUS"
            } else {
                setViewMode("canvas")
                binding.btnFocus.text = "SPLIT"
            }
        }
    }

    private fun setViewMode(mode: String) {
        currentViewMode = mode
        val density = resources.displayMetrics.density

        when (mode) {
            "canvas" -> {
                binding.layoutCanvasSection.visibility = View.VISIBLE
                val lp = binding.layoutCanvasSection.layoutParams as LinearLayout.LayoutParams
                lp.height = 0
                lp.weight = 1.0f
                binding.layoutCanvasSection.layoutParams = lp

                binding.layoutInspectorSection.visibility = View.GONE
            }
            "inspector" -> {
                binding.layoutCanvasSection.visibility = View.GONE
                binding.layoutInspectorSection.visibility = View.VISIBLE
                val lp = binding.layoutInspectorSection.layoutParams as LinearLayout.LayoutParams
                lp.height = 0
                lp.weight = 1.0f
                binding.layoutInspectorSection.layoutParams = lp
            }
            else -> { // "split"
                binding.layoutCanvasSection.visibility = View.VISIBLE
                val lpCanvas = binding.layoutCanvasSection.layoutParams as LinearLayout.LayoutParams
                lpCanvas.height = (230 * density).toInt()
                lpCanvas.weight = 0.0f
                binding.layoutCanvasSection.layoutParams = lpCanvas

                binding.layoutInspectorSection.visibility = View.VISIBLE
                val lpInsp = binding.layoutInspectorSection.layoutParams as LinearLayout.LayoutParams
                lpInsp.height = 0
                lpInsp.weight = 1.0f
                binding.layoutInspectorSection.layoutParams = lpInsp
            }
        }
        highlightActiveModeButton()
    }

    private fun highlightActiveModeButton() {
        binding.btnModeSplit.alpha = if (currentViewMode == "split") 1.0f else 0.5f
        binding.btnModeCanvas.alpha = if (currentViewMode == "canvas") 1.0f else 0.5f
        binding.btnModeInspector.alpha = if (currentViewMode == "inspector") 1.0f else 0.5f
    }

    private fun toggleDeviceWidth() {
        currentDeviceWidthMode = when (currentDeviceWidthMode) {
            "compact" -> "normal"
            "normal" -> "large"
            else -> "compact"
        }
        applyDeviceWidthMode()
    }

    private fun applyDeviceWidthMode() {
        val density = resources.displayMetrics.density
        val targetWidth = when (currentDeviceWidthMode) {
            "compact" -> (360 * density).toInt()
            "normal" -> (400 * density).toInt()
            else -> ViewGroup.LayoutParams.MATCH_PARENT
        }
        val label = when (currentDeviceWidthMode) {
            "compact" -> "360dp"
            "normal" -> "400dp"
            else -> "Full"
        }
        binding.btnDeviceWidth.text = label

        val lp = binding.canvasContainer.layoutParams
        lp.width = targetWidth
        binding.canvasContainer.layoutParams = lp
    }

    private fun setupInspectorTabs() {
        val tabButtons = listOf(
            binding.tabDesign to "design",
            binding.tabLayout to "layout",
            binding.tabColors to "colors",
            binding.tabTypography to "typography",
            binding.tabMaterial to "material",
            binding.tabContent to "content",
            binding.tabActions to "actions",
            binding.tabAnimation to "animation",
            binding.tabStates to "states",
            binding.tabBranding to "branding",
            binding.tabTree to "tree",
            binding.tabAdvanced to "advanced"
        )

        tabButtons.forEach { (btn, tabKey) ->
            btn.setOnClickListener {
                selectInspectorTab(tabKey)
            }
        }
        selectInspectorTab("design")
    }

    private fun selectInspectorTab(tabKey: String) {
        currentActiveTab = tabKey

        // Tab views mapping
        val tabContainers = listOf(
            "design" to binding.layoutTabDesign,
            "layout" to binding.layoutTabLayout,
            "colors" to binding.layoutTabColors,
            "typography" to binding.layoutTabTypography,
            "material" to binding.layoutTabMaterial,
            "content" to binding.layoutTabContent,
            "actions" to binding.layoutTabActions,
            "animation" to binding.layoutTabAnimation,
            "states" to binding.layoutTabStates,
            "branding" to binding.layoutTabBranding,
            "tree" to binding.layoutTabTree,
            "advanced" to binding.layoutTabAdvanced
        )

        tabContainers.forEach { (key, layout) ->
            layout.visibility = if (key == tabKey) View.VISIBLE else View.GONE
        }

        // Highlight active tab button
        val tabButtons = listOf(
            "design" to binding.tabDesign,
            "layout" to binding.tabLayout,
            "colors" to binding.tabColors,
            "typography" to binding.tabTypography,
            "material" to binding.tabMaterial,
            "content" to binding.tabContent,
            "actions" to binding.tabActions,
            "animation" to binding.tabAnimation,
            "states" to binding.tabStates,
            "branding" to binding.tabBranding,
            "tree" to binding.tabTree,
            "advanced" to binding.tabAdvanced
        )

        tabButtons.forEach { (key, btn) ->
            if (key == tabKey) {
                btn.alpha = 1.0f
                btn.strokeWidth = UiStudioEngine.dpToPx(this, 2)
            } else {
                btn.alpha = 0.55f
                btn.strokeWidth = UiStudioEngine.dpToPx(this, 1)
            }
        }
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
                        markUnsaved()
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
        binding.etSearchComponent.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val query = s?.toString()?.trim()?.lowercase().orEmpty()
                handleUniversalStudioSearch(query)
                updateComponentDropdownAndTree()
            }
        })

        binding.btnAddElement.setOnClickListener { showAddComponentDialog() }
        binding.btnDuplicateElement.setOnClickListener { duplicateCurrentComponent() }
        binding.btnDeleteElement.setOnClickListener { deleteCurrentComponent() }
        binding.btnMoveUp.setOnClickListener { reorderComponent(isUp = true) }
        binding.btnMoveDown.setOnClickListener { reorderComponent(isUp = false) }

        binding.btnPreviewAnimation.setOnClickListener {
            val comp = getCurrentComponentConfig()
            val view = canvasViewMap[comp.id]
            if (view != null) {
                UiStudioEngine.playEntranceAnimation(view, comp.animation.copy(enabled = true))
            }
        }
    }

    private fun handleUniversalStudioSearch(query: String) {
        if (query.isBlank()) return
        when {
            query.contains("blur") || query.contains("glass") || query.contains("material") -> selectInspectorTab("material")
            query.contains("radius") || query.contains("corner") || query.contains("shape") -> selectInspectorTab("design")
            query.contains("color") || query.contains("background") || query.contains("hex") || query.contains("tint") -> selectInspectorTab("colors")
            query.contains("anim") || query.contains("motion") -> selectInspectorTab("animation")
            query.contains("text") || query.contains("typo") || query.contains("font") -> selectInspectorTab("typography")
            query.contains("layout") || query.contains("margin") || query.contains("padding") -> selectInspectorTab("layout")
            query.contains("action") || query.contains("nav") -> selectInspectorTab("actions")
            query.contains("state") || query.contains("press") -> selectInspectorTab("states")
            query.contains("brand") || query.contains("logo") -> selectInspectorTab("branding")
        }
    }

    private val emojiCategories = linkedMapOf(
        "Smileys" to listOf("😀", "😃", "😄", "😁", "😆", "😅", "😂", "🤣", "😊", "😇", "🙂", "😉", "😍", "🥰", "😘", "😋", "😜", "🤪", "😎", "🤩", "🥳", "😏", "🥺", "😭", "😤", "🤯", "😳", "😱", "🤗", "🤔", "🤫", "😴"),
        "People" to listOf("👋", "✋", "👌", "✌️", "🤞", "🤟", "🤘", "🤙", "👈", "👉", "👆", "👇", "👍", "👎", "👏", "🙌", "👐", "🤲", "🤝", "🙏", "💪", "🧠", "👀", "👁️", "🧑‍🎓", "👨‍🏫", "👩‍💻", "🦸", "🥷"),
        "Animals" to listOf("🐶", "🐱", "🐭", "🐰", "🦊", "🐻", "🐼", "🐨", "🐯", "🦁", "🐮", "🐷", "🐸", "🐵", "🐔", "🐧", "🐦", "🦆", "🦅", "🦉", "🦄", "🐝", "🦋", "🐙", "🐬", "🐳", "🦈", "🐘", "🦒"),
        "Food" to listOf("🍏", "🍎", "🍐", "🍊", "🍋", "🍌", "🍉", "🍇", "🍓", "🍒", "🍑", "🍍", "🥥", "🥝", "🍅", "🥑", "🥦", "🌽", "🥕", "🥐", "🍞", "🧀", "🍳", "🥞", "🍔", "🍟", "🍕", "🥗", "🍣", "🍦", "🎂", "☕"),
        "Travel" to listOf("🚗", "🚕", "🚙", "🚌", "🏎️", "🚓", "🚑", "🚒", "🚲", "🛵", "🏍️", "🚨", "✈️", "🚀", "🛸", "🚁", "⛵", "🚢", "⚓", "🚦", "🏖️", "🏝️", "⛰️", "🏕️", "🏠", "🏢", "🏛️", "🏫", "🎡", "🗽"),
        "Activities" to listOf("⚽", "🏀", "🏈", "⚾", "🎾", "🏐", "🏉", "🏓", "🏸", "🥊", "🛹", "🎯", "🎮", "🎲", "🧩", "♟️", "🎨", "🎬", "🎤", "🎧", "🎼", "🎹", "🎸", "🏆", "🥇", "🥈", "🥉", "🏅", "🎖️", "🎟️"),
        "Objects" to listOf("📱", "💻", "⌨️", "🖥️", "📷", "🎥", "📺", "📻", "⏱️", "⏰", "⏳", "🔋", "💡", "🔦", "💸", "💵", "💰", "💳", "💎", "⚖️", "🔧", "🔨", "🛡️", "💊", "🧬", "🧪", "📦", "📚", "📖", "✏️", "🔍", "🔒"),
        "Symbols" to listOf("❤️", "🧡", "💛", "💚", "💙", "💜", "🖤", "🤍", "💯", "💢", "💬", "💭", "✨", "⭐", "🌟", "⚡", "🔥", "💧", "🌈", "🎉", "🎊", "🚩", "✓", "✕", "⚠️", "⛔", "🟢", "🔴", "🔵", "🟡", "🟣"),
        "Flags" to listOf("🇮🇳", "🇺🇸", "🇬🇧", "🇨🇦", "🇦🇺", "🇩🇪", "🇫🇷", "🇯🇵", "🇰🇷", "🇧🇷", "🇷🇺", "🇨🇳", "🇿🇦", "🏁", "🚩", "🎌", "🏴", "🏳️")
    )

    private fun showEmojiPickerDialog() {
        val dialogView = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(UiStudioEngine.dpToPx(context, 16), UiStudioEngine.dpToPx(context, 12), UiStudioEngine.dpToPx(context, 16), UiStudioEngine.dpToPx(context, 12))
        }

        val categorySpinner = Spinner(this).apply {
            adapter = ArrayAdapter(this@UiStudioActivity, android.R.layout.simple_spinner_dropdown_item, emojiCategories.keys.toList())
        }
        dialogView.addView(categorySpinner)

        val emojiScrollView = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, UiStudioEngine.dpToPx(context, 260)).apply {
                topMargin = UiStudioEngine.dpToPx(context, 8)
            }
        }
        val emojiGridLayout = GridLayout(this).apply {
            columnCount = 6
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
        emojiScrollView.addView(emojiGridLayout)
        dialogView.addView(emojiScrollView)

        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle("Select Emoji")
            .setView(dialogView)
            .setNegativeButton("Cancel", null)
            .create()

        fun loadCategory(category: String) {
            emojiGridLayout.removeAllViews()
            val emojis = emojiCategories[category].orEmpty()
            for (emoji in emojis) {
                val tv = TextView(this).apply {
                    text = emoji
                    textSize = 22f
                    gravity = Gravity.CENTER
                    val cellSize = UiStudioEngine.dpToPx(context, 44)
                    layoutParams = GridLayout.LayoutParams().apply {
                        width = cellSize
                        height = cellSize
                    }
                    background = ContextCompat.getDrawable(context, R.drawable.bg_circle_translucent)
                    setOnClickListener {
                        val currentText = binding.etContentTitle.text?.toString() ?: ""
                        binding.etContentTitle.setText(if (currentText.isBlank()) emoji else "$currentText $emoji")
                        dialog.dismiss()
                    }
                }
                emojiGridLayout.addView(tv)
            }
        }

        categorySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val cat = emojiCategories.keys.toList()[position]
                loadCategory(cat)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        loadCategory(emojiCategories.keys.first())
        dialog.show()
    }

    private fun loadDraftFromRepository() {
        lifecycleScope.launch {
            binding.tvStudioStatus.text = "Loading saved configuration..."
            binding.tvPersistentStatus.text = "LOADING DRAFT..."

            val result = repo.getDraft()
            result.onSuccess { draft ->
                activeConfig = if (draft.screens.isNotEmpty()) draft else repo.getDefaultTemplate()
                updateStudioStatusBadges()

                // Check and restore editor session state
                val session = repo.loadSessionState()
                if (session != null && activeConfig.screens.containsKey(session.selectedScreenKey)) {
                    currentScreenKey = session.selectedScreenKey
                    val screenIndex = supportedScreens.indexOfFirst { it.id == currentScreenKey }
                    if (screenIndex >= 0) binding.spScreenSelector.setSelection(screenIndex)

                    val screen = activeConfig.screens[currentScreenKey]
                    currentCompKey = if (screen?.components?.containsKey(session.selectedComponentKey) == true) {
                        session.selectedComponentKey
                    } else {
                        screen?.components?.keys?.firstOrNull() ?: ""
                    }
                    currentActiveTab = session.selectedTab
                    currentViewMode = session.viewMode
                    currentDeviceWidthMode = session.deviceWidthMode
                } else {
                    val screenKeys = activeConfig.screens.keys.toList()
                    if (screenKeys.isNotEmpty()) {
                        currentScreenKey = screenKeys[0]
                        val screenIndex = supportedScreens.indexOfFirst { it.id == currentScreenKey }
                        if (screenIndex >= 0) binding.spScreenSelector.setSelection(screenIndex)
                        currentCompKey = activeConfig.screens[currentScreenKey]?.components?.keys?.firstOrNull() ?: ""
                    }
                }

                setViewMode(currentViewMode)
                applyDeviceWidthMode()
                selectInspectorTab(currentActiveTab)
                updateScreenTransitionSelection()
                updateComponentDropdownAndTree()
                renderRealScreenCanvas()
                populateFieldsForCurrentComponent()
                updateUndoRedoButtonState()
            }.onFailure {
                activeConfig = repo.loadCachedDraft() ?: repo.loadCachedConfig() ?: repo.getDefaultTemplate()
                binding.tvPersistentStatus.text = "OFFLINE LOCAL DRAFT"
                binding.tvStudioStatus.text = "Working Offline Draft"
                updateScreenTransitionSelection()
                updateComponentDropdownAndTree()
                renderRealScreenCanvas()
                populateFieldsForCurrentComponent()
                updateUndoRedoButtonState()
            }
        }
    }

    private fun updateStudioStatusBadges() {
        if (activeConfig.status == "published" && activeConfig.version > 0) {
            binding.tvPersistentStatus.text = "★ LIVE v${activeConfig.version} (VERIFIED)"
            binding.tvPersistentStatus.setTextColor(ContextCompat.getColor(this, R.color.eve_green))
            binding.tvStudioStatus.text = "Published v${activeConfig.version} • Live on all devices"
        } else if (hasUnsavedChanges) {
            binding.tvPersistentStatus.text = "● UNSAVED CHANGES"
            binding.tvPersistentStatus.setTextColor(ContextCompat.getColor(this, R.color.eve_system_yellow))
            binding.tvStudioStatus.text = "Working Draft (Unsaved)"
        } else {
            val vStr = if (activeConfig.version > 0) "v${activeConfig.version}" else "v1"
            binding.tvPersistentStatus.text = "DRAFT $vStr • SAVED"
            binding.tvPersistentStatus.setTextColor(ContextCompat.getColor(this, R.color.eve_primary))
            binding.tvStudioStatus.text = "Draft saved locally"
        }
    }

    private fun markUnsaved() {
        hasUnsavedChanges = true
        binding.tvPersistentStatus.text = "● UNSAVED CHANGES"
        binding.tvPersistentStatus.setTextColor(ContextCompat.getColor(this, R.color.eve_system_yellow))
    }

    private fun performCopyStyle() {
        val comp = getCurrentComponentConfig()
        repo.copyStyle(comp)
        AppBulletin.showSuccess(this, "Style copied from '${comp.name}'")
    }

    private fun performPasteStyle() {
        if (!repo.hasCopiedStyle()) {
            AppBulletin.show(this, "No style copied yet. Select a component and tap Copy Style first.")
            return
        }
        val comp = getCurrentComponentConfig()
        pushUndoState()
        updateCurrentComponentConfig { current ->
            repo.pasteStyle(current)
        }
        populateFieldsForCurrentComponent()
        AppBulletin.showSuccess(this, "Style pasted onto '${comp.name}'")
    }

    private fun pushUndoState() {
        if (undoStack.size >= maxHistorySize) {
            undoStack.removeFirst()
        }
        undoStack.addLast(activeConfig)
        redoStack.clear()
        updateUndoRedoButtonState()
    }

    private fun recordFieldEditUndo() {
        val now = System.currentTimeMillis()
        if (now - lastUndoPushTime > 2000L) {
            pushUndoState()
            lastUndoPushTime = now
        }
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

    private fun refreshEntireStudioUi() {
        updateScreenTransitionSelection()
        updateComponentDropdownAndTree()
        renderRealScreenCanvas()
        populateFieldsForCurrentComponent()
    }

    private fun setupAdvancedMode() {
        binding.btnToggleAdvanced.setOnClickListener {
            val isVis = binding.layoutAdvancedContent.visibility == View.VISIBLE
            binding.layoutAdvancedContent.visibility = if (isVis) View.GONE else View.VISIBLE
            if (!isVis) refreshAdvancedRawJson()
        }

        binding.btnApplyRawJson.setOnClickListener {
            applyAdvancedRawJson()
        }

        binding.btnResetToDefault.setOnClickListener { resetCurrentComponentToDefault() }
        binding.btnResetToScreen.setOnClickListener { resetComponentToScreenDefaults() }
        binding.btnResetToGlobal.setOnClickListener { resetComponentToGlobalDefaults() }
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
            markUnsaved()

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

        repo.saveDraftToLocalCache(activeConfig)
        markUnsaved()

        // Live update the view on the canvas
        val view = canvasViewMap[updatedComp.id]
        if (view != null) {
            UiStudioEngine.applyToView(view, updatedComp)
            updatePreviewChildViews(view, updatedComp)
            if (currentlySelectedView == view) {
                val highlightBorder = GradientDrawable().apply {
                    setColor(Color.TRANSPARENT)
                    setStroke(UiStudioEngine.dpToPx(this@UiStudioActivity, 2), Color.parseColor("#007AFF"))
                    cornerRadius = UiStudioEngine.dpToPx(this@UiStudioActivity, updatedComp.appearance.cornerRadius ?: 12).toFloat()
                }
                view.foreground = highlightBorder
            }
        }
        val swatchColor = UiStudioEngine.parseColorSafe(updatedComp.appearance.backgroundColor) ?: Color.DKGRAY
        binding.swatchBgColor.background = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(swatchColor)
        }
    }

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

    private fun renderRealScreenCanvas() {
        binding.canvasScreenContent.removeAllViews()
        canvasViewMap.clear()
        currentlySelectedView = null

        val screen = activeConfig.screens[currentScreenKey] ?: ScreenConfig(id = currentScreenKey)
        val screenBgColor = UiStudioEngine.parseColorSafe(screen.backgroundColor)
            ?: UiStudioEngine.parseColorSafe(activeConfig.designSystem.appBackground)
            ?: Color.BLACK
        binding.canvasContainer.setBackgroundColor(screenBgColor)

        screen.components.forEach { (compKey, comp) ->
            val previewView = createRealPreviewForComponent(compKey, comp)
            canvasViewMap[compKey] = previewView
            binding.canvasScreenContent.addView(previewView)
        }

        if (currentCompKey.isNotBlank()) {
            selectComponent(currentCompKey)
        }
    }

    private fun updatePreviewChildViews(view: View, comp: ComponentConfig) {
        val titleView = view.findViewWithTag<TextView>("title") ?: (view as? TextView)
        titleView?.let {
            if (!comp.content.title.isNullOrBlank()) {
                it.text = comp.content.title
            }
            UiStudioEngine.applyTypography(it, comp)
        }

        val subView = view.findViewWithTag<TextView>("subtitle")
        subView?.let {
            if (!comp.content.subtitle.isNullOrBlank()) {
                it.text = comp.content.subtitle
            }
        }

        val timerView = view.findViewWithTag<TextView>("timer_text")
        timerView?.let {
            if (!comp.content.title.isNullOrBlank()) {
                it.text = comp.content.title
            }
            UiStudioEngine.applyTypography(it, comp)
        }
    }

    private fun createRealPreviewForComponent(compKey: String, comp: ComponentConfig): View {
        val view: View = when {
            comp.type == "timer" || compKey == "timer_pill" -> {
                buildTimerPreview(compKey, comp)
            }
            comp.type == "action_grid" || compKey == "action_grid" || compKey == "bottom_actions" -> {
                buildActionGridPreview(compKey, comp)
            }
            comp.type == "banner" || compKey == "hero_banner" -> {
                buildBannerPreview(compKey, comp)
            }
            comp.type == "badge" || compKey == "streak_pill" -> {
                buildBadgePreview(compKey, comp)
            }
            comp.type == "button" -> {
                buildButtonPreview(compKey, comp)
            }
            comp.type == "text" -> {
                buildTextPreview(compKey, comp)
            }
            comp.type == "divider" -> {
                buildDividerPreview(compKey, comp)
            }
            comp.type == "spacer" -> {
                buildSpacerPreview(compKey, comp)
            }
            comp.type == "input" || compKey == "search_bar" -> {
                buildInputPreview(compKey, comp)
            }
            comp.type == "toggle" -> {
                buildTogglePreview(compKey, comp)
            }
            compKey == "option_item" -> {
                buildOptionItemPreview(compKey, comp)
            }
            else -> {
                buildCardPreview(compKey, comp)
            }
        }

        UiStudioEngine.applyToView(view, comp)

        view.setOnClickListener {
            if (isInteractMode) {
                view.animate()
                    .scaleX(0.96f)
                    .scaleY(0.96f)
                    .setDuration(70)
                    .withEndAction {
                        view.animate().scaleX(1.0f).scaleY(1.0f).setDuration(120).start()
                    }
                    .start()
                AppBulletin.show(this, "Tapped ${comp.name} in Interact Mode")
            } else {
                selectComponent(compKey)
            }
        }

        return view
    }

    private fun buildTimerPreview(compKey: String, comp: ComponentConfig): View {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(
                UiStudioEngine.dpToPx(context, comp.layout.paddingStart ?: 14),
                UiStudioEngine.dpToPx(context, comp.layout.paddingTop ?: 8),
                UiStudioEngine.dpToPx(context, comp.layout.paddingEnd ?: 14),
                UiStudioEngine.dpToPx(context, comp.layout.paddingBottom ?: 8)
            )
        }
        val ringIndicator = View(this).apply {
            val ringSize = UiStudioEngine.dpToPx(context, 16)
            layoutParams = LinearLayout.LayoutParams(ringSize, ringSize).apply {
                marginEnd = UiStudioEngine.dpToPx(context, 8)
            }
            val strokeCol = UiStudioEngine.parseColorSafe(comp.appearance.strokeColor) ?: Color.parseColor("#38BDF8")
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.TRANSPARENT)
                setStroke(UiStudioEngine.dpToPx(context, 2), strokeCol)
            }
        }
        container.addView(ringIndicator)

        val timerTv = TextView(this).apply {
            tag = "timer_text"
            text = comp.content.title?.takeIf { it.isNotBlank() } ?: "45:00"
            setTextColor(UiStudioEngine.parseColorSafe(comp.typography.textColor) ?: Color.parseColor("#38BDF8"))
            textSize = (comp.typography.textSize ?: 14).toFloat()
            setTypeface(null, if (comp.typography.textStyle == "bold") Typeface.BOLD else Typeface.NORMAL)
        }
        container.addView(timerTv)
        return container
    }

    private fun buildActionGridPreview(compKey: String, comp: ComponentConfig): View {
        val gridLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
        val row1 = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = UiStudioEngine.dpToPx(context, 6)
            }
        }
        val row2 = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }

        fun createPill(title: String, isPrimary: Boolean): View {
            return TextView(this).apply {
                text = title
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(0, UiStudioEngine.dpToPx(context, 38), 1f).apply {
                    marginEnd = UiStudioEngine.dpToPx(context, 4)
                    marginStart = UiStudioEngine.dpToPx(context, 4)
                }
                val bg = GradientDrawable().apply {
                    cornerRadius = (comp.appearance.cornerRadius ?: 12) * resources.displayMetrics.density
                    setColor(if (isPrimary) Color.parseColor("#007AFF") else (UiStudioEngine.parseColorSafe(comp.appearance.backgroundColor) ?: Color.parseColor("#1E293B")))
                    setStroke(UiStudioEngine.dpToPx(context, comp.appearance.strokeWidth ?: 1), UiStudioEngine.parseColorSafe(comp.appearance.strokeColor) ?: Color.parseColor("#334155"))
                }
                background = bg
                setTextColor(if (isPrimary) Color.WHITE else (UiStudioEngine.parseColorSafe(comp.typography.textColor) ?: Color.parseColor("#94A3B8")))
                textSize = 12f
                setTypeface(null, Typeface.BOLD)
            }
        }
        row1.addView(createPill("CLEAR", false))
        row1.addView(createPill("MARK REVIEW", false))
        row2.addView(createPill("PREVIOUS", false))
        row2.addView(createPill("SAVE & NEXT", true))
        gridLayout.addView(row1)
        gridLayout.addView(row2)
        return gridLayout
    }

    private fun buildBannerPreview(compKey: String, comp: ComponentConfig): View {
        val bannerLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(
                UiStudioEngine.dpToPx(context, comp.layout.paddingStart ?: 16),
                UiStudioEngine.dpToPx(context, comp.layout.paddingTop ?: 16),
                UiStudioEngine.dpToPx(context, comp.layout.paddingEnd ?: 16),
                UiStudioEngine.dpToPx(context, comp.layout.paddingBottom ?: 16)
            )
        }
        val tagPill = TextView(this).apply {
            text = "FEATURED CHALLENGE"
            textSize = 10f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#38BDF8"))
            setPadding(UiStudioEngine.dpToPx(context, 6), UiStudioEngine.dpToPx(context, 2), UiStudioEngine.dpToPx(context, 6), UiStudioEngine.dpToPx(context, 2))
            background = GradientDrawable().apply {
                cornerRadius = 8 * resources.displayMetrics.density
                setColor(Color.parseColor("#1E3A5F"))
            }
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = UiStudioEngine.dpToPx(context, 8)
            }
        }
        bannerLayout.addView(tagPill)

        val titleTv = TextView(this).apply {
            tag = "title"
            text = comp.content.title?.takeIf { it.isNotBlank() } ?: "EVE Daily Challenge"
            setTextColor(UiStudioEngine.parseColorSafe(comp.typography.textColor) ?: Color.WHITE)
            textSize = (comp.typography.textSize ?: 18).toFloat()
            setTypeface(null, if (comp.typography.textStyle == "bold") Typeface.BOLD else Typeface.NORMAL)
        }
        bannerLayout.addView(titleTv)

        val subTv = TextView(this).apply {
            tag = "subtitle"
            text = comp.content.subtitle ?: "Test your skills with today's featured exam series"
            setTextColor(Color.parseColor("#94A3B8"))
            textSize = 12f
            setPadding(0, UiStudioEngine.dpToPx(context, 4), 0, 0)
        }
        bannerLayout.addView(subTv)
        return bannerLayout
    }

    private fun buildBadgePreview(compKey: String, comp: ComponentConfig): View {
        val badgeLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(
                UiStudioEngine.dpToPx(context, comp.layout.paddingStart ?: 12),
                UiStudioEngine.dpToPx(context, comp.layout.paddingTop ?: 6),
                UiStudioEngine.dpToPx(context, comp.layout.paddingEnd ?: 12),
                UiStudioEngine.dpToPx(context, comp.layout.paddingBottom ?: 6)
            )
        }
        val iconTv = TextView(this).apply {
            text = "🔥"
            textSize = 13f
            setPadding(0, 0, UiStudioEngine.dpToPx(context, 6), 0)
        }
        badgeLayout.addView(iconTv)

        val titleTv = TextView(this).apply {
            tag = "title"
            text = comp.content.title?.takeIf { it.isNotBlank() } ?: "7 Day Streak"
            setTextColor(UiStudioEngine.parseColorSafe(comp.typography.textColor) ?: Color.parseColor("#F59E0B"))
            textSize = (comp.typography.textSize ?: 13).toFloat()
            setTypeface(null, if (comp.typography.textStyle == "bold") Typeface.BOLD else Typeface.NORMAL)
        }
        badgeLayout.addView(titleTv)
        return badgeLayout
    }

    private fun buildButtonPreview(compKey: String, comp: ComponentConfig): View {
        val btnLayout = FrameLayout(this).apply {
            setPadding(
                UiStudioEngine.dpToPx(context, comp.layout.paddingStart ?: 16),
                UiStudioEngine.dpToPx(context, comp.layout.paddingTop ?: 12),
                UiStudioEngine.dpToPx(context, comp.layout.paddingEnd ?: 16),
                UiStudioEngine.dpToPx(context, comp.layout.paddingBottom ?: 12)
            )
        }
        val btnTv = TextView(this).apply {
            tag = "title"
            text = comp.content.title?.takeIf { it.isNotBlank() } ?: comp.name
            setTextColor(UiStudioEngine.parseColorSafe(comp.typography.textColor) ?: Color.WHITE)
            textSize = (comp.typography.textSize ?: 15).toFloat()
            setTypeface(null, if (comp.typography.textStyle == "bold") Typeface.BOLD else Typeface.NORMAL)
            gravity = Gravity.CENTER
            layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
        btnLayout.addView(btnTv)
        return btnLayout
    }

    private fun buildTextPreview(compKey: String, comp: ComponentConfig): View {
        return TextView(this).apply {
            tag = "title"
            text = comp.content.title?.takeIf { it.isNotBlank() } ?: comp.name
            setTextColor(UiStudioEngine.parseColorSafe(comp.typography.textColor) ?: Color.parseColor("#94A3B8"))
            textSize = (comp.typography.textSize ?: 14).toFloat()
            setTypeface(null, if (comp.typography.textStyle == "bold") Typeface.BOLD else Typeface.NORMAL)
            setPadding(
                UiStudioEngine.dpToPx(context, comp.layout.paddingStart ?: 4),
                UiStudioEngine.dpToPx(context, comp.layout.paddingTop ?: 4),
                UiStudioEngine.dpToPx(context, comp.layout.paddingEnd ?: 4),
                UiStudioEngine.dpToPx(context, comp.layout.paddingBottom ?: 4)
            )
        }
    }

    private fun buildOptionItemPreview(compKey: String, comp: ComponentConfig): View {
        val optionLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(
                UiStudioEngine.dpToPx(context, comp.layout.paddingStart ?: 14),
                UiStudioEngine.dpToPx(context, comp.layout.paddingTop ?: 12),
                UiStudioEngine.dpToPx(context, comp.layout.paddingEnd ?: 14),
                UiStudioEngine.dpToPx(context, comp.layout.paddingBottom ?: 12)
            )
        }
        val circleA = TextView(this).apply {
            text = "A"
            gravity = Gravity.CENTER
            textSize = 12f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.WHITE)
            val cSize = UiStudioEngine.dpToPx(context, 26)
            layoutParams = LinearLayout.LayoutParams(cSize, cSize).apply {
                marginEnd = UiStudioEngine.dpToPx(context, 10)
            }
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#334155"))
            }
        }
        optionLayout.addView(circleA)

        val optionTv = TextView(this).apply {
            tag = "title"
            text = comp.content.title?.takeIf { it.isNotBlank() } ?: "Option A: Example answer choice"
            setTextColor(UiStudioEngine.parseColorSafe(comp.typography.textColor) ?: Color.parseColor("#E2E8F0"))
            textSize = (comp.typography.textSize ?: 14).toFloat()
        }
        optionLayout.addView(optionTv)
        return optionLayout
    }

    private fun buildInputPreview(compKey: String, comp: ComponentConfig): View {
        val inputLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(
                UiStudioEngine.dpToPx(context, comp.layout.paddingStart ?: 14),
                UiStudioEngine.dpToPx(context, comp.layout.paddingTop ?: 10),
                UiStudioEngine.dpToPx(context, comp.layout.paddingEnd ?: 14),
                UiStudioEngine.dpToPx(context, comp.layout.paddingBottom ?: 10)
            )
        }
        val searchIcon = TextView(this).apply {
            text = "🔍"
            textSize = 13f
            setPadding(0, 0, UiStudioEngine.dpToPx(context, 8), 0)
        }
        inputLayout.addView(searchIcon)

        val inputTv = TextView(this).apply {
            tag = "title"
            text = comp.content.hint?.takeIf { it.isNotBlank() } ?: comp.content.title?.takeIf { it.isNotBlank() } ?: "Search..."
            setTextColor(Color.parseColor("#64748B"))
            textSize = (comp.typography.textSize ?: 14).toFloat()
        }
        inputLayout.addView(inputTv)
        return inputLayout
    }

    private fun buildDividerPreview(compKey: String, comp: ComponentConfig): View {
        return View(this).apply {
            val strokeCol = UiStudioEngine.parseColorSafe(comp.appearance.strokeColor) ?: Color.parseColor("#334155")
            setBackgroundColor(strokeCol)
            val hPx = UiStudioEngine.parseDimensionPx(context, comp.layout.height, UiStudioEngine.dpToPx(context, 1))
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, hPx).apply {
                setMargins(
                    UiStudioEngine.dpToPx(context, comp.layout.marginStart ?: 16),
                    UiStudioEngine.dpToPx(context, comp.layout.marginTop ?: 8),
                    UiStudioEngine.dpToPx(context, comp.layout.marginEnd ?: 16),
                    UiStudioEngine.dpToPx(context, comp.layout.marginBottom ?: 8)
                )
            }
        }
    }

    private fun buildSpacerPreview(compKey: String, comp: ComponentConfig): View {
        return View(this).apply {
            val hPx = UiStudioEngine.parseDimensionPx(context, comp.layout.height, UiStudioEngine.dpToPx(context, 16))
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, hPx)
        }
    }

    private fun buildTogglePreview(compKey: String, comp: ComponentConfig): View {
        val toggleLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(
                UiStudioEngine.dpToPx(context, comp.layout.paddingStart ?: 14),
                UiStudioEngine.dpToPx(context, comp.layout.paddingTop ?: 10),
                UiStudioEngine.dpToPx(context, comp.layout.paddingEnd ?: 14),
                UiStudioEngine.dpToPx(context, comp.layout.paddingBottom ?: 10)
            )
        }
        val toggleLabel = TextView(this).apply {
            tag = "title"
            text = comp.content.title?.takeIf { it.isNotBlank() } ?: comp.name
            setTextColor(UiStudioEngine.parseColorSafe(comp.typography.textColor) ?: Color.WHITE)
            textSize = (comp.typography.textSize ?: 14).toFloat()
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }
        toggleLayout.addView(toggleLabel)
        val sw = Switch(this).apply {
            isChecked = comp.enabled
            isClickable = false
        }
        toggleLayout.addView(sw)
        return toggleLayout
    }

    private fun buildCardPreview(compKey: String, comp: ComponentConfig): View {
        val cardLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(
                UiStudioEngine.dpToPx(context, comp.layout.paddingStart ?: 14),
                UiStudioEngine.dpToPx(context, comp.layout.paddingTop ?: 14),
                UiStudioEngine.dpToPx(context, comp.layout.paddingEnd ?: 14),
                UiStudioEngine.dpToPx(context, comp.layout.paddingBottom ?: 14)
            )
        }
        val titleTv = TextView(this).apply {
            tag = "title"
            text = comp.content.title?.takeIf { it.isNotBlank() } ?: comp.name
            setTextColor(UiStudioEngine.parseColorSafe(comp.typography.textColor) ?: Color.WHITE)
            textSize = (comp.typography.textSize ?: 15).toFloat()
            setTypeface(null, if (comp.typography.textStyle == "bold") Typeface.BOLD else Typeface.NORMAL)
        }
        cardLayout.addView(titleTv)

        if (!comp.content.subtitle.isNullOrBlank() || comp.type in listOf("banner", "card")) {
            val subTv = TextView(this).apply {
                tag = "subtitle"
                text = comp.content.subtitle ?: "Type: ${comp.type} • ID: $compKey"
                setTextColor(Color.parseColor("#94A3B8"))
                textSize = 12f
                setPadding(0, UiStudioEngine.dpToPx(context, 4), 0, 0)
            }
            cardLayout.addView(subTv)
        }
        return cardLayout
    }

    private fun populateFieldsForCurrentComponent() {
        isUpdatingFields = true
        val comp = getCurrentComponentConfig()

        binding.tvInspectorHeader.text = "INSPECTOR: ${comp.name} [${comp.id}]"

        // Screen Background
        val screen = activeConfig.screens[currentScreenKey]
        val screenBgHex = screen?.backgroundColor ?: ""
        if (binding.etScreenBackgroundColor.text.toString() != screenBgHex) {
            binding.etScreenBackgroundColor.setText(screenBgHex)
        }
        val screenBgColor = UiStudioEngine.parseColorSafe(screenBgHex)
            ?: UiStudioEngine.parseColorSafe(activeConfig.designSystem.appBackground)
            ?: Color.BLACK
        binding.swatchScreenBgColor.background = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(screenBgColor)
        }

        // Design / Appearance
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

        // Material / Blur
        val blurVal = (comp.material.blurRadius ?: 0).coerceIn(0, 35)
        binding.sliderBlurRadius.value = blurVal.toFloat()
        binding.tvBlurLabel.text = "Glass Blur Radius: $blurVal px"
        binding.switchBlurEnabled.isChecked = blurVal > 0

        val matOpacity = (comp.material.materialOpacity ?: 1.0f).coerceIn(0.0f, 1.0f)
        binding.sliderMaterialOpacity.value = matOpacity
        binding.tvMaterialOpacityLabel.text = "Material Surface Opacity: ${(matOpacity * 100).toInt()}%"

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

        // States
        binding.etStatePressedBg.setText(comp.states.pressedBackgroundColor ?: "")
        binding.etStateSelectedBg.setText(comp.states.selectedBackgroundColor ?: "")
        binding.etStateDisabledBg.setText(comp.states.disabledBackgroundColor ?: "")
        binding.etStateSelectedText.setText(comp.states.selectedTextColor ?: "")

        // Branding
        binding.etBrandDisplayName.setText(activeConfig.branding.appDisplayName)
        binding.etBrandShortName.setText(activeConfig.branding.shortName)
        binding.etBrandLogoUrl.setText(activeConfig.branding.logoUrl ?: "")
        binding.etBrandColorHex.setText(activeConfig.branding.brandColor)
        binding.etBrandGlobalBgHex.setText(activeConfig.branding.globalBackgroundColor)

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

        // Screen Background
        addSimpleWatcher(binding.etScreenBackgroundColor) { hex ->
            val screen = activeConfig.screens[currentScreenKey] ?: return@addSimpleWatcher
            val newBg = if (hex.isBlank()) null else hex
            if (screen.backgroundColor != newBg) {
                recordFieldEditUndo()
                val updatedScreen = screen.copy(backgroundColor = newBg)
                val updatedScreens = activeConfig.screens.toMutableMap().apply {
                    put(currentScreenKey, updatedScreen)
                }
                activeConfig = activeConfig.copy(screens = updatedScreens)
                val parsed = UiStudioEngine.parseColorSafe(newBg)
                    ?: UiStudioEngine.parseColorSafe(activeConfig.designSystem.appBackground)
                    ?: Color.BLACK
                binding.swatchScreenBgColor.background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(parsed)
                }
                binding.canvasContainer.setBackgroundColor(parsed)
                repo.saveDraftToLocalCache(activeConfig)
                markUnsaved()
            }
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
        binding.switchBlurEnabled.setOnCheckedChangeListener { _, isChecked ->
            if (!isUpdatingFields) {
                val newRadius = if (isChecked) 18 else 0
                binding.sliderBlurRadius.value = newRadius.toFloat()
                binding.tvBlurLabel.text = "Glass Blur Radius: $newRadius px"
                updateCurrentComponentConfig { comp ->
                    comp.copy(material = comp.material.copy(blurRadius = newRadius))
                }
            }
        }
        binding.sliderBlurRadius.addOnChangeListener { _, value, fromUser ->
            if (fromUser && !isUpdatingFields) {
                val intVal = value.toInt()
                binding.tvBlurLabel.text = "Glass Blur Radius: $intVal px"
                binding.switchBlurEnabled.isChecked = intVal > 0
                updateCurrentComponentConfig { comp ->
                    comp.copy(material = comp.material.copy(blurRadius = intVal))
                }
            }
        }
        binding.sliderMaterialOpacity.addOnChangeListener { _, value, fromUser ->
            if (fromUser && !isUpdatingFields) {
                binding.tvMaterialOpacityLabel.text = "Material Surface Opacity: ${(value * 100).toInt()}%"
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
        binding.btnOpenEmojiPicker.setOnClickListener {
            showEmojiPickerDialog()
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

        // States
        addSimpleWatcher(binding.etStatePressedBg) { hex ->
            updateCurrentComponentConfig { comp ->
                comp.copy(states = comp.states.copy(pressedBackgroundColor = if (hex.isBlank()) null else hex))
            }
        }
        addSimpleWatcher(binding.etStateSelectedBg) { hex ->
            updateCurrentComponentConfig { comp ->
                comp.copy(states = comp.states.copy(selectedBackgroundColor = if (hex.isBlank()) null else hex))
            }
        }
        addSimpleWatcher(binding.etStateDisabledBg) { hex ->
            updateCurrentComponentConfig { comp ->
                comp.copy(states = comp.states.copy(disabledBackgroundColor = if (hex.isBlank()) null else hex))
            }
        }
        addSimpleWatcher(binding.etStateSelectedText) { hex ->
            updateCurrentComponentConfig { comp ->
                comp.copy(states = comp.states.copy(selectedTextColor = if (hex.isBlank()) null else hex))
            }
        }

        // Branding
        addSimpleWatcher(binding.etBrandDisplayName) { name ->
            activeConfig = activeConfig.copy(branding = activeConfig.branding.copy(appDisplayName = name))
            repo.saveDraftToLocalCache(activeConfig)
            markUnsaved()
        }
        addSimpleWatcher(binding.etBrandShortName) { shortName ->
            activeConfig = activeConfig.copy(branding = activeConfig.branding.copy(shortName = shortName))
            repo.saveDraftToLocalCache(activeConfig)
            markUnsaved()
        }
        addSimpleWatcher(binding.etBrandLogoUrl) { url ->
            activeConfig = activeConfig.copy(branding = activeConfig.branding.copy(logoUrl = if (url.isBlank()) null else url))
            repo.saveDraftToLocalCache(activeConfig)
            markUnsaved()
        }
        addSimpleWatcher(binding.etBrandColorHex) { hex ->
            activeConfig = activeConfig.copy(branding = activeConfig.branding.copy(brandColor = hex))
            repo.saveDraftToLocalCache(activeConfig)
            markUnsaved()
        }
        addSimpleWatcher(binding.etBrandGlobalBgHex) { hex ->
            activeConfig = activeConfig.copy(branding = activeConfig.branding.copy(globalBackgroundColor = hex))
            repo.saveDraftToLocalCache(activeConfig)
            markUnsaved()
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
                markUnsaved()

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
        markUnsaved()

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
                markUnsaved()

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
        markUnsaved()

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
            binding.tvPersistentStatus.text = "SAVING DRAFT..."
            val result = repo.saveDraftDetailed(activeConfig)
            binding.btnSaveDraft.isEnabled = true

            when (result) {
                is SaveDraftResult.ServerSuccess -> {
                    hasUnsavedChanges = false
                    activeConfig = result.config
                    binding.tvPersistentStatus.text = "✓ DRAFT SAVED (SYNCED)"
                    binding.tvPersistentStatus.setTextColor(ContextCompat.getColor(this@UiStudioActivity, R.color.eve_green))
                    binding.tvStudioStatus.text = "Draft saved & synced with server"
                    AppBulletin.showSuccess(this@UiStudioActivity, "Draft saved and synced to server successfully")
                }
                is SaveDraftResult.Conflict -> {
                    binding.tvPersistentStatus.text = "NEWER DRAFT EXISTS (CONFLICT)"
                    binding.tvPersistentStatus.setTextColor(ContextCompat.getColor(this@UiStudioActivity, R.color.eve_system_yellow))
                    showConcurrencyConflictDialog(result.serverDraft, result.message)
                }
                is SaveDraftResult.LocalOfflineSuccess -> {
                    hasUnsavedChanges = false
                    binding.tvPersistentStatus.text = "LOCAL DRAFT SAVED • OFFLINE"
                    binding.tvPersistentStatus.setTextColor(ContextCompat.getColor(this@UiStudioActivity, R.color.eve_primary))
                    binding.tvStudioStatus.text = "Local draft saved (Server sync failed)"
                    AppBulletin.show(this@UiStudioActivity, "Local draft saved. Server sync offline: ${result.error}")
                }
                is SaveDraftResult.Failure -> {
                    binding.tvPersistentStatus.text = "SAVE FAILED"
                    binding.tvPersistentStatus.setTextColor(ContextCompat.getColor(this@UiStudioActivity, R.color.eve_red))
                    AppBulletin.showError(this@UiStudioActivity, "Failed to save draft: ${result.error}")
                }
            }
        }
    }

    private fun showConcurrencyConflictDialog(serverDraft: UiStudioConfig?, message: String) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Newer Draft Exists on Server")
            .setMessage("$message\n\nA collaborator or another session updated this draft. Would you like to reload the latest draft from the server or overwrite it with your local changes?")
            .setPositiveButton("Reload Latest") { _, _ ->
                if (serverDraft != null) {
                    activeConfig = serverDraft
                    repo.saveDraftToLocalCache(serverDraft)
                    hasUnsavedChanges = false
                    updateStudioStatusBadges()
                    updateComponentDropdownAndTree()
                    renderRealScreenCanvas()
                    populateFieldsForCurrentComponent()
                    AppBulletin.showSuccess(this, "Loaded latest server draft")
                } else {
                    loadDraftFromRepository()
                }
            }
            .setNegativeButton("Keep Local Draft (Overwrite)") { _, _ ->
                forceSaveDraft()
            }
            .setNeutralButton("Cancel", null)
            .show()
    }

    private fun forceSaveDraft() {
        lifecycleScope.launch {
            binding.btnSaveDraft.isEnabled = false
            binding.tvPersistentStatus.text = "FORCE SAVING DRAFT..."
            val result = repo.saveDraftDetailed(activeConfig, force = true)
            binding.btnSaveDraft.isEnabled = true
            when (result) {
                is SaveDraftResult.ServerSuccess -> {
                    hasUnsavedChanges = false
                    activeConfig = result.config
                    binding.tvPersistentStatus.text = "✓ DRAFT SAVED (OVERWRITTEN)"
                    binding.tvPersistentStatus.setTextColor(ContextCompat.getColor(this@UiStudioActivity, R.color.eve_green))
                    AppBulletin.showSuccess(this@UiStudioActivity, "Draft force-saved successfully")
                }
                else -> {
                    AppBulletin.showError(this@UiStudioActivity, "Failed to force save draft")
                }
            }
        }
    }

    private fun validateCurrentDraft() {
        val validation = repo.validateConfig(activeConfig)
        if (validation.first) {
            binding.tvPersistentStatus.text = "VALIDATION PASSED"
            binding.tvPersistentStatus.setTextColor(ContextCompat.getColor(this, R.color.eve_green))
            AppBulletin.showSuccess(this, "Validation passed: 100% compliant schema")
        } else {
            binding.tvPersistentStatus.text = "VALIDATION ISSUES (${validation.second.size})"
            binding.tvPersistentStatus.setTextColor(ContextCompat.getColor(this, R.color.eve_red))
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
            .setMessage("This will push your changes live to all student devices after verifying the live snapshot.")
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
            binding.tvPersistentStatus.text = "PUBLISHING LIVE..."
            val result = repo.publishVerified(notes, activeConfig)
            binding.btnPublish.isEnabled = true

            when (result) {
                is PublishResult.VerifiedSuccess -> {
                    hasUnsavedChanges = false
                    activeConfig = result.config
                    val timeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(result.publishedAt))
                    binding.tvPersistentStatus.text = "★ LIVE v${result.version} (VERIFIED)"
                    binding.tvPersistentStatus.setTextColor(ContextCompat.getColor(this@UiStudioActivity, R.color.eve_green))
                    binding.tvStudioStatus.text = "Published at $timeStr • Verified active on all devices"
                    AppBulletin.showSuccess(this@UiStudioActivity, "Live v${result.version} published and verified successfully!")
                }
                is PublishResult.VerificationFailed -> {
                    binding.tvPersistentStatus.text = "PUBLISH VERIFICATION FAILED"
                    binding.tvPersistentStatus.setTextColor(ContextCompat.getColor(this@UiStudioActivity, R.color.eve_system_yellow))
                    AppBulletin.showError(this@UiStudioActivity, "Verification failed: ${result.reason}")
                }
                is PublishResult.NetworkFailure -> {
                    binding.tvPersistentStatus.text = "PUBLISH FAILED"
                    binding.tvPersistentStatus.setTextColor(ContextCompat.getColor(this@UiStudioActivity, R.color.eve_red))
                    AppBulletin.showError(this@UiStudioActivity, "Publish failed: ${result.error}")
                }
            }
        }
    }

    private fun showResetOptionsDialog() {
        val options = arrayOf(
            "Reset Selected Component to Native Default",
            "Reset Component to Screen Defaults",
            "Reset Component to Global Design System",
            "Reset Current Screen to Native Default",
            "Reset Working Draft to Published Config",
            "Emergency Factory Reset (All Screens)"
        )

        MaterialAlertDialogBuilder(this)
            .setTitle("Reset & Fallback Options")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> resetCurrentComponentToDefault()
                    1 -> resetComponentToScreenDefaults()
                    2 -> resetComponentToGlobalDefaults()
                    3 -> resetCurrentScreenToDefault()
                    4 -> resetDraftToPublished()
                    5 -> emergencyFactoryReset()
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

    private fun resetComponentToScreenDefaults() {
        val screen = activeConfig.screens[currentScreenKey] ?: return
        pushUndoState()
        updateCurrentComponentConfig { comp ->
            comp.copy(appearance = comp.appearance.copy(backgroundColor = screen.backgroundColor))
        }
        populateFieldsForCurrentComponent()
        AppBulletin.showSuccess(this, "Reset component colors to screen baseline")
    }

    private fun resetComponentToGlobalDefaults() {
        val ds = activeConfig.designSystem
        pushUndoState()
        updateCurrentComponentConfig { comp ->
            comp.copy(
                appearance = comp.appearance.copy(
                    backgroundColor = ds.surfaceBackground,
                    cornerRadius = ds.radiusScale,
                    strokeColor = ds.borderColor,
                    opacity = ds.defaultOpacity
                ),
                material = comp.material.copy(blurRadius = ds.defaultBlurRadius),
                typography = comp.typography.copy(textColor = ds.textPrimary)
            )
        }
        populateFieldsForCurrentComponent()
        AppBulletin.showSuccess(this, "Reset component to Global Design System tokens")
    }

    private fun resetCurrentScreenToDefault() {
        val defaultTemplate = repo.getDefaultTemplate()
        val defaultScreen = defaultTemplate.screens[currentScreenKey]
        if (defaultScreen != null) {
            pushUndoState()
            val updatedScreens = activeConfig.screens.toMutableMap().apply { put(currentScreenKey, defaultScreen) }
            activeConfig = activeConfig.copy(screens = updatedScreens)
            repo.saveDraftToLocalCache(activeConfig)
            markUnsaved()
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
                hasUnsavedChanges = false
                updateStudioStatusBadges()
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
                hasUnsavedChanges = false
                updateStudioStatusBadges()
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
                    markUnsaved()
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
