package com.eve.app.ui

import android.os.Bundle
import android.widget.FrameLayout
import com.eve.app.ui.common.EveBaseActivity

/** Instrumentation host for production layouts and system-bar lifecycle.
 * Never included in release APKs; no network, credentials or student data. */
class RepairVerificationActivity : EveBaseActivity() {
    var retainedAnswer = ""
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        retainedAnswer = savedInstanceState?.getString("answer").orEmpty()
        setContentView(FrameLayout(this))
    }
    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString("answer", retainedAnswer)
        super.onSaveInstanceState(outState)
    }
}
