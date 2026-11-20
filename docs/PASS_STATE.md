# PASS_STATE.md

> Multi-pass Telegram UI exact parity tracking for Eve (`com.eve.app`) against `Telegram-Android-master` (`21f7d109da6ab3506c9e9577f98c6a38771ab335`).

## Pass Status

| Pass | Scope | Status |
|---|---|---|
| **Pass 1** | Foundation Lock (`Theme.java` 929 keys + `currentColorsNoAccent`, upstream `.attheme`/fonts/selectors/`colors.xml`, `AndroidUtilities.getTypeface` null guard, `450L` theme switch, `ResultActivity` null-safe `getButton`, `TELEGRAM_PARITY_LEDGER.md`) | **DONE** (present in baseline) |
| **Pass 2** | Forked/Shadow Component Cleanup (`ui/profile/popup/*` -> `org.telegram.ui.ActionBar.*`, 8 clean `AlertDialog` migrations + tests, purge 0-caller `Cells.kt` & `ui/admin/tg/{DividerCell,TextDetailCell}`) | **DONE (pending CI confirmation)** |
| **Pass 3a** | `MainActivity` rebuilt programmatically (`HomeTelegramViewTree` + `ExamAdapter` + `HomeBannerAdapter`, `RecyclerListView`, `HeaderCell`, `DividerCell`, `EditTextBoldCursor`, `AlertDialog`, `ActionBarPopupWindow`, `Theme.getColor`) | **DONE (pending CI confirmation)** |
| **Pass 3b–3d** | Remaining Student Visible Screens (`ProfileActivity`, `TestActivity`, `ResultActivity`, `BookmarksActivity`, `QrShareActivity`, `UsernameCardActivity`) | PENDING |
| **Pass 4** | Remaining 14 Admin Screens (3–4 screens/pass) | PENDING |
| **Pass 5** | Final Dead-File & Legacy XML/Shadow Purge | PENDING |

## SHADOW CLASS MAP

| Shadow Class | Upstream Replacement | `app/src/main` Callers Left | Which Pass Removes It |
|---|---|---:|---|
| `com.eve.app.ui.profile.popup.{Theme, ActionBarPopupWindow, ActionBarMenuSubItem, PopupPlatform, PopupSwipeBackLayout}` | `org.telegram.ui.ActionBar.{Theme, ActionBarPopupWindow, ActionBarMenuSubItem}` | 0 | Pass 2 (DELETED) |
| `com.eve.app.designsystem.Cells` | `org.telegram.ui.Cells.*` | 0 | Pass 2 (DELETED) |
| `com.eve.app.ui.admin.tg.DividerCell` | `org.telegram.ui.Cells.DividerCell` | 0 | Pass 2 (DELETED) |
| `com.eve.app.ui.admin.tg.TextDetailCell` | `org.telegram.ui.Cells.TextDetailCell` | 0 | Pass 2 (DELETED) |
| `com.eve.app.designsystem.TelegramNativeAlertDialog` | `org.telegram.ui.ActionBar.AlertDialog` | 11 | Pass 3 & Pass 4 (AdminActivity, ManageQuestionsActivity, ManageSyllabusActivity, MainActivity, ProfileActivity, SettingsActivity, EmojiStudioActivity, ReportQuestionDialog, DebugCrashReporter, QuestionEditDialog) |
| `com.eve.app.designsystem.TelegramPopupMenu` | `org.telegram.ui.ActionBar.ActionBarPopupWindow` | 5 | Pass 3 & Pass 4 (MainActivity, ProfileActivity, BookmarksActivity, ExistingExamsAdapter, UserAdminAdapter) |
| `com.eve.app.designsystem.TelegramShareAlert` | BLOCKED (upstream `ShareAlert` needs MTProto/MessagesController) -> `org.telegram.ui.ActionBar.BottomSheet` | 4 | Pass 3 (BookmarksActivity, QrShareActivity, ResultActivity, ProfileGalleryView) |
| `com.eve.app.ui.admin.tg.HeaderCell` | `org.telegram.ui.Cells.HeaderCell` | 6 | Pass 4 (activity_admin.xml, activity_admin_rights_edit.xml, AdminDashboardPager) |
| `com.eve.app.ui.admin.tg.ShadowSectionCell` | `org.telegram.ui.Cells.ShadowSectionCell` | 6 | Pass 4 (activity_admin.xml, activity_admin_rights_edit.xml, AdminDashboardPager) |
| `com.eve.app.ui.admin.tg.TextCell` | `org.telegram.ui.Cells.TextCell` | 6 | Pass 4 (AdminActivity, ActivityLogActivity, activity_admin.xml) |
| `com.eve.app.ui.admin.tg.TextCheckCell` | `org.telegram.ui.Cells.TextCheckCell` | 4 | Pass 4 (EmojiStudioActivity, activity_admin.xml, bottom_sheet_emoji_pack_manager.xml) |
| `com.eve.app.ui.admin.tg.TextInfoPrivacyCell` | `org.telegram.ui.Cells.TextInfoPrivacyCell` | 4 | Pass 4 (activity_admin_rights_edit.xml, activity_poll_create.xml, bottom_sheet_emoji_pack_manager.xml) |
| `com.eve.app.ui.admin.tg.TelegramSegmentedTabs` | `org.telegram.ui.Components.ViewPagerFixed.TabsView` / `FilterTabsView` | 7 | Pass 4 (EditAboutActivity, EmojiStudioActivity, MistakesActivity, PyqActivity) |
| `com.eve.app.designsystem.TelegramActionBar` | `org.telegram.ui.ActionBar.ActionBar` | 46 | Pass 3, Pass 4 & Pass 5 (XML layouts) |
| `com.eve.app.designsystem.TelegramBottomSheet` | `org.telegram.ui.ActionBar.BottomSheet` | 17 | Pass 3, Pass 4 & Pass 5 |
| `com.eve.app.designsystem.TelegramSwitch` | `org.telegram.ui.Components.Switch` | 16 | Pass 3, Pass 4 & Pass 5 |
| `com.eve.app.designsystem.TelegramRadialProgressView` | `org.telegram.ui.Components.RadialProgressView` | 53 | Pass 3, Pass 4 & Pass 5 |
| `com.eve.app.designsystem.ButtonBounce` | `org.telegram.ui.Components.ButtonBounce` | 0 | Pass 5 (0 callers; retained until Pass 5 purge) |
| `com.eve.app.designsystem.MenuDrawable` | `org.telegram.ui.ActionBar.MenuDrawable` | 0 | Pass 5 (0 callers; retained until Pass 5 purge) |
| `com.eve.app.designsystem.TelegramEmptyTextProgressView` | `org.telegram.ui.Components.EmptyTextProgressView` | 0 | Pass 5 (0 callers; retained until Pass 5 purge) |
