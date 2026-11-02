# EVE Telegram final mission checkpoint

> Active continuation: see the latest entry below and TELEGRAM_REMAINING_REQUIREMENTS.md. Whole mission is PARTIAL; historical next-action paragraphs are superseded by the current source/CI state.

Status: PARTIAL. This is a continuation record, not screen acceptance.

## Live verified implementation pointer — 2026-11-02

Latest verified implementation main: **5bf15f8444dd65d5e947b43a538404db701a500f**.
Official Telegram rechecked after implementation: **3de37c8cfb039452be7398909461da6288babf15**.
Reconcile this pointer with the carrying implementation commit and live main/CI on restart.

| Scoped delivery | Commit | Exact-SHA FAST |
|---|---|---|
| A3.2 source camera glass | 7bb581b1b95d7e4e79489db77dfa50c3f9b92b6a | [41392841830](https://github.com/vishu762701/eve-mock-test-app/actions/runs/41392841830) SUCCESS |
| A3.4/A3.6 scanner lifetime/results | 2e5655911e0755a0b3a1892a15d4bafd683631dd | [41394531948](https://github.com/vishu762701/eve-mock-test-app/actions/runs/41394531948) SUCCESS |
| C1-C3/C5 confirmed rights/entries | 40a7bb30fcc2bbb3468d482a0ec6a9e4633e811b | [41395975426](https://github.com/vishu762701/eve-mock-test-app/actions/runs/41395975426) SUCCESS |
| A1 ready-frame item appearance | 66574962d7dc1eb484ec81fb8440112d0279fc5d | [41397993734](https://github.com/vishu762701/eve-mock-test-app/actions/runs/41397993734) SUCCESS |
| A1 card/caption and A4 export isolation | 3b2245bc0743ebd45d89e708eb6a899de0cb7e0a | [41402360574](https://github.com/vishu762701/eve-mock-test-app/actions/runs/41402360574) SUCCESS |
| A3 scanner controls/title | e0963d7dcdfec04bcbe90f56dea26246b549e93b | [41403654857](https://github.com/vishu762701/eve-mock-test-app/actions/runs/41403654857) SUCCESS |
| Profile avatar resource lifecycle | 66f3eea0ad5ffae629e2e2cf5cd8d5f78f508861 | [41405867198](https://github.com/vishu762701/eve-mock-test-app/actions/runs/41405867198) SUCCESS |
| Poll Android response confirmation | 14c9e61be42fd7b969d5788b9ddf37d406df9ce4 | [41406623011](https://github.com/vishu762701/eve-mock-test-app/actions/runs/41406623011) SUCCESS |
| Poll Worker atomic status/audit | a8433b6484b7141f881aa5fb271808703c042302 | [41407181451](https://github.com/vishu762701/eve-mock-test-app/actions/runs/41407181451) SUCCESS |
| D4 live filter callbacks | 936e93aae0273daea08ea0e14fd6f9538d60f7b7 | [41409105317](https://github.com/vishu762701/eve-mock-test-app/actions/runs/41409105317) SUCCESS |
| Seven Home menu actions, first batch | a6c0e042f58135d234b450e50c3c5f96ce38757c | [41411244614](https://github.com/vishu762701/eve-mock-test-app/actions/runs/41411244614) SUCCESS |
| Seven Home menu labels/source assets, second batch | bc139bd3b2cc236d20d399e64e41531bf812d269 | [41413589407](https://github.com/vishu762701/eve-mock-test-app/actions/runs/41413589407) SUCCESS |
| Confirmed Home exam-list contract | e1885471f913d7499116f9e4fe6fa1a6ae31d5bf | [41457197984](https://github.com/vishu762701/eve-mock-test-app/actions/runs/41457197984) SUCCESS |
| Shared dismiss policy/question form retention | 412edc6ed80ca867635cbcf8cee06e56196728f4 | [41462667732](https://github.com/vishu762701/eve-mock-test-app/actions/runs/41462667732) SUCCESS |
| C5 selected question-list request ownership | 03c07dc45031b9c2361db53e7ba650dca729bdce | [41466628937](https://github.com/vishu762701/eve-mock-test-app/actions/runs/41466628937) SUCCESS |
| Confirmed exam rename contract/form | 196c59d55b9710fcfb99bb1f2b4e8629f6bb9aee | [41469072072](https://github.com/vishu762701/eve-mock-test-app/actions/runs/41469072072) SUCCESS |
| Profile editor draft account restoration | 2212767c4de9e0d23edd4439ff5bba0ab85215a3 | [41472089611](https://github.com/vishu762701/eve-mock-test-app/actions/runs/41472089611) attempt 2 SUCCESS |
| Username source role/localized accessible status | 372b108a0b1b1c957566c53eeb7fd69b139e7a3e | [41475078791](https://github.com/vishu762701/eve-mock-test-app/actions/runs/41475078791) SUCCESS |
| Shared question-response integrity | 381a76a610a3f1847f59a4c48939d7f2e4e6aea8 | [41479914039](https://github.com/vishu762701/eve-mock-test-app/actions/runs/41479914039) SUCCESS |
| Category live-palette repair (owner exception) | d2b649f5bac8f4b86549ff854385bafbf3d97c6a | [41481845128](https://github.com/vishu762701/eve-mock-test-app/actions/runs/41481845128) SUCCESS |
| Server question mutation/audit confirmation | 5bf15f8444dd65d5e947b43a538404db701a500f | [41523395700](https://github.com/vishu762701/eve-mock-test-app/actions/runs/41523395700) SUCCESS |

Each FAST passed Android build/JVM/AndroidTest source compilation, all four ABI
builds, pinned Rust security/C API, backend and Firebase. Historical scanner-batch docs 41403654877
and verbatim-import 41403654789 SUCCESS. Native navigation and publication skipped;
no protected verified APK artifact/checksum. Verified scanner batch local: 757 JVM tests, 243 Rust tests,
288 backend tests, 19 Functions tests and 59 CI helper tests pass; lint 0 errors /
1646 warnings. Source inventory 129 entries + 4 verbatim popup assets match pinned
Git/EVE bytes. Local x86_64 APK package com.eve.app and all four ELF libraries'
LOAD alignment 0x4000 verified, not a final published APK or screen acceptance.

Whole requirement ledger remains TELEGRAM_REMAINING_REQUIREMENTS.md. These scoped
scoped engineering increments are MET at source/host/FAST integration level, all
ADAPTED. Full A1/A2/A3/A4/Profile/Admin/Home/student/exam/theme fidelity remains
NOT MET. Physical camera/GPU/reference/device matrix and human GPL review remain
BLOCKED/unavailable; missing gift/service data is not fabricated. Strict personal-qr
acceptance still rejects acceptance. Mission-final protected FULL is not eligible.
Current owner correction: STOP QR-only iteration. QR active-path audit is
QR_ACTIVE_PATH_RECONCILIATION.md; only proven critical QR functional defects justify
new QR changes. First unfinished implementation is outside QR: Profile resource
lifecycle (latest entry below), then concrete Admin/Home/Exam/shared requirements.
Device/reference absence does not authorize speculative QR refinement. No production deployment/migration, signing,
release variable, payment, scoring or backend authority changed. P2 historical bare
SDK35 shutdown exception remains UNKNOWN despite green JUnit. No new development
owner decision is required; production/license/distribution decisions stay separate.


## Recovery (2026-11-01)

Fetched live `main`: `1488ae8a9b3505cfd9e11befa85e3dd3cc5d0a39`; clean checkout.
FAST [41330630656](https://github.com/vishu762701/eve-mock-test-app/actions/runs/41330630656)
succeeded at that SHA (build, backend, Firebase; native suites/publication skipped).
Official HTTPS Git fetch and `ls-remote` identify Telegram HEAD
`3de37c8cfb039452be7398909461da6288babf15`, root tree
`cc3c6ccf48e4167fdec764dfd04e765043f55093`. Existing compatible pin retained.
Its tlottie gitlink is `31f1b542f88e7b4be9a01e749920d857535fc715`.
No branches, resets, production calls, deployment or migration are authorized here.

## Requirement ledger

| ID | Requirement | Current code | Upstream reference | Dependencies | Status | Verification evidence |
|---|---|---|---|---|---|---|
| R1 | Recover current state and preserve successful work | main/checkpoints/history | pinned Git objects | GitHub access | MET | fetched main, inspected latest exact-SHA FAST and checkpoint reports |
| R2 | Complete QR scene rendering | TelegramQrSceneView/QrCodeLayer | QrShareView/QrRender | scene/blur/motion | NOT MET | existing source maps; complete scene and matched runtime absent |
| R3 | Animated emoji selection, restoration, lifecycle | QrEmojiPattern/UsernameCardActivity | QrShareView/QrEmojiPanel | EVE catalogue, native drawable | NOT MET | existing tests; picker/rendering differences remain |
| R4 | Gifts against actual authorized data | no demonstrated EVE gift service | QrShareView premium/gifts | real entitlements | BLOCKED | do not fabricate inventory or payments |
| R5 | Scanner complete motion/lifecycle/permissions | EveQrLiveScannerActivity | QrScanOverlay | CameraX/ZXing | NOT MET | geometry adapter exists; morph and full device matrix absent |
| R6 | QR export correctness | QrGalleryWriter/UsernameCardActivity | QrShareActivity | MediaStore/FileProvider | UNKNOWN | historical PNG/publication/rollback tests; current runtime not rerun |
| R7 | Profile complete source subsystem | ProfileActivity/actions/gooey/popup adapters | ProfileActivity/ProfileActionsView/ProfileGooeyView | original parent/renderer | NOT MET | fidelity contract OPEN; preserve no Bio row |
| R8 | All Admin functions with source navigation/rows | admin activities + Telegram pager | PagerSlidingTabStrip/FilterTabsView/cells | Worker authorization | NOT MET | historical bound-page test; full action/device matrix absent |
| R9 | Home/navigation complete | MainActivity/home adapters | ActionBar/menus/drawer/tabs | discovery/filter contracts | NOT MET | existing atoms; whole flow source/runtime acceptance absent |
| R10 | Remaining student screens complete | settings/history/etc | shared Telegram components | existing business actions | UNKNOWN | whole-screen audit/runtime acceptance remains |
| R11 | Mock Test/Result preserve correctness and integrate Quiz UI | TestActivity/QuestionAdapter/ResultActivity | quiz/radio components | authoritative server scoring | UNKNOWN | no scoring changes authorized; full UI evidence remains |
| R12 | Theme/motion/RTL/accessibility/device matrix | shared themes/motion | source tokens/physics | real devices 60/90/120Hz | NOT MET | Home drawer Night #000000 exception retained; matrix incomplete |
| R13 | Native engine verified, existing failures resolved | vendored tlottie/JNI/CMake and original backdrop C++ | official tlottie gitlink/image.cpp | Rust/NDK/C++ | NOT MET | Rust fixture repaired and 243 tests green; native host raster passes; all-ABI/device/final APK acceptance remains |
| R14 | Preserve security and production-data invariants | existing auth/Worker/D1/Firebase unchanged | EVE contract | no deployments/data changes | MET | scoped diff review: no backend/auth/scoring/data/permissions/link-validation changes; bounded native input adapter; no production test requests introduced |
| R15 | Provenance and distribution safeguards | manifests/notices/release gate | pinned source/license | per-file GPL review | NOT MET | existing byte manifests; combined release obligations OPEN |
| R16 | Exact-SHA FAST for pushed implementation batches | protected build.yml | n/a | GitHub Actions | MET | dc3b7ee7, 24e79891, cdee4932 and A1 544c371f / 41369816744 green; carrying follow-up requires live exact-head reconciliation below |
| R17 | Mission-final six-job FULL and verified APK | protected build.yml | n/a | all feasible requirements | NOT MET | must not label an intermediate run mission-final |
| R18 | Main-only commits, checkpoint and honest reporting | this record | n/a | successful feature gates | MET | two coherent implementation batches; no branches/PR/reset/force push; mission explicitly PARTIAL |

## Native verification batch

Native test fixture repaired; production limits and every renderer assertion retained.
Source map: `docs/NATIVE_RENDERER_TEST_REPAIR.md`. Changed files: build.yml,
THIRD_PARTY_NOTICES.md, vendored src/tests/dos.rs, qr-native-upstream-manifest.json,
this checkpoint and the repair report. Optimized CPU and C API suites each pass
243 tests, zero failures/ignored; 59 CI helper tests pass. Docs/claim/diff checks pass.
Unchanged-source release and debug serial runs each reproduced 241 pass / 1 parser
memory failure; the historical timeouts did not reproduce locally.
Implemented/pushed native verification batch:
`dc3b7ee7329b2517cbb4ac0d63e65e75f5a3c50b`.
FAST [41358042268](https://github.com/vishu762701/eve-mock-test-app/actions/runs/41358042268)
passed build/backend/Firebase, including 243 native tests; native UI and publication
were skipped as required by FAST. Current fetched main before the QR picker batch
is that SHA. The implementation commit containing this checkpoint identifies the
next code batch; always fetch live main before continuing.
The source panel uses 46dp cells (~7 columns), not the current four-column
Unicode/Airbnb grid. Do not present Unicode presets as animated documents.
Remaining QR source scene work follows this dependency repair. All other workstreams
remain open; historical reports are not substituted for fresh acceptance.

Native C API verification command and result: `cargo +1.99.1 test --locked --release --lib --features c-api --manifest-path app/src/main/cpp/vendor/tlottie/Cargo.toml -- --test-threads=1`: 243 passed, 0 failed/ignored, 2.14s.


## QR picker implementation batch

Source map: `docs/QR_EMOJI_PICKER_SOURCE_MAP.md`. New original-Java adaptations:
QrEmojiCell, EmojiPickOverlay and QrEmojiGrid, plus source shadeFor in QrPalettes.
EVE Kotlin integration: QrEmojiPickerGrid, UsernameCardActivity and TelegramQrSceneView.
The four-column Unicode/Airbnb picker is replaced by actual animated EVE catalogue
items through source Java press/selection badges/removal island/scrollbar/masks.
Six-key toggle/drop-oldest remains. Lifecycle cancellation, late asset disposal,
picker-open/scroll/choice restoration and reduced-motion/window visibility are wired.
The Material host and complete source sheet/blur/services remain ADAPTED/open.

Changed tests: QrEmojiPickerGridTest (four real view/lifecycle/restoration cases),
QrEmojiPickerNativeTest (new, compiled), TelegramQrSceneNativeTest (choice/source
restoration), and AdminDashboardRealDataVerificationTest (real Retrofit offline
contract fixture replaces its legacy production GET). No original schema assertion
is removed. Backend, authentication, permissions, Firebase rules, Worker/D1,
scoring, answers, rank, Home Night drawer and Profile Bio removal are unchanged.
Manifests, notices, fidelity contract, source map and this checkpoint accompany code.

Local commands/results (Gradle 8.7 / JDK 21 / Platform 36):

- `:app:testDebugUnitTest`: **677 tests, 151 suites, 0 failed/error/skipped**.
- `:app:compileDebugAndroidTestKotlin`: passed, including new native picker test.
- `:app:lintDebug`: **0 errors**, 1643 warnings (not all triaged); no suppression/baseline changes.
- `:app:assembleDebug -Peve.nativeAbis=x86_64`: passed; local development APK,
  not a mission-final artifact. Actual JNI library packaged; all three LOAD segments
  have `0x4000` (16KiB) alignment. Other ABIs require remote FAST build verification.
- Existing native-graphics QR raster test: **160 real decodes** pass.
- Python CI helper suite: **59 passed**. Docs, scoped acceptance-claim and diff
  checks pass. Claim targets remain OPEN.
- **85** native/QR source manifest entries match pinned official Git source digests
  and actual EVE digests. Prior Bezier facade drift is reconciled, with canonical
  original Java separately recorded EXACT. No source revision is silently changed.

Local environment failures were repaired outside the repository: missing jlink
(JRE -> full JDK) and test-JVM proxy/CA configuration. No TLS verification disabled.
The scroll test's first failure (expected 6, actual 0) came from changing window/span
sizes in the fixture. Fixed-size child viewport retains the exact equality assertion.
A plain Robolectric SDK-35 window reports GONE; the playback unit fixture controls
that window input, with actual platform visibility/playback asserted in AndroidTest.
An earlier changing-source lint snapshot failed SuspiciousIndentation; a stable-source
rerun passed. The full JVM console emitted a bare `Exception in thread "SDK 35 Main
Thread"` fragment at worker shutdown with no attached stack; JUnit XML has no failure
or error. Cause is UNKNOWN, not asserted fixed or used as native-device evidence.

## First unfinished requirement and exact next action

R2/A1 full QR scene remains NOT MET. R3/A2 is improved but still NOT MET for full
source sheet, shared AnimatedEmojiDrawable cache/service equivalence and matched
runtime acceptance. Gift entitlements remain BLOCKED by absent real EVE service/data.
No limitation is represented as owner-accepted. Profile/Admin/Home/student/Quiz,
full theme/accessibility/device matrix and final native acceptance remain open in
the requirement ledger. GPL release clearance remains unapproved.

The picker batch is pushed and exact-SHA FAST is green (evidence below).
Next implementation action: trace QrShareView's full scene draw/composition/recolor/open-close/cancellation
entry points into the existing Java QrCodeLayer/QrRender/QrGlass and Canvas host,
keeping Telegram account/network/premium services excluded. Complete that minimum
source subsystem and matched native runtime evidence before changing its OPEN claim.

Mission-final FULL: NOT RUN, because implementation/acceptance requirements remain
open. Verified GitHub APK artifact and final checksum: NOT AVAILABLE. Do not dispatch
a premature run and describe it as mission-final; do not bypass publication gates.
Local `/dev/kvm` is absent; no local emulator/device or 60/90/120Hz/reference match
is claimed. No production deployment, migration, signing or GPL gate override occurred.


## Latest observed main implementation and exact-SHA CI

Current fetched **implementation main SHA**:
`24e79891acb3cdf875d24dcff295b372475e01b2`.
This evidence-only checkpoint commit follows that implementation SHA; on resume,
fetch `origin/main` and inspect the checkpoint commit's own FAST before new edits.

| Commit | Scoped delivery | Exact-SHA evidence |
|---|---|---|
| `dc3b7ee7329b2517cbb4ac0d63e65e75f5a3c50b` | native security fixture repair + protected build-job step | [FAST 41358042268](https://github.com/vishu762701/eve-mock-test-app/actions/runs/41358042268) SUCCESS |
| `24e79891acb3cdf875d24dcff295b372475e01b2` | original Java QR picker atoms / EVE lifecycle + restoration / offline config contract test | [FAST 41360696652](https://github.com/vishu762701/eve-mock-test-app/actions/runs/41360696652) SUCCESS; [source import 41360696800](https://github.com/vishu762701/eve-mock-test-app/actions/runs/41360696800) SUCCESS; [docs 41360696660](https://github.com/vishu762701/eve-mock-test-app/actions/runs/41360696660) SUCCESS |

Both FAST runs passed Android build/JVM/AndroidTest compilation, native Rust tests,
backend and Firebase. Native navigation and publication were intentionally skipped;
neither run is FULL or screen acceptance. Remote native suite: 243 passed, 0
failed/ignored (2.67s at 24e79891). Local broad JVM: 677 passed, 0 failed/error/skipped.
All three local x86_64 libraries (QR engine and both CameraX JNI libraries) have
`0x4000` LOAD alignment. All four ABIs compile in remote FAST; final all-ABI APK
inspection/publication still requires the protected FULL artifact.

The bare SDK-35 shutdown fragment is present in **both** remote build logs,
including dc3b7ee7 before the new picker code. It predates that picker integration;
its cause is still UNKNOWN, not silently suppressed. CI's actual JVM task is green.
GitHub's approval-variable read returned HTTP 403; the current value of
`EVE_GPL_RELEASE_APPROVED` is UNKNOWN. No variable was written. The release workflow
file and its fail-closed condition are unchanged; documented license review remains
OPEN. No production-ready or legally cleared distribution claim is made.

## File / commit mapping

All paths below are repository-relative. Original source files retain their
upstream/adapter attribution; full line/hash maps are in the linked source reports.

| Commit | Changed files |
|---|---|
| dc3b7ee7 | `.github/workflows/build.yml`; `app/src/main/cpp/vendor/tlottie/src/tests/dos.rs`; `docs/NATIVE_RENDERER_TEST_REPAIR.md`; `docs/TELEGRAM_FINAL_MISSION_CHECKPOINT.md`; `docs/qr-native-upstream-manifest.json`; `THIRD_PARTY_NOTICES.md` |
| 24e79891 | `app/src/main/java/com/eve/app/telegram/qr/QrEmojiCell.java`; `app/src/main/java/com/eve/app/telegram/qr/EmojiPickOverlay.java`; `app/src/main/java/com/eve/app/telegram/qr/QrEmojiGrid.java`; `app/src/main/java/com/eve/app/telegram/qr/QrPalettes.java` |
| 24e79891 | `app/src/main/java/com/eve/app/ui/profile/QrEmojiPickerGrid.kt`; `app/src/main/java/com/eve/app/ui/profile/TelegramQrSceneView.kt`; `app/src/main/java/com/eve/app/ui/profile/UsernameCardActivity.kt` |
| 24e79891 | `app/src/test/java/com/eve/app/ui/profile/QrEmojiPickerGridTest.kt`; `app/src/test/java/com/eve/app/ui/admin/AdminDashboardRealDataVerificationTest.kt`; `app/src/androidTest/java/com/eve/app/ui/profile/QrEmojiPickerNativeTest.kt`; `app/src/androidTest/java/com/eve/app/ui/profile/TelegramQrSceneNativeTest.kt` |
| 24e79891 | `THIRD_PARTY_NOTICES.md`; `docs/qr-native-upstream-manifest.json`; `docs/telegram-profile-fidelity.json`; `docs/QR_EMOJI_PICKER_SOURCE_MAP.md`; `docs/QR_NATIVE_ADMIN_CHECKPOINT.md`; `docs/TELEGRAM_FINAL_MISSION_CHECKPOINT.md` |

## Resume verification commands

The managed workspace toolchain is outside Git under `/workspace/toolchains`.
Use JDK `/workspace/toolchains/jdk-21.0.13+9`, SDK
`/workspace/toolchains/android-sdk`, Gradle `/workspace/toolchains/gradle-8.7/bin/gradle`.
Apply the environment runtime skill's proxy/CA configuration to Gradle **and** test
JVMs; do not disable TLS validation. The repository continues to build with Gradle.

```
gradle :app:testDebugUnitTest :app:compileDebugAndroidTestKotlin :app:lintDebug :app:assembleDebug -Peve.nativeAbis=x86_64
cargo +1.99.1 test --locked --release --lib --features c-api --manifest-path app/src/main/cpp/vendor/tlottie/Cargo.toml -- --test-threads=1
python3 -B -m unittest discover -s .github/scripts/tests -v
node .github/scripts/verify-docs.mjs
python3 .github/scripts/verify_telegram_acceptance.py
git diff --check
```

Do not rerun completed implementation or treat a previous report as the live head.
First unfinished **product requirement** remains R2/A1 full source QR scene;
remaining source sheet/shared-cache/runtime A2 work and scanner morph A4 follow.
Other workstream requirements remain as listed above. Final status: **PARTIAL**.

## Resume 2026-11-02 — active A1 scene batch

Fetched live main `cdee49324290c27995833d178e7eaa0e97de2eb9`, clean checkout;
FAST 41361717397 and docs 41361717394 are successful. All earlier successful
native/picker work remains. Official Telegram fetched master still resolves to
`3de37c8cfb039452be7398909461da6288babf15`; no upstream pin changes.

First unfinished product requirement remains R2/A1. Active implementation scope:
original native backdrop raster + Java scene recolor wave/QR ink, scene glass
composition/cleanup, source card shadow and whole-layer reveal integration.
Required source dependency discovered: `TMessagesProj/jni/image.cpp:1211-1377`
`generateQrBackdrop`, not an Android gradient approximation. Its only imported
native dependencies are JNI, AndroidBitmap and math; unrelated image/network/
account code is excluded. Java source is kept Java, C++ kept C++.

| ID | Requirement | Current code | Upstream reference | Dependencies | Status | Verification evidence |
|---|---|---|---|---|---|---|
| R2.1 | Original background raster and recolor wave subsystem | C++ telegram_qr_backdrop + Java QrBackdropScene | QrShareView:1534-1575,2260-2426; image.cpp:1211-1366 | JNI, AndroidBitmap, QrPalettes | MET | same production native raster runs in host tests; original helpers/pixel body compared unchanged; interrupted target and independent snapshot tested; whole-screen runtime acceptance separate |
| R2.2 | Wave-following QR ink and colored card shadow algorithms | Java QrBackdropScene/QrCardShadow | QrShareView:3573-3588,3690-3806 | existing QrCodeLayer, Java mask | MET | LAB wave ink settlement, colored soft shadow pixels, disposal/recreation and 162 real QR/PNG decodes |
| R2.3 | Glass scene sampling and supported native blur acceptance | Java scene nodes and QrControlBackground | QrShareView:2140-2175,4533-4583 | QrRender, source backdrop, screen origin | NOT MET | CPU stand-in exact screen-offset pixels and cleanup pass; API31 GPU blur implemented/compiled, matching device evidence absent |
| R2.4 | Whole-layer reveal and snapshot platform acceptance | QrSceneReveal + Activity root/translucent theme + independent source crop | QrShareView:2095-2121,3591-3686 | Activity host, EVE branding | NOT MET | source equations/root/control endpoints, interrupted/duplicate close, stale callback cancellation, reduced motion and rotation-before-draw PNG pass; actual Android window/navigation matrix pending |
| R2.5 | Card recolor bounce and emoji pulse algorithms | original Java QrCardMotion | QrShareView:1043,1534-1542,1578-1596,3668-3687 | monotonic frame clock and motion policy | MET | original 320ms peak/undershoot, source continuation, 360ms dip and reduced-motion settlement tested; full-device physics not inferred |

Original root GPLv2/per-file review and production release gate remain applicable.
No release clearance, account service, fake gifts or backend changes are implied.


## A1 scene implementation verification (2026-11-02)

Pre-batch fetched main: `cdee49324290c27995833d178e7eaa0e97de2eb9`.
Implementation commit: `544c371f0ce9419ee1b56892cda8e44d2d37759e`.
This checkpoint is updated by its single focused native-test-input follow-up;
obtain the carrying commit with `git log -- docs/QR_SCENE_NATIVE_SOURCE_MAP.md`.
All source adaptations and line maps are in that report and the 95-entry native/QR
provenance manifest. Original C++ helpers and pixel/release body compare byte-equal
to the pinned image.cpp sections; only includes/JNI binding/null-allocation cleanup
are adapted. Java rendering/motion stays Java. Existing native/picker work is preserved.

Final stable local command:

```
gradle :app:testDebugUnitTest :app:compileDebugAndroidTestKotlin :app:lintDebug :app:assembleDebug -Peve.nativeAbis=x86_64
```

Result: **688 tests, 153 suites, zero failures/errors/skips**; AndroidTest source
compilation, resource/Java/Kotlin/C++ compilation, lint and x86_64 debug assembly
PASS. Lint: **0 errors, 1643 warnings**, no suppression or baseline change.
Eleven added JVM tests cover actual original native gradient/wave/ring pixels,
invalid-boundary rejection, interruption/snapshot state, cross-view glass sampling,
source shadow/bounce/pulse, reveal/cancellation/no-motion and real PNG after rotation.
Existing 160 palette-size QR decodes and two rotation PNG decodes pass.
Actual packaged-JNI test and scene export assertions are compiled in AndroidTest;
not executed locally. The workspace still has no usable local device/KVM matrix.

59 Python CI-helper tests PASS. Docs/acceptance-claim checks, pinned official
verbatim-import comparison and 95 original/EVE SHA-256 provenance checks PASS.
An initial lint pass read a changing source snapshot and flagged one indentation
error; explicit statement braces plus the subsequent stable-source full reruns
pass. No gate/assertion was weakened. The pre-existing bare SDK-35 shutdown
fragment without stack remains visible with green JUnit; cause still UNKNOWN.

The local x86_64 APK contains `libeve_qr_backdrop.so`, `libeve_qr_animation.so`,
`libimage_processing_util_jni.so` and `libsurface_util_jni.so`. All LOAD segments
have `0x4000` alignment; no host test library is packaged. Local-only SHA-256:
`19a3102e291951505d034e3f61edbb58f0c754cfd01446213af0471126a8c46e`.
This is not an Actions artifact, all-ABI final verification, release clearance or
mission-final APK. Published artifact and mission-final FULL remain **NOT MET**.

Changed-file groups in this implementation commit:

- Native production: CMakeLists.txt and telegram_qr_backdrop.cpp; Gradle host-test
  compilation, JNI headers/shim/bridge/shadow and Robolectric config are test-only.
- Java production: QrBackdropNative, QrBackdropScene, QrCardMotion, QrCardShadow,
  QrSceneReveal, QrRender disposal hook and QrControlBackground scene sampling.
- EVE integration: TelegramQrSceneView, canonical-delegating TelegramQrMotion,
  UsernameCardActivity, QR-only manifest/theme; existing auth/routing/exports remain.
- Tests: QrBackdropSceneTest, QrSceneRevealTest, TelegramQrMotionTest,
  TelegramQrRasterTest, QrBackdropNativeTest and TelegramQrSceneNativeTest.
- Documentation: THIRD_PARTY_NOTICES, QR_SCENE_NATIVE_SOURCE_MAP,
  qr-native-upstream-manifest, telegram-profile-fidelity and this checkpoint.

Security review: no Worker/auth/API/scoring/D1/Firebase/production-data changes,
new permissions, external-link routing, secrets or network calls. Native arrays,
finite values and small mutable RGBA buffers are validated; pinned allocations
are released on failure. New asynchronous shadow work has generation ownership;
late results recycle their bitmap. Window/input ownership is scoped to QR; the
whole root consumes touches while revealing. GPL/signing/distribution gates stay
unchanged and unresolved combined release review remains OPEN.

**First unfinished requirement:** R2/A1 full original pattern/scene behavior and
matching Android GPU/window verification. **Exact next implementation action:**
open the pinned QrShareView Pattern/place/wave-push/shared-cache and input callers,
map their minimum dependency graph to EVE's existing native Drawable catalogue,
then replace the remaining Kotlin pattern/gesture approximation with original
Java where compatible. Preserve username payloads, six-pick EVE assets and all
security contracts. Complete source sheet/morph A2 and scanner A4 follow; all other
NOT MET/UNKNOWN rows still apply. Before advancing, require exact-SHA FAST for this
batch and repair any failure. FAST 41369816744 at the implementation SHA is green; FULL is not yet eligible.


## Exact-SHA A1 CI and forward recovery update (2026-11-02)

Live main fetched before this follow-up is
`544c371f0ce9419ee1b56892cda8e44d2d37759e`.
FAST [41369816744](https://github.com/vishu762701/eve-mock-test-app/actions/runs/41369816744)
**SUCCESS** at that exact SHA: Android/JVM/AndroidTest compilation, backend and
Firebase Functions/rules all pass. All four `buildCMakeDebug` targets (arm64-v8a,
armeabi-v7a, x86, x86_64) execute successfully. Native Rust security/C API tests:
243 passed / 0 failed / 0 ignored (2.61s). New host C++ renderer compiles and the
JVM task passes. Source-verbatim 41369816777, docs 41369816731 and dependency
provenance 41369816701 also succeed at the same SHA. GitHub artifacts API returns
an empty list for this FAST run; it publishes no APK and is not FULL acceptance.

The first staged diff check detected one extra EOF blank line in the new C++
file; the compound shell did not stop before commit/push. This forward-only
follow-up removes it, refreshes its manifest SHA-256, and leaves all original
rendering math unchanged. No amend/reset/history rewrite occurred. It also declares
the generated host native library as a JVM Test input: changed renderer bytes must
invalidate Gradle's up-to-date test state. Host platform/JDK versions are compiler
task inputs. The dependency omission did not invalidate the first fresh CI tests;
this repair protects incremental and future native edits.

Final stable follow-up local run:
`gradle :app:testDebugUnitTest :app:compileDebugAndroidTestKotlin :app:lintDebug :app:assembleDebug -Peve.nativeAbis=x86_64`:
**688 tests / 153 suites / zero failures, errors or skips**, lint **0 errors /
1643 warnings**, AndroidTest compile and debug assembly PASS. Native renderer
host task and JVM tests execute again after the dependency declaration. APK
contents/alignment and local-only SHA-256 above are unchanged. Final staged
whitespace and 95-entry provenance checks pass. All earlier limitations remain.

Commit mapping: 544c371f contains all 31 scene/native/host/resource/test/provenance
paths listed in the preceding groups; this single follow-up changes only
`app/build.gradle.kts`, `app/src/main/cpp/telegram_qr_backdrop.cpp` (EOF),
`docs/qr-native-upstream-manifest.json`, `docs/QR_SCENE_NATIVE_SOURCE_MAP.md` and
this checkpoint. This is at most two successful implementation commits for this
backdrop/card/reveal subsystem. No unrelated feature is included.

The follow-up's own SHA/run ID cannot be self-embedded before committing. On
resume fetch main and query the Build Eve APK run whose headSha exactly equals
the fetched commit; require its success before advancing. The final live-head
result must be reported separately once observed. Do not infer it from 544c371f's
success. **Current next action:** push this focused follow-up, observe its exact-SHA
FAST, diagnose any failure, then resume the first unfinished R2 pattern subsystem.
Read-only next-source recovery located `Pattern` at QrShareView:135-166,
place/wave-push at 2568-2662, node key and `NodeSprings` at 2663-2740, atlases via
ensureAtlases:1264 and source input handler:6637 onward. These are investigation
locations, not imported/accepted implementations. Do not import gift documents or
account services into the EVE native-asset adapter.

## 2026-11-02 recovery continuation — R2/A1 pattern physics increment

Live baseline `5ced5663d6d669650a19379ca92a6f4873d38ae7`; official upstream pinned
`3de37c8cfb039452be7398909461da6288babf15`. This change scopes the
first unfinished original pattern-placement/node-wave/gesture subsystem,
with its production Kotlin caller, Java host tests, provenance and notice.
Existing QR backdrop/scene/picker/native/security/CI work is retained.
No backend, scoring, accounts, release variables or migrations are changed.

| ID | Requirement | Current code | Upstream reference | Dependencies | Status | Verification evidence |
|---|---|---|---|---|---|---|
| R2.A1-field | Source Java field placement, springs and gestures | QrPatternField / TelegramQrSceneView | QrShareView:208-229,2565-2787,3369-3407,6637-6768 | Canvas, Drawable, Android MotionEvent | UNKNOWN before exact-SHA CI; screen acceptance OPEN | Four Java production-path JVM tests added; native comparisons pending |
| R2.A1-atlas | Original emoji pattern atlas and swap compositor | Existing EVE shared NativeQrDrawable cache | QrShareView:1264,2843,3364 | Telegram document loader must not be imported | NOT MET | Source atlas/item old-new transitions absent |
| R2.A1-acceptance | Matched-device motion and GPL clearance | Existing fidelity/release contracts | upstream Android source and licenses | camera/device; redistribution review | NOT MET | No verified human-reviewed frames or legal clearance |

Next unfinished piece remains A1 source old/new pattern swap/cache/shine,
then picker-sheet morph. FULL mission and personal-qr remain NOT MET.

## 2026-11-02 R2/A1 additional source slot compositor batch

Baseline prior to change: `eb21b088d8d806500a48170c10d77f468432a9b1` whose exact-SHA FAST
[41372927365](https://github.com/vishu762701/eve-mock-test-app/actions/runs/41372927365)
was SUCCESS. This batch adds `QrPatternSwap.java`, host JVM regression
tests, EVE QR live pattern renderer and source documentation. No backend
or release/security configuration was changed. Commit SHA and CI must be
read from GitHub after push, never filled speculatively.

| ID | Requirement | Current code | Upstream reference | Dependencies | Status | Verification evidence |
|---|---|---|---|---|---|---|
| R2.A1-swap | Original Java per-cell old/new shrink/overshoot and expiry | QrPatternSwap + TelegramQrSceneView | QrShareView:1218,2837,3187,3369 | EVE native Drawable cache | UNKNOWN until CI; screen NOT MET | New four-case pure Java regression tests, native device still OPEN |
| R2.A1-atlas | Original per-pattern texture pooling and shimmer | EVE shared-frame drawable/scene | QrShareView:1264,3050+,3364 | upstream account-dependent animated documents | NOT MET | No original source atlas/lighting compositor |
| R2.A1-runtime | Matched Android runtime visuals and export | Existing EVE source-adapted QR screen | QrShareActivity/QrShareView | Device, GPU/blur, lifecycle | NOT MET | Native matched evidence absent |

First next unaccepted scope: source-compatible per-pattern atlas/scene
shimmer and picker-sheet morph, then scanner and remaining whole mission.

## 2026-11-02 R2/A1 background light-band Java increment

Baseline `50e26ee6e5198b647fe470e0b090420c9dc480f0`, official upstream `3de37c8cfb039452be7398909461da6288babf15`.
The official actual QR emoji field uses reusable Drawables; its ensureAtlases
only initializes gift loaders, not a missing active emoji GPU atlas. No invented
atlas/texture renderer was added.

| ID | Requirement | Current code | Upstream reference | Dependencies | Status | Verification evidence |
|---|---|---|---|---|---|---|
| R2.A1-light | Background shimmer auto sweep, curved/cylinder noise/raster | QrSceneShine, QrBackdropScene, TelegramQrSceneView | QrShareView:2488-2559,2805-2838,3111-3160 | RimTune, RenderNode scene, motion policy | UNKNOWN pending exact-SHA CI; full runtime NOT MET | Java Robolectric pixel/reduced-motion checks added |
| R2.A1-frames | Original animated-emoji draw/cache phase on real device | Existing NativeQrDrawable with previousEmojis | QrShareView:1264,3260-3410 | original Telegram ImageReceiver excluded | NOT MET | native frame/timing comparison not accepted |
| R2.A1-runtime | GPU glass, input, native 60/90/120Hz and GPL | QR activity and CI | QrShareView/QrShareActivity | device/legal source-review | NOT MET | no matched runtime evidence; license gate remains fail-closed |

Next unfinished scope: source per-symbol reflective shine/gesture sampling and
native matched-device validation, then picker sheet + scanner. Whole mission
FULL and published APK still unaccepted.

## A2 picker opening source transition increment — 2026-11-02

Previous live base `acb0fec72f4aaa635c869e3325e38f79dd47c49c`, original Telegram pin remains
`3de37c8cfb039452be7398909461da6288babf15`. First unfinished
picker opening transform implemented through Java `QrPickerOpenMotion`
and `QrEmojiPickerGrid`/Activity production integration. No duplicated
R2 pattern field, slot compositor or Java shine code. The unit tests cover
source 420ms cubic scaling and sheet fade constants; CI and device
validation must be recorded from the actual commit SHA.

| ID | Requirement | Current code | Upstream reference | Dependencies | Status | Verification evidence |
|---|---|---|---|---|---|---|
| R3.A2-open | 420ms source emoji-grid scale and fade | QrPickerOpenMotion, QrEmojiPickerGrid, UsernameCardActivity | QrShareActivity:542-569 | Activity sheet, RecyclerView | UNKNOWN until exact-SHA CI | New host JVM math tests, runtime still OPEN |
| R3.A2-sheet | Original shape/cascade/compositor/blur and complete picker | Current EVE BottomSheetDialog | QrShareActivity:532-758 | original view parent, pack and Telegram services excluded | NOT MET | Source full sheet not imported |
| R2.A1-source | Source patterns and atlas comments | Existing Java field/slot/shine, shared native frame | QrShareView:1264,3364 | EVE native Drawable cache | NOT MET full fidelity | Current upstream ensureAtlases does not actually implement emoji texture batching; do not invent it |

Remaining first required work: original source picker sheet shape,
row cascade and correct glass composition; scanner morph, native device
matrix and remaining mission subsystems follow. GPL release review OPEN.

## A2 follow-up — source island cut in scaled grid (2026-11-02)

Follow-up to `c18a07139dc48080b31e15098deea6f618824513`: preserve its production picker grow. Adapt
`QrShareActivity.java:663-676` in `QrEmojiGrid.java` so the selected
island mask stays aligned while the RecyclerView's 420ms source scale is
0.75→1. Java geometry regressions and Robolectric actual-picker
detach cancellation test added. No backend, access rule, native dependency,
payload, permission, release gate or product setting is changed.

| ID | Requirement | Current code | Upstream reference | Dependencies | Status | Evidence |
|---|---|---|---|---|---|---|
| R3.A2-mask | Opening picker overlay cut aligned with scaled grid | QrEmojiGrid/mapIslandCut; QrEmojiPickerGrid | QrShareActivity:663-676 | location transforms, Canvas | UNKNOWN until exact-SHA FAST | Java cut tests + actual-picker lifecycle JVM test; device pending |
| R3.A2-morph | Complete original sheet parent shape and glass/cascade | EVE BottomSheetDialog | QrShareActivity:532-758 | Telegram parent/scene compositor | NOT MET | Native matched frame/motion comparison absent |

Next piece: parent morph/cascade/blur of A2, source-driven scanner button
morph, then other unfinished mission workstreams. No production GPL clearance.

## A2 sheet island-to-panel source geometry (2026-11-02)

Verified precommit live baseline `b33ad5a78d6d267838ea01ca172378e950f13729`; upstream source
`3de37c8cfb039452be7398909461da6288babf15`. Source Java `QrPickerSheetMorph`, Kotlin
`QrPickerSheetSurface`, real Activity picker caller, JVM motion tests,
picker source map and notices changed in this one feature batch.
Explicit open gaps: the surrounding Material modal/drag differs from
Telegram's all-in-scene compositor; controls, selection and owned
animated Drawables remain EVE-native; mobile GPU/device acceptance and
license review are not yet proven.

| ID | Requirement | Current code | Upstream reference | Dependencies | Status | Verification evidence |
|---|---|---|---|---|---|---|
| R3.A2.shape | Original 420ms cubic sheet shape 144x40 to 10dp inset/radius 28.17 | QrPickerSheetMorph + QrPickerSheetSurface + UsernameCardActivity | QrShareView:5317-5433 | EVE Material host + QR glass Drawable | UNKNOWN before exact-SHA FAST | new 3 JVM geometry/curve tests, device comparison OPEN |
| R3.A2.parent | Complete native sheet parent/drag/cascade/glass lift | existing BottomSheetDialog / QrEmojiPickerGrid | QrShareView:5317-5570, QrShareActivity:532-758 | entire source scene/compositor | NOT MET | native source parity/GPU frame matrix absent |

Next first unfinished requirement: finish original source sheet parent
and close/cascade/blur compositor and run matched physical Android UI
tests. After that scanner morph, remaining QR/Profile/Admin/Home/student,
quiz/theme/RTL/CI/release gates. Whole mission PARTIAL.

## A2 island morph follow-up test and input gating (2026-11-02)

Following exact-SHA FAST success for `0ccbaaa5d6cd865bff8a634f24bd912365045d58` on
`Build Eve APK #41377373314`, the same A2 island morph feature gets
one focused forward-only follow-up: `QrPickerSheetSurface` suppresses
hidden controls while source sheet fraction <0.5; attached native-canvas
Robolectric pixel/input regression is added alongside source-map evidence.
Source `QrShareView.java:6654-6729`, original pin
`3de37c8cfb039452be7398909461da6288babf15`. The FOLLOW-UP SHA's
own FAST must be checked separately. Full parent compositor/source
sheet-sheet parity, device matrix, licensing and mission FULL remain NOT MET.

## A2 close choreography (2026-11-02)

Precommit live HEAD `480cb2a7a714af8682669332e91f89759ffe4eb2`; official source
`3de37c8cfb039452be7398909461da6288babf15`.
Previously completed A2 opening/clip commits and FAST green preserved.
This scoped close choreography integrates original Java source timings,
real picker cell closing and dialog Back/outside/drag dismissal paths;
the source-map and third-party notices were updated.

| ID | Requirement | Current code | Upstream reference | Dependencies | Status | Verification |
|---|---|---|---|---|---|---|
| R3.A2.close | 360ms close + 170ms content fade, interrupted open | QrPickerSheetSurface, QrPickerMorphDialog | QrShareView:5317-5352 | Material dialog window/cancellation | UNKNOWN before exact-SHA FAST | source math and Robolectric view test |
| R3.A2.cascade | 150ms top-first cell closing | QrPickerCloseCascade, QrEmojiPickerGrid | QrShareActivity:609-632 | QrEmojiCell and RecyclerView | UNKNOWN before exact-SHA FAST | pure Java regression tests |
| R3.A2.glass | Original in-scene sheet blur/lift and full morph | EVE Material modal | QrShareView:4740-4790,5317-5570 | original RenderNode GPU | NOT MET | native runtime and licensing OPEN |

Next: source sheet blur/lift compositor or source scanner morph
after exact-SHA FAST; all QR/Profile/Admin/Home/student/quiz/RTL and
mission-final FULL acceptance remain PARTIAL/NOT MET.

## A2 separate source sheet blur/lift (2026-11-02)

Baseline `3650a3e6c1eb794574f1b102f69ce5e3a198047c`, exact-SHA FAST 41379437122 SUCCESS for preceding
A2 source closing/choreography. Official upstream
`3de37c8cfb039452be7398909461da6288babf15`.
This coherent feature isolates `QrShareView:4730-4767,4793-4828`
source Java 1/8-resolution sheet blur/color lift, integrates it into
production `QrBackdropScene` and `UsernameCardActivity` panel,
tests matrix/quantization and updates source/rights maps.

| ID | Requirement | Current code | Upstream reference | Dependencies | Status | Evidence |
|---|---|---|---|---|---|---|
| R3.A2.glass-node | Independent 1/8 blurred node, brightness/contrast lift | QrPickerSheetLift / QrBackdropScene / UsernameCardActivity | QrShareView:4730-4767,4793-4828 | RenderEffect API31, source QR node | UNKNOWN until exact-SHA FAST | Java matrix/motion tests, GPU device evidence OPEN |
| R3.A2.full-parent | Exact in-scene sheet/scroll, rim and gestures | EVE existing Material modal | QrShareView:5317-5570 | native original scene compositor | NOT MET | Matched original-device frames and cancellation matrix absent |

Next unresolved: full Telegram source sheet compositor/runtime and
scanner source morph. Mission-final FULL/license release remain OPEN.

## QR scanner source button-to-screen morph (2026-11-02)

Live precommit `caf429b0b50468876ce38d8ff21f1217ab61cf5f`; previous source picker glass repair's
exact-SHA FAST `41380609101` SUCCESS. Pinned original Telegram
`3de37c8cfb039452be7398909461da6288babf15`.
This coherent scoped scanner feature adds original Java geometry,
Kotlin Canvas/CameraX host, real scan-button Activity caller,
translucent QR theme, two source tests, contract tests,
provenance and notices. No backend, Firebase, auth, admin rules,
release variables, signing, scoring or production migration touched.

| ID | Requirement | Current code | Upstream source | Dependencies | Status | Verification evidence |
|---|---|---|---|---|---|---|
| R5.scan-button | Original source 390ms button-to-screen and 330ms reverse geometry | QrScanMorph, QrScanMorphLayout, UsernameCardActivity, EveQrLiveScannerActivity | QrScanOverlay:99-112,168-195,370-479,561-568,601-623 | EVE QR translucent theme / Activity / CameraX | UNKNOWN until exact-SHA FAST | production-path source contract and Java tests; physical motion OPEN |
| R5.camera-order | Start after open, bounded ephemeral close preview, release before close | EveQrLiveScannerActivity / QrScanMorphLayout | QrScanOverlay:386-403,481-546 | existing CameraX + own username QR parser | UNKNOWN device | EVE permission/gallery/duplicate-result existing checks retained |
| R5.runtime | Match original scan-found polygon, hardware glass and camera lifecycle matrix | EVE QrScannerFrameOverlay | QrScanOverlay:579-708 and full camera dependency | device GPU, real permission lifecycle | NOT MET | original found corners/visual matrix and full permission paths not verified |

Next unfinished: original found-code outline plus camera/gallery/torch
permissions, rotation, rapid cancellation and native runtime; then
remaining QR/Profile/Admin/Home/student/Quiz/theme and FULL/release gates.
Root GPLv2 and combined-app distribution decision remain OPEN.

## R5 scanner permission recovery continuation (2026-11-02)

Live starting main 8ff0a7f4bf05601a6d760b652079d6cfd3a47c94
with exact-SHA FAST 41384393894 SUCCESS. This feature adds
EVE-owned denied/request-blocked settings and gallery choice,
Settings return recovery, single CameraX binding and lifecycle-safe
dialog cleanup. No Telegram camera, accounts or network system imported.
No backend, Firebase, signing, license gate or scoring changes.

| ID | Requirement | Code | Source | Dependencies | Status | Verification |
|---|---|---|---|---|---|---|
| R5.permission | Denial rationale, manual Settings, gallery, granted return | EveQrLiveScannerActivity | EVE Android permission contract | Android ActivityResult + existing TelegramNativeAlertDialog | UNKNOWN until exact-SHA FAST/device | Scanner source contract updated |
| R5.binding | No double async CameraX binding, lifecycle cleanup | EveQrLiveScannerActivity | EVE CameraX contract | ProcessCameraProvider | UNKNOWN until device | One-in-flight guard, release in onDestroy |
| R5.found | Source scanner detected polygon / native GPU parity | QrScannerFrameOverlay | QrScanOverlay:700-760 | ZXing result geometry and transform | NOT MET | Actual device and source comparison pending |

Next: original yellow found-outline with camera image-to-view transforms;
native permission/rotation/torch/gallery tests and wider mission still open.
Root GPL/per-file combined distribution and final FULL remain unaccepted.

## R5 scanner torch async integration checkpoint (2026-11-02)

Baseline main 963c8a7322ee155cbe68c0291b5805074485a117,
exact-SHA FAST 41385170876 SUCCESS. Official Telegram pin
3de37c8cfb039452be7398909461da6288babf15.
New QrTorchGate and production EveQrLiveScannerActivity use
CameraX future completion before reflecting flashlight state,
reject rapid taps and discard stale callbacks. Native tests
remain required after the pushed SHA. No changes to backend,
auth, Firebase contracts, scoring, signing, release gates or
source licenses.

| ID | Requirement | Code | Upstream | Status | Evidence |
|---|---|---|---|---|---|
| R5.torch | Accurate async torch state and accessible button | EveQrLiveScannerActivity, QrTorchGate | QrScanOverlay:783-787,914-922 | UNKNOWN before new FAST | 3 gate JVM tests + scanner source contract |
| R5.device | Flash hardware, background/rotation | same | EVE CameraX | UNKNOWN | Real device matrix pending |
| R5.found | Four-point original found QR outline | QrScannerFrameOverlay | QrScanOverlay:700-782 | NOT MET | Camera image-to-preview geometry and valid points pending |

Next unfinished requirement: source-constrained four-point scan-found
feedback and camera/gallery/torch permission runtime validation,
then original QR/Profile/Admin/Home/student/Quiz/theme and FULL.
Production GPL/per-file combined distribution approval remains OPEN.


## 2026-11-02 continuation: R5 QR scanned-code found overlay

Baseline before implementation: main
813a2b927ff1de25e5c05795323ae4a5d72a78e5,
exact-SHA FAST 41386124717 SUCCESS (Android/JVM/Backend/Firebase).
Official Telegram source pin
3de37c8cfb039452be7398909461da6288babf15.
This continuation adds the original Java QrScanFound source equations,
a real QR frame Canvas caller, exact four-point detection policy,
CameraX shared-ViewPort analysis-to-preview mapping, source found delay,
focused tests and provenance. The old/repaired scanner motion and
permissions work are preserved. No Telegram accounts, MTProto, original
Telegram camera service or network/documents imported; no production
data, server auth, scoring, CI gate, GPL approval or signing changes.

| ID | Requirement | Current code | Upstream ref | Dependencies | Status | Evidence |
|---|---|---|---|---|---|
| R5.found-shape | Source yellow 6dp rounded 18% outline and 0.3 approach | QrScanFound.java / QrScannerFrameOverlay.kt | QrScanOverlay:700-782 | Java Canvas/paint, four genuine ZXing points | UNKNOWN pending exact-SHA CI | New Java pixels/rotation and real overlay JVM tests |
| R5.found-map | Crop/rotation-correct camera-to-preview mapping and four-point-only 450ms delay | EveQrLiveScannerActivity.kt | QrScanOverlay:994-1011,1040-1063 | CameraX ImageProxy/PreviewView shared ViewPort | UNKNOWN pending device matrix | Source contract + buffer-rotation JVM tests; actual CameraX device evidence absent |
| R5.found-runtime | Physical preview orientation and found-timing fidelity | EVE CameraX | Original QrScanOverlay renderer | Emulator/device, navigation, real QR | NOT MET | No matched Android reference frames/60-90-120Hz measurement |

Next after exact-SHA FAST: real Android camera/gallery/torch/denial,
rotation and valid-point display verification; complete original
scanner live blur/source glass and further QR/Profile/Admin/Home/
student/quiz/theme requirements. Mission-final six-job FULL and
verified published APK still NOT MET. GPL review remains OPEN.


## Same mission checkpoint: R5 scan-found Java source integration (2026-11-02)

Current **verified** live main before this evidence-only change:
2f9727862b355f87b676dbc3d15be542074895f3.
Original upstream pin unchanged:
3de37c8cfb039452be7398909461da6288babf15.
The original Java QrScanFound.java, EVE Canvas overlay, real
CameraX shared-ViewPort ImageProxy/PreviewView transformation,
genuine four-point ZXing-only policy and source 450ms found
delay are implemented. Current production code in
EveQrLiveScannerActivity.kt / QrScannerFrameOverlay.kt.
No Telegram account/network/camera infrastructure, migrations,
production secrets, auth/scoring or CI/security bypasses added.

| ID | Requirement | Current code | Upstream reference | Dependencies | Status | Evidence |
|---|---|---|---|---|---|---|
| R5.found-source | Source yellow #FFFFDE07 6dp 18%-arm 0.3 approach and genuine four-point-only display | QrScanFound.java; QrScannerFrameOverlay.kt | QrScanOverlay.java:108-112,346-355,700-782 | Java Canvas + EVE view | MET scoped | FAST 41388793850 SUCCESS, native Robolectric pixel + four-point/rotation tests green |
| R5.found-transform | ImageProxy original pixel coordinates to displayed PreviewView, source conditional 450ms delay | EveQrLiveScannerActivity.kt | QrScanOverlay.java:994-1011,1040-1063 | CameraX 1.4.2, shared ViewPort | MET source-contract/compile, UNKNOWN physical runtime | FAST 41388793850 compiled/tests; hardware display matrix unverified |
| R5.found-device | Actual scanner 60/90/120Hz, crop/rotation, 3-button/gestures, gallery/torch/permissions | Existing Activity/overlay | full QrScanOverlay | Physical Android device/GPU | NOT MET | Native UI matrix skipped in FAST; no comparable source playback |
| Mission FULL/production | Protected six-job FULL, published checksum, license clearance | release gate | Telegram GPLv2/per-file | authorized review and final mission acceptance | NOT MET | No FULL/verified artifact; approval variable value UNKNOWN |

Source implementation commit ff4bc279 failed first FAST
41387565607 because of test-only ApplicationProvider import.
First repair 3d52de7 failed FAST 41387986583 because new
pixel tests used Robolectric legacy graphics (two red/yellow raster
assertions). Second repair 2f972786 uses established native
Skia graphics and **passes** exact-SHA FAST 41388793850.
All prior QR picker/wave/scanner/permissions/torch changes preserved.
Latest docs job 41388793852 SUCCESS. Native Rust tests 243
passed, no job gate weakened. Historical bare SDK35 thread
exception remains UNKNOWN and present in green JVM log.

First next requirement: real Android CameraX-to-preview coordinate
and gallery/camera/torch/permission runtime acceptance, plus
Telegram source scanner live blur/glass and content-fidelity gaps.
Profile/Admin/Home/Student/Quiz/theme/release/FULL remain open.
This checkpoint does not claim whole QR or mission completeness.

## 2026-11-02 continuation — scanner camera-layer source glass

Recovered clean main 58b415857bce257de4ca966a7174ff74b9ae910d; exact-SHA FAST
41389589665 and docs 41389589728 SUCCESS, no artifacts; native/publication skipped.
Latest official Telegram HTTPS Git HEAD remains
3de37c8cfb039452be7398909461da6288babf15, tree
cc3c6ccf48e4167fdec764dfd04e765043f55093. telegram.org/apps official link checked.
No duplicate implementation or concurrent remote main change found before edits.
The full new owner-scope ledger is TELEGRAM_REMAINING_REQUIREMENTS.md; previous
feature/commit/CI records above are retained. Whole mission remains PARTIAL.

A3.2 source gap implemented: original Java CameraView preview-child recording and
32dp DECAL blur, original QrScanOverlay glass fill/fallback/sheens/rim, screen-space
control sampling and RAM-only closing blurred shader. New QrCameraBackdrop.java /
QrScannerGlass.java integrate through QrScanMorphLayout and the actual CameraX
Activity. No live bitmap readback, new permission, backend/Worker/Firebase/scoring,
account service, production mutation or release gate change. Source map:
QR_SCANNER_CAMERA_GLASS_SOURCE_MAP.md. Everything is ADAPTED, not screen EXACT.
PowerManager substitutes for Telegram LiteMode; source controls/layout and actual
TextureView/permission/torch/rotation/reference acceptance remain NOT MET.

All 97 recorded native/QR manifest entries match authenticated official Git blobs
and EVE digests; 4 preexisting stale EVE hashes are reconciled with their actual
successful main file commits recorded in JSON. Four verbatim popup assets match.
This is the recorded inventory, not a claim that it inventories the entire app.
Superproject tags/tree fetched. Full recursive optional media/codec/account clones
were attempted but exceed the 8 GiB executor alongside Android tooling. Unused
partial clones discarded; required tlottie is checked out at exact official gitlink
31f1b542f88e7b4be9a01e749920d857535fc715. Other submodules are uninitialized and
not asserted verified. Source files/pins imported into EVE are unchanged.

Local verification so far:
- Optimized Rust C API command: 243 passed, zero failed/ignored (2.17s).
- Focused source camera-glass and production scanner contract: 4 passed.
- Broad JVM JUnit XML: 727 tests / 167 suites, zero failed/error/skipped.
- AndroidTest compilation passes, including synthetic HWUI/PixelCopy camera-glass
  fixture. It has NOT run on Android. Actual camera is not simulated as device proof.
- Backend npm run build and npm test: 288 passed, zero failed/skipped.
- Functions JS syntax checks and node --test functions/test/*.test.js: 19 passed.
- CI Python helper suite: 59 passed. Docs/claim/import/diff checks pass.
- Stable local :app:testDebugUnitTest :app:compileDebugAndroidTestKotlin :app:lintDebug :app:assembleDebug -Peve.nativeAbis=x86_64 passes (2m17s); lint 0 errors / 1645 warnings. This local debug APK is not a protected published artifact.

Local setup failures: overlapping Gradle attempts raced on generated resource JSON;
subsequent runs are serial. Installed Java runtime lacked JNI headers/jlink; full
JDK21 installed outside Git with normal proxy/CA trust. First new fallback pixel
test sampled low-alpha transparent-buffer unpremultiplication (red 26 vs 28); fixture
now uses the real scanner's opaque backing and retains visible color/clip assertions.
Backend has no npm lockfile; npm ci correctly rejected it, then repository-supported
npm install --no-package-lock + build/tests passed. No production test/assertion,
lint, TLS, native security cap or CI gate was weakened. The historical bare SDK35
thread-shutdown exception fragment remains present/UNKNOWN in otherwise passing JVM.

No local /dev/kvm; after installing platform tools adb devices is empty. Physical
camera/reference/60-90-120Hz/native navigation evidence is unavailable locally.
No premature mission-final FULL, published APK or legal clearance is claimed.
First overall unfinished requirement stays full A1 source/runtime acceptance;
next independent gaps include remaining source scanner control/lifecycle behavior
and an observed dashboard pending-rights visibility path (all default actions can
remain visible when its second rights fetch fails; Worker authorization is separate).
The carrying implementation commit requires its own exact-SHA FAST before advancing.

Local lint initially found five missing endFrame API guards and ten preexisting CameraX Java-marker opt-in errors. Explicit QrRender.hasEffects guard plus androidx.annotation.OptIn repair all 15 without suppressions, baselines or target changes. Stable-source rerun passed. Temporary upstream checkout was shallow-compacted after fetching tags to reclaim build space; current commit/tree and required tlottie objects remain verified; fetched tag IDs are retained in workspace evidence. No EVE/shared history was rewritten.

## Scanner lifecycle continuation — next coherent implementation batch

Current fetched/pushed main before this batch:
7bb581b1b95d7e4e79489db77dfa50c3f9b92b6a.
Exact-SHA FAST 41392841830 SUCCESS, docs 41392841837 SUCCESS,
source-import 41392841829 SUCCESS. All four native ABI build tasks and
243 Rust security/C API tests pass (2.59s). Artifact count zero; both native
navigation suites and publication skipped by FAST. Source glass is ADAPTED;
actual CameraX TextureView/reference/frame evidence remains open.

New EVE ownership implementation: QrCameraSessionGate.kt, actual
EveQrLiveScannerActivity pause/resume/bind/analyzer/permission/Gallery/close paths,
QrCameraSessionGateTest.kt and QrScannerPausedResultTest.kt. Original source
pause/destruction ordering inspected at QrShareActivity:1070-1077 and
QrScanOverlay:370-425,490-546, same official full SHA as above. Full technical
map and integration/test changes: QR_SCANNER_SOURCE_FRAME_CHECKPOINT.md.
Camera/torch stop on pause; old async binds, frames and delayed navigation cannot
survive a session change. Gallery delivery waits for resume and rejects superseded
requests. Saved in-flight permission/Gallery flags prevent duplicate launch during
recreation; denial/Settings return preserves manual recovery. Snapshots remain
before release and all secure-link/four-real-point contracts remain intact.

Ten focused tests (five ticket-state, two actual scanner Activity lifecycle and
three existing source/caller cases) pass; AndroidTest compilation and lint pass.
Stable local :app:testDebugUnitTest :app:compileDebugAndroidTestKotlin :app:lintDebug :app:assembleDebug -Peve.nativeAbis=x86_64 passes (38s): 734 tests / 169 suites, zero failures/errors/skips; lint zero errors / 1645 warnings. All 99 recorded source manifest entries match pinned Git objects/current EVE hashes. Actual device execution remains unavailable.
No new backend, Firebase, scoring, migration, production calls/permissions,
release variable, signing or data-model changes. Owner Bio/About removal verified
in current production Profile source/layout; backend bio data/API tests remain green.
Complete QR/Profile/Admin/Home/student/Quiz/theme/device and mission FULL still open.
Next independent engineering action after this batch's own green FAST: fail-closed
Admin pending-rights visibility and fresh per-screen right checks, preserving Worker
authorization and every legitimate source-tab action. Remaining original scanner
control geometry/physics and matched physical-device matrix are not accepted.

## Admin destination rights/confirmed dashboard continuation

Base fetched main 2e5655911e0755a0b3a1892a15d4bafd683631dd, exact-SHA
FAST 41394531948 SUCCESS and docs 41394531950 SUCCESS. Scanner lifecycle
734-test local/remote batch preserved. Both source scanner batches are now green;
FAST native navigation/publication skipped and artifact lists empty.

New scope C1/C2/C3/C5: shared AdminAccessGate now requires the fresh server
membership AND validated /admin/me metadata before revealing any protected root,
then rechecks current UID/email. Each privileged destination supplies its actual
Worker right; confirmed metadata is passed to dashboard setup so loading/failure
cannot expose all default actions. All 21 destination screens plus hub and extra
ManageUsers resume guard are integrated (23 calls). Existing source pager/tab and
row callbacks remain. Questions exam edits and monitor generation retry require
their compound rights. All four AdminViewModel callers traced; constructor no
longer issues unrelated directory/statistics requests. Missing server response
throws instead of synthesized permissions. Server authority/rights definitions,
Firebase/Worker/D1/scoring, data, production deployments, QR-free decision and GPL
release/signing gates remain unchanged. Source/action map: ADMIN_RIGHTS_CONTINUATION.md.

Local stable validation: :app:testDebugUnitTest :app:compileDebugAndroidTestKotlin
:app:lintDebug :app:assembleDebug -Peve.nativeAbis=x86_64 SUCCESS (53s).
740 tests / 170 suites, zero failures/errors/skips; lint 0 errors / 1645 warnings.
109 scoped admin/policy/identity cases pass, including 13x13 role pairing,
server owner, real malformed Gson metadata, pending/cancelled/failed permissions,
foreign identity, and offline actual repository missing-data handling.
Two existing source guards initially failed because the confirmed callback variable
and retry-right predicate changed; updated guards retain all behavioral assertions.
One overlapping local run polluted a reused log; the final serial broad log and
JUnit XML are clean. No assertion/lint/CI/auth gate weakened. Pinned source 99-file
inventory and unchanged native renderer evidence remain as above.

This batch needs its own exact-SHA FAST after push. Whole mission remains PARTIAL.
Source A1 follow-up discovered by actually opening QrShareView:3171-3180,3245-3304:
original emoji have no band-reflective lighting; that old pending atlas/shine claim
must not drive invented code. Actual ready-item seeded appearance/popIn is missing
from EVE drawEmojiSlot. Next concrete source action: integrate that original Java
appearance path, test late frames/rapid selections/export/reduced motion, then
source scanner control layout/press physics and remaining ledger items. Physical
camera/GPU/matched references, Profile/Admin/Home/student/Quiz app-wide acceptance,
protected mission-final six-job FULL/APK and human GPL review are still NOT MET/BLOCKED.

## A1 real-frame emoji appearance continuation

Base fetched main 40a7bb30fcc2bbb3468d482a0ec6a9e4633e811b; exact-SHA FAST
41395975426 SUCCESS (Android/all four ABI/JVM/AndroidTest compilation, native
243 tests, backend/Firebase); docs 41395975313 SUCCESS. All prior source QR,
scanner permission/torch/found/lifecycle and fresh Admin rights work remains.
Original source pin: 3de37c8cfb039452be7398909461da6288babf15.

Original Java QrEmojiAppearance now supplies ready-item timestamps, per-screen
seeded 300ms spread, 380ms pop and source backOut. Actual SceneView root reveal,
current/outgoing patterns, late native frames, picked-later sentinel, reduced motion,
export and cleanup are integrated. NativeQrDrawable checks real JNI frame return,
freezes the cached frame for export and stops a failed renderer instead of retrying
for every lattice node. Source map, dependencies, licensing and known adaptations:
QR_EMOJI_APPEARANCE_SOURCE_MAP.md. No invented reflective emoji light/GPU atlas,
Unicode preset, gifts, premium service or backend/auth/scoring change.

Stable local Gradle tasks (:app:testDebugUnitTest :app:compileDebugAndroidTestKotlin
:app:lintDebug :app:assembleDebug -Peve.nativeAbis=x86_64) PASS:
745 tests / 171 suites, zero failures/errors/skips; lint 0 errors / 1645 warnings.
The new five source math/readiness/isolation/export tests pass alongside all old
actual QR raster decodes. The new real Android JNI readiness/frozen-frame/close
fixture compiles but is NOT executed locally. Its synthetic solid JSON was rendered
by the same optimized host C API: parse OK, render status 0, 16,384 visible pixels.
No host result is represented as physical Android/GPU evidence. Recorded source
inventory: 101 entries match authenticated Git blobs and current EVE SHA-256;
four verbatim popup imports also match. Python CI helpers 59 pass; docs/claim/diff
checks pass. Pinned Rust 243/backend 288/Functions 19 evidence remains as above.

The carrying implementation SHA needs exact-SHA FAST. Current overall status:
NOT MET for whole A1/A2/A3/A4/Profile/Admin/Home/student/exam/theme source fidelity
and original-versus-EVE/native matrix; BLOCKED locally for physical hardware,
matched reference captures and human GPL distribution approval. No premature final
FULL, production deployment/migration, release-variable change or published APK.
The concrete next implementation action is source whole-scene card/typography/
parent geometry and original scanner control layout/press physics, followed by all
remaining ledger requirements. Active source engine/permissions/security repairs
must be preserved. The historical bare SDK35 thread shutdown fragment remains
unresolved despite green JUnit; no hidden/suppressed failure or legal clearance claim.


## A1 card/caption and A4 export isolation continuation — 2026-11-02

Recovered/refetched main e4dd748651a4b7abecc52f5c9be204fd67a1a64f; its FAST
41399010870 and docs 41399010801 SUCCESS, no artifacts. Official Telegram HEAD
re-fetched, unchanged 3de37c8cfb039452be7398909461da6288babf15; optional submodule,
workspace/hardware and human release limitations remain as recorded above.

QrCardLayout.java and QrCardCaption.java extract original Java card/rest/sheet rise,
condensed glyph/StaticLayout and caption drawing. Exact source font and 12 glyph
bytes are renamed only; root/per-file/font legal review stays OPEN. EVE measured
chrome/compact fit, locale-independent username, native accessibility controls and
Material modal adapter are explicit ADAPTED integration. Actual picker progress
now lifts the scene card, dismissal clears progress/callback, and both root and
child layout changes refresh viewport bounds. Independent export geometry/ink
keeps the live card/gesture bounds intact. Existing native/QR/scanner/Admin paths,
backend/scoring/identity contracts and fail-closed release gate are preserved.
Source map: QR_CARD_CAPTION_SOURCE_MAP.md. Manifest now records 116 identities.

Serial local broad Gradle (:app:testDebugUnitTest :app:compileDebugAndroidTestKotlin
:app:lintDebug :app:assembleDebug -Peve.nativeAbis=x86_64) PASS: 753 tests / 173
suites, zero failures/errors/skips. New geometry, actual font/glyph raster,
30/25/19 fitting, long32-character identity, Turkish locale, interrupted picker
export/live-state isolation and dismissal tests pass. Existing real PNG/ZXing
portrait/landscape/color scenes remain decodable. One old renderer source-text
guard needed the new local card parameter; its actual assertions are preserved.
Python CI helpers 59, docs, claim consistency, four verbatim imports and all116
pinned identities PASS. AndroidTest compiles; no physical test is inferred. The
historical bare SDK35 teardown fragment remains UNKNOWN, despite green JUnit.

The carrying implementation commit still requires exact-SHA FAST. Whole mission
NOT MET; no final FULL, published APK or legal acceptance. First unfinished source
implementation: QrScanOverlay original controls/title placement and press/torch
interpolation through EVE CameraX host. Then all remaining Profile/Admin/Home/
student/exam/theme and native-reference requirements in the full live ledger.


## A3 original scanner controls/title continuation — 2026-11-02

Carrying base main 3b2245bc0743ebd45d89e708eb6a899de0cb7e0a exact-SHA FAST
41402360574 SUCCESS; docs 41402360616 and import verification 41402360702 SUCCESS.
Build includes all four ABIs/pinned native tests/backend/Firebase, with native
navigation and publication skipped. Official source pin unchanged. Previous source
card/caption/export work remains intact; its full runtime acceptance stays OPEN.

QrScannerLayout.java and QrScannerControl.java extract original Java source geometry,
round icon controls, title placement, press convergence and confirmed-torch fill.
Original BackDrawable.java draw/animation bodies match after reversing mechanical
package/unit changes. Eight icons and existing Roboto medium match upstream bytes.
EVE native click/accessibility nodes and measured complete-group compact fit are
ADAPTED; actual Gallery contract, CameraX, permissions, futures, found coordinates,
allowlist and session cancellation remain. English/Hindi labels are resources.
See QR_SCANNER_CONTROLS_SOURCE_MAP.md; inventory now 129 upstream/EVE identities.

Serial local broad Gradle testDebugUnitTest/compileDebugAndroidTestKotlin/lintDebug/
assembleDebug (-Peve.nativeAbis=x86_64) PASS: 757 JVM tests / 174 suites, zero
failures/errors/skips, lint zero errors / 1646 warnings. Final focused four control
tests and compilation of the updated real-control HWUI fixture PASS. Actual native
HWUI fixture is not executed locally; no camera/device/reference proof inferred.
Python CI helpers 59, docs/claims/four verbatim imports/diff and all129 identities
PASS. No native engine or backend change. Historical bare SDK35 teardown fragment
still UNKNOWN despite green JUnit.

The carrying scanner implementation commit requires its own exact-SHA FAST.
Whole A1/A2/A3/A4/B/C/D/E/F remains NOT MET as in the ledger; human legal review,
physical/reference matrix and final protected FULL/APK remain unavailable/not eligible.
Next source action: finish scanner original cached corner renderer and scene parent/
picker gesture/compositor gaps, then remaining Profile/Admin/Home/student requirements.
Do not replay prior successful commits or treat unavailable hardware as a global stop.


## Owner-directed full-mission reconciliation / Profile implementation

Live main e0963d7dcdfec04bcbe90f56dea26246b549e93b refetched; exact-SHA FAST
41403654857 SUCCESS, docs 41403654877/imports 41403654789 SUCCESS; artifacts zero.
No concurrent commit. Green QR is preserved unchanged. The uncommitted QR frame/
corner-cache experiment is removed from production/test sources and deferred.
QR_ACTIVE_PATH_RECONCILIATION.md records active routes, every implemented feature
caller group, intentional adapters/test-only helpers, and critical/source/device/
external-legal gap classes. No new personal-QR critical defect reproduced; whole
acceptance OPEN. Earlier QR-only next-action paragraphs are superseded.

ProfileGooeyView existing Impl now releases CPU bitmap/Canvas and four GPU display
lists/effects on detach; same-size reattach restores size/intensity/blur. No new
renderer/state owner, draw equation, Add bio/About or backend change. Existing
RenderNode mask/blur/bridge code was already present; stale manifest wording is
corrected. Source/lifecycle map: PROFILE_REGRESSION_REPAIR.md latest section.

Local broad :app:testDebugUnitTest :app:compileDebugAndroidTestKotlin :app:lintDebug
:app:assembleDebug -Peve.nativeAbis=x86_64 PASS: 759 tests / 175 suites, zero
failures/errors/skips; focused actual bitmap/RenderNode lifecycle tests pass. Python
helpers 59/docs/claims/diff PASS. Hardware/reference/full-screen/legal results are
not inferred. The carrying Profile implementation needs exact-SHA FAST.

First concrete next action outside QR: repair C4 PollRepository failed/null list
responses becoming empty success and empty create ID being reported successful.
Existing ManagePollsActivity -> PollRepository -> EveApiService -> authenticated
Worker moderate_feedback routes must retain role/data contracts. Continue full
Profile/Admin/Home/Exam/shared ledger; all whole-screen requirements remain NOT MET.
No final FULL/release, legal variable change, deployment or production data write.


## Admin poll response continuation

Recovered main 66f3eea0ad5ffae629e2e2cf5cd8d5f78f508861; exact-SHA FAST
41405867198 SUCCESS and docs 41405867218 SUCCESS. Profile lifecycle repair is
verified at source/host/FAST level; hardware acceptance stays OPEN.

C4.poll-response: PollRepository now rejects failed/null list responses and
creation responses without a nonblank server ID. ManagePollsActivity propagates
cancellation rather than displaying a failure after its lifecycle ends. Existing
fresh moderate_feedback gate, Worker authentication/permissions, vote contracts,
source dialogs and deletion flow remain unchanged. No backend deployment/write.
Before repair, two focused tests reproduced the false-success defects. After
repair, protocol and actual Day/Night bound-row label/callback tests pass. Broad
local unit/AndroidTest compilation/lint/x86_64 assemble PASS; counts recorded in
TELEGRAM_REMAINING_REQUIREMENTS.md. This carrying commit requires exact-SHA FAST.

Next: observe this poll commit's FAST, repair any red gate, then continue Admin
mutation/action audit and Home/Exam/shared requirements. Worker poll status handling
of a nonexistent ID is a separately identified unverified confirmation gap; inspect
with isolated database tests before changing it. QR code remains untouched and
acceptance OPEN. Whole mission NOT MET; device/reference/legal/FULL limits remain.


## Live continuation: Worker poll status confirmation (uncommitted)

Fetched main remains 14c9e61be42fd7b969d5788b9ddf37d406df9ce4, no concurrent
changes. Exact-SHA FAST 41406623011 is still IN PROGRESS (Android build; backend
and Firebase jobs SUCCESS). Do not push over/cancel this required validation.

Pending local files: backend/src/routes/polls.ts and
backend/test/poll_status_confirmation.test.mjs plus requirement ledger. Worker PUT
now rejects non-boolean active, conditions audit on changes()=1 in the same D1
batch, and returns 404 for zero updated rows. Existing authorization is untouched.
Actual isolated SQLite/Hono test passes for malformed input, missing row/no audit,
and successful update/audit. Full backend npm test: 289 PASS, zero failures/skips;
npm run build PASS. Initial npm command used repository root by mistake; rerun
from backend/ completed successfully. No deployment, migration or production write.

Exact next action: wait for FAST 41406623011; repair red before proceeding. Then
review pending Worker diff, add denied-role/transaction rollback cases if needed,
run final docs/diff checks, commit coherent second poll fix to main, and verify
its exact-SHA FAST. Continue remaining Admin/Home/Exam/shared work; QR remains OPEN
and unchanged. Whole mission NOT MET; device/legal/FULL limits unchanged.


Worker poll review result: added real server-right denial and injected audit-failure
rollback cases, plus JSON null validation. Final local backend npm test: 291 PASS,
0 failures/skips; npm run build PASS. The conditional audit uses the immediately
preceding UPDATE changes() inside the same D1 batch; zero-row updates return 404.
Ready for coherent second poll commit once base FAST 41406623011 finishes.
Remaining whole Admin/Home/Exam/shared acceptance stays NOT MET.

Base 14c9e61be42fd7b969d5788b9ddf37d406df9ce4 exact-SHA FAST 41406623011
SUCCESS observed before push; no concurrent remote commit. Worker status fix is
the second coherent poll implementation batch. Its carrying SHA requires FAST.


## Live pointer after Worker push

main a8433b6484b7141f881aa5fb271808703c042302 pushed fast-forward; working tree
was clean immediately after push. Base poll FAST 41406623011 SUCCESS. Current
exact-SHA FAST 41407181451 is IN PROGRESS; docs 41407181426 also triggered. Local
291 backend tests and TypeScript build PASS. No production deploy/migration.

Next confirmed source-level functional gap outside QR: D4 MainActivity.renderChips
reuses chips when categories match, but initial onSelect closure tests
category != selected against the creation-time selected argument. After selection
changes, returning to the original selected category can be ignored. Existing
HomeViewModel.selectCategory already compares current state and handles no-op,
validation/persistence. Exact next action after green Worker FAST: remove stale
Activity guard and dispatch to that existing state owner; add a real repeated
All -> Other -> All callback/state regression test. Do not add a second selection
owner or recreate chips to hide the closure bug. Whole mission remains NOT MET.


## Home D4 live-selection callback implementation

Recovered main a8433b6484b7141f881aa5fb271808703c042302; exact-SHA Worker FAST
41407181451 SUCCESS. Official upstream HEAD rechecked unchanged full pin
3de37c8cfb039452be7398909461da6288babf15. Existing local checkpoint preserved.
HomeAppearance now hosts the same canonical chip rendering code previously in
MainActivity.renderChips. Activity passes current ViewModel selection getter and
existing selectCategory action; reused callbacks no longer capture obsolete state.
No new state owner, visual component, backend change or QR change.

Meaningful regression: 3 actual renderer/native callback tests FAIL under old
captured guard and PASS under live getter. Repeated All/Other/named transitions,
selected no-op event counts, chip reuse, reordering, presentation recreation,
fallback, stable measured widths, Day/Night/Hindi/RTL/200% font are exercised.
Existing ViewModel persistence and native touch fixtures remain. Source map and
limits: TELEGRAM_REMAINING_REQUIREMENTS.md D4 entry; all adaptation, no EXACT claim.

Serial local :app:testDebugUnitTest :app:compileDebugAndroidTestKotlin :app:lintDebug
:app:assembleDebug -Peve.nativeAbis=x86_64 PASS: 769 JVM tests / 178 suites, no
failures/errors/skips. Python helpers 59/docs/claim/diff PASS. Physical device and
whole-screen/source-motion acceptance remain OPEN. Carrying commit needs FAST.

Next action: verify carrying D4 exact-SHA FAST, repair red before moving on. Then
Gate 2 extend existing TelegramMenuPopup from current Theme/History/Bookmarks to
latest owner's seven actions using existing destinations/logout confirmation;
audit existing popup source maps, lifecycle/scroll and older whitelist tests.
No third poll refinement, QR-only iteration, release gate bypass or production
deployment. Full ledger remains NOT MET/BLOCKED where previously documented.


## Gate 2 seven-action canonical Home popup

Base main 936e93aae0273daea08ea0e14fd6f9538d60f7b7 exact-SHA FAST 41409105317
SUCCESS, backend/Firebase SUCCESS, native navigation/publication skipped. D4 filter
callback is source/host/FAST verified; physical matrix remains OPEN.

Current canonical TelegramMenuPopup and MainActivity now wire all seven owner
actions: Theme, History, Bookmarks, Mistake Notebook, Topic-wise Test, PYQ, Logout.
Existing destination Activities and existing logout/presence/cache flow reused;
new native confirmation is single-window guarded and disposed on host teardown.
Existing scroll cap, source roles/assets, shadow, radial selectors, motion and
back/outside behavior preserved. Latest owner requirement supersedes old tests
removing four actions; assertions now require all seven accessible source rows.
Actual popup host test verifies six callbacks after dismissal and no duplicate
callback from stale rows; stale-row case first failed, then isShowing guard fixed.

Final local serial :app:testDebugUnitTest :app:compileDebugAndroidTestKotlin
:app:lintDebug :app:assembleDebug -Peve.nativeAbis=x86_64 PASS: 770 tests / 179 suites,
zero failures/errors/skips. Python helpers 59/docs/claim/diff PASS. Initial missing
string reference and old removed-row assertion repaired without weakening current
requirements. Expanded Android fixture compiles, no physical run claimed. Source
map/dependencies/remaining limits in TELEGRAM_REMAINING_REQUIREMENTS.md latest entry.

Carrying seven-action commit needs exact-SHA FAST before next feature. Next action:
observe FAST, repair red if any, then Gate 2 shared dialogs/drawer/search/toolbar
production caller audit and first reproducible independent gap. Preserve both poll
commits, Profile/native and QR code; all whole-screen source/device acceptance
remains NOT MET/OPEN as recorded. No FULL/release/deploy/legal gate bypass.


## Seven-action feature second/final planned batch: locale and source icon reuse

Base main a6c0e042f58135d234b450e50c3c5f96ce38757c FAST 41411244614 SUCCESS,
docs 41411244627 SUCCESS. No concurrent remote implementation observed. Actual
Home popup callbacks, D4 filter fix and previous Profile/poll/QR work preserved.
Current second batch local results: 772 JVM tests / 179 suites, no failures/errors/
skips, AndroidTest compilation/lint/x86_64 APK PASS. Python helpers 59/docs/claims/
diff PASS. Actual popup tests cover Night and Hindi/RTL 200% font with full localized
accessibility labels and all six destination callbacks/stale-row cancellation.
Existing original notes/paper icons now replace generic icon choices; eight density
assets match official pinned Git bytes. No imported bytes changed or legal gate set.

Carrying commit requires exact-SHA FAST. This is the second planned successful
implementation commit for the seven-action feature; do not open cosmetic third
iterations. Hardware/reference acceptance is still OPEN. Next Gate 2 work: inspect
remaining source-faithful drawer/dialog/search/toolbar integration from actual
upstream and current callers; preserve working implementations and fix evidenced
gaps only. Drawer currently also has existing Topic/PYQ/Mistakes destinations;
these are active navigation entries, not duplicate renderers/screens. They are not
removed merely because overflow links the same destination. No production deploy,
migration, FULL/release gate bypass or claimed device pass. Full ledger NOT MET.


## Home exam-list integrity continuation

Fetched main bc139bd3b2cc236d20d399e64e41531bf812d269 and its exact-SHA FAST
41413589407 SUCCESS, docs 41413589354 SUCCESS. No concurrent changes. Seven-action
Home menu feature has TWO successful implementation commits (a6c0e042 and
bc139bd3); preserve it and do not start cosmetic third iterations. Official HEAD
rechecked unchanged 3de37c8cfb039452be7398909461da6288babf15.

Actual HomeViewModel.load saves/publishes ExamRepository.getExams output. Missing
data in a success response previously became empty and could replace valid cache.
Canonical repository now rejects missing data while retaining valid empty/sorted
lists, category mapping, existing error type and cancellation. All actual callers
(Home, Admin, EditExam, Settings and repository filters) remain on this same method.
Worker GET /api/exams:68-84 confirms nonnull array contract. No backend/scoring/
locks/timer/answer/data migration change. EVE business validation, no Telegram
transplant/parity claim. Three actual repository tests include before/after failure.
Cache retention is inferred from caller flow, not claimed as native/authenticated
end-to-end verification.

Serial local broad Gradle (:app:testDebugUnitTest :app:compileDebugAndroidTestKotlin
:app:lintDebug :app:assembleDebug -Peve.nativeAbis=x86_64) PASS: 775 JVM tests / 180
suites, no failures/errors/skips. Python helpers 59/docs/claims/diff PASS. Carrying
commit requires exact-SHA FAST. First next action: observe FAST, fix red if any,
then continue Gate 2 actual Home/dialog/drawer/search/toolbar callers and remaining
full ledger. Latest official DrawerLayoutContainer.java is a 214-line host whose
onTouchEvent returns false; do not transplant it as if it contains classic drawer
physics. A complete matching active source subsystem has not been established;
preserve current working AndroidX gesture owner rather than inventing source math.
Native/reference/physical matrix and GPL/FULL/release remain OPEN/NOT MET.


## Gate 2 shared dialog policy / active question-form retention

Base main e1885471f913d7499116f9e4fe6fa1a6ae31d5bf FAST 41457197984 SUCCESS,
docs 41457198260 SUCCESS. No concurrent implementation overwritten. Added actual
upstream AlertDialog dismissDialogByButtons policy to existing native renderer;
no new dialog system. Actual gated ManageQuestionsActivity owns one editor window,
canonical builder retains invalid/server-failed drafts, shows inline live-region
errors, disables pending submit/cancel and closes only on server success. Existing
VM/repository/Worker authorization remains canonical; optional failure callback and
cancellation fix avoid silent failure. Constructor injection enables real VM/repo
handler tests without eager authorized reads or production request execution.

Old unconditional dismiss policy was transiently restored: 3 actual Dialog tests
failed, fixed policy passes. Actual production form -> VM -> repo test uses held
API continuations for validation/pending/double tap/denial/retry/success. Two initial
fixture issues (async OnDismiss assertion and layout script selector) repaired;
no legitimate assertion or gate removed. Final serial local Gradle unit/AndroidTest
compile/lint/x86_64 assemble PASS: 782 tests / 183 suites, no failures/errors/skips.
Python helpers 59/docs/claims/diff PASS. Source/adaptation map and exact limitations
are in TELEGRAM_REMAINING_REQUIREMENTS.md latest entry.

Carrying commit requires its own exact-SHA FAST. Native/authenticated UI, TalkBack,
matched appearance, rotation/process-death draft/pending-request restoration remain
OPEN, not claimed passed. Exact next action: observe carrying FAST; repair red
before new feature. Then continue Gate 2 remaining shared-dialog callers and form
state/lifecycle, drawer/search/toolbar source tracing before later gates. Preserve
all green Home/menu (two commits), poll (two commits), Profile and QR work. No
production deployment/migration, scoring change, legal/FULL/release bypass. Whole
mission NOT MET. The source onTouchEvent=false DrawerLayoutContainer finding stays
a scoped trace limitation, never an invented drawer physics implementation.

## C5 selected question-list ownership continuation

Recovered clean main 412edc6ed80ca867635cbcf8cee06e56196728f4; exact-SHA FAST
41462667732 SUCCESS, docs 41462667822 SUCCESS. Official Telegram HEAD unchanged
3de37c8cfb039452be7398909461da6288babf15; no upstream imports changed. Gate 2
shared question-editor lifecycle tracing exposed a prerequisite C5 race: old exam
reads and mutation completions could replace current exam rows. Implemented in the
existing VM and spinner caller; no parallel state owner/screen. First six real
VM/repository tests reproduced failures before repair; actual Spinner callbacks
also cover reorder/rebind/new-View retained identity/fallback/no-op. Detailed scoped
ledger and limits in TELEGRAM_REMAINING_REQUIREMENTS.md latest C5 entry. This is
not a full editor rotation/process-death solution; that stays OPEN. No production
writes/deploy/migration, scoring changes or QR work. Carrying commit requires its
exact-SHA FAST; on red repair before unrelated implementation. Next eligible step
after green: active shared-dialog validation/server-failure callers (including
ManageQuestions promptRenameExam), then remaining full Home/Profile/Admin ledger.

C5 local final checks: `./gradlew :app:testDebugUnitTest :app:compileDebugAndroidTestKotlin :app:lintDebug :app:assembleDebug -Peve.nativeAbis=x86_64` PASS; 791 tests / 184 suites, zero failures/errors/skips. Nine new ownership tests PASS; initial six fail-before evidence in question-list-before.log. Python CI helpers 59/docs/acceptance/diff PASS. Historical bare SDK 35 Main Thread exception also appeared in this successful suite; root cause remains UNKNOWN, not suppressed. FAST still required for carrying commit.

## Active exam rename continuation

Base main 03c07dc45031b9c2361db53e7ba650dca729bdce exact-SHA FAST 41466628937
SUCCESS, docs 41466628886 SUCCESS; artifact list empty (FAST publication skipped).
No concurrent changes on fetch. Active shared-dialog caller audit found Android
examName vs Worker newName mismatch, invalid form auto-dismissal, unobserved errors,
and missing-target false Worker success. Coherent fix covers existing repository,
VM, same dialog renderer, gated Activity caller, English/Hindi resources and Worker
route, preserving installed-client alias and existing transactional name cascades.
No production deploy/migration. Source classification ADAPTED; see current ledger.
C5 selected-list ownership and all earlier green Home/Profile/QR/poll work preserved.

Fail-before evidence: two actual form tests fail; four of six original isolated
Worker cases fail. Fixture extraction keeps the sole real production form/caller,
not a competing implementation. First backend command had an incorrect relative
path; subsequent TypeScript check caught required condition.bindings, repaired
locally. Final uniquely named logs/results are authoritative, not overlapping early
logs. Native authenticated rotation/process-death draft restoration remains OPEN.
Carrying commit needs exact-SHA FAST; on red repair before unrelated features.

Rename final local verification: serial `./gradlew :app:testDebugUnitTest :app:compileDebugAndroidTestKotlin :app:lintDebug :app:assembleDebug -Peve.nativeAbis=x86_64` PASS (794 JVM tests / 185 suites, zero failures/errors/skips). `cd backend && npm test && npm run build` PASS (299 tests, no failures/skips); eight new real-handler cases. CI helpers 59, docs, structural acceptance and diff checks PASS. Bare historical SDK 35 Main Thread line still UNKNOWN in successful JVM suite. Carrying exact-SHA FAST required; FULL/native/reference/legal acceptance not established. After green continue remaining shared-dialog/Home/Profile production paths; next Profile category/data restoration/identity audit, preserving no Add bio/About and existing confirmed-store/request-owner mechanisms.

## Profile editor restoration ownership continuation

Recovered clean main 196c59d55b9710fcfb99bb1f2b4e8629f6bb9aee FAST 41469072072
SUCCESS, docs 41469072384 SUCCESS. Official Telegram HEAD rechecked unchanged
3de37c8cfb039452be7398909461da6288babf15. Rename Android+Worker coherent batch has
one successful implementation commit; no production deploy. Whole mission NOT MET.

Existing editor guard rebound current Firebase identity after recreation, losing
draft ownership. Current implementation saves original owner with native registry,
passes owner metadata from three active Profile callers, rejects mismatched/unknown
restoration before inflation, and retains existing live listener cleanup. Protects
Name/Phone/Username and retained Bio compatibility Activity; no visible Bio/About
row restored. Actual production Activity tests use only synthetic SDK boundary,
not an auth bypass in shipped code. Fixture bootstrap errors separated from real
fail-before evidence (two test failures, two existing behaviors pass). Fixed
original tests pass; expanded per-editor tests/broad checks pending below. Existing
API readbacks/queued-request owner metadata/Worker authorization unchanged.

First action on interruption: inspect this carrying commit/local changes and CI.
Finish required broad checks/FAST, repair red before new implementation. Then
continue full ledger Home/shared/Profile functionality; pending-request restoration
and device/reference matrix remain OPEN, not claimed solved by draft ownership.
No production deploy/migration, scoring/security downgrade, new account service,
GPL variable change or speculative QR iteration. Source classification ADAPTED EVE
adapter, not exact Telegram account implementation.

B5/B7 ownership final local verification: serial `./gradlew :app:testDebugUnitTest :app:compileDebugAndroidTestKotlin :app:lintDebug :app:assembleDebug -Peve.nativeAbis=x86_64` PASS; 805 JVM tests / 186 suites, zero failures/errors/skips. Eleven new Activity lifecycle tests PASS; Python helpers 59/docs/structural acceptance/diff PASS. Existing backend unchanged from predecessor FAST (299 tests). Historical bare SDK 35 Main Thread warning persists UNKNOWN; no suppression. Exact-SHA FAST required. Next independent Profile investigation: username availability callback cancellation/error ordering, field pending-save restoration and remaining source UI paths; preserve this ownership guard and no Bio/About rows. Full mission/device/GPL/FULL acceptance NOT MET.

## Username source theme/localization continuation

Base 2212767c4de9e0d23edd4439ff5bba0ab85215a3 FAST 41472089611 attempt 2 SUCCESS,
docs 41472089940 SUCCESS. First attempt stopped at transient CMake SDK TLS handshake;
failed job rerun at unchanged SHA passed, no source/gate changes or suppression.
Generated Gradle cache entries were trimmed locally to free ~300MiB; source,
dependencies and evidence logs preserved. Official upstream remains pinned at
3de37c8cfb039452be7398909461da6288babf15. No concurrent implementation overwritten.

Four actual Username Activity tests reproduce hardcoded status contrast/missing
Hindi failure before repair. Existing canonical color roles/palette and Activity
callback now use original three source role values, existing accessibleForeground
policy, native semantic status/live region and localized English/Hindi resources.
Actual source-file hashes, night byte comparison and source properties are recorded
in color-migration/source-role-contract.json and current requirement ledger.
Five-character EVE username contract is intentionally retained, classified ADAPTED
against source's Telegram-specific four-character flow. No Telegram accounts/API,
backend mutation, scoring, new renderer, Bio/About row or QR implementation change.

Carrying exact-SHA FAST required after broad checks. First next action on resume:
reconcile local/main and carrying CI; repair red before another feature. Then
continue actual pending profile/edit/lifecycle and remaining Home/shared/Admin/
student paths, preserving completed work. Whole UI/device/reference/GPL/FULL/APK
acceptance still NOT MET; no protected FULL eligibility or release approval implied.

Username final local checks: `./gradlew :app:testDebugUnitTest :app:compileDebugAndroidTestKotlin :app:lintDebug :app:assembleDebug -Peve.nativeAbis=x86_64` PASS; 809 tests / 187 suites, zero failures/errors/skips. Four new actual Activity status/theme/contrast/localization tests PASS. Python helper 59/docs/claims/diff PASS; three source scalar comparisons and Night full-byte identity PASS. Bare historical SDK 35 Main Thread line still UNKNOWN; no suppression. Exact-SHA FAST required; complete Profile/F1/source visual/device acceptance NOT MET.

## Shared question-response integrity continuation

Base live main 372b108a0b1b1c957566c53eeb7fd69b139e7a3e FAST 41475078791 SUCCESS,
docs 41475079226 SUCCESS. Existing canonical getQuestions converted missing data
to empty success; same-exam VM refresh erased confirmed rows. Actual repository/
Gson/VM fail-before tests reproduce missing data and null entries. Guard now
rejects them, preserving valid arrays/order/mock/topic/PYQ filters, cancellation,
request ownership and server-redacted answers. No backend/scoring/schema change.
Five added host tests pass. Final serial local unit/AndroidTest-compile/lint/
x86_64 assemble PASS: 814 tests / 188 suites, zero failures/errors/skips; helpers
59/docs/claims/diff PASS. Required carrying exact-SHA FAST still pending at commit.

Category B4/F1 theme repair was separately prepared and tested: native unselected
ring remains stale after live theme traversal. Small existing-component repair
passes 811 tests /188 suites and broad local checks, but category already has TWO
successful feature commits verified LIVE: feb9c888 / FAST 41163967661 SUCCESS,
14a62972 / FAST 41165900930 SUCCESS. Owner exception requested asynchronously
under ENGINEERING_STANDARD.md:31; owner explicitly answered "Haan, ek additional repair commit authorize hai" while this question-response batch was being prepared. ONE additional coherent category repair commit is authorized after the current required FAST. Patch is NOT applied,
committed or pushed; local tree restored only after archived copies/hash checks.
Review /workspace/pending-category-theme-repair/REVIEW.md; repair.patch SHA-256
236f8f9716ba51d0b14bbf7dec20a688cd63328ebac0227bfa03a6d11699e757;
base 372b108a0b1b1c957566c53eeb7fd69b139e7a3e, git apply --check PASS. Full copied
source/tests/map + metadata.json retained in same archive. If authorized later,
reconcile current main, apply reviewed patch, run checks and exact-SHA FAST. Do
not treat this external local artifact as an integrated main implementation.

Ledger IDs normalized: editor restoration/status batches belong B5/B7 (fields/
identity), not B4 (category). Nothing dropped; old evidence/commits preserved.
Latest successful UI status source scalar+Night identity evidence and legal notice
remain on main. No new QR iteration, device pass, legal release or FULL/APK claim.

First next action: observe carrying FAST; repair red before another feature. Then
continue actual Profile popup backdrop paint/alpha lifecycle and Home/shared/
Admin/student requirements. ProfilePopupBlurBackdrop has one prior implementation
commit (89254123); inspect real paint/runtime/source before changes. Complete
mission remains NOT MET. Production Worker/D1 deployment and GPL review remain
separate; no release variable touched. Unavailable hardware does not block code.

## Authorized category live-theme repair

Recovered main 381a76a610a3f1847f59a4c48939d7f2e4e6aea8 FAST 41479914039 SUCCESS,
docs 41479913807 SUCCESS. Owner explicitly authorized ONE additional category
repair commit: "Haan, ek additional repair commit authorize hai". Reviewed archive
patch/hash applied without conflict; source/test bytes unchanged from review.
Existing row updatePalette is called by canonical ThemeApplier, preserving same
indicator/selection/animation/click/geometry and prior color-role adaptation.
Source map, two preceding successful category SHAs and approval are recorded in
EVE_PROFILE_CATEGORY_PROVENANCE.md. Final local broad checks PASS: 816 JVM tests /
189 suites, zero failures/errors/skips; AndroidTest compile/lint/x86_64 assemble,
helpers59/docs/acceptance/diff PASS. Carrying exact-SHA FAST mandatory; this is the
single specifically authorized third category repair, not unlimited iterations.

First next action: observe carrying FAST, repair red. Then prioritize newly traced
EditExam question-action visibility/permission + active duplicated question form
callbacks, C1/C5, over cosmetic popup rim work. EditExam only gates manage_exams
while question mutations require manage_questions; Worker enforcement preserved.
Question UPDATE/DELETE zero-row fake success/false audit and UI Undo contract need
actual handler tests, never guessed restore semantics or live writes. Current
source has one active EditExam add button and separately built dialog using the
same dialog_add_question layout; canonical ManageQuestions builder already handles
confirmed/invalid/failed submissions. Reuse it if compatible without creating
another ViewModel/state owner or removing destinations. Full mission NOT MET;
QR/reference/physical/FULL/GPL/release gates remain OPEN. No production deployment.

## Question mutation confirmation continuation

Live recovered main d2b649f5bac8f4b86549ff854385bafbf3d97c6a FAST 41481845128
SUCCESS, docs 41481845112 SUCCESS. Category authorized third repair integrated;
no additional category exception or iteration. Current server question mutation
change is independent: conditional atomic audits, missing UPDATE -> 404, existing
DELETE idempotent absence success retained without false duplicate audit. Actual
Hono + all-migration SQLite tests reproduce 3 failures before repair, 5 controls
pass; initial fixture POST path corrected before product evidence. Full backend
307 PASS, TypeScript build PASS, helpers59/docs/acceptance/diff PASS. No production
write/deploy/migration; Android source unchanged from verified predecessor. Own
carrying exact-SHA FAST required; fix red before unrelated implementation.

Owner Undo product question is PENDING, not answered by generic resume message:
current AppUndoBar comment mandates immediate delete while no supported restore
API exists. Real DELETE then UPDATE test proves current fake restoration. Choices
asked: Telegram-style five-second deferred commit, or immediate delete plus staged
safe restore API (production deployment/migration still separately authorized).
Do not assume recommended option or create an invented restore service meanwhile.
Continue independent EditExam question-action role visibility and canonical Add
form integration after carrying gate green. Avoid another ViewModel/data owner:
EditExam already owns its repository/list; share stateless existing form presentation
with submit/busy callbacks if compatible. Fresh entry gate is manage_exams, but
question Add/import/edit/delete Worker routes require manage_questions; hide/guard
those controls with fresh returned rights and preserve Worker enforcement. Existing
private unused AdminActivity forms remain unrelated; don't delete by name alone.

Other known actual paths: EditExam add/edit validators auto-dismiss invalid inputs;
its delete currently changes list/shows success before network and Undo UPDATE
cannot restore. Full C1/C5/global/device/reference/FULL/GPL acceptance NOT MET.
First next action: verify carrying FAST, then independently repair real role/form
caller path, incorporating actual Undo answer only when it arrives. No speculative
QR work, source parity inflation, license variable change or production deploy.

## EditExam child-action authorization / canonical Add form

Base 5bf15f8444dd65d5e947b43a538404db701a500f FAST 41523395700 SUCCESS, docs
41523395561 SUCCESS. Server question confirmation batch has 307 backend tests
PASS, no production deployment. Owner Undo timing/API choice is STILL PENDING;
generic resume was not its answer. Continue independent authorized work.

Actual protected EditExam Activity tests isolate ONLY auth/data boundaries and
reproduce 4 failures/1 passing owner+denied-entry control. Fresh child rights and
host identity lifetime are now wired into production controls; same canonical Add
presentation handles both real active creation entrypoints without another VM/
repository/state owner. EditExam retains its list/repository, title and mutation;
window owned/dismissed on destroy. Shared AdminAccessGate hides/closes stale UID/
email host and cleans listener. Question row callbacks guard stale host rights.
Seven expanded actual Activity tests + existing real canonical form/VM/repo tests
PASS. Local serial broad Gradle PASS: 823 tests /190 suites, zero failures/errors/
skips; AndroidTest compile/lint/x86_64 assemble, helpers59/docs/acceptance/diff PASS.
No backend/scoring/answer API or new permission change. Source ADAPTED existing
native dialog policy + EVE authorization adapter; no whole-device parity claim.

Existing source assertions updated for canonical ownership while preserving four
modal workflows, original inputs/mutations, cancel and destructive styling checks;
no gate/legitimate assertion suppressed. Other dialog_add_question XML callers
Analytics/Flagged EDIT existing records; unused AdminActivity Add private method
has no caller. Neither was deleted by name or confused with an active duplicate.

First next action: verify carrying FAST; repair red. Then PRIORITIZE newly traced
Analytics/Flagged question edit target identity and nested-rights bug: their lookup
uses id == target OR questionText == text, so a first same-text row can override the
provided authoritative ID. Entry rights are view_analytics_logs/moderate_feedback,
not manage_questions. Trace real handlers/adapters, add meaningful regressions,
fix with one canonical target resolver and fresh action bindings, preserve existing
roles/data/score and do not invent support for unavailable generated-question IDs.
Also OPEN: EditExam bulk-picker result/context restoration through pending fresh
permission after recreation; current callback fails closed. Undo decision pending;
implement its chosen model only on actual owner answer. Source/UI/native/FULL/GPL
acceptance remains NOT MET; QR remains preserved/OPEN, no speculative iterations.
