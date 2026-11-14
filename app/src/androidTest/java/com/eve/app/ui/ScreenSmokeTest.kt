package com.eve.app.ui

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.eve.app.data.repository.AppContentRepository
import com.eve.app.ui.about.AboutActivity
import com.eve.app.ui.about.ContentDisplayActivity
import com.eve.app.ui.admin.*
import com.eve.app.ui.admin.emoji.EmojiStudioActivity
import com.eve.app.ui.bookmarks.BookmarksActivity
import com.eve.app.ui.feedback.FeedbackActivity
import com.eve.app.ui.history.HistoryActivity
import com.eve.app.ui.home.ExamTestsActivity
import com.eve.app.ui.home.MainActivity
import com.eve.app.ui.leaderboard.LeaderboardActivity
import com.eve.app.ui.login.LoginActivity
import com.eve.app.ui.mistakes.MistakesActivity
import com.eve.app.ui.notifications.NotificationsActivity
import com.eve.app.ui.performance.PerformanceActivity
import com.eve.app.ui.poll.PollCreateActivity
import com.eve.app.ui.practice.PracticeActivity
import com.eve.app.ui.premium.PaymentCheckoutActivity
import com.eve.app.ui.premium.PremiumActivity
import com.eve.app.ui.profile.*
import com.eve.app.ui.pyq.PyqActivity
import com.eve.app.ui.result.ResultActivity
import com.eve.app.ui.result.ResultDetailActivity
import com.eve.app.ui.settings.*
import com.eve.app.ui.syllabus.SyllabusActivity
import com.eve.app.ui.test.TestActivity
import com.eve.app.util.Constants
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileOutputStream

/**
 * Phase 0 Screen Smoke Test.
 * Launches every registered Activity from AndroidManifest.xml with valid extras
 * using ActivityScenario and verifies that startup completes without crashing.
 */
@RunWith(AndroidJUnit4::class)
class ScreenSmokeTest {

    private val context: Context get() = ApplicationProvider.getApplicationContext()

