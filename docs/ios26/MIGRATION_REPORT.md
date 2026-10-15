# Eve iOS 26 migration — recovered delivery report

## Status and recovered checkpoint

**VERIFIED: local implementation and build checks pass. Runtime release verification is pending.**

Repository: `vishu762701/eve-mock-test-app`, branch `main`, baseline
`5f28980b64b87873984dd2f8d0f70baff54e5971`.
The user's latest instruction, “Push and make applies”, authorizes delivery to main
and supersedes the earlier local-only recovery instruction. Remote SHA and Actions
verification are pending delivery; local validation below is already complete.

On recovery, the existing migration diff and Apple evidence were intact. The last
recorded build had compiled Kotlin and stopped at JdkImageTransform because the
runtime lacked `jlink`. The final started operation had downloaded/extracted a full
JDK; `java -version` and `jlink --version` verified that it had completed. No build
operation was still running. The source was preserved and validation resumed from
that checkpoint. The exact instant at which the usage limit interrupted execution
is **UNKNOWN**; build success was never inferred from the downloaded JDK.

## Completed work

| Requirement | Status | Result |
|---|---|---|
| Authority | VERIFIED | User-requested UI ADR-009 and current geometry authority recorded in DECISIONS, SOURCE_OF_TRUTH, GEMINI and matching repository rules. The older JARVIS ADR-009 is retained; full titles distinguish the numbering collision. |
| Apple source verification | VERIFIED | Official Color, Typography and Materials JSON fetched in the original session; saved JSON, extracted rows and SHA-256 hashes retained. All 12 light/dark system palette rows already matched resources. Default text sizes/leading already matched type.xml. |
| Shape migration | VERIFIED in source/build | Capsules, 26dp fixed cards/menus and 34dp sheets/dialogs use shared dimensions/styles. EveShapes performs parent-minus-padding with a 4dp minimum. Add Question's 34dp modal/16dp inset fields use 18dp; ordinary nested/form surfaces use their 14dp role. Numeric choices are Eve's interpretation. |
| Touch targets/density | VERIFIED in source/build | Explicit small controls are raised to 48dp, list rows have 52dp minima, selected card padding increases modestly. ResultTabs no longer resets the track to 44dp: a 56dp track leaves 48dp targets after its 4dp insets. |
| Glass scope | VERIFIED in source/build | Existing top/navigation and Result tab regions gain backdrop-only glass; menus, sheets and dialogs use the documented material approximations. Banner/drawer/content blur removed; cards/rows remain opaque. Chrome/menu RenderEffect, bounded floating-dialog window blur and sheet window blur preserve sharp foregrounds. Older-API resources/helpers provide opaque fallbacks. |
| Blue | VERIFIED | Interactive aliases resolve to #0088FF / #0091FF. Unused eve_accent_text_readable retains #005BC0 / #72B2FF. Explicit blue contrast exceptions are reported, never counted as AA text passes. |
| Background roles | VERIFIED in source/build | Home/Login/Test use plain canvases; grouped screens use grouped canvases, secondary cells and tertiary nested surfaces. System inset backing follows the screen role. |
| Typography check | VERIFIED in source/build | No layout numeric textSize or literal textColor overrides remain. Home/Login use Large Title; pushed headers use inline Headline. Core type.xml values are retained. Android's existing medium-family semibold approximation remains documented; exact rendered glyph weight is not measured. |
| Protection | VERIFIED by diff/tests | ThemeManager and ThemeSwitchAnimator, animation assets/XML nodes/sizes/triggers, icon files and UI strings remain unchanged. Protected palette snapshots pass. The Home bell's existing colour callback now resolves primaryLabel on glass under the explicit glass-colour instruction; no animation data or timing change. Popup reveal logic is retained. |
| CI delivery contract | VERIFIED in source/script tests | Existing build/native/backend gates retained. Both navigation modes and complete visual evidence gate remain required before the sole Eve-debug-apk upload. Requested fixture PNGs are available through disposable test logs; the additional verification artifact upload was removed. |
| GitHub Actions/runtime visuals | UNKNOWN / pending Actions run | No current native emulator execution or Actions run is claimed. |

[Per-file inventory](FILE_CHANGES.md) includes every layout, drawable, Kotlin file,
resource, test, workflow and authority/evidence document touched.

## Local verification evidence

- `testDebugUnitTest`: **253 tests, 0 failures, 0 errors, 0 skipped**.
- `assembleDebug`, `lintDebug`, `assembleDebugAndroidTest`: **PASS**.
- Lint: **0 errors, 1,660 warnings**; the unchanged baseline also has 1,660 warnings.
  Comparison in LINT_COMPARISON.json shows the existing Home overdraw warning now
  names the plain canvas, and the intentionally unused readable-blue fallback
  replaces an older unused-material warning. No warning suppression or lowered
  non-blue contrast threshold was added.
