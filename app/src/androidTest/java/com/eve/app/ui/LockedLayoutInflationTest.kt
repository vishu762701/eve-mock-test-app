package com.eve.app.ui

import android.content.res.Configuration
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.test.platform.app.InstrumentationRegistry
import com.eve.app.R
import org.junit.Assert.assertNotNull
import org.junit.Test

/** Every app-owned layout must inflate with the production theme in both modes. */
class LockedLayoutInflationTest {
    @Test fun allProductionLayoutsInflateInBothThemes() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.runOnMainSync {
            for (night in listOf(Configuration.UI_MODE_NIGHT_NO, Configuration.UI_MODE_NIGHT_YES)) {
                val base = instrumentation.targetContext
                val config = Configuration(base.resources.configuration).apply {
                    uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or night
                }
                val context = ContextThemeWrapper(base.createConfigurationContext(config), R.style.Theme_Eve)
                val inflater = LayoutInflater.from(context)
                val layouts = listOf(
                    R.layout.activity_about,
                    R.layout.activity_activity_log,
                    R.layout.activity_admin,
                    R.layout.activity_admin_analytics,
                    R.layout.activity_api_usage,
                    R.layout.activity_app_config,
                    R.layout.activity_bookmarks,
                    R.layout.activity_content_display,
                    R.layout.activity_edit_about,
                    R.layout.activity_edit_exam,
                    R.layout.activity_exam_tests,
                    R.layout.activity_feedback,
                    R.layout.activity_feedback_messages,
                    R.layout.activity_flagged_questions,
                    R.layout.activity_generated_tests,
                    R.layout.activity_history,
                    R.layout.activity_leaderboard,
                    R.layout.activity_login,
                    R.layout.activity_main,
                    R.layout.activity_manage_exams,
                    R.layout.activity_manage_existing_exams,
                    R.layout.activity_manage_polls,
                    R.layout.activity_manage_premium,
                    R.layout.activity_manage_syllabus,
                    R.layout.activity_manage_users,
                    R.layout.activity_mistakes,
                    R.layout.activity_notifications,
                    R.layout.activity_payment_checkout,
                    R.layout.activity_performance,
                    R.layout.activity_practice,
                    R.layout.activity_premium,
                    R.layout.activity_profile,
                    R.layout.activity_pyq,
                    R.layout.activity_result,
                    R.layout.activity_result_detail,
                    R.layout.activity_send_notification,
                    R.layout.activity_settings,
                    R.layout.activity_syllabus,
                    R.layout.activity_system_monitor,
                    R.layout.activity_test,
                    R.layout.bottom_sheet_category_picker,
                    R.layout.bottom_sheet_completed_exam,
                    R.layout.bottom_sheet_payment_method,
                    R.layout.bottom_sheet_target_exams,
                    R.layout.dialog_add_question,
                    R.layout.dialog_create_feedback_post,
                    R.layout.dialog_create_poll,
                    R.layout.dialog_edit_question_details,
                    R.layout.dialog_manage_banners,
                    R.layout.dialog_manual_grant_premium,
                    R.layout.dialog_post_replies,
                    R.layout.dialog_reply_feedback,
                    R.layout.dialog_report_question,
                    R.layout.item_admin_banner,
                    R.layout.item_admin_email,
                    R.layout.item_admin_premium_user,
                    R.layout.item_admin_syllabus,
                    R.layout.item_admin_transaction,
                    R.layout.item_analytics_exam,
                    R.layout.item_analytics_question,
                    R.layout.item_answer,
                    R.layout.item_audit_log,
                    R.layout.item_bookmark_card,
                    R.layout.item_category_header,
                    R.layout.item_exam,
                    R.layout.item_exam_dropdown,
                    R.layout.item_exam_test,
                    R.layout.item_existing_exam_admin,
                    R.layout.item_feedback_message,
                    R.layout.item_feedback_post,
                    R.layout.item_flagged_question,
                    R.layout.item_generated_test,
                    R.layout.item_history,
                    R.layout.item_home_banner,
                    R.layout.item_leaderboard,
                    R.layout.item_manage_question,
                    R.layout.item_mistake,
                    R.layout.item_notification,
                    R.layout.item_palette_circle,
                    R.layout.item_poll_admin,
                    R.layout.item_poll_option_input,
                    R.layout.item_poll_option_result,
                    R.layout.item_post_reply,
                    R.layout.item_question,
                    R.layout.item_result_topic,
                    R.layout.item_sent_broadcast,
                    R.layout.item_student_action,
                    R.layout.item_syllabus,
                    R.layout.item_topic_stat,
                    R.layout.item_user_admin,
                    R.layout.layout_bulletin,
                    R.layout.layout_undo_bar,
                    R.layout.popup_telegram_menu,
                    R.layout.view_empty_state,
                    R.layout.view_error_state
                )
                for (layout in layouts) assertNotNull(context.resources.getResourceName(layout), inflater.inflate(layout, FrameLayout(context), false))
            }
        }
    }
}
