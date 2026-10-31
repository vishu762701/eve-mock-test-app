/*
 * Derived from Telegram Android ActionBar.java & ActionBarMenuItem.java,
 * Copyright Nikolai Kudashov, 2013-2018. GNU GPL v2 or later. See THIRD_PARTY_NOTICES.md.
 */

package com.eve.app.ui.admin.tg

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.Context
import android.graphics.Typeface
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.activity.OnBackPressedDispatcherOwner
import com.eve.app.R
import com.eve.app.theme.TelegramThemeEngine

/**
 * Official Telegram ActionBar with expanding search ported for EVE Admin Hub.
 * Upstream: TMessagesProj/src/main/java/org/telegram/ui/ActionBar/ActionBar.java:150-250
 *           TMessagesProj/src/main/java/org/telegram/ui/ActionBar/ActionBarMenuItem.java:1316-1390
 * - Height: 56dp
 * - Back button: 48x48dp
 * - Title: 20sp bold
 * - Subtitle: 14sp
 * - ADAPTED search overlay; original Telegram ActionBar.java:1188-1275 uses 150ms alpha and 0.95 title scale.
 * - Rapid search toggles cancel old animations and guard IME callbacks.
 */
class TelegramActionBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val density = resources.displayMetrics.density
    private fun dp(v: Float): Int = (v * density).toInt()

    val backButton: ImageButton = ImageButton(context)
    private val titlesContainer: LinearLayout = LinearLayout(context)
    val titleTextView: TextView = TextView(context)
    val subtitleTextView: TextView = TextView(context)

    val searchButton: ImageButton = ImageButton(context)
    val filterButton: ImageButton = ImageButton(context)
    val actionButton: ImageButton = ImageButton(context)
    private var hasFilterAction: Boolean = false
    private val searchContainer: FrameLayout = FrameLayout(context)
    val searchEditText: EditText = EditText(context)
    val clearButton: ImageButton = ImageButton(context)

    var isSearchExpanded: Boolean = false
        private set
    private var searchAnimationGeneration: Int = 0

    var onSearchTextChanged: ((String) -> Unit)? = null
    var onSearchCollapsed: (() -> Unit)? = null
    var onBackPressed: (() -> Unit)? = null
    private var searchBackBound = false

    init {
        val palette = TelegramThemeEngine.resolveCurrentPalette(context)
        minimumHeight = dp(56f)

        val outValue = TypedValue()
        context.theme.resolveAttribute(android.R.attr.selectableItemBackgroundBorderless, outValue, true)
        val selectableBorderless = outValue.resourceId

        // 1. Back button
        backButton.setImageResource(R.drawable.ic_back)
        backButton.scaleType = ImageView.ScaleType.CENTER
        backButton.setColorFilter(palette.textPrimary)
        backButton.setBackgroundResource(selectableBorderless)
        backButton.contentDescription = "Back"
        addView(backButton, LayoutParams(dp(48f), dp(48f), Gravity.START or Gravity.CENTER_VERTICAL).apply {
            marginStart = dp(4f)
        })
        backButton.setOnClickListener {
            if (isSearchExpanded) {
                closeSearch()
            } else {
                onBackPressed?.invoke()
            }
        }

        // 2. Titles Container (Title + Subtitle)
        titlesContainer.orientation = LinearLayout.VERTICAL
        titlesContainer.gravity = Gravity.CENTER_VERTICAL

        titleTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20f)
        titleTextView.typeface = Typeface.DEFAULT_BOLD
        titleTextView.setTextColor(palette.textPrimary)
        titleTextView.ellipsize = TextUtils.TruncateAt.END
        titleTextView.maxLines = 1
        titlesContainer.addView(titleTextView, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT))

        subtitleTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14f)
        subtitleTextView.setTextColor(palette.textSecondary)
        subtitleTextView.ellipsize = TextUtils.TruncateAt.END
        subtitleTextView.maxLines = 1
        subtitleTextView.visibility = View.GONE
        titlesContainer.addView(subtitleTextView, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT))

        addView(titlesContainer, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, Gravity.START or Gravity.CENTER_VERTICAL).apply {
            marginStart = dp(64f)
            marginEnd = dp(56f)
        })

        // 3. Search Action Button
        searchButton.setImageResource(R.drawable.ic_search)
        searchButton.scaleType = ImageView.ScaleType.CENTER
        searchButton.setColorFilter(palette.textPrimary)
        searchButton.setBackgroundResource(selectableBorderless)
        searchButton.contentDescription = "Search"
        addView(searchButton, LayoutParams(dp(48f), dp(48f), Gravity.END or Gravity.CENTER_VERTICAL).apply {
            marginEnd = dp(4f)
        })
        searchButton.setOnClickListener {
            openSearch()
        }

        // 3a. Filter Action Button
        filterButton.setImageResource(R.drawable.ic_filter)
        filterButton.scaleType = ImageView.ScaleType.CENTER
        filterButton.setColorFilter(palette.textPrimary)
        filterButton.setBackgroundResource(selectableBorderless)
        filterButton.contentDescription = "Filter"
        filterButton.visibility = View.GONE
        addView(filterButton, LayoutParams(dp(48f), dp(48f), Gravity.END or Gravity.CENTER_VERTICAL).apply {
            marginEnd = dp(52f)
        })

        // 3b. Generic / Done Action Button
        actionButton.scaleType = ImageView.ScaleType.CENTER
        actionButton.setBackgroundResource(selectableBorderless)
        actionButton.visibility = View.GONE
        addView(actionButton, LayoutParams(dp(48f), dp(48f), Gravity.END or Gravity.CENTER_VERTICAL).apply {
            marginEnd = dp(4f)
        })

        // 4. Search Container (Hidden until search expanded)
        searchContainer.visibility = View.GONE
        searchContainer.setBackgroundColor(palette.surface)

        searchEditText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 18f)
        searchEditText.setTextColor(palette.textPrimary)
        searchEditText.setHintTextColor(palette.textSecondary)
        searchEditText.hint = "Search admin functions..."
        searchEditText.background = null
        searchEditText.maxLines = 1
        searchEditText.isSingleLine = true
        searchEditText.setPadding(0, 0, dp(48f), 0)

        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s?.toString().orEmpty()
                clearButton.visibility = if (query.isNotEmpty()) View.VISIBLE else View.GONE
                onSearchTextChanged?.invoke(query)
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        searchContainer.addView(searchEditText, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT, Gravity.CENTER_VERTICAL).apply {
            marginStart = dp(64f)
            marginEnd = dp(8f)
        })

        clearButton.setImageResource(R.drawable.ic_close)
        clearButton.scaleType = ImageView.ScaleType.CENTER
        clearButton.setColorFilter(palette.textSecondary)
        clearButton.setBackgroundResource(selectableBorderless)
        clearButton.contentDescription = "Clear search"
        clearButton.visibility = View.GONE
        clearButton.setOnClickListener {
            if (searchEditText.text.isNotEmpty()) {
                searchEditText.setText("")
            } else {
                closeSearch()
            }
        }
        searchContainer.addView(clearButton, LayoutParams(dp(48f), dp(48f), Gravity.END or Gravity.CENTER_VERTICAL).apply {
            marginEnd = dp(4f)
        })

        addView(searchContainer, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        // Keep back navigation above the opaque search background.
        backButton.bringToFront()
    }

    /**
     * ADAPTED EVE activity navigation. Telegram's search field closes before
     * the parent fragment exits; hardware/gesture Back must do the same.
     * Register only after the activity's fresh AdminAccessGate succeeds.
     */
    fun bindSystemBack(owner: OnBackPressedDispatcherOwner) {
        if (searchBackBound) return
        searchBackBound = true
        owner.onBackPressedDispatcher.addCallback(owner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (isSearchExpanded) {
                    closeSearch()
                } else {
                    isEnabled = false
                    owner.onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }

    fun setTitle(title: CharSequence) {
        titleTextView.text = title
    }

    fun setSubtitle(subtitle: CharSequence?) {
        if (!subtitle.isNullOrBlank()) {
            subtitleTextView.text = subtitle
            subtitleTextView.visibility = View.VISIBLE
        } else {
            subtitleTextView.visibility = View.GONE
        }
    }

    fun openSearch() {
        if (isSearchExpanded) return
        isSearchExpanded = true
        val generation = ++searchAnimationGeneration
        val wasVisible = searchContainer.visibility == View.VISIBLE
        searchContainer.animate().setListener(null).cancel()
        searchContainer.visibility = View.VISIBLE
        if (!wasVisible) searchContainer.alpha = 0f
        titlesContainer.animate().cancel()
        // Telegram ActionBar.java:1207-1237,1275 - 150ms alpha/0.95 scale.
        titlesContainer.animate().alpha(0f).scaleX(0.95f).scaleY(0.95f).setDuration(150L).start()
        searchContainer.animate().alpha(1f).setDuration(150L).setListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                if (generation != searchAnimationGeneration || !isSearchExpanded || !isAttachedToWindow) return
                searchEditText.requestFocus()
                val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                imm?.showSoftInput(searchEditText, InputMethodManager.SHOW_IMPLICIT)
            }
        }).start()
        searchButton.visibility = View.GONE
        if (hasFilterAction) filterButton.visibility = View.GONE
    }

    fun closeSearch() {
        if (!isSearchExpanded) return
        isSearchExpanded = false
        val generation = ++searchAnimationGeneration
        searchContainer.animate().setListener(null).cancel()
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.hideSoftInputFromWindow(searchEditText.windowToken, 0)
        searchEditText.clearFocus()
        searchEditText.setText("")
        titlesContainer.animate().cancel()
        titlesContainer.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(150L).start()
        searchContainer.animate().alpha(0f).setDuration(150L).setListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                if (generation != searchAnimationGeneration || isSearchExpanded) return
                searchContainer.visibility = View.GONE
                searchButton.visibility = View.VISIBLE
                if (hasFilterAction) filterButton.visibility = View.VISIBLE
                onSearchCollapsed?.invoke()
            }
        }).start()
    }

    override fun onDetachedFromWindow() {
        searchAnimationGeneration++
        searchContainer.animate().setListener(null).cancel()
        titlesContainer.animate().cancel()
        super.onDetachedFromWindow()
    }

    fun showFilterAction(onClick: () -> Unit) {
        val palette = TelegramThemeEngine.resolveCurrentPalette(context)
        hasFilterAction = true
        filterButton.setColorFilter(palette.textPrimary)
        filterButton.visibility = if (isSearchExpanded) View.GONE else View.VISIBLE
        filterButton.setOnClickListener { onClick() }
    }

    fun hideFilterAction() {
        hasFilterAction = false
        filterButton.visibility = View.GONE
    }

    fun showDoneAction(onClick: () -> Unit) {
        val palette = TelegramThemeEngine.resolveCurrentPalette(context)
        actionButton.setImageResource(R.drawable.ic_done)
        actionButton.setColorFilter(palette.accent)
        actionButton.contentDescription = "Done"
        actionButton.visibility = View.VISIBLE
        searchButton.visibility = View.GONE
        actionButton.setOnClickListener { onClick() }
    }

    fun hideAction() {
        actionButton.visibility = View.GONE
        searchButton.visibility = View.VISIBLE
    }
}
