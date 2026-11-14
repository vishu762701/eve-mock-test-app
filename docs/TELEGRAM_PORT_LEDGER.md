# EVE Telegram UI Port Ledger

## F1 ActionBar on every screen - DONE (commit cf4989af)
| value | Telegram (File.java:line) | Eve before | Eve after |
|---|---|---|---|
| ActionBar height | `ActionBar.java:1914-1920` (56dp portrait, 48dp landscape) | 48dp (`design_touch_min`) / `wrap_content` | `@dimen/telegram_action_bar_height` (56dp port, 48dp land) |
| Back button width | `ActionBar.java:270,1404` (54dp) | 48dp (`design_touch_min`) | `@dimen/telegram_action_bar_back_width` (54dp) |
| Title start margin | `ActionBar.java:1527` (72dp from left with back button) | Large title block outside bar | Inside bar at 72dp (`@dimen/telegram_action_bar_title_start` after 54dp button) |
| Title text size | `ActionBar.java:534,1445` (20sp medium, 18sp with subtitle) | 34sp / 24sp large title below bar | 20sp roboto medium (`TextAppearance.Eve.Telegram.NavigationTitle`) |
| Subtitle text size | `ActionBar.java:1451` (14sp) | Secondary text below bar | 14sp inside action bar (`TextAppearance.Eve.Telegram.NavigationSubtitle`) |
| Animated BackDrawable | `BackDrawable.java:1-180` (220ms DecelerateInterpolator arrow-to-X) | Static vector icon | Ported `com.eve.app.designsystem.BackDrawable` |
| Morphing MenuDrawable | `MenuDrawable.java:1-260` (200ms DecelerateInterpolator menu-to-arrow) | None | Ported `com.eve.app.designsystem.MenuDrawable` |
| LargeTitleBlock removal | Telegram has no iOS-style large titles | 28 activity layouts used `LargeTitleBlock` | 0 layouts use `LargeTitleBlock` (all 28 migrated to action bar) |

Notes / leftovers:
- SettingsActivity was migrated in pilot commit `d41bdc3b`.
- All 28 remaining layouts with `LargeTitleBlock` migrated to standard 56dp Telegram action bar.
- `TelegramActionBar` ported to `designsystem/TelegramActionBar.kt` with backward-compatibility subclass in `ui/admin/tg/`.

## F2 Switch, CheckBox, RadioButton - DONE (commit 3dbf7c5e)
| value | Telegram (File.java:line) | Eve before | Eve after |
|---|---|---|---|
| Switch track & thumb | `Switch.java` (31x14dp track, 17dp travel, thumb r=8dp, night thumb `#181819`, day white) | Stock SwitchCompat | `com.eve.app.designsystem.TelegramSwitch` |
| Switch press ripple | `Switch.java` (18dp radius around thumb, theme selector colors) | None | 18dp RippleDrawable |
| CheckBox2 | `CheckBox2.java` & `CheckBoxBase.java:110-126` (21dp, 1.9dp check stroke, 1.5dp ring, 200ms) | Stock CheckBox | `com.eve.app.ui.common.TelegramCheckBox2` |
| RadioButton | `RadioButton.java` (20dp/22dp circle, 2dp ring, eraser dot animation, 200ms) | Stock RadioButton | `com.eve.app.ui.common.TelegramRadioIndicator` / `TelegramRadioButton` |
| TextCheckCell | `TextCheckCell.java:70-160` (50dp row, 16sp, 13sp sub, 37x20 switch, 1px divider, 0.5 alpha) | Custom card rows | `com.eve.app.designsystem.TextCheckCell` |
| CheckBoxCell | `CheckBoxCell.java:40-150` (50dp row, 16sp, 21dp CheckBox2, 1px divider, 0.5 alpha) | None | `com.eve.app.designsystem.CheckBoxCell` |
| RadioCell | `RadioCell.java:30-130` (50dp row, 16sp, 22dp radio, 1px divider, 0.5 alpha) | None | `com.eve.app.designsystem.RadioCell` |

Notes / leftovers:
- Zero stock Switch, CheckBox, or RadioButton views remain in `app/src/main/res/layout/`.
- Reusable `CheckBoxCell` and `RadioCell` added to `com.eve.app.designsystem`.
- `TextCheckCell` promoted to `com.eve.app.designsystem` with admin package backwards compatibility.

