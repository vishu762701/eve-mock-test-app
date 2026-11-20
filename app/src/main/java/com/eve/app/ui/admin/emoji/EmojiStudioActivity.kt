package com.eve.app.ui.admin.emoji

import android.content.DialogInterface
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.airbnb.lottie.LottieAnimationView
import com.eve.app.R
import com.eve.app.data.model.EmojiItem
import com.eve.app.data.model.EmojiPack
import com.eve.app.data.remote.toUserFriendlyMessage
import com.eve.app.data.repository.EmojiRepository
import com.eve.app.databinding.BottomSheetEmojiPackManagerBinding
import com.eve.app.databinding.BottomSheetEmojiPlacementBinding
import com.eve.app.designsystem.TelegramBottomSheet
import com.eve.app.designsystem.TelegramNativeAlertDialog
import com.eve.app.ui.admin.tg.TelegramSegmentedTabs
import com.eve.app.designsystem.TelegramSwitch
import com.eve.app.ui.admin.tg.TextCheckCell
import com.eve.app.ui.common.EveBaseActivity
import com.eve.app.util.AdminAccessGate
import com.eve.app.util.AppBulletin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.telegram.messenger.AndroidUtilities
import org.telegram.ui.ActionBar.ActionBar
import org.telegram.ui.ActionBar.ActionBarMenuItem
import org.telegram.ui.ActionBar.Theme
import org.telegram.ui.Components.EmptyTextProgressView
import org.telegram.ui.Components.LayoutHelper
import org.telegram.ui.Components.RecyclerListView
import org.telegram.ui.Stories.recorder.ButtonWithCounterView

/**
 * Motion Emoji & Sticker Studio powered by real ported Telegram UI components:
 * ActionBar (with live ActionBarMenuItem search + refresh), RecyclerListView (4-col grid),
 * EmptyTextProgressView, ButtonWithCounterView, plus Recent category, live looping
 * long-press ContentPreview dialog, and full placement/pack/import workflows.
 */
class EmojiStudioActivity : EveBaseActivity() {

    companion object {
        private const val MENU_SEARCH = 1
        private const val MENU_REFRESH = 2
    }

    private val emojiRepo = EmojiRepository()
    private lateinit var adapter: EmojiGridAdapter

    private var allPacks: List<EmojiPack> = emptyList()
    private var allItems: List<EmojiItem> = emptyList()
    private var serverItemIds: Set<String> = emptySet()
    private var displayedItems: List<EmojiItem> = emptyList()
    private var tabTitles: List<String> = listOf("All")

    private var currentTabIndex = 0
    private var searchQuery = ""
    private var globalTelegramEnabled = true
    private var telegramBotConfigured = false
    private var dataJob: Job? = null
    private var importRunning = false
    private var authorized = false
    private var reauthorizeOnResume = false
    private var authorizedUid: String? = null
    private var authorizedEmail: String? = null
    private var placementDialog: TelegramBottomSheet? = null
    private var packManagerDialog: TelegramBottomSheet? = null
    private var importOptionsDialog: TelegramNativeAlertDialog? = null
    private var itemDetailsDialog: TelegramNativeAlertDialog? = null
    private var pendingUpload: android.net.Uri? = null

    private lateinit var rootView: LinearLayout
    private lateinit var actionBar: ActionBar
    private lateinit var segmentedTabs: TelegramSegmentedTabs
    private lateinit var listView: RecyclerListView
    private lateinit var emptyView: EmptyTextProgressView
    private lateinit var bottomBar: LinearLayout
    private lateinit var btnManagePacks: ButtonWithCounterView
    private lateinit var btnImport: ButtonWithCounterView

    private val uploadPicker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            if (authorized) uploadCustom(uri) else pendingUpload = uri
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val live = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
        authorizedUid = live?.uid
        authorizedEmail = live?.email

        Theme.syncFromEvePalette(this)

