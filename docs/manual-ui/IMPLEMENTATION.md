# Manual UI implementation evidence

## Baseline and component mapping

Fetched clean `origin/main` at `5c4909f613da55415225880844f862fed7c92339`.
No nested AGENTS.md files exist. The current user specifications supersede the
older numeric approvals in APPROVED_UI_IMPLEMENTATION_AUDIT.md.

The native Result Overview metric component is `rowOverviewStatisticsTiles`
(`ResultStatisticsGrid`), containing the bound Right, Wrong, Unattempted, and
Accuracy values (`tvCorrectCount`, `tvWrongCount`, `tvUnattemptedCount`, `tvAccuracy`).
Its previous runtime code explicitly imposed 116×96dp tiles, with shared semantic
shape backgrounds, not 158×116dp MaterialCardViews. The role matches metric cards;
the reference dimensions do not match this source. The full-width 28dp Performance
Standing card remains full width with unchanged geometry. No metrics section added.

## Coordinated geometry

| Requirement | Production files | Implementation |
|---|---|---|
| Home banner | HomeAppearance.kt, activity_main.xml, dimens.xml | Width = viewport − 32dp; 160dp high; uniform 16dp clipped Outline; zero stroke. Existing 16dp horizontal margins retained, 6dp top/bottom margins. Full-bleed pager/image/blur/matte remain unpadded. CTA overlay layer has 12dp padding, CTA text 15sp. Only one overlay item exists, so no artificial 12dp item gap or duplicate item was introduced. |
| Home pills | HomeAppearance.kt, HomeCategoryChipGroup.kt, activity_main.xml | 65dp nominal minimum width, 40dp nominal minimum height; 71dp requested corner size resolves to capsule geometry. 8dp start/end content inset, 13sp text, original 8dp group gap and 16dp margins. Content-driven width capped at viewport − 32dp, end ellipsis with full accessible label; height grows with scaled font metrics. Parent supplies 48dp vertical touch delegates and accessibility regions. |
| Mock options | TelegramRadioButton.kt; existing item_question.xml unchanged | 16dp corners for checked/unchecked backgrounds, zero stroke. Existing width viewport − 32dp, minHeight 56dp, 14sp text, 10dp gaps retained. 12dp edges; indicator+gap adds 32dp to start text inset, total 44dp. Relative padding and mirrored indicator support RTL. Admin option surfaces retain their geometry and receive the global existing-border rule. |
| Result track | activity_result.xml, bg_result_segmented_track.xml, bg_result_segmented_indicator.xml, styles.xml, ResultTabs.kt | Width viewport − 32dp; 44dp nominal height; requested 30dp corners physically capped to capsule. Zero stroke. Track padding 4dp; 3dp indicator horizontal insets yield 6dp visual gap without breaking equal segment distribution. 13sp custom native labels preserve font size instead of Material auto-shrinking. Two-line/ellipsis labels and content-based minimum track height for enlarged fonts. Original elastic indicator, tab ordering and callbacks retained. |
| Overview metrics | activity_result.xml, ResultStatisticsGrid.kt, bg_result_metric_{right,wrong,unattempted,medium}.xml | Each card 170dp width, 116dp minimum height (exact at ordinary text), 35dp corners, 1dp border, 14dp padding, all values/labels 12sp. 12dp inter-card gaps, original 16dp screen inset. One column below 384dp viewport, two at 393/412dp, up to four at tablet width. No silent width shrinking; content may grow vertically. |

## Border classification

Dedicated `eve_shape_border` resolves to **#FF000000** Day and **#FFFFFFFF** Night.
Existing visible decorative strokes now use 1dp; integer-pixel APIs round density
once and clamp to at least one pixel. Shared color tokens and the shared hairline
dimension are unchanged, preserving their separator/icon/text/graphic uses.

### XML shape inventory (before → after)

