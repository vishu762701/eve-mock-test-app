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
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.NumberPicker
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.doOnLayout
import androidx.core.widget.TextViewCompat
import androidx.lifecycle.lifecycleScope
import coil.load
import com.eve.app.R
import com.eve.app.data.model.PremiumStatusDto
import com.eve.app.data.remote.ApiClient
import com.eve.app.data.repository.AdminRepository
import com.eve.app.data.repository.PremiumRepository
import com.eve.app.databinding.ActivityProfileBinding
import com.eve.app.theme.TelegramThemeEngine
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
import kotlin.math.roundToInt

class ProfileActivity : EveBaseActivity() {

    private lateinit var binding: ActivityProfileBinding
    private val adminRepo = AdminRepository()
    private var progressDialog: TelegramNativeAlertDialog? = null
    private var pendingPhotoUri: Uri? = null

    private var currentDob: String = ""
    private var currentCategory: String = "General"
    private var currentPhone: String = ""
    private var currentUsername: String = ""
    // Null means server Bio support has not been deployed; empty is a valid cleared Bio.
    private var currentBio: String? = null

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

    private val editBioLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                loadProfile()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ProfileBioCapability.apply(binding.cellBio, binding.tvBioValue, null)
        binding.glassToolbar.bringToFront()
        setupAvatarWindowHost()

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
        setupProfileActionBounce()
        setupProfileActionAccessibility()
        setupCells()
        setupAdminAndPremium(user.email)

