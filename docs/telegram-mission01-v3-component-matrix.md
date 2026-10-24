# EVE Mission 01 V3 — Source/implementation/verification matrix

This matrix documents the EVE repository baseline SHA 6be58114a1d9531e1e357fd3e342b8ac5bc9ed4a and Telegram ZIP SHA-256 `0785365538ce05cf5bdf44a15d8fd5f5219f8193db0a2d124af9665fc63fd7b3`. Reference archive is a ZIP with no embedded Git commit identity: **do not invent an exact Telegram upstream revision**. Statuses are evidence levels, not promises. Archive `LICENSE` is GPL-2.0 and `README.md` requires own branding and corresponding-source compliance. App `main` commits must pass both native navigation UI suites, unit/lint, backend/functions before publishing a debug APK.

> **Current authoritative delta (supersedes historical V3 baseline below).** The repository now has root `LICENSE` (GPL-3.0-or-later distribution intent), full upstream Telegram GPL notice, and `THIRD_PARTY_NOTICES.md`. Some Telegram-owned static action WebPs are already imported with per-density SHA-256 regression tests; the mutable reference is `docs/telegram-icon-source-registry.tsv`, not the older candidate-only table below. The remaining EVE-specific graphics do not automatically have Telegram counterparts. The repo's FAST/FULL split reserves emulators for a completed mission, not every intermediate push. Source distribution and third-party SDK license compatibility still require verification before production publication. This historical file must not be treated as a ban on all GPL-compliant Telegram imports or a claim that currently imported assets do not exist.

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

## Icon actions — verified static source imports and remaining candidates

The following source paths and Java call sites were searched and read from the supplied archive. Twenty-seven tracked action icons now use original Telegram GPL-covered static WebP art, with source-density hashes pinned by regression tests; this is not a claim of Telegram-exact animation, tint, or full-screen fidelity. Existing EVE resource IDs and actions are retained. Some other icons remain EVE-owned redraws or unmapped. This section supersedes the historical candidate-only statuses.

| EVE icon / actual caller | Verified Telegram source asset / call site | Status |
| --- | --- | --- |
| `ic_camera` / Profile photo | `drawable-hdpi/msg_camera.webp` — ImageUpdater.java:278 | OFFICIAL_TELEGRAM_GPL_ART_IMPORTED; original static WebP, source bytes pinned |
| `ic_copy` / Copy EVE ID | `drawable-hdpi/msg_copy.webp` — BusinessLinksActivity.java:395 | OFFICIAL_TELEGRAM_GPL_ART_IMPORTED; original static WebP, source bytes pinned |
| `ic_download` / Syllabus download | `drawable-hdpi/msg_download.webp` — ChatActivity.java:10377 | OFFICIAL_TELEGRAM_GPL_ART_IMPORTED; original static WebP, source bytes pinned |
| `ic_info` / About/API info | `drawable-hdpi/msg_info.webp` — ChatActivity.java:31892 | OFFICIAL_TELEGRAM_GPL_ART_IMPORTED; original static WebP, source bytes pinned |
| `ic_link` / Admin banner link | `drawable-hdpi/msg_link.webp` — ChatActivity.java:40482 | OFFICIAL_TELEGRAM_GPL_ART_IMPORTED; original static WebP, source bytes pinned (Telegram action copies link; EVE edits link) |
| `ic_logout` / Logout | `drawable-hdpi/msg_leave.webp` — SettingsActivity.java:342 | OFFICIAL_TELEGRAM_GPL_ART_IMPORTED; original static WebP, source bytes pinned; verified Telegram LogOut call site |
| `ic_send` / Send notification | `drawable-hdpi/msg_send.webp` — ShareAlert.java:2359 | OFFICIAL_TELEGRAM_GPL_ART_IMPORTED; original static WebP, source bytes pinned (same send action, different domain) |
| `ic_back`, `ic_more_vert`, `ic_search`, `ic_delete`, `ic_share`, `ic_close`, `ic_bookmark`, `ic_settings`, `ic_edit` | Source archive WebP assets listed in `telegram-icon-source-registry.tsv` | OFFICIAL_TELEGRAM_GPL_ART_IMPORTED; source bytes pinned, static only |
| `ic_bell`, `ic_bell_off`, `ic_notification`, `ic_pin` | EVE-owned generic redraws; no proven identical Telegram source glyph | EVE_ORIGINAL_REDRAW_NOT_SOURCE_EXACT |
| Exam-domain performance, score, grade, syllabus, PYQ and question icons | No source-equivalent verified | Preserve semantics, mark NO EXACT COUNTERPART after true source call-site audit |

