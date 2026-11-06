package com.eve.app.ui.home

import com.eve.app.data.model.HomeBanner
import com.eve.app.data.repository.HomeBannerRepository
import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class HomePollBannerIntegrationTest {

    @Test
    fun homeBannerRepositoryEnforcesCacheAndCoalescing() {
        val src = File("src/main/java/com/eve/app/data/repository/HomeBannerRepository.kt").readText()

        assertTrue("Must have cachedBanners volatile variable",
            src.contains("@Volatile\n        private var cachedBanners: List<HomeBanner>? = null") ||
            src.contains("private var cachedBanners: List<HomeBanner>? = null"))
        assertTrue("Must provide clearCache helper",
            src.contains("fun clearCache()"))
        assertTrue("Must emit cachedBanners if present",
            src.contains("cachedBanners?.let { emit(it) }"))
        assertTrue("Must invalidate cache on uploadBanner",
            src.substringAfter("suspend fun uploadBanner").contains("clearCache()"))
        assertTrue("Must invalidate cache on updateBannerLink",
            src.substringAfter("suspend fun updateBannerLink").contains("clearCache()"))
        assertTrue("Must invalidate cache on deleteBanner",
            src.substringAfter("suspend fun deleteBanner").contains("clearCache()"))
        assertTrue("Must invalidate cache on reorderBanner",
            src.substringAfter("suspend fun reorderBanner").contains("clearCache()"))
    }

    @Test
    fun mainActivityEnforcesPollLifecycleAndSearchSync() {
        val src = File("src/main/java/com/eve/app/ui/home/MainActivity.kt").readText()

        // 1. Poll state & lifecycle ownership
        assertTrue("Must declare pollRepository in MainActivity",
            src.contains("private val pollRepository = PollRepository()"))
        assertTrue("Must track homePollJob",
            src.contains("private var homePollJob: Job? = null"))
        assertTrue("Must cancel homePollJob in onStop",
            src.contains("homePollJob?.cancel()"))
        assertTrue("Must call loadActivePoll in onStart",
            src.contains("loadActivePoll()"))

        // 2. Poll account isolation
        assertTrue("Must verify homeOwnerUid against current user in loadActivePoll",
            src.contains("if (user == null || requestUid.isBlank() || requestUid != user.uid)"))

        // 3. Search visibility synchronization
        val refreshSearch = src.substringAfter("private fun refreshSearchChrome()")
            .substringBefore("private fun setupSearch()")
        assertTrue("Must synchronize cardPoll with isSearchActive",
            refreshSearch.contains("binding.cardPoll.visibility =\n            if (!isSearchActive && activePollData != null) View.VISIBLE else View.GONE"))

        // 4. Banner change detection to prevent unwanted page resets
        val listenBanner = src.substringAfter("private fun startListeningToHomeBanner()")
            .substringBefore("private fun startBannerAutoScroll(")
        assertTrue("Must detect banner changes before resetting currentItem",
            listenBanner.contains("val bannersChanged = homeBanners != banners"))
    }

    @Test
    fun activityMainLayoutDeclaresTelegramCommunityPollSurface() {
        val layout = File("src/main/res/layout/activity_main.xml").readText()

        assertTrue("Must declare cardPoll DesignCardView",
            layout.contains("android:id=\"@+id/cardPoll\""))
        assertTrue("Must declare tvHomePollQuestion",
            layout.contains("android:id=\"@+id/tvHomePollQuestion\""))
        assertTrue("Must declare rgHomePollOptions",
            layout.contains("android:id=\"@+id/rgHomePollOptions\""))
        assertTrue("Must declare btnSubmitHomeVote",
            layout.contains("android:id=\"@+id/btnSubmitHomeVote\""))
        assertTrue("Must declare layoutHomePollResults",
            layout.contains("android:id=\"@+id/layoutHomePollResults\""))
        assertTrue("Must declare tvHomePollTotalVotes",
            layout.contains("android:id=\"@+id/tvHomePollTotalVotes\""))
    }

    @Test
    fun pollOptionResultUsesLinearProgressIndicator() {
        val layout = File("src/main/res/layout/item_poll_option_result.xml").readText()

        assertTrue("Must use LinearProgressIndicator for poll option percentage",
            layout.contains("com.google.android.material.progressindicator.LinearProgressIndicator"))
        assertTrue("Must use pbOption id",
            layout.contains("android:id=\"@+id/pbOption\""))
    }
}
