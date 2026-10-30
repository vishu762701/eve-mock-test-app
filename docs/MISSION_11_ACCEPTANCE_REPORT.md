# EVE — Mission 11 Acceptance and Continuation Report

**Date:** 2026-10-30
**Repository:** https://github.com/vishu762701/eve-mock-test-app
**Branch:** `main` only
**Mission 10 checkpoint:** `470c4c7908a8d1e16d68ec6cb9548689cc904ff9`
**Latest audited integration / test repair SHA:** `b384eebf0c1cbbd0acf80ca51d96a06625aa1d8b` (subject to live HEAD recheck)
**Status:** **Final six-job acceptance is decided by the latest exact-SHA GitHub Actions FULL run, not by an outdated checkpoint. Never infer pixel-exact Telegram parity solely from CI.**
**Terminal FULL CI history:** [41028834051](https://github.com/vishu762701/eve-mock-test-app/actions/runs/41028834051) and [41030892301](https://github.com/vishu762701/eve-mock-test-app/actions/runs/41030892301) failed native assertions; [41034705273](https://github.com/vishu762701/eve-mock-test-app/actions/runs/41034705273) failed AndroidTest compilation; the focused next FAST repair is [41035286301](https://github.com/vishu762701/eve-mock-test-app/actions/runs/41035286301). Inspect the next final FULL on its exact SHA.
**Mission 11 verified APK:** **Only a six-job successful final FULL may publish the artifact; inspect the resulting run and compute its ZIP/APK SHA-256 before claiming delivery.**

**Continuity note:** Sections 1–9 contain dated historical checkpoints; their statements that FULL was never triggered or that only one dialog was migrated describe earlier points in time. Section 10 below supersedes those transient status statements. Final evidence must be checked from GitHub Actions at the exact commit.

This document is the G7 resumable ledger and deliberately is **not** a certification that user-visible Telegram parity was achieved. Its own documentation commit advances `main` beyond the implementation SHA above. Check remote HEAD anew before any further write.

## 1. G0 / G1 provenance

- Uploaded archive available independently of owner phone: `Telegrammaster.zip`, 14,794 ZIP entries, SHA-256 `4b65e10b8fc63b4cbcb7377026a5da470782547fc0c0575bf0b71da676028c10`. Actual archive hash was recomputed in the current chat. It has **no** `EVE_MANIFEST.sha256` (see `THIRD_PARTY_NOTICES.md`); do not invent one.
- Repository's upstream source-content audit identifies `DrKLO/Telegram@3de37c8cfb039452be7398909461da6288babf15` via reconstructed Git tree `cc3c6ccf48e4167fdec764dfd04e765043f55093`. That is a **recorded independent repository audit**, distinct from this turn's ZIP SHA check.
- Telegram artwork/code copyright and GPL v2-or-later notices must remain intact. EVE root `LICENSE` is GPL v3; permission, combined distribution and source-availability obligations must be verified before claiming a dependency-complete Telegram renderer is distributable.
- Never transplant Telegram auth, endpoints, `UserConfig`, `ApplicationLoader`, `LaunchActivity`, private keys or account services into EVE.

## 2. Source mapping and classification

| Property | EVE file / implementation | Uploaded Telegram source, revision above | Classification | Caveat |
|---|---|---|---|---|
| Popup row gap and original shadow | `popup_telegram_menu.xml`, `TelegramMenuPopup.kt`, original `popup_fixed_alert4` nine-patch | `ActionBar/ActionBarPopupWindow.java:233–234,1110–1126`; original popup resource | **EXACT atoms / ADAPTED host** | Full popup window and insets not independently matched by device captures |
| Source alert background, width and dark color | `TelegramDialogSourceBackground.kt` | `ActionBar/AlertDialog.java:302–318,489–504,680–693`; `DarkAlertDialog.java:11–19` | **ADAPTED** | EVE still uses AppCompat/Material AlertDialog view hierarchy |
| Dialog action selector and dimensions | `DesignDialogs.kt` | `ActionBar/AlertDialog.java:1137–1215`; `ActionBar/Theme.java:4802–4808` | **ADAPTED** | Android RippleDrawable and 48dp touch-target substitutions, not Telegram's native renderer |
| Full AlertDialog renderer, custom variants, motion and dependence tree | **Not transplanted** | `ActionBar/AlertDialog.java` (2,069 lines), imports `ApplicationLoader`, `UserConfig`, `NotificationCenter`, `LaunchActivity`, `StarsIntroActivity` etc. | **BLOCKED** | Must separate all app/network dependencies, preserve EVE callbacks/security and satisfy licensing. Mere paint/layout substitution cannot be called EXACT. |
| Drawer outside contours and gesture physics | `MainActivity.setupDrawer()`, `DrawerLayout` | `ActionBar/DrawerLayoutContainer.java` | **ADAPTED** | AndroidX DrawerLayout is not Telegram's gesture implementation |
| Seven fallback avatar gradients | `AvatarDrawable.kt` | `ActionBar/ThemeColors.java:161–174`, `Components/AvatarDrawable.java:571–577` | **EXACT color/gradient atoms** | EVE stable-ID hash/initials selection ADAPTED |
| White avatar text | `AvatarDrawable.kt` | `ActionBar/ThemeColors.java:155`, `Components/AvatarDrawable.java:566` | **EXACT text color** | Actual live app rendering not captured yet; native test added |
| Category pill selected BLACK/WHITE role | `EveDesignSystem.kt`, `MainActivity.kt` | No claim of an exact Telegram black/white selected category chip | **ADAPTED owner override** | Live Day/Night device switch needs screenshot evidence |
| Search recent-query model | `HomeSearchHistory.kt` | Telegram search transition is a UI reference, not the EVE exam privacy/data model | **ADAPTED** | Account-scoped local history; still needs lifecycle/IME screenshots |
| Progressive exam numbers | `ProgressiveQuestionStrip.kt`, `QuestionPaletteAdapter.kt` | `Components/FilterTabsView.java:837,855–881`, quint/320ms easing | **EXACT physics atom / ADAPTED choreography** | EVE exam-specific reveal/persistence is not an upstream feature |
| Notification bell | `notification_bell.json`, `MainActivity.kt`, `NotificationStore.kt` | Historical EVE Git blob `bff3b04887e06d0f144a641b8ec5c14f640c4dd4` | **HISTORICAL EVE, not Telegram/Apple EXACT** | Real notification-driven visible playback requires device video/frames |

## 3. G2/G3 sequential commit and exact-SHA FAST ledger

Commits below are after Mission 10, oldest to newest. Some initial FAST failures were subsequently repaired with succeeding commits; never erase or relabel those failures.

| SHA prefix | Purpose | Exact-SHA FAST |
|---|---|---|
| `e9b1688fb4b1` | fix(home): restore Telegram-sourced popup section group gap | SUCCESS [40993750943](https://github.com/vishu762701/eve-mock-test-app/actions/runs/40993750943) |
| `d84790220083` | fix(home): enforce live monochrome category chip palette without Material tint | FAILED [40994192010](https://github.com/vishu762701/eve-mock-test-app/actions/runs/40994192010) |
| `b1b6804327bf` | fix(home): use supported Chip palette API and import theme engine | SUCCESS [40994509860](https://github.com/vishu762701/eve-mock-test-app/actions/runs/40994509860) |
| `285a95288e7e` | feat(home): empty-first account-scoped search with real suggestions and Clear All | SUCCESS [40995030992](https://github.com/vishu762701/eve-mock-test-app/actions/runs/40995030992) |
| `c79eef81198a` | fix(exams): restore compact nested exam outer card with unboxed original book icon | FAILED [40995483203](https://github.com/vishu762701/eve-mock-test-app/actions/runs/40995483203) |
| `7d05ab7b5417` | test(layout): keep full typography scan after adding compact nested exam layout | SUCCESS [40995765838](https://github.com/vishu762701/eve-mock-test-app/actions/runs/40995765838) |
| `731da3c130d6` | fix(test): emphasize question and test title hierarchy while keeping options regular | FAILED [40996172951](https://github.com/vishu762701/eve-mock-test-app/actions/runs/40996172951) |
| `450840b65180` | test(test): assert Mission 11 bold question hierarchy while retaining normal answer choices | SUCCESS [40996541258](https://github.com/vishu762701/eve-mock-test-app/actions/runs/40996541258) |
| `06a83aa2e9f5` | test(native): assert real bold question hierarchy against regular MCQ choices | SUCCESS [40996963194](https://github.com/vishu762701/eve-mock-test-app/actions/runs/40996963194) |
| `04011d784c4f` | fix(test): show selected answer green without correctness feedback | FAILED [40999052554](https://github.com/vishu762701/eve-mock-test-app/actions/runs/40999052554) |
| `dc9b116dd52f` | test(test): assert green selected answer ignores Telegram accent overrides | SUCCESS [40999335922](https://github.com/vishu762701/eve-mock-test-app/actions/runs/40999335922) |
| `b2ada9e0b46e` | fix(test): use theme-accurate monochrome Save and Next primary pill | SUCCESS [40999712634](https://github.com/vishu762701/eve-mock-test-app/actions/runs/40999712634) |
| `7621909e6d53` | feat(test): persist progressive question navigator reveal per attempt | SUCCESS [41000106008](https://github.com/vishu762701/eve-mock-test-app/actions/runs/41000106008) |
| `0161132d7689` | fix(test): preserve previously visited circles when migrating old attempts | SUCCESS [41000516748](https://github.com/vishu762701/eve-mock-test-app/actions/runs/41000516748) |
| `881e559dca6e` | feat(test): animate newly revealed question circles with Telegram quint timing | SUCCESS [41000820055](https://github.com/vishu762701/eve-mock-test-app/actions/runs/41000820055) |
| `0c211024b6aa` | fix(home): play historical bell only for actual notifications at full duration | SUCCESS [41001309117](https://github.com/vishu762701/eve-mock-test-app/actions/runs/41001309117) |
| `c24a967f1bb9` | fix(home): transplant authentic Telegram popup GapView nine-patch shadow | SUCCESS [41001741401](https://github.com/vishu762701/eve-mock-test-app/actions/runs/41001741401) |
| `6a35dbfef6a7` | fix(dialog): retain Telegram phone and both tablet width constraints | SUCCESS [41002151410](https://github.com/vishu762701/eve-mock-test-app/actions/runs/41002151410) |
| `7c9e60516256` | fix(test): render true green selected radio indicator over accessible tinted tile | SUCCESS [41002467067](https://github.com/vishu762701/eve-mock-test-app/actions/runs/41002467067) |
| `71cdc37c5196` | fix(test): refresh green answer and monochrome action on live theme palette changes | SUCCESS [41004314161](https://github.com/vishu762701/eve-mock-test-app/actions/runs/41004314161) |
| `4cf0aec23a6b` | fix(dialog): restore Telegram masked ripple and action geometry atoms | FAILED [41004589084](https://github.com/vishu762701/eve-mock-test-app/actions/runs/41004589084) |
| `b4b811cd6c56` | test(dialog): validate source ripple and destructive-red ordering | SUCCESS [41005007227](https://github.com/vishu762701/eve-mock-test-app/actions/runs/41005007227) |
| `460e093d30f5` | fix(home): restore authorized Admin Dashboard after closing search | SUCCESS [41005412909](https://github.com/vishu762701/eve-mock-test-app/actions/runs/41005412909) |
| `dee1d18bed71` | fix(home): preserve one-time greeting motion through first onStart | SUCCESS [41005807020](https://github.com/vishu762701/eve-mock-test-app/actions/runs/41005807020) |
| `45003d1fb2f7` | fix(home): replay only pending real notification after lifecycle resumes | SUCCESS [41006233440](https://github.com/vishu762701/eve-mock-test-app/actions/runs/41006233440) |
| `17da008e4fb1` | fix(home): avoid bell playback for first historical broadcast backfill | SUCCESS [41006964521](https://github.com/vishu762701/eve-mock-test-app/actions/runs/41006964521) |
| `18d614856349` | fix(home): establish bell sync baseline even for empty server broadcast page | SUCCESS [41007332263](https://github.com/vishu762701/eve-mock-test-app/actions/runs/41007332263) |
| `d8bf05033cc3` | fix(avatar): render genuine Telegram two-stop fallback gradients | SUCCESS [41007723148](https://github.com/vishu762701/eve-mock-test-app/actions/runs/41007723148) |
| `42dc240bf031` | fix(notifications): align inbox empty broadcast sync with Home baseline | SUCCESS [41008114672](https://github.com/vishu762701/eve-mock-test-app/actions/runs/41008114672) |
| `cc5f56d87952` | fix(avatar): use authentic Telegram white initials over source gradients | SUCCESS [41008497392](https://github.com/vishu762701/eve-mock-test-app/actions/runs/41008497392) |
| `408521563efc` | fix(home): refresh public floating-link position on resume, discard stale overlapping UI responses | SUCCESS [41010509160](https://github.com/vishu762701/eve-mock-test-app/actions/runs/41010509160) |
| `f71f74f758d0` | fix(home): accessible community link click, no link launch after drag-release | SUCCESS [41010894448](https://github.com/vishu762701/eve-mock-test-app/actions/runs/41010894448) |
| `ba5d25514843` | fix(home): one admin drag save at a time; rollback failed optimistic coordinates | FAILED [41011240288](https://github.com/vishu762701/eve-mock-test-app/actions/runs/41011240288) — two source-contract assertions expected the previous weaker admin guard |
| `82e5dd1b672f` | test(home): align both contracts to the stronger admin+pending-write guard | SUCCESS [41011542384](https://github.com/vishu762701/eve-mock-test-app/actions/runs/41011542384) |

**Latest implementation FAST:** [41011542384](https://github.com/vishu762701/eve-mock-test-app/actions/runs/41011542384), **SUCCESS**, at `82e5dd1b672f7688f0bd5e468e9c9db163c8f2b5`. This verifies FAST compilation/JVM/backend/Firebase checks, not native device rendering.

## 4. R01–R10 acceptance matrix (G4/G5)

Statuses reflect the **complete owner acceptance criterion**, not simply Kotlin code existence.

| Requirement | Source/code evidence | Acceptance status | Remaining proof |
|---|---|---|---|
| R01 Home selected chip monochrome and single-selection | `EveDesignSystem.kt`, `HomeCategorySelectionNativeTest.kt`, `HomeCategoryChipGroup.kt` | **UNKNOWN** | Live theme toggle and All/Other screenshots at target widths |
| R02 Historical notification bell animation | `MainActivity.kt`, `NotificationStore.kt`, original `notification_bell.json` | **UNKNOWN** | Actual notification event, red dot, frame/video and lifecycle test on Home |
| R03 Telegram menu group divider and popup | `TelegramMenuPopup.kt`, original GapView resource, popup tests | **UNKNOWN** | Normal/night, RTL, placement, clipping and motion screenshots |
| R04 Blank-first account-scoped search and Clear All | `MainActivity.kt`, `HomeSearchHistory.kt` | **UNKNOWN** | Account switch, keyboard/back, Hindi, results/empty real device |
| R05 Compact nested English outer row | `item_exam_dropdown.xml`, relevant adapter/layout tests | **UNKNOWN** | Visually prove outer card present and book icon unboxed |
| R06 Bold question/header vs regular choices | `QuestionAdapter.kt`, `TestActivity.kt`, hierarchy contract/native tests | **UNKNOWN** | Hindi/RTL/large font and actual test screenshot |
| R07 GREEN selected answers, no correctness leak | `TelegramRadioButton.kt`, `TelegramAnswerGreenNativeTest.kt`, theme refresh | **UNKNOWN** | On-device manual selection and restoring answers in both modes |
| R08 Day BLACK/WHITE and Night WHITE/BLACK Save and Next | `TestPrimaryActionAppearance.kt`, native test and `TestActivity.kt` | **UNKNOWN** | On-device enabled/disabled/theme/submit behavior |
| R09 Initially only Q1, persistent progressive question circles | `ProgressiveQuestionStrip.kt`, `TestViewModel.kt`, `QuestionPaletteAdapter.kt` | **UNKNOWN** | Real 1→2→3, resume, rotation, bookmark, rapid navigation |
| R10 Source-timed reveal/chip motion | `QuestionPaletteAdapter.kt`, source-physics tests | **UNKNOWN** | Start/mid/end frames, reduced motion, 60/90/120Hz, RTL |

**Inherited Mission 09/10:** Home drawer's nine rows, owner night black override, logout in Settings, floating link code/admin gate, app palette, theme reveal, search, popup and non-OS confirmations all have related source and tests, but **the present mission SHA has no final six-job native matrix or paired source/EVE visual review**. Mark runtime/pixel parity **UNKNOWN**; for the *complete original Telegram dialog renderer*, classification is **BLOCKED** as above. Do not substitute Mission 10's earlier green FULL for Mission 11 evidence.

## 5. G4 / G5 gaps

- FAST builds compile app and JVM tests; AndroidTest source/native emulator jobs are included in the terminal FULL profile, **not** exercised in a Mission 11 terminal run yet.
- No direct device/emulator control or live signed-in Firebase test account was available in this chat. No screenshots, videos, frame timings, live backend drag authorization, OS navigation comparisons, accessibility traversal, RTL/Hindi font-scale or physical device observations were fabricated.
- The current EVE dialog system has exact source-derived atoms but **is not** `org.telegram.ui.ActionBar.AlertDialog`'s full view/gesture/animation/dismissal implementation. Installing the upstream class as-is would import unrelated Telegram social/account services; do not do so.
- A full renderer integration requires a scoped EVE-only dependency isolation design, licence/release review, native integration tests for every confirmation type and a security audit before any acceptance claim.

## 6. G6 terminal CI / APK

**NOT STARTED**. Do **not** add `[eve-full]` to a documentation or intermediate implementation commit while G4/G5 items above remain unverified. No verified Mission 11 debug APK exists. In particular, the historical Mission 10 APK and its digest do not prove this mission's current implementation.

## 7. G7 exact next checkpoint

1. Re-fetch remote `main` SHA; do not treat this report's last implementation SHA as the live branch head.
2. Verify the latest documentation-only push's exact-SHA FAST run if one exists.
3. Finish EVE-only complete Telegram dialog renderer isolation with original layout/action classes **without importing any Telegram account/network/service state**; determine compatible source/attribution/release obligations. Map all EVE-owned confirmation/alert callers and preserve system permission UI.
4. Obtain full native functional + matched Day/Night RTL/Hindi/phone/tablet evidence for R01–R10, floating link admin/regular user taps and coordinate propagation, profile/drawer, menu, full dialogs and scroll seam. Repair any real failures in serial micro-commits, FAST after each.
5. Only when all G4/G5 requirements are satisfied, create the final `[eve-full]` implementation commit; verify exact SHA and **all six FULL jobs** (backend, Firebase, Android build/lint, gesture emulator, three-button emulator, APK publish) plus published APK SHA-256 and size. Update this report truthfully with HEAD/SHA distinction.

**Uncommitted edits:** None in a local checkout; all referenced implementation edits are GitHub commits. This report is a new GitHub file, not a local uncommitted workspace.

## 8. Subsequent evidence-backed continuation (2026-10-30; do not infer runtime parity)

**Latest implementation SHA before this documentation commit:** `82e5dd1b672f7688f0bd5e468e9c9db163c8f2b5`.

### Floating community link: code-level repairs, G4/G5 pending

- Returning students previously read the singleton's 10-minute cached link position. `MainActivity.onResume()` now requests `refreshFloatingLink(forceRefresh = true)` from the public endpoint. A generation token prevents an older overlapping Home response from overriding the latest displayed coordinates. This does not claim push-based real-time propagation while Home stays open.
- Community-icon `ACTION_CLICK` is now handled by an actual `OnClickListener`, including keyboard/screen-reader activation. The physical tap dispatches `performClick()` only for a short non-drag gesture, retaining the link-open debounce. The parent card has one accessible label and decorative artwork is hidden from separate screen-reader focus.
- Only a verified admin may start a long-press drag. While an admin position write is in progress, further drag starts are inhibited. On a failed write the visual icon rolls back to the previously confirmed coordinates; successful writes retain backend authorization. The failed FAST on `ba5d2551` identified two preexisting exact-string checks for a weaker admin guard; `82e5dd1b` repaired those contract expectations **without weakening the implementation**.
- These source/JVM tests do not prove live administrator identity, backend permission rejection, actual public propagation, Voice/TalkBack dispatch, or physical insets/drag bounds. Those remain **UNKNOWN** until authenticated native execution.

### Verified G1 blocker: Telegram original AlertDialog dependencies

The uploaded source's `TMessagesProj/src/main/java/org/telegram/ui/ActionBar/AlertDialog.java` has **2,069 source lines**. Its imports at lines **59–84** include `ApplicationLoader`, `UserConfig`, `NotificationCenter`, `SharedConfig`, `Browser`, `LaunchActivity`, `StarsIntroActivity`, `RLottieDrawable`, `EffectsTextView` and other app-specific components. It is **not** a self-contained Android dialog widget. EVE's existing `DesignDialogs.kt` and `TelegramDialogSourceBackground.kt` implement authentic background/size/typography/ripple atoms in an **ADAPTED AppCompat renderer**. The unmodified whole original view/interaction/motion renderer is **BLOCKED** pending dependency-isolated EVE integration, licensing/source-distribution validation and full callback/OS-dialog invariants. Never import Telegram authentication/network machinery to resolve its dependencies.

### Independent G4/G5 blocker and terminal gate

The active container provides neither `adb` nor Android `emulator`/`sdkmanager`, and no logged-in owner test device or authenticated admin/student harness is connected here. Intermediate GitHub FAST jobs verify compile/JVM, not run the native emulator suite. The repository prohibits an early `[eve-full]` run while original renderer, paired Telegram/EVE visual states and authenticated runtime acceptance are unresolved. **No Mission 11 six-job terminal FULL or APK SHA verification has occurred.**

**G7 resume action:** Re-fetch remote `main` and the documentation-only push FAST outcome; do not repeat the four floating-link commits. Next, isolate genuine Telegram AlertDialog UI dependencies without copying `UserConfig`/account/service stack, verify full GPL obligations, implement/validate original renderer per confirmation variant. Then obtain matched genuine Day/Night, RTL/Hindi, 320/360/393dp/tablet, gesture/three-button and admin/student runtime evidence for R01–R10 plus inherited Home flows. Only when all blocking acceptance gates are met, initiate `[eve-full]` on a mission-final implementation SHA, inspect six job conclusions and verify the published APK digest. Any missing prerequisite retains **MISSION 11 INCOMPLETE**.

## 9. Recovery continuation — UI-only Telegram dialog port (2026-10-30)

**This continuation is NOT Mission 11 completion.** Baseline `2d2a6b596f806ac3cb361d34a02def0120e6e8e0`; latest implementation `4ca8a959bc8e14f65a3fbeb7eaa69d43c0846605`. No full native acceptance run, no published Mission 11 APK and no screenshot parity proof.

- **Source:** Uploaded `Telegrammaster.zip`, recomputed SHA-256 `4b65e10b8fc63b4cbcb7377026a5da470782547fc0c0575bf0b71da676028c10`, contains 14,794 entries; original `ActionBar/AlertDialog.java` has 2,069 lines, SHA-256 `82bcc4ea545ade397e337c39bac39458ba29f3fa91d166bd6ac70ace2733f8aa`. Upstream `styles.xml:143–157` is a floating transparent `Theme.Dialog`. The ZIP contains no `EVE_MANIFEST.sha256`; do not confuse this with historical repacked archives.
- **Original dependencies:** `AlertDialog.java:59–84,658–714,1407–1412,1801–1804` references `NotificationCenter`, `UserConfig`, `ApplicationLoader`, `LaunchActivity`, `StarsIntroActivity` and `BalanceCloud`. Do not import any Telegram account/service logic into EVE.
- **New narrow UI port:** `TelegramNativeAlertDialog.kt`, `Theme.Eve.TelegramNativeDialog` and `SettingsActivity.setupLogoutSetting()` now have a native, dependency-isolated **ADAPTED** title/message/two-action variant with verified source-derived 9-patch, width, basic typography, 0.5 dim, selector and floating style. It invokes EVE's existing logout callback only on the positive action. `TelegramNativeLogoutDialogTest` and `TelegramNativeDialogThemeContractTest` were added. Native tests are compiled by FAST but have not been run on a device.
- **Still missing:** source-complete custom item/choice/progress/blur/rich-content/animation renderer branches, remaining EVE-owned confirmation callers, matched rendered frames/interaction evidence, account-authenticated admin and student flows, RTL/Hindi/tablets/large text/navigation-mode validation, terminal six-job FULL and APK SHA-256. R01–R10 and Mission 09/10 functional/visual acceptance remain **UNKNOWN** where the earlier table marked them unknown.

| SHA | Microstep | Exact-SHA CI |
|---|---|---|
| `6168181e64d988e2f63c555d2e1f0e65fcb8523a` | Introduce source-backed native message dialog, Settings logout migration, native tests | **FAILED**, [41014797363](https://github.com/vishu762701/eve-mock-test-app/actions/runs/41014797363): Kotlin compiler `ScrollView.LayoutParams` and `background` local-variable shadowing |
| `9dae5d2be6addc66fb18e83540a1bf0186906ece` | Focused Kotlin compilation repair | **SUCCESS**, [41015045732](https://github.com/vishu762701/eve-mock-test-app/actions/runs/41015045732) |
| `a9edb4c67477e35eef6fd2e0d23b8185885e223d` | Restore original floating/translucent Dialog theme; notice and contract | **SUCCESS**, [41015546227](https://github.com/vishu762701/eve-mock-test-app/actions/runs/41015546227); docs workflow [41015546309](https://github.com/vishu762701/eve-mock-test-app/actions/runs/41015546309) SUCCESS |
| `4ca8a959bc8e14f65a3fbeb7eaa69d43c0846605` | Original Activity-liveness safety guard and contract | **SUCCESS**, [41016012951](https://github.com/vishu762701/eve-mock-test-app/actions/runs/41016012951) |

**First executable next action:** finish the remaining genuine Telegram dialog UI branches as EVE-only source-isolated ports, preserve all original dialogs' validation/custom callbacks, and obtain native functional/visual evidence. Do not equate a source-derived partial port with a complete renderer. The connected environment has no Android SDK/adb emulator or connected authenticated test device. FULL remains gated, and no new APK checksum can be claimed. No local checkout or uncommitted work was created; all implementation changes are remote GitHub commits.


## 10. Reconciled final-dialog integration and acceptance gate — 2026-10-30

### Feature-atomic integration ledger (only new functional units; no duplicate work)

| Integration | Main commit / evidence | Outcome |
|---|---|---|
| Exam leave, submit, attempt-limit, offline/status dialogs | \`7f55fbab970898f8954382495eda21a238865de4\`; FAST 41021518466 | FAST green |
| Settings deletion, reauthentication and progress; focused follow-up test repair | \`5d42ee2e\`, \`0ae41eca\`; FAST 41022695588 | FAST green |
| Admin privileged confirmations, feedback replies, delete and maintenance operations | \`07cdb607\`; FAST 41023491272 | FAST green |
| Student report form, reattempt/result flows | \`8e9a09b3\`; FAST 41023895252 | FAST green |
| Home feedback post deletion and Profile progress | \`b39579eb\`; FAST 41024395183 | FAST green |
| Notifications, Premium and flagged-question admin dialogs | \`422c504e\`; FAST 41024781119 | FAST green |
| Users, syllabus non-dismissing radio choice, analytics and feedback moderation | \`95ad8b5d\`; FAST 41025492080 | FAST green |
| Exam editor/manager dialogs, cancellation and destructive action behavior | \`457f8808\`; FAST 41026071694 | FAST green |
| AdminActivity message, item, custom-form and progress dialogs | \`8b839691\`; FAST 41026653653 | FAST green |
| Poll creation custom form | \`e94aaf46\`; FAST 41027093931 | FAST green |
| Debug-only crash trace viewer, preserving private storage and manual Copy Trace | \`8c1a08d6\`; FAST 41028131560 | FAST green |
| Source-derived native window, source notice, device acceptance tests | \`fd52cc85\`; FULL 41028834051 failed two unrelated stale MCQ selected-color assertions | Not accepted |
| Subsequent native regressions and avatar/dialog contracts | \`943e0a40\`; FULL 41030892301 failed the same MCQ assertions | Not accepted |
| Exact green drawable + premultiplied pixel regression | \`ff7bbe08\`; FULL 41034705273 caught test compile syntax error; \`b384eebf\` repairs the syntax | FAST verification pending at report preparation |

**Source-fidelity classification:** The verified original \`Telegrammaster.zip\` is SHA-256 \`4b65e10b8fc63b4cbcb7377026a5da470782547fc0c0575bf0b71da676028c10\` at \`DrKLO/Telegram@3de37c8cfb039452be7398909461da6288babf15\`. EVE's \`TelegramNativeAlertDialog.kt\` is a GPL-derived, **ADAPTED dependency-isolated port**; EVE's selection radio mode, backend callbacks, 48dp accessibility and host composition are **ADAPTED**, not claims that the entire Telegram 2,069-line renderer was copied verbatim. The original source-derived 9-patch and related atoms remain recorded in \`THIRD_PARTY_NOTICES.md\`. Telegram account/network services were not imported.

**R01–R10 and inherited UI requirements:** Implementation and focused source contracts exist; native/emulator execution of those assertions and genuine day/night screen fixtures must be evaluated on the final exact SHA. Successful automated UI tests provide relevant runtime evidence but do **not** by themselves certify pixel-perfect original Telegram animations, device-specific frames, authenticated student/admin operations, or the user's physical phone. Any unsupported subclaim stays \`UNKNOWN\` or \`ADAPTED\` rather than false \`EXACT\`.

**Terminal G6 gate:** On the final \`[eve-full]\` commit, inspect each job separately: backend, firebase-functions, build/lint/JVM, Native UI (gesture), Native UI (three-button), and publish verified debug APK. Require all six jobs successful, an exact-SHA artifact and independently computed APK digest. Earlier failed FULL runs remain failed. The GitHub Actions result is the authoritative, time-sensitive completion record. Never rewrite source tests solely to hide a real regression.
