# EVE Mission 01 V3 — Source/implementation/verification matrix

This matrix documents the EVE repository baseline SHA 6be58114a1d9531e1e357fd3e342b8ac5bc9ed4a and Telegram ZIP SHA-256 `0785365538ce05cf5bdf44a15d8fd5f5219f8193db0a2d124af9665fc63fd7b3`. Reference archive is a ZIP with no embedded Git commit identity: **do not invent an exact Telegram upstream revision**. Statuses are evidence levels, not promises. Archive `LICENSE` is GPL-2.0 and `README.md` requires own branding and corresponding-source compliance. App `main` commits must pass both native navigation UI suites, unit/lint, backend/functions before publishing a debug APK.

## Typography (source units vs EVE accessibility)

| Component/role | Telegram archive source (relative to TMessagesProj/src/main) | EVE paths / actual state | Source-fidelity status |
| --- | --- | --- | --- |
| ActionBar no-subtitle title | `java/org/telegram/ui/ActionBar/ActionBar.java:1443-1462` | `res/values/telegram_typography.xml`, phone landscape/tablet qualifiers; result/profile/activity headers | IMPLEMENTED, source 20dp/18dp converted to sp, style not identical ActionBar container |
| Paired title/subtitle | `java/org/telegram/ui/ActionBar/ActionBar.java:1445-1458` | `activity_result.xml`, `activity_result_detail.xml`, ResultActivity/ResultDetailActivity | IMPLEMENTED, 18/14sp phone, 20/16sp tablet; runtime flow source-mapped |
| Medium face selector | `java/org/telegram/messenger/AndroidUtilities.java:262-270`, `assets/fonts/rmedium.ttf` | FontManager, `res/font/telegram_roboto_medium.ttf`, matching Google Apache-2.0 notice | IMPLEMENTED, exact font bytes source verified by SHA in unit test |
| Dialog title and message | `java/org/telegram/ui/ActionBar/AlertDialog.java:788-795,849-854` | `telegram_typography.xml`, `design_widgets.xml`, feedback dialogs | IMPLEMENTED, 20/16sp, EVE Material dialog structure retained |
| Dialog action text | `java/org/telegram/ui/ActionBar/AlertDialog.java:1112-1215` | DialogAction styles, DesignDialogs | IMPLEMENTED 16sp medium, original EVE button geometry retained |
| Settings label/detail | `java/org/telegram/ui/Cells/TextDetailSettingsCell.java:40-55` | shared Telegram.SettingsLabel/.SettingsDetail, mapped settings list | PARTIAL: 16/13sp source metric, remaining callers/containers unverified |
| Conversation-list analog | `java/org/telegram/ui/Cells/DialogCell.java:1252-1254`, Theme.java:7812,7897 | `item_exam.xml`, `item_history.xml`, `item_feedback_post.xml`, `ExamAdapter.kt` | IMPLEMENTED 17/16/12sp mapping, EVE exam row layout retained |
| Tabs | `java/org/telegram/ui/Components/ScrollSlidingTextTabStrip.java:549-554` | `DesignSegmentedTabs.kt`, `DesignType.kt`, `design_type.xml` | IMPLEMENTED 15sp medium, indicator animation/track not source-exact |
| Menu item | `java/org/telegram/ui/ActionBar/ActionBarMenuItem.java:493-504` | `popup_telegram_menu.xml`, TelegramMenuItem shared style | IMPLEMENTED 16sp, EVE menu surfaces/transitions retained |
| Search input | `java/org/telegram/ui/ActionBar/ActionBarMenuItem.java:1530-1532` | `activity_main.xml` search field, styles | IMPLEMENTED 18sp, control mechanics not exact Telegram search |
| Shared body + secondary | `java/org/telegram/ui/ActionBar/ThemeColors.java:100,197`, night.attheme foreground | `design_type.xml`, `telegram_colors.xml` Day/Night | IMPLEMENTED token mapping; runtime external text setters and 95-layout exhaustive review PENDING |
| Unmapped EVE score, timer, palette, question options | No exact Telegram exam component | score, timer, answer controls remain EVE | NO EXACT TELEGRAM COUNTERPART — preserve accuracy and source-authoritative scoring |

## Icon actions — source candidates only, not artwork import

