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
| 6 Enforce/final APK | Pending | No final APK is claimed at this boundary |

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