    private fun createDummyImageUri(): Uri {
        val file = File(context.cacheDir, "smoke_dummy_avatar.png")
        if (!file.exists()) {
            val bmp = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
            FileOutputStream(file).use { out ->
                bmp.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            bmp.recycle()
        }
        return Uri.fromFile(file)
    }

    @Test
    fun launchCoreFlowScreens() {
        ActivityScenario.launch<LoginActivity>(Intent(context, LoginActivity::class.java)).use {
            assertNotNull(it)
        }
        ActivityScenario.launch<MainActivity>(Intent(context, MainActivity::class.java)).use {
            assertNotNull(it)
        }
    }

    @Test
    fun launchProfileScreens() {
        ActivityScenario.launch<ProfileActivity>(Intent(context, ProfileActivity::class.java)).use {
            assertNotNull(it)
        }
        val qrIntent = Intent(context, UsernameCardActivity::class.java).apply {
            data = Uri.parse("eve-mock-test://username/testuser")
        }
        ActivityScenario.launch<UsernameCardActivity>(qrIntent).use {
            assertNotNull(it)
        }
        val qrShareIntent = Intent(context, QrShareActivity::class.java).apply {
            data = Uri.parse("eve-mock-test://username/testuser")
        }
        ActivityScenario.launch<QrShareActivity>(qrShareIntent).use {
            assertNotNull(it)
        }
        ActivityScenario.launch<EveQrLiveScannerActivity>(Intent(context, EveQrLiveScannerActivity::class.java)).use {
            assertNotNull(it)
        }
        val cropIntent = Intent(context, AvatarCropActivity::class.java).apply {
            putExtra(AvatarCropActivity.EXTRA_SOURCE_URI, createDummyImageUri())
        }
        ActivityScenario.launch<AvatarCropActivity>(cropIntent).use {
            assertNotNull(it)
        }
        val photoCropIntent = Intent(context, PhotoCropActivity::class.java).apply {
            putExtra(AvatarCropActivity.EXTRA_SOURCE_URI, createDummyImageUri())
        }
        ActivityScenario.launch<PhotoCropActivity>(photoCropIntent).use {
            assertNotNull(it)
        }
    }

    @Test
    fun launchProfileEditorScreens() {
        ActivityScenario.launch<ChangeNameActivity>(Intent(context, ChangeNameActivity::class.java).putExtra("current_name", "Test User")).use {
            assertNotNull(it)
        }
        ActivityScenario.launch<ChangeUsernameActivity>(Intent(context, ChangeUsernameActivity::class.java).putExtra("current_username", "testuser")).use {
            assertNotNull(it)
        }
        ActivityScenario.launch<ChangePhoneActivity>(Intent(context, ChangePhoneActivity::class.java).putExtra("current_phone", "+919876543210")).use {
            assertNotNull(it)
        }
        ActivityScenario.launch<ChangeBioActivity>(Intent(context, ChangeBioActivity::class.java).putExtra("current_bio", "Test Bio")).use {
            assertNotNull(it)
        }
    }

    @Test
    fun launchExamAndTestScreens() {
        val examTestsIntent = Intent(context, ExamTestsActivity::class.java).apply {
            putExtra(Constants.EXTRA_EXAM_ID, "fixture_exam")
            putExtra(Constants.EXTRA_EXAM_NAME, "SSC CGL")
        }
        ActivityScenario.launch<ExamTestsActivity>(examTestsIntent).use {
            assertNotNull(it)
        }

        val testIntent = Intent(context, TestActivity::class.java).apply {
            putExtra(Constants.EXTRA_EXAM_ID, "fixture_exam")
            putExtra(Constants.EXTRA_EXAM_NAME, "SSC CGL Mock 1")
            putExtra(Constants.EXTRA_TIME_LIMIT, 30)
        }
        ActivityScenario.launch<TestActivity>(testIntent).use {
            assertNotNull(it)
        }

        val resultIntent = Intent(context, ResultActivity::class.java).apply {
            putExtra(Constants.EXTRA_EXAM_ID, "fixture_exam")
            putExtra(Constants.EXTRA_EXAM_NAME, "SSC CGL Mock 1")
            putExtra(Constants.EXTRA_RESULT_OWNER_UID, "fixture_uid")
            putExtra("EXTRA_SCORE", 75.0)
            putExtra("EXTRA_TOTAL", 100)
            putExtra("EXTRA_CORRECT", 75)
            putExtra("EXTRA_WRONG", 10)
            putExtra("EXTRA_UNATTEMPTED", 15)
        }
        ActivityScenario.launch<ResultActivity>(resultIntent).use {
            assertNotNull(it)
        }

        val detailIntent = Intent(context, ResultDetailActivity::class.java).apply {
            putExtra(ResultDetailActivity.EXTRA_DETAIL_TYPE, ResultDetailActivity.TYPE_STATISTICS)
            putExtra(Constants.EXTRA_EXAM_ID, "fixture_exam")
            putExtra(Constants.EXTRA_EXAM_NAME, "SSC CGL Mock 1")
            putExtra(Constants.EXTRA_RESULT_OWNER_UID, "fixture_uid")
            putExtra("EXTRA_SCORE", 75.0)
            putExtra("EXTRA_TOTAL", 100)
            putExtra("EXTRA_CORRECT", 75)
            putExtra("EXTRA_WRONG", 10)
            putExtra("EXTRA_UNATTEMPTED", 15)
        }
        ActivityScenario.launch<ResultDetailActivity>(detailIntent).use {
            assertNotNull(it)
        }

        val leaderboardIntent = Intent(context, LeaderboardActivity::class.java).apply {
            putExtra(Constants.EXTRA_EXAM_ID, "fixture_exam")
        }
        ActivityScenario.launch<LeaderboardActivity>(leaderboardIntent).use {
            assertNotNull(it)
        }

        ActivityScenario.launch<PerformanceActivity>(Intent(context, PerformanceActivity::class.java)).use {
            assertNotNull(it)
        }
        ActivityScenario.launch<PracticeActivity>(Intent(context, PracticeActivity::class.java)).use {
            assertNotNull(it)
        }
        ActivityScenario.launch<PyqActivity>(Intent(context, PyqActivity::class.java)).use {
            assertNotNull(it)
        }
        ActivityScenario.launch<HistoryActivity>(Intent(context, HistoryActivity::class.java)).use {
            assertNotNull(it)
        }
        ActivityScenario.launch<BookmarksActivity>(Intent(context, BookmarksActivity::class.java)).use {
            assertNotNull(it)
        }
        ActivityScenario.launch<MistakesActivity>(Intent(context, MistakesActivity::class.java)).use {
            assertNotNull(it)
        }
    }

    @Test
    fun launchSettingsAndUtilityScreens() {
        ActivityScenario.launch<SettingsActivity>(Intent(context, SettingsActivity::class.java)).use {
            assertNotNull(it)
        }
        ActivityScenario.launch<AppearanceActivity>(Intent(context, AppearanceActivity::class.java)).use {
            assertNotNull(it)
        }
        ActivityScenario.launch<ThemeActivity>(Intent(context, ThemeActivity::class.java)).use {
            assertNotNull(it)
        }
        ActivityScenario.launch<LanguageSelectActivity>(Intent(context, LanguageSelectActivity::class.java)).use {
            assertNotNull(it)
        }
        ActivityScenario.launch<PasscodeActivity>(Intent(context, PasscodeActivity::class.java)).use {
            assertNotNull(it)
        }
        ActivityScenario.launch<CacheControlActivity>(Intent(context, CacheControlActivity::class.java)).use {
            assertNotNull(it)
        }
        ActivityScenario.launch<PollCreateActivity>(Intent(context, PollCreateActivity::class.java)).use {
            assertNotNull(it)
        }
        ActivityScenario.launch<AboutActivity>(Intent(context, AboutActivity::class.java)).use {
            assertNotNull(it)
        }
        val contentIntent = Intent(context, ContentDisplayActivity::class.java).apply {
            putExtra(ContentDisplayActivity.EXTRA_CONTENT_TYPE, AppContentRepository.TYPE_PRIVACY)
        }
        ActivityScenario.launch<ContentDisplayActivity>(contentIntent).use {
            assertNotNull(it)
        }
        val syllabusIntent = Intent(context, SyllabusActivity::class.java).apply {
            putExtra(Constants.EXTRA_EXAM_ID, "fixture_exam")
            putExtra(Constants.EXTRA_EXAM_NAME, "SSC CGL")
        }
        ActivityScenario.launch<SyllabusActivity>(syllabusIntent).use {
            assertNotNull(it)
        }
        ActivityScenario.launch<NotificationsActivity>(Intent(context, NotificationsActivity::class.java)).use {
            assertNotNull(it)
        }
        ActivityScenario.launch<FeedbackActivity>(Intent(context, FeedbackActivity::class.java)).use {
            assertNotNull(it)
        }
        ActivityScenario.launch<PremiumActivity>(Intent(context, PremiumActivity::class.java)).use {
            assertNotNull(it)
        }
        val checkoutIntent = Intent(context, PaymentCheckoutActivity::class.java).apply {
            putExtra(PaymentCheckoutActivity.EXTRA_PLAN_NAME, "Lifetime Pro")
            putExtra(PaymentCheckoutActivity.EXTRA_AMOUNT, 499)
            putExtra(PaymentCheckoutActivity.EXTRA_ORDER_ID, "order_fixture_123")
            putExtra(PaymentCheckoutActivity.EXTRA_UPI_URI, "upi://pay?pa=test@upi")
        }
        ActivityScenario.launch<PaymentCheckoutActivity>(checkoutIntent).use {
            assertNotNull(it)
        }
    }

    @Test
    fun launchAdminScreens() {
        ActivityScenario.launch<AdminActivity>(Intent(context, AdminActivity::class.java)).use {
            assertNotNull(it)
        }
        ActivityScenario.launch<ManageQuestionsActivity>(Intent(context, ManageQuestionsActivity::class.java)).use {
            assertNotNull(it)
        }
        ActivityScenario.launch<AdminsAndRightsActivity>(Intent(context, AdminsAndRightsActivity::class.java)).use {
            assertNotNull(it)
        }
        val rightsEditIntent = Intent(context, AdminRightsEditActivity::class.java).apply {
            putExtra(AdminRightsEditActivity.EXTRA_EMAIL, "admin@test.invalid")
            putExtra(AdminRightsEditActivity.EXTRA_IS_OWNER, false)
            putExtra(AdminRightsEditActivity.EXTRA_CAN_EDIT, true)
        }
        ActivityScenario.launch<AdminRightsEditActivity>(rightsEditIntent).use {
            assertNotNull(it)
        }
        ActivityScenario.launch<SystemMonitorActivity>(Intent(context, SystemMonitorActivity::class.java)).use {
            assertNotNull(it)
        }
        ActivityScenario.launch<AdminAnalyticsActivity>(Intent(context, AdminAnalyticsActivity::class.java)).use {
            assertNotNull(it)
        }
        ActivityScenario.launch<SendNotificationActivity>(Intent(context, SendNotificationActivity::class.java)).use {
            assertNotNull(it)
        }
        ActivityScenario.launch<GeneratedTestsActivity>(Intent(context, GeneratedTestsActivity::class.java)).use {
            assertNotNull(it)
        }
        ActivityScenario.launch<ManagePollsActivity>(Intent(context, ManagePollsActivity::class.java)).use {
            assertNotNull(it)
        }
        ActivityScenario.launch<EditAboutActivity>(Intent(context, EditAboutActivity::class.java)).use {
            assertNotNull(it)
        }
        ActivityScenario.launch<FeedbackMessagesActivity>(Intent(context, FeedbackMessagesActivity::class.java)).use {
            assertNotNull(it)
        }
        ActivityScenario.launch<ManageExistingExamsActivity>(Intent(context, ManageExistingExamsActivity::class.java)).use {
            assertNotNull(it)
        }
        val editExamIntent = Intent(context, EditExamActivity::class.java).apply {
            putExtra(Constants.EXTRA_EXAM_ID, "fixture_exam")
            putExtra(Constants.EXTRA_EXAM_NAME, "SSC CGL")
            putExtra(Constants.EXTRA_EXAM_CATEGORY, "SSC")
        }
        ActivityScenario.launch<EditExamActivity>(editExamIntent).use {
            assertNotNull(it)
        }
        ActivityScenario.launch<ManageUsersActivity>(Intent(context, ManageUsersActivity::class.java)).use {
            assertNotNull(it)
        }
        ActivityScenario.launch<AppConfigActivity>(Intent(context, AppConfigActivity::class.java)).use {
            assertNotNull(it)
        }
        ActivityScenario.launch<ActivityLogActivity>(Intent(context, ActivityLogActivity::class.java)).use {
            assertNotNull(it)
        }
        ActivityScenario.launch<EmojiStudioActivity>(Intent(context, EmojiStudioActivity::class.java)).use {
            assertNotNull(it)
        }
        ActivityScenario.launch<FlaggedQuestionsActivity>(Intent(context, FlaggedQuestionsActivity::class.java)).use {
            assertNotNull(it)
        }
        ActivityScenario.launch<ApiUsageActivity>(Intent(context, ApiUsageActivity::class.java)).use {
            assertNotNull(it)
        }
        ActivityScenario.launch<ManageSyllabusActivity>(Intent(context, ManageSyllabusActivity::class.java)).use {
            assertNotNull(it)
        }
        ActivityScenario.launch<ManagePremiumActivity>(Intent(context, ManagePremiumActivity::class.java)).use {
            assertNotNull(it)
        }
    }
}