- `python3 -B -m unittest discover -s .github/scripts/tests -v`: **13 PASS**.
- `python3 -B .github/scripts/check_design_system.py`: **PASS**, with explicit
  user-chosen blue exceptions. Blue on white is **3.52:1**; blue on some other
  content surfaces is lower. Opaque filled-control white/blue ratios remain
  3.52:1 Light / 3.23:1 Dark and must not be described as 4.5:1 text compliance.
- Resource XML parsing, protected animation-node/UI-string comparisons and
  `git diff --check`: **PASS**. See VALIDATION.json for counts and boundaries.
- Static primary-label contrast on thin glass, composited against opposing black
  and white backdrops, is at least **14.88:1 Light / 10.70:1 Dark**. All four
  approximation tokens exceed 4.5:1 for primary labels in both modes. This is
  mathematical colour evidence, not hardware compositor proof.

Actual Android command (environment toolchain only; no project dependency changes):

```sh
JAVA_HOME=/workspace/toolchain/jdk ANDROID_HOME=/workspace/toolchain/sdk /workspace/toolchain/gradle-8.7/bin/gradle -Djavax.net.ssl.trustStore=/etc/ssl/certs/java/cacerts -Dhttps.proxyHost=proxy -Dhttps.proxyPort=8080 -Dhttp.proxyHost=proxy -Dhttp.proxyPort=8080 testDebugUnitTest assembleDebug lintDebug assembleDebugAndroidTest --console=plain
```

[Gradle output](GRADLE_VALIDATION.log), [script output](SCRIPT_VALIDATION.log),
[contrast output](CONTRAST_VALIDATION.log), [lint comparison](LINT_COMPARISON.json)
and [machine-readable validation](VALIDATION.json) preserve the observed results.

## Apple rows actually read

The colour RGB values are from the official JSON image-reference alt text; HEX is
a lossless conversion. Typography is the iOS/iPadOS Large (default) table, in points.
All requested light palette values and default type sizes match these rows.

| Colour | Light RGB | Light HEX | Dark RGB | Dark HEX |
|---|---|---|---|---|
| red | R-255,G-56,B-60 | #FF383C | R-255,G-66,B-69 | #FF4245 |
| orange | R-255,G-141,B-40 | #FF8D28 | R-255,G-146,B-48 | #FF9230 |
| yellow | R-255,G-204,B-0 | #FFCC00 | R-255,G-214,B-0 | #FFD600 |
| green | R-52,G-199,B-89 | #34C759 | R-48,G-209,B-88 | #30D158 |
| mint | R-0,G-200,B-179 | #00C8B3 | R-0,G-218,B-195 | #00DAC3 |
| teal | R-0,G-195,B-208 | #00C3D0 | R-0,G-210,B-224 | #00D2E0 |
| cyan | R-0,G-192,B-232 | #00C0E8 | R-60,G-211,B-254 | #3CD3FE |
| blue | R-0,G-136,B-255 | #0088FF | R-0,G-145,B-255 | #0091FF |
| indigo | R-97,G-85,B-245 | #6155F5 | R-109,G-124,B-255 | #6D7CFF |
| purple | R-203,G-48,B-224 | #CB30E0 | R-219,G-52,B-242 | #DB34F2 |
| pink | R-255,G-45,B-85 | #FF2D55 | R-255,G-55,B-95 | #FF375F |
| brown | R-172,G-127,B-94 | #AC7F5E | R-183,G-138,B-102 | #B78A66 |

## iOS/iPadOS Large (default) Dynamic Type

| Style | Weight | Size (pt) | Leading (pt) | Emphasized |
|---|---|---|---|---|
| Large Title | Regular | 34 | 41 | Bold |
| Title 1 | Regular | 28 | 34 | Bold |
| Title 2 | Regular | 22 | 28 | Bold |
| Title 3 | Regular | 20 | 25 | Semibold |
| Headline | Semibold | 17 | 22 | Semibold |
| Body | Regular | 17 | 22 | Semibold |
| Callout | Regular | 16 | 21 | Semibold |
| Subhead | Regular | 15 | 20 | Semibold |
| Footnote | Regular | 13 | 18 | Semibold |
| Caption 1 | Regular | 12 | 16 | Semibold |
| Caption 2 | Regular | 11 | 13 | Semibold |

## Light and Dark report