| File | Baseline stroke | Classification / result |
|---|---|---|
| `app/src/main/res/drawable/bg_active_context_badge.xml` | 1dp / @color/eve_stroke | 1dp / `eve_shape_border` |
| `app/src/main/res/drawable/bg_btn_report.xml` | 1dp / @color/eve_status_warning_stroke | 1dp / `eve_shape_border` |
| `app/src/main/res/drawable/bg_category_chip.xml` | @dimen/eve_stroke_hairline / @color/eve_separator | 1dp / `eve_shape_border` |
| `app/src/main/res/drawable/bg_category_dropdown_pill.xml` | @dimen/eve_stroke_hairline / @color/eve_separator | 1dp / `eve_shape_border` |
| `app/src/main/res/drawable/bg_circle_badge.xml` | 1dp / @color/eve_stroke | 1dp / `eve_shape_border` |
| `app/src/main/res/drawable/bg_circle_translucent.xml` | @dimen/eve_stroke_hairline / @color/eve_separator | 1dp / `eve_shape_border` |
| `app/src/main/res/drawable/bg_dialog_liquid_glass.xml` | @dimen/eve_stroke_hairline / @color/eve_separator | 1dp / `eve_shape_border` |
| `app/src/main/res/drawable/bg_dot_red.xml` | 1.5dp / @color/eve_surface | Preserved: notification/state marker halo |
| `app/src/main/res/drawable/bg_drawer_glass.xml` | 1dp / @color/eve_drawer_glass_border | 1dp / `eve_shape_border` |
| `app/src/main/res/drawable/bg_edit_text_rounded.xml` | @dimen/eve_stroke_hairline / @color/eve_separator | 1dp / `eve_shape_border` |
| `app/src/main/res/drawable/bg_option_badge_default.xml` | 1dp / @color/eve_stroke | 1dp / `eve_shape_border` |
| `app/src/main/res/drawable/bg_option_review_correct.xml` | 1.5dp / @color/eve_tile_right_border | 1dp / `eve_shape_border` |
| `app/src/main/res/drawable/bg_option_review_default.xml` | 1dp / @color/eve_border | 1dp / `eve_shape_border` |
| `app/src/main/res/drawable/bg_option_review_selected.xml` | 1.5dp / @color/eve_stroke | 1dp / `eve_shape_border` |
| `app/src/main/res/drawable/bg_option_review_wrong.xml` | 1.5dp / @color/eve_tile_wrong_border | 1dp / `eve_shape_border` |
| `app/src/main/res/drawable/bg_popup_menu.xml` | 0.5dp / @color/eve_separator | 1dp / `eve_shape_border` |
| `app/src/main/res/drawable/bg_question_timer_chip.xml` | @dimen/eve_stroke_hairline / @color/eve_separator | 1dp / `eve_shape_border` |
| `app/src/main/res/drawable/bg_search_field.xml` | @dimen/eve_stroke_hairline / @color/eve_input_stroke | 1dp / `eve_shape_border` |
| `app/src/main/res/drawable/bg_sheet_bottom.xml` | @dimen/eve_stroke_hairline / @color/eve_separator | 1dp / `eve_shape_border` |
| `app/src/main/res/drawable/bg_soft_pill.xml` | @dimen/eve_stroke_hairline / @color/eve_separator | 1dp / `eve_shape_border` |
| `app/src/main/res/drawable/bg_stat_mini.xml` | @dimen/eve_stroke_hairline / @color/eve_separator | 1dp / `eve_shape_border` |
| `app/src/main/res/drawable/bg_streak_pill.xml` | @dimen/eve_stroke_hairline / @color/eve_separator | 1dp / `eve_shape_border` |
| `app/src/main/res/drawable/bg_tab_segmented_indicator.xml` | 0.5dp / @color/eve_fill_tertiary | 1dp / `eve_shape_border` |
| `app/src/main/res/drawable/bg_tab_segmented_track.xml` | @dimen/eve_stroke_hairline / @color/eve_separator | 1dp / `eve_shape_border` |
| `app/src/main/res/drawable/bg_tile_medium.xml` | @dimen/eve_stroke_hairline / @color/eve_tile_medium_border | 1dp / `eve_shape_border` |
| `app/src/main/res/drawable/bg_tile_right.xml` | @dimen/eve_stroke_hairline / @color/eve_tile_right_border | 1dp / `eve_shape_border` |
| `app/src/main/res/drawable/bg_tile_unattempted.xml` | @dimen/eve_stroke_hairline / @color/eve_separator | 1dp / `eve_shape_border` |
| `app/src/main/res/drawable/bg_tile_wrong.xml` | @dimen/eve_stroke_hairline / @color/eve_tile_wrong_border | 1dp / `eve_shape_border` |

