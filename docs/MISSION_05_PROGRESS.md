# Mission 05 — evidence ledger

## A.1-01 Telegram submenu roles (source-backed mapping)
- Baseline main: 3e489fde602ca96e412cf43288149915ddae8e7c.
- Source attachment SHA-256: b5bbfb7f78ec76769c888d30eb58831845e809b11ab413c722c3646a30be161e; 146 ZIP entries, 144 upstream entries; all per-entry SHA-256/size and CRC validated.
- Telegram `TMessagesProj/src/main/java/org/telegram/ui/ActionBar/ThemeColors.java:203-206`: Day submenu item/icon #FF1A1D21, background #FFFFFFFF, separator #FFF5F5F5.
- Telegram `TMessagesProj/src/main/assets/night.attheme:35,108,316,362`: Night submenu background #FF282829, separator #FF1E1E1F, icon/text #FFFFFFFF.
- EVE `TelegramColorRoles.kt`: adds original Day fallback defaults; `TelegramThemeEngine.kt`: resolves roles independently from active or custom imported themes; `TelegramThemeEngineTest.kt`: covers Day/Night and imported-theme overrides.
- Classification: EXACT raw ARGB mapping in new palette fields, not yet any screen's final composited color. No unverified assertion of complete app-wide color coverage.
- Source pack is NOT a complete original checkout; whole original Telegramsource.zip hash is manifest-reported, not independently checked here.
- Remaining: inspect all chrome consumers and migrate family by family (A.2); implement drawer (B), popup (C), bulletin remaining work (D), then final integrated FULL (E).
- FAST #40497770337 on commit 989b202c2 FAILED: 416 tests / 1 failed. Real root cause: `TelegramThemeEngine.kt:111-114,201-215` imported-theme validator requires an authoritative recognized base role; newly added test fixture had only submenu roles. Repair adds `windowBackgroundGray` to this test fixture and uses an established negative-sign-safe filename; no production validation is weakened. Fresh FAST result pending.

## B.1-01 Remove redundant Home drawer destinations (no screen deletion)
- Parent main 78f2e8e3f919c96cc7aee476e7d15438d3c349ee; XML `activity_main.xml` removes exactly `layoutDrawerHistory`, `layoutDrawerSaved`, `layoutDrawerAbout` nodes.
- `MainActivity.kt` removes only the corresponding drawer bindings/callbacks, keeps existing History/Bookmarks popup routes and all six retained rows.
- `EveDesignSystemTest.kt` updates structural contract to 6 drawer rows and no removed IDs; source-exact drawer geometry still remains B.3, not claimed here.
- CI: check fresh commit-specific FAST run before marking VERIFIED.

## B.2-01 Add Settings → About real destination
- Parent main de92e0d5e9d943dfeb149fe00e90b5fd49a78f25; `activity_settings.xml` adds accessible About row under INFORMATION and existing section/card style. `SettingsActivity.kt` routes to the already manifest-registered `AboutActivity` (not a placeholder); prior independent Profile/About surface stays intact.
- `EveDesignSystemTest.kt` requires the About row and original minimum touch token; build and native navigation proof pending fresh FAST / final FULL.
- B.1 FAST #40499004153 on de92e0d5: SUCCESS (build, backend, firebase-functions).

## B.3-01 Fix Home pill/drawer stacking hierarchy
- Parent main 3d6a6ac5fcd275606b1d526336fef2821d5ddfff; `EveNavigationHost.kt` previously placed the Home pill directly in decor's `android.R.id.content` above `DrawerLayout`. New `homeContentHost/homeNavOverlay` in `activity_main.xml` makes the pill a descendant of DrawerLayout's content child, so actual native drawer sibling renders above it. Other screen hosts are unchanged.
- New source tests: JVM XML hierarchy assertions and `HomeDrawerNavigationNativeTest.kt` (native attach/START drawer). The panel's existing shape/color/inset code remains unchanged; EVE/Telegram geometry parity and system navigation-region surface paint NOT YET VERIFIED.
- Telegram `DrawerLayoutContainer.java:190-212` forwards system/cutout/IME insets; EVE's DrawerLayout is an adaptation, not a byte-exact port.
- B.2 FAST run #40499647957 on 3d6a6ac5: SUCCESS.