        loadProfilePhoto()
        loadProfile()
    }

    /**
     * Telegram ProfileActivity.java:5354-5359 renders ProfileGooeyView in a
     * MATCH_PARENT avatar host. EVE keeps the scroll gesture target in its
     * NestedScrollView and paints the real photo behind it, at window scale.
     */
    private fun setupAvatarWindowHost() {
        val contentRoot = findViewById<ViewGroup>(android.R.id.content)
        // The original profile camera transition is allowed to draw into the
        // system cutout, not clipped at the padded content root boundary.
        contentRoot?.clipChildren = false
        contentRoot?.clipToPadding = false
        binding.root.clipChildren = false
        binding.root.clipToPadding = false

        binding.root.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            positionFullWindowAvatarHost()
        }
        binding.root.doOnLayout { positionFullWindowAvatarHost() }
    }

    private fun positionFullWindowAvatarHost() {
        val contentRoot = findViewById<ViewGroup>(android.R.id.content)
        val insets = ViewCompat.getRootWindowInsets(binding.root)
        val physicalTop = insets?.getInsets(
            WindowInsetsCompat.Type.statusBars() or WindowInsetsCompat.Type.displayCutout()
        )?.top ?: 0
        // SystemBarHelper may have padded android.R.id.content before dispatch
        // reaches this child. Prefer the authoritative larger inset.
        val statusTop = maxOf(physicalTop, contentRoot?.paddingTop ?: 0)
        val image = binding.avatarContainer
        val params = image.layoutParams as FrameLayout.LayoutParams
        val desiredTop = (66f * resources.displayMetrics.density).roundToInt() + statusTop
        if (params.topMargin != desiredTop) {
            params.topMargin = desiredTop
            image.layoutParams = params
        }
        binding.avatarGooey.translationY = -statusTop.toFloat()
    }

    private fun setupScrollAndGooey() {
        val density = resources.displayMetrics.density
        binding.profileScrollView.setOnScrollChangeListener { _, _, scrollY, _, _ ->
            renderProfileScrollState(scrollY, density)
        }
        // NestedScrollView restores saved scroll after layout. Reapply ALL
        // header effects, not just button opacity, when recreated/rotated.
        binding.profileScrollView.doOnLayout {
            renderProfileScrollState(binding.profileScrollView.scrollY, density)
        }
    }

    private fun renderProfileScrollState(scrollY: Int, density: Float) {
        val progress = ProfileScrollState.progress(scrollY, density)
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

        val info = binding.avatarGooey.notchInfo
        binding.avatarContainer.translationY = if (info != null) {
            // Telegram ProfileGooeyView.java:397-405 uses this center for
            // the round front-camera mask. The EVE scroll trajectory is
            // adapted because the renderer now lives outside the scroll.
            val targetCenterY = if (info.isLikelyCircle) {
                info.bounds.bottom - info.bounds.width() / 2f
            } else {
                info.bounds.centerY()
            }
            val initialCenterY = binding.avatarContainer.top +
                binding.avatarContainer.height / 2f
            ProfileAvatarTrajectory.offsetToNotch(initialCenterY, targetCenterY, progress)
        } else {
            // Without a built-in cutout, retain the original scrolling
            // header displacement and source dynamic status-bar offset.
            val endOffset = binding.avatarGooey.getEndOffset(true, currentScale)
            -scrollY.toFloat() + endOffset * progress
        }

        // Alpha fading of toolbar title vs avatar/name
        binding.tvToolbarTitle.alpha = progress
        val headerContentAlpha = (1f - progress * 1.5f).coerceIn(0f, 1f)
        binding.tvName.alpha = headerContentAlpha
        binding.tvStatus.alpha = headerContentAlpha

        // Telegram ProfileActionsView.java:247-337 stages backgrounds and
        // icon/text independently as the 74dp action area collapses.
        ProfileActionsCollapse.applyTo(
            listOf(binding.btnSetPhoto, binding.btnEditInfo, binding.btnSettings),
            ProfileActionsCollapse.atScroll(scrollY.toFloat(), density)
        )
    }

    private fun setupActions() {
        binding.btnSetPhoto.setOnClickListener {
            pickPhotoLauncher.launch("image/*")
        }
        binding.avatarTouchTarget.setOnClickListener {
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
                putExtra("current_name", binding.tvName.text.toString())
            }
            editInfoLauncher.launch(intent)
        }

        binding.btnSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
    }

    /**
     * Telegram ProfileActionsView.java:520-587 applies ButtonBounce to each
     * active action, not to the entire scrolling container.
     */
    override fun onApplyThemePalette(palette: TelegramThemeEngine.ResolvedPalette) {
        super.onApplyThemePalette(palette)
        if (::binding.isInitialized) {
            // Stable Telegram icon variants: outline on light button surfaces,
            // filled on dark; photo-viewer expansion crossfade remains unported.
            ProfileActionIcons.applyTo(
                listOf(binding.btnSetPhoto, binding.btnEditInfo, binding.btnSettings),
                surfaceColor = androidx.core.content.ContextCompat.getColor(
                    this, R.color.category_pill_unselected_bg
                )
            )
        }
    }

    private fun setupProfileActionBounce() {
        ProfileActionBounce.install(binding.btnSetPhoto)
        ProfileActionBounce.install(binding.btnEditInfo)
        ProfileActionBounce.install(binding.btnSettings)
    }

    private fun setupProfileActionAccessibility() {
        ProfileActionAccessibility.install(
            listOf(binding.btnSetPhoto, binding.btnEditInfo, binding.btnSettings)
        )
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

        // Optional Bio uses server capability detection, not optimistic local-only state.
        binding.cellBio.setOnClickListener {
            val bio = currentBio ?: return@setOnClickListener
            editBioLauncher.launch(Intent(this, ChangeBioActivity::class.java).apply {
                putExtra("current_bio", bio)
            })
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

    private var categorySaveInFlight = false

    private fun selectCategory(cat: String) {
        if (categorySaveInFlight) return
        // Close the radio list without claiming a persisted selection prematurely.
        binding.layoutCategoryOptions.visibility = View.GONE
        binding.ivCategoryArrow.animate().rotation(0f).setDuration(150).start()
        if (cat.equals(currentCategory, ignoreCase = true)) return

        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        categorySaveInFlight = true
        binding.cellCategory.isEnabled = false
        lifecycleScope.launch {
            try {
                val res = ApiClient.apiService.updateProfile(mapOf("category" to cat))
                if (res.success) {
                    val confirmed = ApiClient.apiService.getProfile()
                    if (ProfileSaveConfirmation.matches(
                            userId, FirebaseAuth.getInstance().currentUser?.uid,
                            confirmed.success, cat, confirmed.data?.category,
                            ignoreCase = true
                        )) {
                        currentCategory = cat
                        updateCategoryRadios(cat)
                        binding.tvCategoryValue.text = cat
                        AppBulletin.showSuccess(this@ProfileActivity, "Category updated to $cat")
                    } else {
                        AppBulletin.showError(this@ProfileActivity,
                            "Category update could not be verified. Refresh your profile before retrying.")
                    }
                } else {
                    AppBulletin.showError(this@ProfileActivity, res.error ?: "Failed to update category")
                }
            } catch (e: Exception) {
                AppBulletin.showError(this@ProfileActivity, e.localizedMessage ?: "Failed to update category")
            } finally {
                // Previous value remains selected on any error or stale server response.
                updateCategoryRadios(currentCategory)
                binding.cellCategory.isEnabled = true
                categorySaveInFlight = false
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

    private var birthdaySaveInFlight = false

    private fun saveBirthday(dob: String) {
        if (birthdaySaveInFlight || dob == currentDob) return
        if (!ProfileBirthday.isValid(dob)) {
            AppBulletin.showError(this, "Please select a valid date of birth")
            return
        }
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        birthdaySaveInFlight = true
        binding.cellBirthday.isEnabled = false
        lifecycleScope.launch {
            try {
                val response = ApiClient.apiService.updateProfile(mapOf("dob" to dob))
                if (response.success) {
                    val confirmed = ApiClient.apiService.getProfile()
                    if (confirmed.success && confirmed.data?.dob == dob &&
                        FirebaseAuth.getInstance().currentUser?.uid == userId) {
                        currentDob = dob
                        binding.tvBirthdayValue.text = formatTelegramBirthday(dob)
                        AppBulletin.showSuccess(this@ProfileActivity, "Birthday updated")
                    } else {
                        AppBulletin.showError(this@ProfileActivity,
                            "Birthday update could not be verified. Refresh your profile before retrying.")
                    }
                } else {
                    AppBulletin.showError(this@ProfileActivity,
                        response.error ?: "Failed to update birthday")
                }
            } catch (e: Exception) {
                AppBulletin.showError(this@ProfileActivity,
                    e.localizedMessage ?: "Failed to update birthday")
            } finally {
                binding.cellBirthday.isEnabled = true
                birthdaySaveInFlight = false
            }
        }
    }

    private fun formatTelegramBirthday(dobString: String): String =
        ProfileBirthday.format(dobString)

    private fun setupAdminAndPremium(email: String?) {
        val setupAdminUi = {
            // Admin is a single role badge; the profile status line is not a role badge.
            // Real EVE presence is not yet server-authoritative: never fabricate "online".
            binding.chipAdmin.visibility = View.VISIBLE
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
        currentBio = null
        ProfileBioCapability.apply(binding.cellBio, binding.tvBioValue, null)

        lifecycleScope.launch {
            try {
                val res = ApiClient.apiService.getProfile()
                if (res.success && res.data != null) {
                    val profile = res.data

                    // Eve ID
                    val eveId = profile.eveId ?: "--"
                    binding.tvEveId.text = eveId

                    // Only a server-authenticated foreground lease can establish online status.
                    binding.tvStatus.text = ProfilePresenceStatus.label(profile.isOnline, eveId)

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

                    currentBio = profile.bio
                    ProfileBioCapability.apply(binding.cellBio, binding.tvBioValue, profile.bio)

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
