# Telegram Android text/icon migration: scoped source register

**Status:** Partial migration delivered to `main`; final native UI and artifact results must be checked on the exact delivery commit. No app-wide or pixel-identical claim.

Telegram reference archive SHA-256: `0785365538ce05cf5bdf44a15d8fd5f5219f8193db0a2d124af9665fc63fd7b3` (uploaded `Telegram-master (1).zip`). The archive has no .git history, so upstream commit identity is not proven. EVE authoritative starting main SHA: `251475ad7712b4da22339f48bd9e242e9d6b2971`. EVE ZIP was not supplied to this chat.

Source paths below start at `TMessagesProj/src/main/java/org/telegram/`.

| EVE mapped text role | Telegram source:line | Telegram observed properties | EVE style | Fidelity |
|---|---|---|---|---|
| Settings/Notifications/Result heading | `ui/ActionBar/ActionBar.java:522-535,1437-1461` | 20dp portrait one-line title, 18dp when subtitle/landscape, 17dp glass; `AndroidUtilities.bold()`; title role | NavigationTitle 20sp, system medium | Partial: EVE headings are not full Telegram ActionBar, no responsive alternate |
| Settings row label | `ui/Cells/TextDetailSettingsCell.java:40-45` | 16dp regular, `key_windowBackgroundWhiteBlackText` | SettingsLabel 16sp | Mapped |
| Settings row supporting label | `ui/Cells/TextDetailSettingsCell.java:50-55,87-92` | 13dp, gray text, optional multiline | SettingsDetail 13sp | Mapped, wrapping preserved |
| Settings section heading | `ui/Cells/HeaderCell.java:48-53,94-99` | 14dp, `AndroidUtilities.bold()`, default `key_windowBackgroundWhiteBlueHeader` | SettingsSection 14sp, medium; dedicated day/night blue role | Light adapted for contrast; night exact theme value |
| Popup menu action | `ui/ActionBar/ActionBarMenuItem.java:493-504` | 16dp, submenu theme role | MenuItem 16sp | Mapped |
| Dialog title/message | `ui/ActionBar/AlertDialog.java:792-794,851-852` | 20dp bold / 16dp regular | DialogTitle/DialogMessage | Defined, NOT wired |
| Search field | `ui/ActionBar/ActionBarMenuItem.java:1530-1532` | 18dp, search text theme | SearchInput | Defined, NOT wired |

`AndroidUtilities.java:262-270,2392-2412`: Telegram title bold uses bundled `fonts/rmedium.ttf` or Android system-weight 500 depending on SharedConfig. EVE currently uses the Android system medium option; this does **not** guarantee the bundled default's exact glyph metrics. Telegram uses DIP units for many text properties; EVE uses SP to preserve Android fontScale accessibility. Theme color evidence: `ui/ActionBar/ThemeColors.java:14-16,100,107,197-204`, `assets/night.attheme:1-2,88,115,319,362,423`. Night secondary is deliberately opaque for contrast. Light supporting text is adjusted to `#6C6C70` to exceed 4.5:1 on the `#F5F5F5` canvas and `#FFFFFF` surface. The Telegram light `BlueHeader` theme role defaults to `#298ACF`, which is only 3.42:1 against EVE's grouped `#F5F5F5` background. EVE uses an explicitly accessibility-adapted `#176A9E` for Light header contrast (5.36:1) and Telegram's actual night `windowBackgroundWhiteBlueHeader=#6CB6F8` (night.attheme:93). These adaptations are NOT exact Telegram light palette reproduction.

### Icon source candidates: audited as paths only, NOT imported

| EVE action | Actual ZIP asset | Confidence |
|---|---|---|
| Back | `res/drawable-{mdpi,hdpi,xhdpi,xxhdpi}/ic_ab_back.webp` | ActionBar.java:288-293,453 accepts drawable; individual caller/state unconfirmed |
| Overflow | `res/drawable-{mdpi,hdpi,xhdpi,xxhdpi}/ic_ab_other.webp` | Asset found; call-site/state not verified |
| Search | `res/drawable-{mdpi,hdpi,xhdpi,xxhdpi}/msg_search.webp` | Asset found; call-site/state not verified |
| Delete | `res/drawable-{mdpi,hdpi,xhdpi,xxhdpi}/msg_delete.webp` | Asset found; contextual use unverified |
| Share | `res/drawable-{mdpi,hdpi,xhdpi,xxhdpi}/msg_share.webp` | Asset found; contextual use unverified |
| Bell | `res/drawable-{mdpi,hdpi,xhdpi,xxhdpi}/msg_notifications.webp` | **NO EXACT TELEGRAM COUNTERPART established** for EVE inbox/bell |
| Exam score, accuracy, ranking, attempts | none established | **NO EXACT TELEGRAM COUNTERPART** |