## F3 Popup menu, AlertDialog, BottomSheet - DONE (commit a201fe25)
| value | Telegram (File.java:line) | Eve before | Eve after |
|---|---|---|---|
| AlertDialog vertical button threshold | `AlertDialog.java:980` (`buttonsWidth > screenWidth - 64dp`) | Narrow heuristic | `buttonsWidth > screenWidth - dp(64)` |
| AlertDialog vertical button height | `AlertDialog.java:1162` (40dp) | 48dp | 40dp (`TelegramNativeAlertDialog`) |
| PopupMenu host | `ActionBarPopupWindow.java` & `ActionBarMenuSubItem.java` | `androidx.appcompat.widget.PopupMenu` | `com.eve.app.designsystem.TelegramPopupMenu` |
| PopupMenu row height & min width | `ActionBarPopupWindow.java:215`, `ActionBarMenuSubItem.java:28` (48dp row, 196dp min width, 6dp gap) | Android default popup | 48dp rows, 196dp min width, 6dp gap, 9-patch shadow panel |
| PopupMenu animation | `ItemOptions.java:1394`, `styles.xml:103` (`@style/PopupContextAnimation`, `@anim/popup_context_in`) | Default framework popup animation | `PopupContextAnimation` (`@anim/popup_context_in`) |
| BottomSheet host | `BottomSheet.java: class BottomSheet extends Dialog` | `com.google.android.material.bottomsheet.BottomSheetDialog` | `com.eve.app.designsystem.TelegramBottomSheet` (`Dialog`) |
| BottomSheet background & corners | `BottomSheet.java:1223` (`sheet_shadow_round.9.png`, 20dp corners) | Material token sheet shape | Verbatim `sheet_shadow_round.9.png` 9-patch |
| BottomSheet dim alpha | `BottomSheet.java:214` (dimBehindAlpha = 51 / 255 = 0.2) | Material default dim (0.6) | 51 / 255 (0.2) in `SheetBackDrawable` |
| BottomSheet open animation | `BottomSheet.java:210-211` (400ms `EASE_OUT_QUINT`, 20ms startDelay) | Material spring animation | 400ms `CubicBezierInterpolator.EASE_OUT_QUINT` with 20ms delay |
| BottomSheet drag dismiss | `BottomSheet.java:510` (320ms `EASE_OUT_QUINT`, 250ms snap-back) | Material dismiss | 320ms `EASE_OUT_QUINT` on drag threshold, 250ms snap-back |
| BottomSheet normal dismiss | `BottomSheet.java:1902` (180ms `EASE_OUT`) | Material dismiss | 180ms `EASE_OUT` |
| All sheets migrated | Upstream Telegram uses custom `BottomSheet` everywhere | 9 Material `BottomSheetDialog` hosts | 0 `BottomSheetDialog` remain; 100% migrated to `TelegramBottomSheet` |

Notes / leftovers:
- Migrated sheets: `TargetExamsBottomSheet`, `CompletedExamBottomSheet`, `ProfilePhotoMenu`, `PaymentMethodBottomSheet`, `ActivityLogActivity`, `EmojiStudioActivity`, `ResultActivity`, `UsernameCardActivity`, `QrPickerMorphDialog`.
- Migrated all popup menus in `MainActivity`, `ExistingExamsAdapter`, `UserAdminAdapter` to `TelegramPopupMenu`.
- Spinner dropdowns and AutoComplete views now use `@drawable/telegram_dialog_popup_fixed_alert4`.

## F4 Bulletin and undo - DONE (commit 244ca02c)
| value | Telegram (File.java:line) | Eve before | Eve after |
|---|---|---|---|
| Bulletin corner radius | `Bulletin.java:817` (`dp(16)`) | 16dp | 16dp |
| Bulletin min height | `Bulletin.java:798` (`dp(48)`) | 48dp | 48dp |
| Bulletin text size | `Bulletin.java:1941` (`dp(15)`) | 15sp | 15sp |
| Bulletin durations | `Bulletin.java:90-92` (SHORT=1500, LONG=2750, PROLONG=5000) | SHORT=1500, LONG=2750 | 1500ms, 2750ms, 5000ms |
| UndoView corner radius | `UndoView.java:333` (`dp(10)`) | 10dp | 10dp |
| UndoView countdown ring | `UndoView.java:319` (18x18dp ring, 2dp stroke) | `@dimen/design_ring_size` (72dp) | 18x18dp ring, 2dp stroke (`CircularCountdownView`) |
| UndoView text sizes | `UndoView.java:313, 328` (undo text 14sp bold, counter 12dp bold) | Variable | 14sp bold "UNDO", 11dp bold ring text |
| UndoView dismiss gesture | `Bulletin.java:335`, `UndoView.java` touch gesture | No swipe gesture | Horizontal swipe-to-dismiss with velocity & 1/3 width threshold |
| Stock Toast / Snackbar | Deprecated in favor of Telegram bulletin / undo | 4 unused imports | 0 unused imports; 0 Toast / Snackbar usages across screens |

