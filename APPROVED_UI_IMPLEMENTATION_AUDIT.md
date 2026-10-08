# Approved UI implementation audit

Source of approval: `EVE_MASTER_DESIGN_HANDOFF.md` and the user's final numerical
specifications. Home and Mock settings and the Result segmented tabs are approved.
Other Result card styling and Full Leaderboard redesigns remain unapproved.

Repository: https://github.com/vishu762701/eve-mock-test-app.git
Original remote main: `cc47908aba6cbf55d27680749cc3223c866a8621`.
Previous unmerged implementation: `299dbd1d5dcfa4332956b913b3ce3b268af8b8c4`.
Local main safely fast-forwarded through that work; no branch or PR created for this task.
No pre-existing uncommitted changes were present. `AGENTS.md` records the user-required
permanent direct-main workflow.

## Verification scope

Native tests inflate **production XML**, call the **production adapters/custom views**,
and use the same Home presentation helper as MainActivity. They inspect resolved
values after theme inflation and binding, lay out views, check selected-pill and timer
pixels, and export native Canvas bitmap renderings in both themes. Fixtures contain
no production user data. These are rendered native layout fixtures, not screenshots
of authenticated live-data activities. Home banner image/network content and hardware
blur are not exercised. Full-window/system-bar rendering, live Firebase authentication,
and production network end-to-end flows are not claimed as visually verified.

At initial commit, native execution is pending the API 35 GitHub Actions emulator:
this workspace has no device/emulator or `/dev/kvm`. Rows explicitly stay UNVERIFIED
until actual native results are available. Static values have been traced through
XML/styles/drawables, custom Paint drawing, adapters, theme qualifiers, and runtime
handlers. Result card styling and the native Result review palette are isolated from
Mock Test changes. Next/Submit has its own zero-stroke style.