### Styles, inheritance and runtime outlines

- `styles.xml`: existing outlined/orange/delete buttons, cards, semantic tiles,
  cutoff/category chips. `approved_ui_styles.xml`: Clear/Review/Previous borders.
  Next/Submit keeps its explicit 0dp override.
- Both themes select `Widget.Eve.AssistChip`, inheriting Material 1.12's existing
  1dp Assist outline and replacing only its outline color. Runtime Practice/PYQ
  chips receive the standard through their existing theme inheritance.
- `HomeAppearance`, `TargetExamsBottomSheet`, `ExamAdapter`, `QuestionPaletteAdapter`,
  `LeaderboardAdapter`, `FeedbackMessagesAdapter`, `MistakesAdapter`: checked,
  active, learned, unread, and recycled outlines standardized, fills/labels retained.
- `TelegramRadioButton`: existing Admin surfaces only. Approved Mock mode replaces
  these with its separate zero-stroke StateListDrawable as before.
- `EveLiquidGlassView`: existing custom rim becomes 1dp opaque black/white; inset
  path prevents losing half the stroke to clipping. Optical highlight remains
  within the rim so it cannot tint the border.
- Explicit Material outlined inputs get 1dp normal/focused strokes and dedicated
  normal/error colors through `selector_shape_border.xml`. Material requires a
  stateful selector to replace default/disabled/hovered colors, not just focus. Filled inputs' bottom underline is a separator and stays
  unchanged. Error text/icons/fills retain their semantics.

### Layout coverage (existing decorative borders only)