## C.1/C.2-01 Popup bounds, rows, source role colors
- Parent main 29f87b4348a0f806ec855c9700341d45af8f893b. `TelegramMenuPopup.kt` replaces unbounded UNSPECIFIED measures with window-visible-frame bounded EXACT/AT_MOST measures and clamped horizontal anchored offset. Android framework retains drop-down vertical positioning; seven rows live inside `menuScroll`.
- Original `ActionBarMenuSubItem.java:51,78-84,97,126-130`: 48dp default row, 18dp horizontal padding, 16sp text; EVE uses minimum 48dp and multiline-safe wrap content (ACCESSIBILITY ADAPTATION), not falsely called byte-exact.
- Original `ThemeColors.java:51,203-206` + `night.attheme:35,108,316,362,417`: submenu text/icon, background/selector colors. New `submenuSelector` preserves raw Day #0F000000 / Night #19FFFFFF and all role overrides. Popup updates foreground ripple and text/icon tints from active theme.
- Original source-backed assets ALREADY imported and unchanged: History msg_recent, Bookmarks msg_saved, Light mode msg_brightness_high, Logout msg_leave (density fingerprint docs). For Night-mode moon, Mistake Notebook, Topic-wise Test, PYQ, a semantically exact original counterpart is NOT VERIFIED; retain EVE glyphs unchanged. No fabricated icons.
- Popup DesignGlassLayout and its rounded/glass contour remain an EVE adaptation; original `ActionBarPopupWindow.java:153-170,531-538` nine-patch drawable renderer is not transplanted or falsely claimed exact. Full visual parity remains BLOCKED until source dependencies/license/rendering verified.
- Native interaction/IME/full-matrix clipping evidence still pending; FAST compilation is not device-pass evidence.

## C.2-02 AAPT scrollbars attribute repair
- Commit 10c2eedb FAST #40501567437 FAILED Android resource linking: `popup_telegram_menu.xml:6` used `android:scrollbars="insideOverlay"`, which is not valid; valid values are `none|horizontal|vertical`. `insideOverlay` belongs to `android:scrollbarStyle`.
- Corrected to vertical scrollbars + insideOverlay style. Added JVM XML assertions to prevent regression; no CI gate weakened. Recheck new FAST run on this repaired main SHA.

## C.3-01 Native popup viewport and seven-row test source
- Parent main 16fef77a86d8f7a67a258a91648e28e0b5447ff6. New `app/src/androidTest/java/com/eve/app/ui/HomeOverflowPopupNativeTest.kt` uses a credentials-free attached RepairVerificationActivity, actual TelegramMenuPopup, global screen bounds against visible display frame, seven rows (touch height and labels), scrollable last action, and dismissal.
- This is an AndroidTest source **not yet executed** on gesture/three-button devices. FAST compile gate is expected, but actual geometry/RTL/IME and theme-reveal tests remain UNKNOWN; do not mark native PASS from compilation.
- Repaired C.2 FAST #40501814392 on commit 16fef77a: SUCCESS (build/backend/firebase-functions).

## B.3-02 Home-only edge-to-edge inset routing
- Parent main 5badf981da3ad8ef347ee3dadb1f47a555fbd754. SystemBarHelper.kt lines 144-157 previously padded and consumed root android.R.id.content insets for every Activity, leaving the Home drawer short of transparent system navigation region.
- Changed Home-only path to pad mainContentContainer with saved base padding; leaves DrawerLayout/decor unpadded and forwards original insets to the drawer fitsSystemWindows content and existing floating pill listener. All non-Home behavior untouched.
- Extended HomeDrawerNavigationNativeTest.kt to dispatch synthetic 3-button system insets, assert full-window drawer host and Home body navigation clearance. Native test not executed on either navigation mode yet.
- C3 FAST #40502524249 on 5badf981: SUCCESS.

