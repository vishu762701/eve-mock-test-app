/*
 * Derived from Telegram Android ProfileActivity.java, Copyright Nikolai Kudashov.
 * GNU GPL v2 or later. See THIRD_PARTY_NOTICES.md.
 */

package com.eve.app.ui.profile

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.NumberPicker
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.widget.TextViewCompat
import androidx.lifecycle.lifecycleScope
import coil.load
import com.eve.app.R
import com.eve.app.data.model.PremiumStatusDto
import com.eve.app.data.remote.ApiClient
import com.eve.app.data.repository.AdminRepository
import com.eve.app.data.repository.PremiumRepository
import com.eve.app.databinding.ActivityProfileBinding
import com.eve.app.designsystem.TelegramNativeAlertDialog
import com.eve.app.designsystem.TelegramRadialProgressView
import com.eve.app.ui.common.EveBaseActivity
import com.eve.app.ui.settings.SettingsActivity
import com.eve.app.util.AppBulletin
import com.eve.app.util.EveNavigationHelper
import com.eve.app.util.ProfilePhotoManager
import com.eve.app.util.ReminderScheduler
import com.eve.app.util.SessionManager
import com.eve.app.util.isHardcodedAdmin
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class ProfileActivity : EveBaseActivity() {

    private lateinit var binding: ActivityProfileBinding
    private val adminRepo = AdminRepository()
    private var progressDialog: TelegramNativeAlertDialog? = null
    private var pendingPhotoUri: Uri? = null

    private var currentDob: String = ""
    private var currentCategory: String = "General"
    private var currentPhone: String = ""
    private var currentUsername: String = ""

    private val pickPhotoLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) {
                previewSelectedPhoto(uri)
            }
        }

    private val editInfoLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                loadProfile()
            }
        }

    private val editUsernameLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                loadProfile()
            }
        }

    private val editPhoneLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                loadProfile()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.glassToolbar.bringToFront()

        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            finish()
            return
        }

        binding.btnBack.setOnClickListener {
            EveNavigationHelper.finishWithTransition(this)
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (pendingPhotoUri != null) {
                    cancelPhotoPreview()
                } else {
                    finish()
                }
            }
        })

        setupScrollAndGooey()
        setupActions()
        setupCells()
        setupAdminAndPremium(user.email)

        loadProfilePhoto()
        loadProfile()
    }

    private fun setupScrollAndGooey() {
        val density = resources.displayMetrics.density
        val collapseRange = 100f * density

        binding.profileScrollView.setOnScrollChangeListener { _, _, scrollY, _, _ ->
            val progress = (scrollY.toFloat() / collapseRange).coerceIn(0f, 1f)

            // Gooey avatar update (ProfileActivity.java:6039-6047)
            binding.avatarGooey.setPullProgress(progress)
            val blur = ((progress.coerceIn(0.2f, 0.7f) - 0.2f) / 0.5f).coerceAtMost(0.75f)
            binding.avatarGooey.setBlurIntensity(blur)
            binding.avatarGooey.setGooeyEnabled(progress > 0f && progress < 1f)

            // Avatar translation towards notch & scale (ProfileActivity.java:8730-8740)
            val endScale = binding.avatarGooey.getAvatarEndScale()
            val currentScale = 1f - (1f - endScale) * progress
            binding.avatarContainer.scaleX = currentScale
            binding.avatarContainer.scaleY = currentScale

            val endOffset = binding.avatarGooey.getEndOffset(true, currentScale)
            binding.avatarContainer.translationY = endOffset * progress

            // Alpha fading of toolbar title vs avatar/name
            binding.tvToolbarTitle.alpha = progress
            val headerContentAlpha = (1f - progress * 1.5f).coerceIn(0f, 1f)
            binding.tvName.alpha = headerContentAlpha
            binding.tvStatus.alpha = headerContentAlpha
            binding.layoutActions.alpha = headerContentAlpha
        }
    }

    private fun setupActions() {
        binding.btnSetPhoto.setOnClickListener {
            pickPhotoLauncher.launch("image/*")
        }
        binding.ivProfilePhoto.setOnClickListener {
            pickPhotoLauncher.launch("image/*")
        }
        binding.btnCancelPhoto.setOnClickListener {
            cancelPhotoPreview()
        }
        binding.btnSavePhoto.setOnClickListener {
            savePendingPhoto()
        }

        binding.btnEditInfo.setOnClickListener {
            val user = FirebaseAuth.getInstance().currentUser
            val intent = Intent(this, ChangeNameActivity::class.java).apply {
                putExtra("current_name", user?.displayName ?: binding.tvName.text.toString())
            }
            editInfoLauncher.launch(intent)
        }

        binding.btnSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
    }

    private fun setupCells() {
        // Mobile cell
        binding.cellMobile.setOnClickListener {
            val intent = Intent(this, ChangePhoneActivity::class.java).apply {
                putExtra("current_phone", currentPhone)
            }
            editPhoneLauncher.launch(intent)
        }

        // Username cell
        binding.cellUsername.setOnClickListener {
            val intent = Intent(this, ChangeUsernameActivity::class.java).apply {
                putExtra("current_username", currentUsername)
            }
            editUsernameLauncher.launch(intent)
        }

        // Birthday cell
        binding.cellBirthday.setOnClickListener {
            showBirthdayPicker(currentDob)
        }

        // Category cell & expansion
        binding.cellCategory.setOnClickListener {
            val isExpanded = binding.layoutCategoryOptions.visibility == View.VISIBLE
            if (isExpanded) {
                binding.layoutCategoryOptions.visibility = View.GONE
                binding.ivCategoryArrow.animate().rotation(0f).setDuration(150).start()
            } else {
                binding.layoutCategoryOptions.visibility = View.VISIBLE
                binding.ivCategoryArrow.animate().rotation(90f).setDuration(150).start()
            }
        }

        binding.radioGeneral.setOnClickListener { selectCategory("General") }
        binding.radioObc.setOnClickListener { selectCategory("OBC") }
        binding.radioSc.setOnClickListener { selectCategory("SC") }
        binding.radioSt.setOnClickListener { selectCategory("ST") }

        // Eve ID copy
        val copyEveIdAction = {
            val eveId = binding.tvEveId.text.toString().trim()
            if (eveId.isNotBlank() && eveId != "--" && eveId != "Loading...") {
                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("EVE ID", eveId)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(this, "EVE ID copied to clipboard", Toast.LENGTH_SHORT).show()
            }
        }
        binding.cellEveId.setOnClickListener { copyEveIdAction() }
        binding.btnCopyEveId.setOnClickListener { copyEveIdAction() }

        // Daily Reminder toggle with TelegramSwitch
        binding.switchReminder.isChecked = ReminderScheduler.isEnabled(this)
        binding.switchReminder.setOnCheckedChangeListener { _, isChecked ->
            ReminderScheduler.setEnabled(this, isChecked)
        }
        binding.cellReminder.setOnClickListener {
            binding.switchReminder.toggle()
        }
    }

    private fun selectCategory(cat: String) {
        currentCategory = cat
        updateCategoryRadios(cat)
        binding.tvCategoryValue.text = cat

        // Collapse category dropdown
        binding.layoutCategoryOptions.visibility = View.GONE
        binding.ivCategoryArrow.animate().rotation(0f).setDuration(150).start()

        // Save immediately to server
        lifecycleScope.launch {
            try {
                val res = ApiClient.apiService.updateProfile(mapOf("category" to cat))
                if (res.success) {
                    AppBulletin.showSuccess(this@ProfileActivity, "Category updated to $cat")
                } else {
                    AppBulletin.showError(this@ProfileActivity, res.error ?: "Failed to update category")
                }
            } catch (e: Exception) {
                AppBulletin.showError(this@ProfileActivity, e.localizedMessage ?: "Failed to update category")
            }
        }
    }

    private fun updateCategoryRadios(cat: String) {
        val upper = cat.uppercase(Locale.US)
        binding.radioGeneral.isChecked = upper == "GENERAL"
        binding.radioObc.isChecked = upper == "OBC"
        binding.radioSc.isChecked = upper == "SC"
        binding.radioSt.isChecked = upper == "ST"
    }

    private fun showBirthdayPicker(dob: String) {
        val cal = Calendar.getInstance()
        if (dob.isNotBlank()) {
            val parts = dob.split("/")
            if (parts.size == 3) {
                val d = parts[0].toIntOrNull()
                val m = parts[1].toIntOrNull()
                val y = parts[2].toIntOrNull()
                if (d != null && m != null && y != null) {
                    cal.set(y, m - 1, d)
                }
            }
        } else {
            cal.add(Calendar.YEAR, -18)
        }

        val context = this
        val density = resources.displayMetrics.density
        fun dp(v: Int): Int = (v * density).toInt()

        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            weightSum = 1.0f
            setPadding(dp(16), dp(12), dp(16), dp(12))
        }

        val dayPicker = NumberPicker(context).apply {
            minValue = 1
            maxValue = 31
            value = cal.get(Calendar.DAY_OF_MONTH)
        }

        val months = arrayOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
        val monthPicker = NumberPicker(context).apply {
            minValue = 0
            maxValue = 11
            displayedValues = months
            value = cal.get(Calendar.MONTH)
        }

        val curYear = Calendar.getInstance().get(Calendar.YEAR)
        val yearPicker = NumberPicker(context).apply {
            minValue = 1940
            maxValue = curYear
            value = cal.get(Calendar.YEAR)
        }

        fun updateDays() {
            val tempCal = Calendar.getInstance()
            tempCal.set(Calendar.YEAR, yearPicker.value)
            tempCal.set(Calendar.MONTH, monthPicker.value)
            dayPicker.maxValue = tempCal.getActualMaximum(Calendar.DAY_OF_MONTH)
        }
        monthPicker.setOnValueChangedListener { _, _, _ -> updateDays() }
        yearPicker.setOnValueChangedListener { _, _, _ -> updateDays() }
        updateDays()

        layout.addView(dayPicker, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 0.3f))
        layout.addView(monthPicker, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 0.35f))
        layout.addView(yearPicker, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 0.35f))

        TelegramNativeAlertDialog.Builder(context)
            .setTitle("Date of Birth")
            .setView(layout)
            .setPositiveButton("Set") { _, _ ->
                val formatted = String.format(Locale.US, "%02d/%02d/%04d", dayPicker.value, monthPicker.value + 1, yearPicker.value)
                saveBirthday(formatted)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun saveBirthday(dob: String) {
        currentDob = dob
        binding.tvBirthdayValue.text = formatTelegramBirthday(dob)

        lifecycleScope.launch {
            try {
                val res = ApiClient.apiService.updateProfile(mapOf("dob" to dob))
                if (res.success) {
                    AppBulletin.showSuccess(this@ProfileActivity, "Birthday updated")
                } else {
                    AppBulletin.showError(this@ProfileActivity, res.error ?: "Failed to update birthday")
                }
            } catch (e: Exception) {
                AppBulletin.showError(this@ProfileActivity, e.localizedMessage ?: "Failed to update birthday")
            }
        }
    }

    private fun formatTelegramBirthday(dobString: String): String {
        if (dobString.isBlank()) return "Add your birthday"
        val parts = dobString.split("/")
        if (parts.size != 3) return dobString
        val d = parts[0].toIntOrNull() ?: return dobString
        val m = parts[1].toIntOrNull() ?: return dobString
        val y = parts[2].toIntOrNull() ?: return dobString

        val cal = Calendar.getInstance()
        cal.set(Calendar.YEAR, y)
        cal.set(Calendar.MONTH, m - 1)
        cal.set(Calendar.DAY_OF_MONTH, d)

        val today = Calendar.getInstance()
        var age = today.get(Calendar.YEAR) - y
        if (today.get(Calendar.DAY_OF_YEAR) < cal.get(Calendar.DAY_OF_YEAR)) {
            age--
        }

        val dateStr = SimpleDateFormat("MMM d, yyyy", Locale.US).format(cal.time)
        return "$dateStr ($age years old)"
    }

    private fun setupAdminAndPremium(email: String?) {
        val setupAdminUi = {
            binding.tvStatus.text = "Admin"
            binding.chipAdmin.visibility = View.VISIBLE
            binding.cellEditAbout.visibility = View.VISIBLE
            binding.cellEditAbout.setOnClickListener {
                startActivity(Intent(this, com.eve.app.ui.admin.EditAboutActivity::class.java))
            }
        }

        val cachedAdmin = SessionManager.getCachedAdminStatus(email, allowStale = true)
            ?: isHardcodedAdmin(email)
        if (cachedAdmin) {
            setupAdminUi()
        }
        lifecycleScope.launch {
            if (adminRepo.isAdmin(email)) setupAdminUi()
        }

        val updatePremiumBadge = { status: PremiumStatusDto? ->
            val isPrem = status?.isPremium ?: PremiumRepository.isCurrentUserPremium(this)
            if (isPrem) {
                binding.chipPremium.visibility = View.VISIBLE
                val exp = status?.expiresAt
                if (status?.isLifetime == true) {
                    binding.chipPremium.text = "Premium • Lifetime"
                } else if (exp != null && exp > 0) {
                    val dateStr = SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date(exp))
                    binding.chipPremium.text = "Premium Active • Expires $dateStr"
                } else {
                    binding.chipPremium.text = "Premium Active"
                }
            } else {
                binding.chipPremium.visibility = View.GONE
            }
        }
        updatePremiumBadge(null)
        binding.chipPremium.setOnClickListener {
            startActivity(Intent(this, com.eve.app.ui.premium.PremiumActivity::class.java))
        }
        lifecycleScope.launch {
            val res = PremiumRepository.refreshStatus(this@ProfileActivity)
            if (res.isSuccess) {
                updatePremiumBadge(res.getOrNull())
            }
        }
    }

    private fun loadProfile() {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        val currentName = user.displayName?.takeIf { it.isNotBlank() } ?: "Student"
        binding.tvName.text = currentName
        binding.tvToolbarTitle.text = currentName

        lifecycleScope.launch {
            try {
                val res = ApiClient.apiService.getProfile()
                if (res.success && res.data != null) {
                    val profile = res.data

                    // Eve ID
                    val eveId = profile.eveId ?: "--"
                    binding.tvEveId.text = eveId

                    // Status line (Admin for admins, or Eve ID)
                    if (binding.chipAdmin.visibility != View.VISIBLE) {
                        binding.tvStatus.text = if (eveId != "--") "Eve ID: $eveId" else "Student"
                    }

                    // Name
                    if (profile.displayName.isNotBlank()) {
                        binding.tvName.text = profile.displayName
                        binding.tvToolbarTitle.text = profile.displayName
                    }

                    // Mobile
                    currentPhone = profile.phone ?: ""
                    binding.tvMobileValue.text = if (currentPhone.isNotBlank()) currentPhone else "Add phone number"

                    // Username
                    currentUsername = profile.username ?: ""
                    val displayUsername = if (currentUsername.isNotBlank()) {
                        if (currentUsername.startsWith("@")) currentUsername else "@$currentUsername"
                    } else {
                        "Set username"
                    }
                    binding.tvUsernameValue.text = displayUsername

                    // Birthday
                    currentDob = profile.dob
                    binding.tvBirthdayValue.text = formatTelegramBirthday(currentDob)

                    // Category
                    currentCategory = if (profile.category.isNotBlank()) profile.category else "General"
                    binding.tvCategoryValue.text = currentCategory
                    updateCategoryRadios(currentCategory)
                }
            } catch (e: Exception) {
                android.util.Log.w("ProfileActivity", "Error loading profile: ${e.message}")
            }
        }
    }

    private fun loadProfilePhoto() {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        ProfilePhotoManager.applyTo(
            this,
            binding.ivProfilePhoto,
            user.photoUrl?.toString(),
            R.drawable.design_avatar_surface,
            userName = user.displayName,
            userId = user.uid
        )
    }

    private fun previewSelectedPhoto(uri: Uri) {
        pendingPhotoUri = uri
        binding.ivProfilePhoto.background = null
        binding.ivProfilePhoto.setPadding(0, 0, 0, 0)
        binding.ivProfilePhoto.scaleType = ImageView.ScaleType.CENTER_CROP
        binding.ivProfilePhoto.load(uri) {
            crossfade(true)
            placeholder(R.drawable.ic_person)
            error(R.drawable.ic_person)
        }
        binding.layoutPhotoActions.visibility = View.VISIBLE
    }

    private fun cancelPhotoPreview() {
        pendingPhotoUri = null
        binding.layoutPhotoActions.visibility = View.GONE
        loadProfilePhoto()
    }

    private fun savePendingPhoto() {
        val uri = pendingPhotoUri ?: return
        if (ProfilePhotoManager.savePhoto(this, uri)) {
            AppBulletin.showSuccess(this, "Profile photo updated")
        } else {
            AppBulletin.showError(this, "Failed to save photo")
        }
        pendingPhotoUri = null
        binding.layoutPhotoActions.visibility = View.GONE
        loadProfilePhoto()
    }

    private fun showProgress(msg: String) {
        dismissProgress()
        val builder = TelegramNativeAlertDialog.Builder(this)
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            val space = resources.getDimensionPixelSize(R.dimen.design_card_padding)
            setPadding(space, space, space, space)
            gravity = android.view.Gravity.CENTER_VERTICAL
            addView(TelegramRadialProgressView(this@ProfileActivity), LinearLayout.LayoutParams(
                resources.getDimensionPixelSize(R.dimen.design_ring_size),
                resources.getDimensionPixelSize(R.dimen.design_ring_size)))
            addView(TextView(this@ProfileActivity).apply {
                text = msg
                setPadding(resources.getDimensionPixelSize(R.dimen.design_section_gap), 0, 0, 0)
                TextViewCompat.setTextAppearance(this, R.style.TextAppearance_Eve_Token_Body)
            })
        }
        builder.setView(layout)
        builder.setCancelable(false)
        progressDialog = builder.create().apply { show() }
    }

    private fun dismissProgress() {
        progressDialog?.dismiss()
        progressDialog = null
    }

    override fun onDestroy() {
        progressDialog?.dismiss()
        progressDialog = null
        super.onDestroy()
    }
}