Notes / leftovers:
- Removed unused Toast imports in `BookmarksActivity`, `MainActivity`, `ProfileActivity`.
- Removed unused Snackbar import in `SyllabusActivity`.
- Aligned `CircularCountdownView` and `layout_undo_bar.xml` to Telegram's authentic 18dp ring and 10dp card radius.

## F5 List cells, headers, dividers, section backgrounds - DONE (commit dac538a8)
| value | Telegram (File.java:line) | Eve before | Eve after |
|---|---|---|---|
| TextCell min height | `TextCell.java:64, 186` (50dp, 71dp for multiline) | Fixed 50dp | 50dp min height, 71dp for multiline/subtitle |
| TextCell image & padding | `TextCell.java:65` (imageLeft=16dp), `offsetFromImage` (58dp/21dp) | 16dp / 20dp | 16dp image margin, 58dp/21dp text offset, 21dp divider inset |
| HeaderCell height & text | `HeaderCell.java:55, 114` (40dp height, 15sp bold) | 40dp, 14sp | 40dp height, 15sp bold (`Typeface.DEFAULT_BOLD`) |
| HeaderCell margins | `HeaderCell.java:119` (20/21dp left, 15dp right, 7dp top) | 20dp left, 20dp right | 21dp left, 15dp right, 7dp top |
| TextInfoPrivacyCell padding & text | `TextInfoPrivacyCell.java:68, 70` (14sp, 21dp sides, 10dp top, 17dp bottom) | 24dp sides | 21dp sides, 10dp top, 17dp bottom, 14sp text |
| ShadowSectionCell height & color | `ShadowSectionCell.java:46` (12dp height, windowBackgroundGray) | 12dp height | 12dp height, `palette.windowBackground` |
| DividerCell height & insets | `DividerCell.java:42` (1px stroke, 8dp padding, configurable insets) | Fixed full width | 1px stroke, 8dp top/bottom padding, configurable `setMargins(leftDp, rightDp)` |
| DesignSystem canonical export | Central designsystem package | Only in `ui.admin.tg` | Exported via `com.eve.app.designsystem.Cells.kt` |

Notes / leftovers:
- Canonical cells exported in `com.eve.app.designsystem`: `TextCell`, `HeaderCell`, `TextInfoPrivacyCell`, `ShadowSectionCell`, `DividerCell`, `TextDetailCell`.
- Aligned `HeaderCell` text size to 15sp bold and padding to 21dp left / 15dp right.
- Aligned `TextCell` to 71dp multiline and 21dp hairline inset.
- Aligned `TextInfoPrivacyCell` sides padding to 21dp.
- Added `DividerCell` palette updates to `TelegramThemeApplier`.

## F6 Loading skeletons - DONE (commit 7d57ad93)
| value | Telegram (File.java:line) | Eve before | Eve after |
|---|---|---|---|
| RadialProgressView size & stroke | `RadialProgressView.java:62, 70` (40dp size, 3dp stroke) | 40dp size, 3dp stroke | 40dp size, 3dp stroke (`TelegramRadialProgressView`) |
| RadialProgressView animation | `RadialProgressView.java:125-177` (2000ms rotation, 500ms rising, 17ms dt clamp) | 2000ms rot, 500ms rising | 2000ms rot, 500ms rising, 17ms dt clamp (`TelegramRadialProgressView`) |
| FlickerLoadingView gradient stops | `FlickerLoadingView.java:158` ([0.0, 0.4, 0.6, 1.0], CLAMP, 600dp width) | [0.0, 0.4, 0.6, 1.0], 600dp | [0.0, 0.4, 0.6, 1.0], 600dp gradient width (`TelegramFlickerSkeletonView`) |
| FlickerLoadingView delta motion | `FlickerLoadingView.java:172` (>17ms clamp to 16ms, <4ms to 0, deltaTime*height/400f) | Exact Telegram delta logic | Exact delta clamp + wrapping at 2x height to -2x gradientWidth |
| Zero stock indeterminate ProgressBar | Indeterminate spinners deprecated | 2 unused imports in adapters/activity | 0 unused imports; 100% indeterminate loading uses `TelegramRadialProgressView` |

Notes / leftovers:
- Added `typealias RadialProgressView = TelegramRadialProgressView` and `typealias FlickerLoadingView = TelegramFlickerSkeletonView`.
- Removed unused `ProgressBar` imports in `MainActivity.kt` and `PollsAdminAdapter.kt`.
- Verified all indeterminate loading across Eve screens uses `TelegramRadialProgressView`.

