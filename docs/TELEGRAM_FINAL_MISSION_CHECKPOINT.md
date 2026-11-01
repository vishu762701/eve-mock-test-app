# EVE Telegram final mission checkpoint

Status: PARTIAL. This is a continuation record, not screen acceptance.

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
| R13 | Native engine verified, existing failures resolved | vendored tlottie/JNI/CMake | official tlottie gitlink | Rust/NDK/C++ | NOT MET | first unfinished dependency: 3 previously recorded Rust test failures |
| R14 | Security and production-data invariants | existing auth/Worker/D1/Firebase | EVE contract | no deployments/data changes | UNKNOWN | no changes yet; final diff review required |
| R15 | Provenance and distribution safeguards | manifests/notices/release gate | pinned source/license | per-file GPL review | NOT MET | existing byte manifests; combined release obligations OPEN |
| R16 | Exact-SHA FAST for pushed implementation batches | protected build.yml | n/a | GitHub Actions | MET | dc3b7ee7 / 41358042268 and 24e79891 / 41360696652 green; this evidence-only follow-up requires its own head check |
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
