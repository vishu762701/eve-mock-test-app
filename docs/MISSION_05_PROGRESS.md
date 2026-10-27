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
