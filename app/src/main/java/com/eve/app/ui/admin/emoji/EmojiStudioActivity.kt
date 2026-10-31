package com.eve.app.ui.admin.emoji

import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import android.widget.EditText
import androidx.activity.result.contract.ActivityResultContracts
import com.eve.app.designsystem.TelegramNativeAlertDialog
import coil.load
import com.eve.app.designsystem.TelegramSwitch
import com.eve.app.data.remote.toUserFriendlyMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.withContext
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.eve.app.R
import com.eve.app.data.model.EmojiItem
import com.eve.app.data.model.EmojiPack
import com.eve.app.data.repository.EmojiRepository
import com.eve.app.databinding.ActivityEmojiStudioBinding
import com.eve.app.databinding.BottomSheetEmojiPackManagerBinding
import com.eve.app.databinding.BottomSheetEmojiPlacementBinding
import com.eve.app.ui.admin.tg.TextCheckCell
import com.eve.app.ui.common.EveBaseActivity
import com.eve.app.util.AdminAccessGate
import com.eve.app.util.AppBulletin
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.launch

class EmojiStudioActivity : EveBaseActivity() {

    private lateinit var binding: ActivityEmojiStudioBinding
    private val emojiRepo = EmojiRepository()
    private lateinit var adapter: EmojiGridAdapter

    private var allPacks: List<EmojiPack> = emptyList()
    private var allItems: List<EmojiItem> = emptyList()
    private var serverItemIds: Set<String> = emptySet()
    private var displayedItems: List<EmojiItem> = emptyList()

    private var currentTabIndex = 0
    private var searchQuery = ""
    private var globalTelegramEnabled = true
    private var telegramBotConfigured = false
    private var dataJob: Job? = null
    private var importRunning = false
    private var authorized = false
    private var pendingUpload: android.net.Uri? = null
    private val uploadPicker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            if (authorized) uploadCustom(uri) else pendingUpload = uri
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEmojiStudioBinding.inflate(layoutInflater)
        setContentView(binding.root)

