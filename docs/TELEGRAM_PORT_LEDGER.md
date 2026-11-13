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

## F3 Popup menu, AlertDialog, BottomSheet - PENDING_CI
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
