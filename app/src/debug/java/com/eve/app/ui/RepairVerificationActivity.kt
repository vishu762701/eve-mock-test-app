package com.eve.app.ui

import android.os.Bundle
import android.widget.FrameLayout
import com.eve.app.ui.common.EveBaseActivity

/** Instrumentation host for production layouts and system-bar lifecycle.
 * Never included in release APKs; no network, credentials or student data. */
class RepairVerificationActivity : EveBaseActivity() {
    var retainedAnswer = ""
    var themeRecreationDetected = false
        private set
    private var homeFixture = false
    var homeBinding: com.eve.app.databinding.ActivityMainBinding? = null
        private set

    fun showLoadedHomeFixture(listState: android.os.Parcelable? = null) {
        homeFixture = true
        val b = com.eve.app.databinding.ActivityMainBinding.inflate(layoutInflater)
        homeBinding = b
        setContentView(b.root)
        b.tvWelcome.text = "Home handoff fixture"
        b.rvExams.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this)
        b.rvExams.layoutAnimation = null
        b.rvExams.adapter = com.eve.app.ui.home.ExamAdapter({ _, _, _ -> }).apply {
            submit(List(40) { i -> com.eve.app.ui.home.HomeListItem.ExamRow(
                com.eve.app.data.model.Exam(id = "fixture-$i", examName = "Fixture exam $i"), false, false) })
        }
        com.eve.app.util.ShimmerHelper.showContentImmediately(b.shimmerSkeletonHome, b.rvExams)
        b.rvExams.layoutManager?.onRestoreInstanceState(listState)
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        themeRecreationDetected = com.eve.app.util.ThemeManager.isThemeRecreation(this, savedInstanceState)
        retainedAnswer = savedInstanceState?.getString("answer").orEmpty()
        if (savedInstanceState?.getBoolean("home_fixture") == true) {
            @Suppress("DEPRECATION")
            showLoadedHomeFixture(savedInstanceState.getParcelable("fixture_scroll"))
        } else setContentView(FrameLayout(this))
    }
    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString("answer", retainedAnswer)
        outState.putBoolean("home_fixture", homeFixture)
        outState.putParcelable("fixture_scroll", homeBinding?.rvExams?.layoutManager?.onSaveInstanceState())
        super.onSaveInstanceState(outState)
    }
}
