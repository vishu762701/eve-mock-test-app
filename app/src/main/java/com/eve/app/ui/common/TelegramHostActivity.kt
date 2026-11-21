package com.eve.app.ui.common

import android.app.Activity
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.Menu
import android.view.ViewGroup
import android.view.Window
import android.widget.FrameLayout
import android.widget.LinearLayout
import org.telegram.messenger.AndroidUtilities
import org.telegram.ui.ActionBar.ActionBar
import org.telegram.ui.ActionBar.BaseFragment
import org.telegram.ui.ActionBar.INavigationLayout
import org.telegram.ui.ActionBar.Theme

/**
 * Reusable base Activity that implements Telegram's INavigationLayout and hosts a single BaseFragment.
 */
abstract class TelegramHostActivity<F : BaseFragment> : EveBaseActivity(), INavigationLayout {
    protected lateinit var fragment: F
    private lateinit var rootOverlay: FrameLayout
    protected lateinit var container: LinearLayout
    private val fragmentStack = mutableListOf<BaseFragment>()
    private var delegate: INavigationLayout.INavigationLayoutDelegate? = null
    private var stackChangedListener: Runnable? = null
    private var bubbleMode = false
    private var sheetMode = false
    private var hostWindow: Window? = null

    override fun getWindow(): Window = hostWindow ?: super<EveBaseActivity>.getWindow()

    protected open fun createTelegramFragment(savedInstanceState: Bundle?): F? = createFragment()

    protected open fun createFragment(): F? = null

    protected open fun onBeforeCreateFragment(savedInstanceState: Bundle?): Boolean = true

    protected open fun onFragmentHosted(fragment: F) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!onBeforeCreateFragment(savedInstanceState)) {
            return
        }

        AndroidUtilities.checkDisplaySize(this, resources.configuration)
        Theme.syncFromEvePalette(this)
        Theme.createCommonResources(this)

        rootOverlay = FrameLayout(this).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite))
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
        rootOverlay.addView(container)

        val created = createTelegramFragment(savedInstanceState) ?: return
        fragment = created
        fragmentStack.clear()
        fragmentStack.add(fragment)

        fragment.setParentLayout(this)
        fragment.onFragmentCreate()

        val fragmentView = fragment.createView(this)
        val actionBar = fragment.actionBar

        if (actionBar != null) {
            (actionBar.parent as? ViewGroup)?.removeView(actionBar)
            actionBar.setOccupyStatusBar(false)
            val actionBarHeight = ActionBar.getCurrentActionBarHeight()
            container.addView(
                actionBar,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    actionBarHeight
                )
            )
        }

        if (fragmentView != null) {
            (fragmentView.parent as? ViewGroup)?.removeView(fragmentView)
            container.addView(
                fragmentView,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            )
        }

        setContentView(rootOverlay)
        onFragmentHosted(fragment)
        fragment.onTransitionAnimationEnd(true, false)
    }

    override fun onResume() {
        super.onResume()
        if (::fragment.isInitialized) {
            fragment.onResume()
        }
    }

    override fun onPause() {
        super.onPause()
        if (::fragment.isInitialized) {
            fragment.onPause()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::fragment.isInitialized) {
            fragment.onFragmentDestroy()
        }
        fragmentStack.clear()
    }

    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        if (::fragment.isInitialized && !fragment.onBackPressed(true)) {
            return
        }
        super.onBackPressed()
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
    }

    override fun onLowMemory() {
        super.onLowMemory()
    }

    // --- INavigationLayout implementation ---

    override fun getView(): ViewGroup =
        if (::container.isInitialized) container else findViewById(android.R.id.content)

    override fun getParentActivity(): Activity = this

    override fun getOverlayContainerView(): FrameLayout =
        if (::rootOverlay.isInitialized) rootOverlay else findViewById(android.R.id.content)

    override fun getFragmentStack(): MutableList<BaseFragment> = fragmentStack

    override fun isLayersLayout(): Boolean = false

    override fun isRightLayout(): Boolean = false

    override fun presentFragment(params: INavigationLayout.NavigationParams?): Boolean = false

    override fun checkTransitionAnimation(): Boolean = false

    override fun addFragmentToStack(fragment: BaseFragment?, position: Int): Boolean {
        if (fragment == null) return false
        fragment.setParentLayout(this)
        if (position < 0 || position > fragmentStack.size) {
            fragmentStack.add(fragment)
        } else {
            fragmentStack.add(position, fragment)
        }
        stackChangedListener?.run()
        return true
    }

    override fun removeFragmentFromStack(fragment: BaseFragment?, immediate: Boolean) {
        if (fragment == null) return
        fragmentStack.remove(fragment)
        stackChangedListener?.run()
    }

    override fun setDelegate(delegate: INavigationLayout.INavigationLayoutDelegate?) {
        this.delegate = delegate
    }

    override fun closeLastFragment(animated: Boolean, forceNoAnimation: Boolean) {
        finish()
    }

    override fun setRemoveActionBarExtraHeight(removeExtraHeight: Boolean) {}

    override fun setTitleOverlayText(title: String?, titleId: Int, action: Runnable?) {}

    override fun animateThemedValues(
        settings: INavigationLayout.ThemeAnimationSettings?,
        onDone: Runnable?
    ) {
        onDone?.run()
    }

    override fun getThemeAnimationValue(): Float = 1f

    override fun setFragmentStackChangedListener(onFragmentStackChanged: Runnable?) {
        this.stackChangedListener = onFragmentStackChanged
    }

    override fun isTransitionAnimationInProgress(): Boolean = false

    override fun resumeDelayedFragmentAnimation() {}

    override fun allowSwipe(): Boolean = false

    override fun isInPassivePreviewMode(): Boolean = false

    override fun setInBubbleMode(bubbleMode: Boolean) {
        this.bubbleMode = bubbleMode
    }

    override fun isInBubbleMode(): Boolean = bubbleMode

    override fun isInPreviewMode(): Boolean = false

    override fun isPreviewOpenAnimationInProgress(): Boolean = false

    override fun movePreviewFragment(dy: Float) {}

    override fun expandPreviewFragment() {}

    override fun finishPreviewFragment() {}

    override fun setFragmentPanTranslationOffset(offset: Int) {}

    override fun setHighlightActionButtons(highlight: Boolean) {}

    override fun getCurrentPreviewFragmentAlpha(): Float = 1f

    override fun drawCurrentPreviewFragment(canvas: Canvas?, foregroundDrawable: Drawable?) {}

    override fun drawHeaderShadow(canvas: Canvas?, alpha: Int, y: Int) {}

    override fun isSwipeInProgress(): Boolean = false

    override fun extendActionMode(menu: Menu?): Boolean = false

    override fun onActionModeStarted(mode: Any?) {}

    override fun onActionModeFinished(mode: Any?) {}

    override fun setIsSheet(isSheet: Boolean) {
        this.sheetMode = isSheet
    }

    override fun isSheet(): Boolean = sheetMode

    override fun updateTitleOverlay() {}

    override fun setWindow(window: Window?) {
        this.hostWindow = window
    }

    override fun setNavigationBarColor(color: Int) {
        (hostWindow ?: window)?.navigationBarColor = color
    }
}