## B.3-03 Constrain drawer header and row labels without shortening destination text
- Parent main ecdf36a7e8ff044ad79d77021d536df4b83d827e. Home drawer header name/email/Premium line keep their actual accessible text and ellipsize within panel width; six destination row TextViews use weighted remaining width so icons and text do not overlap at large fonts. Row heights stay wrap_content with min 52dp inside scrollable list.
- Scoped structural JVM assertions added for bounded header; no view IDs, callbacks or premium animation modified. This is EVE accessibility adaptation, not an original Telegram drawer-profile renderer claim.
- B.3-02 FAST #40503561917 on ecdf36a7: SUCCESS (build/backend/functions). Native device rows/font scaling still UNKNOWN until FULL.

## D.0 / D.1–D.3 — Mission 04 baseline closure evidence (READ-ONLY)
- Baseline main 3e489fde602ca96e412cf43288149915ddae8e7c: FULL #40471817212 **SUCCESS six jobs**, superseding old failed #40469943287. Native UI gesture job 122082230914: 78/78 primary tests success, plus four 1/1 batches; native three-button job 122082230970: 78/78 primary tests success plus four 1/1 batches. These are historical *baseline-SHA* actual native executions, not current Mission 05 test execution.
- Source-provenance: official Telegram `Components/Bulletin.java` 2594 lines, SHA256 a140ff6cdc47f0b404272dedd18dbff962aa2510ccce87d9591d6cce6a7dc4c6, state/show/hide:151-158,247-287,420-492; `BulletinFactory.java` SHA256 611ffafeb8832c27f5871c90fb216bfb421720c16d08f7d18e550cc4f8a1d41f, undo icon/background:376,756-823. Original keys undo_background/undo_infoColor/undo_cancelColor are source roles; EVE's single-host queue, 8000ms error and accessibility viewport corrections are **ADAPTED**, never verbatim 100% Telegram behavior.
- GitHub compare original baseline → 11ebc12bedee857d0c074610e231ff04ee48b868 lists no changes to `AppBulletin.kt`, `BulletinHostNativeTest.kt`, `.github/scripts/verify-native-ui.sh` or `layout_telegram_bulletin.xml`; no unneeded M04 reimplementation was performed. Existing 78-test FULL included `BulletinHostNativeTest` cases for lifecycle, reduced-motion, bidirectional swipes, Hindi RTL, IME-if-available, long viewport, floating pill, theme and queued action. The IME conditional case is not evidence that an actual soft keyboard displayed on every run; treat hardware/device-specific IME behavior as UNKNOWN.
- On Mission 05 final SHA, bulletin can be affected indirectly by Home navigation host/inset changes, so require a new final FULL before claiming current integrated success.