| Screen | Component | Required value | Actual implemented value | Source file and line | Dark Theme status | Light Theme status | Verification method | PASS / FAIL / UNVERIFIED |
|---|---|---|---|---|---|---|---|---|
| Home | Screen | Dark #000000; Light #FFFFFF | Dark #000000; Light #FFFFFF | `app/src/main/res/layout/activity_main.xml:14` | UNVERIFIED (CI pending) | UNVERIFIED (CI pending) | Native view assertions + rendered bitmap; pending CI | UNVERIFIED |
| Home | Banner corner radius | 18dp | 18dp | `app/src/main/java/com/eve/app/ui/home/HomeAppearance.kt:18` | UNVERIFIED (CI pending) | UNVERIFIED (CI pending) | Native view assertions + rendered bitmap; pending CI | UNVERIFIED |
| Home | All / Other adjacency | All, Other, then remaining categories | All, Other, then remaining categories | `app/src/main/java/com/eve/app/ui/home/HomeAppearance.kt:15` | UNVERIFIED (CI pending) | UNVERIFIED (CI pending) | Native view assertions + rendered bitmap; pending CI | UNVERIFIED |
| Home | Category pill radius | 50dp | 50dp | `app/src/main/java/com/eve/app/ui/home/HomeAppearance.kt:32` | UNVERIFIED (CI pending) | UNVERIFIED (CI pending) | Native view assertions + rendered bitmap; pending CI | UNVERIFIED |
| Home | Exam card radius | 8dp | 8dp | `app/src/main/res/layout/item_exam.xml:13` | UNVERIFIED (CI pending) | UNVERIFIED (CI pending) | Native view assertions + rendered bitmap; pending CI | UNVERIFIED |
| Home | Search bare icon | No visible background / border; 48dp touch target; callbacks retained | No visible background / border; 48dp touch target; callbacks retained | `app/src/main/res/layout/activity_main.xml:59` | UNVERIFIED (CI pending) | UNVERIFIED (CI pending) | Native view assertions + rendered bitmap; pending CI | UNVERIFIED |
| Home | Notification bare icon | No visible background / border; 48dp touch target; callbacks retained | No visible background / border; 48dp touch target; callbacks retained | `app/src/main/res/layout/activity_main.xml:79` | UNVERIFIED (CI pending) | UNVERIFIED (CI pending) | Native view assertions + rendered bitmap; pending CI | UNVERIFIED |
| Home | Overflow bare icon | No visible background / border; 48dp touch target; callbacks retained | No visible background / border; 48dp touch target; callbacks retained | `app/src/main/res/layout/activity_main.xml:104` | UNVERIFIED (CI pending) | UNVERIFIED (CI pending) | Native view assertions + rendered bitmap; pending CI | UNVERIFIED |
| Home | Profile circle unchanged | Original 40dp ShapeCircle, 8dp padding, background retained | Original 40dp ShapeCircle, 8dp padding, background retained | `app/src/main/res/layout/activity_main.xml:42` | UNVERIFIED (CI pending) | UNVERIFIED (CI pending) | Native view assertions + rendered bitmap; pending CI | UNVERIFIED |
| Home | Airplane hidden | GONE in XML and runtime | GONE in XML and runtime | `app/src/main/res/layout/activity_main.xml:424`; `app/src/main/java/com/eve/app/ui/home/MainActivity.kt:812` | UNVERIFIED (CI pending) | UNVERIFIED (CI pending) | Native view assertions + rendered bitmap; pending CI | UNVERIFIED |
| Mock Test | Screen background | Dark #000000; Light #FFFFFF | Dark #000000; Light #FFFFFF | `app/src/main/res/layout/activity_test.xml:7` | UNVERIFIED (CI pending) | UNVERIFIED (CI pending) | Native view assertions + rendered bitmap; pending CI | UNVERIFIED |
| Mock Test | Question background | None | None | `app/src/main/res/layout/item_question.xml:109` | UNVERIFIED (CI pending) | UNVERIFIED (CI pending) | Native view assertions + rendered bitmap; pending CI | UNVERIFIED |
| Mock Test | Option background | #111111 dark / #F3F3F5 light | #111111 dark / #F3F3F5 light | `app/src/main/res/values/approved_ui_colors.xml:4`; `app/src/main/res/values-night/approved_ui_colors.xml:4` | UNVERIFIED (CI pending) | UNVERIFIED (CI pending) | Native view assertions + rendered bitmap; pending CI | UNVERIFIED |
| Mock Test | Action/navigation background | #FFFFFF dark / #000000 light | #FFFFFF dark / #000000 light | `app/src/main/res/values/approved_ui_colors.xml:7`; `app/src/main/res/values-night/approved_ui_colors.xml:7` | UNVERIFIED (CI pending) | UNVERIFIED (CI pending) | Native view assertions + rendered bitmap; pending CI | UNVERIFIED |
| Mock Test | Action/navigation foreground | #000000 dark / #FFFFFF light | #000000 dark / #FFFFFF light | `app/src/main/res/values/approved_ui_colors.xml:8`; `app/src/main/res/values-night/approved_ui_colors.xml:8` | UNVERIFIED (CI pending) | UNVERIFIED (CI pending) | Native view assertions + rendered bitmap; pending CI | UNVERIFIED |
| Mock Test | Palette background | #000000 dark / #EEEEF0 light | #000000 dark / #EEEEF0 light | `app/src/main/res/values/approved_ui_colors.xml:9`; `app/src/main/res/values-night/approved_ui_colors.xml:9` | UNVERIFIED (CI pending) | UNVERIFIED (CI pending) | Native view assertions + rendered bitmap; pending CI | UNVERIFIED |
| Mock Test | Selected answer | #34C759 both themes | #34C759 both themes | `app/src/main/res/values/approved_ui_colors.xml:5`; `app/src/main/res/values-night/approved_ui_colors.xml:5` | UNVERIFIED (CI pending) | UNVERIFIED (CI pending) | Native view assertions + rendered bitmap; pending CI | UNVERIFIED |
| Mock Test | Unselected answer | #111111 dark / #F3F3F5 light | #111111 dark / #F3F3F5 light | `app/src/main/res/values/approved_ui_colors.xml:4`; `app/src/main/res/values-night/approved_ui_colors.xml:4` | UNVERIFIED (CI pending) | UNVERIFIED (CI pending) | Native view assertions + rendered bitmap; pending CI | UNVERIFIED |
| Mock Test | Marked for review | #FFCC00 both themes | #FFCC00 both themes | `app/src/main/res/values/approved_ui_colors.xml:6`; `app/src/main/res/values-night/approved_ui_colors.xml:6` | UNVERIFIED (CI pending) | UNVERIFIED (CI pending) | Native view assertions + rendered bitmap; pending CI | UNVERIFIED |
| Mock Test | Timer ring | #FFFFFF dark / #000000 light | #FFFFFF dark / #000000 light | `app/src/main/res/values/approved_ui_colors.xml:3`; `app/src/main/res/values-night/approved_ui_colors.xml:3` | UNVERIFIED (CI pending) | UNVERIFIED (CI pending) | Native view assertions + rendered bitmap; pending CI | UNVERIFIED |
| Mock Test | Answered palette | #34C759 both themes | #34C759 both themes | `app/src/main/res/values/approved_ui_colors.xml:5`; `app/src/main/res/values-night/approved_ui_colors.xml:5` | UNVERIFIED (CI pending) | UNVERIFIED (CI pending) | Native view assertions + rendered bitmap; pending CI | UNVERIFIED |
| Mock Test | Review palette | #FFCC00 both themes | #FFCC00 both themes | `app/src/main/res/values/approved_ui_colors.xml:6`; `app/src/main/res/values-night/approved_ui_colors.xml:6` | UNVERIFIED (CI pending) | UNVERIFIED (CI pending) | Native view assertions + rendered bitmap; pending CI | UNVERIFIED |
| Mock Test | Timer background | #000000 dark / #FFFFFF light | #000000 dark / #FFFFFF light | `app/src/main/java/com/eve/app/ui/common/CircularTimerView.kt:79` | UNVERIFIED (CI pending) | UNVERIFIED (CI pending) | Native view assertions + rendered bitmap; pending CI | UNVERIFIED |
| Mock Test | Timer circle | Equal 44dp width/height | Equal 44dp width/height | `app/src/main/java/com/eve/app/ui/common/CircularTimerView.kt:68` | UNVERIFIED (CI pending) | UNVERIFIED (CI pending) | Native view assertions + rendered bitmap; pending CI | UNVERIFIED |
| Mock Test | Palette circles | Equal 38dp width/height; 19dp radius | Equal 38dp width/height; 19dp radius | `app/src/main/java/com/eve/app/ui/common/QuestionPaletteAdapter.kt:124` | UNVERIFIED (CI pending) | UNVERIFIED (CI pending) | Native view assertions + rendered bitmap; pending CI | UNVERIFIED |
| Mock Test | Question shape | No visible background / border | No visible background / border | `app/src/main/res/layout/item_question.xml:109` | UNVERIFIED (CI pending) | UNVERIFIED (CI pending) | Native view assertions + rendered bitmap; pending CI | UNVERIFIED |
| Mock Test | Answer shape | Rounded rectangle, 12dp radius | Rounded rectangle, 12dp radius | `app/src/main/java/com/eve/app/ui/common/TelegramRadioButton.kt:124` | UNVERIFIED (CI pending) | UNVERIFIED (CI pending) | Native view assertions + rendered bitmap; pending CI | UNVERIFIED |
| Mock Test | Selection indicator circle | 20dp outer diameter | 20dp outer diameter | `app/src/main/java/com/eve/app/ui/common/TelegramRadioButton.kt:205` | UNVERIFIED (CI pending) | UNVERIFIED (CI pending) | Native view assertions + rendered bitmap; pending CI | UNVERIFIED |
| Mock Test | Bookmark bare icon | No decorative background / border | No decorative background / border | `app/src/main/res/layout/item_question.xml:74` | UNVERIFIED (CI pending) | UNVERIFIED (CI pending) | Native view assertions + rendered bitmap; pending CI | UNVERIFIED |
| Mock Test | Report bare icon | No decorative background / border | No decorative background / border | `app/src/main/res/layout/item_question.xml:93` | UNVERIFIED (CI pending) | UNVERIFIED (CI pending) | Native view assertions + rendered bitmap; pending CI | UNVERIFIED |
| Mock Test | Clear pill | Pill; native MaterialButton corner radius 50dp | Pill; native MaterialButton corner radius 50dp | `app/src/main/res/layout/activity_test.xml:196` | UNVERIFIED (CI pending) | UNVERIFIED (CI pending) | Native view assertions + rendered bitmap; pending CI | UNVERIFIED |
| Mock Test | Review pill | Pill; native MaterialButton corner radius 50dp | Pill; native MaterialButton corner radius 50dp | `app/src/main/res/layout/activity_test.xml:211` | UNVERIFIED (CI pending) | UNVERIFIED (CI pending) | Native view assertions + rendered bitmap; pending CI | UNVERIFIED |
| Mock Test | Previous pill | Pill; native MaterialButton corner radius 50dp | Pill; native MaterialButton corner radius 50dp | `app/src/main/res/layout/activity_test.xml:236` | UNVERIFIED (CI pending) | UNVERIFIED (CI pending) | Native view assertions + rendered bitmap; pending CI | UNVERIFIED |
| Mock Test | Next / Submit pill | Pill; native MaterialButton corner radius 50dp | Pill; native MaterialButton corner radius 50dp | `app/src/main/res/layout/activity_test.xml:251` | UNVERIFIED (CI pending) | UNVERIFIED (CI pending) | Native view assertions + rendered bitmap; pending CI | UNVERIFIED |
| Mock Test | Timer border | 2dp #FFFFFF dark / #000000 light | 2dp #FFFFFF dark / #000000 light | `app/src/main/java/com/eve/app/ui/common/CircularTimerView.kt:29` | UNVERIFIED (CI pending) | UNVERIFIED (CI pending) | Native Paint inspection + ring pixels; pending CI | UNVERIFIED |
| Mock Test | Palette border | 1dp #888888 both themes | 1dp #888888 both themes | `app/src/main/java/com/eve/app/ui/common/QuestionPaletteAdapter.kt:136` | UNVERIFIED (CI pending) | UNVERIFIED (CI pending) | Native MaterialCardView stroke properties; pending CI | UNVERIFIED |
| Mock Test | Question border | 0dp | 0dp | `app/src/main/res/layout/item_question.xml:109` | UNVERIFIED (CI pending) | UNVERIFIED (CI pending) | Native background null assertion; pending CI | UNVERIFIED |
| Mock Test | Option border | 0dp | 0dp | `app/src/main/java/com/eve/app/ui/common/TelegramRadioButton.kt:123` | UNVERIFIED (CI pending) | UNVERIFIED (CI pending) | Drawable source audit + native surface pixels; pending CI | UNVERIFIED |
| Mock Test | Indicator border | 2dp #FFFFFF dark / #000000 light | 2dp #FFFFFF dark / #000000 light | `app/src/main/java/com/eve/app/ui/common/TelegramRadioButton.kt:194` | UNVERIFIED (CI pending) | UNVERIFIED (CI pending) | Native Paint inspection + indicator pixels; pending CI | UNVERIFIED |
| Mock Test | Bookmark border | 0dp | 0dp | `app/src/main/res/layout/item_question.xml:74` | UNVERIFIED (CI pending) | UNVERIFIED (CI pending) | Transparent native background assertion; pending CI | UNVERIFIED |
| Mock Test | Report border | 0dp | 0dp | `app/src/main/res/layout/item_question.xml:93` | UNVERIFIED (CI pending) | UNVERIFIED (CI pending) | Transparent native background assertion; pending CI | UNVERIFIED |
| Mock Test | Clear border | 1dp #888888 | 1dp #888888 | `app/src/main/res/values/approved_ui_styles.xml:22` | UNVERIFIED (CI pending) | UNVERIFIED (CI pending) | Native MaterialButton stroke inspection; pending CI | UNVERIFIED |
| Mock Test | Review border | 1dp #888888 | 1dp #888888 | `app/src/main/res/values/approved_ui_styles.xml:22` | UNVERIFIED (CI pending) | UNVERIFIED (CI pending) | Native MaterialButton stroke inspection; pending CI | UNVERIFIED |
| Mock Test | Previous border | 1dp #888888 | 1dp #888888 | `app/src/main/res/values/approved_ui_styles.xml:22` | UNVERIFIED (CI pending) | UNVERIFIED (CI pending) | Native MaterialButton stroke inspection; pending CI | UNVERIFIED |
| Mock Test | Next / Submit border | 0dp (dedicated Next style) | 0dp (dedicated Next style) | `app/src/main/res/values/approved_ui_styles.xml:29` | UNVERIFIED (CI pending) | UNVERIFIED (CI pending) | Native MaterialButton stroke inspection; pending CI | UNVERIFIED |
| Mock Test | Timer size | 44dp | 44dp | `app/src/main/res/layout/activity_test.xml:28` | UNVERIFIED (CI pending) | UNVERIFIED (CI pending) | Native view assertions + rendered bitmap; pending CI | UNVERIFIED |
| Mock Test | Timer text | 12sp | 12sp | `app/src/main/java/com/eve/app/ui/common/CircularTimerView.kt:46` | UNVERIFIED (CI pending) | UNVERIFIED (CI pending) | Native view assertions + rendered bitmap; pending CI | UNVERIFIED |
| Mock Test | Palette cell size | 38dp | 38dp | `app/src/main/java/com/eve/app/ui/common/QuestionPaletteAdapter.kt:121` | UNVERIFIED (CI pending) | UNVERIFIED (CI pending) | Native view assertions + rendered bitmap; pending CI | UNVERIFIED |
| Mock Test | Palette gap | 8dp (4dp + 4dp) | 8dp (4dp + 4dp) | `app/src/main/java/com/eve/app/ui/common/QuestionPaletteAdapter.kt:118` | UNVERIFIED (CI pending) | UNVERIFIED (CI pending) | Native view assertions + rendered bitmap; pending CI | UNVERIFIED |
| Mock Test | Palette number text | 12sp | 12sp | `app/src/main/java/com/eve/app/ui/common/QuestionPaletteAdapter.kt:125` | UNVERIFIED (CI pending) | UNVERIFIED (CI pending) | Native view assertions + rendered bitmap; pending CI | UNVERIFIED |
| Mock Test | Real palette counts / scrolling | Dynamic test count; no five-row limit | Dynamic test count; no five-row limit | `app/src/main/java/com/eve/app/ui/test/TestActivity.kt:662` | UNVERIFIED (CI pending) | UNVERIFIED (CI pending) | Native 60-question adapter/navigation; source count mapping; pending CI | UNVERIFIED |
| Mock Test | Question font | 16sp | 16sp | `app/src/main/res/layout/item_question.xml:123` | UNVERIFIED (CI pending) | UNVERIFIED (CI pending) | Native view assertions + rendered bitmap; pending CI | UNVERIFIED |
| Mock Test | Question line spacing | 1.5 multiplier | 1.5 multiplier | `app/src/main/res/layout/item_question.xml:120` | UNVERIFIED (CI pending) | UNVERIFIED (CI pending) | Native view assertions + rendered bitmap; pending CI | UNVERIFIED |
| Mock Test | Question gap | 16dp from question wrapper to options; no extra min-height | 16dp from question wrapper to options; no extra min-height | `app/src/main/res/layout/item_question.xml:132` | UNVERIFIED (CI pending) | UNVERIFIED (CI pending) | Native view assertions + rendered bitmap; pending CI | UNVERIFIED |
| Mock Test | Question alignment | Left | Left | `app/src/main/res/layout/item_question.xml:121` | UNVERIFIED (CI pending) | UNVERIFIED (CI pending) | Native view assertions + rendered bitmap; pending CI | UNVERIFIED |
| Mock Test | Option font | 14sp | 14sp | `app/src/main/res/layout/item_question.xml:51` | UNVERIFIED (CI pending) | UNVERIFIED (CI pending) | Native view assertions + rendered bitmap; pending CI | UNVERIFIED |
| Mock Test | Option minimum height | 56dp | 56dp | `app/src/main/res/layout/item_question.xml:149` | UNVERIFIED (CI pending) | UNVERIFIED (CI pending) | Native view assertions + rendered bitmap; pending CI | UNVERIFIED |
| Mock Test | Option gap | 10dp between options; no trailing extra gap | 10dp between options; no trailing extra gap | `app/src/main/res/layout/item_question.xml:145` | UNVERIFIED (CI pending) | UNVERIFIED (CI pending) | Native view assertions + rendered bitmap; pending CI | UNVERIFIED |
| Mock Test | Option content padding | 12dp edge inset; text inset includes 20dp indicator + 12dp gap | 12dp edge inset; text inset includes 20dp indicator + 12dp gap | `app/src/main/java/com/eve/app/ui/common/TelegramRadioButton.kt:137` | UNVERIFIED (CI pending) | UNVERIFIED (CI pending) | Native view assertions + rendered bitmap; pending CI | UNVERIFIED |
| Mock Test | Selection indicator size | 20dp (9dp radius centerline + 1dp half-stroke) | 20dp (9dp radius centerline + 1dp half-stroke) | `app/src/main/java/com/eve/app/ui/common/TelegramRadioButton.kt:205` | UNVERIFIED (CI pending) | UNVERIFIED (CI pending) | Native view assertions + rendered bitmap; pending CI | UNVERIFIED |
| Mock Test | Clear height | 44dp | 44dp | `app/src/main/res/layout/activity_test.xml:196` | UNVERIFIED (CI pending) | UNVERIFIED (CI pending) | Native view assertions + rendered bitmap; pending CI | UNVERIFIED |
| Mock Test | Review height | 44dp | 44dp | `app/src/main/res/layout/activity_test.xml:211` | UNVERIFIED (CI pending) | UNVERIFIED (CI pending) | Native view assertions + rendered bitmap; pending CI | UNVERIFIED |
| Mock Test | Previous height | 48dp | 48dp | `app/src/main/res/layout/activity_test.xml:236` | UNVERIFIED (CI pending) | UNVERIFIED (CI pending) | Native view assertions + rendered bitmap; pending CI | UNVERIFIED |
| Mock Test | Next / Submit height | 48dp | 48dp | `app/src/main/res/layout/activity_test.xml:251` | UNVERIFIED (CI pending) | UNVERIFIED (CI pending) | Native view assertions + rendered bitmap; pending CI | UNVERIFIED |
| Mock Test | Horizontal padding | 16dp for page, button content, bottom bar | 16dp for page, button content, bottom bar | `app/src/main/res/layout/activity_test.xml:172` | UNVERIFIED (CI pending) | UNVERIFIED (CI pending) | Native view assertions + rendered bitmap; pending CI | UNVERIFIED |
| Mock Test | Action gap | 8dp (4 + 4) | 8dp (4 + 4) | `app/src/main/res/layout/activity_test.xml:191` | UNVERIFIED (CI pending) | UNVERIFIED (CI pending) | Native view assertions + rendered bitmap; pending CI | UNVERIFIED |
| Mock Test | Navigation gap | 12dp (6 + 6) | 12dp (6 + 6) | `app/src/main/res/layout/activity_test.xml:231` | UNVERIFIED (CI pending) | UNVERIFIED (CI pending) | Native view assertions + rendered bitmap; pending CI | UNVERIFIED |
| Mock Test | Section gap | 20dp | 20dp | `app/src/main/res/layout/activity_test.xml:225` | UNVERIFIED (CI pending) | UNVERIFIED (CI pending) | Native view assertions + rendered bitmap; pending CI | UNVERIFIED |
| Mock Test | Navigation layout | Equal weights and measured widths | Equal weights and measured widths | `app/src/main/res/layout/activity_test.xml:236` | UNVERIFIED (CI pending) | UNVERIFIED (CI pending) | Native view assertions + rendered bitmap; pending CI | UNVERIFIED |
| Mock Test | Action font | 13sp | 13sp | `app/src/main/res/values/approved_ui_styles.xml:10` | UNVERIFIED (CI pending) | UNVERIFIED (CI pending) | Native view assertions + rendered bitmap; pending CI | UNVERIFIED |
| Mock Test | Navigation font | 14sp | 14sp | `app/src/main/res/values/approved_ui_styles.xml:26` | UNVERIFIED (CI pending) | UNVERIFIED (CI pending) | Native view assertions + rendered bitmap; pending CI | UNVERIFIED |
| Mock Test | No extra shadows / outlines | Zero button/palette elevation; no question/option/Next stroke | Zero button/palette elevation; no question/option/Next stroke | `app/src/main/res/values/approved_ui_styles.xml:19` | UNVERIFIED (CI pending) | UNVERIFIED (CI pending) | Native property assertions + XML/custom drawable audit; pending CI | UNVERIFIED |
| Mock Test | Timer behavior / warnings | Time/progress updates retained; normal arc visible; existing warning color | Time/progress updates retained; normal arc visible; existing warning color | `app/src/main/java/com/eve/app/ui/common/CircularTimerView.kt:91` | UNVERIFIED (CI pending) | UNVERIFIED (CI pending) | Native full/half ring pixels + cumulative timer unit tests; pending CI | UNVERIFIED |
| Mock Test | Answer / Clear / reselect | Saved answer binding silent; clear does not submit; reselection works | Saved answer binding silent; clear does not submit; reselection works | `app/src/main/java/com/eve/app/ui/test/QuestionAdapter.kt:213` | UNVERIFIED (CI pending) | UNVERIFIED (CI pending) | ApprovedUiFunctionalTest callbacks; pending CI | UNVERIFIED |
| Mock Test | Mark / unmark review | Existing business logic retained | Existing business logic retained | `app/src/main/java/com/eve/app/ui/test/TestActivity.kt:156` | UNVERIFIED (CI pending) | UNVERIFIED (CI pending) | Diff review + existing unit tests; bookmark/report callback and palette tests where applicable | UNVERIFIED |
| Mock Test | Previous / Next / Submit | Existing business logic retained | Existing business logic retained | `app/src/main/java/com/eve/app/ui/test/TestActivity.kt:140` | UNVERIFIED (CI pending) | UNVERIFIED (CI pending) | Diff review + existing unit tests; bookmark/report callback and palette tests where applicable | UNVERIFIED |
| Mock Test | Offline / loading / error | Existing business logic retained | Existing business logic retained | `app/src/main/java/com/eve/app/ui/test/TestActivity.kt:281` | UNVERIFIED (CI pending) | UNVERIFIED (CI pending) | Diff review + existing unit tests; bookmark/report callback and palette tests where applicable | UNVERIFIED |
| Mock Test | Bookmark / Report | Existing business logic retained | Existing business logic retained | `app/src/main/java/com/eve/app/ui/test/QuestionAdapter.kt:59` | UNVERIFIED (CI pending) | UNVERIFIED (CI pending) | Diff review + existing unit tests; bookmark/report callback and palette tests where applicable | UNVERIFIED |
| Result | Tab order | Overview, Review, Leaderboard | Overview, Review, Leaderboard | `app/src/main/res/layout/activity_result.xml:126` | UNVERIFIED (CI pending) | UNVERIFIED (CI pending) | Native view assertions + rendered bitmap; pending CI | UNVERIFIED |
| Result | Floating rounded track | Existing rounded segmented geometry; isolated Result drawable | Existing rounded segmented geometry; isolated Result drawable | `app/src/main/res/drawable/bg_result_segmented_track.xml:5` | UNVERIFIED (CI pending) | UNVERIFIED (CI pending) | Native view assertions + rendered bitmap; pending CI | UNVERIFIED |
| Result | Dark track reference | #171717 | #171717 | `app/src/main/res/values-night/approved_ui_colors.xml:11` | UNVERIFIED (CI pending) | UNVERIFIED (CI pending) | Native view assertions + rendered bitmap; pending CI | UNVERIFIED |
| Result | Light track reference | #F1F1F3 | #F1F1F3 | `app/src/main/res/values/approved_ui_colors.xml:11` | UNVERIFIED (CI pending) | UNVERIFIED (CI pending) | Native view assertions + rendered bitmap; pending CI | UNVERIFIED |
| Result | Selected pill | #FFFFFF dark / #000000 light | #FFFFFF dark / #000000 light | `app/src/main/res/drawable/bg_result_segmented_indicator.xml:8` | UNVERIFIED (CI pending) | UNVERIFIED (CI pending) | Native view assertions + rendered bitmap; pending CI | UNVERIFIED |
| Result | Selected text | #000000 dark / #FFFFFF light | #000000 dark / #FFFFFF light | `app/src/main/res/layout/activity_result.xml:119` | UNVERIFIED (CI pending) | UNVERIFIED (CI pending) | Native view assertions + rendered bitmap; pending CI | UNVERIFIED |
| Result | Unselected text | Readable #FFFFFF dark / #000000 light | Readable #FFFFFF dark / #000000 light | `app/src/main/res/layout/activity_result.xml:121` | UNVERIFIED (CI pending) | UNVERIFIED (CI pending) | Native view assertions + rendered bitmap; pending CI | UNVERIFIED |
| Result | No underline | Full-height inset pill; indicator gravity stretch | Full-height inset pill; indicator gravity stretch | `app/src/main/res/layout/activity_result.xml:113` | UNVERIFIED (CI pending) | UNVERIFIED (CI pending) | Native view assertions + rendered bitmap; pending CI | UNVERIFIED |
| Result | No additional bottom navigation | Only existing Result navigation | Only existing Result navigation | `app/src/main/res/layout/activity_result.xml:101` | UNVERIFIED (CI pending) | UNVERIFIED (CI pending) | Native view assertions + rendered bitmap; pending CI | UNVERIFIED |
| Result | Initial selection | Overview, index 0 | Overview, index 0 | `app/src/main/java/com/eve/app/ui/result/ResultTabs.kt:10` | UNVERIFIED (CI pending) | UNVERIFIED (CI pending) | Native view assertions + rendered bitmap; pending CI | UNVERIFIED |
| Result | Tab section mapping | 0 Overview / 1 Review / 2 Leaderboard | 0 Overview / 1 Review / 2 Leaderboard | `app/src/main/java/com/eve/app/ui/result/ResultTabs.kt:13` | UNVERIFIED (CI pending) | UNVERIFIED (CI pending) | Native view assertions + rendered bitmap; pending CI | UNVERIFIED |
| Result | Overview statistics / analytics | Preserved; no experimental card redesign | Preserved; no experimental card redesign | `app/src/main/java/com/eve/app/ui/result/ResultActivity.kt:45` | UNVERIFIED (CI pending) | UNVERIFIED (CI pending) | Scoped diff review + JVM tests; no authenticated end-to-end claims | UNVERIFIED |
| Result | Review palette / filters / explanations | Preserved; no experimental card redesign | Preserved; no experimental card redesign | `app/src/main/java/com/eve/app/ui/result/ResultActivity.kt:45` | UNVERIFIED (CI pending) | UNVERIFIED (CI pending) | Scoped diff review + JVM tests; no authenticated end-to-end claims | UNVERIFIED |
| Result | Leaderboard summary / Full Leaderboard navigation | Preserved; no experimental card redesign | Preserved; no experimental card redesign | `app/src/main/java/com/eve/app/ui/result/ResultActivity.kt:45` | UNVERIFIED (CI pending) | UNVERIFIED (CI pending) | Scoped diff review + JVM tests; no authenticated end-to-end claims | UNVERIFIED |
| Result | Back / sharing / reattempt | Preserved; no experimental card redesign | Preserved; no experimental card redesign | `app/src/main/java/com/eve/app/ui/result/ResultActivity.kt:45` | UNVERIFIED (CI pending) | UNVERIFIED (CI pending) | Scoped diff review + JVM tests; no authenticated end-to-end claims | UNVERIFIED |
| Result | Cutoff / rank / percentile | Preserved; no experimental card redesign | Preserved; no experimental card redesign | `app/src/main/java/com/eve/app/ui/result/ResultActivity.kt:45` | UNVERIFIED (CI pending) | UNVERIFIED (CI pending) | Scoped diff review + JVM tests; no authenticated end-to-end claims | UNVERIFIED |
| Result | Existing card colors / radii / borders preserved | Preserved; no experimental card redesign | Preserved; no experimental card redesign | `app/src/main/java/com/eve/app/ui/result/ResultActivity.kt:45` | UNVERIFIED (CI pending) | UNVERIFIED (CI pending) | Scoped diff review + JVM tests; no authenticated end-to-end claims | UNVERIFIED |
| Full Leaderboard | Existing UI and behavior | Header/back; conditional rank, percentile, score; rows; loading/empty/error/Retry preserved | Header/back; conditional rank, percentile, score; rows; loading/empty/error/Retry preserved | `app/src/main/java/com/eve/app/ui/leaderboard/LeaderboardActivity.kt:27` | UNVERIFIED (CI pending) | UNVERIFIED (CI pending) | Byte-identical files versus cc47908; existing regression suite | UNVERIFIED |
| UI Studio | Active implementation removed | No Admin entry, Activity, runtime overrides, API methods, backend routes/types | No Admin entry, Activity, runtime overrides, API methods, backend routes/types | `backend/src/index.ts:5` | UNVERIFIED (CI pending) | UNVERIFIED (CI pending) | Repository-wide active-source grep + retired_routes.test.cjs | UNVERIFIED |
| UI Studio | Historical database compatibility | 0012_ui_studio.sql retained byte-for-byte; no production data deletion | 0012_ui_studio.sql retained byte-for-byte; no production data deletion | `backend/migrations/0012_ui_studio.sql:4` | UNVERIFIED (CI pending) | UNVERIFIED (CI pending) | Git diff; no database/schema cleanup performed | UNVERIFIED |