- `app/src/main/res/layout/activity_admin.xml`
- `app/src/main/res/layout/activity_admin_analytics.xml`
- `app/src/main/res/layout/activity_api_usage.xml`
- `app/src/main/res/layout/activity_app_config.xml`
- `app/src/main/res/layout/activity_content_display.xml`
- `app/src/main/res/layout/activity_edit_exam.xml`
- `app/src/main/res/layout/activity_feedback.xml`
- `app/src/main/res/layout/activity_leaderboard.xml`
- `app/src/main/res/layout/activity_main.xml`
- `app/src/main/res/layout/activity_manage_exams.xml`
- `app/src/main/res/layout/activity_manage_existing_exams.xml`
- `app/src/main/res/layout/activity_manage_premium.xml`
- `app/src/main/res/layout/activity_manage_syllabus.xml`
- `app/src/main/res/layout/activity_manage_users.xml`
- `app/src/main/res/layout/activity_payment_checkout.xml`
- `app/src/main/res/layout/activity_premium.xml`
- `app/src/main/res/layout/activity_profile.xml`
- `app/src/main/res/layout/activity_result.xml`
- `app/src/main/res/layout/activity_result_detail.xml`
- `app/src/main/res/layout/activity_settings.xml`
- `app/src/main/res/layout/bottom_sheet_payment_method.xml`
- `app/src/main/res/layout/dialog_add_question.xml`
- `app/src/main/res/layout/dialog_create_feedback_post.xml`
- `app/src/main/res/layout/dialog_edit_question_details.xml`
- `app/src/main/res/layout/dialog_manage_banners.xml`
- `app/src/main/res/layout/dialog_manual_grant_premium.xml`
- `app/src/main/res/layout/dialog_reply_feedback.xml`
- `app/src/main/res/layout/dialog_report_question.xml`
- `app/src/main/res/layout/item_admin_banner.xml`
- `app/src/main/res/layout/item_admin_premium_user.xml`
- `app/src/main/res/layout/item_admin_syllabus.xml`
- `app/src/main/res/layout/item_admin_transaction.xml`
- `app/src/main/res/layout/item_analytics_exam.xml`
- `app/src/main/res/layout/item_answer.xml`
- `app/src/main/res/layout/item_audit_log.xml`
- `app/src/main/res/layout/item_bookmark_card.xml`
- `app/src/main/res/layout/item_exam.xml`
- `app/src/main/res/layout/item_existing_exam_admin.xml`
- `app/src/main/res/layout/item_feedback_message.xml`
- `app/src/main/res/layout/item_feedback_post.xml`
- `app/src/main/res/layout/item_flagged_question.xml`
- `app/src/main/res/layout/item_generated_test.xml`
- `app/src/main/res/layout/item_mistake.xml`
- `app/src/main/res/layout/item_palette_circle.xml`
- `app/src/main/res/layout/item_poll_admin.xml`
- `app/src/main/res/layout/item_post_reply.xml`
- `app/src/main/res/layout/item_sent_broadcast.xml`
- `app/src/main/res/layout/item_syllabus.xml`
- `app/src/main/res/layout/item_user_admin.xml`
- `app/src/main/res/layout/layout_bulletin.xml`
- `app/src/main/res/layout/layout_undo_bar.xml`
- `app/src/main/res/layout/popup_telegram_menu.xml`

### Intentional exclusions

Home banner, approved Mock option surfaces, Result track and selected Result pill,
Next/Submit, hidden floating-airplane zero-stroke card, all other explicit zero
strokes; vector/Lottie artwork, radio rings/checks, timer/countdown/gauge arcs,
chart lines, dividers (including EveGlassToolbar bottom separator), shimmer/loading,
notification/status dot halo, ripples, shadows/blur, and optical highlights.
`ShareCardHelper` generates fixed-resolution shareable bitmap artwork, not an
Android UI container; its pixels are not dp geometry and remain unchanged.

## Verification and limitations

See the final execution report for actual local/CI results. Added JVM inventory
regressions and native production-inflation tests covering widths 320/360/393/412/
800dp, font scales 1/1.5/2, both themes and Day→Night→Day reinflation, option states,
metric columns, inherited outlines, and outlined Admin inputs. Existing rendering
fixtures now capture at 360dp and superseded radius/border assertions are updated.
No backend/authentication/scoring/API files changed.

Native fixture renderings are not authenticated live-data screen captures.
Network banner images, real hardware blur and live backend end-to-end flows require
manual device verification. The repository's existing Activity recreation/theme
reveal tests continue to exercise their previous state-restoration coverage.

## Local quality gate results

`./gradlew testDebugUnitTest assembleDebug assembleDebugAndroidTest lintDebug`
PASS using Gradle 8.7, JDK 21 and SDK 36. 206 JVM tests, zero failures/errors.
`git diff --check` PASS. All Android resource XML parses. A structural comparison
confirmed 50 changed layouts outside Home/Result change only classified outline
attributes and retain their hierarchy. No background-plus-Material duplicate
outlines found. Full diff reviewed.

The workspace had only a Java runtime and no SDK/Gradle or KVM. A full JDK and
SDK/Gradle were installed locally; the JDK uses the environment's trusted CA
store. Initial missing-jlink/TLS/tooling failures were repaired. A restricted
Material layout override exposed by lint was replaced with public layout callbacks;
no assertions or lint checks suppressed. Native execution follows the push in
the existing API 35 gesture/three-button GitHub Actions jobs. Final delivery and
CI results are reported in the execution response.