## E.1 — Source coverage / exactness / scope audit
- Source extract independently verified: SHA256 b5bbfb7f78ec76769c888d30eb58831845e809b11ab413c722c3646a30be161e; 144 original files + 2 generated proofs; ZIP CRC all valid, per-entry SHA256/size matches. Complete original Telegramsource.zip hash is **not** independently verified.
- Direct upstream file hashes: `ThemeColors.java` 49f27ef5f1a33844ca8818feb4b737a96df8d8d4eaef50a20b11225c46c2000d; `Theme.java` b2cc15c52f4b82ca79cee15c3e3d62f93c4b4e91e578e6da850fd279a1591e90; `night.attheme` c93fb2934d2684888a5d9e72a36361c0789d6bc745db23043da7eb88155c5469; `DrawerLayoutContainer.java` 883035388ebadfaec375a5c5ca8e87183f63ac7d299b0172bb1fa63f9c3c9a5d; `ActionBarMenuSubItem.java` c3e9b530875ca77a70a628adb533de6ba059ce57184ed3a24249f5b2034ed271; `ActionBarPopupWindow.java` 4f4ccd178f82abf4e9cb1cdff330b57d34df9340bf23a2d86d75e1fe429a05c3.
- A global chrome: existing `docs/color-migration/active-color-inventory.tsv` contains 1204 classified entries: 452 source role/alias, 243 runtime/component producers, 265 EVE semantic/status exceptions, 143 compositing/platform primitives, 92 AA adaptations, 9 verified asset/default roles. `docs/color-migration/role-map.tsv` tracks 14 role families and existing source-linked tests. The already-present TelegramThemeEngine/Applier and Light/Night aliases are retained; Mission 05 adds Telegram submenu roles and tests. **ADAPTED** for foreground AA and native compositor; neither 1204-line older inventory nor source hashes prove screen-by-screen visual pixel parity on the new SHA.
- B drawer: exactly Profile, Premium, Performance, Overall Leaderboard, Syllabus and Settings; drawer-only History/Saved/About rows deleted without deleting their underlying activities/data. New Settings About row navigates to existing AboutActivity. The drawer/pill z-order and Home-only root/system inset routing are native Android adaptations. Original source `DrawerLayoutContainer.java:190-212` is a window/inset host, not a complete matching EVE sidebar-profile renderer: **NOT FOUND** for exact corner/width/shadow parity, retain existing panel shape rather than inventing Telegram values.
- C overflow: seven existing actions/callbacks preserved. Source-backed popup row default 48dp and horizontal padding 18dp from `ActionBarMenuSubItem.java:51,97`; background/text/icon/selector from `ThemeColors.java:51,203-206`, `night.attheme:35,108,316,362,417`. Actual EVE popup uses a scrollable 48dp **minimum** row and viewport-bounded PopupWindow (accessibility/platform **ADAPTED**), not exact Telegram drawing code. `ActionBarPopupWindow.java:153-170,531-538` refers to nine-patch assets, but this complete renderer/asset-license integration is **BLOCKED** from being represented as byte-exact. Missing semantically exact EVE-only Mistake Notebook / Topic Test / PYQ and night-moon glyphs **NOT FOUND**; existing glyphs untouched, no fake source icons.
- Third party source notice and release GPL/signing gates retained; no Telegram ZIP/assets or branded content added to repo. Upstream-root GPLv2 and per-file compatibility remain subject to the existing distribution gate; never claim GPL-3 combined binary clearance solely from upstream SHA or a green build.
- Changed paths since baseline are limited to theme-role files, Home Drawer/Settings/Popup source/layout, Home navigation/system insets, directly related JVM/native tests and this progress ledger. No Firebase/backend/Firestore/scoring/auth/admin/Lottie/security production sources edited.
- FAST record (commit-specific): A1 repair #40498445859 SUCCESS; B1 #40499004153 SUCCESS; B2 #40499647957 SUCCESS; Home drawer host #40500511870 SUCCESS; C2 first #40501567437 FAILED (AAPT attribute, repaired); C2 repair #40501814392 SUCCESS; C3 #40502524249 SUCCESS; Home inset repair #40503561917 SUCCESS; Drawer text repair #40504362925 SUCCESS.
- **Pre-final checkpoint:** latest FAST code SHA 11ebc12bedee857d0c074610e231ff04ee48b868 GREEN (Android/JVM/backend/firebase-functions). Actual new native tests and current final APK not yet executed/published. Current physical Samsung colors, IME, accessibility focus, RTL, animation and original-nine-patch pixel parity remain UNKNOWN or BLOCKED as itemized above.

## E.2/E.3 — Final mission-wide native and release gate
- The final [eve-full] commit below triggers project-standard FULL only after all achievable scoped code and FAST work above. Evaluate all six same-SHA jobs, screenshot evidence and APK. If red, repair root cause on main with FAST commits before new final FULL. A passing FULL does NOT clear the four source-exact icon roles, original popup nine-patch rendering/license, or EVE sidebar exactness.