**Licensing blocker:** Uploaded root LICENSE:1-3 specifies GNU GPL v2. README.md:9-15 says avoid Telegram branding and publish source. At inspected EVE baseline top-level LICENSE was not present. GPL-version compatibility and whole-app source-distribution obligations are unresolved; **no Telegram-authored code, images or animations were copied; Google's separately licensed Roboto Medium font is documented below**. Only style parameters were independently authored. Do not import/deliver GPL assets until a compatible license/source-publication plan is verified.

**Protection:** Views with `android:tag="eve_telegram_type"` bypass the retired published config renderer in `TypographyApplier`; dynamic old presets cannot override them. This marker must not be reused for unrelated View tags.

**Regression:** `TelegramTypeSourceRolesTest.textRolesMeetNormalTextContrastOnDayAndNightSurfaces` covers primary, secondary, section and dialog roles against both EVE canvas and card surfaces. The previous failed native UI assertion was `StudentUiRenderingTest` Settings `PREFERENCES` (3.36:1); this fix changes app text colors rather than weakening the assertion.

**Remaining gates:** all other screen roles (questions, timers, scores, button, badge, input, dialog), source-exact icon art/states/motion, full layout/dynamic View inventory, cache/Inter cleanup, custom fonts, license compatibility, light/dark screenshots, RTL/Devanagari/fontScale, native UI, security and publication checks. No obsolete file deleted because reachability is not fully established.

## Main-only incremental component coverage (second change)

Source-verified matches from the same uploaded Telegram ZIP:
- `ui/Cells/DialogCell.java:1252-1254` and `ui/ActionBar/Theme.java:7803-7815`: conversation-row name 17dp medium/bold() and preview 16dp regular. Adapted to EVE `item_notification.xml` title/body at **17sp/16sp**, without copying Telegram messaging content or interactions.
- `ui/ActionBar/Theme.java:7897,7926`: dialog-list date text 12dp and `key_chats_date`, mapped to the EVE inbox relative-time label as 12sp with accessible secondary color. These are semantic counterparts, not an assertion that notifications and chats are identical products.
- `ui/ActionBar/ActionBarMenuItem.java:1530-1532`: 18dp ActionBar search text. Wired Home's `etSearch` to a derived `Widget.Eve.Token.Input.TelegramSearch` that **inherits** EVE's input background, focus states, cursor, input type and touch area. Style parameters are adapted to SP.
- `ui/Cells/TextSettingsCell.java:82` / `TextDetailSettingsCell.java:40-45`: 16dp settings-action label applied to About screen's Privacy/Terms/Contact links. About navigation title uses the already mapped ActionBar-derived style.

All migrated views bear the `eve_telegram_type` tag to avoid a stale published Inter/iOS typography override. Existing EVE bell drawing and Lottie, button callbacks and scoring logic are untouched. No Telegram-authored GPL source/art assets were copied; the independently Google-licensed font is documented below.

### Remaining critical blockers
- GPL-2.0-only compatibility and full corresponding-source obligations are unresolved: no Telegram icon art, RLottie, or bundled Telegram fonts may be imported into a production build without resolution.
- Other views, programmatic custom Views, dialog internals, exam-specific roles and Telegram-unique icon states remain unmapped; do not describe the app as Telegram-exact.
- CI Android build/lint/native UI, light/dark captures, font scale 1.0/enlarged, Devanagari/RTL, permission and motion regression require evidence on the new commit.

## Main-only native typography foundation — full shared-token coverage

Mapped all `TextAppearance.Eve.*` and `TextAppearance.Eve.Token.*` typography resources, plus shared Android button/input widget fonts and `Theme.Eve.Foundation` family, away from bundled Inter and onto platform `sans-serif` / `sans-serif-medium` (Telegram `AndroidUtilities.bold()` uses platform 500 on supported devices). Original fonts/assets are preserved because standalone license and dependency reachability are unresolved, **not** because they remain approved default.

The shared Home/nav title and content title now use 20sp/26sp as the ActionBar title analogue. Body 16sp/22sp maps Telegram message/setting regular text. Heading 17sp/22sp maps DialogCell conversation header. The 34sp hero value and 11/12sp auxiliary labels are EVE exam-content-specific compatibility semantics, *not* asserted official Telegram universal typography roles. All base/sw360/sw600 dimension variants were updated together. Semantic success/error colors are retained for exam-result meaning.

