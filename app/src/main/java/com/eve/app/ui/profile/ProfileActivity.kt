/*
 * Derived from Telegram Android ProfileActivity.java, Copyright Nikolai Kudashov.
 * GNU GPL v2 or later. See THIRD_PARTY_NOTICES.md.
 */

package com.eve.app.ui.profile

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.NumberPicker
import android.widget.TextView
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.content.IntentCompat
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
import org.telegram.ui.ActionBar.ActionBarMenuSubItem
import org.telegram.ui.ActionBar.ActionBarPopupWindow
import org.telegram.ui.ActionBar.AlertDialog
import org.telegram.ui.ActionBar.Theme
import org.telegram.ui.Components.RadialProgressView
import com.eve.app.theme.TelegramThemeEngine
import com.eve.app.designsystem.TelegramRadialProgressView
import org.telegram.ui.ActionBar.BackDrawable
import com.eve.app.util.VibrationHelper
import org.telegram.ui.Components.CubicBezierInterpolator
import com.eve.app.ui.common.EveBaseActivity
import com.eve.app.ui.settings.SettingsActivity
import com.eve.app.util.AppBulletin
import com.eve.app.util.EveNavigationHelper
import com.eve.app.util.ProfilePhotoManager
import com.eve.app.util.ReminderScheduler
import com.eve.app.util.SessionManager
import com.eve.app.util.hasFreshWorkerAdminHint
import com.google.firebase.auth.FirebaseAuth
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

private const val STATE_CAMERA_URI = "profile_photo_camera_uri"
private const val STATE_CAMERA_FILE = "profile_photo_camera_file"
private const val STATE_PENDING_PHOTO_URI = "profile_photo_pending_uri"
private const val STATE_PENDING_PHOTO_CROPPED = "profile_photo_pending_cropped"
private const val STATE_CROP_FILE = "profile_photo_crop_file"

class ProfileActivity : EveBaseActivity() {

    private lateinit var binding: ProfileTelegramViewTree
    private val backDrawable = BackDrawable(false)
    private val adminRepo = AdminRepository()
    private var progressDialog: AlertDialog? = null
    private var pendingPhotoUri: Uri? = null
    private var pendingPhotoCropped = false
    private var croppedPhotoFile: File? = null
    private var photoSaveJob: Job? = null
    private var cameraPhotoUri: Uri? = null
    private var cameraPhotoFile: File? = null
    private var profilePhotoMenu: com.eve.app.designsystem.TelegramBottomSheet? = null

    private var profileOwnerUid: String? = null
    private var profileConfirmed = false
    private var currentDisplayName = ""
    private var birthdayDialog: android.app.Dialog? = null
    private var profileLoadJob: Job? = null
    private var presenceSequence = 0L
    private var expansionFraction = 0f
    private var wasAbsorbedIntoNotch = false
    private var qrNavigationPending = false
    private var profileOptionsPopup: ProfileOverflowPopup? = null
    private var profileAuthListener: FirebaseAuth.AuthStateListener? = null
    private var currentDob: String = ""
    private var currentCategory: String = "General"
    private var currentPhone: String = ""
    private var currentUsername: String = ""