Every row below has **VERIFIED source/resource/compilation evidence** and **UNKNOWN
current rendered-device appearance**. No screenshots were produced or reviewed
from this migration. Ios26ChromeTest and the existing native suites are prepared
for Actions execution; their presence/compilation does not constitute a run.

| Surface | Light source treatment | Dark source treatment | Current runtime evidence |
|---|---|---|---|
| Nav/top bar | Thin #D9FFFFFF tint, primary black labels/icons; capsule | Thin #D91C1C1E tint, primary white labels/icons; capsule | UNKNOWN; ios26-nav-bar fixture prepared |
| Tab bar | Capsule track/thumb, primary unselected labels, blue selected control | Same adaptive roles | UNKNOWN; ios26-tab-bar and Result fixtures prepared |
| 3-dot menu | Regular tint, 26dp corners, primary labels/icons | Regular dark tint, 26dp corners, primary labels/icons | UNKNOWN; ios26-menu fixture prepared |
| One sheet: Target Exams | Regular modal material, 34dp top corners, capsule chips | Adaptive dark equivalents | UNKNOWN; ios26-sheet and target-exams fixtures prepared |
| One dialog: Report Question | Plain-content hierarchy in 34dp floating glass dialog | Adaptive dark equivalents | UNKNOWN; ios26-dialog content fixture prepared; window/compositor inspection still required |
| Home | #FFFFFF canvas; #F2F2F7 raised content; opaque banner/drawer | #000000 canvas; #1C1C1E raised content | UNKNOWN; existing Home/native fixtures prepared |
| Test | #FFFFFF canvas; protected answer/state fills; concentric options | #000000 canvas; protected dark answer/state fills | UNKNOWN; existing Mock Test fixtures prepared |
| Result | #F2F2F7 grouped canvas; #FFFFFF cards; #F2F2F7 nested surfaces | #000000 grouped canvas; #1C1C1E cards; #2C2C2E nested surfaces | UNKNOWN; Overview/Review/Leaderboard fixtures prepared |
| Settings | #F2F2F7 grouped canvas, #FFFFFF cells | #000000 grouped canvas, #1C1C1E cells | UNKNOWN; student-settings fixtures prepared |
| Login | #FFFFFF plain canvas; semantic form/type roles | #000000 plain canvas; adaptive form/type roles | UNKNOWN; student-login/signup fixtures prepared |

The production Result segmented navigation is above its content; it stays in that
region. There is no separate bottom tab-bar container to relocate or invent.
The in-app Sign In prompt found in source is Settings' reauthentication alert:
its outside dim/blur flags are cleared. The Google account picker is external
Play-services UI and was not modified or exercised.

## What could not be verified

- Current native suites in gesture and three-button navigation: no attached device,
  no installed emulator and no /dev/kvm. `adb devices` returned an empty list.
- Current light/dark PNGs, hardware blur/clipping, low-API opaque fallback on a
  device, Sign In outside-area appearance, enlarged-font reflow and physical navigation.
- Actual Eve/Device preference switching and rendered semibold appearance on a device.
- Authenticated/network production flows, Google account picker, real exam submission
  and live account data. Their business implementation was not changed by this task.
- Remote uploaded commit SHA, GitHub Actions conclusions and published APK artifact:
  delivery verification is pending the authorized push.

**INFERRED**: the resource and renderer changes should produce the intended
appearance because compilation, binding/source tests and colour mathematics pass.
This is not a substitute for the missing runtime evidence above.

## Manual verification before release

1. Push the preserved changes to main under the latest authorization. Compare local HEAD with
   `git ls-remote origin refs/heads/main`; do not force-push.
2. Run/review Build Eve APK for that commit. Both native navigation jobs, build,
   backend and Firebase Functions checks must pass. Confirm only Eve-debug-apk is
   uploaded, after the verification gates pass.
3. Review the requested light/dark fixture images in job logs and the full native
   gallery on the runner. Run `eve_visual_review.py --enforce` against collected
   captures when reviewing locally. Canvas chrome/dialog-content fixtures do not
   certify hardware blur or actual dialog-window composition.
4. On a phone, inspect nav/tab/menu, Target Exams, Report Question and the Sign In
   prompt in both themes. Check sharp foregrounds, clear Sign In outside area and
   opaque content cards. Check a pre-31 device's opaque fallback.
5. Open Home, Test, all Result tabs, Settings and Login; increase font scale, use
   both font preferences, toggle Day/Night through the Telegram reveal, and check
   that controls fit and protected answer colours/animations remain correct.

Delivery is authorized; Actions and device checks remain pending until observed.