        rootView = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray))
        }
        setContentView(rootView)

        // 1. Real Telegram ActionBar with live Search + Refresh
        actionBar = ActionBar(this).apply {
            setBackgroundColor(Theme.getColor(Theme.key_actionBarDefault))
            setItemsBackgroundColor(Theme.getColor(Theme.key_actionBarDefaultSelector), false)
            setItemsColor(Theme.getColor(Theme.key_actionBarDefaultIcon), false)
            setTitleColor(Theme.getColor(Theme.key_actionBarDefaultTitle))
            setSubtitleColor(Theme.getColor(Theme.key_actionBarDefaultSubtitle))
            setBackButtonImage(R.drawable.ic_ab_back)
            setTitle("Animated Emoji")
            setSubtitle("Emoji Studio & Placements")
            val menu = createMenu()
            val searchItem = menu.addItem(MENU_SEARCH, R.drawable.msg_search)
                .setIsSearchField(true)
                .setActionBarMenuItemSearchListener(object : ActionBarMenuItem.ActionBarMenuItemSearchListener() {
                    override fun onSearchCollapse() {
                        searchQuery = ""
                        if (authorized) filterAndDisplayItems()
                    }

                    override fun onTextChanged(editText: EditText?) {
                        searchQuery = editText?.text?.toString()?.trim()?.lowercase().orEmpty()
                        if (authorized) filterAndDisplayItems()
                    }
                })
            searchItem.setSearchFieldHint("Search emoji, pack or source…")
            menu.addItem(MENU_REFRESH, R.drawable.msg_retry)
            setActionBarMenuOnItemClick(object : ActionBar.ActionBarMenuOnItemClick() {
                override fun onItemClick(id: Int) {
                    when (id) {
                        -1 -> finish()
                        MENU_REFRESH -> if (authorized) loadData()
                    }
                }
            })
        }
        rootView.addView(
            actionBar,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        // 2. Category tabs (Recent, All, Noto, Telegram UI, Custom Packs)
        segmentedTabs = TelegramSegmentedTabs(this).apply {
            setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite))
            visibility = View.GONE
        }
        rootView.addView(
            segmentedTabs,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        // 3. Content frame with RecyclerListView + EmptyTextProgressView
        val contentFrame = FrameLayout(this)
        rootView.addView(
            contentFrame,
            LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
        )

        listView = RecyclerListView(this).apply {
            layoutManager = GridLayoutManager(this@EmojiStudioActivity, 4)
            clipToPadding = false
            val pad = AndroidUtilities.dp(8f)
            setPadding(pad, pad, pad, AndroidUtilities.dp(16f))
            visibility = View.GONE
        }
        contentFrame.addView(
            listView,
            LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT.toFloat())
        )

        emptyView = EmptyTextProgressView(this).apply {
            setShowAtCenter(true)
            setTextColor(Theme.getColor(Theme.key_emptyListPlaceholder))
            setProgressBarColor(Theme.getColor(Theme.key_progressCircle))
            setText("No motion emojis match this filter")
            showProgress()
        }
        contentFrame.addView(
            emptyView,
            LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT.toFloat())
        )

        // 4. Bottom action bar with Telegram ButtonWithCounterView buttons
        bottomBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite))
            val padH = AndroidUtilities.dp(12f)
            val padV = AndroidUtilities.dp(10f)
            setPadding(padH, padV, padH, padV)
            visibility = View.GONE
        }
        btnManagePacks = ButtonWithCounterView(this, false, null).apply {
            setRound()
            setNeutral()
            setText("Manage Packs", false)
            setOnClickListener { showPackManagerBottomSheet() }
        }
        btnImport = ButtonWithCounterView(this, null).apply {
            setRound()
            setText("Import Emoji", false)
            setOnClickListener { showImportOptionsDialog() }
        }
        bottomBar.addView(
            btnManagePacks,
            LinearLayout.LayoutParams(0, AndroidUtilities.dp(44f), 1f).apply {
                marginEnd = AndroidUtilities.dp(8f)
            }
        )
        bottomBar.addView(
            btnImport,
            LinearLayout.LayoutParams(0, AndroidUtilities.dp(44f), 1f)
        )
        rootView.addView(
            bottomBar,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        adapter = EmojiGridAdapter(
            onItemClick = { item ->
                EmojiRenderer.recordRecent(this, item.id)
                showPlacementBottomSheet(item)
            },
            onItemLongClick = { item ->
                EmojiRenderer.recordRecent(this, item.id)
                showAnimatedPreviewDialog(item)
            }
        )
        listView.adapter = adapter

        AdminAccessGate.requireAdmin(this, rootView, requireFreshServer = true, requiredRight = "manage_content") {
            authorized = true
            authorizedUid = live?.uid
            authorizedEmail = live?.email
            segmentedTabs.visibility = View.VISIBLE
            bottomBar.visibility = View.VISIBLE
            loadData()
            pendingUpload?.let { pendingUpload = null; uploadCustom(it) }
        }
    }

    override fun onResume() {
        super.onResume()
        EmojiRenderer.resumeAll()
        if (reauthorizeOnResume && ::rootView.isInitialized) {
            reauthorizeOnResume = false
            val live = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
            if (live == null || live.uid != authorizedUid || live.email != authorizedEmail) {
                finish()
                return
            }
            listView.visibility = View.GONE
            emptyView.visibility = View.VISIBLE
            emptyView.showProgress()
            segmentedTabs.visibility = View.GONE
            bottomBar.visibility = View.GONE
            AdminAccessGate.requireAdmin(this, rootView, requireFreshServer = true, requiredRight = "manage_content") {
                authorized = true
                authorizedUid = live.uid
                authorizedEmail = live.email
                segmentedTabs.visibility = View.VISIBLE
                bottomBar.visibility = View.VISIBLE
                loadData()
            }
        }
    }

    override fun onPause() {
        EmojiRenderer.pauseAll()
        super.onPause()
    }

    override fun onStop() {
        reauthorizeOnResume = true
        dataJob?.cancel()
        placementDialog?.dismiss()
        placementDialog = null
        packManagerDialog?.dismiss()
        packManagerDialog = null
        importOptionsDialog?.dismiss()
        importOptionsDialog = null
        itemDetailsDialog?.dismiss()
        itemDetailsDialog = null
        super.onStop()
    }

    override fun onDestroy() {
        dataJob?.cancel()
        placementDialog?.dismiss()
        placementDialog = null
        packManagerDialog?.dismiss()
        packManagerDialog = null
        importOptionsDialog?.dismiss()
        importOptionsDialog = null
        itemDetailsDialog?.dismiss()
        itemDetailsDialog = null
        super.onDestroy()
    }

    private fun loadData() {
        dataJob?.cancel()
        listView.visibility = View.GONE
        emptyView.visibility = View.VISIBLE
        emptyView.showProgress()

        dataJob = lifecycleScope.launch {
            val packsResult = emojiRepo.getPacks()
            val itemsResult = emojiRepo.getAllItems()

            if (packsResult.isFailure || itemsResult.isFailure) {
                emptyView.setText("Failed to load motion emojis")
                emptyView.showTextView()
                AppBulletin.showError(
                    this@EmojiStudioActivity,
                    (packsResult.exceptionOrNull() ?: itemsResult.exceptionOrNull())!!.toUserFriendlyMessage()
                )
                return@launch
            }
            if (packsResult.isSuccess) {
                val data = packsResult.getOrNull()
                allPacks = data?.packs?.map { dto ->
                    EmojiPack(
                        id = dto.id,
                        name = dto.name,
                        title = dto.title,
                        source = dto.source,
                        licenseAttribution = dto.licenseAttribution,
                        enabled = dto.enabled,
                        itemCount = dto.itemCount,
                        createdAt = dto.createdAt,
                        createdBy = dto.createdBy ?: ""
                    )
                } ?: emptyList()
                globalTelegramEnabled = data?.globalTelegramEnabled ?: true
                telegramBotConfigured = data?.telegramBotConfigured ?: false
            }

            val serverItems = itemsResult.getOrDefault(emptyList())
            val bundledUiItems = if (allPacks.any { it.id == "pack_tg_ui" }) getBundledTelegramUiItems() else emptyList()
            serverItemIds = serverItems.map { it.id }.toSet()
            allItems = (serverItems + bundledUiItems).distinctBy { it.id }

            setupTabs()
            filterAndDisplayItems()
        }
    }

    private fun setupTabs() {
        val titles = mutableListOf("All", "Recent", "Noto", "Telegram UI")
        allPacks.forEach { pack ->
            if (pack.source != "telegram-ui" && pack.source != "noto" && !titles.contains(pack.title)) {
                titles.add(pack.title)
            }
        }
        tabTitles = titles
        if (currentTabIndex !in tabTitles.indices) currentTabIndex = 0
        segmentedTabs.setTabs(tabTitles)
        segmentedTabs.onTabSelectedListener = { pos ->
            currentTabIndex = pos
            filterAndDisplayItems()
        }
    }

    private fun filterAndDisplayItems() {
        val tabName = tabTitles.getOrElse(currentTabIndex) { "All" }
        val recentOrder = if (tabName == "Recent") EmojiRenderer.getRecentIds(this) else emptyList()
        val recentSet = recentOrder.toSet()

        val filtered = allItems.filter { item ->
            val matchesTab = when (tabName) {
                "All" -> true
                "Recent" -> item.id in recentSet
                "Noto" -> item.source == "noto" || item.packSlug == "noto_animated_emoji"
                "Telegram UI" -> item.source == "telegram-ui"
                else -> item.packTitle.equals(tabName, ignoreCase = true)
            }
            val matchesSearch = if (searchQuery.isBlank()) true else {
                item.altEmoji.contains(searchQuery) ||
                    (item.packTitle?.lowercase()?.contains(searchQuery) == true) ||
                    (item.packSlug?.lowercase()?.contains(searchQuery) == true) ||
                    item.source.lowercase().contains(searchQuery) ||
                    item.id.lowercase().contains(searchQuery)
            }
            matchesTab && matchesSearch
        }

        displayedItems = if (tabName == "Recent") {
            filtered.sortedBy { recentOrder.indexOf(it.id).let { idx -> if (idx < 0) Int.MAX_VALUE else idx } }
        } else {
            filtered
        }

        adapter.submitList(displayedItems)
        actionBar.setSubtitle("${displayedItems.size} ${if (displayedItems.size == 1) "emoji" else "emojis"} • $tabName")
        if (displayedItems.isEmpty()) {
            listView.visibility = View.GONE
            emptyView.visibility = View.VISIBLE
            emptyView.setText(
                when {
                    searchQuery.isNotBlank() -> "No emojis match \"$searchQuery\""
                    tabName == "Recent" -> "No recent emojis yet\nTap any motion emoji to preview or place it."
                    else -> "No emojis in $tabName"
                }
            )
            emptyView.showTextView()
        } else {
            emptyView.visibility = View.GONE
            listView.visibility = View.VISIBLE
        }
    }

    private fun getBundledTelegramUiItems(): List<EmojiItem> {
        val rawIcons = listOf(
            Triple("tg_fire_on", "🔥", "Fire"),
            Triple("tg_ic_admin", "👮", "Admin"),
            Triple("tg_ic_pin", "📌", "Pin"),
            Triple("tg_ic_ban", "🚫", "Ban"),
            Triple("tg_ic_unban", "✅", "Unban"),
            Triple("tg_filters", "🔍", "Filters"),
            Triple("tg_filter_new", "✨", "Filter New"),
            Triple("tg_chats_archive", "📦", "Archive"),
            Triple("tg_folder_in", "📂", "Folder In"),
            Triple("tg_folder_out", "📁", "Folder Out"),
            Triple("tg_ic_delete", "🗑️", "Delete"),
            Triple("tg_ic_mute", "🔕", "Mute"),
            Triple("tg_ic_unmute", "🔔", "Unmute"),
            Triple("tg_ic_unpin", "📍", "Unpin"),
            Triple("tg_qr_code_logo", "📱", "QR Code"),
            Triple("tg_report_police", "🚨", "Report"),
            Triple("tg_saved_messages", "💾", "Saved"),
            Triple("tg_statistic_preload", "📊", "Stats"),
            Triple("tg_swipe_delete", "🧹", "Swipe Delete"),
            Triple("tg_tab_poll", "🗳️", "Poll")
        )

        return rawIcons.map { (resName, alt, _) ->
            EmojiItem(
                id = "bundled_$resName",
                packId = "pack_tg_ui",
                packTitle = "Telegram UI Icons",
                packSlug = "telegram_ui_icons",
                altEmoji = alt,
                kind = "lottie",
                url = "res:raw/$resName",
                storageKey = "res:raw/$resName",
                byteSize = 0,
                sha256 = null,
                enabled = true,
                premiumOrigin = false,
                telegramCustomEmojiId = null,
                source = "telegram-ui",
                createdAt = 0L
            )
        }
    }

    /**
     * Telegram ContentPreviewViewer-style large looping animated preview on long-press,
     * with direct Apply Placement action and full metadata inspection.
     */
    private fun showAnimatedPreviewDialog(item: EmojiItem) {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            val pad = AndroidUtilities.dp(20f)
            setPadding(pad, pad, pad, AndroidUtilities.dp(8f))
        }
        val previewBox = FrameLayout(this)
        val lottie = LottieAnimationView(this)
        val staticImg = ImageView(this).apply { scaleType = ImageView.ScaleType.FIT_CENTER }
        val altTv = TextView(this).apply {
            gravity = Gravity.CENTER
            setTextSize(TypedValue.COMPLEX_UNIT_DIP, 52f)
        }
        val boxSize = AndroidUtilities.dp(132f)
        previewBox.addView(lottie, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT.toFloat()))
        previewBox.addView(staticImg, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT.toFloat()))
        previewBox.addView(altTv, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT.toFloat()))
        EmojiRenderer.bindPreview(lottie, staticImg, altTv, item, loop = true)
        container.addView(previewBox, LinearLayout.LayoutParams(boxSize, boxSize))

        val metaTv = TextView(this).apply {
            setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13f)
            setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText))
            setPadding(0, AndroidUtilities.dp(12f), 0, 0)
            text = buildString {
                append("Alt: ${item.altEmoji}  •  Kind: ${item.kind}\n")
                append("Pack: ${item.packTitle ?: item.packSlug ?: "N/A"}\n")
                append("Source: ${item.source}  •  Size: ${if (item.byteSize > 0) "${item.byteSize} B" else "Bundled"}\n")
                append("Premium Origin: ${if (item.premiumOrigin) "Yes (Telegram)" else "No"}")
            }
        }
        container.addView(
            metaTv,
            LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        )

        val dialog = TelegramNativeAlertDialog.Builder(this)
            .setTitle("${item.altEmoji} Motion Preview")
            .setView(container)
            .setPositiveButton("Apply Placement") { _, _ -> showPlacementBottomSheet(item) }
            .setNegativeButton("Close", null)
            .create()
        itemDetailsDialog = dialog
        dialog.setOnDismissListener {
            lottie.cancelAnimation()
            if (itemDetailsDialog === dialog) itemDetailsDialog = null
        }
        dialog.show()
    }

    private fun showPlacementBottomSheet(item: EmojiItem) {
        val sheetBinding = BottomSheetEmojiPlacementBinding.inflate(layoutInflater)
        val dialog = TelegramBottomSheet.Builder(this)
            .setCustomView(sheetBinding.root)
            .create()
        placementDialog = dialog
        com.eve.app.ui.home.TelegramBottomSheetSourceBackdrop.install(dialog, this)
        dialog.setOnDismissListener {
            sheetBinding.ivPlacementPreview.cancelAnimation()
            if (placementDialog === dialog) placementDialog = null
        }

        sheetBinding.tvPlacementEmojiTitle.text = "${item.altEmoji} Placement"
        sheetBinding.tvPlacementPackName.text = item.packTitle ?: item.source

        EmojiRenderer.bindPreview(
            lottieView = sheetBinding.ivPlacementPreview,
            staticView = null,
            altView = null,
            item = item,
            loop = true
        )

        sheetBinding.rgSlotType.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.rbExamCard -> sheetBinding.tilTargetId.hint = "Exam ID (e.g. physics_test_1)"
                R.id.rbCategoryHeader -> sheetBinding.tilTargetId.hint = "Category Name (e.g. Science)"
                R.id.rbBroadcast -> sheetBinding.tilTargetId.hint = "Broadcast ID"
            }
        }

        sheetBinding.rowRemovePlacement.setText("Remove Placement", needDivider = false)
        sheetBinding.rowRemovePlacement.setShowArrow(false)
        sheetBinding.rowRemovePlacement.makeRed()
        sheetBinding.rowRemovePlacement.contentDescription = "Remove emoji placement from target"
        val canApply = item.id in serverItemIds
        var requestRunning = false
        fun selection(): Pair<String, String> {
            val slot = when (sheetBinding.rgSlotType.checkedRadioButtonId) {
                R.id.rbCategoryHeader -> "category_header"
                R.id.rbBroadcast -> "broadcast"
                else -> "exam_card"
            }
            return slot to sheetBinding.etTargetId.text?.toString()?.trim().orEmpty()
        }
        fun setRequestRunning(running: Boolean) {
            requestRunning = running
            sheetBinding.btnApplyPlacement.isEnabled = canApply && !running
            sheetBinding.rowRemovePlacement.isEnabled = !running
        }
        sheetBinding.rowRemovePlacement.setOnClickListener {
            if (requestRunning) return@setOnClickListener
            val (slotType, targetId) = selection()
            if (targetId.isBlank() || targetId.length > 128) {
                AppBulletin.showError(this, "Enter a target ID containing 1-128 characters.")
                return@setOnClickListener
            }
            TelegramNativeAlertDialog.Builder(this)
                .setTitle("Remove Placement?")
                .setMessage("Remove the emoji currently assigned to $slotType '$targetId'?")
                .setPositiveButton("Remove") { _, _ ->
                    if (!requestRunning) {
                        setRequestRunning(true)
                        lifecycleScope.launch {
                            try {
                                val result = emojiRepo.clearPlacement(slotType, targetId)
                                if (result.isSuccess) {
                                    dialog.dismiss()
                                    AppBulletin.showSuccess(this@EmojiStudioActivity, "Emoji placement removed.")
                                } else {
                                    AppBulletin.showError(
                                        this@EmojiStudioActivity,
                                        result.exceptionOrNull()?.toUserFriendlyMessage() ?: "Unable to remove placement."
                                    )
                                }
                            } finally {
                                setRequestRunning(false)
                            }
                        }
                    }
                }
                .setNegativeButton("Cancel", null)
                .makeRed(DialogInterface.BUTTON_POSITIVE)
                .show()
        }
        sheetBinding.btnCancelPlacement.setOnClickListener { dialog.dismiss() }
        sheetBinding.btnApplyPlacement.isEnabled = canApply
        if (!canApply) sheetBinding.tvPlacementPackName.text = "Preview only: server catalogue update required."
        sheetBinding.btnApplyPlacement.setOnClickListener {
            if (requestRunning) return@setOnClickListener
            val slotType = when (sheetBinding.rgSlotType.checkedRadioButtonId) {
                R.id.rbCategoryHeader -> "category_header"
                R.id.rbBroadcast -> "broadcast"
                else -> "exam_card"
            }
            val targetId = sheetBinding.etTargetId.text?.toString()?.trim() ?: ""
            if (targetId.isBlank() || targetId.length > 128) {
                AppBulletin.showError(this, "Enter a target ID containing 1-128 characters.")
                return@setOnClickListener
            }
            setRequestRunning(true)
            lifecycleScope.launch {
                try {
                    val res = emojiRepo.setPlacement(slotType, targetId, item.id)
                    if (res.isSuccess) {
                        EmojiRenderer.recordRecent(this@EmojiStudioActivity, item.id)
                        dialog.dismiss()
                        AppBulletin.showSuccess(this@EmojiStudioActivity, "Placed ${item.altEmoji} on $slotType!")
                    } else {
                        AppBulletin.showError(
                            this@EmojiStudioActivity,
                            res.exceptionOrNull()?.toUserFriendlyMessage() ?: "Failed to set placement"
                        )
                    }
                } finally {
                    setRequestRunning(false)
                }
            }
        }

        dialog.show()
    }

    private fun showPackManagerBottomSheet() {
        val sheetBinding = BottomSheetEmojiPackManagerBinding.inflate(layoutInflater)
        val dialog = TelegramBottomSheet.Builder(this)
            .setCustomView(sheetBinding.root)
            .create()
        packManagerDialog = dialog
        dialog.setOnDismissListener { if (packManagerDialog === dialog) packManagerDialog = null }

        sheetBinding.cellGlobalTelegramSwitch.setTextAndCheck(
            "Global Telegram Emoji Switch",
            globalTelegramEnabled,
            needDivider = true
        )
        bindPersistedSwitch(
            sheetBinding.cellGlobalTelegramSwitch,
            globalTelegramEnabled,
            save = { emojiRepo.toggleGlobalTelegramStatus(it) },
            onSaved = { globalTelegramEnabled = it }
        )

        sheetBinding.cellTelegramBotStatus.visibility = if (telegramBotConfigured) View.VISIBLE else View.GONE
        if (telegramBotConfigured) sheetBinding.cellTelegramBotStatus.setText("Telegram sticker imports are available.")

        sheetBinding.headerPacksList.setText("INSTALLED PACKS")

        sheetBinding.containerPacks.removeAllViews()
        allPacks.forEach { pack ->
            val cell = TextCheckCell(this)
            cell.setTextAndCheck(pack.title, pack.enabled, needDivider = true)
            bindPersistedSwitch(
                cell,
                pack.enabled,
                save = { emojiRepo.togglePackStatus(pack.id, it) },
                onSaved = { enabled -> allPacks = allPacks.map { if (it.id == pack.id) it.copy(enabled = enabled) else it } }
            )

            cell.setOnLongClickListener {
                TelegramNativeAlertDialog.Builder(this)
                    .setTitle("Delete Pack")
                    .setMessage("Permanently takedown and delete '${pack.title}' and all its emoji files?")
                    .setPositiveButton("Delete Permanently") { _, _ ->
                        lifecycleScope.launch {
                            val res = emojiRepo.deletePack(pack.id)
                            if (res.isSuccess) {
                                if (res.getOrNull() == true) AppBulletin.showSuccess(this@EmojiStudioActivity, "Pack removed.")
                                else AppBulletin.showError(this@EmojiStudioActivity, "Pack removed. Some media cleanup could not be completed.")
                                dialog.dismiss()
                                loadData()
                            } else {
                                AppBulletin.showError(this@EmojiStudioActivity, res.exceptionOrNull()?.toUserFriendlyMessage() ?: "Failed to delete pack")
                            }
                        }
                    }
                    .setNegativeButton("Cancel", null)
                    .makeRed(DialogInterface.BUTTON_POSITIVE)
                    .show()
                true
            }

            sheetBinding.containerPacks.addView(cell)
        }

        dialog.show()
    }

    private fun showImportOptionsDialog() {
        if (importRunning) return
        val actions = mutableListOf<Pair<String, () -> Unit>>()
        if (telegramBotConfigured) actions += "Import Telegram Sticker Pack" to { showTelegramImportDialog() }
        actions += "Import Google Noto Animated Emoji" to { showNotoImportDialog() }
        actions += "Upload Custom JSON/WebP/PNG" to {
            uploadPicker.launch(arrayOf("application/json", "image/webp", "image/png"))
        }
        val dialog = TelegramNativeAlertDialog.Builder(this)
            .setTitle("Import Emoji")
            .setItems(actions.map { it.first }.toTypedArray()) { _, which -> actions[which].second() }
            .setNegativeButton("Cancel", null)
            .create()
        importOptionsDialog = dialog
        dialog.setOnDismissListener { if (importOptionsDialog === dialog) importOptionsDialog = null }
        dialog.show()
    }

    private fun showTelegramImportDialog() {
        if (!telegramBotConfigured || importRunning) return

        val input = EditText(this).apply {
            hint = "Pack name or link (e.g. Duck or t.me/addstickers/...)"
            setSingleLine()
        }

        TelegramNativeAlertDialog.Builder(this)
            .setTitle("Import Telegram Pack")
            .setView(input)
            .setPositiveButton("Import") { _, _ ->
                val packName = input.text.toString().trim()
                if (packName.isNotBlank()) {
                    runImport {
                        var offset = 0
                        var count = 0
                        val errors = mutableListOf<String>()
                        do {
                            val info = emojiRepo.importTelegramPack(packName, offset).getOrThrow()
                            count += info.importedCount
                            errors += info.errors.orEmpty()
                            val next = info.nextOffset
                            check(next == null || next > offset) { "Import did not progress. Please retry." }
                            offset = next ?: -1
                        } while (offset >= 0)
                        if (errors.isNotEmpty()) throw IllegalStateException("Imported $count stickers. ${errors.first()}")
                        "Imported $count stickers."
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showNotoImportDialog() {
        val categories = arrayOf("Popular (Smileys, Star, Fire)", "Study (Books, Graduation, Clock)")
        TelegramNativeAlertDialog.Builder(this)
            .setTitle("Import Google Noto Emoji")
            .setItems(categories) { _, which ->
                val cat = if (which == 0) "popular" else "study"
                runImport {
                    val info = emojiRepo.importNotoEmoji(cat).getOrThrow()
                    if (!info.errors.isNullOrEmpty()) throw IllegalStateException("Imported ${info.importedCount}. ${info.errors.first()}")
                    "Imported ${info.importedCount} Noto animated emojis."
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun runImport(action: suspend () -> String) {
        if (importRunning) return
        importRunning = true
        btnImport.setLoading(true)
        lifecycleScope.launch {
            try { AppBulletin.showSuccess(this@EmojiStudioActivity, action()) }
            catch (e: kotlinx.coroutines.CancellationException) { throw e }
            catch (e: Exception) { AppBulletin.showError(this@EmojiStudioActivity, e.toUserFriendlyMessage()) }
            finally {
                importRunning = false
                btnImport.setLoading(false)
            }
            loadData()
        }
    }

    private fun uploadCustom(uri: android.net.Uri) = runImport {
        val (bytes, mime) = withContext(Dispatchers.IO) {
            val mime = contentResolver.getType(uri) ?: "application/json"
            val bytes = contentResolver.openInputStream(uri)?.use(EmojiUploadReader::read)
                ?: throw IllegalArgumentException("Unable to open the selected file.")
            bytes to mime
        }
        emojiRepo.upload(bytes, mime).getOrThrow()
        "Emoji uploaded."
    }

    private fun bindPersistedSwitch(
        cell: TextCheckCell,
        initial: Boolean,
        save: suspend (Boolean) -> Result<Unit>,
        onSaved: (Boolean) -> Unit
    ) {
        var confirmed = initial
        lateinit var listener: TelegramSwitch.OnCheckedChangeListener
        listener = TelegramSwitch.OnCheckedChangeListener { _, checked ->
            cell.isEnabled = false
            cell.checkBox.isEnabled = false
            lifecycleScope.launch {
                val result = save(checked)
                if (result.isSuccess) {
                    confirmed = checked
                    onSaved(checked)
                } else {
                    cell.setOnCheckedChangeListener(null)
                    cell.isChecked = confirmed
                    cell.setOnCheckedChangeListener(listener)
                    AppBulletin.showError(
                        this@EmojiStudioActivity,
                        result.exceptionOrNull()?.toUserFriendlyMessage() ?: "Update failed."
                    )
                }
                cell.isEnabled = true
                cell.checkBox.isEnabled = true
            }
        }
        cell.setOnCheckedChangeListener(listener)
    }
}
