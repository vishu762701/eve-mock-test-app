# EVE — 2026-10-31 Telegram / Profile / Poll / Category / Bell Mission G7 Checkpoint

**Mission status: INCOMPLETE; do not label this report mission acceptance.**

Repository: `vishu762701/eve-mock-test-app`; branch: `main` only.
Mission instructions: owner conversation dated 2026-10-31, FID-01 through FID-07 / A01–A13.
Baseline initially observed: `e47f3a88885b5f73632472a7bcbcd8b3459a7ddb`.
**Last feature SHA examined:** `f73d0c9fbcb57cd30be438b3c9a0e9a80d3c96ff` (the separate docs-only checkpoint commit advances `main`; re-fetch HEAD before any subsequent write).
No new branches, PRs, force-pushes or production deployments were made as part of this work.

## G0 / G1 verified input and scope

- Governance read from LIVE `main`: `AGENTS.md`, `ENGINEERING_STANDARD.md`, `SOURCE_OF_TRUTH.md`, `docs/EVE_ONE_TIME_CHAT_STARTER.md`, `THIRD_PARTY_NOTICES.md`, prior Mission 06/11 acceptance reports, CI workflows, relevant client/backend implementations.
- User-supplied `Telegrammaster(5).zip` inspected in current container: SHA-256 `4b65e10b8fc63b4cbcb7377026a5da470782547fc0c0575bf0b71da676028c10`; archive CRC/integrity passed. `EVE_MANIFEST.sha256` is ABSENT; never claim it was checked. `THIRD_PARTY_NOTICES.md` separately records exact-content Git tree authentication of this SHA to official `DrKLO/Telegram@3de37c8cfb039452be7398909461da6288babf15` with root tree `cc3c6ccf48e4167fdec764dfd04e765043f55093`. Upstream Git commit/tree checked via GitHub API. Do not infer permissions beyond GPL/third-party notices.
- Original upstream file hashes inspected from ZIP: `Cells/RadioCell.java` Git blob `11dfa01a0734c271cbf0701faceebb73234f8683`; `ProfileActivity.java` blob `7805f9d6d3a216972a18f9e367ad7199f2624555`. See specific per-property maps in `docs/EVE_PROFILE_CATEGORY_PROVENANCE.md` and `docs/EVE_PROFILE_TOOLBAR_PROVENANCE.md`.
- Native source surface inventory from main Git tree: 49 Activity classes, 109 layout XMLs, 326 Android Kotlin/Java files, 109 JVM test files, 40 instrumentation test files (inventory counts are NOT app-wide parity proof).
- `.github/workflows/deploy-worker.yml:3-9,36-52` auto-triggers remote D1 migration + production Worker deployment on `backend/**` pushes. Owner's resume explicitly prohibits production deployment; therefore no bio/presence backend/schema change was pushed. Do NOT bypass this gate.

## Requirement ledger (code presence != native acceptance)