## Source and compliance inventory

- **Copied Telegram-owned icon artwork in EVE:** 27 tracked icon resource IDs use imported static Telegram GPL art. Existing derived theme behavior is separately attributed in `THIRD_PARTY_NOTICES.md`. This does **not** establish whole-APK legal compliance or permission to publish a production release.
- **Separately licensed Google Roboto Medium:** present and attributed under Apache-2.0; source-matched font SHA pinned in `TelegramTypeSourceRolesTest`.
- **Production distribution blocker:** Telegram artwork has already been imported under the repository's GPL-3 licensing plan. Complete corresponding-source availability, dependency/SDK compatibility, applicable copyright notices and production distribution obligations still require audit. A public repository and bundled license texts alone do not prove compliance. **Do not distribute a production GPL-derived APK until that audit passes.**
- Avoid Telegram launcher/logo, reuse only EVE-owned branding. Separate EVE dynamic / system icons and Firebase notification small icons from action icon surface inventory.

## Mission status and CI prevention

- Typography central defaults and source-mapped subset: IMPLEMENTED; comprehensive visual runtime parity across all 95 screens still UNVERIFIED.
- Icon art: 27 source-byte-matched Telegram static icons, 4 EVE-owned redraws, and 40 documented semantic mismatches among 71 tracked icon IDs; zero remain pending source inventory audit. Static source art does not prove icon animation or exact caller-state/tint fidelity.
- CI V3: Android unit-test source compilation, AAPT resources, lint/JVM checks now run in build job before **both** native emulator jobs (`native-ui.needs: build`); the full APK publication gate still depends on build, native-ui, backend and firebase-functions. Release signing fails closed without real credentials. History of older red runs and latest final green check must be tied to their own SHAs, not assumed from this matrix.
- No local Android Gradle/environment build was performed via connected GitHub APIs. CI source and visual screenshot evidence will be evaluated only for the exact final `main` SHA.

## Acceptance gaps

PENDING: per-screen/RTL/fontScale/native screenshot comparison, Telegram-specific icon state/animation and caller runtime fidelity, GPL compatibility and corresponding-source distribution checks, and caller-by-caller UI style overrides. All 71 icon IDs have inventory classifications; this does not establish exact runtime parity. Final full CI must be checked on the current SHA. Do not call the mission COMPLETE until gaps close or the user explicitly approves narrower scope.

## GPL v2-or-later compatibility path (verified upstream statement)

