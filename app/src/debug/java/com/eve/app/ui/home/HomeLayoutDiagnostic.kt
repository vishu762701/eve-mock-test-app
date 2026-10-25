package com.eve.app.ui.home

import android.app.Activity
import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout

/**
 * DEBUG BUILD ONLY: noninteractive, disposable overlay capturing screen state.
 * No geometry or blur is changed. Release source set contains a no-op counterpart.
 */
object HomeLayoutDiagnostic {
    private const val TAG = "eve_home_debug_diagnostic"
    private var lastDrawerOffset = Float.NaN
    private var drawerMotionState = "unknown"
    private val registered = java.util.WeakHashMap<DrawerLayout, Boolean>()

    fun report(activity: Activity, drawer: DrawerLayout, pill: View?, overlays: List<Pair<String, View>>) {
        val decor = activity.window.decorView as? FrameLayout ?: return
        if (registered.put(drawer, true) == null) {
            drawer.addDrawerListener(object : DrawerLayout.SimpleDrawerListener() {
                override fun onDrawerSlide(drawerView: View, slideOffset: Float) {
                    lastDrawerOffset = slideOffset
                }
                override fun onDrawerStateChanged(newState: Int) {
                    drawerMotionState = when (newState) {
                        DrawerLayout.STATE_IDLE -> "idle"
                        DrawerLayout.STATE_DRAGGING -> "dragging"
                        DrawerLayout.STATE_SETTLING -> "settling"
                        else -> "unknown($newState)"
                    }
                }
            })
        }
        val panel = (decor.findViewWithTag<View>(TAG) as? TextView)
            ?: TextView(activity).apply {
                tag = TAG
                textSize = 9f
                typeface = android.graphics.Typeface.MONOSPACE
                setTextColor(Color.WHITE)
                setBackgroundColor(0xE6000000.toInt())
                setPadding(8, 6, 8, 6)
                isClickable = false
                isFocusable = false
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
                decor.addView(this, FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    Gravity.BOTTOM
                ))
            }
        val overlaysPlus = overlays + listOfNotNull(
            decor.findViewWithTag<View>("pre_reveal_overlay")?.let { "preReveal" to it },
            decor.findViewWithTag<View>("theme_switch_freeze_overlay")?.let { "freeze" to it }
        )
        fun info(label: String, view: View?): String {
            if (view == null) return "$label: MISSING"
            val vis = when (view.visibility) { View.VISIBLE -> "V"; View.INVISIBLE -> "I"; else -> "G" }
            val loc = IntArray(2)
            view.getLocationOnScreen(loc)
            val lp = view.layoutParams
            val grav = (lp as? FrameLayout.LayoutParams)?.gravity?.toString() ?: "-"
            return "$label ${view.javaClass.simpleName} $vis a=${"%.2f".format(view.alpha)} " +
                "t=${view.translationX.toInt()},${view.translationY.toInt()} " +
                "b=${loc[0]},${loc[1]},${view.width},${view.height} g=$grav"
        }
        fun update() {
            val dimFlag = (activity.window.attributes.flags and
                android.view.WindowManager.LayoutParams.FLAG_DIM_BEHIND) != 0
            val snapshot = buildString {
                append("EVE HOME DEBUG | drawer open=${drawer.isDrawerOpen(GravityCompat.START)} ")
                append("visible=${drawer.isDrawerVisible(GravityCompat.START)} ")
                append("offset=${if (lastDrawerOffset.isNaN()) "not-seen" else "%.2f".format(lastDrawerOffset)} ")
                append("motion=$drawerMotionState dim=$dimFlag/${activity.window.attributes.dimAmount}\n")
                append(info("pill", pill)).append('\n')
                append(info("drawer", drawer)).append('\n')
                for ((name, view) in overlaysPlus) append(info(name, view)).append('\n')
                append("decorChildCount=${decor.childCount}; ThemeTransition=")
                append(com.eve.app.util.ThemeManager.isTransitioning)
                append('\n')
                // Enumerate any unrecognized fullscreen layer above the Home content.
                // A stuck snapshot/dim view may have no known tag. Show draw order,
                // bounds and alpha without collecting personal content or pixels.
                val contentRoot = activity.findViewById<ViewGroup>(android.R.id.content)
                for ((owner, group) in listOfNotNull(
                    "decor" to decor,
                    contentRoot?.let { "content" to it }
                )) {
                    val candidates = (0 until group.childCount).map { index ->
                        index to group.getChildAt(index)
                    }.filter { (_, child) ->
                        child !== panel && child.visibility == View.VISIBLE &&
                            child.width >= decor.width * 0.75f &&
                            child.height >= decor.height * 0.75f
                    }.takeLast(4)
                    for ((index, layer) in candidates) {
                        append(info("$owner[$index]", layer)).append('\n')
                    }
                }
            }
            android.util.Log.d("EveHomeLayoutDiag", snapshot)
            panel.text = snapshot
            panel.bringToFront()
        }
        panel.post { update() }
        panel.postDelayed({ if (panel.isAttachedToWindow) update() }, 750L)
    }
}