| Requirement | Status | Concrete currently inspected proof | Remaining blocker / acceptance |
|---|---|---|---|
| FID-01 app-wide source-exact Telegram UI/motion | **PARTIAL** | Actual 49 Activity / 109 layout inventory; earlier source-derived components in `app/src/main/java/com/eve/app/designsystem`, provenance notices | No exhaustive caller/property mapping and no matched Telegram/EVE native frames on the physical device. EXACT cannot be asserted for app-wide UI. |
| FID-02 Poll destructive action | **PARTIAL, high-impact bug repaired** | `item_poll_admin.xml:19` runtime `android:text="Delete"`; `PollsAdminAdapter.kt:114-116` runtime label + accessibility + click; `PollDeleteLabelNativeTest.kt` | FAST successful, but Day/Night native inflation/contrast and authorization/concurrent mutation acceptance not executed on device. |
| FID-03 Category selection | **PARTIAL** | `ProfileActivity.kt:246-284` authenticated readback + rollback/inflight guard; `TelegramProfileRadioCell.kt:25-100` source-derived accessible radio row; `activity_profile.xml:437-476`; JVM/native tests and provenance | Telegram `RadioCell.java:55-85,96-108,132-137` is source; EVE **ADAPTED**, 50dp minimum and large-font variable height; native interactions, RTL/font, rotation, failure recovery unverified in device. |
| FID-04 Profile appearance + source motion | **PARTIAL** | `ProfileActivity.kt:124-153` existing gooey scroll; `ProfileGooeyView.kt:160-168` real notch setup; `ProfileActivity.kt:405+` duplicate Admin removed; About removed from Profile XML; Admin editor remains in `AdminActivity.kt:281-284`; `activity_profile.xml:11-16` toolbar Z-shadow removed | Original `ProfileActionsView.java` renderer and its source-exact motion/gestures still not dependency-complete; native matched screenshots/video, notch, overscroll, API fallback, typography and dark-theme shape acceptance missing. No fake online status. |
| FID-05 authenticated Profile data | **PARTIAL** | Phone `ChangePhoneActivity.kt:45-83`; username `ChangeUsernameActivity.kt:158-197`; category `ProfileActivity.kt:246+`; DOB `ProfileActivity.kt:365-402`, `ProfileBirthday.kt`; displayName `ChangeNameActivity.kt:44-86`; client mutation + authenticated GET readback added, regression sources pass FAST | Cross-device/old-client tests UNKNOWN; personal Bio schema/API/renderer absent. Existing backend PUT supports fields only `displayName,dob,category,username,phone` (`backend/src/routes/auth.ts:347-353`). Production auto-deploy constraint blocks safe bio/presence backend changes. Admin multiword **displayName** is bounded by existing 100-character server contract, separate from canonical 5-32 character @username. |
| FID-06 iOS-26 official Home bell | **PARTIAL functional / BLOCKED official asset** | `MainActivity.kt:1444+` explicitly calls bell original EVE asset; tap animation/idempotent navigation `:1528+`, unread preserved in `NotificationStore`, read acknowledgment in `NotificationsActivity`; fast regression repaired | Apple SF Symbols 7/WWDC documentation does not establish permission to embed official animated symbol asset on Android. **Do not call existing Lottie Apple-original** or silently substitute a lookalike. Native timing/event/badge acceptance not executed. |
| FID-07 regression/security/complete six-job FULL | **PARTIAL / final UNKNOWN** | Each repaired intermediate implementation SHA followed by exact-SHA FAST; no altered `backend/**`, Firebase rules, auth or D1 migrations in this session | Full final 6-job run and published APK checksum NOT performed; cannot declare publish-ready or source-perfect. |

## Commit and exact-SHA FAST ledger (chronological)