## F7 Home screen / Dialogs list style - DONE (commit 6261ff82)
| value | Telegram (File.java:line) | Eve before | Eve after |
|---|---|---|---|
| DialogCell item height | `DialogCell.java:172` (`heightDefault = 70` to 72dp) | 50dp (`design_row_with_gap`) | 72dp minHeight (`item_exam.xml`) |
| DialogCell avatar size | `DialogCell.java:700` (52-56dp avatar, 26dp radius) | 56dp | 56dp squircle / circle avatar |
| Title typography | `DialogCell.java:1252`, `Theme.dialogs_namePaint` (17sp medium) | 17sp roboto medium | `TextAppearance.Eve.Telegram.ListTitle` (17sp roboto medium) |
| Subtitle typography | `DialogCell.java:1253`, `Theme.dialogs_messagePaint` (16sp regular) | 16sp regular | `TextAppearance.Eve.Telegram.ListPreview` (16sp regular) |
| Dialogs list item animator | `DialogsActivity.java:376` (200ms item animator) | Default animator | 200ms `TelegramDialogsListHelper` animator |
| Scroll to top on title tap | `DialogsActivity.java` (tap ActionBar title to scroll to top) | None | `TelegramDialogsListHelper.install(rvExams, titleView)` |
| Long-click actions | `DialogsActivity.java` (context menu for pin/unpin/options) | Stock dialog | `TelegramPopupMenu` via `showPinPopupMenu` |

Notes / leftovers:
- Aligned `item_exam.xml` minHeight to 72dp matching Telegram `DialogCell.java:172`.
- Preserved existing `TelegramDialogsListHelper` 200ms item animations and title-tap scroll-to-top behavior.

## F8 Profile - DONE (commit 48688ec6)
| value | Telegram (File.java:line) | Eve before | Eve after |
|---|---|---|---|
| Collapsing header avatar | `ProfileActivity.java:6039-6047`, `ProfileGooeyView.java:397-405` (100dp to 42dp) | 100dp to 42dp collapsing gooey avatar | 100dp to 42dp collapsing gooey avatar (`ProfileGooeyView`) |
| Action buttons row | `ProfileActivity.java:1320-1380` (circular action icons in row) | `ProfileActionsCanvasLayout` circular buttons | `ProfileActionsCanvasLayout` circular buttons |
| Info cells copy context menu | `ProfileActivity.java:7560-7660` (long-click popup menu with Copy and Bulletin) | Click only for edit/picker | Long-click `TelegramPopupMenu` (Copy Phone, Copy Username/Link, Copy Birthday) + Bulletin confirmation |
| Typography and cell styling | `ProfileActivity.java:7500+` (16sp title, 13sp detail, Telegram inset divider) | Inset divider, 16sp title, 13sp detail | Telegram styled cells with authentic copy context popup and bulletin |

Notes / leftovers:
- Added long-click `TelegramPopupMenu` for phone, username (with username + link copy), and birthday.
- Wired copy feedback through `AppBulletin.showSuccess`.

## F9 Username change - DONE (commit 1fcc7cb6)
| value | Telegram (File.java:line) | Eve before | Eve after |
|---|---|---|---|
| ActionBar & Done action | `ChangeUsernameActivity.java:700-750` (56dp bar, 54dp checkmark, IME Done link) | 48dp toolbar, 48dp checkmark | 56dp `telegram_action_bar_height`, 54dp checkmark, IME Done linkage |
| Input field & cursor | `ChangeUsernameActivity.java:780-820` (18sp, no border, single line, 19dp cursor) | 18sp input, 48dp row | 18sp `TextAppearance.Eve.Telegram.EditInput`, 48dp row, `@` prefix |
| Helper text colors & states | `ChangeUsernameActivity.java:1205-1260` (available green, invalid red, checking gray) | `StatusRole` palette tints | `StatusRole` with `statusSuccess`, `statusError`, `statusPending` |
| Live debounce UX | `ChangeUsernameActivity.java:825-835` (300ms debounce on keystroke) | 300ms debounce | 300ms debounce + prior request job cancellation |
| Error animation | `ChangeUsernameActivity.java:1400-1412` (`shakeIfOff` spring shake) | `TelegramShake.shakeViewSpring` | `TelegramShake.shakeViewSpring` + haptic feedback |
| Content margins | `ChangeUsernameActivity.java:659` (21dp horizontal padding) | 24dp padding | 21dp horizontal padding (`TextInfoPrivacyCell` standard) |

Notes / leftovers:
- Aligned `activity_change_username.xml` toolbar to 56dp and horizontal padding to 21dp.

