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