## E.3-01 Final FULL trigger correction (no workflow gate change)
- Documentation-only final marker commit a8ee51adfe12065ba23234e31c92be0ed72abb37 did NOT start Build Eve APK because `.github/workflows/build.yml:4-12` uses `paths-ignore: docs/**, **.md`. It did pass Verify Documentation #40505153398. No premature FULL occurred.
- This final same-scope test commit adds raw source-based `dialogButtonSelector` Day #0F000000, Night #19FFFFFF and custom .attheme override assertions to `TelegramThemeEngineTest.kt`, so a nonignored source change will trigger the existing unchanged [eve-full] workflow.
- Last FAST before final #40504362925: SUCCESS on 11ebc12bedee857d0c074610e231ff04ee48b868. Final job/device/API screenshots and artifact outcomes to be recorded from exact new SHA, not assumed here.

## E.3-02 Repair FULL native popup coordinate-space assertion
- Baseline full SHA `7b63c6d75dd165c56a1ae601db23fa99d1266572`, workflow [#40505409856](https://github.com/vishu762701/eve-mock-test-app/actions/runs/40505409856): build/backend/firebase-functions SUCCESS; gesture/three-button native FAILED; publish SKIPPED.
- Both native logs showed `HomeOverflowPopupNativeTest.allSevenRowsRemainInsideVisibleWindowAndScroll` reported `Rect(0,0 - 656,882)` from `PopupWindow.contentView.getGlobalVisibleRect()` against **screen-space** visible display frame `Rect(0,63 - 1080,1794)` (or bottom 1857). `getGlobalVisibleRect()` uses the popup's own window origin and was not a comparable absolute display coordinate. This was a test-coordinate bug; it does NOT alone establish that popup was correctly positioned.
- Test now reads `root.getLocationOnScreen()` and constructs full-width/height screen-space bounds; retained top/bottom/left/right assertions (no weakened thresholds) and all seven row checks. FAST compilation then FULL native must prove actual popup position. On a genuine overflow from this corrected check, fix the production positioning instead of weakening the test.

## E.3-03 Repaired FAST verified; final FULL trigger
- `815e4bb85b02e8cb894d05af2f008867772b2427` [FAST #40512233373](https://github.com/vishu762701/eve-mock-test-app/actions/runs/40512233373): **SUCCESS** Android build/JVM/native-source compile, backend and firebase-functions. Native/Publish correctly SKIPPED because FAST.
- Source-test comment accompanies this [eve-full] final trigger, since docs-only changes are path-ignored by `.github/workflows/build.yml:4-12`. Production source/behavior and corrected bounds assertions remain unchanged. **Do not call final FULL success or APK published until exact-SHA six job results and artifact are read.**

## E.3-04 Distinguish popup animation transforms from real clipping
- `88d98eb892a54eb358894027eac79a8c34810794` FULL [#40513282309](https://github.com/vishu762701/eve-mock-test-app/actions/runs/40513282309): build/backend/functions SUCCESS, native three-button SUCCESS, gesture FAILED (15/80 primary tests), publish SKIPPED.
- Gesture popup test saw a screen-space position `(526,399)`, layout size `656x882` versus visible frame `(0,63)-(1080,1857)`. `TelegramPopupHelper.kt:49-61` scales the popup root 0.8 → 1.0 over 220ms about the anchor pivot; `getLocationOnScreen` includes that transform while `View.width/height` do not. The earlier bounds test mixed transformed origin and unscaled size. Gesture timing caught the animation; three-button suite passed.
- Native test now checks **visual scaled bounds while animation is active** and rechecks **full unscaled bounds after 350ms**, with all four viewport edges still enforced and seven rows preserved. No production popup behavior changed or test disabled. Next FAST and FULL required before VERIFIED.
- Gesture system foreground failures were separate: events log shows `am_anr` for `com.google.android.apps.nexuslauncher` at 13:40:53 (Input dispatching timed out; application has no focused window); many test screens reported `android` active package. NOT a reason to ignore EVE native tests; CI fixture stabilization must be diagnosed separately.