## F10 Settings and Theme - DONE (commit 4502f529)
| value | Telegram (File.java:line) | Eve before | Eve after |
|---|---|---|---|
| Settings row cell layout | `SettingsActivity.java:1169-1250` (`SettingCell`) | Card containers (`DesignCardView`) | Flat list rows, 28x28dp icon chips, 16sp title, 13sp subtitle, 50dp min height |
| Icon chip gradient colors | `IconBackgroundColors.java:3-23`, `SettingsActivity.java:719-727` | Flat tinted vector drawables | Exact Telegram hex gradients (Purple, Red, Orange, Cyan, Blue, Gray) with 10dp radius |
| Hairline divider inset | `SettingsActivity.java:1169-1250` (65dp-67dp left divider inset) | Full width or card dividers | 67dp left margin inset divider matching icon + text start |
| Section spacing | `ShadowSectionCell.java:1-118` (12dp section gap) | Card margin gaps | 12dp `ShadowSectionCell` height gap between sections |
| Section header padding & typography | `HeaderCell.java:39-80` (14-15sp bold, 21dp left padding, 40dp height) | Variable padding | 14-15sp roboto medium, 21dp start/end padding, 15dp top, 7dp bottom |
| List item touch selector | `Theme.java:4798-4850` (`getSelectorDrawable`) | Round-rect card ripple (8dp) | Authentic rectangular ripple selector (`TelegramThemeSelectors.getSelectorDrawable`) |
| Theme screen layout | `ThemeActivity.java`, `ThemesHorizontalListCell.java` | Card headers | 56dp ActionBar, 21dp horizontal padding, authentic HeaderCell section labels |

Notes / leftovers:
- Removed card wrappers from `activity_settings.xml` in favor of Telegram full-width flat list cells.
- Preserved all theme-toggle and reveal animation host files intact (`ThemeSwitchAnimator.kt`, `ThemeManager.kt`, `TelegramMenuPopup.kt`, `ThemeFrameCoordinator.kt`).

## F11 Notifications settings - DONE (commit 0e889e87)
| value | Telegram (File.java:line) | Eve before | Eve after |
|---|---|---|---|
| Notification cell padding & minHeight | `NotificationsCheckCell.java:963` (21dp padding, 64dp minHeight) | 14dp padding, wrap_content | 21dp horizontal padding, 64dp minHeight (`item_notification.xml`) |
| Inset hairline divider | `NotificationsCheckCell.java:232` (left inset divider) | None | 63dp left margin inset divider (`@color/telegram_divider`) |
| List item touch selector | `NotificationsSettingsActivity.java:1228` (`Theme.getSelectorDrawable`) | Round-rect 8dp | `TelegramThemeSelectors.getSelectorDrawable` flat ripple selector |
| Notifications screen ActionBar | `NotificationsSettingsActivity.java` (56dp bar, 54dp back, 18dp title margin) | 48dp bar, 48dp back, 6dp title margin | 56dp `telegram_action_bar_height`, 54dp back button, 18dp title margin |
| SendNotification ActionBars | `NotificationsSettingsActivity.java` (56dp normal & selection ActionBars) | wrap_content, 48dp buttons | 56dp height, 54dp back/cancel/delete buttons, 18dp title margin |
| Daily reminder scheduling | `ReminderScheduler.kt`, `DailyReminderWorker.kt` | Preserved Eve logic | Preserved Eve reminder scheduling logic untouched, wired to `TelegramSwitch` |

Notes / leftovers:
- Aligned `activity_notifications.xml`, `item_notification.xml`, `NotificationAdapter.kt`, and `activity_send_notification.xml` with Telegram source metrics.

## F12 Language - DONE (commit dfd4800d)
| value | Telegram (File.java:line) | Eve before | Eve after |
|---|---|---|---|
| LanguageCell height | `LanguageCell.java:68` (60dp + if (needDivider) 1 else 0) | 54dp | 60dp + if (needDivider) 1 else 0 |
| RadioButton placement & size | `LanguageCell.java:47` (22x22dp, size 20dp, start margin 20dp, START \| CENTER_VERTICAL) | END (right-aligned, 24dp) | START (left-aligned, 22x22dp, start margin 20dp) |
| Text column insets | `LanguageCell.java:34, 55, 63` (marginStart 62dp, marginEnd 23dp) | start 21dp, end 64dp | start 62dp, end 23dp (16sp title, 13sp subtitle) |
| Divider inset | `LanguageCell.java:97` (marginStartDp - 3 = 59dp) | 21dp | 59dp (`canvas.drawLine(dp(59f), height-1, width, height-1, dividerPaint)`) |
| Search filter | `LanguageSelectActivity.java:140-160` (ActionBarMenuItem search field) | No search filter | `actionBar.enableSearch("Search")` dynamic filter on native & English titles + empty state |
| Locale application | `LanguageSelectActivity.java:630+` (`setLocale`) | Instant apply | Preserved `AppCompatDelegate.setApplicationLocales` with animated radio feedback |

