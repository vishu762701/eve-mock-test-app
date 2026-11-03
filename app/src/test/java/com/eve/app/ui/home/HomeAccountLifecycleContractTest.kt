package com.eve.app.ui.home

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/** One retained Home cannot expose a previous account's async UI state. */
class HomeAccountLifecycleContractTest {
    private val home = File("src/main/java/com/eve/app/ui/home/MainActivity.kt").readText()

    @Test fun oneAuthListenerPerStartedHomeAndMatchingOwner() {
        val listener = home.substringAfter("private val homeAuthListener")
            .substringBefore("private var isSearchActive")
        assertTrue(listener.contains("auth.currentUser?.uid != homeOwnerUid"))
        assertTrue(listener.contains("notifJob?.cancel()"))
        assertTrue(listener.contains("bannerObserverJob?.cancel()"))
        assertTrue(listener.contains("finish()"))
        val start = home.substringAfter("override fun onStart()")
            .substringBefore("override fun onStop()")
        assertTrue(start.contains("if (!searchOwnerCurrent) { finish(); return }"))
        assertTrue(start.contains("FirebaseAuth.getInstance().addAuthStateListener(homeAuthListener)"))
        assertTrue(home.substringAfter("override fun onStop()")
            .contains("FirebaseAuth.getInstance().removeAuthStateListener(homeAuthListener)"))
    }

    @Test fun delayedAdminRightsCannotBeAppliedAfterSwitchingUsers() {
        val callback = "adminRepo.isAdmin(current.email)"
        val start = home.substringAfter("override fun onStart()")
            .substringBefore("override fun onStop()")
        val resume = home.substringAfter("override fun onResume()")
            .substringBefore("override fun onPause()")
        for (segment in listOf(start, resume)) {
            val request = segment.indexOf(callback)
            val guard = segment.indexOf("if (!searchOwnerCurrent || isFinishing || isDestroyed) return@launch", request)
            val load = segment.indexOf("viewModel.loadForUser(current.uid, admin)", guard)
            assertTrue("Admin result must be rechecked after suspension", request >= 0 && guard > request && load > guard)
        }
    }

    @Test fun recentExamCardCannotUsePreviousUsersGlobalPreference() {
        val vm = File("src/main/java/com/eve/app/ui/home/HomeViewModel.kt").readText()
        assertTrue(home.contains("HomeRecentExamStore.save(this, homeOwnerUid, exam.id)"))
        assertTrue(vm.contains("HomeRecentExamStore.load("))
        assertTrue(vm.contains("lastUserLoaded"))
        assertTrue(!vm.contains(".getString(\"last_exam_id\", null)"))
        assertTrue(!home.contains(".putString(\"last_exam_id\", exam.id)"))
    }

    @Test fun examClickAndLateGeneratedTestListRequireOriginalOwner() {
        val start = home.substringAfter("private val adapter = ExamAdapter(")
            .substringBefore("onLongClick =")
        assertTrue(start.contains("if (!searchOwnerCurrent || isFinishing || isDestroyed) return@ExamAdapter"))
        val call = start.indexOf("getLiveGeneratedTests(exam.id)")
        val guard = start.indexOf("if (!searchOwnerCurrent || isFinishing || isDestroyed)", call)
        val navigate = start.indexOf("startActivity(intent)", guard)
        assertTrue(call >= 0 && guard > call && navigate > guard)
        assertTrue(start.contains("catch (cancel: kotlinx.coroutines.CancellationException)"))
    }
}
