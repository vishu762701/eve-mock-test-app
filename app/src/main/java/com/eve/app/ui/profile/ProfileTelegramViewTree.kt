package com.eve.app.ui.profile

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.text.TextUtils
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.eve.app.R
import com.eve.app.designsystem.TelegramActionBar
import com.eve.app.designsystem.TelegramAvatarImageView
import com.eve.app.designsystem.TelegramButton
import com.eve.app.designsystem.TelegramChip
import com.eve.app.designsystem.TelegramSwitch
import org.telegram.messenger.AndroidUtilities
import org.telegram.ui.ActionBar.Theme
import org.telegram.ui.Components.LayoutHelper

/**
 * Programmatic Telegram-native view hierarchy for ProfileActivity (Pass 3b).
 * Replaces activity_profile.xml inflation; every color, surface, divider, ripple,
 * and typeface comes from org.telegram.ui.ActionBar.Theme & AndroidUtilities.
 */
class ProfileTelegramViewTree(context: Context) {

    private fun dp(v: Float): Int = AndroidUtilities.dp(v)
    private fun mediumFont(): Typeface =
        AndroidUtilities.getTypeface("fonts/rmedium.ttf") ?: Typeface.DEFAULT_BOLD

    val root: FrameLayout = FrameLayout(context).apply {
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
    }

    val profileScrollView: ProfilePullScrollView = ProfilePullScrollView(context).apply {
        id = R.id.profileScrollView
        isFillViewport = true
        isVerticalScrollBarEnabled = false
    }

    val avatarOuterContainer: FrameLayout = FrameLayout(context).apply {
        id = R.id.avatarOuterContainer
        clipChildren = false
        clipToPadding = false
    }

    val avatarGooey: ProfileGooeyView = ProfileGooeyView(context).apply {
        id = R.id.avatarGooey
    }

    val avatarContainer: FrameLayout = FrameLayout(context).apply {
        id = R.id.avatarContainer
        clipChildren = false
        clipToPadding = false
    }

    val ivProfilePhoto: TelegramAvatarImageView = TelegramAvatarImageView(context).apply {
        id = R.id.ivProfilePhoto
        scaleType = ImageView.ScaleType.CENTER_CROP
    }

    val profilePhotoScrim: View = View(context).apply {
        id = R.id.profilePhotoScrim
        alpha = 0f
    }

    val avatarTouchTarget: View = View(context).apply {
        id = R.id.avatarTouchTarget
        isClickable = true
        isFocusable = true
        contentDescription = "Profile photo"
    }

    val tvName: TextView = TextView(context).apply {
        id = R.id.tvName
        setTextSize(TypedValue.COMPLEX_UNIT_DIP, 22f)
        typeface = mediumFont()
        gravity = Gravity.CENTER
        maxLines = 1
        ellipsize = TextUtils.TruncateAt.END
    }

