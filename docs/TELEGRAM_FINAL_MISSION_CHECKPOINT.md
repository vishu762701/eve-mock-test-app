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
| R16 | Exact-SHA FAST for pushed batches | protected build.yml | n/a | GitHub Actions | NOT MET | baseline green; no new batch yet |
| R17 | Mission-final six-job FULL and verified APK | protected build.yml | n/a | all feasible requirements | NOT MET | must not label an intermediate run mission-final |
| R18 | Main-only commits, checkpoint and honest final report | this record | n/a | successful feature gates | NOT MET | ongoing |

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
- `:app:lintDebug`: **0 errors**, 1643 warnings; no suppression/baseline changes.
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

Next action for this batch: push the coherent picker implementation to main;
observe its exact-SHA FAST and repair any failing gate before unrelated work.
Then trace QrShareView's full scene draw/composition/recolor/open-close/cancellation
entry points into the existing Java QrCodeLayer/QrRender/QrGlass and Canvas host,
keeping Telegram account/network/premium services excluded. Complete that minimum
source subsystem and matched native runtime evidence before changing its OPEN claim.

Mission-final FULL: NOT RUN, because implementation/acceptance requirements remain
open. Verified GitHub APK artifact and final checksum: NOT AVAILABLE. Do not dispatch
a premature run and describe it as mission-final; do not bypass publication gates.
Local `/dev/kvm` is absent; no local emulator/device or 60/90/120Hz/reference match
is claimed. No production deployment, migration, signing or GPL gate override occurred.