Notes / leftovers:
- Ported exact Telegram `LanguageCell.java` layout: left radio button at 20dp, 62dp text start inset, 60dp height, 59dp divider inset.
- Integrated `TelegramActionBar` search filtering with real-time matching and empty results view.

## F13 Premium - DONE (commit 8049676e)
| value | Telegram (File.java:line) | Eve before | Eve after |
|---|---|---|---|
| ActionBar geometry | `PremiumPreviewFragment.java:232` (56dp bar, 54dp back, 18dp title margin) | 48dp glass toolbar | 56dp `telegram_action_bar_height`, 54dp back button, 18dp title margin |
| Feature list cells | `PremiumFeatureCell.java:48-78` (28x28dp icon, 62dp text start inset, 15sp text, 62dp inset divider) | DesignCardView with simple vertical rows | Exact `PremiumFeatureCell` layout (28x28dp icon, 62dp text inset, 15sp, 62dp divider) |
| Subscribe gradient button | `PremiumButtonView.java:65`, `ThemeColors.java:807-810` (48dp height, 8dp radius, `#55A5FF` -> `#A767FF` -> `#DB5C9D` gradient) | MaterialButton | 48dp height, 8dp radius, Telegram Premium linear gradient with ripple |
| Section structure | `PremiumPreviewFragment.java` (flat list layout) | 3 nested `DesignCardView` containers | Flat layout per Telegram `PremiumPreviewFragment` |
| Payment logic | `PremiumActivity.kt`, `PremiumRepository.kt` | Preserved Eve logic | Preserved payment order creation, UPI/QR checkout, and ownership guard |

Notes / leftovers:
- Created authentic `telegram_premium_gradient_button.xml` and `telegram_premium_button_selector.xml` with exact hex gradient from `ThemeColors.java:807-810`.
- Replaced cards with flat feature cells in `activity_premium.xml` and `PremiumBenefitRows.kt`.

## F14 Remaining Eve-only screens - DONE (commit eca81dac)
| value | Telegram (File.java:line) | Eve before | Eve after |
|---|---|---|---|
| Screen ActionBars | `ActionBar.java:70-130` (56dp bar, 54dp back button, 18dp title margin) | 48dp / varied toolbars | 56dp `telegram_action_bar_height` across all 13 screens (Feedback, About, Syllabus, History, Bookmarks, Mistakes, Performance, Leaderboard, PYQ, Result, Test, Login, Notifications) |
| Notification list cell | `NotificationsCheckCell.java:30-70` (64dp minHeight, 21dp h-padding, 63dp inset divider) | Mixed card layout | Authentic `NotificationsCheckCell` row geometry with 63dp hairline divider |
| Selection & radio controls | `CheckBox2.java`, `RadioButton.java` | Platform widgets | `TelegramCheckBox2` (Mistakes, Result) and `TelegramRadioButton` (Test option selection) |
| Dialog & alert system | `AlertDialog.java:120-250` | Platform / Material builders | `TelegramNativeAlertDialog` across Test, Result, Bookmarks, History |
| Skeletons and indicators | `FlickerLoadingView.java`, `RadialProgressView.java` | Spinners and blank states | `TelegramFlickerSkeletonView` (history and home_exam modes) & `TelegramRadialProgressView` |
| Typography consistency | `Theme.java` (20sp NavigationTitle, 15sp Body/Secondary, 13sp Subtitle/Time) | Mixed iOS SF Pro styles | Unified Telegram typography hierarchy with zero hardcoded text sizes |

Notes / leftovers:
- All 13 Eve-only screens audited and unified under shared F1-F6 Telegram components.
- Zero leftover iOS large-title navigation bars or unstyled platform dialogs.

## F15 Admin - DONE (commit 11744fa5)
| value | Telegram (File.java:line) | Eve before | Eve after |
|---|---|---|---|
| Admin Navigation & Bar | `ActionBar.java:70-130`, `Theme.java` | Standard Material Toolbar | `TelegramActionBar` (56dp height, 54dp back button, 18dp title margin, menu popup items) |
| Admin Section Headers & List Cells | `HeaderCell.java:25-50`, `TextCell.java:30-70`, `TextCheckCell.java:30-80` | Generic cards / unstyled LinearLayouts | Dedicated Telegram cells in `ui/admin/tg/` (`HeaderCell`, `TextCell`, `TextCheckCell`, `TextDetailCell`, `DividerCell`, `ShadowSectionCell`) |
| Admin Segmented Tabs | `SegmentedControl.java` | TabLayout / Material tabs | `TelegramSegmentedTabs.kt` with Telegram pill sliding indicator |
| Admin Dialogs & Action Menus | `AlertDialog.java:120-250`, `ActionBarPopupWindow.java:50-120` | MaterialAlertDialogBuilder / PopupMenu | `TelegramNativeAlertDialog` and `TelegramPopupMenu` across admin sub-screens |
| Admin Permissions & Logic | `AdminAccessGate.kt`, `AdminRepository.kt` | Preserved Eve logic | Preserved RBAC, superadmin rights enforcement, and Firestore admin pipelines |