The following source paths and Java call sites were searched and read from the supplied archive. Most assets are GPL-covered as part of the Telegram repository; none was copied into EVE. The original EVE actions, resource IDs, dimensions, accessibility labels and animation triggers remain unchanged.

| EVE icon / actual caller | Verified Telegram source asset / call site | Status |
| --- | --- | --- |
| `ic_camera` / Profile photo | `drawable-hdpi/msg_camera.webp` — ImageUpdater.java:278 | CANDIDATE_NOT_IMPORTED |
| `ic_copy` / Copy EVE ID | `drawable-hdpi/msg_copy.webp` — BusinessLinksActivity.java:395 | CANDIDATE_NOT_IMPORTED |
| `ic_download` / Syllabus download | `drawable-hdpi/msg_download.webp` — ChatActivity.java:10377 | CANDIDATE_NOT_IMPORTED |
| `ic_info` / About/API info | `drawable-hdpi/msg_info.webp` — ChatActivity.java:31892 | CANDIDATE_NOT_IMPORTED |
| `ic_link` / Admin banner link | `drawable-hdpi/msg_link.webp` — ChatActivity.java:40482 | CANDIDATE_NOT_IMPORTED (Telegram action copies link; EVE edits link) |
| `ic_logout` / Logout | `drawable-hdpi/msg_leave.webp` — SettingsActivity.java:342 | CANDIDATE_NOT_IMPORTED; verified Telegram LogOut call site |
| `ic_send` / Send notification | `drawable-hdpi/msg_send.webp` — ShareAlert.java:2359 | CANDIDATE_NOT_IMPORTED (same send action, different domain) |
| `ic_back`, `ic_more_vert`, `ic_search`, `ic_delete`, `ic_share`, `ic_close`, `ic_bookmark`, `ic_bell`, `ic_bell_off`, `ic_notification`, `ic_settings`, `ic_edit`, `ic_pin` | Source references in `telegram-icon-source-registry.tsv`, 13 EVE-owned drawable vectors | IMPLEMENTED EVE-owned art, **NOT TELEGRAM-EXACT** |
| Exam-domain performance, score, grade, syllabus, PYQ and question icons | No source-equivalent verified | Preserve semantics, mark NO EXACT COUNTERPART after true source call-site audit |

## Source and compliance inventory

- **Copied Telegram-owned code or icon artwork in EVE:** **NONE VERIFIED**; no GPL import release can be claimed.
- **Separately licensed Google Roboto Medium:** present and attributed under Apache-2.0; source-matched font SHA pinned in `TelegramTypeSourceRolesTest`.
- **Licensing blocker for original Telegram bitmap/vector import:** GPLv2 applicability for exact selected art, compatible licensing for a combined Android APK and third-party linked dependencies, corresponding-source deliverable, copyright and third-party notices, and distribution rights require a lawful plan. User has authorized publishing relevant code, but a mere public repository does not by itself demonstrate full compliance. Until resolved, **do not distribute GPL-derived APKs**.
- Avoid Telegram launcher/logo, reuse only EVE-owned branding. Separate EVE dynamic / system icons and Firebase notification small icons from action icon surface inventory.

## Mission status and CI prevention

- Typography central defaults and source-mapped subset: IMPLEMENTED; comprehensive visual runtime parity across all 95 screens still UNVERIFIED.
- Icon art: 13 EVE-owned replacements; 7 new genuine-source candidates mapped in registry; 71 total tracked (historical inventory). Most remain UNMAPPED or NO EXACT COUNTERPART; exact Telegram art NOT IMPLEMENTED.
- CI V3: Android unit-test source compilation, AAPT resources, lint/JVM checks now run in build job before **both** native emulator jobs (`native-ui.needs: build`); the full APK publication gate still depends on build, native-ui, backend and firebase-functions. Release signing fails closed without real credentials. History of older red runs and latest final green check must be tied to their own SHAs, not assumed from this matrix.
- No local Android Gradle/environment build was performed via connected GitHub APIs. CI source and visual screenshot evidence will be evaluated only for the exact final `main` SHA.

## Acceptance gaps

PENDING: per-screen/RTL/fontScale/native screenshot comparison, remaining Telegram-specific icon/state/animation sources, original artwork GPL compatibility and corresponding-source distribution checks, caller-by-caller UI style overrides, full 71 icon review, and final SHA CI proof. Do not call the mission COMPLETE until gaps close or the user explicitly approves narrower scope.
