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
