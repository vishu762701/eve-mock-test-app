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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import coil.load
import com.eve.app.R
import com.eve.app.data.model.PremiumStatusDto
import com.eve.app.data.remote.ApiClient
import com.eve.app.data.remote.UserProfileResponse
import com.eve.app.data.remote.ProfileRequestOwner
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
import com.eve.app.util.hasFreshWorkerAdminHint
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
    private var photoSaveJob: Job? = null

    private var profileOwnerUid: String? = null
    private var profileConfirmed = false
    private var currentDisplayName = ""
    private var birthdayDialog: TelegramNativeAlertDialog? = null
    private var profileLoadJob: Job? = null
    private var presenceSequence = 0L
    private var expansionFraction = 0f
    private var qrNavigationPending = false
    private var profileOptionsPopup: ProfileOverflowPopup? = null
    private var profileAuthListener: FirebaseAuth.AuthStateListener? = null
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
                profileOwnerUid?.let { ProfileSession.store.invalidate(it) }
                profileLoadJob?.cancel()
                loadProfile()
            }
        }

    private val editUsernameLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                profileOwnerUid?.let { ProfileSession.store.invalidate(it) }
                profileLoadJob?.cancel()
                loadProfile()
            }
        }

    private val editPhoneLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                profileOwnerUid?.let { ProfileSession.store.invalidate(it) }
                profileLoadJob?.cancel()
                loadProfile()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.glassToolbar.bringToFront()
        setupAvatarWindowHost()

        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            finish()
            return
        }

        profileOwnerUid = user.uid
        profileAuthListener = FirebaseAuth.AuthStateListener { auth ->
            if (auth.currentUser?.uid != profileOwnerUid) {
                presenceSequence++
                binding.tvStatus.text = ProfilePresenceStatus.label(null)
                finish()
            }
        }.also { FirebaseAuth.getInstance().addAuthStateListener(it) }
        binding.tvStatus.text = ProfilePresenceStatus.label(null)
        binding.btnBack.setOnClickListener { showPersonalQr() }
        binding.btnProfileMenu.setOnClickListener { showProfileOptions() }
        setPersonalEditingEnabled(false)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (pendingPhotoUri != null) {
                    cancelPhotoPreview()
                } else if (binding.profileScrollView.isExpanded || expansionFraction > 0) {
                    binding.profileScrollView.setExpanded(false)
                } else {
                    finish()
                }
            }
        })

        setupScrollAndGooey()
        if (savedInstanceState == null) {
            ProfileSession.store.position(user.uid)?.let { position ->
                binding.profileScrollView.doOnLayout {
                    binding.profileScrollView.scrollTo(0, position.scrollY)
                    binding.profileScrollView.setExpanded(position.expanded, animated = false)
                }
            }
        }
        setupActions()
        setupProfileActionBounce()
        setupProfileActionAccessibility()
        setupCells()
        setupAdminAndPremium(user.email)

        loadProfilePhoto()
        ProfileSession.store.peek(user.uid)?.let { renderConfirmedProfile(it) }
        loadProfile()
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                while (isActive) {
                    // Share the initial/revalidation request with profile rendering.
                    refreshPresence()
                    delay(30_000L)
                }
            }
        }
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
        binding.profileScrollView.maximumPull = (binding.root.width - binding.glassToolbar.height -
            152f * resources.displayMetrics.density).coerceAtLeast(0f)
        ProfileHeaderExpansion.apply(binding, expansionFraction, statusTop)
        if (expansionFraction > 0) {
            binding.avatarGooey.translationY = -statusTop.toFloat()
            return
        }
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
        binding.profileScrollView.canExpand = {
            pendingPhotoUri == null && hasProfilePhoto() &&
                resources.configuration.smallestScreenWidthDp < 600 &&
                resources.configuration.orientation != android.content.res.Configuration.ORIENTATION_LANDSCAPE
        }
        binding.profileScrollView.onExpansionChanged = { fraction ->
            expansionFraction = fraction
            // Expanded photo mode must restore the window renderer even after
            // a prior fully-collapsed camera endpoint.
            if (fraction > 0f) ProfileScrollState.applyGooeyVisibility(binding.avatarGooey, 0f)
            positionFullWindowAvatarHost()
            if (fraction == 0f) renderProfileScrollState(binding.profileScrollView.scrollY, density)
        }
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
        if (expansionFraction > 0) return
        val progress = ProfileScrollState.progress(scrollY, density)
        // Gooey avatar update (ProfileActivity.java:6039-6047)
        binding.avatarGooey.setPullProgress(progress)
        val blur = ((progress.coerceIn(0.2f, 0.7f) - 0.2f) / 0.5f).coerceAtMost(0.75f)
        binding.avatarGooey.setBlurIntensity(blur)
        binding.avatarGooey.setGooeyEnabled(progress > 0f && progress < 1f)
        // Telegram ProfileActivity.java:6053 hides the entire renderer at
        // its fully absorbed camera endpoint, then restores it on reverse scroll.
        ProfileScrollState.applyGooeyVisibility(binding.avatarGooey, progress)

        // Avatar translation towards notch & scale (ProfileActivity.java:8730-8740)
        val info = binding.avatarGooey.notchInfo
        val currentScale = ProfileAvatarTrajectory.sourceAvatarScale(
            progress,
            info?.takeIf { it.isLikelyCircle }?.bounds?.width(),
            density
        )
        binding.avatarContainer.scaleX = currentScale
        binding.avatarContainer.scaleY = currentScale
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
            if (resources.configuration.fontScale > 1.4f)
                ProfileActionsCollapse.atHeight(scrollY.toFloat(), density, binding.layoutActions.layoutParams.height.toFloat())
            else ProfileActionsCollapse.atScroll(scrollY.toFloat(), density)
        )
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            if (progress > 0f && progress < 1f) {
                val blurNode = binding.avatarGooey.getBlurRenderNode()
                binding.layoutActions.setDrawingBlur(
                    blurNode,
                    binding.ivProfilePhoto,
                    currentScale,
                    binding.avatarContainer.translationY
                )
            } else {
                binding.layoutActions.setDrawingBlur(false)
            }
        }
    }

    private fun setupActions() {
        binding.btnSetPhoto.setOnClickListener {
            if (!isProfileOwner()) return@setOnClickListener
            binding.profileScrollView.setExpanded(false, animated = false)
            pickPhotoLauncher.launch("image/*")
        }
        binding.avatarTouchTarget.setOnClickListener {
            if (hasProfilePhoto() && binding.profileScrollView.canExpand()) {
                binding.profileScrollView.setExpanded(!binding.profileScrollView.isExpanded)
            } else pickPhotoLauncher.launch("image/*")
        }
        binding.btnCancelPhoto.setOnClickListener {
            cancelPhotoPreview()
        }
        binding.btnSavePhoto.setOnClickListener {
            savePendingPhoto()
        }

        binding.btnEditInfo.setOnClickListener {
            if (!isProfileOwner()) return@setOnClickListener
            if (!profileConfirmed) { loadProfile(); return@setOnClickListener }
            val user = FirebaseAuth.getInstance().currentUser
            val intent = Intent(this, ChangeNameActivity::class.java).apply {
                putExtra(ProfileEditorAccountGuard.EXTRA_OWNER_UID, profileOwnerUid)
                putExtra("current_name", currentDisplayName)
            }
            editInfoLauncher.launch(intent)
        }

        binding.btnSettings.setOnClickListener {
            if (!isProfileOwner()) return@setOnClickListener
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
            profileOptionsPopup?.dismiss(immediate = true)
            // Stable Telegram icon variants: outline on light button surfaces,
            // filled on dark; photo-viewer expansion crossfade remains unported.
            val surface = androidx.core.content.ContextCompat.getColor(
                this, R.color.category_pill_unselected_bg
            )
            binding.layoutActions.setActionColor(surface)
            ProfileActionIcons.applyTo(
                listOf(binding.btnSetPhoto, binding.btnEditInfo, binding.btnSettings),
                surfaceColor = surface
            )
            if (expansionFraction > 0) positionFullWindowAvatarHost()
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
            if (!isProfileOwner()) return@setOnClickListener
            val intent = Intent(this, ChangePhoneActivity::class.java).apply {
                putExtra(ProfileEditorAccountGuard.EXTRA_OWNER_UID, profileOwnerUid)
                putExtra("current_phone", currentPhone)
            }
            editPhoneLauncher.launch(intent)
        }

        // Username cell
        binding.cellUsername.setOnClickListener {
            if (!isProfileOwner()) return@setOnClickListener
            val intent = Intent(this, ChangeUsernameActivity::class.java).apply {
                putExtra(ProfileEditorAccountGuard.EXTRA_OWNER_UID, profileOwnerUid)
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
        if (categorySaveInFlight || !profileConfirmed) return
        // Close the radio list without claiming a persisted selection prematurely.
        binding.layoutCategoryOptions.visibility = View.GONE
        binding.ivCategoryArrow.animate().rotation(0f).setDuration(150).start()
        if (cat.equals(currentCategory, ignoreCase = true)) return

        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        if (userId != profileOwnerUid) { finish(); return }
        categorySaveInFlight = true
        var saveVerified = false
        binding.cellCategory.isEnabled = false
        lifecycleScope.launch {
            try {
                val res = ApiClient.apiService.updateProfile(mapOf("category" to cat), ProfileRequestOwner(userId))
                if (res.success) {
                    val confirmed = ApiClient.apiService.getProfile(ProfileRequestOwner(userId))
                    if (confirmed.data?.id == userId && ProfileSaveConfirmation.matches(
                            userId, FirebaseAuth.getInstance().currentUser?.uid,
                            confirmed.success, cat, confirmed.data?.category,
                            ignoreCase = true
                        )) {
                        ProfileSession.store.accept(userId, confirmed.data!!)
                        saveVerified = true
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
            } catch (cancel: CancellationException) {
                throw cancel
            } catch (e: Exception) {
                AppBulletin.showError(this@ProfileActivity, e.localizedMessage ?: "Failed to update category")
            } finally {
                if (!saveVerified) {
                    ProfileSession.store.invalidate(userId)
                    profileConfirmed = false; setPersonalEditingEnabled(false)
                }
                // Previous value remains selected on any error or stale server response.
                updateCategoryRadios(currentCategory)
                binding.cellCategory.isEnabled = profileConfirmed
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
        if (isFinishing || isDestroyed || birthdayDialog?.isShowing == true) return
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

        val dialog = TelegramNativeAlertDialog.Builder(context)
            .setTitle("Date of Birth")
            .setView(layout)
            .setPositiveButton("Set") { _, _ ->
                val formatted = String.format(Locale.US, "%02d/%02d/%04d", dayPicker.value, monthPicker.value + 1, yearPicker.value)
                saveBirthday(formatted)
            }
            .setNegativeButton("Cancel", null)
            .show()
        birthdayDialog = dialog
        dialog.setOnDismissListener { if (birthdayDialog === dialog) birthdayDialog = null }
    }

    private var birthdaySaveInFlight = false

    private fun saveBirthday(dob: String) {
        if (birthdaySaveInFlight || !profileConfirmed || dob == currentDob) return
        if (!ProfileBirthday.isValid(dob)) {
            AppBulletin.showError(this, "Please select a valid date of birth")
            return
        }
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        if (userId != profileOwnerUid) { finish(); return }
        birthdaySaveInFlight = true
        var saveVerified = false
        binding.cellBirthday.isEnabled = false
        lifecycleScope.launch {
            try {
                val response = ApiClient.apiService.updateProfile(mapOf("dob" to dob), ProfileRequestOwner(userId))
                if (response.success) {
                    val confirmed = ApiClient.apiService.getProfile(ProfileRequestOwner(userId))
                    if (confirmed.success && confirmed.data?.id == userId && confirmed.data.dob == dob &&
                        FirebaseAuth.getInstance().currentUser?.uid == userId) {
                        ProfileSession.store.accept(userId, confirmed.data)
                        saveVerified = true
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
            } catch (cancel: CancellationException) {
                throw cancel
            } catch (e: Exception) {
                AppBulletin.showError(this@ProfileActivity,
                    e.localizedMessage ?: "Failed to update birthday")
            } finally {
                if (!saveVerified) {
                    ProfileSession.store.invalidate(userId)
                    profileConfirmed = false; setPersonalEditingEnabled(false)
                }
                binding.cellBirthday.isEnabled = profileConfirmed
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

        val cachedAdmin = hasFreshWorkerAdminHint(email)
        if (cachedAdmin) {
            setupAdminUi()
        }
        lifecycleScope.launch {
            val confirmedAdmin = adminRepo.isAdmin(email)
            if (!isProfileOwner()) return@launch
            if (confirmedAdmin) setupAdminUi() else binding.chipAdmin.visibility = View.GONE
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
            if (!isProfileOwner()) return@launch
            if (res.isSuccess) {
                updatePremiumBadge(res.getOrNull())
            }
        }
    }

    private fun loadProfile() {
        val user = FirebaseAuth.getInstance().currentUser ?: run { finish(); return }
        if (user.uid != profileOwnerUid) { finish(); return }
        if (profileLoadJob?.isActive == true) return
        val requestSequence = ++presenceSequence
        val currentName = user.displayName?.takeIf { it.isNotBlank() } ?: "Student"
        if (binding.tvName.text.isNullOrBlank()) {
            binding.tvName.text = currentName
            binding.tvToolbarTitle.text = currentName
        }
        // Preserve confirmed rows, editing and image/scroll state during revalidation.
        profileLoadJob = lifecycleScope.launch {
            try {
                val profile = ProfileSession.store.get(user.uid)
                if (FirebaseAuth.getInstance().currentUser?.uid != user.uid) return@launch
                renderConfirmedProfile(profile)
                if (requestSequence == presenceSequence) binding.tvStatus.text = ProfilePresenceStatus.label(profile.isOnline)
            } catch (cancel: CancellationException) {
                throw cancel
            } catch (e: Exception) {
                if (requestSequence == presenceSequence) binding.tvStatus.text = ProfilePresenceStatus.label(null)
                AppBulletin.showError(this@ProfileActivity, if (profileConfirmed)
                    "Could not check for profile updates. Showing the last confirmed information."
                    else "Profile unavailable. Check your connection and reopen your profile.")
            }
        }
    }

    private fun renderConfirmedProfile(profile: UserProfileResponse) {
        if (profile.id != profileOwnerUid || !isProfileOwner()) return
        currentDisplayName = profile.displayName.orEmpty()
        val name = currentDisplayName.ifBlank { "Student" }
        binding.tvName.text = name
        binding.tvToolbarTitle.text = name
        currentPhone = profile.phone.orEmpty()
        binding.tvMobileValue.text = currentPhone.ifBlank { "Add phone number" }
        currentUsername = profile.username.orEmpty()
        binding.tvUsernameValue.text = if (currentUsername.isBlank()) "Set username" else "@${currentUsername.removePrefix("@")}"
        if (!birthdaySaveInFlight) {
            currentDob = profile.dob
            binding.tvBirthdayValue.text = formatTelegramBirthday(currentDob)
        }
        if (!categorySaveInFlight) {
            currentCategory = profile.category.orEmpty()
            binding.tvCategoryValue.text = currentCategory.ifBlank { "Select category" }
            updateCategoryRadios(currentCategory)
        }
        profileConfirmed = true
        setPersonalEditingEnabled(true)
    }

    private fun isProfileOwner(): Boolean {
        if (FirebaseAuth.getInstance().currentUser?.uid == profileOwnerUid && profileOwnerUid != null) return true
        finish()
        return false
    }

    private fun hasProfilePhoto(): Boolean = ProfilePhotoManager.hasCustomPhoto(this, profileOwnerUid) ||
        !FirebaseAuth.getInstance().currentUser?.photoUrl?.toString().isNullOrBlank()

    private fun setPersonalEditingEnabled(enabled: Boolean) {
        for (view in listOf(binding.cellMobile, binding.cellUsername)) view.isEnabled = enabled
        binding.cellBirthday.isEnabled = enabled && !birthdaySaveInFlight
        binding.cellCategory.isEnabled = enabled && !categorySaveInFlight
    }

    private fun showPersonalQr() {
        if (FirebaseAuth.getInstance().currentUser?.uid != profileOwnerUid) { finish(); return }
        if (!profileConfirmed) { loadProfile(); return }
        if (qrNavigationPending) return
        val payload = ProfileQrPayload.encode(currentUsername)
        if (payload == null) { binding.cellUsername.performClick(); return }
        qrNavigationPending = true
        startActivity(Intent(this, UsernameCardActivity::class.java).setData(Uri.parse(payload)))
    }

    private fun showProfileOptions() {
        if (!isProfileOwner() || isFinishing || isDestroyed) return
        if (profileOptionsPopup?.isShowing == true) { profileOptionsPopup?.dismiss(); return }
        profileOptionsPopup = ProfileOverflowPopup(binding.btnProfileMenu).also { popup ->
            popup.show(profileConfirmed && ProfileQrPayload.normalize(currentUsername) != null,
                onUsername = { binding.cellUsername.performClick() },
                onCopyLink = {
                    val payload = ProfileQrPayload.encode(currentUsername) ?: return@show
                    (getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager)
                        .setPrimaryClip(ClipData.newPlainText("Profile link", payload))
                    AppBulletin.showSuccess(this, "Profile link copied")
                }, canEdit = profileConfirmed)
        }
    }

    private suspend fun refreshPresence() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null || uid != profileOwnerUid) { finish(); return }
        val sequence = ++presenceSequence
        try {
            val profile = ProfileSession.store.get(uid)
            if (sequence == presenceSequence && FirebaseAuth.getInstance().currentUser?.uid == uid) {
                renderConfirmedProfile(profile)
                val value = profile.isOnline
                binding.tvStatus.text = ProfilePresenceStatus.label(value)
            }
        } catch (cancel: CancellationException) { throw cancel }
        catch (_: Exception) {
            if (sequence == presenceSequence) binding.tvStatus.text = ProfilePresenceStatus.label(null)
        }
    }

    override fun onResume() {
        super.onResume()
        qrNavigationPending = false
        if (profileOwnerUid != null && FirebaseAuth.getInstance().currentUser?.uid != profileOwnerUid) { finish(); return }
    }

    override fun onPause() {
        profileOptionsPopup?.dismiss(immediate = true)
        if (::binding.isInitialized) profileOwnerUid?.let { uid ->
            ProfileSession.store.rememberPosition(uid, ConfirmedProfileStore.Position(
                binding.profileScrollView.scrollY, binding.profileScrollView.isExpanded))
        }
        presenceSequence++
        if (::binding.isInitialized) binding.tvStatus.text = ProfilePresenceStatus.label(null)
        super.onPause()
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
        photoSaveJob?.cancel()
        photoSaveJob = null
        binding.layoutActions.stopLoading(binding.btnSetPhoto)
        pendingPhotoUri = null
        binding.layoutPhotoActions.visibility = View.GONE
        loadProfilePhoto()
    }

    private fun savePendingPhoto() {
        if (!isProfileOwner()) return
        val uri = pendingPhotoUri ?: return
        val ownerUid = profileOwnerUid ?: return
        photoSaveJob?.cancel()
        binding.layoutActions.startLoading(binding.btnSetPhoto)
        photoSaveJob = lifecycleScope.launch(Dispatchers.IO) {
            val ok = try {
                ProfilePhotoManager.savePhoto(this@ProfileActivity, uri, ownerUid)
            } catch (e: Exception) {
                false
            }
            withContext(Dispatchers.Main) {
                binding.layoutActions.stopLoading(binding.btnSetPhoto)
                if (!isProfileOwner() || profileOwnerUid != ownerUid) return@withContext
                if (ok) {
                    AppBulletin.showSuccess(this@ProfileActivity, "Profile photo updated")
                } else {
                    AppBulletin.showError(this@ProfileActivity, "Failed to save photo")
                }
                pendingPhotoUri = null
                binding.layoutPhotoActions.visibility = View.GONE
                loadProfilePhoto()
            }
        }
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
        photoSaveJob?.cancel()
        photoSaveJob = null
        binding.layoutActions.stopLoading(binding.btnSetPhoto)
        binding.layoutActions.setDrawingBlur(false)
        birthdayDialog?.dismiss()
        birthdayDialog = null
        profileAuthListener?.let { FirebaseAuth.getInstance().removeAuthStateListener(it) }
        profileAuthListener = null
        profileOptionsPopup?.dismiss(immediate = true)
        profileOptionsPopup = null
        progressDialog?.dismiss()
        progressDialog = null
        super.onDestroy()
    }
}
