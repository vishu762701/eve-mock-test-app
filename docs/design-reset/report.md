# EVE design reset evidence

This report replaces the earlier chronological task notes. Current authority is the owner’s locked specification and the two explicit exceptions: remove Device font; permit black navigation area on API 24–25 in light mode. XML Views/ViewBinding only; no Compose screens.

| Phase | Status at Phase 5 boundary | Evidence |
| --- | --- | --- |
| 0 Baseline | Verified | pre-design-reset at 40ecfd366ccd3df94cbeed8f2526355f940dcec5; baseline.json and baseline-sha256.json |
| 1 Root | Implemented and locally verified | Root day/night color, dimension, type, shape, motion and default widget themes |
| 2 Components | Implemented and locally verified | Shared solid cards/dialogs/sheets, native controls, shadow clipping and spacing |
| 3 Navigation/icons/motion | Verified on hosted devices | 6761cf64bf309f1dcd9d9824be37a2d41b157346, run 39216050826; all six jobs success |
| 4 All screens | Implemented; final native certification pending | screens.md; 95 XML layouts, runtime/custom-view migration; recovery fixes in Phase 5 |
| 5 Remove old | Local gates verified | cleanup-evidence.json; deleted-files.md; phase5-inventory.json |
| 6 Enforce/final APK | Local checks verified; hosted delivery pending | scripts/design_check.sh PASS; no final APK is claimed at this boundary |

## Checks and actual hosted state

Local configured Temurin 21 / Gradle 8.7 / Android SDK: `./gradlew assembleDebug testDebugUnitTest lintDebug assembleDebugAndroidTest` PASS, 261 JVM tests, lint zero errors. Latest recovery run: BUILD SUCCESSFUL in 7s, 86 tasks. Python workflow/static contracts: 15 PASS. Drive scanner node tests: 2 PASS. Documentation check and git diff checks are rerun before commit. Hosted emulators cannot run locally because /dev/kvm is unavailable.

Run 39196346487 succeeded at bf97f7f2f5999bb700ae8436c18ca062afc0d2df, but its APK is an earlier phase, not the final design reset. Latest pushed run 39252548325 at 1a0aa87c48125a0b7e91635c50211bfa6855772a: build/backend/functions success; native three-button failure (10/40); native gesture cancelled at its timeout; APK publishing skipped. Actual job logs were inspected. Never reuse either earlier APK as final delivery.

Native recovery fixes preserve functional assertions: Material TabLayout’s stock onMeasure was repeatedly restoring stock text metrics while our layout reapplied different metrics. The shared control now uses Material’s native custom-label slot and exact scaled token dimensions, retaining selection/ViewPager callbacks. Segmented-strip inner width/34dp segment height corrected. CircularProgressIndicator no longer receives an incompatible framework progressDrawable. Disabled-state color tests now pass real enabled drawable states; capsule geometry is measured through RelativeCornerSize; translucent chart track pixels allow only premultiplication rounding while exact paint tokens remain asserted. Popup selection retains actual native clicks with a bounded window-idle wait and diagnostic failure details. No native success is claimed until hosted checks pass.

## Root values, licenses and compatibility

Exact palette resides in values/design_colors.xml and values-night/design_colors.xml. Poppins Regular 400, Medium 500, Bold 700 are bundled; no system/device-font selection remains. All seven type roles use no font padding.