The official Telegram Apps source page (<https://telegram.org/apps/>) describes its Android source as **GPL version 2 or later**, whereas the supplied archive's root `LICENSE` contains the full GPL v2 text. The Apache Software Foundation licensing FAQ (<https://www.apache.org/foundation/license-faq>) says Apache-2.0 is not compatible with GPLv2-only, but is compatible with GPLv3. This matters because EVE depends on AndroidX, Material and other third-party packages; their exact notice/license expressions must be inventoried for the **combined work**, rather than assuming that merely making the repository public solves linking. A permitted GPLv3-or-later route may exist for original Telegram artwork, **subject to verifying that each selected icon has that grant, all EVE code can be distributed accordingly, dependencies permit combination, the corresponding source and notices are complete, and no Firebase signing keys/API secrets enter the source package**. The user has authorized complying with licenses, but this audit has not independently established rights to relicense every non-owned EVE dependency. Thus no Telegram GPL icon is shipped in the APK at this checkpoint. GPL-free EVE-owned glyphs and already Apache-licensed source-matched Roboto fonts remain integrated.

## Actual CI failure history and V3 repairs

| Exact run | First root cause | Corrective changes | Status interpretation |
| --- | --- | --- | --- |
| `39885169104`, `39885465532` | Native `LockedFoundationTest.defaultWidgetsResolveTheLockedRootInBothThemes`: expected hard-coded black (-16777216), got source-derived Telegram ink (-15065823) | Semantic `telegram_type_primary` assertion in both Light/Dark; preflight tests now assert token resolution, not literal black | Historical failures only; newer SHA needed |
| `39885836727` | Kotlin `TelegramResultSubtitleSourceTest.kt:20:53` attempted undeclared `attemptDate` inside test assertion | Test checks `Attempted on ` source text; compile of both app and unit-test sources is now a mandatory early gate | Historical failure only |
| `39889074470` | New host Python CI regression test incorrectly matched a backslash-prefixed GitHub expression | Test now matches actual `if: ${{ success() }}` via concatenated string without a backslash | New regression acknowledged; repaired main-only |
| V3 prevention | Parallel native emulators previously ran despite Android compile errors | `native-ui.needs: build` and Android job sequence: resources/Kotlin+unit compile → lint/JVM tests → **release signing refusal dry-run** → assembleDebug/assembleDebugAndroidTest → both nav-mode emulators → fully gated APK | Exact last-SHA job evidence must still be inspected |

The `assembleRelease --dry-run` check uses `env -u` for all 4 release signing variables. It must fail for the **specific GradleException** from the release signing block, not succeed and not fail with some unrelated Gradle/SDK error. It does not create a release APK; no production signing material is exposed.

## Additional exam/admin list-row source mapping

Three more real EVE XML surfaces use the existing source-traced `DialogCell.java:1252-1254` and `Theme.java:7812,7897` roles. `item_syllabus.xml`: exam name `ListTitle` 17sp, PDF filename `ListPreview` 16sp; filename middle ellipsis preserved. `item_user_admin.xml`: display name 17sp, email preview 16sp (end ellipsis), membership/activity timestamps 12sp. `item_admin_banner.xml`: banner title 17sp, upload metadata 12sp, optional link preview 16sp. All new XML bindings carry `eve_telegram_type` tag so published legacy typography cannot override the proven Roboto medium or Day/Night ink. Matched ResultAndSecondaryScreensTypographyTest assertion and a focused XML regression test are included in the same commit. No admin authorization, upload/download callback, badge semantic status or data formatting was changed. The source list metrics are used as an EVE cell adaptation, **not a claim that the whole card geometry is Telegram-exact**.


## Seven further Telegram icon assets — exact density copies

EVE `ic_camera`, `ic_copy`, `ic_download`, `ic_info`, `ic_link`, `ic_logout`, `ic_send` now use the corresponding original Telegram Android `msg_camera`, `msg_copy`, `msg_download`, `msg_info`, `msg_link`, `msg_leave`, `msg_send` WebP density variants. The original XML vectors for these seven EVE IDs are removed. `TelegramAdditionalSourceIconAssetsTest` checks exact source SHA-256 for 28 images; existing EVE icon call-sites, tints, action handlers and accessibility remain. The previous CANDIDATE_NOT_IMPORTED status for these seven is historical. This migrates artwork only, not Telegram animations or business flows. Across both source-import phases there are now 10 source-exact UI icons; other semantic counterparts and full RTL/screenshot comparison remain pending. `LICENSE` and `THIRD_PARTY_NOTICES.md` document GPL inheritance and production-distribution review requirements.
