package com.eve.app.ui.home

import android.app.Activity
import android.view.View
import androidx.drawerlayout.widget.DrawerLayout

/** Release build never creates, displays or logs Home diagnostics. */
object HomeLayoutDiagnostic {
    @Suppress("UNUSED_PARAMETER")
    fun report(activity: Activity, drawer: DrawerLayout, pill: View?, overlays: List<Pair<String, View>>) = Unit
}