The prior published `ios26_balanced` typography configuration/cache and backend contract are retained but no longer allowed to repaint XML text or programmatic `DesignType` components. A future `telegram_native` preset requires separate compatibility/security verification before remote overrides may be re-enabled. No deprecated server API or bundled fonts were removed because active dependencies have not been fully audited. Tested compatibility role constraints are updated to measure **actual new fonts and sizes**, not disabled.

## Source-fidelity correction: the guessed icon pass was reverted

The prior UI-only 2dp uniform-stroke edits and independently improvised share-arrow geometry are **NOT real Telegram source mappings**. They were explicitly rejected as insufficient by the owner's source-exact requirement. The 10 affected vector drawables (`ic_back`, `ic_more_vert`, `ic_search`, `ic_delete`, `ic_bell`, `ic_settings`, `ic_bookmark`, `ic_close`, `ic_send`, `ic_share`) were restored from verified pre-mission EVE commit `251475ad7712b4da22339f48bd9e242e9d6b2971`. Original resource names, touch areas, tint and event handlers remain intact. The retained MIT Tabler baseline is a **temporary working fallback**, not the requested Telegram-exact replacement.

Actual Telegram static art candidates and call sites remain in the source register. Exact Telegram binaries and procedural animation code are **BLOCKED for import pending compatible GPLv2-or-later licensing and complete corresponding-source publication analysis**. Preserve EVE domain-specific icons where Telegram has no exact counterpart. The app must not ship under a false "Telegram icons migrated" label.
## Native UI regression discovered and corrected at test-contract level

The first global-font migration CI run `39776033769` passed Android build/lint/unit, backend and Firebase, but gesture instrumentation found three stale iOS typography assertions (actual 16sp option text vs former 17sp, modal `fontFamily=sans-serif` returned TYPE_STRING rather than the retired Inter font resource ID). The corresponding tests were updated to assert the **new runtime tokens** while preserving every size, layout, state, gesture, question-answer functional and image evidence check. Production XML/Java/Kotlin UI was NOT changed for this test-only correction. This is a replacement of obsolete iOS expectations, not weakening an unmet Telegram or functional guarantee. Confirm the new commit's CI before claiming green.

Follow-up stacktrace `ApprovedUiRenderingTest.kt:254` specifically identified the question-body 17sp assertion, not only the answer option. The native test now checks both question and answer text at the migrated 16sp size.

### Dialog + ActionBar source correction (new main change)

Telegram source ZIP `ui/ActionBar/ActionBar.java:522-537,1443-1455` binds default title to `Theme.key_actionBarDefaultTitle`, bold() system 500 or bundled rmedium, padding 8dp top/bottom, and no-subtitle title text size 20dp default, 18dp phone landscape, 20dp tablet. Existing EVE navigation title role is 20sp; new qualified `values-land/telegram_typography.xml` uses 18sp on phone landscape, and `values-sw600dp-land/telegram_typography.xml` restores 20sp on tablets. EVE existing toolbar padding is preserved to avoid unapproved geometry change; this is a **documented deviation**, not pixel-exact ActionBar.

Telegram `ui/ActionBar/AlertDialog.java:788-795,849-854` gives title 20dp with `bold()` and themed dialog text, and message 16dp regular with the same dialog text role in the standard case. Shared `Widget.Eve.Token.Dialog.Title` and `Body` now use `TextAppearance.Eve.Telegram.DialogTitle` (20sp) and `DialogMessage` (16sp). Feedback-create and feedback-reply custom modal headers are mapped; existing controls, accessibility tags and form behavior are unchanged. Theme resolution stays day/night through `telegram_type_dialog`, and Dp→Sp remains the accessibility deviation.

## Exact Telegram default Roboto Medium — Google Apache-2.0 asset

The Telegram archive's actual `SharedConfig.java:619` default is `useSystemBoldFont=false`. `AndroidUtilities.java:262-270` therefore uses bundled `rmedium.ttf` for `bold()` unless system font is explicitly selected. The exact Google-owned Roboto Medium 2.138 font file, from matching archive/upstream Git blob `aba251d159c783048b352d17f6581d406f2a6ddb`, now ships under `res/font/telegram_roboto_medium.ttf` and is applied to audited ActionBar/Section/Dialog/Notification title roles. Its Google copyright, full Apache-2.0 license and source SHA-256 are bundled in app assets. No Telegram GPL-owned icon or Java code is copied.

This corrects the earlier inexact system-500 substitute for the named roles only. Global/app-specific legacy types and platform-dependent fallback renderings remain a documented fidelity gap; do not label the whole mission complete. Details and 95-layout inventory: `docs/telegram-full-layout-inventory.md`.