    private val pickPhotoLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) launchAvatarCrop(uri)
        }

    // ImageUpdater.java:650-718: the camera action writes a temporary image and
    // reuses the same preview/crop pipeline as the gallery path.
    private val takePhotoLauncher =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { saved ->
            if (saved && cameraPhotoUri != null) {
                launchAvatarCrop(cameraPhotoUri!!)
            } else {
                clearCameraTemp()
            }
        }

    // PhotoCropActivity.java:757-775: the selected/captured image opens the
    // source square crop before ImageUpdater's 800px avatar export.
    private val cropPhotoLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            clearCameraTemp()
            if (result.resultCode != Activity.RESULT_OK) return@registerForActivityResult
            val data = result.data ?: return@registerForActivityResult
            val uri = IntentCompat.getParcelableExtra(
                data,
                PhotoCropActivity.EXTRA_OUTPUT_URI,
                Uri::class.java
            ) ?: data.data ?: return@registerForActivityResult
            val path = data.getStringExtra(PhotoCropActivity.EXTRA_OUTPUT_PATH)
            discardCroppedPhoto()
            croppedPhotoFile = path?.let(::File)
            previewSelectedPhoto(uri, cropped = true)
        }

    private val cameraPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                launchCameraCapture()
            } else {
                AppBulletin.showError(this, getString(R.string.profile_camera_permission_denied))
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
        binding = ProfileTelegramViewTree(this)
        setContentView(binding.root)
        binding.glassToolbar.bringToFront()
        binding.btnBack.setImageDrawable(backDrawable)
        setupAvatarWindowHost()
        restoreCameraCaptureState(savedInstanceState)

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
        binding.tvStatus.text = ProfilePresenceStatus.label(true)
        binding.btnBack.setOnClickListener { finish() }
        binding.btnProfileQr.setOnClickListener { showPersonalQr() }
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
        restorePendingPhotoState(savedInstanceState)
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
        binding.avatarGooey.refreshNotchInfo(insets)
        val physicalTop = insets?.getInsets(
            WindowInsetsCompat.Type.statusBars() or WindowInsetsCompat.Type.displayCutout()
        )?.top ?: 0
        // SystemBarHelper may have padded android.R.id.content before dispatch
        // reaches this child. Prefer the authoritative larger inset.
        val statusTop = maxOf(physicalTop, contentRoot?.paddingTop ?: 0)
        val density = resources.displayMetrics.density
        val scrollChild = binding.profileScrollView.getChildAt(0)
        if (scrollChild != null && binding.profileScrollView.height > 0) {
            val requiredHeight = binding.profileScrollView.height + (260f * density).roundToInt()
            if (scrollChild.minimumHeight != requiredHeight) {
                scrollChild.minimumHeight = requiredHeight
            }
        }
        binding.profileScrollView.maximumPull = (binding.root.width - binding.glassToolbar.height -
            152f * density).coerceAtLeast(0f)
        ProfileHeaderExpansion.apply(binding, expansionFraction, statusTop)
        if (expansionFraction > 0) {
            binding.avatarGooey.translationY = -statusTop.toFloat()
            return
        }
        val image = binding.avatarContainer
        val params = image.layoutParams as FrameLayout.LayoutParams
        val desiredTop = (35f * resources.displayMetrics.density).roundToInt() + statusTop
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
        binding.profileScrollView.onOverPullWhenExpanded = {
            openFullPhotoViewer()
        }
        binding.profileScrollView.onExpansionChanged = { fraction ->
            expansionFraction = fraction
            // Expanded photo mode must restore the window renderer even after
            // a prior fully-collapsed camera endpoint.
            if (fraction > 0f) {
                ProfileScrollState.applyGooeyVisibility(binding.avatarGooey, 0f)
                binding.glassToolbar.setBackgroundColor(Color.TRANSPARENT)
            }
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
        if (progress >= 1f && !wasAbsorbedIntoNotch) {
            wasAbsorbedIntoNotch = true
            if (scrollY > 0) {
                try {
                    binding.root.performHapticFeedback(
                        android.view.HapticFeedbackConstants.KEYBOARD_TAP,
                        android.view.HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING
                    )
                } catch (_: Exception) {}
            }
        } else if (progress < 0.95f) {
            wasAbsorbedIntoNotch = false
        }

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
            val targetCenterY = binding.avatarGooey.notchCenterY(info)
            val initialCenterY = binding.avatarContainer.top +
                binding.avatarContainer.height / 2f
            ProfileAvatarTrajectory.offsetToNotch(initialCenterY, targetCenterY, progress)
        } else {
            val endOffset = binding.avatarGooey.getEndOffset(true, currentScale)
            -scrollY.toFloat() + endOffset * progress
        }

        // Official Telegram (VN20261117_173850.mp4 00:08-00:20): Name & Status stay horizontally centered as they rise into the sticky top bar
        val diff = 1f - progress
        val nameScale = 0.90f + 0.10f * diff
        binding.tvName.pivotX = binding.tvName.width / 2f
        binding.tvName.pivotY = binding.tvName.height / 2f
        binding.tvName.scaleX = nameScale
        binding.tvName.scaleY = nameScale
        binding.tvName.translationX = 0f
        binding.tvStatus.translationX = 0f
        binding.tvName.translationY = 0f
        binding.tvStatus.translationY = 0f

        val headerContentAlpha = (1f - progress).coerceIn(0f, 1f)
        val chipAlpha = (1f - progress * 1.8f).coerceIn(0f, 1f)
        binding.tvToolbarTitle.alpha = progress
        binding.tvToolbarSubtitle.alpha = progress
        binding.tvToolbarTitle.translationY = (1f - progress) * (18f * density)
        binding.tvToolbarSubtitle.translationY = (1f - progress) * (18f * density)
        binding.tvToolbarSubtitle.text = binding.tvStatus.text
        binding.tvToolbarSubtitle.setTextColor(binding.tvStatus.currentTextColor)
        binding.tvName.alpha = headerContentAlpha
        binding.tvStatus.alpha = headerContentAlpha
        binding.chipAdmin.alpha = chipAlpha
        binding.chipPremium.alpha = chipAlpha

        val palette = TelegramThemeEngine.resolveCurrentPalette(this)
        val toolbarAlpha = (((progress - 0.75f) / 0.25f).coerceIn(0f, 1f) * 242f).roundToInt()
        binding.glassToolbar.setBackgroundColor(
            if (toolbarAlpha > 0) androidx.core.graphics.ColorUtils.setAlphaComponent(palette.surface, toolbarAlpha)
            else Color.TRANSPARENT
        )

        // Telegram ProfileActionsView.java:247-337 collapse over scroll
        val actionsTotalHeight = binding.layoutActions.layoutParams.height.toFloat().coerceAtLeast(74f * density)
        val actionsCollapseState = if (resources.configuration.fontScale > 1.4f)
            ProfileActionsCollapse.atHeight(scrollY.toFloat(), density, actionsTotalHeight)
        else ProfileActionsCollapse.atScroll(scrollY.toFloat(), density)
        ProfileActionsCollapse.applyTo(
            listOf(binding.btnSetPhoto, binding.btnEditInfo, binding.btnSettings),
            actionsCollapseState
        )
        val collapseShift = -(1f - actionsCollapseState.fraction) * (54f * density)
        binding.layoutActions.translationY = 0f
        binding.cardPersonalInfo.translationY = collapseShift
        binding.cardCategory.translationY = collapseShift
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
            showProfilePhotoMenu()
        }
        binding.avatarTouchTarget.setOnClickListener {
            if (hasProfilePhoto() && binding.profileScrollView.canExpand()) {
                if (binding.profileScrollView.isExpanded) {
                    if (!openFullPhotoViewer()) {
                        binding.profileScrollView.setExpanded(false)
                    }
                } else {
                    binding.profileScrollView.setExpanded(true)
                }
            } else showProfilePhotoMenu()
        }
        binding.avatarTouchTarget.setOnLongClickListener {
            openFullPhotoViewer()
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

    private fun openFullPhotoViewer(): Boolean {
        val customUri = ProfilePhotoManager.getCustomPhotoUri(this, profileOwnerUid)
        val rawGoogleUrl = FirebaseAuth.getInstance().currentUser?.photoUrl?.toString()
        val uri = customUri ?: rawGoogleUrl
            ?.replace(Regex("=s\\d+(-c)?$"), "=s800-c")
            ?.let(Uri::parse)
        return if (uri != null) {
            com.eve.app.designsystem.ProfileGalleryView.show(this, listOf(uri))
            true
        } else false
    }

    /**
     * Telegram ProfileActionsView.java:520-587 applies ButtonBounce to each
     * active action, not to the entire scrolling container.
     */
    override fun onApplyThemePalette(palette: TelegramThemeEngine.ResolvedPalette) {
        if (::binding.isInitialized) binding.applyTelegramTheme()
        super.onApplyThemePalette(palette)
        if (::binding.isInitialized) {
            profileOptionsPopup?.dismiss(immediate = true)
            profilePhotoMenu?.dismiss()
            // Telegram: ProfileActivity.java:1271-1280 @ 21f7d109da6ab3506c9e9577f98c6a38771ab335
            // Dynamic action button color calculation based on perceived brightness and HSV adaptation
            val baseColor = if (palette.isDark) 0xFF212D3B.toInt() else Color.WHITE
            val brightness = TelegramThemeEngine.computePerceivedBrightnessTg(baseColor)
            val btnColor = if (brightness > 0.8f) {
                Color.WHITE
            } else if (brightness < 0.2f) {
                TelegramThemeEngine.multAlpha(TelegramThemeEngine.adaptHSV(baseColor, +0.02f, +0.25f), 0.35f)
            } else {
                TelegramThemeEngine.multAlpha(TelegramThemeEngine.adaptProfileEmojiColor(baseColor), 0.15f)
            }
            val surface = if (btnColor != 0) btnColor else Theme.getColor(Theme.key_windowBackgroundGray)
            binding.layoutActions.setActionColor(surface)
            ProfileActionIcons.applyTo(
                listOf(binding.btnSetPhoto, binding.btnEditInfo, binding.btnSettings),
                surfaceColor = surface
            )
            val rowSelector = {
                com.eve.app.designsystem.TelegramThemeSelectors.getRoundRectSelectorDrawable(
                    12 * resources.displayMetrics.density,
                    palette.actionSelector
                )
            }
            backDrawable.setColor(palette.textPrimary)
            backDrawable.setRotatedColor(palette.textPrimary)
            binding.cardPersonalInfo.updatePalette(palette)
            binding.cardCategory.updatePalette(palette)
            val sectionColor = Theme.getColor(Theme.key_windowBackgroundWhiteBlueHeader)
            binding.tvAccountHeader.setTextColor(sectionColor)
            binding.tvStudyHeader.setTextColor(sectionColor)
            binding.dividerMobile.setBackgroundColor(palette.hairline)
            binding.dividerUsername.setBackgroundColor(palette.hairline)
            binding.dividerCategory.setBackgroundColor(palette.hairline)
            binding.radioGeneral.updatePalette(palette)
            binding.radioObc.updatePalette(palette)
            binding.radioSc.updatePalette(palette)
            binding.radioSt.updatePalette(palette)
            binding.switchReminder.invalidate()
            binding.cellMobile.background = rowSelector()
            binding.cellUsername.background = rowSelector()
            binding.cellBirthday.background = rowSelector()
            binding.cellCategory.background = rowSelector()
            binding.cellReminder.background = rowSelector()
            ProfileHeaderExpansion.apply(binding, expansionFraction, 0)
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

    private fun copyToClipboard(label: String, text: String, successMessage: String) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard?.setPrimaryClip(clip)
        AppBulletin.showSuccess(this, successMessage)
    }

    private fun launchPhoneEditor() {
        if (!isProfileOwner()) return
        val intent = Intent(this, ChangePhoneActivity::class.java).apply {
            putExtra(ProfileEditorAccountGuard.EXTRA_OWNER_UID, profileOwnerUid)
            putExtra("current_phone", currentPhone)
        }
        editPhoneLauncher.launch(intent)
    }

    private fun launchUsernameEditor() {
        if (!isProfileOwner()) return
        val intent = Intent(this, ChangeUsernameActivity::class.java).apply {
            putExtra(ProfileEditorAccountGuard.EXTRA_OWNER_UID, profileOwnerUid)
            putExtra("current_username", currentUsername)
        }
        editUsernameLauncher.launch(intent)
    }

    private fun showMobileContextMenu(): Boolean {
        if (!isProfileOwner()) return false
        val phone = currentPhone.trim()
        if (phone.isBlank()) {
            launchPhoneEditor()
            return true
        }
        showTelegramContextMenu(
            binding.cellMobile,
            listOf(
                Triple("Copy", R.drawable.ic_copy) { copyToClipboard("Phone", phone, "Phone copied to clipboard") },
                Triple("Replace number", R.drawable.telegram_profile_outline_edit) { launchPhoneEditor() }
            )
        )
        return true
    }

    private fun showUsernameContextMenu(): Boolean {
        if (!isProfileOwner()) return false
        val uname = currentUsername.trim().removePrefix("@")
        if (uname.isBlank()) {
            launchUsernameEditor()
            return true
        }
        showTelegramContextMenu(
            binding.cellUsername,
            listOf(
                Triple(getString(R.string.copy_username), R.drawable.ic_copy) { copyToClipboard("Username", "@$uname", "Username copied to clipboard") },
                Triple("Change username", R.drawable.telegram_profile_outline_edit) { launchUsernameEditor() }
            )
        )
        return true
    }

    private fun showBirthdayContextMenu(): Boolean {
        if (!isProfileOwner()) return false
        val dob = currentDob.trim()
        if (dob.isBlank()) {
            showBirthdayPicker(currentDob)
            return true
        }
        val formatted = binding.tvBirthdayValue.text.toString().ifBlank { formatTelegramBirthday(dob) }
        showTelegramContextMenu(
            binding.cellBirthday,
            listOf(
                Triple("Copy", R.drawable.ic_copy) { copyToClipboard("Birthday", formatted, getString(R.string.birthday_copied)) },
                Triple("Change", R.drawable.telegram_profile_outline_edit) { showBirthdayPicker(currentDob) },
                Triple("Remove", R.drawable.ic_delete) { removeBirthday() }
            ),
            destructiveLast = true
        )
        return true
    }

    private fun setupCells() {
        // Mobile cell (VN20261117_173850.mp4 00:34-00:36: tap/long-press opens Copy + Replace number)
        binding.cellMobile.setOnClickListener { showMobileContextMenu() }
        binding.cellMobile.setOnLongClickListener { showMobileContextMenu() }

        // Username cell (VN20261117_173850.mp4 00:37-00:39: tap/long-press opens Copy username + Change username)
        binding.cellUsername.setOnClickListener { showUsernameContextMenu() }
        binding.cellUsername.setOnLongClickListener { showUsernameContextMenu() }

        // Birthday cell (VN20261117_173850.mp4 00:40-00:50: tap/long-press opens Copy + Change + Remove)
        binding.cellBirthday.setOnClickListener { showBirthdayContextMenu() }
        binding.cellBirthday.setOnLongClickListener { showBirthdayContextMenu() }

        // Category cell & expansion
        binding.cellCategory.setOnClickListener {
            VibrationHelper.vibrateLightHaptic(this)
            val isExpanded = binding.layoutCategoryOptions.visibility == View.VISIBLE
            if (isExpanded) {
                binding.layoutCategoryOptions.visibility = View.GONE
                binding.ivCategoryArrow.animate().rotation(0f).setDuration(220L)
                    .setInterpolator(CubicBezierInterpolator.EASE_OUT_QUINT).start()
            } else {
                binding.layoutCategoryOptions.visibility = View.VISIBLE
                binding.ivCategoryArrow.animate().rotation(90f).setDuration(220L)
                    .setInterpolator(CubicBezierInterpolator.EASE_OUT_QUINT).start()
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
        binding.ivCategoryArrow.animate().rotation(0f).setDuration(220L).setInterpolator(CubicBezierInterpolator.EASE_OUT_QUINT).start()
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
        val palette = TelegramThemeEngine.resolveCurrentPalette(context)
        val density = resources.displayMetrics.density
        fun dp(v: Int): Int = (v * density + 0.5f).toInt()

        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(6), 0, dp(16))
        }

        val titleView = TextView(context).apply {
            text = "Birthday"
            setTextColor(palette.textPrimary)
            setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 20f)
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setPadding(dp(21), dp(8), dp(21), dp(4))
        }
        root.addView(titleView, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))

        val wheelRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            weightSum = 1.0f
            setPadding(dp(16), dp(4), dp(16), dp(4))
        }

        val dayPicker = NumberPicker(context).apply {
            minValue = 1
            maxValue = 31
            value = cal.get(Calendar.DAY_OF_MONTH)
        }

        val months = arrayOf(
            "January", "February", "March", "April", "May", "June",
            "July", "August", "September", "October", "November", "December"
        )
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

        wheelRow.addView(dayPicker, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 0.28f))
        wheelRow.addView(monthPicker, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 0.44f))
        wheelRow.addView(yearPicker, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 0.28f))
        root.addView(wheelRow, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))

        val hintView = TextView(context).apply {
            text = "Only your contacts can see your birthday."
            setTextColor(palette.textSecondary)
            setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 13f)
            gravity = android.view.Gravity.CENTER
            setPadding(dp(21), dp(8), dp(21), dp(14))
        }
        root.addView(hintView, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))

        var sheetRef: com.eve.app.designsystem.TelegramBottomSheet? = null
        val saveBtn = TextView(context).apply {
            text = "Save"
            setTextColor(Color.WHITE)
            setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 15f)
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            gravity = android.view.Gravity.CENTER
            isClickable = true
            isFocusable = true
            background = android.graphics.drawable.GradientDrawable().apply {
                shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                cornerRadius = dp(8).toFloat()
                setColor(palette.accent)
            }
            setOnClickListener {
                val formatted = String.format(Locale.US, "%02d/%02d/%04d", dayPicker.value, monthPicker.value + 1, yearPicker.value)
                sheetRef?.dismiss()
                saveBirthday(formatted)
            }
        }
        root.addView(
            saveBtn,
            LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(48)).apply {
                marginStart = dp(16)
                marginEnd = dp(16)
            }
        )

        val dialog = com.eve.app.designsystem.TelegramBottomSheet.Builder(context)
            .setCustomView(root)
            .create()
        sheetRef = dialog
        birthdayDialog = dialog
        dialog.setOnDismissListener { if (birthdayDialog === dialog) birthdayDialog = null }
        dialog.show()
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

    private fun removeBirthday() {
        if (birthdaySaveInFlight || !profileConfirmed || currentDob.isBlank()) return
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        if (userId != profileOwnerUid) { finish(); return }
        birthdaySaveInFlight = true
        binding.cellBirthday.isEnabled = false
        lifecycleScope.launch {
            try {
                val response = ApiClient.apiService.updateProfile(mapOf("dob" to ""), ProfileRequestOwner(userId))
                if (response.success) {
                    val confirmed = ApiClient.apiService.getProfile(ProfileRequestOwner(userId))
                    if (confirmed.success && confirmed.data?.id == userId) {
                        ProfileSession.store.accept(userId, confirmed.data)
                        currentDob = confirmed.data.dob
                        binding.tvBirthdayValue.text = formatTelegramBirthday(currentDob)
                        AppBulletin.showSuccess(this@ProfileActivity, "Birthday removed")
                    }
                }
            } catch (cancel: CancellationException) {
                throw cancel
            } catch (_: Exception) {
            } finally {
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
                if (requestSequence == presenceSequence) {
                    binding.tvStatus.text = ProfilePresenceStatus.label(profile.isOnline)
                    if (profile.isOnline == null && isProfileOwner()) binding.tvStatus.text = ProfilePresenceStatus.label(true)
                }
            } catch (cancel: CancellationException) {
                throw cancel
            } catch (e: Exception) {
                if (requestSequence == presenceSequence) binding.tvStatus.text = ProfilePresenceStatus.label(true)
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
        val normalized = ProfileQrPayload.normalize(currentUsername)
        val payload = if (normalized != null) {
            ProfileQrPayload.encode(normalized)
        } else {
            val uid = FirebaseAuth.getInstance().currentUser?.uid ?: "student"
            "eve-mock-test://user/$uid"
        }
        qrNavigationPending = true
        startActivity(Intent(this, QrShareActivity::class.java).setData(Uri.parse(payload)))
    }

    private fun showProfileOptions() {
        if (!isProfileOwner() || isFinishing || isDestroyed) return
        if (profileOptionsPopup?.isShowing == true) { profileOptionsPopup?.dismiss(); return }
        profileOptionsPopup = ProfileOverflowPopup(binding.btnProfileMenu).also { popup ->
            popup.show(
                usernameEnabled = profileConfirmed && ProfileQrPayload.normalize(currentUsername) != null,
                onUsername = { launchUsernameEditor() },
                onCopyLink = {
                    val payload = ProfileQrPayload.encode(currentUsername) ?: return@show
                    (getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager)
                        .setPrimaryClip(ClipData.newPlainText("Profile link", payload))
                    AppBulletin.showSuccess(this, "Profile link copied")
                },
                onShareQr = null,
                canEdit = profileConfirmed,
                onChangeColor = {
                    startActivity(Intent(this, com.eve.app.ui.settings.ThemeActivity::class.java))
                }
            )
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
                if (value == null && isProfileOwner()) binding.tvStatus.text = ProfilePresenceStatus.label(true)
            }
        } catch (cancel: CancellationException) { throw cancel }
        catch (_: Exception) {
            if (sequence == presenceSequence) binding.tvStatus.text = ProfilePresenceStatus.label(true)
        }
    }

    override fun onResume() {
        super.onResume()
        qrNavigationPending = false
        if (profileOwnerUid != null && FirebaseAuth.getInstance().currentUser?.uid != profileOwnerUid) { finish(); return }
        if (!::binding.isInitialized) return
    }

    override fun onPause() {
        profileOptionsPopup?.dismiss(immediate = true)
        profilePhotoMenu?.dismiss()
        if (::binding.isInitialized) profileOwnerUid?.let { uid ->
            ProfileSession.store.rememberPosition(uid, ConfirmedProfileStore.Position(
                binding.profileScrollView.scrollY, binding.profileScrollView.isExpanded))
        }
        presenceSequence++
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

    private fun previewSelectedPhoto(uri: Uri, cropped: Boolean = false) {
        pendingPhotoUri = uri
        pendingPhotoCropped = cropped
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

    private fun launchAvatarCrop(uri: Uri) {
        if (!isProfileOwner()) return
        if (pendingPhotoUri != null) return
        binding.profileScrollView.setExpanded(false, animated = false)
        try {
            cropPhotoLauncher.launch(PhotoCropActivity.intent(this, uri))
        } catch (_: ActivityNotFoundException) {
            clearCameraTemp()
            AppBulletin.showError(this, getString(R.string.profile_crop_load_failed))
        } catch (_: Exception) {
            clearCameraTemp()
            AppBulletin.showError(this, getString(R.string.profile_crop_load_failed))
        }
    }

    private fun cancelPhotoPreview() {
        photoSaveJob?.cancel()
        photoSaveJob = null
        binding.layoutActions.stopLoading(binding.btnSetPhoto)
        pendingPhotoUri = null
        pendingPhotoCropped = false
        discardCroppedPhoto()
        clearCameraTemp()
        binding.layoutPhotoActions.visibility = View.GONE
        loadProfilePhoto()
    }

    private fun savePendingPhoto() {
        if (!isProfileOwner()) return
        val uri = pendingPhotoUri ?: return
        val ownerUid = profileOwnerUid ?: return
        val cropped = pendingPhotoCropped
        photoSaveJob?.cancel()
        binding.layoutActions.startLoading(binding.btnSetPhoto)
        photoSaveJob = lifecycleScope.launch(Dispatchers.IO) {
            val ok = try {
                if (cropped) {
                    ProfilePhotoManager.saveCroppedPhoto(
                        this@ProfileActivity,
                        uri,
                        ownerUid
                    )
                } else {
                    ProfilePhotoManager.savePhoto(this@ProfileActivity, uri, ownerUid)
                }
            } catch (e: Exception) {
                false
            }
            withContext(Dispatchers.Main) {
                binding.layoutActions.stopLoading(binding.btnSetPhoto)
                if (!isProfileOwner() || profileOwnerUid != ownerUid) return@withContext
                if (ok) {
                    AppBulletin.showSuccess(
                        this@ProfileActivity,
                        getString(R.string.profile_photo_updated)
                    )
                } else {
                    AppBulletin.showError(
                        this@ProfileActivity,
                        getString(R.string.profile_photo_save_failed)
                    )
                }
                pendingPhotoUri = null
                pendingPhotoCropped = false
                discardCroppedPhoto()
                clearCameraTemp()
                binding.layoutPhotoActions.visibility = View.GONE
                loadProfilePhoto()
            }
        }
    }

    private fun discardCroppedPhoto() {
        croppedPhotoFile?.delete()
        croppedPhotoFile = null
    }

    private fun restorePendingPhotoState(state: Bundle?) {
        if (state == null) return
        val path = state.getString(STATE_CROP_FILE)
        val uri = state.getString(STATE_PENDING_PHOTO_URI)?.let(Uri::parse)
        val file = path?.let(::File)
        if (file != null && file.exists() && uri != null) {
            croppedPhotoFile = file
            previewSelectedPhoto(uri, state.getBoolean(STATE_PENDING_PHOTO_CROPPED))
        } else {
            file?.delete()
        }
    }

    /**
     * ImageUpdater.java:253-332 source menu. The subtitle/dialog rotation is
     * owned by the existing profile activity; only one sheet may be attached
     * to a window, so an already-visible sheet is replaced.
     */
    private fun showProfilePhotoMenu() {
        if (!isProfileOwner() || isFinishing || isDestroyed) return
        if (pendingPhotoUri != null) return
        profilePhotoMenu?.dismiss()
        val dialog = ProfilePhotoMenu.show(
            this,
            hasAvatar = ProfilePhotoManager.hasCustomPhoto(this, profileOwnerUid)
        ) { item ->
            if (isProfileOwner()) {
                when (item.action) {
                    ProfilePhotoMenuModel.Action.CAMERA -> requestCameraCapture()
                    ProfilePhotoMenuModel.Action.GALLERY -> {
                        binding.profileScrollView.setExpanded(false, animated = false)
                        pickPhotoLauncher.launch("image/*")
                    }
                    ProfilePhotoMenuModel.Action.REMOVE -> removeProfilePhoto()
                }
            }
        }
        profilePhotoMenu = dialog
        dialog.setOnDismissListener {
            if (profilePhotoMenu === dialog) profilePhotoMenu = null
        }
    }

    private fun requestCameraCapture() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            launchCameraCapture()
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun launchCameraCapture() {
        if (!isProfileOwner()) return
        clearCameraTemp()
        val file = try {
            File.createTempFile("profile_photo_", ".jpg", cacheDir)
        } catch (_: Exception) {
            null
        }
        if (file == null) {
            AppBulletin.showError(this, getString(R.string.profile_camera_unavailable))
            return
        }
        val uri = try {
            FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
        } catch (_: Exception) {
            null
        }
        if (uri == null) {
            file.delete()
            AppBulletin.showError(this, getString(R.string.profile_camera_unavailable))
            return
        }
        cameraPhotoFile = file
        cameraPhotoUri = uri
        try {
            takePhotoLauncher.launch(uri)
        } catch (_: ActivityNotFoundException) {
            clearCameraTemp()
            AppBulletin.showError(this, getString(R.string.profile_camera_unavailable))
        } catch (_: Exception) {
            clearCameraTemp()
            AppBulletin.showError(this, getString(R.string.profile_camera_unavailable))
        }
    }

    private fun removeProfilePhoto() {
        val uid = profileOwnerUid ?: return
        if (!isProfileOwner()) return
        if (!ProfilePhotoManager.hasCustomPhoto(this, uid)) {
            loadProfilePhoto()
            return
        }
        // Telegram removes the avatar directly from the source action menu
        // (ProfileActivity.java:6993-7000 -> ImageUpdater onDeleteAvatar).
        ProfilePhotoManager.removePhoto(this, uid)
        if (ProfilePhotoManager.hasCustomPhoto(this, uid)) {
            AppBulletin.showError(this, getString(R.string.profile_photo_remove_failed))
        } else {
            loadProfilePhoto()
            AppBulletin.showSuccess(this, getString(R.string.profile_photo_removed))
        }
    }

    private fun clearCameraTemp() {
        cameraPhotoUri = null
        cameraPhotoFile?.delete()
        cameraPhotoFile = null
    }

    private fun restoreCameraCaptureState(state: Bundle?) {
        if (state == null) return
        val filePath = state.getString(STATE_CAMERA_FILE)
        val uri = state.getString(STATE_CAMERA_URI)?.let(Uri::parse)
        val file = filePath?.let(::File)
        if (file != null && file.exists() && uri != null) {
            cameraPhotoFile = file
            cameraPhotoUri = uri
        } else {
            file?.delete()
            cameraPhotoUri = null
            cameraPhotoFile = null
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(STATE_CAMERA_URI, cameraPhotoUri?.toString())
        outState.putString(STATE_CAMERA_FILE, cameraPhotoFile?.absolutePath)
        outState.putString(STATE_PENDING_PHOTO_URI, pendingPhotoUri?.toString())
        outState.putBoolean(STATE_PENDING_PHOTO_CROPPED, pendingPhotoCropped)
        outState.putString(STATE_CROP_FILE, croppedPhotoFile?.absolutePath)
    }

    private fun showTelegramContextMenu(
        anchor: View,
        items: List<Triple<String, Int, () -> Unit>>,
        destructiveLast: Boolean = false
    ) {
        val layout = ActionBarPopupWindow.ActionBarPopupWindowLayout(this)
        layout.setFitItems(true)
        val popup = ActionBarPopupWindow(
            layout,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        )
        items.forEachIndexed { idx, (label, icon, action) ->
            val sub = ActionBarMenuSubItem(this, idx == 0, idx == items.lastIndex)
            sub.setTextAndIcon(label, icon)
            if (destructiveLast && idx == items.lastIndex) {
                val red = Theme.getColor(Theme.key_text_RedRegular)
                sub.setColors(red, red)
            }
            sub.setOnClickListener {
                popup.dismiss()
                action()
            }
            layout.addView(sub)
        }
        popup.isOutsideTouchable = true
        popup.isClippingEnabled = true
        popup.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
        popup.showAsDropDown(anchor)
        popup.startAnimation()
    }

    private fun showProgress(msg: String) {
        dismissProgress()
        val builder = AlertDialog.Builder(this)
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            val space = org.telegram.messenger.AndroidUtilities.dp(20f)
            setPadding(space, space, space, space)
            gravity = android.view.Gravity.CENTER_VERTICAL
            addView(RadialProgressView(this@ProfileActivity), LinearLayout.LayoutParams(
                org.telegram.messenger.AndroidUtilities.dp(32f),
                org.telegram.messenger.AndroidUtilities.dp(32f)))
            addView(TextView(this@ProfileActivity).apply {
                text = msg
                setPadding(org.telegram.messenger.AndroidUtilities.dp(16f), 0, 0, 0)
                setTextColor(Theme.getColor(Theme.key_dialogTextBlack))
                setTextSize(android.util.TypedValue.COMPLEX_UNIT_DIP, 16f)
            })
        }
        builder.setView(layout)
        progressDialog = builder.create().apply {
            setCancelable(false)
            show()
        }
    }

    private fun dismissProgress() {
        progressDialog?.dismiss()
        progressDialog = null
    }

    override fun onStop() {
        birthdayDialog?.dismiss()
        birthdayDialog = null
        profileOptionsPopup?.dismiss(immediate = true)
        profileOptionsPopup = null
        profilePhotoMenu?.dismiss()
        profilePhotoMenu = null
        progressDialog?.dismiss()
        progressDialog = null
        super.onStop()
    }

    override fun onDestroy() {
        photoSaveJob?.cancel()
        photoSaveJob = null
        // A camera app can trigger a configuration change while its result is
        // pending. Keep that temp file alive for the recreated instance, which
        // restores STATE_CAMERA_FILE/STATE_CAMERA_URI; delete it otherwise.
        if (!isChangingConfigurations) {
            clearCameraTemp()
            discardCroppedPhoto()
        }
        if (::binding.isInitialized) {
            binding.layoutActions.stopLoading(binding.btnSetPhoto)
            binding.layoutActions.setDrawingBlur(false)
        }
        birthdayDialog?.dismiss()
        birthdayDialog = null
        profileAuthListener?.let { FirebaseAuth.getInstance().removeAuthStateListener(it) }
        profileAuthListener = null
        profileOptionsPopup?.dismiss(immediate = true)
        profileOptionsPopup = null
        profilePhotoMenu?.dismiss()
        profilePhotoMenu = null
        progressDialog?.dismiss()
        progressDialog = null
        super.onDestroy()
    }
}