| Feature / repair commit | Purpose | GitHub Actions run | Outcome |
|---|---|---|---|
| `ce352af81a3d7001d9a06217bbf867fb2f929c73` | Poll runtime Delete label + native test | [41162484036](https://github.com/vishu762701/eve-mock-test-app/actions/runs/41162484036) | FAST GREEN |
| `99ba4e0b9c857a49bcad572af55301f1d27833de` | Remove duplicate Admin and Profile-only About link | [41162824407](https://github.com/vishu762701/eve-mock-test-app/actions/runs/41162824407) | FAST GREEN |
| `0f9625e0be6ca80ee26ea5bdca8b07e407230ec8` | Phone post-save GET readback | [41163139740](https://github.com/vishu762701/eve-mock-test-app/actions/runs/41163139740) | FAST GREEN |
| `028d33de70f6808e525e2ba94df506cf093f07d7` | Username post-save GET + stale-availability rejection | [41163474894](https://github.com/vishu762701/eve-mock-test-app/actions/runs/41163474894) | FAST GREEN |
| `ada9cff6a07d68ca77d69f4eebb19c3fd01cbbca` | Telegram-derived category radio & readback | [41163823349](https://github.com/vishu762701/eve-mock-test-app/actions/runs/41163823349) | FAILED unresolved `minHeight` Kotlin reference |
| `feb9c8887564506fb7b953bfc5c0d2d2b46096ba` | Forward-only property/row-height repair | [41163967661](https://github.com/vishu762701/eve-mock-test-app/actions/runs/41163967661) | FAST GREEN |
| `28e87eff9d255fa6e88fde87cb527c92a3c54899` | Existing EVE bell animation before tap navigation | [41164386549](https://github.com/vishu762701/eve-mock-test-app/actions/runs/41164386549) | FAILED obsolete unit test (tap cannot animate) |
| `72f1b8f03a57c3e7d8837865af7ee789143c3336` | Forward-only update of obsolete test, preserve unread assertions | [41164614528](https://github.com/vishu762701/eve-mock-test-app/actions/runs/41164614528) | FAST GREEN |
| `f7ecf72a9d347e4b985e16a2ab8b91169490b8f2` | DOB strict validation, leap-year age and server readback | [41165590850](https://github.com/vishu762701/eve-mock-test-app/actions/runs/41165590850) | FAST GREEN |
| `14a629726be44bac3381c107324778c9ab919fc7` | Category adaptive large-font row, native tests, provenance | [41165900930](https://github.com/vishu762701/eve-mock-test-app/actions/runs/41165900930) | FAST GREEN; docs [41165900920](https://github.com/vishu762701/eve-mock-test-app/actions/runs/41165900920) GREEN |
| `712137421ab886a192195fb8d9f2521d2b813ca6` | Multiword displayName editor, stale-name caller and server readback | [41166208487](https://github.com/vishu762701/eve-mock-test-app/actions/runs/41166208487) | FAST GREEN |
| `f73d0c9fbcb57cd30be438b3c9a0e9a80d3c96ff` | Remove Profile 4dp elevation+4dp translationZ per original shadow-disabled property | [41166519694](https://github.com/vishu762701/eve-mock-test-app/actions/runs/41166519694) | FAST GREEN; docs [41166519664](https://github.com/vishu762701/eve-mock-test-app/actions/runs/41166519664) GREEN |

FAST GREEN = Android assemble, JVM tests, AndroidTest source compilation, backend and Functions jobs pass. Native navigation emulators, lint FULL and APK publication are skipped in FAST. Neither a native pass nor visual parity is claimed.

## Ranked related findings and blockers

1. **P1 BLOCKED release/data contract:** Personal bio cannot be durably stored without a new authenticated API schema/DTO + D1 migration. `backend/src/routes/auth.ts:347-353` lacks bio, and `.github/workflows/deploy-worker.yml:3-9` auto-deploys on backend push. Owner forbids production deployment in this resume. Do not submit UI-only fake save. Prepare backwards-compatible migration/feature gate and staged rollout for explicit authorization.
2. **P1 UNKNOWN native visual/motion:** Profile source-exact liquid notch motion, action row, avatar transition and backend-authenticated mobile behavior need real device/emulator evidence. Original `ProfileActionsView.java` includes `MODE_MY_PROFILE` and `KEY_SET_PHOTO/EDIT_INFO/SETTINGS` but EVE's three action XML tiles are not the complete renderer.
3. **P1 BLOCKED real presence:** `last_active` alone does not truthfully establish online/offline/multi-device status. A privacy-preserving server presence contract needs work and deploy safety approval; never fake 'online'.
4. **P2 BLOCKED official Apple asset:** Source docs explain Apple SF Symbols 7 Draw/Variable Draw through Apple platform APIs; official exact Android redistribution permission/asset not proved. Preserve original-EVE status.
5. **P2 UNKNOWN system integration:** DOB, phone, username, category and display-name client readback are FAST-tested but have not been tested across two real accounts/devices, process death, native IME/insets or API 24–36. Avoid calling the persistence bugs completely resolved until those tests pass.
6. **P2 INCOMPLETE app-wide scope:** Full inventory exists, but per-callsite original Telegram source/behavior/rendering matrix has not been completed for all 49 Activities / 109 layouts. This is required before claiming FID-01.

## Exact G7 continuation

- At checkpoint preparation, code HEAD: `f73d0c9fbcb57cd30be438b3c9a0e9a80d3c96ff`, all relevant latest FAST jobs GREEN. The docs-only checkpoint commit advances HEAD; ALWAYS re-fetch live `main` and exact-SHA CI before additional write. No local checkout, no uncommitted code or hidden changes; user ZIP was inspected read-only.
- **NEXT SINGLE SAFE ACTION:** Read this G7 and latest main files, then reproduce/inspect the actual Profile source-backed `ProfileActionsView` geometry and dependency closure, building a per-property verified map for the 3 existing EVE actions before any renderer rewrite. Do not duplicate completed Profile role/About, data-readback, category, bell tap or toolbar work.
- Next independent work: safely integrate verified native source-backed Profile action cells/header/motion, then caller-by-caller app-wide UI, preserving auth/feature semantics. Per feature: main HEAD CAS -> coherent commit -> exact-SHA FAST; stop on red and repair forward only.
- **External constraints:** Owner must explicitly approve safe production backend rollout strategy before live bio/presence changes; official Apple icon needs rights/usable Android source. Original native screenshot comparisons and authenticated device sessions are not available from GitHub static tools; mark those acceptance claims UNKNOWN until tested.
- **Final FULL:** Do not append `[eve-full]` now. Only once FID-01..07 / A01..A13 are implemented, native-verified and all blockers resolved, run last SHA's six-job FULL (build/lint, gesture, 3-button, backend, Firebase Functions, APK publication), then compute real published APK SHA-256. Current APK checksum: **NOT AVAILABLE**. Mission remains **INCOMPLETE**.
