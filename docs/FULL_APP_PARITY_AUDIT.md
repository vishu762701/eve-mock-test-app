# FULL_APP_PARITY_AUDIT.md

- **Starting HEAD**: `fe39b93d2d10e270a23ab742915e8b83c8903eab`
- **Total Registered Activities in `AndroidManifest.xml`**: **59**
- **Telegram Source Archive**: `Telegram-Android-master-unchanged.tar.zst` (SHA-256 `18995e0cbddb2f1d4c417bf1829018e91de84678b3ae2f63900d0c243d383402`, upstream `21f7d109da6ab3506c9e9577f98c6a38771ab335`)
- **Mission Status**: `NOT COMPLETE` — Settings crash root cause identified & fixed in `Theme.cut` / `Theme.java` + `AndroidUtilities.checkDisplaySize` fixed + regression tests added; full CI build, instrumentation, visual screenshot parity, Macrobenchmark latency, and ARM64 release APK sizing remain blocked locally due to missing Android SDK / `javac` / emulator / internet in VM.

| # | Registered Activity | Current Implementation | Honest Mission Status (Sec 4.3) |
|---|---|---|---|
| 1 | `.ui.login.LoginActivity` | Real `org.telegram.ui.*` Views (`ActionBar`/`Cells`/`RecyclerListView`) | `PARTIAL (needs CI runtime, visual & latency verification)` |
| 2 | `.ui.home.MainActivity` | Real `org.telegram.ui.*` Views (`ActionBar`/`Cells`/`RecyclerListView`) | `PARTIAL (needs CI runtime, visual & latency verification)` |
| 3 | `.ui.profile.EveQrLiveScannerActivity` | Real `org.telegram.ui.*` Views (`ActionBar`/`Cells`/`RecyclerListView`) | `PARTIAL (needs CI runtime, visual & latency verification)` |
| 4 | `.ui.profile.QrShareActivity` | EVE `KOTLIN_TG_DS` design system | `PARTIAL (not yet direct `org.telegram.ui.*`)` |
| 5 | `.ui.profile.UsernameCardActivity` | EVE `KOTLIN_TG_DS` design system | `PARTIAL (not yet direct `org.telegram.ui.*`)` |
| 6 | `.ui.poll.PollCreateActivity` | EVE `KOTLIN_TG_DS` design system | `PARTIAL (not yet direct `org.telegram.ui.*`)` |
| 7 | `.ui.settings.PasscodeActivity` | Ported `org.telegram.ui.*` Fragment | `INTERACTION_VERIFIED (Port Smoke only; visual/perf pending)` |
| 8 | `.ui.settings.CacheControlActivity` | Real `org.telegram.ui.*` Views (`ActionBar`/`Cells`/`RecyclerListView`) | `PARTIAL (needs CI runtime, visual & latency verification)` |
| 9 | `.ui.profile.ProfileActivity` | Real `org.telegram.ui.*` Views (`ActionBar`/`Cells`/`RecyclerListView`) | `PARTIAL (needs CI runtime, visual & latency verification)` |
| 10 | `.ui.profile.AvatarCropActivity` | Thin alias / legacy | `PARTIAL` |
| 11 | `.ui.profile.PhotoCropActivity` | Ported `org.telegram.ui.*` Fragment | `IMPLEMENTED_NOT_TESTED` |
| 12 | `.ui.profile.ChangeNameActivity` | Ported `org.telegram.ui.*` Fragment | `INTERACTION_VERIFIED (Port Smoke only; visual/perf pending)` |
| 13 | `.ui.profile.ChangeUsernameActivity` | Ported `org.telegram.ui.*` Fragment | `INTERACTION_VERIFIED (Port Smoke only; visual/perf pending)` |
| 14 | `.ui.profile.ChangePhoneActivity` | Real `org.telegram.ui.*` Views (`ActionBar`/`Cells`/`RecyclerListView`) | `PARTIAL (needs CI runtime, visual & latency verification)` |
| 15 | `.ui.profile.ChangeBioActivity` | Ported `org.telegram.ui.*` Fragment | `INTERACTION_VERIFIED (Port Smoke only; visual/perf pending)` |
| 16 | `.ui.history.HistoryActivity` | Real `org.telegram.ui.*` Views (`ActionBar`/`Cells`/`RecyclerListView`) | `PARTIAL (needs CI runtime, visual & latency verification)` |
| 17 | `.ui.bookmarks.BookmarksActivity` | EVE `KOTLIN_TG_DS` design system | `PARTIAL (not yet direct `org.telegram.ui.*`)` |
| 18 | `.ui.mistakes.MistakesActivity` | Real `org.telegram.ui.*` Views (`ActionBar`/`Cells`/`RecyclerListView`) | `PARTIAL (needs CI runtime, visual & latency verification)` |
| 19 | `.ui.test.TestActivity` | EVE `KOTLIN_TG_DS` design system | `PARTIAL (not yet direct `org.telegram.ui.*`)` |
| 20 | `.ui.home.ExamTestsActivity` | Real `org.telegram.ui.*` Views (`ActionBar`/`Cells`/`RecyclerListView`) | `PARTIAL (needs CI runtime, visual & latency verification)` |
| 21 | `.ui.result.ResultActivity` | EVE `KOTLIN_TG_DS` design system | `PARTIAL (not yet direct `org.telegram.ui.*`)` |
| 22 | `.ui.result.ResultDetailActivity` | Real `org.telegram.ui.*` Views (`ActionBar`/`Cells`/`RecyclerListView`) | `PARTIAL (needs CI runtime, visual & latency verification)` |
| 23 | `.ui.leaderboard.LeaderboardActivity` | Real `org.telegram.ui.*` Views (`ActionBar`/`Cells`/`RecyclerListView`) | `PARTIAL (needs CI runtime, visual & latency verification)` |
| 24 | `.ui.performance.PerformanceActivity` | Real `org.telegram.ui.*` Views (`ActionBar`/`Cells`/`RecyclerListView`) | `PARTIAL (needs CI runtime, visual & latency verification)` |
| 25 | `.ui.practice.PracticeActivity` | Real `org.telegram.ui.*` Views (`ActionBar`/`Cells`/`RecyclerListView`) | `PARTIAL (needs CI runtime, visual & latency verification)` |
| 26 | `.ui.pyq.PyqActivity` | Real `org.telegram.ui.*` Views (`ActionBar`/`Cells`/`RecyclerListView`) | `PARTIAL (needs CI runtime, visual & latency verification)` |
| 27 | `.ui.admin.AdminActivity` | EVE `KOTLIN_TG_DS` design system | `PARTIAL (not yet direct `org.telegram.ui.*`)` |
| 28 | `.ui.admin.ManageQuestionsActivity` | EVE `KOTLIN_TG_DS` design system | `PARTIAL (not yet direct `org.telegram.ui.*`)` |
| 29 | `.ui.admin.AdminsAndRightsActivity` | Real `org.telegram.ui.*` Views (`ActionBar`/`Cells`/`RecyclerListView`) | `PARTIAL (needs CI runtime, visual & latency verification)` |
| 30 | `.ui.admin.AdminRightsEditActivity` | EVE `KOTLIN_TG_DS` design system | `PARTIAL (not yet direct `org.telegram.ui.*`)` |
| 31 | `.ui.admin.SystemMonitorActivity` | Real `org.telegram.ui.*` Views (`ActionBar`/`Cells`/`RecyclerListView`) | `PARTIAL (needs CI runtime, visual & latency verification)` |
| 32 | `.ui.admin.AdminAnalyticsActivity` | EVE `KOTLIN_TG_DS` design system | `PARTIAL (not yet direct `org.telegram.ui.*`)` |
| 33 | `.ui.admin.SendNotificationActivity` | EVE `KOTLIN_TG_DS` design system | `PARTIAL (not yet direct `org.telegram.ui.*`)` |
| 34 | `.ui.admin.ManageExamsActivity` | EVE `KOTLIN_TG_DS` design system | `PARTIAL (not yet direct `org.telegram.ui.*`)` |
| 35 | `.ui.admin.GeneratedTestsActivity` | Real `org.telegram.ui.*` Views (`ActionBar`/`Cells`/`RecyclerListView`) | `PARTIAL (needs CI runtime, visual & latency verification)` |
| 36 | `.ui.admin.ManagePollsActivity` | Real `org.telegram.ui.*` Views (`ActionBar`/`Cells`/`RecyclerListView`) | `PARTIAL (needs CI runtime, visual & latency verification)` |
| 37 | `.ui.about.AboutActivity` | Real `org.telegram.ui.*` Views (`ActionBar`/`Cells`/`RecyclerListView`) | `PARTIAL (needs CI runtime, visual & latency verification)` |
| 38 | `.ui.settings.SettingsActivity` | Real `org.telegram.ui.*` Views (`ActionBar`/`Cells`/`RecyclerListView`) | `PARTIAL (needs CI runtime, visual & latency verification)` |
| 39 | `.ui.settings.AppearanceActivity` | Thin alias / legacy | `PARTIAL` |
| 40 | `.ui.settings.ThemeActivity` | Real `org.telegram.ui.*` Views (`ActionBar`/`Cells`/`RecyclerListView`) | `PARTIAL (needs CI runtime, visual & latency verification)` |
| 41 | `.ui.settings.LanguageSelectActivity` | Ported `org.telegram.ui.*` Fragment | `INTERACTION_VERIFIED (Port Smoke only; visual/perf pending)` |
| 42 | `.ui.about.ContentDisplayActivity` | Real `org.telegram.ui.*` Views (`ActionBar`/`Cells`/`RecyclerListView`) | `PARTIAL (needs CI runtime, visual & latency verification)` |
| 43 | `.ui.admin.EditAboutActivity` | EVE `KOTLIN_TG_DS` design system | `PARTIAL (not yet direct `org.telegram.ui.*`)` |
| 44 | `.ui.syllabus.SyllabusActivity` | Real `org.telegram.ui.*` Views (`ActionBar`/`Cells`/`RecyclerListView`) | `PARTIAL (needs CI runtime, visual & latency verification)` |
| 45 | `.ui.notifications.NotificationsActivity` | Real `org.telegram.ui.*` Views (`ActionBar`/`Cells`/`RecyclerListView`) | `PARTIAL (needs CI runtime, visual & latency verification)` |
| 46 | `.ui.feedback.FeedbackActivity` | Real `org.telegram.ui.*` Views (`ActionBar`/`Cells`/`RecyclerListView`) | `PARTIAL (needs CI runtime, visual & latency verification)` |
| 47 | `.ui.admin.FeedbackMessagesActivity` | Real `org.telegram.ui.*` Views (`ActionBar`/`Cells`/`RecyclerListView`) | `PARTIAL (needs CI runtime, visual & latency verification)` |
| 48 | `.ui.admin.ManageExistingExamsActivity` | Real `org.telegram.ui.*` Views (`ActionBar`/`Cells`/`RecyclerListView`) | `PARTIAL (needs CI runtime, visual & latency verification)` |
| 49 | `.ui.admin.EditExamActivity` | EVE `KOTLIN_TG_DS` design system | `PARTIAL (not yet direct `org.telegram.ui.*`)` |
| 50 | `.ui.admin.ManageUsersActivity` | EVE `KOTLIN_TG_DS` design system | `PARTIAL (not yet direct `org.telegram.ui.*`)` |
| 51 | `.ui.admin.AppConfigActivity` | EVE `KOTLIN_TG_DS` design system | `PARTIAL (not yet direct `org.telegram.ui.*`)` |
| 52 | `.ui.admin.ActivityLogActivity` | EVE `KOTLIN_TG_DS` design system | `PARTIAL (not yet direct `org.telegram.ui.*`)` |
| 53 | `.ui.admin.emoji.EmojiStudioActivity` | Real `org.telegram.ui.*` Views (`ActionBar`/`Cells`/`RecyclerListView`) | `PARTIAL (needs CI runtime, visual & latency verification)` |
| 54 | `.ui.admin.FlaggedQuestionsActivity` | EVE `KOTLIN_TG_DS` design system | `PARTIAL (not yet direct `org.telegram.ui.*`)` |
| 55 | `.ui.admin.ApiUsageActivity` | Real `org.telegram.ui.*` Views (`ActionBar`/`Cells`/`RecyclerListView`) | `PARTIAL (needs CI runtime, visual & latency verification)` |
| 56 | `.ui.admin.ManageSyllabusActivity` | EVE `KOTLIN_TG_DS` design system | `PARTIAL (not yet direct `org.telegram.ui.*`)` |
| 57 | `.ui.premium.PremiumActivity` | Real `org.telegram.ui.*` Views (`ActionBar`/`Cells`/`RecyclerListView`) | `PARTIAL (needs CI runtime, visual & latency verification)` |
| 58 | `.ui.premium.PaymentCheckoutActivity` | Real `org.telegram.ui.*` Views (`ActionBar`/`Cells`/`RecyclerListView`) | `PARTIAL (needs CI runtime, visual & latency verification)` |
| 59 | `.ui.admin.ManagePremiumActivity` | EVE `KOTLIN_TG_DS` design system | `PARTIAL (not yet direct `org.telegram.ui.*`)` |