> Copyright 2020 The Poppins Project Authors (https://github.com/itfoundry/Poppins)
>
> This Font Software is licensed under the SIL Open Font License, Version 1.1.
>
> The OFL allows the licensed fonts to be used, studied, modified and redistributed freely as long as they are not sold by themselves.

Full license is bundled at app/src/main/assets/licenses/Poppins-OFL.txt. Tabler v3.49.0 uses the MIT license, read before conversion; source mappings are in tabler-icons.json. Vectors use 24 grid, 2.75 stroke, round caps/joins. Full bundled license is in assets/licenses. BlurView 2.0.6 (Apache 2.0), already compatible with AGP 8.5.2 / min SDK 24, provides real API 31+ backdrop blur; radius 14dp, tint 55%, fallback tint 94%, 0.5dp hairline. Foreground content is not blurred. Below API 31 or unavailable blur uses fallback.

Cards map requested y=6dp/blur=20dp depth to native elevation 9dp. API 28+ black ambient/spot shadow color alpha is 16% day / 60% night, with platform multipliers 1.0. API 24–27 uses platform black shadow alpha .16/.60. Android controls exact blur shape, so 9dp is the requested approximation, not a claim of an exact 20dp native kernel. Surface elevation overlays are disabled. Shared clipping ancestors release clipChildren/clipToPadding, scrolling content reserves bottom clearance, tabs are above content.

## Cleanup proof

733 resource keys were removed only after actual lint UnusedResources diagnostics and zero references outside the deletion group; four pruning rounds and lint-output SHA references are recorded. Twenty-four retired production classes, ten binary/font/GL assets and seventy stale task documentation/artifact files have independent zero-reference/hash evidence. Lint does not enumerate each binary/class/document individually: those use explicit reference evidence plus full lint/build gates, not a fabricated per-file lint claim. Remaining bundled fonts are only Poppins 400/500/700 and their XML family. Protected raw Lottie files are retained even when lint considers them unused. Every deleted file is listed in deleted-files.md.

Only equal-value eve_bg/eve_text color aliases remain for byte-identical protected ThemeManager. eveHorizontalBreakpoint remains a functional responsive-layout attribute, not an obsolete design token. Modern EveGlassToolbar and minimal protected-window cleanup support remain because they implement current chrome/protected behavior.

## Preservation and verification limits

ThemeSwitchAnimator.kt and ThemeManager.kt remain byte-identical to baseline. TelegramMenuPopup.kt has the previously verified Phase 3 container-only change; no Phase 4/5 change. Sun/moon toggle resources and every raw/Lottie asset remain unchanged. Lottie playback, motion, timing and triggers are preserved under the later owner instruction; whether their existing artwork colors are an extra palette exception is pending the already-asked product question. Do not claim full Lottie hue normalization.

All screen/source entries are listed in screens.md. “Implemented” is source migration, not authenticated production end-to-end verification. Native fixtures cover light/dark, gesture/three-button navigation, responsive type, layouts, popup behavior and theme reveal; final rerun remains required. Notifications use Android’s standard template: app-owned icons are migrated, but the OS controls notification typography. No app widget/RemoteViews layout exists. Native DatePicker internals and all authenticated production data states have not been individually certified. Test/auth headers retain their functional screen structure; a pixel-perfect large-title assertion for these specific screens is unverified.

Retained hues and small system-color text contrast inventory are finalized with Phase 6. Never infer final completion from a prior progress message.

## Phase 6 enforcement and final local checks

`scripts/design_check.sh` delegates to the adjacent Python checker, exits non-zero on violations and prints PASS only after scanning production XML/Kotlin/Java, deleted-resource references, old design tokens, layout type/font literals, corner/elevation literals, divider views, Poppins inventory, identical sw360/sw600 size-token copies and baseline hashes of protected core/sun/moon/raw Lottie resources. It is deliberately not wired into CI. Nine temporary negative fixtures were each rejected (hex, text size, font family, typeface, elevation, radius, divider, deleted reference and old token); fixtures were then removed and the clean tree rechecked PASS. Protected animation files/resources are the explicit owner exception. FastBlurHelper's unchanged channel masks are image arithmetic, not chosen UI colors; its hash is checked against baseline.

Existing Lottie color lookups moved unchanged into DesignLottieTokens.kt; callbacks delegate with the same original color, stroke/fill flag, alpha and theme target. No playback logic or assets changed. Whether to normalize the preserved artwork palette remains the already-asked product choice, so this checker PASS does not assert Lottie hue normalization.

Final local four Android gates PASS (32s, 86 tasks); 261 JVM tests and lint zero errors. The baseline warning volume was reduced by removing obsolete resources; existing compiler SDK/deprecation warnings remain. Full type/geometry integration certification still depends on the hosted native jobs. No successful job is rerun manually.

Small system-color text on white, including conditional runtime states, is listed in [contrast-inventory.md](contrast-inventory.md); machine-readable source details are in contrast-evidence.json. Exact system colors remain as specified despite their measured ratios below 4.5:1.

### Retained other hues

These seventeen cyan/teal/mint/pink-family colors are retained in the root derived token files; this lists declared tokens, including tokens without a remaining active caller. The same RGBs are present in both qualifiers; they are not remapped to a different hue. Existing Lottie artwork colors are separately retained pending the product choice above.

| Token | RGB |
| --- | --- |
| `retained_hue_ff007987` | `#007987` |
| `retained_hue_ff008299` | `#008299` |
| `retained_hue_ff00c0e8` | `#00C0E8` |
| `retained_hue_ff00c3d0` | `#00C3D0` |
| `retained_hue_ff00c8b3` | `#00C8B3` |
| `retained_hue_ff00d2e0` | `#00D2E0` |
| `retained_hue_ff00dac3` | `#00DAC3` |
| `retained_hue_ff0c817b` | `#0C817B` |
| `retained_hue_ff142e2c` | `#142E2C` |
| `retained_hue_ff3cd3fe` | `#3CD3FE` |
| `retained_hue_ff40cbe0` | `#40CBE0` |
| `retained_hue_ff63e6e2` | `#63E6E2` |
| `retained_hue_ffd30f45` | `#D30F45` |
| `retained_hue_ffe5f9f6` | `#E5F9F6` |
| `retained_hue_ffff2d55` | `#FF2D55` |
| `retained_hue_ffff375f` | `#FF375F` |
| `retained_hue_ffff6482` | `#FF6482` |

### Before/after counts

Reproduced with `python3 scripts/design_inventory.py`; includes token definitions/comments, all qualifiers and XML font families. These are source occurrences, not hardcoded-color violations.

| Metric | Baseline | Phase 6 |
| --- | --- | --- |
| hex_color_occurrences | 496 | 137 |
| literal_dp_occurrences | 3007 | 658 |
| literal_sp_occurrences | 181 | 42 |
| style_definitions_including_qualifiers | 279 | 116 |
| drawable_files_including_qualifiers | 133 | 103 |
| font_files_including_xml_families | 8 | 4 |
| layout_files_including_qualifiers | 95 | 95 |
| kotlin_files | 237 | 240 |

Commands and local log hashes: phase6-verification.json. The four local gates use the configured JDK 21, Gradle 8.7 and Android SDK with the session proxy/trust store; no signing or workflow configuration was changed.


## Final native recovery checkpoint

Run 39334405207 at 06df86d39dbdad9348b58ee4f7e18cc7bc668a48 verified build/backend/functions success, both native modes failure, publishing skipped. Actual decoded logs show the same four failures in both modes; instrumentation crashed after 25/40 because a fixture asserted inside a posted main-thread callback. This is not reported as a successful device run.

Recovery preserves every original feature assertion. The blur fixture now waits for real window attachment before seeding native blur, captures asynchronous assertion failures and reports them on the test thread. Transparent track pixels allow only the already-measured 8-bit alpha premultiplication rounding; the underlying drawable still requires the exact track token. Palette row measurement now accounts for both its weighted Poppins heading and nested legend, avoiding the verified 320dp wrap. Chip maximum width uses the configuration's app width and two exact 12dp margins; the obsolete 32dp fixture expectation was replaced by the margin token. Native failure XML is printed on failure with the original non-zero exit status retained, so further problems include their source lines.

All four local Android gates PASS (1m24s, 86 tasks); Python 15 PASS; design_check PASS. Native rerun remains required before final publishing. No protected animation or Lottie playback code changed in this recovery.


Run 39335399036 at 3989010 completed all 40 native tests in each mode with two failing test cases per mode; build/backend/functions passed and publication was skipped. The earlier track/heading/window-attachment failures cleared. Full failure XML identified four remaining geometry bindings: the overview tiles still used Column instead of the shared Tile role, review letter keys had fixed 28dp heights, the rank inset host was fixed at 44dp including its padding, and the exam spinner row allowed multiline text inside a single-line input. They now use shared Tile padding, minimum-sized/wrapping key and compact-tile roles, and the shared single-line SpinnerItem respectively. The payment timer capsule's secondary text was also a measured contrast defect; its existing accent capsule now uses the specified on-accent chip label and icon tokens. All values come from the locked roles; no palette substitution or weakened text/contrast checks.

The existing hardware capture now verifies only the category text area (excluding the selected-check icon) and emits home-attached light/dark fixtures for review. This distinguishes real attached rendering from early offscreen Material font callbacks. Final native certification remains pending for these minimal bindings.

Final binding verification: all four Android gates PASS in 17s (261 JVM tests, lint zero errors), standalone design checker PASS, 15 Python contracts PASS, documentation validation PASS, bash syntax PASS and git diff --check PASS. The single updated JVM expectation now checks the new shared ChoiceKey binding and independently asserts that it inherits the unchanged locked Heading type role.


Run 39336905960 at fda21cc completed all 40 tests per navigation mode; build/backend/functions succeeded and both native jobs failed two cases, so no APK was published. Its logs confirm the prior tile, review/rank text, spinner and payment-timer contrast defects cleared. The remaining failure paths were the neutral input background's nullable stroke and the still-local two-line Practice/PYQ action override, followed by clipping of the long sandbox-action caption.

Material 1.12 source confirms canDrawStroke skips transparent colors. The shared input now updates its existing MaterialShapeDrawable through the public EditText background (last box layer for native outlined dropdowns), covering initial neutral, focus, error, disabled and counter-overflow states; no reflection or second outline is added. The native check still demands exact colors/width and now transfers focus to an explicit fixture sink and verifies the previous ring clears. Practice/PYQ actions inherit the existing single-line capsule rule. “Test Sandbox Payment” fits the locked label size; the original full purpose remains in accessibility contentDescription and its callback is unchanged.

All four local Android gates PASS again (35s, 261 JVM tests, lint zero errors), standalone design checker PASS, Python contracts 15 PASS and git diff --check PASS. Attached category text passed its tightened glyph-only assertion in both modes in run 39336905960; final hosted rerun is still required for the latest shared-input/action fixes.


Run 39338680515 at 5325483 completed all 40 tests per mode with one failing fixture per mode; Student rendering/text/contrast verification and input focus/error/neutral checks passed. The remaining exception is ManualUiSpecificationTest line 283 dereferencing an absent stroke color on an already-asserted zero-width button outline. The fixture now treats absent color as unpainted, retains exact zero stroke width, and still checks explicit color when present. All enabled/disabled/read-state opacity and foreground assertions are retained. Production UI code is unchanged in this correction. All four local gates PASS in 5s; final native rerun/publication pending.


Run 39340175160 at 623ba85 completed 40 tests per mode with one remaining ManualUiSpecificationTest failure: an enlarged-font custom tab label had no completed text layout (lineCount 0). The zero-width stroke fixture issue cleared. Shared tab labels are now installed, given maxLines=1 and sized before Material's onMeasure, rather than mutating typography after onLayout. Selection callbacks and the exact 14sp role/40dp strip/34dp segment remain unchanged; font layout assertions remain strict. Shared touch regions continue to update through their existing global-layout bindings.

Source evidence review also found three palette-legend marker Views without backgrounds and review-item top/bottom margins summing to 16dp. Generic success/error/neutral dot styles now restore system-green/system-red/secondary meaning, and two existing 4dp margins give the exact 8dp inter-card gap. No new feature, palette value or screen-local style was introduced. All four local Android gates PASS in 28s; final native rerun/publication remains pending.
