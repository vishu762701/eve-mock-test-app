package com.eve.app.ui.profile

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.provider.Settings
import android.util.TypedValue
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import com.eve.app.R
import com.eve.app.theme.TelegramThemeEngine
import com.eve.app.ui.profile.popup.ActionBarMenuSubItem
import com.eve.app.ui.profile.popup.ActionBarPopupWindow
import com.eve.app.ui.profile.popup.PopupPlatform
import com.eve.app.ui.profile.popup.Theme
import kotlin.math.ceil

/** EVE destinations around the actual Telegram normal ActionBar popup renderer. */
internal class ProfileOverflowPopup(private val anchor: View) {
    private var window: ActionBarPopupWindow? = null
    val isShowing: Boolean get() = window?.isShowing == true

    fun show(usernameEnabled: Boolean, onUsername: () -> Unit, onCopyLink: () -> Unit, canEdit: Boolean = true) {
        if (isShowing) return
        val context = anchor.context
        PopupPlatform.density = context.resources.displayMetrics.density
        fun dp(value: Float) = PopupPlatform.dp(value)
        val palette = TelegramThemeEngine.resolveCurrentPalette(context)
        val provider = Theme.ResourcesProvider { key ->
            when (key) {
                Theme.key_actionBarDefaultSubmenuBackground -> palette.submenuBackground
                Theme.key_actionBarDefaultSubmenuItem -> palette.submenuText
                Theme.key_actionBarDefaultSubmenuItemIcon -> palette.submenuIcon
                Theme.key_dialogButtonSelector -> palette.submenuSelector
                Theme.key_actionBarDefaultSubmenuSeparator -> palette.submenuSeparator
                else -> Color.BLACK
            }
        }
        val layout = ActionBarPopupWindow.ActionBarPopupWindowLayout(context,
            R.drawable.telegram_dialog_popup_fixed_alert4, provider,
            ActionBarPopupWindow.ActionBarPopupWindowLayout.FLAG_USE_SWIPEBACK)
        layout.setFitItems(true)
        val popup = ActionBarPopupWindow(layout, -2, -2, true)
        window = popup
        val motion = Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f
        popup.setAnimationEnabled(motion)
        layout.setAnimationEnabled(motion)
        popup.animationStyle = 0
        popup.isOutsideTouchable = true
        popup.isClippingEnabled = true
        popup.inputMethodMode = ActionBarPopupWindow.INPUT_METHOD_NOT_NEEDED
        popup.softInputMode = WindowManager.LayoutParams.SOFT_INPUT_STATE_UNSPECIFIED
        popup.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        layout.isFocusableInTouchMode = true
        layout.setDispatchKeyEventListener { event ->
            if (event.keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) popup.dismiss()
        }
        fun row(label: String, icon: Int, top: Boolean, bottom: Boolean, enabled: Boolean, action: () -> Unit) {
            val item = ActionBarMenuSubItem(context, 0, top, bottom, provider)
            item.setTextAndIcon(label, icon, null)
            // Native text respects system font settings; full labels are exposed
            // to TalkBack even when the original single-line row ellipsizes.
            item.textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            item.minimumWidth = dp(196f)
            item.contentDescription = label
            item.isFocusable = true
            item.isClickable = true
            item.isEnabled = enabled
            item.setOnClickListener {
                if (!item.isEnabled) return@setOnClickListener
                popup.dismiss()
                action()
            }
            val textHeight = item.textView.paint.fontMetricsInt.let { it.bottom - it.top }
            item.setItemHeight(ceil(maxOf(dp(48f), textHeight).toFloat() / PopupPlatform.density).toInt())
            layout.addView(item, LinearLayout.LayoutParams(-1, -2))
        }
        row("Change username", R.drawable.profile_menu_username_change, true, false, canEdit, onUsername)
        row("Copy link to profile", R.drawable.profile_msg_link2, false, true, usernameEnabled, onCopyLink)
        val frame = android.graphics.Rect()
        anchor.getWindowVisibleDisplayFrame(frame)
        layout.measure(View.MeasureSpec.makeMeasureSpec((frame.width() - dp(40f)).coerceAtLeast(dp(48f)), View.MeasureSpec.AT_MOST),
            View.MeasureSpec.makeMeasureSpec(frame.height(), View.MeasureSpec.AT_MOST))
        popup.width = layout.measuredWidth
        popup.height = layout.measuredHeight
        popup.setOnDismissListener { if (window === popup) window = null }
        // ActionBarMenuItem.updateOrShowPopup: right edge of the triggering item,
        // menu content starts at the ActionBar's top rather than below it.
        popup.showAsDropDown(anchor, anchor.width - layout.measuredWidth, -anchor.height)
        popup.startAnimation()
    }

    fun dismiss(immediate: Boolean = false) { window?.dismiss(!immediate) }
}