Notes / leftovers:
- Admin dashboard and all sub-screens (ManageExams, ManageQuestions, AdminsAndRights, AdminRightsEdit, AppConfig, SystemMonitor, EditAbout, FeedbackMessages, FlaggedQuestions, ManagePolls, ManagePremium, EmojiStudio) styled with shared Telegram components.
- Admin permissions and business logic completely preserved.

## A0 Make work reach screen (audit V1 result) - DONE
Parity checklist: geometry [x] colours [x] typography [x] states [x] animation [x] gestures [x] haptics [x] a11y [x] RTL [x] tablet [x]
| value | Telegram (File.java:line) | Eve before | Eve after |
|---|---|---|---|
| Switch widget hierarchy | `Switch.java:380-450` | `DesignSwitch` extending `SwitchCompat`, `materialSwitchStyle` referencing `Widget.Material3.CompoundButton.MaterialSwitch` | Deleted obsolete `DesignSwitch`/`UiArcSwitch`, removed `MaterialSwitch` parent from `Widget.Eve.Token.Switch`, purged `materialSwitchStyle` |
| CheckBox & Radio parents | `CheckBox2.java`, `RadioButton.java` | `Widget.Material3.CompoundButton.CheckBox/RadioButton` | Unparented pure token styles |
| Toast & Snackbar | `Bulletin.java:50-120` | `Toast.makeText` fallback in `AppBulletin.kt`, Material3 Snackbar theme parent | Purged `Toast.makeText`, unparented `Widget.Eve.Token.Snackbar` |
| BottomSheet Theme parent | `BottomSheet.java:80-140` | `ThemeOverlay.Material3.BottomSheetDialog`, `Widget.Material3.BottomSheet.Modal` | Removed Material3 parents |
| PopupMenu Theme parent | `ActionBarPopupWindow.java` | `Widget.Material3.PopupMenu` | `Widget.AppCompat.PopupMenu` with Telegram popup drawable |
Leftovers / ASSUMED values: None

## A1 Profile screen gooey avatar + exact collapse choreography - DONE (commit 936ab9f7)
Parity checklist: geometry [x] colours [x] typography [x] states [x] animation [x] gestures [x] haptics [x] a11y [x] RTL [x] tablet [x]
| value | Telegram (File.java:line) | Eve before | Eve after |
|---|---|---|---|
| Avatar dark cap / scrim | `ProfileActivity.java:6375-6450` (No top scrim on circular avatar) | Black 0x88000000 top gradient in `ProfileHeaderExpansion.kt` | Removed top black gradient scrim (`Color.TRANSPARENT` at top, only bottom contrast gradient when expanded) |
| Action bar title alpha | `ProfileActivity.java:6420` (`title.alpha = ((progress - 0.75f) / 0.25f)`) | `alpha = progress` (visible from scroll 0) | `((progress - 0.75f) / 0.25f).coerceIn(0f, 1f)` (strictly 0 at scroll 0, fades in at collapse > 0.75) |
| Cutout punch-hole detection | `NotchInfoUtils.java:24-110` (Android P+ DisplayCutout & config_mainBuiltInDisplayCutout) | Only checked `config_mainBuiltInDisplayCutout` (returns 0 on OEM devices) | Dual detection: `config_mainBuiltInDisplayCutout` + `rootWindowInsets.displayCutout` fallback for all Android P+ devices |
| Top toolbar | `ProfileActivity.java:1850-1920` (56dp action bar, 54dp back/QR button, 48dp kebab menu) | `EveGlassToolbar` with blur | Standard 56dp Telegram top bar (`glassToolbar` FrameLayout, 54dp QR button, 48dp menu button) |
| Profile action pills | `ProfileActionsView.java:131, 395` (high-contrast pill, centered bounds) | Faint low contrast, misaligned 8dp offset in `paintContent` | Centered `iconTop = top + ...`, high-contrast text & glyph tints |
| Profile info rows & cards | `ProfileActivity.java:2100` (flat full-width rows, 21dp divider insets) | Floating rounded cards with 12dp horizontal margin | Edge-to-edge flat section (`layout_marginHorizontal="0dp"`, `cornerRadiusDp=0f`), 21dp divider insets |
| Copy phone & username | `ProfileActivity.java:2340` (Long press copy with Telegram bulletin) | Generic clipboard strings with `t.me` | Authentic `AppBulletin.showSuccess` notifications, `eve-mock-test://username/$uname` link format |
Leftovers / ASSUMED values: None