        AdminAccessGate.requireAdmin(this, binding.root, requireFreshServer = true) {
            authorized = true
            setupViews()
            loadData()
            pendingUpload?.let { pendingUpload = null; uploadCustom(it) }
        }
    }

    private fun setupViews() {
        // Setup Telegram Action Bar
        binding.actionBar.setTitle("Animated Emoji")
        binding.actionBar.setSubtitle("Emoji Studio & Placements")
        binding.actionBar.onBackPressed = { finish() }
        binding.actionBar.bindSystemBack(this)
        binding.actionBar.onSearchTextChanged = { query ->
            searchQuery = query.trim().lowercase()
            filterAndDisplayItems()
        }
        binding.actionBar.onSearchCollapsed = {
            searchQuery = ""
            filterAndDisplayItems()
        }

        // Setup Grid RecyclerView
        adapter = EmojiGridAdapter(
            onItemClick = { item -> showPlacementBottomSheet(item) },
            onItemLongClick = { item -> showItemDetailsDialog(item) }
        )
        binding.rvEmojiGrid.layoutManager = GridLayoutManager(this, 4)
        binding.rvEmojiGrid.adapter = adapter

        // Setup Buttons
        binding.btnManagePacks.setOnClickListener { showPackManagerBottomSheet() }
        binding.btnImport.setOnClickListener { showImportOptionsDialog() }
    }

    private fun loadData() {
        dataJob?.cancel()
        binding.progressLoading.visibility = View.VISIBLE
        binding.emptyState.visibility = View.GONE

        dataJob = lifecycleScope.launch {
            val packsResult = emojiRepo.getPacks()
            val itemsResult = emojiRepo.getAllItems()

            binding.progressLoading.visibility = View.GONE

            if (packsResult.isFailure || itemsResult.isFailure) {
                AppBulletin.showError(this@EmojiStudioActivity,
                    (packsResult.exceptionOrNull() ?: itemsResult.exceptionOrNull())!!.toUserFriendlyMessage())
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
            // Older Workers may omit bundled IDs. Preview remains available, placement
            // requires an actual server item; never resurrect a deleted pack.
            val bundledUiItems = if (allPacks.any { it.id == "pack_tg_ui" }) getBundledTelegramUiItems() else emptyList()
            serverItemIds = serverItems.map { it.id }.toSet()
            allItems = (serverItems + bundledUiItems).distinctBy { it.id }

            setupTabs()
            filterAndDisplayItems()
        }
    }

    private fun setupTabs() {
        val tabTitles = mutableListOf("All", "Noto", "Telegram UI")
        allPacks.forEach { pack ->
            if (pack.source != "telegram-ui" && pack.source != "noto" && !tabTitles.contains(pack.title)) {
                tabTitles.add(pack.title)
            }
        }
        currentTabIndex = 0
        binding.segmentedTabs.setTabs(tabTitles)
        binding.segmentedTabs.onTabSelectedListener = { pos ->
            currentTabIndex = pos
            filterAndDisplayItems()
        }
    }

    private fun filterAndDisplayItems() {
        val tabName = when (currentTabIndex) {
            0 -> "All"
            1 -> "Noto"
            2 -> "Telegram UI"
            else -> {
                val extraPacks = allPacks.filter { it.source != "telegram-ui" && it.source != "noto" }
                val idx = currentTabIndex - 3
                if (idx in extraPacks.indices) extraPacks[idx].title else "All"
            }
        }

        displayedItems = allItems.filter { item ->
            val matchesTab = when (tabName) {
                "All" -> true
                "Noto" -> item.source == "noto" || item.packSlug == "noto_animated_emoji"
                "Telegram UI" -> item.source == "telegram-ui"
                else -> item.packTitle.equals(tabName, ignoreCase = true)
            }
            val matchesSearch = if (searchQuery.isBlank()) true else {
                item.altEmoji.contains(searchQuery) ||
                (item.packTitle?.lowercase()?.contains(searchQuery) == true) ||
                (item.source.lowercase().contains(searchQuery))
            }
            matchesTab && matchesSearch
        }

        adapter.submitList(displayedItems)
        binding.emptyState.visibility = if (displayedItems.isEmpty()) View.VISIBLE else View.GONE
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

        return rawIcons.map { (resName, alt, name) ->
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

    private fun showPlacementBottomSheet(item: EmojiItem) {
        val dialog = BottomSheetDialog(this)
        val sheetBinding = BottomSheetEmojiPlacementBinding.inflate(layoutInflater)
        dialog.setContentView(sheetBinding.root)

        sheetBinding.tvPlacementEmojiTitle.text = "${item.altEmoji} Placement"
        sheetBinding.tvPlacementPackName.text = item.packTitle ?: item.source

        if (item.kind == "static") {
            sheetBinding.ivPlacementPreview.load(item.url)
        } else if (item.url.startsWith("res:raw/")) {
            val resName = item.url.removePrefix("res:raw/")
            val resId = resources.getIdentifier(resName, "raw", packageName)
            if (resId != 0) {
                sheetBinding.ivPlacementPreview.setAnimation(resId)
                sheetBinding.ivPlacementPreview.playAnimation()
            }
        } else if (item.url.isNotBlank()) {
            sheetBinding.ivPlacementPreview.setAnimationFromUrl(item.url)
            sheetBinding.ivPlacementPreview.playAnimation()
        }

        sheetBinding.rgSlotType.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.rbExamCard -> sheetBinding.tilTargetId.hint = "Exam ID (e.g. physics_test_1)"
                R.id.rbCategoryHeader -> sheetBinding.tilTargetId.hint = "Category Name (e.g. Science)"
                R.id.rbBroadcast -> sheetBinding.tilTargetId.hint = "Broadcast ID"
            }
        }

        // This source-derived TextCell makes the existing Worker DELETE route
        // reachable without bypassing permission checks or claiming early success.
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
                                    AppBulletin.showError(this@EmojiStudioActivity,
                                        result.exceptionOrNull()?.toUserFriendlyMessage() ?: "Unable to remove placement.")
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
                        dialog.dismiss()
                        AppBulletin.showSuccess(this@EmojiStudioActivity, "Placed ${item.altEmoji} on $slotType!")
                    } else {
                        AppBulletin.showError(this@EmojiStudioActivity,
                            res.exceptionOrNull()?.toUserFriendlyMessage() ?: "Failed to set placement")
                    }
                } finally {
                    setRequestRunning(false)
                }
            }
        }

        dialog.show()
    }

    private fun showPackManagerBottomSheet() {
        val dialog = BottomSheetDialog(this)
        val sheetBinding = BottomSheetEmojiPackManagerBinding.inflate(layoutInflater)
        dialog.setContentView(sheetBinding.root)

        // Global Telegram Kill Switch
        sheetBinding.cellGlobalTelegramSwitch.setTextAndCheck(
            "Global Telegram Emoji Switch",
            globalTelegramEnabled,
            needDivider = true
        )
        bindPersistedSwitch(sheetBinding.cellGlobalTelegramSwitch, globalTelegramEnabled,
            save = { emojiRepo.toggleGlobalTelegramStatus(it) },
            onSaved = { globalTelegramEnabled = it })

        sheetBinding.cellTelegramBotStatus.visibility = if (telegramBotConfigured) View.VISIBLE else View.GONE
        if (telegramBotConfigured) sheetBinding.cellTelegramBotStatus.setText("Telegram sticker imports are available.")

        sheetBinding.headerPacksList.setText("INSTALLED PACKS")

        // Render Packs List
        sheetBinding.containerPacks.removeAllViews()
        allPacks.forEach { pack ->
            val cell = TextCheckCell(this)
            cell.setTextAndCheck(pack.title, pack.enabled, needDivider = true)
            bindPersistedSwitch(cell, pack.enabled,
                save = { emojiRepo.togglePackStatus(pack.id, it) },
                onSaved = { enabled -> allPacks = allPacks.map { if (it.id == pack.id) it.copy(enabled = enabled) else it } })

            // Long press to delete pack
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
        TelegramNativeAlertDialog.Builder(this)
            .setTitle("Import Emoji")
            .setItems(actions.map { it.first }.toTypedArray()) { _, which -> actions[which].second() }
            .setNegativeButton("Cancel", null).show()
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
        binding.btnImport.isEnabled = false
        binding.progressLoading.visibility = View.VISIBLE
        lifecycleScope.launch {
            try { AppBulletin.showSuccess(this@EmojiStudioActivity, action()) }
            catch (e: kotlinx.coroutines.CancellationException) { throw e }
            catch (e: Exception) { AppBulletin.showError(this@EmojiStudioActivity, e.toUserFriendlyMessage()) }
            finally {
                importRunning = false
                binding.btnImport.isEnabled = true
                binding.progressLoading.visibility = View.GONE
            }
            loadData() // Partial imports also need honest readback.
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

    private fun bindPersistedSwitch(cell: TextCheckCell, initial: Boolean,
        save: suspend (Boolean) -> Result<Unit>, onSaved: (Boolean) -> Unit) {
        var confirmed = initial
        lateinit var listener: TelegramSwitch.OnCheckedChangeListener
        listener = TelegramSwitch.OnCheckedChangeListener { _, checked ->
            cell.isEnabled = false
            cell.checkBox.isEnabled = false
            lifecycleScope.launch {
                val result = save(checked)
                if (result.isSuccess) { confirmed = checked; onSaved(checked) }
                else {
                    cell.setOnCheckedChangeListener(null)
                    cell.isChecked = confirmed
                    cell.setOnCheckedChangeListener(listener)
                    AppBulletin.showError(this@EmojiStudioActivity,
                        result.exceptionOrNull()?.toUserFriendlyMessage() ?: "Update failed.")
                }
                cell.isEnabled = true
                cell.checkBox.isEnabled = true
            }
        }
        cell.setOnCheckedChangeListener(listener)
    }

    private fun showItemDetailsDialog(item: EmojiItem) {
        val details = StringBuilder()
            .append("Alt Emoji: ${item.altEmoji}\n")
            .append("Source: ${item.source}\n")
            .append("Pack: ${item.packTitle ?: item.packSlug ?: "N/A"}\n")
            .append("Kind: ${item.kind}\n")
            .append("Size: ${if (item.byteSize > 0) "${item.byteSize} bytes" else "Not registered on server"}\n")
            .append("Premium Origin: ${if (item.premiumOrigin) "Yes (Telegram)" else "No"}\n")
            .append("Storage: ${item.storageKey ?: item.url}")

        TelegramNativeAlertDialog.Builder(this)
            .setTitle("Emoji Details")
            .setMessage(details.toString())
            .setPositiveButton("OK", null)
            .show()
    }
}
