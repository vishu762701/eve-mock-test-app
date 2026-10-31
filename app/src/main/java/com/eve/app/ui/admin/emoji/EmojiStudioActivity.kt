package com.eve.app.ui.admin.emoji

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.eve.app.R
import com.eve.app.data.model.EmojiItem
import com.eve.app.data.model.EmojiPack
import com.eve.app.data.remote.EmojiPacksResponseDto
import com.eve.app.data.repository.EmojiRepository
import com.eve.app.databinding.ActivityEmojiStudioBinding
import com.eve.app.databinding.BottomSheetEmojiPackManagerBinding
import com.eve.app.databinding.BottomSheetEmojiPlacementBinding
import com.eve.app.theme.TelegramThemeEngine
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
    private var displayedItems: List<EmojiItem> = emptyList()

    private var currentTabIndex = 0
    private var searchQuery = ""
    private var globalTelegramEnabled = true
    private var telegramBotConfigured = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEmojiStudioBinding.inflate(layoutInflater)
        setContentView(binding.root)

        AdminAccessGate.requireAdmin(this, binding.root, requireFreshServer = true) {
            setupViews()
            loadData()
        }
    }

    private fun setupViews() {
        // Setup Telegram Action Bar
        binding.actionBar.setTitle("Animated Emoji")
        binding.actionBar.setSubtitle("Emoji Studio & Placements")
        binding.actionBar.onBackPressed = { finish() }
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
        binding.progressLoading.visibility = View.VISIBLE
        binding.emptyState.visibility = View.GONE

        lifecycleScope.launch {
            val packsResult = emojiRepo.getPacks()
            val itemsResult = emojiRepo.getItems(limit = 100)

            binding.progressLoading.visibility = View.GONE

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
            val bundledUiItems = getBundledTelegramUiItems()
            allItems = bundledUiItems + serverItems

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
                byteSize = 1024,
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

        if (item.url.startsWith("res:raw/")) {
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

        sheetBinding.btnCancelPlacement.setOnClickListener { dialog.dismiss() }
        sheetBinding.btnApplyPlacement.setOnClickListener {
            val slotType = when (sheetBinding.rgSlotType.checkedRadioButtonId) {
                R.id.rbCategoryHeader -> "category_header"
                R.id.rbBroadcast -> "broadcast"
                else -> "exam_card"
            }
            val targetId = sheetBinding.etTargetId.text?.toString()?.trim() ?: ""
            if (targetId.isBlank()) {
                AppBulletin.showError(this, "Target ID is required")
                return@setOnClickListener
            }

            dialog.dismiss()
            lifecycleScope.launch {
                val res = emojiRepo.setPlacement(slotType, targetId, item.id)
                if (res.isSuccess) {
                    AppBulletin.showSuccess(this@EmojiStudioActivity, "Placed ${item.altEmoji} on $slotType!")
                } else {
                    AppBulletin.showError(this@EmojiStudioActivity, res.exceptionOrNull()?.message ?: "Failed to set placement")
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
        sheetBinding.cellGlobalTelegramSwitch.setOnCheckedChangeListener { _, checked ->
            globalTelegramEnabled = checked
            lifecycleScope.launch {
                val res = emojiRepo.toggleGlobalTelegramStatus(checked)
                if (res.isSuccess) {
                    AppBulletin.showSuccess(this@EmojiStudioActivity, "Telegram emoji ${if (checked) "enabled" else "disabled"}")
                } else {
                    AppBulletin.showError(this@EmojiStudioActivity, "Failed to update global switch")
                }
            }
        }

        // Telegram Bot Status
        if (telegramBotConfigured) {
            sheetBinding.cellTelegramBotStatus.setText("Telegram Bot Token is configured. Sticker imports are active.")
        } else {
            sheetBinding.cellTelegramBotStatus.setText("Telegram Bot Token is not configured. Telegram import is unavailable.")
        }

        sheetBinding.headerPacksList.setText("INSTALLED PACKS")

        // Render Packs List
        sheetBinding.containerPacks.removeAllViews()
        allPacks.forEach { pack ->
            val cell = TextCheckCell(this)
            cell.setTextAndCheck(pack.title, pack.enabled, needDivider = true)
            cell.setOnCheckedChangeListener { _, checked ->
                lifecycleScope.launch {
                    emojiRepo.togglePackStatus(pack.id, checked)
                }
            }

            // Long press to delete pack
            cell.setOnLongClickListener {
                AlertDialog.Builder(this)
                    .setTitle("Delete Pack")
                    .setMessage("Permanently takedown and delete '${pack.title}' and all its emoji files?")
                    .setPositiveButton("Delete Permanently") { _, _ ->
                        lifecycleScope.launch {
                            val res = emojiRepo.deletePack(pack.id)
                            if (res.isSuccess) {
                                AppBulletin.showSuccess(this@EmojiStudioActivity, "Pack removed permanently.")
                                dialog.dismiss()
                                loadData()
                            } else {
                                AppBulletin.showError(this@EmojiStudioActivity, "Failed to delete pack")
                            }
                        }
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
                true
            }

            sheetBinding.containerPacks.addView(cell)
        }

        dialog.show()
    }

    private fun showImportOptionsDialog() {
        val options = arrayOf(
            "Import Telegram Sticker Pack",
            "Import Google Noto Animated Emoji",
            "Upload Custom JSON/WebP"
        )

        AlertDialog.Builder(this)
            .setTitle("Import Emoji")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showTelegramImportDialog()
                    1 -> showNotoImportDialog()
                    2 -> AppBulletin.showSuccess(this, "Upload Lottie JSON via Admin API")
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showTelegramImportDialog() {
        if (!telegramBotConfigured) {
            AlertDialog.Builder(this)
                .setTitle("Telegram Import Not Configured")
                .setMessage("TELEGRAM_BOT_TOKEN is not configured on the Worker. Please set it as a Cloudflare Worker secret.")
                .setPositiveButton("OK", null)
                .show()
            return
        }

        val input = EditText(this).apply {
            hint = "Pack name or link (e.g. Duck or t.me/addstickers/...)"
            setSingleLine()
        }

        AlertDialog.Builder(this)
            .setTitle("Import Telegram Pack")
            .setView(input)
            .setPositiveButton("Import") { _, _ ->
                val packName = input.text.toString().trim()
                if (packName.isNotBlank()) {
                    binding.progressLoading.visibility = View.VISIBLE
                    lifecycleScope.launch {
                        val res = emojiRepo.importTelegramPack(packName)
                        binding.progressLoading.visibility = View.GONE
                        if (res.isSuccess) {
                            val info = res.getOrNull()
                            AppBulletin.showSuccess(
                                this@EmojiStudioActivity,
                                "Imported ${info?.importedCount ?: 0} stickers from '${info?.packTitle}'"
                            )
                            loadData()
                        } else {
                            AppBulletin.showError(this@EmojiStudioActivity, res.exceptionOrNull()?.message ?: "Failed")
                        }
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showNotoImportDialog() {
        val categories = arrayOf("Popular (Smileys, Star, Fire)", "Study (Books, Graduation, Clock)")
        AlertDialog.Builder(this)
            .setTitle("Import Google Noto Emoji")
            .setItems(categories) { _, which ->
                val cat = if (which == 0) "popular" else "study"
                binding.progressLoading.visibility = View.VISIBLE
                lifecycleScope.launch {
                    val res = emojiRepo.importNotoEmoji(cat)
                    binding.progressLoading.visibility = View.GONE
                    if (res.isSuccess) {
                        AppBulletin.showSuccess(this@EmojiStudioActivity, "Imported ${res.getOrNull()?.importedCount ?: 0} Noto animated emojis!")
                        loadData()
                    } else {
                        AppBulletin.showError(this@EmojiStudioActivity, "Failed to import Noto emojis")
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showItemDetailsDialog(item: EmojiItem) {
        val details = StringBuilder()
            .append("Alt Emoji: ${item.altEmoji}\n")
            .append("Source: ${item.source}\n")
            .append("Pack: ${item.packTitle ?: item.packSlug ?: "N/A"}\n")
            .append("Kind: ${item.kind}\n")
            .append("Size: ${item.byteSize} bytes\n")
            .append("Premium Origin: ${if (item.premiumOrigin) "Yes (Telegram)" else "No"}\n")
            .append("Storage: ${item.storageKey ?: item.url}")

        AlertDialog.Builder(this)
            .setTitle("Emoji Details")
            .setMessage(details.toString())
            .setPositiveButton("OK", null)
            .show()
    }
}