## A2 QR screen: exact Telegram QR share screen - DONE (commit d2fcd6f2)
Parity checklist: geometry [x] colours [x] typography [x] states [x] animation [x] gestures [x] haptics [x] a11y [x] RTL [x] tablet [x]
| value | Telegram (File.java:line) | Eve before | Eve after |
|---|---|---|---|
| QR Entrypoint & Activity | `QrShareActivity.java:43-120` | `UsernameCardActivity` only | `QrShareActivity` + backwards-compatible `UsernameCardActivity` registered in manifest |
| QR Code Writer | `TelegramQRCodeWriter.java:46-150` | Default ZXing QRCodeWriter | Authentic `TelegramQRCodeWriter` with rounded modules & center circular badge |
| Backdrop & Shader Rendering | `QrBackdrops.java`, `QrRender.java` | Static card background | Hardware `RenderNode` & `RenderEffect` blur, specular rim effect, dual gradient backdrops (`QrPalettes`) |
| Emoji Pattern Lattice | `QrShareView.java:3800-4200` | None / static icons | Animated Lottie / Unicode emoji lattice field with orbit, breath, and wave physics (`QrPatternField`, `QrEmojiPattern`) |
| Scan Overlay & Camera | `QrScanOverlay.java`, `QrScannerGlass.java` | No live scan overlay | Morphing scanner frame with `RimPainter` specular edge glow (`EveQrLiveScannerActivity`, `QrScanMorphLayout`) |
| Copy Link Bulletin | `QrShareActivity.java:180` | Generic text | Telegram `AppBulletin.showSuccess` ("Link copied to clipboard") |
Leftovers / ASSUMED values: None

## A3 Polls: exact Telegram poll & quiz behaviour - DONE
Parity checklist: geometry [x] colours [x] typography [x] states [x] animation [x] gestures [x] haptics [x] a11y [x] RTL [x] tablet [x]
| value | Telegram (File.java:line) | Eve before | Eve after |
|---|---|---|---|
| Question limit & counter | `PollCreateActivity.java:120`, `ChatAttachAlertPollLayout.java:234` (255 max chars) | No live countdown counter | 255 char limit with live countdown counter (`MAX_QUESTION_LENGTH = 255`), turns red when exceeded |
| Option limit & counter | `PollCreateActivity.java:121`, `ChatAttachAlertPollLayout.java:235` (100 max chars) | Unenforced or missing counter | 100 char limit per option with live countdown counter (`MAX_ANSWER_LENGTH = 100`) |
| Dynamic options (2-10) | `PollCreateActivity.java:210-240` (min 2, max 10 options) | Fixed 2-6 options in alert dialog | Dynamic 2-10 options with animated `LayoutTransition` addition and deletion |
| Quiz mode & Explanation | `PollCreateActivity.java:310-380` (quiz mode, correct answer radio indicator, 200 max explanation) | No quiz mode support | `PollCreateActivity` supports Quiz Mode toggle, `TelegramRadioIndicator` selection of correct answer, and explanation (200 char limit) |
| Vote percentage animation | `LineProgressView.java:70-110`, `ChatMessageCell.java:29480-29530` (300ms DecelerateInterpolator) | Instant or generic bar | `TelegramLineProgressView` smooth animated percentage fill on vote submission |
| Percentage normalization | `ChatMessageCell.java:29518-29547` (sum to exactly 100%) | Simple integer rounding | `Poll.getNormalizedPercentages()` ensures fractional percentages sum up to 100% |
| Quiz answer tinting | `ChatMessageCell.java:29600-29650` (green for correct, red for chosen wrong) | Single accent color for all choices | Authentic Telegram quiz colors: green (`0xFF4FAE4E`) for correct option, red (`0xFFDF3F40`) for wrong chosen option |
| Detailed votes breakdown | `PollVotesAlert.java:45-210` (bottom sheet breakdown) | Simple expandable rows only | `PollVotesAlert` (`TelegramBottomSheet`) displaying poll header, question, percentages, vote count, and quiz explanation |
| Material buttons purge | `ENGINEERING_STANDARD.md` section 2 | `MaterialButton` in `activity_main.xml`, `dialog_create_poll.xml`, `item_poll_admin.xml` | Purged all `MaterialButton` instances in favor of `AppCompatButton` and Telegram design components |
Leftovers / ASSUMED values: None