## Native verification repair

The first CI run compiled and passed build/lint/JVM/backend gates, but both native
suites exposed two fixture assumptions: the new 360dp/420dpi navigation row has
an odd available pixel count (414/415px weighted split), and an unattached View
cannot synchronously execute its posted click callback. The width assertion now
checks exact row conservation and a balanced integer allocation at the mandated
viewport. The click assertion is retained in an attached existing debug Activity
host and waits for the real UI queue. Production behavior was not changed to
accommodate either test. Subsequent native results are in the execution response.

The next native run passed the attached touch-target check and all existing
functional/reveal/rendering tests, then exposed an XML measurement detail in the
new matrix: GradientDrawable inflates its 35dp radius through pixel-size rounding
(92px at 420dpi), unlike Kotlin's float radius (91.875px). The assertion now checks
the exact native XML dp conversion. Added attached Home hardware screenshots and
text-ink assertions after a committed frame; software Canvas captures alone cannot
establish centered horizontally scrolling Chip text before a real pre-draw.

The full matrix and attached Home text check passed in both navigation modes;
three-button passed all 25 tests. Gesture exposed an existing fixture race: the
three-button-only SystemUI `cmd uimode` workaround also ran in gesture mode and
asynchronously recreated the source Activity during app capture, correctly causing
`source_stopped_during_preparation` cancellation. Restricted that workaround to
its documented navigation mode and wait for system accessibility/UI idle before
starting app capture there. All state/reveal/system-bar assertions remain intact;
production theme code is unchanged. Final rerun results are in the execution report.

A final View-alpha audit found two additional disabled outlined controls: Admin
reply Mark Read (`PostRepliesAdapter`) and the exam time picker
(`ManageExamsActivity`). Their 0.5 View alpha would fade an otherwise opaque
stroke. Both now retain alpha 1 with the same half-opacity disabled label instead;
enabled/click behavior remains unchanged. Native reply holder tests exercise
read→unread→read recycling and assert 1dp opaque outlines with dimmed labels.
Borderless download/reorder/delete-icon alpha and transition fades remain unchanged.

Material 1.12 dependency inspection additionally identified `MaterialSwitch`'s
unchecked track decoration: a 2dp vector path outline, tinted gray/translucent
when disabled. This is a control border rather than icon artwork. Both themes
now inherit `Widget.Eve.MaterialSwitch`, retaining its exact 52×32dp path, native
thumb/track fills and animation, replacing only outline width with 1dp and
unchecked/disabled tint with `eve_shape_border`. Checked outlines stay transparent
as in the library baseline. `eve_switch_track_outline.xml` and
`selector_switch_outline.xml` are covered by JVM inventory and native inherited
checked/unchecked/enabled/disabled assertions. Covered MaterialSwitch users:
Admin, App Config, Manage Premium and manual premium grant dialog. Legacy
SwitchMaterial artwork has no such decorative outline and remains unchanged.

The gesture suite passed after narrowing the SystemUI workaround. Three-button
still exposed the same source-recreation race despite accessibility-idle waiting:
UiModeManager configuration propagation can outlive that idle point. The workaround
now runs after the app reveal has settled and before verifying/capturing system-bar
appearance. It cannot replace the source during app capture, and all original
reveal, retained-answer, overlay cleanup and bar-appearance assertions remain.

Visual inspection of the Leaderboard-selected reference capture exposed a genuine
padding regression: Material 1.12 TabLayout.onMeasure forces its fixed strip to
the outer width, ignoring horizontal padding, clipping the last pill. The existing
`EveGlassTabLayout` (used exclusively by Result) now remeasures its fixed strip to
width minus horizontal padding after Material measurement. Same ID, clipping,
press compression, haptics and elastic indicator are retained. Added actual strip
bounds and selected right-corner pixel assertions for all three tabs in both themes,
plus the complete responsive matrix. This preserves the full 4dp track inset
without horizontal overflow.