## Verification results

- Android `assembleDebug`, `lintDebug`, `testDebugUnitTest`, `assembleDebugAndroidTest`: PASS locally (189 tests; lint 0 errors).
- Backend TypeScript `npm run build`: PASS.
- Backend tests using the environment's supported proxy dispatcher: 73 PASS, 0 failed.
- Native `ApprovedUiFunctionalTest` and `ApprovedUiRenderingTest`: pending CI emulator.
- XML/resource linking: Android AAPT compile/link plus a repository XML parse check.
- Active UI Studio reference audit (`app/src/main`, `backend/src`): zero matches.
- Full Leaderboard activity, adapter, ViewModel, activity XML and row XML: unchanged versus original main.
- Production data, exam/question/result/history/authentication/Firebase features: retained.
- Historical UI Studio docs and D1 migration retained; negative retired-route tests intentionally name old routes.
- Prior cleanup/removal list and reference proof: `docs/implementation/approved-ui-removal.md`.
- This correction removes no additional unrelated files or dependencies.

## Corrections beyond the previous implementation

- Removed the 48dp question-wrapper minimum height that inflated the question-to-option gap.
- Removed the last option's trailing 10dp margin; option-to-option gaps remain 10dp.
- The 2dp timer arc now occupies its 44dp circle without an extra 2px inset, and no
  opaque full-color track conceals its depletion. Existing countdown/warning logic remains.
- Added an explicit Next/Submit zero-stroke style; Clear, Review and Previous retain 1dp.
- Factored the existing Home category/outline styling into a native presentation helper,
  used by production and runtime verification, without changing other Home properties.
- Added exhaustive native value checks and dark/light render artifacts, and this audit.

## Remaining verification limitations

Native fixtures verify geometry, styling and selected-tab content visibility, not
real exam submission to production or authenticated network results. Those business
handlers remain unchanged and relevant JVM/backend regression tests run. No production
database reset, migration deletion, force-push, branch-protection bypass, or new design.

## Intermediate native verification

Run 37758687488: 3 existing interaction tests passed; the new rendering test
failed at a half-ring edge pixel (#0B0B0B versus black) due to native antialiasing.
The exact Paint color and 2dp stroke assertions passed. The curved-edge check
now allows a 16-channel antialiasing tolerance while retaining exact Paint/value
assertions. The render export moved to scoped MediaStore Pictures storage so
Gradle's automatic APK uninstall cannot delete the artifacts. Final native
verification remains pending; this intermediate run is not reported as passed.