    val tvStatus: TextView = TextView(context).apply {
        id = R.id.tvStatus
        setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14f)
        gravity = Gravity.CENTER
        maxLines = 1
        ellipsize = TextUtils.TruncateAt.END
    }

    val chipAdmin: TelegramChip = TelegramChip(context).apply {
        id = R.id.chipAdmin
        text = "Admin"
        visibility = View.GONE
    }

    val chipPremium: TelegramChip = TelegramChip(context).apply {
        id = R.id.chipPremium
        text = "Premium"
        visibility = View.GONE
    }

    val layoutPhotoActions: LinearLayout = LinearLayout(context).apply {
        id = R.id.layoutPhotoActions
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER
        visibility = View.GONE
        setPadding(dp(16f), dp(8f), dp(16f), dp(4f))
    }

    val btnCancelPhoto: TelegramButton = TelegramButton(context).apply {
        id = R.id.btnCancelPhoto
        text = "Cancel"
    }

    val btnSavePhoto: TelegramButton = TelegramButton(context).apply {
        id = R.id.btnSavePhoto
        text = "Save Photo"
    }

    val layoutActions: ProfileActionsCanvasLayout = ProfileActionsCanvasLayout(context).apply {
        id = R.id.layoutActions
    }

    val btnSetPhoto: LinearLayout = createActionPill(context, R.id.btnSetPhoto, R.drawable.telegram_profile_photo, "Set Photo")
    val btnEditInfo: LinearLayout = createActionPill(context, R.id.btnEditInfo, R.drawable.telegram_profile_edit, "Edit Info")
    val btnSettings: LinearLayout = createActionPill(context, R.id.btnSettings, R.drawable.telegram_profile_settings, "Settings")

    // --- Account / Personal info section ---
    val cardPersonalInfo: TelegramProfileSection = TelegramProfileSection(context).apply {
        id = R.id.cardPersonalInfo
        orientation = LinearLayout.VERTICAL
    }

    val tvAccountHeader: TextView = TextView(context).apply {
        id = R.id.tvAccountHeader
        text = "Account"
        setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14f)
        typeface = mediumFont()
        setPadding(dp(20f), dp(14f), dp(20f), dp(6f))
    }

    val cellMobile: LinearLayout
    val tvMobileValue: TextView = createCellValue(context, R.id.tvMobileValue)
    val dividerMobile: View = View(context).apply { id = R.id.dividerMobile }

    val cellUsername: LinearLayout
    val tvUsernameValue: TextView = createCellValue(context, R.id.tvUsernameValue)
    val dividerUsername: View = View(context).apply { id = R.id.dividerUsername }

    val cellBirthday: LinearLayout
    val tvBirthdayValue: TextView = createCellValue(context, R.id.tvBirthdayValue)

    // --- Study & Category section ---
    val cardCategory: TelegramProfileSection = TelegramProfileSection(context).apply {
        id = R.id.cardCategory
        orientation = LinearLayout.VERTICAL
    }

    val tvStudyHeader: TextView = TextView(context).apply {
        id = R.id.tvStudyHeader
        text = "Study Preferences"
        setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14f)
        typeface = mediumFont()
        setPadding(dp(20f), dp(14f), dp(20f), dp(6f))
    }

    val cellCategory: LinearLayout
    val tvCategoryValue: TextView = createCellValue(context, R.id.tvCategoryValue)
    val ivCategoryArrow: ImageView = ImageView(context).apply {
        id = R.id.ivCategoryArrow
        setImageResource(R.drawable.ic_ab_back)
        rotation = -90f
        scaleType = ImageView.ScaleType.CENTER_INSIDE
    }

    val layoutCategoryOptions: LinearLayout = LinearLayout(context).apply {
        id = R.id.layoutCategoryOptions
        orientation = LinearLayout.VERTICAL
        visibility = View.GONE
    }

    val radioGeneral: TelegramProfileRadioCell = TelegramProfileRadioCell(context).apply {
        id = R.id.radioGeneral
        setText("General")
    }
    val radioObc: TelegramProfileRadioCell = TelegramProfileRadioCell(context).apply {
        id = R.id.radioObc
        setText("OBC")
    }
    val radioSc: TelegramProfileRadioCell = TelegramProfileRadioCell(context).apply {
        id = R.id.radioSc
        setText("SC")
    }
    val radioSt: TelegramProfileRadioCell = TelegramProfileRadioCell(context).apply {
        id = R.id.radioSt
        setText("ST")
    }

    val dividerCategory: View = View(context).apply { id = R.id.dividerCategory }

    val cellReminder: LinearLayout
    val switchReminder: TelegramSwitch = TelegramSwitch(context).apply {
        id = R.id.switchReminder
    }

    // --- Top overlay toolbar (Telegram ActionBar style) ---
    val glassToolbar: TelegramActionBar = TelegramActionBar(context).apply {
        id = R.id.glassToolbar
    }

    val btnBack: ImageButton = createTopIcon(context, R.id.btnBack, R.drawable.ic_ab_back, "Back")
    val tvToolbarTitle: TextView = TextView(context).apply {
        id = R.id.tvToolbarTitle
        setTextSize(TypedValue.COMPLEX_UNIT_DIP, 19f)
        typeface = mediumFont()
        maxLines = 1
        ellipsize = TextUtils.TruncateAt.END
        alpha = 0f
    }
    val tvToolbarSubtitle: TextView = TextView(context).apply {
        id = R.id.tvToolbarSubtitle
        setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13f)
        maxLines = 1
        ellipsize = TextUtils.TruncateAt.END
        alpha = 0f
    }
    val btnProfileQr: ImageButton = createTopIcon(context, R.id.btnProfileQr, R.drawable.telegram_profile_qr, "QR Code")
    val btnProfileMenu: ImageButton = createTopIcon(context, R.id.btnProfileMenu, R.drawable.ic_ab_other, "More options")

    private val subtitleViews = mutableListOf<TextView>()
    private val valueViews = mutableListOf<TextView>()

    init {
        // 1. Scroll content column
        val scrollCol = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            clipChildren = false
            clipToPadding = false
            setPadding(0, 0, 0, dp(32f))
        }

        // Avatar header block
        avatarContainer.addView(ivProfilePhoto, LayoutHelper.createFrame(100, 100, Gravity.CENTER))
        avatarContainer.addView(profilePhotoScrim, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT.toFloat()))
        avatarOuterContainer.addView(avatarGooey, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT.toFloat()))
        avatarOuterContainer.addView(avatarContainer, FrameLayout.LayoutParams(dp(100f), dp(100f), Gravity.TOP or Gravity.CENTER_HORIZONTAL).apply {
            topMargin = dp(35f)
        })
        avatarOuterContainer.addView(avatarTouchTarget, FrameLayout.LayoutParams(dp(108f), dp(108f), Gravity.TOP or Gravity.CENTER_HORIZONTAL).apply {
            topMargin = dp(31f)
        })
        scrollCol.addView(avatarOuterContainer, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 144))

        // Name, status, chips
        scrollCol.addView(tvName, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 24f, 6f, 24f, 2f))
        scrollCol.addView(tvStatus, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 24f, 0f, 24f, 6f))

        val chipRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        chipRow.addView(chipAdmin, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 4f, 0f, 4f, 0f))
        chipRow.addView(chipPremium, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 4f, 0f, 4f, 0f))
        scrollCol.addView(chipRow, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0f, 0f, 0f, 6f))

        // Photo save/cancel actions
        layoutPhotoActions.addView(btnCancelPhoto, LinearLayout.LayoutParams(0, dp(42f), 1f).apply { marginEnd = dp(8f) })
        layoutPhotoActions.addView(btnSavePhoto, LinearLayout.LayoutParams(0, dp(42f), 1f).apply { marginStart = dp(8f) })
        scrollCol.addView(layoutPhotoActions, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT))

        // Action pills row
        layoutActions.addView(btnSetPhoto, LinearLayout.LayoutParams(0, dp(64f), 1f).apply { marginEnd = dp(6f) })
        layoutActions.addView(btnEditInfo, LinearLayout.LayoutParams(0, dp(64f), 1f).apply { marginStart = dp(3f); marginEnd = dp(3f) })
        layoutActions.addView(btnSettings, LinearLayout.LayoutParams(0, dp(64f), 1f).apply { marginStart = dp(6f) })
        scrollCol.addView(layoutActions, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 16f, 8f, 16f, 14f))

        // Account section
        cellMobile = createInfoCell(context, R.id.cellMobile, tvMobileValue, "Mobile")
        cellUsername = createInfoCell(context, R.id.cellUsername, tvUsernameValue, "Username")
        cellBirthday = createInfoCell(context, R.id.cellBirthday, tvBirthdayValue, "Birthday")

        cardPersonalInfo.addView(tvAccountHeader)
        cardPersonalInfo.addView(cellMobile)
        cardPersonalInfo.addView(dividerMobile, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1).apply { marginStart = dp(20f) })
        cardPersonalInfo.addView(cellUsername)
        cardPersonalInfo.addView(dividerUsername, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1).apply { marginStart = dp(20f) })
        cardPersonalInfo.addView(cellBirthday)
        scrollCol.addView(cardPersonalInfo, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 12f, 0f, 12f, 12f))

        // Study & Category section
        cellCategory = createCategoryCell(context)
        layoutCategoryOptions.addView(radioGeneral, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT))
        layoutCategoryOptions.addView(radioObc, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT))
        layoutCategoryOptions.addView(radioSc, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT))
        layoutCategoryOptions.addView(radioSt, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT))

        cellReminder = createReminderCell(context)

        cardCategory.addView(tvStudyHeader)
        cardCategory.addView(cellCategory)
        cardCategory.addView(layoutCategoryOptions)
        cardCategory.addView(dividerCategory, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1).apply { marginStart = dp(20f) })
        cardCategory.addView(cellReminder)
        scrollCol.addView(cardCategory, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 12f, 0f, 12f, 12f))

        profileScrollView.addView(scrollCol, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        ))
        root.addView(profileScrollView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT.toFloat()))

        // 2. Top overlay toolbar
        val barRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(4f), 0, dp(4f), 0)
        }
        val titleCol = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(10f), 0, dp(8f), 0)
        }
        titleCol.addView(tvToolbarTitle)
        titleCol.addView(tvToolbarSubtitle)

        barRow.addView(btnBack, LinearLayout.LayoutParams(dp(48f), dp(48f)))
        barRow.addView(titleCol, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        barRow.addView(btnProfileQr, LinearLayout.LayoutParams(dp(48f), dp(48f)))
        barRow.addView(btnProfileMenu, LinearLayout.LayoutParams(dp(48f), dp(48f)))

        glassToolbar.addView(barRow, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 56f))
        root.addView(glassToolbar, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.TOP))

        applyTelegramTheme()
    }

    private fun createTopIcon(context: Context, viewId: Int, iconRes: Int, desc: String): ImageButton {
        return ImageButton(context).apply {
            id = viewId
            setImageResource(iconRes)
            contentDescription = desc
            scaleType = ImageView.ScaleType.CENTER_INSIDE
            setPadding(dp(12f), dp(12f), dp(12f), dp(12f))
        }
    }

    private fun createActionPill(context: Context, viewId: Int, iconRes: Int, label: String): LinearLayout {
        val pill = LinearLayout(context).apply {
            id = viewId
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            isClickable = true
            isFocusable = true
            contentDescription = label
            setPadding(dp(8f), dp(8f), dp(8f), dp(8f))
        }
        val iv = ImageView(context).apply {
            setImageResource(iconRes)
            scaleType = ImageView.ScaleType.CENTER_INSIDE
        }
        val tv = TextView(context).apply {
            text = label
            setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12f)
            typeface = mediumFont()
            gravity = Gravity.CENTER
            maxLines = 1
        }
        pill.addView(iv, LayoutHelper.createLinear(24, 24, 0f, 0f, 0f, 4f))
        pill.addView(tv, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT))
        return pill
    }

    private fun createCellValue(context: Context, viewId: Int): TextView {
        return TextView(context).apply {
            id = viewId
            setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16f)
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
            valueViews.add(this)
        }
    }

    private fun createInfoCell(context: Context, viewId: Int, valueView: TextView, subtitle: String): LinearLayout {
        val cell = LinearLayout(context).apply {
            id = viewId
            orientation = LinearLayout.VERTICAL
            isClickable = true
            isFocusable = true
            setPadding(dp(20f), dp(10f), dp(20f), dp(10f))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        }
        val sub = TextView(context).apply {
            text = subtitle
            setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13f)
            subtitleViews.add(this)
        }
        cell.addView(valueView)
        cell.addView(sub, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0f, 2f, 0f, 0f))
        return cell
    }

    private fun createCategoryCell(context: Context): LinearLayout {
        val row = LinearLayout(context).apply {
            id = R.id.cellCategory
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            isClickable = true
            isFocusable = true
            setPadding(dp(20f), dp(10f), dp(16f), dp(10f))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        }
        val textCol = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
        val sub = TextView(context).apply {
            text = "Category"
            setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13f)
            subtitleViews.add(this)
        }
        textCol.addView(tvCategoryValue)
        textCol.addView(sub, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0f, 2f, 0f, 0f))
        row.addView(textCol, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        row.addView(ivCategoryArrow, LayoutHelper.createLinear(20, 20))
        return row
    }

    private fun createReminderCell(context: Context): LinearLayout {
        val row = LinearLayout(context).apply {
            id = R.id.cellReminder
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            isClickable = true
            isFocusable = true
            setPadding(dp(20f), dp(12f), dp(16f), dp(12f))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        }
        val textCol = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
        val title = TextView(context).apply {
            text = "Daily Practice Reminder"
            setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16f)
            valueViews.add(this)
        }
        val sub = TextView(context).apply {
            text = "Get a daily reminder to practice mock tests"
            setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13f)
            subtitleViews.add(this)
        }
        textCol.addView(title)
        textCol.addView(sub, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0f, 2f, 0f, 0f))
        row.addView(textCol, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        row.addView(switchReminder, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT))
        return row
    }

    fun applyTelegramTheme() {
        val bgGray = Theme.getColor(Theme.key_windowBackgroundGray)
        val textPrimary = Theme.getColor(Theme.key_windowBackgroundWhiteBlackText)
        val textSecondary = Theme.getColor(Theme.key_windowBackgroundWhiteGrayText)
        val headerBlue = Theme.getColor(Theme.key_windowBackgroundWhiteBlueHeader)
        val divider = Theme.getColor(Theme.key_divider)
        val abIcon = Theme.getColor(Theme.key_actionBarDefaultIcon)
        val abSelector = Theme.getColor(Theme.key_actionBarDefaultSelector)

        root.setBackgroundColor(bgGray)
        profileScrollView.setBackgroundColor(bgGray)
        tvName.setTextColor(textPrimary)
        tvStatus.setTextColor(textSecondary)
        tvToolbarTitle.setTextColor(textPrimary)
        tvToolbarSubtitle.setTextColor(textSecondary)
        tvAccountHeader.setTextColor(headerBlue)
        tvStudyHeader.setTextColor(headerBlue)

        valueViews.forEach { it.setTextColor(textPrimary) }
        subtitleViews.forEach { it.setTextColor(textSecondary) }
        ivCategoryArrow.imageTintList = ColorStateList.valueOf(textSecondary)

        val iconTint = ColorStateList.valueOf(abIcon)
        listOf(btnBack, btnProfileQr, btnProfileMenu).forEach { btn ->
            btn.imageTintList = iconTint
            btn.background = Theme.createSelectorDrawable(abSelector, Theme.RIPPLE_MASK_CIRCLE_20DP)
        }

        listOf(cellMobile, cellUsername, cellBirthday, cellCategory, cellReminder).forEach { cell ->
            cell.background = Theme.getSelectorDrawable(false)
        }
        dividerMobile.setBackgroundColor(divider)
        dividerUsername.setBackgroundColor(divider)
        dividerCategory.setBackgroundColor(divider)
    }
}
